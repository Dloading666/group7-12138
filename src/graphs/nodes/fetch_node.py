"""
数据获取节点（async）
"""
import logging
import httpx
from bs4 import BeautifulSoup

from graphs.state import RPAWorkflowState

logger = logging.getLogger(__name__)
MAX_CONTENT_CHARS = 50_000


def _extract_text(html: str) -> str:
    try:
        soup = BeautifulSoup(html, "html.parser")
        for tag in soup(["script", "style", "nav", "footer", "header"]):
            tag.decompose()
        return soup.get_text(separator="\n", strip=True)
    except Exception:
        return html


async def fetch_data_node(state: RPAWorkflowState) -> dict:
    params = state.get("params") or {}
    url: str = params.get("url", "").strip()
    content: str = params.get("content", "").strip()

    if url:
        logger.info(f"[fetch] 开始抓取 URL: {url}")
        try:
            headers = {"User-Agent": "Mozilla/5.0 (compatible; RPA-Agent/1.0)"}
            async with httpx.AsyncClient(timeout=30, follow_redirects=True, headers=headers) as client:
                resp = await client.get(url)
                resp.raise_for_status()
                raw = _extract_text(resp.text)[:MAX_CONTENT_CHARS]
                logger.info(f"[fetch] 抓取成功，内容长度: {len(raw)} 字符")
                return {"raw_content": raw, "error_message": ""}
        except Exception as e:
            msg = f"抓取 URL 失败：{e}"
            logger.error(f"[fetch] {msg}")
            return {"raw_content": "", "error_message": msg}

    if content:
        logger.info(f"[fetch] 使用传入内容，长度: {len(content)} 字符")
        return {"raw_content": content[:MAX_CONTENT_CHARS], "error_message": ""}

    import json
    fallback = json.dumps(params, ensure_ascii=False, indent=2)
    logger.warning("[fetch] 未提供 url 或 content，以 params 作为分析对象")
    return {"raw_content": fallback, "error_message": ""}
