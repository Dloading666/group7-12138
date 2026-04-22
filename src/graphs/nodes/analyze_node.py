"""
AI 分析节点（async）
"""
import logging
import os

from graphs.state import RPAWorkflowState

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是一个专业的数据分析助手，服务于 RPA 自动化管理平台。
请对提供的内容进行深入分析，关注以下维度：
1. 核心主题与关键信息
2. 重要数据、事实、数字
3. 内在规律或趋势（如有）
4. 可供决策参考的洞察

请使用中文回答，结构清晰，层次分明。"""

MAX_INPUT_CHARS = 8_000


def _env_first(*names: str, default: str = "") -> str:
    for name in names:
        value = os.getenv(name)
        if value is not None and str(value).strip() != "":
            return str(value).strip()
    return default


def _get_llm():
    api_key = _env_first("LLM_API_KEY", "OPENAI_API_KEY", default="")
    if not api_key or api_key in ("your-api-key-here", "sk-xxx"):
        return None

    try:
        from langchain_openai import ChatOpenAI

        base_url = _env_first("LLM_BASE_URL", "OPENAI_BASE_URL", default="https://api.minimaxi.com/v1")
        default_headers = {}
        if "openrouter" in base_url:
            default_headers = {
                "HTTP-Referer": "https://github.com/rpa-management",
                "X-Title": "RPA Management Platform",
            }

        return ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=_env_first("LLM_MODEL", "OPENAI_MODEL", default="MiniMax-M2.7"),
            temperature=0.3,
            max_retries=2,
            default_headers=default_headers,
        )
    except Exception as exc:
        logger.error("[analyze] 初始化 LLM 失败: %s", exc)
        return None


async def analyze_data_node(state: RPAWorkflowState) -> dict:
    if state.get("error_message"):
        logger.info("[analyze] 前置节点出错，跳过分析")
        return {}

    raw_content = state.get("raw_content", "")
    params = state.get("params") or {}
    query = params.get("query", "请对以下内容进行全面分析")
    task_type = params.get("type", "analysis")

    type_hint = {
        "summary": "请重点关注内容的核心要点，输出简洁分析",
        "qa": f"请重点回答以下问题：{query}",
        "extract": "请提取内容中的结构化信息，例如人名、地点、时间、金额等",
        "analysis": "请对内容进行全面深度分析",
    }.get(task_type, "请对内容进行全面深度分析")

    llm = _get_llm()
    if llm is None:
        logger.warning("[analyze] 未配置 LLM，返回模拟分析结果")
        mock = (
            "【模拟分析结果，请配置 LLM_API_KEY 以启用真实分析】\n\n"
            f"任务类型：{task_type}\n"
            f"内容长度：{len(raw_content)} 字符\n"
            f"分析维度：{type_hint}\n\n"
            f"内容摘录（前 200 字）：\n{raw_content[:200]}..."
        )
        return {"analysis": mock}

    try:
        from langchain_core.messages import HumanMessage, SystemMessage

        content_slice = raw_content[:MAX_INPUT_CHARS]
        messages = [
            SystemMessage(content=SYSTEM_PROMPT),
            HumanMessage(content=f"【分析要求】{type_hint}\n\n【待分析内容】\n{content_slice}"),
        ]
        logger.info("[analyze] 调用 LLM 分析，内容长度=%s", len(content_slice))
        response = await llm.ainvoke(messages)
        return {"analysis": response.content, "error_message": ""}
    except Exception as exc:
        logger.error("[analyze] AI 分析调用失败: %s", exc)
        return {"analysis": "", "error_message": f"AI 分析调用失败：{exc}"}
