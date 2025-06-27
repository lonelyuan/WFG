# WFG - WebFuzz生成框架

WebFuzzGen 是基于测试脚本生成的通用WebFuzz框架。

## 项目流程

### 静态分析 (SA)

```mermaid
flowchart TD
    subgraph SA[静态分析]
        A[框架分析] --> B[API提取]
        B --> C[上下文扩展]
        C --> D[分析总结]
        D -- "信息不足" --> C
    end
```

## 运行方法

### 环境配置

本项目使用 `uv` 进行环境和依赖管理。

### 启动命令

```bash
uv run python src/main.py -p <your_project_path> sa
```
