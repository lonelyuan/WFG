## 代码理解Agent

#### 需求
解析java源代码项目，提取所有api，提取每个api相关的信息，输出为api摘要列表。基于这些信息能够生成对应api的测试代码。

#### 技术栈

使用pocketflow库实现agent，依照其规范实现节点/工具等。
使用treesitter作为通用的ast解析模块，在./lib中安装完成。
【将来计划使用JavaParser替换，保留接口】

#### 项目架构

驱动层：第三方工具的驱动
- Treesitter集成
- JavaParser集成

核心层：定义工具接口
- Parser接口 (策略模式)
- SymbolResolver接口：查找特定符号的实现
- ContextExtender接口

实现层nodes：具体的工具实现 
- RegexParser / ASTParser
- LocalSymbolResolver / ImportResolver
- FrameworkContextExtender / SemanticExtender

调度层：实例化分析过程，调用LLM
- flow.py：

配置层：
- FrameworkConfig (注解配置、规则配置)
- PromptConfig (LLM提示词配置)

#### 开发计划
子模块：（以文件为单位，存在列出几种可选的方案）
- 解析器
  - 项目预处理：
    输出：项目类型，文件列表（文件格式，大小）
  - api提取：匹配@RequestMapping注解，进而获得整个函数
    实现方法：
    1. 基于正则匹配
    2. 基于AST解析
  - 语法/符号拓展：针对指定符号（类，函数，变量）搜索其定义
    输入：符号和代码位置
    输出：符号定义处的源代码+代码位置
    实现方法：
    - 区分内部符号/外部符号，外部则搜索import
    - 处理别名等【方案待补充】
  - 语义/启发式拓展：
    - 基于框架特定的开发模式拓展：（如：注解，拦截器等，配置文件）
    - 基于符号名搜索的拓展
  - LLM反思：让LLM提问，还需要哪些符号的信息
- 整合器：按需调用解析器
  整体流程伪代码：
    第1步. 预处理：源代码 -> 框架信息 -> 框架api模式 -> api函数
    第2步. API提取：框架api模式 -> api函数
    foreach API:
        第3步. 上下文拓展：向agent询问以下问题
           - 问题1：该API需要的输入格式是如何
           - 问题2：该API能够操作哪些状态源
           - 问题3：该API的调用了哪些潜在的危险函数，能够造成什么类型的漏洞
        第4步. 摘要生成：如果当前上下文无法满足生成，则继续拓展信息
           输出：
           - API定义；
           - 输入格式；
           - sink点相关；
           - 状态相关：登录
