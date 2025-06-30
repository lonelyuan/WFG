## 代码理解Agent

- core: 数据结构定义
- nodes: 节点定义，在flow.py中组装流程
  - FrameworkAnalysis: 框架分析节点 - 解析项目框架类型等元信息
    - 文档摘要子节点 / TODO HeuristicExt: 启发式拓展子节点 - 基于框架特定的开发模式拓展
  - APIExtraction: API提取节点 - 从项目中提取API列表
    - TODO GrammerExt: 语法拓展子节点 - 针对指定符号（类，函数，变量）搜索其定义 - 针对api函数内部的符号进行拓展。
  - ContextExtention: 上下文扩展节点 - 获取API相关的更多代码上下文
    - TODO ReflectExt: 语义拓展子节点 - 让LLM反问需要获取哪些符号的信息
  - SummaryGeneration: 摘要生成节点 - 生成API摘要
- tools 驱动层 - 第三方工具的驱动
  - code_slicer.py: 代码切片工具，根据行号返回源代码
  - JavaParser: java的AST解析工具
    - APIExtraction: 分析注解提取API
    - GoToDefinition: 搜索给定符号的定义
    - FindReference: 搜索指定符号的引用
    - TODO CallGraph: 生成项目的调用图
  - TODO Treesitter集成 - 其他语言的ast工具
