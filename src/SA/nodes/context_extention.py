from re import S
from typing import List, Dict, Any
from pocketflow import Node
from concurrent.futures import ThreadPoolExecutor, as_completed
import re
from pathlib import Path

from SA.core.models import APIInfo, AnalysisResult, AnalysisStatus, ProjectInfo
from Util.prompts.prompt_craft import PromptCrafter
from Util.llm_api import call_llm
from Util.logger import get_logger
from ..tools.code_slicer import slice_code
from ..tools.java_parser import find_definitions
import json


class ContextExtentionNode(Node):
    """上下文扩展节点 - 获取API相关的更多信息"""
    def __init__(self):
        super().__init__()
        self.logger = get_logger("SA.ContextExtention")
        
    def prep(self, shared: Dict[str, Any]) -> tuple[List[APIInfo], ProjectInfo, Path]:
        apis = shared["apis"]
        self.logger.info(f"Preparing to analyze {len(apis)} APIs")
        return apis, shared["project_info"], Path(shared["session_dir"])
    
    def _analyze_api(self, api: APIInfo, project_info: ProjectInfo, prompt_template: PromptCrafter, log_dir: Path) -> AnalysisResult:
        self.logger.info(f"Analyzing API: {api.req.path}")
        # --- CONTEXT EXTENTION ---
        symbols_to_find = set(api.references)
        if api.req.body and "type" in api.req.body:
            symbols_to_find.add(api.req.body["type"])
        for param_type in api.req.query_params.values():
            symbols_to_find.add(param_type)

        additional_context = ""
        if symbols_to_find:
            context_parts = []
            for ref in symbols_to_find:
                def_result = find_definitions(str(project_info.root_path), ref)
                if def_result["status"] == 0 and def_result["stdout"]:
                    try:
                        definitions = json.loads(def_result["stdout"])
                        if definitions:
                            definition_code = definitions[0].get("definition_code", "")
                            context_parts.append(f"// Definition for {ref}\n{definition_code}\n")
                    except json.JSONDecodeError:
                        self.logger.warning(f"Could not parse definition for {ref}")
            additional_context = "\n".join(context_parts)
        # --- END CONTEXT EXTENTION ---

        prompt_params = {
            "PROJECT_TYPE": project_info.project_type,
            "FUNCTION": slice_code(project_info.root_path, api.code_pos),
            "CONTEXT_INFO": additional_context
        }
        
        user_prompt = prompt_template.craft_prompt(prompt_params)
        system_prompt = "\n".join(prompt_template.templates["system_prompt"])
        
        llm_res_str = call_llm(user_prompt, model="qwen", temperature=0.0, system_role=system_prompt, log_dir=log_dir)
        
        return AnalysisResult(api_info=api, analysis=llm_res_str, status=AnalysisStatus.INCOMPLETE)

    def exec(self, prep_res: tuple[List[APIInfo], ProjectInfo, Path]) -> List[AnalysisResult]:
        apis, project_info, session_dir = prep_res
        prompt_template = PromptCrafter("SA/cn.json", "all")
        analysis_results = []
        log_dir = session_dir / "logs" / "llm"

        with ThreadPoolExecutor(max_workers=4) as executor:
            futures = [executor.submit(self._analyze_api, api, project_info, prompt_template, log_dir) for api in apis]
            for future in as_completed(futures):
                try:
                    analysis_results.append(future.result())
                    self.logger.info(f"Progress: {len(analysis_results)} / {len(apis)} APIs analyzed")
                except Exception as e:
                    self.logger.error(f"Error analyzing API: {e}")

        # --- 正确率统计 ---
        success_count = 0
        total_count = len(analysis_results)
        if total_count > 0:
            for result in analysis_results:
                try:
                    # Clean the response string from markdown fences
                    clean_res = re.sub(r"```json\n?|```", "", result.analysis).strip()
                    llm_output = json.loads(clean_res)
                    if llm_output.get("status") == "success":
                        success_count += 1
                except (json.JSONDecodeError, AttributeError):
                    self.logger.warning(f"Could not parse LLM output for statistics: {result.analysis}")
            
            success_rate = (success_count / total_count) * 100
            self.logger.info(f"--- Analysis Statistics ---")
            self.logger.info(f"Total APIs analyzed: {total_count}")
            self.logger.info(f"Successful analyses: {success_count}")
            self.logger.info(f"Success Rate: {success_rate:.2f}%")
            self.logger.info(f"--------------------------")
                
        self.logger.info(f"API分析完成，成功分析 {len(analysis_results)} 个API")
        return analysis_results

    def post(self, shared, prep_res, exec_res) -> List[AnalysisResult]:
        shared['analysis_results'] = exec_res
        return exec_res