"""
摘要生成节点（async）
对 analyze_node 的输出进行结构化压缩，生成：
  - 一句话总结
  - 3-5 个关键要点
  - 整体结论
输出为严格 JSON 以便后续节点直接使用。
"""
import json
import logging
import os
import re

from graphs.state import RPAWorkflowState

logger = logging.getLogger(__name__)

SUMMARY_SYSTEM_PROMPT = """你是一个结构化摘要专家。
请基于提供的分析报告，生成精炼的结构化摘要。

【输出要求】
严格输出 JSON，格式如下，不要有任何额外说明：
{
  "one_sentence": "不超过50字的核心总结",
  "key_points": ["要点1（不超过30字）", "要点2", "要点3"],
  "conclusion": "不超过100字的整体结论"
}"""

MAX_ANALYSIS_CHARS = 6_000


def _extract_json(text: str) -> dict:
    """从 LLM 输出中提取 JSON 对象（容忍 markdown 代码块包裹）"""
    # 去掉 ```json ... ``` 包裹
    text = re.sub(r"```json\s*", "", text)
    text = re.sub(r"```\s*", "", text)
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        # 尝试从文本中正则提取第一个 { ... }
        m = re.search(r"\{.*\}", text, re.DOTALL)
        if m:
            try:
                return json.loads(m.group())
            except Exception:
                pass
    return {}


async def generate_summary_node(state: RPAWorkflowState) -> dict:
    """
    将分析结果压缩为结构化摘要，同时组装最终 result 对象。
    若前置节点已报错，直接跳过。
    """
    if state.get("error_message"):
        logger.info("[summary] 前置节点出错，跳过摘要生成")
        return {}

    analysis = state.get("analysis", "")
    raw_content = state.get("raw_content", "")

    api_key = os.getenv("OPENAI_API_KEY", "").strip()

    # ── 无 LLM 配置时返回模拟摘要 ─────────────────────────────────────
    if not api_key or api_key in ("your-api-key-here", "sk-xxx"):
        logger.warning("[summary] OPENAI_API_KEY 未配置，使用模拟摘要")
        mock_result = {
            "summary": "工作流执行成功（模拟摘要，请配置 OPENAI_API_KEY）",
            "key_points": [
                "数据获取成功",
                "分析流程完整执行",
                "请配置 API 密钥获取真实摘要",
            ],
            "conclusion": "RPA AI 工作流已成功运行，等待 LLM 配置后可获取真实分析结果",
            "analysis": analysis,
            "data_length": len(raw_content),
        }
        return {
            "summary": mock_result["summary"],
            "key_points": mock_result["key_points"],
            "result": mock_result,
            "error_message": "",
        }

    # ── 调用 LLM 生成摘要（async）─────────────────────────────────────
    try:
        from langchain_openai import ChatOpenAI
        from langchain_core.messages import HumanMessage, SystemMessage

        base_url = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")
        default_headers = {}
        if "openrouter" in base_url:
            default_headers = {
                "HTTP-Referer": "https://github.com/rpa-management",
                "X-Title": "RPA Management Platform",
            }
        llm = ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
            temperature=0.1,
            max_retries=2,
            default_headers=default_headers,
        )
        messages = [
            SystemMessage(content=SUMMARY_SYSTEM_PROMPT),
            HumanMessage(content=f"请对以下分析报告生成结构化摘要：\n\n{analysis[:MAX_ANALYSIS_CHARS]}"),
        ]
        logger.info("[summary] 调用 LLM 生成摘要")
        resp = await llm.ainvoke(messages)
        parsed = _extract_json(resp.content)

        if not parsed:
            # JSON 解析失败，降级为原始文本
            logger.warning("[summary] JSON 解析失败，使用原始文本作为摘要")
            parsed = {"one_sentence": resp.content[:200], "key_points": [], "conclusion": ""}

        final_result = {
            "summary": parsed.get("one_sentence", ""),
            "key_points": parsed.get("key_points", []),
            "conclusion": parsed.get("conclusion", ""),
            "analysis": analysis,
            "data_length": len(raw_content),
        }
        logger.info("[summary] 摘要生成完成")
        return {
            "summary": final_result["summary"],
            "key_points": final_result["key_points"],
            "result": final_result,
            "error_message": "",
        }

    except Exception as e:
        msg = f"摘要生成失败：{e}"
        logger.error(f"[summary] {msg}")
        # 摘要失败不是致命错误，用分析结果兜底
        fallback_result = {
            "summary": "摘要生成失败，详见 analysis 字段",
            "key_points": [],
            "conclusion": "",
            "analysis": analysis,
            "data_length": len(raw_content),
        }
        return {
            "summary": fallback_result["summary"],
            "key_points": [],
            "result": fallback_result,
            "error_message": "",   # 摘要失败不阻断成功回调
        }
