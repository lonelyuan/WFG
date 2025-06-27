import subprocess
import os
from pathlib import Path
from Util.logger import get_logger

logger = get_logger("JavaParser")

JAVA_PATH = os.getenv("JAVA_PATH")

JAVA_PARSER_PATH = "./javaParser/target/javaParser-1.0-jar-with-dependencies.jar"
JAVA_PARSER_PATH = Path(__file__).parent / JAVA_PARSER_PATH

def parse_java_code(project_path: str, output_path: str) -> dict:
    """
    调用jar包，解析java代码
    """
    if not JAVA_PATH:
        raise ValueError("JAVA_PATH is not set")
    if not JAVA_PARSER_PATH:
        raise ValueError("JAVA_PARSER_PATH is not set")    
    cmd = [JAVA_PATH, "-jar", str(JAVA_PARSER_PATH), project_path, "-o", output_path]
    logger.info(f"cmd: {cmd}")
    result = subprocess.run(cmd, capture_output=True)
    if result.returncode != 0:
        error_message = result.stderr.decode('utf-8', errors='ignore')
        logger.error(f"java_parser failed: {error_message}")
    
    stdout_message = result.stdout.decode('utf-8', errors='ignore')
    stderr_message = result.stderr.decode('utf-8', errors='ignore')
    return {"cmd": cmd, "status": result.returncode, "stdout": stdout_message, "stderr": stderr_message}




