"""
RPA 工作流状态定义
在 LangGraph 中，State 是贯穿所有节点的共享数据结构
"""
from typing import TypedDict, Optional, List


class RPAWorkflowState(TypedDict):
    # ── 输入（由 Spring Boot 提交时携带） ──────────────────────────────
    task_id: str            # Spring Boot 任务编号，如 TASK-20240101-001
    workflow_id: Optional[int]  # 工作流定义ID
    callback_url: str       # 执行完毕后回调 Spring Boot 的地址
    params: dict            # 任务参数：url / content / query / type 等
    run_id: str             # 本次 Agent 运行ID（UUID）

    # ── 处理中间状态 ────────────────────────────────────────────────────
    raw_content: str        # 从 URL 抓取或直接使用的原始文本
    analysis: str           # LLM 对内容的分析报告
    summary: str            # 一句话摘要
    key_points: List[str]   # 关键要点列表

    # ── 输出结果 ─────────────────────────────────────────────────────────
    status: str             # "completed" | "failed"
    error_message: str      # 失败时的错误信息
    result: dict            # 最终回调给 Spring Boot 的结果对象
