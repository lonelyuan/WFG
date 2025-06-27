from re import S
from typing import List, Dict, Any
from pocketflow import Node

from SA.core.models import APIInfo, AnalysisResult, AnalysisStatus, ProjectInfo
from Util.prompts.prompt_craft import PromptCrafter
from Util.llm_api import call_llm
from Util.logger import get_logger
from ..tools.code_slicer import slice_code


class ContextExtentionNode(Node):
    """上下文扩展节点 - 获取API相关的更多信息"""
    def __init__(self):
        super().__init__()
        self.logger = get_logger("SA.ContextExtention")
        
    def prep(self, shared: Dict[str, Any]) -> tuple[List[APIInfo], str]:
        apis = shared["apis"]
        self.logger.info(f"Preparing to analyze {len(apis)} APIs")
        return apis, shared["project_info"]
    
    def exec(self, prep_res: tuple[List[APIInfo], ProjectInfo]) -> List[AnalysisResult]:
        apis,  project_info = prep_res
        prompt_template = PromptCrafter("SA/cn.json", "all")
        analysis_results = []
        for i, api in enumerate(apis):
            self.logger.info(f"API {i+1}/{len(apis)}: {api.req.path}")
            result = AnalysisResult(
                api_info=api,
                analysis="",
                status=AnalysisStatus.INCOMPLETE
            )
            prompt_params = {
                "PROJECT_TYPE":project_info.project_type,
                "FUNCTION":slice_code(project_info.root_path,api.code_pos),
                # "CONTEXT_INFO":project_info.files,
            }
            prompt = prompt_template.craft_prompt(prompt_params)
            self.logger.info(f"prompt: {prompt}")
            llm_res = call_llm(prompt, model="qwen", temperature=0.0)
            self.logger.info(f"llm_res: {llm_res}")
            result.analysis = llm_res
            analysis_results.append(result)
                
        self.logger.info(f"API分析完成，成功分析 {len(analysis_results)} 个API")
        return analysis_results

    def post(self, shared, prep_res, exec_res) -> List[AnalysisResult]:
        
        return exec_res