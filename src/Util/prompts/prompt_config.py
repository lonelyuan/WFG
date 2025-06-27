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
            self.templates = json.load(f)[prompt_type]
    def craft_prompt(self, params: dict) -> str:
        template = "\n".join(self.templates)
        for key, value in params.items():
            template = template.replace(f"<{key}>", str(value))
        return template