import argparse
import asyncio
import json
import os
import re
import threading
import traceback
import logging
import uuid
from typing import Any, Dict, Iterable, AsyncIterable, AsyncGenerator, Optional
import cozeloop
import uvicorn
import time
import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import StreamingResponse, JSONResponse
from langchain_core.runnables import RunnableConfig
from langgraph.graph import StateGraph, END
from langgraph.graph.state import CompiledStateGraph
from coze_coding_utils.runtime_ctx.context import new_context, Context
from coze_coding_utils.helper import graph_helper
from coze_coding_utils.log.node_log import LOG_FILE
from coze_coding_utils.log.write_log import setup_logging, request_context
from coze_coding_utils.log.config import LOG_LEVEL
from coze_coding_utils.error.classifier import ErrorClassifier, classify_error
from coze_coding_utils.helper.stream_runner import AgentStreamRunner, WorkflowStreamRunner,agent_stream_handler,workflow_stream_handler, RunOpt

setup_logging(
    log_file=LOG_FILE,
    max_bytes=100 * 1024 * 1024, # 100MB
    backup_count=5,
    log_level=LOG_LEVEL,
    use_json_format=True,
    console_output=True
)

logger = logging.getLogger(__name__)
from coze_coding_utils.helper.agent_helper import to_stream_input
from coze_coding_utils.log.parser import LangGraphParser
from coze_coding_utils.log.err_trace import extract_core_stack
from coze_coding_utils.log.loop_trace import init_run_config, init_agent_config

load_dotenv(override=True)


# 超时配置常量
TIMEOUT_SECONDS = 900  # 15分钟

class GraphService:
    def __init__(self):
        # 用于跟踪正在运行的任务（使用asyncio.Task）
        self.running_tasks: Dict[str, asyncio.Task] = {}
        # 错误分类器
        self.error_classifier = ErrorClassifier()
        # stream runner
        self._agent_stream_runner = AgentStreamRunner()
        self._workflow_stream_runner = WorkflowStreamRunner()
        self._graph = None
        self._graph_lock = threading.Lock()

    def _get_graph(self, ctx=Context):
        if graph_helper.is_agent_proj():
            return graph_helper.get_agent_instance("agents.agent", ctx)

        if self._graph is not None:
            return self._graph
        with self._graph_lock:
            if self._graph is not None:
                return self._graph
            self._graph = graph_helper.get_graph_instance("graphs.graph")
            return self._graph

    @staticmethod
    def _sse_event(data: Any, event_id: Any = None) -> str:
        id_line = f"id: {event_id}\n" if event_id else ""
        return f"{id_line}event: message\ndata: {json.dumps(data, ensure_ascii=False, default=str)}\n\n"

    def _get_stream_runner(self):
        if graph_helper.is_agent_proj():
            return self._agent_stream_runner
        else:
            return self._workflow_stream_runner

    # 流式运行（原始迭代器）：本地调用使用
    def stream(self, payload: Dict[str, Any], run_config: RunnableConfig, ctx=Context) -> Iterable[Any]:
        graph = self._get_graph(ctx)
        stream_runner = self._get_stream_runner()
        for chunk in stream_runner.stream(payload, graph, run_config, ctx):
            yield chunk

    # 同步运行：本地/HTTP 通用
    async def run(self, payload: Dict[str, Any], ctx=None) -> Dict[str, Any]:
        if ctx is None:
            ctx = new_context("run")

        run_id = ctx.run_id
        logger.info(f"Starting run with run_id: {run_id}")

        try:
            graph = self._get_graph(ctx)
            # custom tracer
            run_config = init_run_config(graph, ctx)
            run_config["configurable"] = {"thread_id": ctx.run_id}

            # 直接调用，LangGraph会在当前任务上下文中执行
            # 如果当前任务被取消，LangGraph的执行也会被取消
            return await graph.ainvoke(payload, config=run_config, context=ctx)

        except asyncio.CancelledError:
            logger.info(f"Run {run_id} was cancelled")
            return {"status": "cancelled", "run_id": run_id, "message": "Execution was cancelled"}
        except Exception as e:
            # 使用错误分类器分类错误
            err = self.error_classifier.classify(e, {"node_name": "run", "run_id": run_id})
            # 记录详细的错误信息和堆栈跟踪
            logger.error(
                f"Error in GraphService.run: [{err.code}] {err.message}\n"
                f"Category: {err.category.name}\n"
                f"Traceback:\n{extract_core_stack()}"
            )
            # 保留原始异常堆栈，便于上层返回真正的报错位置
            raise
        finally:
            # 清理任务记录
            self.running_tasks.pop(run_id, None)

    # 流式运行（SSE 格式化）：HTTP 路由使用
    async def stream_sse(self, payload: Dict[str, Any], ctx=None, run_opt: Optional[RunOpt] = None) -> AsyncGenerator[str, None]:
        if ctx is None:
            ctx = new_context(method="stream_sse")
        if run_opt is None:
            run_opt = RunOpt()

        run_id = ctx.run_id
        logger.info(f"Starting stream with run_id: {run_id}")
        graph = self._get_graph(ctx)
        if graph_helper.is_agent_proj():
            run_config = init_agent_config(graph, ctx)
        else:
            run_config = init_run_config(graph, ctx)  # vibeflow

        is_workflow = not graph_helper.is_agent_proj()

        try:
            async for chunk in self.astream(payload, graph, run_config=run_config, ctx=ctx, run_opt=run_opt):
                if is_workflow and isinstance(chunk, tuple):
                    event_id, data = chunk
                    yield self._sse_event(data, event_id)
                else:
                    yield self._sse_event(chunk)
        finally:
            # 清理任务记录
            self.running_tasks.pop(run_id, None)
            cozeloop.flush()

    # 取消执行 - 使用asyncio的标准方式
    def cancel_run(self, run_id: str, ctx: Optional[Context] = None) -> Dict[str, Any]:
        """
        取消指定run_id的执行

        使用asyncio.Task.cancel()来取消任务,这是标准的Python异步取消机制。
        LangGraph会在节点之间检查CancelledError,实现优雅的取消。
        """
        logger.info(f"Attempting to cancel run_id: {run_id}")

        # 查找对应的任务
        if run_id in self.running_tasks:
            task = self.running_tasks[run_id]
            if not task.done():
                # 使用asyncio的标准取消机制
                # 这会在下一个await点抛出CancelledError
                task.cancel()
                logger.info(f"Cancellation requested for run_id: {run_id}")
                return {
                    "status": "success",
                    "run_id": run_id,
                    "message": "Cancellation signal sent, task will be cancelled at next await point"
                }
            else:
                logger.info(f"Task already completed for run_id: {run_id}")
                return {
                    "status": "already_completed",
                    "run_id": run_id,
                    "message": "Task has already completed"
                }
        else:
            logger.warning(f"No active task found for run_id: {run_id}")
            return {
                "status": "not_found",
                "run_id": run_id,
                "message": "No active task found with this run_id. Task may have already completed or run_id is invalid."
            }

    # 运行指定节点：本地/HTTP 通用
    async def run_node(self, node_id: str, payload: Dict[str, Any], ctx=None) -> Any:
        if ctx is None or Context.run_id == "":
            ctx = new_context(method="node_run")

        _graph = self._get_graph()
        node_func, input_cls, output_cls = graph_helper.get_graph_node_func_with_inout(_graph.get_graph(), node_id)
        if node_func is None or input_cls is None:
            raise KeyError(f"node_id '{node_id}' not found")

        parser = LangGraphParser(_graph)
        metadata = parser.get_node_metadata(node_id) or {}

        _g = StateGraph(input_cls, input_schema=input_cls, output_schema=output_cls)
        _g.add_node("sn", node_func, metadata=metadata)
        _g.set_entry_point("sn")
        _g.add_edge("sn", END)
        _graph = _g.compile()

        run_config = init_run_config(_graph, ctx)
        return await _graph.ainvoke(payload, config=run_config)

    def graph_inout_schema(self) -> Any:
        if graph_helper.is_agent_proj():
            return {"input_schema": {}, "output_schema": {}}
        builder = getattr(self._get_graph(), 'builder', None)
        if builder is not None:
            input_cls = getattr(builder, 'input_schema', None) or self.graph.get_input_schema()
            output_cls = getattr(builder, 'output_schema', None) or self.graph.get_output_schema()
        else:
            logger.warning(f"No builder input schema found for graph_inout_schema, using graph input schema instead")
            input_cls = self.graph.get_input_schema()
            output_cls = self.graph.get_output_schema()

        return {
            "input_schema": input_cls.model_json_schema(), 
            "output_schema": output_cls.model_json_schema(),
            "code":0,
            "msg":""
        }

    async def astream(self, payload: Dict[str, Any], graph: CompiledStateGraph, run_config: RunnableConfig, ctx=Context, run_opt: Optional[RunOpt] = None) -> AsyncIterable[Any]:
        stream_runner = self._get_stream_runner()
        async for chunk in stream_runner.astream(payload, graph, run_config, ctx, run_opt):
            yield chunk


service = GraphService()
app = FastAPI()


def _get_openai_base_url() -> str:
    return os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/")


def _is_ollama_base_url(base_url: str) -> bool:
    normalized = (base_url or "").lower()
    return "11434" in normalized or "ollama" in normalized


def _resolve_chat_options(base_url: str, temperature: Any, max_tokens: Any):
    resolved_temperature = 0.1
    if temperature is not None:
        resolved_temperature = float(temperature)

    if max_tokens is not None:
        resolved_max_tokens = int(max_tokens)
    elif _is_ollama_base_url(base_url):
        # Keep local Ollama replies shorter so the first answer returns sooner.
        resolved_max_tokens = 128
    else:
        resolved_max_tokens = 512

    resolved_timeout = float(os.getenv("OPENAI_TIMEOUT_SECONDS", "180"))
    return resolved_temperature, resolved_max_tokens, resolved_timeout

# OpenAI 兼容接口处理器
def _create_chat_llm(model: Optional[str], temperature: Any, max_tokens: Any):
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key or api_key in ("your-api-key-here", "sk-xxx"):
        raise RuntimeError("OPENAI_API_KEY is not configured")

    from langchain_openai import ChatOpenAI

    base_url = _get_openai_base_url()
    default_headers = {}
    if "openrouter" in base_url:
        default_headers = {
            "HTTP-Referer": "https://github.com/rpa-management",
            "X-Title": "RPA Management Platform",
        }

    resolved_temperature, resolved_max_tokens, resolved_timeout = _resolve_chat_options(
        base_url, temperature, max_tokens
    )

    return ChatOpenAI(
        api_key=api_key,
        base_url=base_url,
        model=model or os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
        temperature=resolved_temperature,
        max_tokens=resolved_max_tokens,
        timeout=resolved_timeout,
        max_retries=0,
        default_headers=default_headers,
    )


async def _proxy_ollama_chat_completion(payload: Dict[str, Any], model: str):
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    base_url = _get_openai_base_url()
    resolved_temperature, resolved_max_tokens, resolved_timeout = _resolve_chat_options(
        base_url, payload.get("temperature"), payload.get("max_tokens")
    )

    headers = {
        "Content-Type": "application/json",
    }
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"

    request_payload = {
        "model": model,
        "messages": payload.get("messages", []),
        "stream": False,
        "temperature": resolved_temperature,
        "max_tokens": resolved_max_tokens,
    }

    async with httpx.AsyncClient(timeout=resolved_timeout) as client:
        response = await client.post(
            f"{base_url}/chat/completions",
            json=request_payload,
            headers=headers,
        )
        response.raise_for_status()
        body = response.json()

    content = _coerce_chat_text(
        (((body.get("choices") or [{}])[0]).get("message") or {}).get("content")
    )
    if not content:
        raise RuntimeError("Empty assistant response")

    return body.get("model") or model, content, body.get("usage")


def _coerce_chat_text(content: Any) -> str:
    if content is None:
        return ""

    if isinstance(content, str):
        return content.strip()

    if isinstance(content, list):
        parts = []
        for item in content:
            if isinstance(item, str):
                text = item.strip()
                if text:
                    parts.append(text)
                continue

            if not isinstance(item, dict):
                text = str(item).strip()
                if text:
                    parts.append(text)
                continue

            item_type = item.get("type")
            if item_type == "text":
                text = str(item.get("text", "")).strip()
                if text:
                    parts.append(text)
                continue

            if item_type == "image_url":
                image = item.get("image_url") or {}
                url = str(image.get("url", "")).strip()
                if url:
                    parts.append(url)
                continue

            text = str(item).strip()
            if text:
                parts.append(text)

        return "\n".join(parts)

    return str(content).strip()


def _convert_openai_messages(messages_raw: Any):
    from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

    converted = []
    for item in messages_raw or []:
        if not isinstance(item, dict):
            continue

        content = _coerce_chat_text(item.get("content"))
        if not content:
            continue

        role = str(item.get("role", "user")).lower()
        if role == "system":
            converted.append(SystemMessage(content=content))
        elif role == "assistant":
            converted.append(AIMessage(content=content))
        else:
            converted.append(HumanMessage(content=content))

    return converted


def _build_openai_response(request_id: str, model: str, content: str, usage: Optional[Dict[str, int]] = None) -> Dict[str, Any]:
    response = {
        "id": request_id,
        "object": "chat.completion",
        "created": int(time.time()),
        "model": model,
        "choices": [
            {
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": content,
                },
                "finish_reason": "stop",
            }
        ],
    }
    if usage:
        response["usage"] = usage
    return response


def _build_openai_error(message: str, status_code: int, error_type: str, code: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={
            "error": {
                "message": message,
                "type": error_type,
                "code": code,
            }
        },
    )


def _find_latest_message_content(messages_raw: Any, role: str) -> str:
    expected_role = role.lower()
    for item in reversed(messages_raw or []):
        if not isinstance(item, dict):
            continue

        actual_role = str(item.get("role", "")).lower()
        if actual_role != expected_role:
            continue

        content = _coerce_chat_text(item.get("content"))
        if content:
            return content

    return ""


def _extract_prompt_section(system_prompt: str, label: str) -> str:
    if not system_prompt:
        return ""

    pattern = rf"【{re.escape(label)}】\s*(.*?)(?=\n\s*【|\Z)"
    matched = re.search(pattern, system_prompt, flags=re.S)
    return matched.group(1).strip() if matched else ""


def _trim_text(value: str, max_length: int = 280) -> str:
    compact = re.sub(r"\s+", " ", (value or "")).strip()
    if len(compact) <= max_length:
        return compact
    return compact[: max_length - 3] + "..."


def _build_fallback_chat_response(messages_raw: Any, error: Exception) -> str:
    system_prompt = _find_latest_message_content(messages_raw, "system")
    user_question = _find_latest_message_content(messages_raw, "user") or "未提供具体问题"
    last_assistant = _find_latest_message_content(messages_raw, "assistant")

    source_title = _extract_prompt_section(system_prompt, "采集结果标题")
    source_url = _extract_prompt_section(system_prompt, "采集结果URL")
    source_summary = _extract_prompt_section(system_prompt, "采集结果摘要")
    initial_analysis = _extract_prompt_section(system_prompt, "首次分析结论")

    context_text = _trim_text(last_assistant or initial_analysis or source_summary, 420)
    question_text = user_question.strip()
    lower_question = question_text.lower()

    if any(keyword in question_text for keyword in ("网址", "链接", "地址")) or "url" in lower_question:
        direct_answer = f"当前采集到的页面地址是：{source_url}" if source_url else "当前上下文里没有提取到可直接返回的网址。"
    elif any(keyword in question_text for keyword in ("标题", "名称", "叫什么", "是什么")):
        if source_title:
            direct_answer = f"当前采集对象是“{source_title}”。"
        elif context_text:
            direct_answer = f"基于现有分析，它对应的对象信息是：{context_text}"
        else:
            direct_answer = "当前上下文不足，暂时无法准确判断这个对象是什么。"
    elif any(keyword in question_text for keyword in ("做什么", "干什么", "功能", "用途", "作用")):
        if context_text:
            direct_answer = f"基于现有采集结果，它目前更像是这样一个对象：{context_text}"
        else:
            direct_answer = "当前采集结果信息较少，只能确认这是本次分析关联的页面，具体功能仍需要更多原始内容。"
    elif any(keyword in question_text for keyword in ("总结", "摘要", "概括", "概述")):
        direct_answer = context_text or "当前采集结果信息较少，暂时无法给出更完整的总结。"
    elif any(keyword in question_text for keyword in ("风险", "问题", "隐患", "注意")):
        direct_answer = "基于当前采集结果，暂未提取到明确的风险条目；现阶段更大的限制是原始页面信息不足，很多判断还不能下结论。"
    else:
        direct_answer = context_text or "我已经收到你的问题，但当前只能基于已有采集结果和历史分析提供有限回答。"

    reply_lines = [
        "当前问答服务遇到了上游模型临时限流，我先基于平台里已有的采集结果和历史分析给你一个保底答复。",
        "",
        f"关于你的问题“{question_text}”：",
        direct_answer,
    ]

    if source_title:
        reply_lines.extend(["", f"来源标题：{source_title}"])
    if source_url:
        reply_lines.append(f"来源地址：{source_url}")
    if source_summary and source_summary not in direct_answer:
        reply_lines.extend(["", f"现有摘要：{_trim_text(source_summary, 220)}"])

    reply_lines.extend([
        "",
        "说明：这次回复使用的是本地兜底回答，没有新增超出当前采集结果之外的推断。",
    ])

    logger.warning("Using fallback chat response due to upstream LLM failure: %s", error)
    return "\n".join(reply_lines)

HEADER_X_RUN_ID = "x-run-id"
@app.post("/run")
async def http_run(request: Request) -> Dict[str, Any]:
    global result
    raw_body = await request.body()
    try:
        body_text = raw_body.decode("utf-8")
    except Exception as e:
        body_text = str(raw_body)
        raise HTTPException(status_code=400,
                            detail=f"Invalid JSON format: {body_text}, traceback: {traceback.format_exc()}, error: {e}")

    ctx = new_context(method="run", headers=request.headers)
    # 优先使用上游指定的 run_id，保证 cancel 能精确匹配
    upstream_run_id = request.headers.get(HEADER_X_RUN_ID)
    if upstream_run_id:
        ctx.run_id = upstream_run_id
    run_id = ctx.run_id
    request_context.set(ctx)

    logger.info(
        f"Received request for /run: "
        f"run_id={run_id}, "
        f"query={dict(request.query_params)}, "
        f"body={body_text}"
    )

    try:
        payload = await request.json()

        # 创建任务并记录 - 这是关键，让我们可以通过run_id取消任务
        task = asyncio.create_task(service.run(payload, ctx))
        service.running_tasks[run_id] = task

        try:
            result = await asyncio.wait_for(task, timeout=float(TIMEOUT_SECONDS))
        except asyncio.TimeoutError:
            logger.error(f"Run execution timeout after {TIMEOUT_SECONDS}s for run_id: {run_id}")
            task.cancel()
            try:
                result = await task
            except asyncio.CancelledError:
                return {
                    "status": "timeout",
                    "run_id": run_id,
                    "message": f"Execution timeout: exceeded {TIMEOUT_SECONDS} seconds"
                }

        if not result:
            result = {}
        if isinstance(result, dict):
            result["run_id"] = run_id
        return result

    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error in http_run: {e}, traceback: {traceback.format_exc()}")
        raise HTTPException(status_code=400, detail=f"Invalid JSON format, {extract_core_stack()}")

    except asyncio.CancelledError:
        logger.info(f"Request cancelled for run_id: {run_id}")
        result = {"status": "cancelled", "run_id": run_id, "message": "Execution was cancelled"}
        return result

    except Exception as e:
        # 使用错误分类器获取错误信息
        error_response = service.error_classifier.get_error_response(e, {"node_name": "http_run", "run_id": run_id})
        logger.error(
            f"Unexpected error in http_run: [{error_response['error_code']}] {error_response['error_message']}, "
            f"traceback: {traceback.format_exc()}", exc_info=True
        )
        raise HTTPException(
            status_code=500,
            detail={
                "error_code": error_response["error_code"],
                "error_message": error_response["error_message"],
                "stack_trace": extract_core_stack(),
            }
        )
    finally:
        cozeloop.flush()


HEADER_X_WORKFLOW_STREAM_MODE = "x-workflow-stream-mode"


def _register_task(run_id: str, task: asyncio.Task):
    service.running_tasks[run_id] = task


@app.post("/stream_run")
async def http_stream_run(request: Request):
    ctx = new_context(method="stream_run", headers=request.headers)
    # 优先使用上游指定的 run_id，保证 cancel 能精确匹配
    upstream_run_id = request.headers.get(HEADER_X_RUN_ID)
    if upstream_run_id:
        ctx.run_id = upstream_run_id
    workflow_stream_mode = request.headers.get(HEADER_X_WORKFLOW_STREAM_MODE, "").lower()
    workflow_debug = workflow_stream_mode == "debug"
    request_context.set(ctx)
    raw_body = await request.body()
    try:
        body_text = raw_body.decode("utf-8")
    except Exception as e:
        body_text = str(raw_body)
        raise HTTPException(status_code=400,
                            detail=f"Invalid JSON format: {body_text}, traceback: {extract_core_stack()}, error: {e}")
    run_id = ctx.run_id
    is_agent = graph_helper.is_agent_proj()
    logger.info(
        f"Received request for /stream_run: "
        f"run_id={run_id}, "
        f"is_agent_project={is_agent}, "
        f"query={dict(request.query_params)}, "
        f"body={body_text}"
    )
    try:
        payload = await request.json()
    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error in http_stream_run: {e}, traceback: {traceback.format_exc()}")
        raise HTTPException(status_code=400, detail=f"Invalid JSON format:{extract_core_stack()}")

    if is_agent:
        stream_generator = agent_stream_handler(
            payload=payload,
            ctx=ctx,
            run_id=run_id,
            stream_sse_func=service.stream_sse,
            sse_event_func=service._sse_event,
            error_classifier=service.error_classifier,
            register_task_func=_register_task,
        )
    else:
        stream_generator = workflow_stream_handler(
            payload=payload,
            ctx=ctx,
            run_id=run_id,
            stream_sse_func=service.stream_sse,
            sse_event_func=service._sse_event,
            error_classifier=service.error_classifier,
            register_task_func=_register_task,
            run_opt=RunOpt(workflow_debug=workflow_debug),
        )

    response = StreamingResponse(stream_generator, media_type="text/event-stream")
    return response

@app.post("/cancel/{run_id}")
async def http_cancel(run_id: str, request: Request):
    """
    取消指定run_id的执行

    使用asyncio.Task.cancel()实现取消,这是Python标准的异步任务取消机制。
    LangGraph会在节点之间的await点检查CancelledError,实现优雅取消。
    """
    ctx = new_context(method="cancel", headers=request.headers)
    request_context.set(ctx)
    logger.info(f"Received cancel request for run_id: {run_id}")
    result = service.cancel_run(run_id, ctx)
    return result


@app.post(path="/node_run/{node_id}")
async def http_node_run(node_id: str, request: Request):
    raw_body = await request.body()
    try:
        body_text = raw_body.decode("utf-8")
    except UnicodeDecodeError:
        body_text = str(raw_body)
        raise HTTPException(status_code=400, detail=f"Invalid JSON format: {body_text}")
    ctx = new_context(method="node_run", headers=request.headers)
    request_context.set(ctx)
    logger.info(
        f"Received request for /node_run/{node_id}: "
        f"query={dict(request.query_params)}, "
        f"body={body_text}",
    )

    try:
        payload = await request.json()
    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error in http_node_run: {e}, traceback: {traceback.format_exc()}")
        raise HTTPException(status_code=400, detail=f"Invalid JSON format:{extract_core_stack()}")
    try:
        return await service.run_node(node_id, payload, ctx)
    except KeyError:
        raise HTTPException(status_code=404,
                            detail=f"node_id '{node_id}' not found or input miss required fields, traceback: {extract_core_stack()}")
    except Exception as e:
        # 使用错误分类器获取错误信息
        error_response = service.error_classifier.get_error_response(e, {"node_name": node_id})
        logger.error(
            f"Unexpected error in http_node_run: [{error_response['error_code']}] {error_response['error_message']}, "
            f"traceback: {traceback.format_exc()}", exc_info=True
        )
        raise HTTPException(
            status_code=500,
            detail={
                "error_code": error_response["error_code"],
                "error_message": error_response["error_message"],
                "stack_trace": extract_core_stack(),
            }
        )
    finally:
        cozeloop.flush()


@app.post("/v1/chat/completions")
async def openai_chat_completions(request: Request):
    """OpenAI Chat Completions API 兼容接口"""
    ctx = new_context(method="openai_chat", headers=request.headers)
    request_context.set(ctx)

    logger.info(f"Received request for /v1/chat/completions: run_id={ctx.run_id}")

    try:
        payload = await request.json()
        session_id = str(
            payload.get("session_id")
            or payload.get("taskId")
            or payload.get("task_id")
            or ctx.run_id
        ).strip()
        payload["session_id"] = session_id

        if payload.get("stream"):
            return _build_openai_error("stream=true is not supported", 400, "invalid_request_error", "400003")

        raw_messages = payload.get("messages", [])
        messages = _convert_openai_messages(raw_messages)
        if not messages:
            return _build_openai_error("No user message found", 400, "invalid_request_error", "400002")

        model = os.getenv("OPENAI_MODEL", "").strip() or payload.get("model") or "gpt-4o-mini"
        usage = None
        try:
            if _is_ollama_base_url(_get_openai_base_url()):
                model, content, usage = await _proxy_ollama_chat_completion(payload, model)
            else:
                llm = _create_chat_llm(model, payload.get("temperature"), payload.get("max_tokens"))
                result = await llm.ainvoke(messages)
                content = _coerce_chat_text(result.content)
                if not content:
                    raise RuntimeError("Empty assistant response")

                usage_metadata = getattr(result, "usage_metadata", None)
                if isinstance(usage_metadata, dict):
                    usage = {
                        "prompt_tokens": int(usage_metadata.get("input_tokens", 0)),
                        "completion_tokens": int(usage_metadata.get("output_tokens", 0)),
                        "total_tokens": int(usage_metadata.get("total_tokens", 0)),
                    }
        except Exception as llm_error:
            model = f"{model}-fallback"
            content = _build_fallback_chat_response(raw_messages, llm_error)

        return JSONResponse(
            content=_build_openai_response(
                request_id=f"chatcmpl-{ctx.run_id}",
                model=model,
                content=content,
                usage=usage,
            )
        )
    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error in openai_chat_completions: {e}")
        raise HTTPException(status_code=400, detail="Invalid JSON format")
    except Exception as e:
        logger.error(f"Error in openai_chat_completions: {e}", exc_info=True)
        return _build_openai_error(str(e), 500, "internal_error", "103002")
    finally:
        cozeloop.flush()


@app.post("/submit")
async def http_submit(request: Request) -> Dict[str, Any]:
    """
    非阻塞任务提交端点（供 Spring Boot AgentApiClient 使用）。

    接收格式：
      { "taskId": "TASK-xxx", "workflowId": 1, "callbackUrl": "http://...", "params": {...} }

    立即返回 runId，后台异步执行 RPA 工作流，完成后 POST callbackUrl 通知 Spring Boot。
    """
    try:
        payload = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON body")

    task_id: str = payload.get("taskId", "")
    callback_url: str = payload.get("callbackUrl", "")
    run_id: str = str(uuid.uuid4())

    logger.info(f"[submit] 接收任务: taskId={task_id}, runId={run_id}, callbackUrl={callback_url}")

    async def _run_workflow():
        from graphs.graph import graph as rpa_graph

        initial_state = {
            "task_id": task_id,
            "workflow_id": payload.get("workflowId"),
            "callback_url": callback_url,
            "params": payload.get("params") or {},
            "run_id": run_id,
            "raw_content": "",
            "analysis": "",
            "summary": "",
            "key_points": [],
            "status": "",
            "error_message": "",
            "result": {},
        }
        try:
            logger.info(f"[submit] 工作流开始执行: taskId={task_id}")
            await rpa_graph.ainvoke(initial_state)
            logger.info(f"[submit] 工作流执行完成: taskId={task_id}")
        except Exception as ex:
            logger.error(f"[submit] 工作流异常: taskId={task_id}, error={ex}", exc_info=True)
            if callback_url:
                try:
                    async with httpx.AsyncClient(timeout=10) as client:
                        await client.post(
                            callback_url,
                            json={
                                "taskId": task_id,
                                "runId": run_id,
                                "status": "failed",
                                "errorMessage": f"Agent 内部异常：{ex}",
                            },
                        )
                except Exception as cb_ex:
                    logger.error(f"[submit] 兜底回调也失败: {cb_ex}")

    asyncio.create_task(_run_workflow())

    return {
        "runId": run_id,
        "status": "accepted",
        "message": f"任务 {task_id} 已接受，正在执行中",
    }


@app.get("/health")
async def health_check():
    try:
        # 这里可以添加更多的健康检查逻辑
        return {
            "status": "ok",
            "message": "Service is running",
        }
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))


@app.get(path="/graph_parameter")
async def http_graph_inout_parameter(request: Request):
    return service.graph_inout_schema()

def parse_args():
    parser = argparse.ArgumentParser(description="Start FastAPI server")
    parser.add_argument("-m", type=str, default="http", help="Run mode, support http,flow,node")
    parser.add_argument("-n", type=str, default="", help="Node ID for single node run")
    parser.add_argument("-p", type=int, default=5000, help="HTTP server port")
    parser.add_argument("-i", type=str, default="", help="Input JSON string for flow/node mode")
    return parser.parse_args()


def parse_input(input_str: str) -> Dict[str, Any]:
    """Parse input string, support both JSON string and plain text"""
    if not input_str:
        return {"text": "你好"}

    # Try to parse as JSON first
    try:
        return json.loads(input_str)
    except json.JSONDecodeError:
        # If not valid JSON, treat as plain text
        return {"text": input_str}

def start_http_server(port):
    workers = 1
    reload = False
    if graph_helper.is_dev_env():
        reload = True

    logger.info(f"Start HTTP Server, Port: {port}, Workers: {workers}")
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=reload, workers=workers)

if __name__ == "__main__":
    args = parse_args()
    if args.m == "http":
        start_http_server(args.p)
    elif args.m == "flow":
        payload = parse_input(args.i)
        result = asyncio.run(service.run(payload))
        print(json.dumps(result, ensure_ascii=False, indent=2))
    elif args.m == "node" and args.n:
        payload = parse_input(args.i)
        result = asyncio.run(service.run_node(args.n, payload))
        print(json.dumps(result, ensure_ascii=False, indent=2))
    elif args.m == "agent":
        agent_ctx = new_context(method="agent")
        for chunk in service.stream(
                {
                    "type": "query",
                    "session_id": "1",
                    "message": "你好",
                    "content": {
                        "query": {
                            "prompt": [
                                {
                                    "type": "text",
                                    "content": {"text": "现在几点了？请调用工具获取当前时间"},
                                }
                            ]
                        }
                    },
                },
                run_config={"configurable": {"session_id": "1"}},
                ctx=agent_ctx,
        ):
            print(chunk)
