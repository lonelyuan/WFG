import argparse
import os
import sys
from pathlib import Path
from Util.logger import init_logger, get_logger
from dotenv import load_dotenv

load_dotenv(dotenv_path="config.env")

def run_SA(project_path: str, **kwargs):
    """运行SA静态分析模块"""
    from SA.flow import run_sa_analysis
    logger = get_logger("Main")

    result = run_sa_analysis(project_path)

    if result["success"]:
        logger.info(f"SA analysis completed! Session directory: {result['session_dir']}")
        return True
    else:
        logger.warning(f"SA analysis failed: {result['error']}")
        return False

def run_DF(project_path: str, **kwargs):
    """运行DF模糊测试模块 - 占位符"""
    print(f"DF模糊测试模块 - 项目: {project_path}")
    print("功能待实现...")
    return True


def run_Gen(project_path: str, **kwargs):
    """运行脚本生成模块 - 占位符"""
    print(f"脚本生成模块 - 项目: {project_path}")
    print("功能待实现...")
    return True

def run_all(project_path: str, **kwargs):
    """运行所有模块"""
    print(f"运行所有模块 - 项目: {project_path}")
    print("功能待实现...")
    return True


def create_parser():
    parser = argparse.ArgumentParser(
        prog="WFG",
        description="WebFuzzGen: 基于LLM的Web应用模糊测试框架",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=""" python main.py --project /path/to/project """
    )
    
    # 全局参数
    parser.add_argument("--project", "-p", type=str, required=True, help="项目路径")
    parser.add_argument("--output", "-o", type=str, help="输出文件路径")
    parser.add_argument("--verbose", "-v", action="store_true", help="保存详细日志到文件")
    parser.add_argument("--config", type=str, help="配置文件路径")
     
    # 子命令
    subparsers = parser.add_subparsers(dest="command", help="可用的功能模块")
    # SA静态分析阶段
    sa_parser = subparsers.add_parser("sa", help="静态代码分析")
    # DF动态测试阶段
    df_parser = subparsers.add_parser("df", help="动态测试")
    # Gen脚本生成阶段
    gen_parser = subparsers.add_parser("gen", help="代码生成")
    # 所有阶段，默认执行all
    all_parser = subparsers.add_parser("all", help="所有阶段")
    return parser


def validate_project_path(project_path: str) -> bool:
    if not os.path.exists(project_path):
        print(f"错误: 项目路径不存在: {project_path}")
        return False
    if not os.path.isdir(project_path):
        print(f"错误: 路径不是目录: {project_path}")
        return False
    return True


def main():
    parser = create_parser()
    args = parser.parse_args()
    if args.command is None:
        args.command = "all"    
    init_logger("WFG", verbose=args.verbose)
    if hasattr(args, 'project') and args.project:
        if not validate_project_path(args.project):
            return 1
    success = False
    if args.command == "sa":
        success = run_SA(
            project_path=args.project,
            output=args.output,
        )
    elif args.command == "df":
        success = run_DF(
            project_path=args.project,
        )
    elif args.command == "gen":
        success = run_Gen(
            project_path=args.project,
        )
    elif args.command == "all":
        success = run_all(
            project_path=args.project,
        )
    else:
        parser.print_help()
        return 1
    
    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())