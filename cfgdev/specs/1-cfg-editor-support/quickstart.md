# Quick Start: CFG文件编辑器支持

**Feature**: CFG文件编辑器支持
**Date**: 2025-11-06

## 概述

本插件为.cfg配置文件提供完整的编辑器支持，包括语法高亮、智能提示、类型定义跳转和外键跳转功能。

## 安装和设置

### 1. 安装插件
```bash
# 从VSCode Marketplace安装
# 或从本地安装
code --install-extension cfg-editor-support.vsix
```

### 2. 配置设置
在VSCode设置中配置插件选项：
```json
{
  "cfg.enableSyntaxHighlighting": true,
  "cfg.enableAutoCompletion": true,
  "cfg.validationLevel": "semantic",
  "cfg.enableTypeNavigation": true,
  "cfg.enableForeignKeyNavigation": true
}
```

## 基本功能

### 1. 语法高亮
- 打开任意.cfg文件
- 不同类型元素自动着色
- 支持嵌套结构和注释

### 2. 智能提示
- 输入时显示自动补全
- 基于上下文的建议
- 错误检测和提示

### 3. 类型定义跳转
- 右键点击类型名称
- 使用Ctrl+Click跳转
- 支持跨文件跳转

### 4. 外键跳转
- 右键点击外键引用
- 使用Ctrl+Click跳转
- 查看引用关系

## 使用示例

### 编辑配置文件
```cfg
# 结构体定义
struct Position {
    x:int;
    y:int;
}

# 接口定义（多态）
interface completecondition (enumRef='completeconditiontype', defaultImpl='TestNoColumn') {
    struct KillMonster {
        monsterid:int ->other.monster;
        count:int;
    }

    struct TalkNpc {
        npcid:int;
    }

    struct TestNoColumn {
    }

    struct ConditionAnd {
        cond1:task.completecondition (pack);
        cond2:task.completecondition (pack);
    }
}

# 表定义
table completeconditiontype[id] (enum='name') {
    id:int; // 任务完成条件类型
    name:str; // 程序用名字
}

table task[taskid] {
    taskid:int ->task.taskextraexp (nullable); // 任务ID
    name:list<text> (fix=2); // 程序用名字
    nexttask:int ->task (nullable); // 下一个任务
    completecondition:task.completecondition; // 完成条件
    exp:int; // 经验奖励
    testDefaultBean:TestDefaultBean (pack); // 测试结构体
}
```

### 使用跳转功能
1. 将鼠标悬停在类型名称上查看文档
2. Ctrl+点击结构体/接口/表名称跳转到定义
3. Ctrl+点击外键引用（->, =>）跳转到目标表
4. Ctrl+点击接口实现类跳转到具体结构体
5. Ctrl+点击enumRef表跳转到枚举表定义
6. 使用右键菜单查找所有引用

### 验证配置
1. 保存文件时自动验证
2. 查看问题面板中的错误和警告
3. 使用快速修复功能

## 高级功能

### 1. 批量验证
```bash
# 在终端中运行
cfg validate ./configs/
```

### 2. 索引重建
```bash
# 重建项目索引
cfg rebuild-index
```

### 3. 性能监控
- 查看输出面板的性能统计
- 监控内存使用情况
- 调整缓存设置优化性能

## 故障排除

### 常见问题

**Q: 语法高亮不工作**
A: 检查文件扩展名是否为.cfg，确认插件已激活

**Q: 跳转功能无效**
A: 确保相关配置文件在同一项目中，检查索引状态

**Q: 性能缓慢**
A: 减少同时打开的文件数量，调整缓存设置

### 调试模式
启用调试模式获取详细日志：
```json
{
  "cfg.debugMode": true,
  "cfg.logLevel": "verbose"
}
```

## 性能优化

### 1. 大型项目
- 使用项目级索引
- 启用增量解析
- 配置内存限制

### 2. 网络文件
- 启用文件缓存
- 减少自动验证频率
- 使用本地副本

### 3. 资源限制
- 调整并发处理数量
- 限制最大文件大小
- 配置超时设置

## 扩展开发

### 自定义验证规则
```typescript
// 添加自定义验证器
vscode.commands.registerCommand('cfg.addCustomValidator', (validator: CustomValidator) => {
  // 注册自定义验证逻辑
});
```

### 插件集成
```typescript
// 与其他插件集成
const cfgExtension = vscode.extensions.getExtension('cfg-editor-support');
if (cfgExtension) {
  const api = cfgExtension.exports;
  // 使用插件API
}
```

## 支持资源

- [用户手册](./docs/user-guide.md)
- [API参考](./docs/api-reference.md)
- [故障排除指南](./docs/troubleshooting.md)
- [GitHub仓库](https://github.com/your-repo/cfg-editor-support)