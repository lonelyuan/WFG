# 基于AST的Java代码解析工具


输入java项目的根目录，使用javaparser库递归的分析每个java代码文件，分别实现以下功能：

- api提取：
针对spring框架的绝大部分常用注解，进行模式匹配，提取所有api，输出api的定义，请求参数格式，controller函数的源代码等。输出api列表到指定json文件中（具体参数格式参加java_parser.py）

- 符号解析：go to defination
输入特定符号（包括代码位置），解析其定义，输出符号定义的源代码

- 引用查找：find reference
输入特定符号（包括代码位置），查找代码中所有引用，输出引用处列表的代码位置

- 调用链分析：GC
输入特定符号，输出调用关系。