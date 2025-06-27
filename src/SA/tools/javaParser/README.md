# 基于AST的Java代码解析工具

输入java项目的根目录，使用javaparser库递归的分析每个java代码文件，分别实现以下功能：

## 已实现功能

### 1. API提取
针对spring框架的绝大部分常用注解，进行模式匹配，提取所有api，输出api的定义，请求参数格式，controller函数的源代码等。

```bash
java -jar parser.jar <project-path> api [-o <output-file>]
```

### 2. 引用查找 (Find Reference)
输入特定符号，查找代码中所有引用，输出引用处列表的代码位置和上下文信息。

```bash
# 查找所有名为 "methodName" 的引用
java -jar parser.jar <project-path> reference -s <symbol> [-o <output-file>]

# 查找特定位置定义的符号的所有引用
java -jar parser.jar <project-path> reference -s <symbol> -f <file> -l <line> [-o <output-file>]
```

### 3. 调用链分析 (Call Graph)
分析项目中所有方法的调用关系，构建完整的调用图。

```bash
java -jar parser.jar <project-path> callgraph [-o <output-file>]
```

## 待实现功能

### 符号解析 (Go to Definition)
输入特定符号（包括代码位置），解析其定义，输出符号定义的源代码

## 编译和运行

```bash
# 编译项目
mvn clean package

# 运行
java -jar target/javaParser-1.0-jar-with-dependencies.jar <command>
```