---
layout: page
title: 资源接口
parent: MCP 服务器
nav_order: 2
---

# 资源接口

MCP服务器将每个配置表作为资源暴露，通过标准化的资源接口访问表数据。

## 资源URI格式

- 资源URI格式：`table://{table_name}`
- 示例：`table://user`、`table://item`

## 可用接口

### 1. 获取资源列表

获取所有可用的表资源列表。

**请求:**
```bash
GET /resources/list
```

**响应格式:**
```json
{
  "resources": [
    {
      "uri": "table://user",
      "name": "user",
      "description": "Table: user",
      "mimeType": "application/json"
    },
    {
      "uri": "table://item",
      "name": "item",
      "description": "Table: item",
      "mimeType": "application/json"
    }
  ]
}
```

### 2. 读取资源内容

读取指定表的所有数据。

**请求:**
```bash
GET /resources/read?table://{table_name}
```

**示例:**
```bash
GET /resources/read?table://user
```

**响应格式:**
```json
{
  "contents": [
    {
      "uri": "table://user",
      "mimeType": "application/json",
      "text": "{\"table\":\"user\",\"count\":2,\"records\":[{\"id\":\"1\",\"title\":\"admin\",\"value\":\"{\\\"id\\\":1,\\\"username\\\":\\\"admin\\\",\\\"role\\\":\\\"admin\\\"}\"},{\"id\":\"2\",\"title\":\"user\",\"value\":\"{\\\"id\\\":2,\\\"username\\\":\\\"user\\\",\\\"role\\\":\\\"user\\\"}\"}]}"
    }
  ]
}
```

## 使用示例

### 获取所有可用资源

```bash
curl http://localhost:3000/resources/list
```

### 读取特定表数据

```bash
# 读取用户表数据
curl "http://localhost:3000/resources/read?table://user"

# 读取物品表数据
curl "http://localhost:3000/resources/read?table://item"
```

### 批量读取多个表

```bash
# 获取资源列表
RESOURCES=$(curl -s http://localhost:3000/resources/list | jq -r '.resources[].uri')

# 逐个读取所有表数据
for uri in $RESOURCES; do
  echo "Reading $uri"
  curl -s "http://localhost:3000/resources/read?$uri" | jq .
  echo ""
done
```

## 错误处理

资源接口返回标准化的错误响应：

```json
{
  "error": "Table not found: nonexistent_table"
}
```

**常见错误:**
- `400`：无效的资源URI格式
- `404`：表不存在
- `405`：HTTP方法不支持

## 与工具接口的关系

资源接口和工具接口提供了不同的数据访问方式：

| 特性 | 资源接口 | 工具接口 |
|------|----------|----------|
| 数据格式 | 完整的表数据 | 结构化的查询结果 |
| 查询能力 | 无查询条件 | 支持条件查询 |
| 操作类型 | 只读 | 支持CRUD操作 |
| 适用场景 | 批量数据导出 | 交互式查询和操作 |

### 使用建议

- **资源接口**：适合需要获取完整表数据的场景，如数据导出、备份等
- **工具接口**：适合需要条件查询、数据操作等交互式场景

## 性能考虑

- 资源接口返回完整的表数据，对于大型表可能会有性能影响
- 建议在需要完整数据时使用资源接口，在需要特定数据时使用工具接口
- 服务器支持数据缓存和懒加载，优化访问性能