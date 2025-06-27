from re import S
from typing import List, Dict, Any
from pocketflow import Node

from SA.core.models import APIInfo, AnalysisResult
from Util.config import PromptCrafter
from Util.llm_api import call_llm
from Util.logger import get_logger
from ..tools.code_slicer import slice_code


class APIAnalysisNode(Node):
    """API分析节点 - 使用LLM分析API"""
    def __init__(self):
        super().__init__()
        self.logger = get_logger("SA.APIAnalysis")
        
    def prep(self, shared: Dict[str, Any]) -> tuple[List[APIInfo], str]:
        apis = shared["apis"]
        self.logger.info(f"Preparing to analyze {len(apis)} APIs")
        return apis, shared["project_path"]
    
    def exec(self, prep_res: tuple[List[APIInfo], str]) -> List[AnalysisResult]:
        apis, project_path = prep_res
        prompt_template = PromptCrafter("APIAnalysis/input_format.json", "meta_prompts")
        analysis_results = []
        for i, api in enumerate(apis):
            self.logger.info(f"API {i+1}/{len(apis)}: {api.req.path}")
            prompt_params = {
                "SRC_NAME":api.req.path,
                "SRC_LINE":api.code_pos,
                "FUNCTION":slice_code(project_path,api.code_pos),
                "SINK_VALUES":api.req.body,
                "CALL_STATEMENTS":api.req.query_params,
            }
            prompt = prompt_template.craft_prompt(prompt_params)
            self.logger.debug(f"prompt: {prompt}")
            analysis_text = call_llm(prompt, model="deepseek", temperature=0.0)
            result = AnalysisResult(
                api_info=api,
                analysis=analysis_text,
                confidence=0.8,  # 默认置信度
                suggestions=[]  # 可以进一步解析LLM输出获取建议
            )
            analysis_results.append(result)
                
        self.logger.info(f"API分析完成，成功分析 {len(analysis_results)} 个API")
        return analysis_results