---
layout: page
title: 快速开始
parent: MCP 服务器
nav_order: 3
---

# 快速开始

本指南将帮助您快速启动和使用MCP服务器。

## 前提条件

- JDK 21 或更高版本
- 已构建的 cfggen.jar 文件
- 有效的配置数据目录

## 步骤1：构建项目

确保您已构建了 cfggen.jar：

```bash
# 在项目根目录执行
genjar.bat  # Windows
# 或
./genjar.sh  # Linux/Mac
```

## 步骤2：准备配置数据

确保您有一个包含有效配置数据的目录，例如 `example/config` 目录。

## 步骤3：启动MCP服务器

```bash
# 在 example 目录中执行
java -jar ../cfggen.jar -datadir config -gen mcpserver
```

服务器启动后，您将看到类似以下的输出：

```
MCP Server started on port 3000
Available tables: [user, item, role, ...]
```

## 步骤4：测试服务器

### 测试工具列表

```bash
curl http://localhost:3000/tools/list
```

您应该看到类似以下的响应：

```json
{
  "tools": [
    {
      "name": "schema_query",
      "description": "查询表结构信息",
      "inputSchema": {
        "type": "object",
        "properties": {
          "table": {
            "type": "string",
            "description": "表名"
          }
        }
      }
    },
    // ... 其他工具
  ]
}
```

### 测试表结构查询

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

### 测试数据查询

```bash
curl -X POST http://localhost:3000/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "data_query",
    "arguments": {
      "table": "user",
      "condition": ""
    }
  }'
```

### 测试资源访问

```bash
# 获取资源列表
curl http://localhost:3000/resources/list

# 读取特定表数据
curl "http://localhost:3000/resources/read?table://user"
```

## 配置选项

### 端口配置

默认端口为3000，您可以通过参数修改：

```bash
java -jar ../cfggen.jar -datadir config -gen mcpserver port=8080
```

### AI配置

如果需要AI功能支持，可以指定AI配置文件：

```bash
java -jar ../cfggen.jar -datadir config -gen mcpserver aicfg=ai.json
```

### 数据过滤

只处理包含特定tag的数据：

```bash
java -jar ../cfggen.jar -datadir config -gen mcpserver own=server
```

## 故障排除

### 服务器无法启动

- 检查端口是否被占用
- 检查配置数据目录是否有效
- 检查 cfggen.jar 文件是否存在

### 工具调用失败

- 检查表名是否正确
- 检查请求格式是否符合JSON规范
- 查看服务器日志获取详细错误信息

### 数据查询无结果

- 检查表是否存在
- 检查数据是否已正确加载
- 使用空条件查询所有数据

## 下一步

- 查看[工具接口](tools.md)了解详细的API使用方法
- 查看[资源接口](resources.md)了解资源访问方式
- 集成到您的AI应用中