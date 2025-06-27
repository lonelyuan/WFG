from typing import Dict, Any, List
from pocketflow import Node
from Util.logger import get_logger

from SA.core.models import APIInfo, AnalysisResult, SummaryResult
from Util.config import PromptCrafter
from Util.llm_api import call_llm


class SummaryGenerationNode(Node):
    """摘要生成节点 - 生成最终的分析报告"""
    def __init__(self):
        super().__init__()
        self.logger = get_logger("SA.SummaryGeneration")
        
    def prep(self, shared: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "apis": shared.get("apis", []),
            "api_analysis": shared.get("api_analysis", []),
            "project_type": shared.get("project_type"),
            "project_path": shared.get("project_path")
        }
    
    def exec(self, prep_data: Dict[str, Any]):
        apis = prep_data["apis"]
        analysis_results = prep_data["api_analysis"]
        
        self.logger.info(f"生成分析摘要 - API总数: {len(apis)}")