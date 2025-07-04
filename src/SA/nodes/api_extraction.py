import json
from pathlib import Path
from typing import List, Dict, Any
from pocketflow import Node
from SA.tools.code_slicer import slice_code
from Util.logger import get_logger

from ..core.models import APIInfo, HTTPRequest
from ..tools.java_parser import parse_java_code


class APIExtractionNode(Node):
    """API提取节点 - 从项目中提取API信息"""
    def __init__(self):
        super().__init__()
        self.logger = get_logger("SA.APIExtraction")
        
    def prep(self, shared: Dict[str, Any]):
        project_info = shared["project_info"]
        project_path = project_info.root_path
        session_dir = shared["session_dir"]

        if "Java" in str(project_info.project_type):
            self.parser_func = parse_java_code
            self.logger.info(f"Found parser for {project_info.project_type}: {self.parser_func.__name__}")
        else:
            self.logger.warning(f"Unsupported project type: {project_info.project_type}")
            raise ValueError
        
        return project_path, session_dir
    
    def exec(self, prep_res: tuple[Path, Path]):
        project_path, session_dir = prep_res
        tool_result = self.parser_func(str(project_path), str(session_dir))
        if tool_result['status'] != 0:
            self.logger.warning(f"No APIs extracted from {project_path}")
        return tool_result
    
    def post(self, shared, prep_res, exec_res):
        api_dir = shared["session_dir"] / "data" / "API"
        deserialized_apis = []

        if not api_dir.exists() or not api_dir.is_dir():
            self.logger.warning(f"API output directory not found: {api_dir}")
            shared["apis"] = []
            return

        for api_file in api_dir.glob("*.json"):
            with open(api_file, "r", encoding="utf-8") as f:
                try:
                    controller_data = json.load(f)
                    for api_dict in controller_data.get("apis", []):
                        req_dict = api_dict.pop("req", {})
                        http_request = HTTPRequest(**req_dict)
                        api_info = APIInfo(req=http_request, **api_dict)
                        deserialized_apis.append(api_info)
                except json.JSONDecodeError:
                    self.logger.error(f"Failed to decode JSON from {api_file}")
                    continue
        
        self.logger.info(f"Loaded {len(deserialized_apis)} API objects from {len(list(api_dir.glob('*.json')))} files.")
        shared["apis"] = deserialized_apis
        
        # for api in deserialized_apis:
            # self.logger.info(f"API: {api.controller_name}.{api.method_name} {api.code_pos}")
            # self.logger.info(f"{api.req}")
            # self.logger.info("\n"+slice_code(shared["project_path"], api.code_pos))
