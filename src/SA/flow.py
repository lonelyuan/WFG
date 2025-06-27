"""
SA阶段流程编排
"""

from pathlib import Path
from pocketflow import Flow
from regex import P
from Util.logger import get_logger, get_session_dir

from .nodes import (
    ProjectAnalysisNode,
    APIExtractionNode, 
    ContextExtentionNode,
    SummaryGenerationNode
)


def create_sa_flow() -> Flow:
    # 创建节点实例
    project_analysis = ProjectAnalysisNode()
    api_extraction = APIExtractionNode()
    context_extention = ContextExtentionNode()
    summary_generation = SummaryGenerationNode()
    # 构建流程图
    flow = project_analysis >> api_extraction >> context_extention >> summary_generation
    # 创建流程
    flow = Flow(start=project_analysis)
    return flow


def run_sa_analysis(project_path: str) -> dict:
    """
    运行SA静态分析
    
    Args:
        project_path: 项目路径
        
    Returns:
        dict: 分析结果
    """
    logger = get_logger("SA.Flow")
    session_dir = get_session_dir()
    logger.info(f"Starting SA analysis for {project_path} at {session_dir}".center(50, "="))
    shared = {
        "project_path": str(Path(project_path).absolute()),
        "project_info": None,
        "apis": [],
        "api_count": 0,
        "api_analysis": [],
        "final_summary": None,
        "session_dir": session_dir
    }    
    flow = create_sa_flow()
    result_action = flow.run(shared)

    logger.info("SA analysis summary:")
    if shared.get("final_summary"):
        summary = shared["final_summary"]
        logger.info(f"API number: {summary.total_apis}")
    else:
        logger.warning("No summary generated")            
    
    return {
        "success": True,
        "session_id": session_dir.name if session_dir else "unknown",
        "session_dir": str(session_dir) if session_dir else "unknown",
        "shared_data": shared,
        "final_action": result_action
    }
        
