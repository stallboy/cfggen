---
layout: page
title: 工具接口
parent: MCP 服务器
nav_order: 1
---

# 工具接口

MCP服务器提供以下工具用于查询和操作配置数据。

## 可用工具

### 1. schema_query

查询指定表的结构信息。

**参数:**
- `table`：表名（必需）

**示例请求:**
```json
{
  "name": "schema_query",
  "arguments": {
    "table": "user"
  }
}
```

**响应格式:**
```json
{
  "table": "user",
  "comment": "用户表",
  "primaryKey": "id",
  "uniqueKeys": ["username"],
  "fields": [
    {
      "name": "id",
      "type": "int",
      "comment": "用户ID"
    },
    {
      "name": "username",
      "type": "string",
      "comment": "用户名"
    }
  ],
  "foreignKeys": [
    {
      "name": "fk_role",
      "keys": ["role_id"],
      "refTable": "role",
      "refType": "NORMAL",
      "refKeys": ["id"]
    }
  ]
}
```

### 2. data_query

查询指定表的数据。

**参数:**
- `table`：表名（必需）
- `condition`：查询条件（可选，支持简单的字符串匹配）

**示例请求:**
```json
{
  "name": "data_query",
  "arguments": {
    "table": "user",
    "condition": "admin"
  }
}
```

**响应格式:**
```json
{
  "table": "user",
  "count": 2,
  "records": [
    {
      "id": "1",
      "title": "admin_user",
      "value": "{\"id\":1,\"username\":\"admin\",\"role\":\"admin\"}"
    },
    {
      "id": "2",
      "title": "admin_manager",
      "value": "{\"id\":2,\"username\":\"manager\",\"role\":\"admin\"}"
    }
  ]
}
```

### 3. data_update

更新指定表的记录。

**参数:**
- `table`：表名（必需）
- `id`：记录ID（必需）
- `newValue`：新值（必需，JSON格式）

**示例请求:**
```json
{
  "name": "data_update",
  "arguments": {
    "table": "user",
    "id": "1",
    "newValue": "{\"name\":\"new_name\",\"age\":25}"
  }
}
```

**响应格式:**
```json
{
  "resultCode": "SUCCESS",
  "table": "user",
  "id": "1",
  "recordIds": [
    {
      "id": "1",
      "title": "new_name"
    }
  ]
}
```

### 4. data_remove

删除指定表的记录。

**参数:**
- `table`：表名（必需）
- `id`：记录ID（必需）

**示例请求:**
```json
{
  "name": "data_remove",
  "arguments": {
    "table": "user",
    "id": "1"
  }
}
```

**响应格式:**
```json
{
  "resultCode": "SUCCESS",
  "table": "user",
  "id": "1"
}
```

## 错误处理

所有工具调用都返回标准化的JSON响应，包含错误信息：

```json
{
  "error": "Table not found: nonexistent_table"
}
```

**常见错误码:**
- `400`：请求参数错误
- `404`：表或记录不存在
- `405`：HTTP方法不支持
- `500`：服务器内部错误

## 使用示例

### 获取工具列表

```bash
curl http://localhost:3000/tools/list
```

### 调用工具

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

### 批量查询

```bash
# 查询所有用户
curl -X POST http://localhost:3000/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "data_query",
    "arguments": {
      "table": "user",
      "condition": ""
    }
  }'

# 查询管理员用户
curl -X POST http://localhost:3000/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "data_query",
    "arguments": {
      "table": "user",
      "condition": "admin"
    }
  }'
```