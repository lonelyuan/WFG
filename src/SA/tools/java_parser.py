import subprocess
import os
import json
from pathlib import Path
from typing import List, Dict, Optional
from Util.logger import get_logger

logger = get_logger("JavaParser")

JAVA_PATH = os.getenv("JAVA_PATH", "java")  # 默认使用系统路径中的java
JAVA_PARSER_PATH = "./javaParser/target/javaParser-1.0-jar-with-dependencies.jar"
JAVA_PARSER_PATH = Path(__file__).parent / JAVA_PARSER_PATH

def _run_java_parser(cmd: List[str]) -> dict:
    """运行 java parser 命令的通用方法"""
    logger.info(f"Running command: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    
    if result.returncode != 0:
        error_message = result.stderr
        logger.error(f"JavaParser failed: {error_message}")
    
    return {
        "cmd": cmd,
        "status": result.returncode,
        "stdout": result.stdout,
        "stderr": result.stderr
    }

def extract_apis(project_path: str, output_dir: Optional[str] = None) -> dict:
    """提取 Spring API 信息"""
    cmd = [JAVA_PATH, "-jar", str(JAVA_PARSER_PATH), project_path, "API"]
    if output_dir:
        cmd.extend(["-o", output_dir])
    
    return _run_java_parser(cmd)

def find_references(project_path: str, symbol_name: str, 
                   target_file: Optional[str] = None, 
                   target_line: Optional[int] = None,
                   output_path: Optional[str] = None) -> dict:
    """查找符号引用"""
    cmd = [JAVA_PATH, "-jar", str(JAVA_PARSER_PATH), project_path, "REF", "-s", symbol_name]
    
    if target_file:
        cmd.extend(["-f", target_file])
    if target_line is not None:
        cmd.extend(["-l", str(target_line)])
    if output_path:
        cmd.extend(["-o", output_path])
    
    return _run_java_parser(cmd)

def build_call_graph(project_path: str, output_path: Optional[str] = None, 
                     image_path: Optional[str] = None, layout: str = "DOT",
                     class_filter: Optional[str] = None, keep_isolated: bool = False,
                     no_image: bool = False) -> dict:
    """构建方法调用图并生成可视化
    
    Args:
        project_path: Java项目路径
        output_path: JSON输出文件路径
        image_path: 图片输出文件路径（支持.svg、.png、.pdf格式）
        layout: 布局算法 (DOT: 层次化布局, NEATO: 弹簧力布局)
        class_filter: 类名过滤器（逗号分隔）
        keep_isolated: 是否保留孤立节点
        no_image: 是否跳过图片生成
    """
    cmd = [JAVA_PATH, "-jar", str(JAVA_PARSER_PATH), project_path, "CG"]
    
    if output_path:
        cmd.extend(["-o", output_path])
    if image_path:
        cmd.extend(["-img", image_path])
    if layout and layout != "DOT":
        cmd.extend(["-layout", layout])
    if class_filter:
        cmd.extend(["-filter", class_filter])
    if keep_isolated:
        cmd.append("-keep-isolated")
    if no_image:
        cmd.append("-no-image")
    
    return _run_java_parser(cmd)

def find_definitions(project_path: str, symbol_name: str, output_path: Optional[str] = None) -> dict:
    """查找符号定义"""
    cmd = [JAVA_PATH, "-jar", str(JAVA_PARSER_PATH), project_path, "DEF", "-s", symbol_name]
    
    if output_path:
        cmd.extend(["-o", output_path])
    
    return _run_java_parser(cmd)

def parse_java_code(project_path: str, output_dir: str) -> dict:
    """向后兼容的 API 提取方法"""
    return extract_apis(project_path, output_dir)




