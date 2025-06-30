# 基于AST的Java代码解析工具

输入java项目的根目录，使用javaparser库递归的分析每个java代码文件，分别实现以下功能：

## 已实现功能

### 1. API提取与符号分析
针对Spring框架Controller类，提取API信息并进行完整的符号分析，输出包含API定义、符号表和上下文引用的增强分析结果。

```bash
java -jar parser.jar <project-path> API [-o <output-dir>]
```

输出格式：
```json
{ "controller_name" : "AdminController",
  "file_path" : "src\\main\\java\\ltd\\newbee\\mall\\controller\\admin\\AdminController.java",
  "apis" : [ {
    "controller_name" : "AdminController",
    "method_name" : "login",
    "code_pos" : "src\\main\\java\\ltd\\newbee\\mall\\controller\\admin\\AdminController.java:L37-L40",
    "req" : {
      "method" : "GET",
      "path" : "/login",
      "query_params" : { },
      "body" : { }
    },
    "references" : ["cn.hutool.captcha.ShearCaptcha"（第三方库）,"ltd.newbee.mall.entity.AdminUser"（应用内其他包）,"ltd.newbee.mall.controller.admin.AdminController.adminUserService"（类内部成员）]
```

### 2. 引用查找 (Find Reference)
输入特定符号，查找代码中所有引用位置和上下文信息。

```bash
# 查找所有名为符号的引用
java -jar parser.jar <project-path> REF -s <symbol> [-o <output-file>]

# 查找特定位置定义的符号的所有引用
java -jar parser.jar <project-path> REF -s <symbol> -f <file> -l <line> [-o <output-file>]
```

### 3. 调用链分析 (Call Graph)
分析项目中所有方法的调用关系，构建完整的调用图并支持可视化。

```bash
java -jar parser.jar <project-path> CG [-o <output-file>] [-img <image-file>] [-layout <engine>] [-filter <classes>] [-keep-isolated] [-no-image]
```

### 4. 符号定义查找 (Go to Definition)
输入特定符号，查找其定义位置和源代码。

```bash
java -jar parser.jar <project-path> DEF -s <symbol> [-o <output-file>]
```

## 编译和运行

```bash
# 编译项目
mvn clean package

# 运行
java -jar target/javaParser-1.0-jar-with-dependencies.jar <command>
```