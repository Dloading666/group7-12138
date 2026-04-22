"""
摘要生成节点（async）
"""
import json
import logging
import os
import re

from graphs.state import RPAWorkflowState

logger = logging.getLogger(__name__)

SUMMARY_SYSTEM_PROMPT = """你是一个结构化摘要专家。
请基于提供的分析报告，生成精炼的结构化摘要。

严格输出 JSON，不要附加额外说明，格式如下：
{
  "one_sentence": "不超过 80 字的核心总结",
  "key_points": ["要点 1", "要点 2", "要点 3"],
  "conclusion": "不超过 200 字的整体结论"
}"""

MAX_ANALYSIS_CHARS = 6_000


def _env_first(*names: str, default: str = "") -> str:
    for name in names:
        value = os.getenv(name)
        if value is not None and str(value).strip() != "":
            return str(value).strip()
    return default


def _extract_json(text: str) -> dict:
    text = re.sub(r"```json\s*", "", text)
    text = re.sub(r"```\s*", "", text)
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        matched = re.search(r"\{.*\}", text, re.DOTALL)
        if matched:
            try:
                return json.loads(matched.group())
            except Exception:
                pass
    return {}


async def generate_summary_node(state: RPAWorkflowState) -> dict:
    if state.get("error_message"):
        logger.info("[summary] 前置节点出错，跳过摘要")
        return {}

    analysis = state.get("analysis", "")
    raw_content = state.get("raw_content", "")
    api_key = _env_first("LLM_API_KEY", "OPENAI_API_KEY", default="")

    if not api_key or api_key in ("your-api-key-here", "sk-xxx"):
        logger.warning("[summary] 未配置 LLM，返回模拟摘要")
        mock_result = {
            "summary": "工作流执行完成（模拟摘要，请配置 LLM_API_KEY）",
            "key_points": [
                "数据获取完成",
                "分析流程已执行",
                "配置 LLM 后可生成真实摘要",
            ],
            "conclusion": "当前结果来自本地兜底逻辑，适合验证流程链路，不建议作为正式分析结论。",
            "analysis": analysis,
            "data_length": len(raw_content),
        }
        return {
            "summary": mock_result["summary"],
            "key_points": mock_result["key_points"],
            "result": mock_result,
            "error_message": "",
        }

    try:
        from langchain_openai import ChatOpenAI
        from langchain_core.messages import HumanMessage, SystemMessage

        base_url = _env_first("LLM_BASE_URL", "OPENAI_BASE_URL", default="https://api.minimaxi.com/v1")
        default_headers = {}
        if "openrouter" in base_url:
            default_headers = {
                "HTTP-Referer": "https://github.com/rpa-management",
                "X-Title": "RPA Management Platform",
            }

        llm = ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=_env_first("LLM_MODEL", "OPENAI_MODEL", default="MiniMax-M2.7"),
            temperature=0.1,
            max_retries=2,
            default_headers=default_headers,
        )
        messages = [
            SystemMessage(content=SUMMARY_SYSTEM_PROMPT),
            HumanMessage(content=f"请对以下分析报告生成结构化摘要：\n\n{analysis[:MAX_ANALYSIS_CHARS]}"),
        ]
        response = await llm.ainvoke(messages)
        parsed = _extract_json(response.content)

        if not parsed:
            logger.warning("[summary] JSON 解析失败，降级使用原始文本")
            parsed = {"one_sentence": response.content[:200], "key_points": [], "conclusion": ""}

        final_result = {
            "summary": parsed.get("one_sentence", ""),
            "key_points": parsed.get("key_points", []),
            "conclusion": parsed.get("conclusion", ""),
            "analysis": analysis,
            "data_length": len(raw_content),
        }
        return {
            "summary": final_result["summary"],
            "key_points": final_result["key_points"],
            "result": final_result,
            "error_message": "",
        }
    except Exception as exc:
        logger.error("[summary] 摘要生成失败: %s", exc)
        fallback_result = {
            "summary": "摘要生成失败，请查看 analysis 字段",
            "key_points": [],
            "conclusion": "",
            "analysis": analysis,
            "data_length": len(raw_content),
        }
        return {
            "summary": fallback_result["summary"],
            "key_points": [],
            "result": fallback_result,
            "error_message": "",
        }
