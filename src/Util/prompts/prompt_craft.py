import json
from pathlib import Path
from typing import Dict, Any, Optional
from Util.logger import get_logger

class PromptCrafter:
    """提示词配置管理器"""
    def __init__(self, template_path, prompt_type):
        self.logger = get_logger("Util.PromptCrafter")
        self.template_path = template_path
        with open(Path(__file__).parent / self.template_path, 'r', encoding='utf-8') as f:
            if prompt_type == "all":
                self.templates = json.load(f)
            else:
                self.templates = json.load(f)[prompt_type]

         
    def craft_prompt(self, params: dict) -> str:
        if "user_prompt" in self.templates and isinstance(self.templates["user_prompt"], list):
            template = "\n".join(self.templates["user_prompt"])
        elif "user_prompt" in self.templates:
             template = json.dumps(self.templates["user_prompt"], ensure_ascii=False)
        else: # Fallback for old format
            if isinstance(self.templates, list):
                template = "\n".join(self.templates)
            else:
                template = json.dumps(self.templates, ensure_ascii=False)

        for key, value in params.items():
            template = template.replace(f"<{key}>", str(value))
        return template