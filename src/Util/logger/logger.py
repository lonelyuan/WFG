"""
WFG统一日志管理系统
基于loguru实现，支持会话级别的日志和数据存储
"""

import sys
import os
import json
from pathlib import Path
from datetime import datetime
from loguru import logger
from typing import Optional, Dict, Any, Union

def format_file_path(record):
    cwd = os.getcwd()
    file_path = record["file"].path
    try:
        rel_path = os.path.relpath(file_path, cwd)
        if len(rel_path) < len(file_path):
            file_path = rel_path
    except ValueError:
        pass
    path_with_line = f"{file_path}:{record['line']}"
    return f"{path_with_line:<40}"

class SessionManager:
    """会话管理器 - 负责每次运行的独立存储"""
    
    def __init__(self, session_id: Optional[str] = None, module_name: str = "WFG"):
        self.session_id = session_id or self._generate_session_id()
        self.module_name = module_name
        
        # 创建会话目录结构
        self.base_logs_dir = Path(__file__).parent.parent.parent.parent / "logs"
        self.session_dir = self.base_logs_dir / self.session_id
        self.logs_dir = self.session_dir / "logs"
        self.data_dir = self.session_dir / "data"
        
        # 确保目录存在
        self.logs_dir.mkdir(parents=True, exist_ok=True)
        self.data_dir.mkdir(parents=True, exist_ok=True)
        
        # 节点计数器
        self.node_counter = 0
    
    def _generate_session_id(self) -> str:
        """生成会话ID"""
        return datetime.now().strftime("%Y%m%d_%H%M%S")
    
    def get_next_node_id(self) -> str:
        """获取下一个节点ID"""
        self.node_counter += 1
        return f"node_{self.node_counter:03d}"
    
    def save_shared_data(self, node_name: str, shared_data: Dict[str, Any], 
                        node_id: Optional[str] = None) -> None:
        """保存共享数据快照"""
        if node_id is None:
            node_id = self.get_next_node_id()
        
        timestamp = datetime.now().strftime("%H%M%S")
        filename = f"{node_id}_{node_name}_{timestamp}.json"
        filepath = self.data_dir / filename
        
        # 序列化共享数据
        serializable_data = self._make_serializable(shared_data)
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump({
                    "timestamp": datetime.now().isoformat(),
                    "node_name": node_name,
                    "node_id": node_id,
                    "data": serializable_data
                }, f, indent=2, ensure_ascii=False)
            
            logger.info(f"共享数据已保存: {filename}")
        except Exception as e:
            logger.error(f"保存共享数据失败: {e}")
    
    def _make_serializable(self, obj: Any, _seen=None) -> Any:
        """将对象转换为可序列化格式，避免循环引用"""
        if _seen is None:
            _seen = set()
        
        # 避免循环引用
        obj_id = id(obj)
        if obj_id in _seen:
            return f"<循环引用: {type(obj).__name__}>"
        
        if isinstance(obj, dict):
            _seen.add(obj_id)
            result = {k: self._make_serializable(v, _seen) for k, v in obj.items()}
            _seen.remove(obj_id)
            return result
        elif isinstance(obj, (list, tuple)):
            _seen.add(obj_id)
            result = [self._make_serializable(item, _seen) for item in obj]
            _seen.remove(obj_id)
            return result
        elif hasattr(obj, '__dict__'):
            _seen.add(obj_id)
            # 对象转换为字典
            result = {
                "_type": obj.__class__.__name__,
                "_module": obj.__class__.__module__,
                **{k: self._make_serializable(v, _seen) for k, v in obj.__dict__.items() 
                   if not k.startswith('_')}  # 忽略私有属性
            }
            _seen.remove(obj_id)
            return result
        elif isinstance(obj, (str, int, float, bool, type(None))):
            return obj
        elif hasattr(obj, '__str__'):
            return {
                "_type": obj.__class__.__name__,
                "_repr": str(obj)[:200]  # 限制字符串长度
            }
        else:
            return str(obj)[:200]  # 限制字符串长度

class WFGLogger:
    """WFG统一日志管理器"""
    
    def __init__(self, session_id: Optional[str] = None, module_name: str = "WFG", verbose: bool = False):
        self.session_manager = SessionManager(session_id, module_name)
        self.module_name = module_name
        self.verbose = verbose
        self.setup_logger()
    
    @property
    def session_id(self) -> str:
        return self.session_manager.session_id
    
    @property
    def session_dir(self) -> Path:
        return self.session_manager.session_dir
    
    def setup_logger(self):
        logger.remove()
        # 控制台输出
        logger.add(
            sys.stderr,
            format="<green>{time:HH:mm:ss}</green> | <blue>{extra[formatted_path]}</blue> | <level>{message}</level>",
            level="INFO",
            colorize=True,
            filter=lambda record: record.update({"extra": {**record["extra"], "formatted_path": format_file_path(record)}}) or True
        )
        
        # 详细日志文件 - 仅在verbose模式下启用
        if self.verbose:
            detailed_log = self.session_manager.logs_dir / f"{self.module_name.lower()}_detailed.log"
            logger.add(
                detailed_log,
                format="{time:YYYY-MM-DD HH:mm:ss.SSS} {level.icon} {extra[formatted_path]} | {message}",
                level="DEBUG",
                rotation="10 MB",
                filter=lambda record: record.update({"extra": {**record["extra"], "formatted_path": format_file_path(record)}}) or True
            )
        
        # 错误日志 - 始终保留
        error_log = self.session_manager.base_logs_dir / "errors.log"
        logger.add(
            error_log,
            format="{time:YYYY-MM-DD HH:mm:ss.SSS} {level.icon} {extra[formatted_path]} | {message}",
            level="WARNING",
            backtrace=True,
            diagnose=True,
            filter=lambda record: record.update({"extra": {**record["extra"], "formatted_path": format_file_path(record)}}) or True
        )
        
        # 创建会话信息文件
        session_info = {
            "session_id": self.session_id,
            "module": self.module_name,
            "start_time": datetime.now().isoformat(),
            "logs_dir": str(self.session_manager.logs_dir),
            "data_dir": str(self.session_manager.data_dir)
        }
        
        session_file = self.session_manager.session_dir / "session_info.json"
        with open(session_file, 'w', encoding='utf-8') as f:
            json.dump(session_info, f, indent=2, ensure_ascii=False)
        logger.bind(module=self.module_name).info(f"Initialized logger in session: {self.session_dir}")
    
    def get_logger(self, component: str = ""):
        """获取带组件名称的logger"""
        module_path = f"{self.module_name}.{component}" if component else self.module_name
        return logger.bind(module=module_path)
    
    def save_shared_data(self, node_name: str, shared_data: Dict[str, Any]) -> None:
        """保存共享数据快照"""
        self.session_manager.save_shared_data(node_name, shared_data)


_wfg_logger = None

def init_logger(module_name: str = "WFG", session_id: Optional[str] = None, verbose: bool = False) -> WFGLogger:
    """初始化日志系统"""
    global _wfg_logger
    _wfg_logger = WFGLogger(session_id, module_name, verbose)
    return _wfg_logger

def get_logger(component: str = ""):
    """获取logger实例"""
    global _wfg_logger
    if _wfg_logger is None:
        _wfg_logger = init_logger()
    return _wfg_logger.get_logger(component)

def save_shared_data(node_name: str, shared_data: Dict[str, Any]) -> None:
    """保存共享数据快照的便利函数"""
    global _wfg_logger
    if _wfg_logger is not None:
        _wfg_logger.save_shared_data(node_name, shared_data)

def get_session_dir() -> Optional[Path]:
    """获取当前会话目录"""
    global _wfg_logger
    if _wfg_logger is not None:
        return _wfg_logger.session_dir
    return None 