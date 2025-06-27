from pocketflow import Flow
from Util.logger import get_logger, get_session_dir
from .core.models import ProjectInfo
from .nodes import (
    FrameWorkAnalysisNode,
    APIExtractionNode, 
    ContextExtentionNode,
    SummaryGenerationNode
)


def create_sa_flow() -> Flow:
    # 创建节点实例
    framework_analysis = FrameWorkAnalysisNode()
    api_extraction = APIExtractionNode()
    context_extention = ContextExtentionNode()
    summary_generation = SummaryGenerationNode()
    # 构建流程图
    f1 = framework_analysis >> api_extraction >> context_extention >> summary_generation
    f2 = summary_generation - "needs_more_info" >> context_extention
    # 创建流程
    flow = Flow(start=framework_analysis)
    return flow


def run_sa_analysis(project_path: str) -> dict:
    logger = get_logger("SA.Flow")
    session_dir = get_session_dir()
    logger.info(f"Starting SA analysis for {project_path} at {session_dir}".center(50, "="))
    project_info = ProjectInfo(root_path=project_path, project_type="", files=[])
    shared = {
        "project_info": project_info,
        "apis": [],
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
        
