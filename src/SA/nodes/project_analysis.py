from pathlib import Path
from typing import Dict, Any
from pocketflow import Node
from Util.logger import get_logger
from ..core.models import ProjectInfo, FileInfo
from ..tools.project_detect import detect_project_type

def scan_directory(base_path: Path):
    """扫描目录并输出文本文件的绝对路径和行数"""
    res = []
    for path in base_path.rglob('*'):
        if any(part.startswith('.') for part in path.parts):
            continue
        if path.is_file():
            size = path.stat().st_size
            res.append(FileInfo(path=str(path), size=size, file_type=path.suffix))
    return res

class ProjectAnalysisNode(Node):
    """项目分析节点 - 解析项目结构和元信息"""
    def __init__(self):
        super().__init__()
        self.logger = get_logger("SA.ProjectAnalysis")
        
    def prep(self, shared: Dict[str, Any]) -> str:
        project_path = shared["project_path"]
        self.logger.info(f"Preparing to analyze project: {project_path}")
        return project_path
    
    def exec(self, project_path: str) -> ProjectInfo:
        """
        实现文件夹解析，提取以下信息：
        - 框架类型：生态-框架。
        - 文件分布：每份代码文件的后缀、行数
        """
        framework_type = detect_project_type(project_path)
        if len(framework_type.items()) > 1:
            self.logger.warning(f"Multiple framework types detected: {framework_type}")
        else:
            self.logger.info(f"Framework type: {framework_type}")
        
        file_list = scan_directory(Path(project_path))
        self.logger.info(f"File list: {len(file_list)}")
        res = ProjectInfo(
            project_type=list(framework_type.keys())[0],
            root_path=project_path,
            files=file_list
        )
        return res

    def post(self, shared, prep_res, exec_res):
        shared["project_info"] = exec_res