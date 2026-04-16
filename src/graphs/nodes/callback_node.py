"""
回调节点（async）
这是工作流的最后一个节点，无论成功还是失败都会执行。
负责将执行结果 POST 到 Spring Boot 的 /api/agent/callback 接口。
"""
import logging
import httpx

from graphs.state import RPAWorkflowState

logger = logging.getLogger(__name__)

CALLBACK_TIMEOUT = 15   # 回调请求超时秒数
MAX_RETRY = 3           # 回调失败最大重试次数


async def _do_post(url: str, payload: dict) -> bool:
    """带重试的异步 HTTP POST，返回是否成功"""
    for attempt in range(1, MAX_RETRY + 1):
        try:
            async with httpx.AsyncClient(timeout=CALLBACK_TIMEOUT) as client:
                resp = await client.post(url, json=payload)
                if resp.status_code < 500:          # 2xx/4xx 均视为"送达"
                    logger.info(f"[callback] 回调成功 (attempt {attempt}): {resp.status_code}")
                    return True
                logger.warning(f"[callback] 服务端错误 {resp.status_code}，重试 {attempt}/{MAX_RETRY}")
        except Exception as e:
            logger.warning(f"[callback] 回调失败 (attempt {attempt}/{MAX_RETRY}): {e}")
    return False


async def send_callback_node(state: RPAWorkflowState) -> dict:
    """
    统一回调节点：
    - error_message 为空 → 发送 completed 回调
    - error_message 非空 → 发送 failed 回调
    回调失败只记录日志，不抛异常（任务状态在 Agent 侧已确定）
    """
    task_id = state.get("task_id", "")
    run_id = state.get("run_id", "")
    callback_url = state.get("callback_url", "")
    error_message = state.get("error_message", "")

    if not callback_url:
        logger.warning(f"[callback] task_id={task_id} 未提供 callbackUrl，跳过回调")
        return {"status": "failed" if error_message else "completed"}

    if error_message:
        # ── 失败回调 ────────────────────────────────────────────────────
        payload = {
            "taskId": task_id,
            "runId": run_id,
            "status": "failed",
            "errorMessage": error_message,
        }
        logger.info(f"[callback] 发送失败回调: taskId={task_id}, error={error_message[:100]}")
        await _do_post(callback_url, payload)
        return {"status": "failed"}
    else:
        # ── 成功回调 ────────────────────────────────────────────────────
        result = state.get("result") or {}
        payload = {
            "taskId": task_id,
            "runId": run_id,
            "status": "completed",
            "result": result,
        }
        logger.info(
            f"[callback] 发送成功回调: taskId={task_id}, "
            f"summary={str(result.get('summary', ''))[:80]}"
        )
        await _do_post(callback_url, payload)
        return {"status": "completed"}
