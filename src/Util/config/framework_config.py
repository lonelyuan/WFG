import json
from pathlib import Path
from typing import Dict, List, Any, Optional

class FrameworkConfig:
    """框架配置管理器"""
    
    def __init__(self, config_path: Optional[str] = None):
        self.config_path = config_path if config_path is not None else self._get_default_config_path()
        self._load_config()
    
    def _get_default_config_path(self) -> str:
        return str(Path(__file__).parent / "framework_rules.json")
    
    def _load_config(self):
        """加载配置文件"""
        try:
            with open(self.config_path, 'r', encoding='utf-8') as f:
                self.config = json.load(f)
        except FileNotFoundError:
            self.config = self._get_default_config()
    
    def _get_default_config(self) -> Dict[str, Any]:
        """默认配置"""
        return {
            "spring_boot": {
                "api_annotations": ["@RequestMapping", "@GetMapping", "@PostMapping", "@PutMapping", "@DeleteMapping"],
                "controller_annotations": ["@RestController", "@Controller"],
                "patterns": {
                    "request_mapping": r"@RequestMapping\s*\([^)]*\)",
                    "method_mapping": r"@(Get|Post|Put|Delete)Mapping\s*\([^)]*\)"
                }
            },
            "jax_rs": {
                "api_annotations": ["@Path", "@GET", "@POST", "@PUT", "@DELETE"],
                "controller_annotations": ["@Path"],
                "patterns": {
                    "path": r"@Path\s*\([^)]*\)",
                    "method": r"@(GET|POST|PUT|DELETE)"
                }
            }
        }
    
    def get_api_annotations(self, project_type: str) -> List[str]:
        """获取API注解列表"""
        return self.config.get(project_type, {}).get("api_annotations", [])
    
    def get_framework_patterns(self, project_type: str) -> Dict[str, str]:
        """获取正则表达式模式"""
        return self.config.get(project_type, {}).get("framework_patterns", {}) 