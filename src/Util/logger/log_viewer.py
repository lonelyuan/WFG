#!/usr/bin/env python3
"""
WFG日志查看器
支持查看会话级别的日志和数据
"""

import os
import sys
import json
import argparse
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Any


class LogViewer:
    """日志查看器"""
    
    def __init__(self, logs_base_dir: str | None = None):
        if logs_base_dir is None:
            self.logs_base_dir = Path(__file__).parent.parent.parent / "logs"
        else:
            self.logs_base_dir = Path(logs_base_dir)
    
    def list_sessions(self) -> List[Dict[str, Any]]:
        """列出所有会话"""
        sessions = []
        
        if not self.logs_base_dir.exists():
            return sessions
        
        for session_dir in self.logs_base_dir.iterdir():
            if session_dir.is_dir() and session_dir.name != "errors.log":
                session_info_file = session_dir / "session_info.json"
                
                if session_info_file.exists():
                    try:
                        with open(session_info_file, 'r', encoding='utf-8') as f:
                            session_info = json.load(f)
                        sessions.append({
                            "session_id": session_info["session_id"],
                            "module": session_info["module"],
                            "start_time": session_info["start_time"],
                            "session_dir": str(session_dir)
                        })
                    except Exception as e:
                        print(f"读取会话信息失败 {session_dir}: {e}")
                else:
                    # 兼容旧格式 - 从目录名推断
                    sessions.append({
                        "session_id": session_dir.name,
                        "module": "Unknown",
                        "start_time": "Unknown",
                        "session_dir": str(session_dir)
                    })
        
        # 按时间排序（最新的在前）
        sessions.sort(key=lambda x: x["start_time"], reverse=True)
        return sessions
    
    def view_session_logs(self, session_id: str, tail_lines: int = 50):
        """查看指定会话的日志"""
        session_dir = self.logs_base_dir / session_id
        
        if not session_dir.exists():
            print(f"会话不存在: {session_id}")
            return
        
        logs_dir = session_dir / "logs"
        if not logs_dir.exists():
            print(f"会话日志目录不存在: {logs_dir}")
            return
        
        # 查找日志文件
        log_files = list(logs_dir.glob("*.log"))
        
        if not log_files:
            print(f"会话中未找到日志文件: {logs_dir}")
            return
        
        print(f"会话 {session_id} 的日志:")
        print("=" * 80)
        
        for log_file in log_files:
            print(f"\n>>> {log_file.name} <<<")
            try:
                with open(log_file, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
                    
                if tail_lines > 0:
                    lines = lines[-tail_lines:]
                    if len(lines) == tail_lines:
                        print(f"... (显示最后 {tail_lines} 行)")
                
                for line in lines:
                    print(line.rstrip())
                    
            except Exception as e:
                print(f"读取日志文件失败: {e}")
    
    def view_session_data(self, session_id: str):
        """查看指定会话的数据"""
        session_dir = self.logs_base_dir / session_id
        
        if not session_dir.exists():
            print(f"会话不存在: {session_id}")
            return
        
        data_dir = session_dir / "data"
        if not data_dir.exists():
            print(f"会话数据目录不存在: {data_dir}")
            return
        
        # 查找数据文件
        data_files = sorted(list(data_dir.glob("*.json")))
        
        if not data_files:
            print(f"会话中未找到数据文件: {data_dir}")
            return
        
        print(f"会话 {session_id} 的数据文件:")
        print("=" * 80)
        
        for data_file in data_files:
            print(f"\n>>> {data_file.name} <<<")
            try:
                with open(data_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                
                # 简化显示
                print(f"时间戳: {data.get('timestamp', 'Unknown')}")
                print(f"节点名称: {data.get('node_name', 'Unknown')}")
                print(f"节点ID: {data.get('node_id', 'Unknown')}")
                
                # 显示数据概要
                shared_data = data.get('data', {})
                print(f"数据键: {list(shared_data.keys())}")
                
                # 显示部分重要数据
                if 'apis' in shared_data and shared_data['apis']:
                    print(f"API数量: {len(shared_data['apis'])}")
                
                if 'final_summary' in shared_data and shared_data['final_summary']:
                    summary = shared_data['final_summary']
                    if isinstance(summary, dict):
                        print(f"最终摘要: API总数={summary.get('total_apis', 0)}")
                    
            except Exception as e:
                print(f"读取数据文件失败: {e}")
    
    def view_latest_session(self, tail_lines: int = 50):
        """查看最新会话"""
        sessions = self.list_sessions()
        
        if not sessions:
            print("未找到任何会话")
            return
        
        latest_session = sessions[0]
        print(f"最新会话: {latest_session['session_id']}")
        self.view_session_logs(latest_session['session_id'], tail_lines)


def main():
    parser = argparse.ArgumentParser(description="WFG日志查看器")
    parser.add_argument("--logs-dir", type=str, help="日志基础目录")
    parser.add_argument("--list", "-l", action="store_true", help="列出所有会话")
    parser.add_argument("--session", "-s", type=str, help="查看指定会话的日志")
    parser.add_argument("--data", "-d", type=str, help="查看指定会话的数据")
    parser.add_argument("--latest", action="store_true", help="查看最新会话")
    parser.add_argument("--tail", "-t", type=int, default=50, help="显示最后N行 (默认50行，0表示全部)")
    
    args = parser.parse_args()
    
    viewer = LogViewer(args.logs_dir)
    
    if args.list:
        sessions = viewer.list_sessions()
        if sessions:
            print("可用会话:")
            print("-" * 80)
            for session in sessions:
                print(f"会话ID: {session['session_id']}")
                print(f"模块: {session['module']}")
                print(f"开始时间: {session['start_time']}")
                print(f"目录: {session['session_dir']}")
                print("-" * 40)
        else:
            print("未找到任何会话")
    
    elif args.session:
        viewer.view_session_logs(args.session, args.tail)
    
    elif args.data:
        viewer.view_session_data(args.data)
    
    elif args.latest:
        viewer.view_latest_session(args.tail)
    
    else:
        parser.print_help()


if __name__ == "__main__":
    main() 