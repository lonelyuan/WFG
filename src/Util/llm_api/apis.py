from openai import OpenAI
from typing import List, Dict, Any, Optional
from Util.logger import get_logger
import time
from pathlib import Path
import os

logger = get_logger("Util")

DEFAULT_MODEL = "qwen"
DEFAULT_TEMPERATURE = 0.0
DEFAULT_SYSTEM_ROLE = "You are a experienced programmer and good at understanding programs."

def call_qwen(message: str, temperature: float = DEFAULT_TEMPERATURE, system_role: str = DEFAULT_SYSTEM_ROLE) -> str:
    """使用Qwen模型推理"""
    model_input: List[Dict[str, Any]] = [
        {"role": "system", "content": system_role},
        {"role": "user", "content": message}
    ]

    client = OpenAI(
        api_key="none",
        base_url="http://10.46.156.37:6342/v1",
        # timeout=30.0,
        # max_retries=3,
    )
    
    response = client.chat.completions.create(
        model="tq-gpt-qwen2.5-coder-32b",
        messages=model_input,  # type: ignore
        # temperature=temperature,
    )
    return response.choices[0].message.content or ""


def call_deepseek(message: str, model_name: str = "deepseek-r1", temperature: float = DEFAULT_TEMPERATURE, 
                        system_role: str = DEFAULT_SYSTEM_ROLE) -> str:
    """使用DeepSeek模型推理"""
    api_key = "eaf2d4a3-5097-454b-9e94-e1eb950e4e30"
    model_input = [
        {"role": "system", "content": system_role},
        {"role": "user", "content": message}
    ]
    client = OpenAI(
        api_key=api_key, 
        base_url="https://ark.cn-beijing.volces.com/api/v3"
    )
    response = client.chat.completions.create(
        model=model_name,
        messages=model_input,  # type: ignore
        temperature=temperature,
    )
    return response.choices[0].message.content or ""
    

def call_llm(message: str, 
             model: str = DEFAULT_MODEL,
             temperature: float = DEFAULT_TEMPERATURE,
             system_role: str = DEFAULT_SYSTEM_ROLE,
             log_dir: Optional[Path] = None,
             measure_cost: bool = False) -> str:
    logger.info(f"Calling LLM: {model}")

    if log_dir:
        os.makedirs(log_dir, exist_ok=True)
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        log_file_path = log_dir / f"{timestamp}_{model}.log"
        
        with open(log_file_path, "w", encoding="utf-8") as f:
            f.write("--- PROMPT ---\n")
            f.write(f"User: {message}\n\n")

    try:
        if "qwen" in model.lower():
            output = call_qwen(message, temperature, system_role)
        elif "deepseek" in model.lower():
            output = call_deepseek(message, model, temperature, system_role)
        else:
            raise ValueError(f"Unsupported model: {model}")
        if measure_cost: # 简单估算token数量（实际项目使用tiktoken）
            input_tokens = len(system_role.split()) + len(message.split())
            output_tokens = len(output.split())
            logger.info(f"Token consumption - input: {input_tokens}, output: {output_tokens}")
        
        if log_dir:
            with open(log_file_path, "a", encoding="utf-8") as f:
                f.write("--- RESPONSE ---\n")
                f.write(output)
        
        return output
        
    except Exception as e:
        logger.error(f"LLM call failed: {str(e)}")
        raise e