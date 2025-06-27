import sys
import os
from Util.llm_api import call_llm

def test_api():
    message = "写一个简单的Python函数来计算斐波那契数列"
    try:
        response = call_llm(message, model="qwen", temperature=0.0)
        print("响应内容:", response)
        print("测试完成！")
    except Exception as e:
        print(f"测试失败: {e}")

if __name__ == "__main__":
    """
    uv run python -m Util.testapi
    """
    test_api() 
