from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
from .models import APIInfo, SymbolInfo, ProjectInfo


class Parser(ABC):
    """代码解析器接口"""
    
    @abstractmethod
    def parse_project(self, project_path: str) -> ProjectInfo:
        """解析整个项目，返回项目信息"""
        pass
    
    @abstractmethod
    def extract_apis(self, file_path: str) -> List[APIInfo]:
        """从文件中提取API信息"""
        pass


class SymbolResolver(ABC):
    """符号解析器接口"""
    
    @abstractmethod
    def resolve_symbol(self, symbol_name: str, file_path: str, line: int) -> Optional[SymbolInfo]:
        """解析指定符号的定义"""
        pass
    
    @abstractmethod
    def find_imports(self, file_path: str) -> List[str]:
        """查找文件的所有导入"""
        pass


class ContextExtender(ABC):
    """上下文扩展器接口"""
    
    @abstractmethod
    def extend_api_context(self, api_info: APIInfo) -> Dict[str, Any]:
        """扩展API的上下文信息"""
        pass
    
    @abstractmethod
    def analyze_dependencies(self, api_info: APIInfo) -> List[SymbolInfo]:
        """分析API的依赖关系"""
        pass 