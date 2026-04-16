"""
RPA AI 工作流图 (LangGraph)
流程：
  fetch_data → analyze_data → generate_summary → send_callback → END

错误传播机制：
  任何节点出错时将 error_message 写入 state，后续节点检测到后跳过自身逻辑，
  最终由 send_callback 节点统一发送成功或失败回调给 Spring Boot。
"""
import logging
from langgraph.graph import StateGraph, END

from graphs.state import RPAWorkflowState
from graphs.nodes.fetch_node import fetch_data_node
from graphs.nodes.analyze_node import analyze_data_node
from graphs.nodes.summary_node import generate_summary_node
from graphs.nodes.callback_node import send_callback_node

logger = logging.getLogger(__name__)


def build_rpa_workflow():
    """构建并编译 RPA AI 工作流图"""
    builder = StateGraph(RPAWorkflowState)

    # ── 注册节点 ────────────────────────────────────────────────────────
    builder.add_node("fetch_data",       fetch_data_node)
    builder.add_node("analyze_data",     analyze_data_node)
    builder.add_node("generate_summary", generate_summary_node)
    builder.add_node("send_callback",    send_callback_node)

    # ── 设置入口 ─────────────────────────────────────────────────────────
    builder.set_entry_point("fetch_data")

    # ── 线性边（错误在节点内部传播，callback 统一处理成功/失败） ──────────
    builder.add_edge("fetch_data",       "analyze_data")
    builder.add_edge("analyze_data",     "generate_summary")
    builder.add_edge("generate_summary", "send_callback")
    builder.add_edge("send_callback",    END)

    compiled = builder.compile()
    logger.info("[graph] RPA 工作流图构建完成")
    return compiled


# ── 模块级单例，供 main.py 和 coze_coding_utils 框架直接导入 ─────────────
graph = build_rpa_workflow()
