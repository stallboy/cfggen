---
layout: home
title: MCP 服务器
nav_order: 3
---

# MCP 服务器

MCP（Model Context Protocol）服务器为 cfggen 项目提供了符合MCP规范的HTTP服务器，通过标准化的工具和资源接口访问配置数据。

[View it on GitHub][cfggen repo]{: .btn .fs-5 .mb-4 .mb-md-0 }

## 主要功能

* **符合MCP规范**：实现标准化的工具和资源接口
* **HTTP服务器**：基于Java HttpServer实现，支持跨域请求
* **JSON响应**：所有响应使用JSON格式
* **类型安全**：基于Schema的类型系统
* **错误处理**：完整的错误处理和验证机制

## 快速开始

### 启动MCP服务器

```bash
java -jar cfggen.jar -datadir config -gen mcpserver
```

### 可选参数

- `port`：服务器端口，默认3000
- `aicfg`：AI配置文件路径

```bash
java -jar cfggen.jar -datadir config -gen mcpserver port=8080
```

## 接口文档

- [工具接口](tools.md) - MCP标准工具调用接口
- [资源接口](resources.md) - 表数据资源访问接口

## 使用示例

### 获取工具列表

```bash
curl http://localhost:3000/tools/list
```

### 查询表结构

```bash
curl -X POST http://localhost:3000/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "schema_query",
    "arguments": {
      "table": "user"
    }
  }'
```

### 读取表数据资源

```bash
curl "http://localhost:3000/resources/read?table://user"
```

[cfggen repo]: https://github.com/stallboy/cfggen