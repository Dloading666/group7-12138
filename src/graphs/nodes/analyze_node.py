"""
AI 分析节点（async）
使用 LangChain + OpenAI 对原始内容进行深度分析。
支持通过环境变量切换模型和 API 地址（可对接 DeepSeek / 本地 Ollama 等兼容接口）。
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

请使用中文回复，结构清晰，层次分明。"""

MAX_INPUT_CHARS = 8_000   # 送给 LLM 的内容上限（避免超出 token 限制）


def _get_llm():
    """根据环境变量创建 LLM 实例，返回 None 表示未配置"""
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key or api_key in ("your-api-key-here", "sk-xxx"):
        return None

    try:
        from langchain_openai import ChatOpenAI
        base_url = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")
        # OpenRouter 需要额外请求头，其他服务忽略这些头不影响
        default_headers = {}
        if "openrouter" in base_url:
            default_headers = {
                "HTTP-Referer": "https://github.com/rpa-management",
                "X-Title": "RPA Management Platform",
            }
        return ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
            temperature=0.3,
            max_retries=2,
            default_headers=default_headers,
        )
    except Exception as e:
        logger.error(f"[analyze] 初始化 LLM 失败: {e}")
        return None


async def analyze_data_node(state: RPAWorkflowState) -> dict:
    """
    对 raw_content 进行 AI 分析，将结果写入 state.analysis。
    若前置节点已报错（error_message 非空），直接跳过本节点。
    """
    if state.get("error_message"):
        logger.info("[analyze] 前置节点出错，跳过分析")
        return {}

    raw_content = state.get("raw_content", "")
    params = state.get("params") or {}
    query: str = params.get("query", "请对以下内容进行全面分析")
    task_type: str = params.get("type", "analysis")

    # 根据任务类型调整 prompt
    type_hint = {
        "summary":  "请重点关注内容的核心要点，输出简洁的分析",
        "qa":       f"请重点回答以下问题：{query}",
        "extract":  "请提取内容中的结构化信息（人名、地点、时间、金额等）",
        "analysis": "请对内容进行全面深度分析",
    }.get(task_type, "请对内容进行全面深度分析")

    llm = _get_llm()

    # ── 无 LLM 配置时返回模拟结果 ─────────────────────────────────────
    if llm is None:
        logger.warning("[analyze] OPENAI_API_KEY 未配置，使用模拟分析结果")
        mock = (
            f"【模拟分析结果 - 请配置 OPENAI_API_KEY 以启用真实 AI 分析】\n\n"
            f"任务类型：{task_type}\n"
            f"内容长度：{len(raw_content)} 字符\n"
            f"分析维度：{type_hint}\n\n"
            f"内容摘录（前200字）：\n{raw_content[:200]}..."
        )
        return {"analysis": mock}

    # ── 调用 LLM（async）─────────────────────────────────────────────
    try:
        from langchain_core.messages import HumanMessage, SystemMessage

        content_slice = raw_content[:MAX_INPUT_CHARS]
        messages = [
            SystemMessage(content=SYSTEM_PROMPT),
            HumanMessage(content=(
                f"【分析要求】{type_hint}\n\n"
                f"【待分析内容】\n{content_slice}"
            )),
        ]
        logger.info(f"[analyze] 调用 LLM 分析，内容长度: {len(content_slice)}")
        resp = await llm.ainvoke(messages)
        logger.info("[analyze] LLM 分析完成")
        return {"analysis": resp.content, "error_message": ""}

    except Exception as e:
        msg = f"AI 分析调用失败：{e}"
        logger.error(f"[analyze] {msg}")
        return {"analysis": "", "error_message": msg}
