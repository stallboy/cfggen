# Quick Start Guide: VSCode CFG Extension

**Date**: 2025-11-08
**Version**: 1.0.0
**目标用户**: 游戏策划人员、配置开发工程师

## 目录
- [安装](#安装)
- [配置](#配置)
- [使用指南](#使用指南)
- [示例](#示例)
- [故障排除](#故障排除)

## 安装

### 从VSCode市场安装（推荐）

1. 打开VSCode
2. 按 `Ctrl+Shift+X` 打开扩展面板
3. 搜索 "CFG Language Support"
4. 点击"安装"

### 从源码安装

```bash
# 克隆仓库
git clone <repository-url>
cd vscode-cfg-extension

# 安装依赖
npm install

# 编译TypeScript
npm run compile

# 在VSCode中调试
# 按F5打开扩展开发主机
```

### 开发模式安装

```bash
# 1. 编译扩展
npm run compile

# 2. 打包扩展
npm run package

# 3. 从VSIX安装
code --install-extension cfg-language-support-1.0.0.vsix
```

## 配置

### 主题颜色设置

打开VSCode设置 (`Ctrl+,`)，搜索 "CFG"，配置以下选项：

#### 主题选择

```json
{
  "cfg.theme": "chineseClassical"  // 可选值: "default" | "chineseClassical"
}
```

**说明**:
- `default`: VSCode标准配色（蓝色关键字、青色类型、绿色注释）
- `chineseClassical`: 中国古典色配色（黛青关键字、苍青类型、竹青注释）

#### 性能设置

```json
{
  "cfg.enableCache": true,           // 启用符号表缓存
  "cfg.maxFileSize": 10485760        // 最大文件大小（10MB）
}
```

#### 完整配置示例

```json
{
  "cfg.theme": "chineseClassical",
  "cfg.enableCache": true,
  "cfg.maxFileSize": 10485760,
  "editor.formatOnSave": true,
  "files.associations": {
    "*.cfg": "cfg"
  }
}
```

## 使用指南

### 1. 打开.cfg文件

扩展激活后，.cfg文件将自动应用语法高亮和语言功能。

```bash
# 示例文件位置
example/config/task/task.cfg
```

### 2. 语法高亮

扩展提供基于ANTLR4的精确语法高亮，使用两套主题色（中国古典色为默认）。

**高亮元素**:

1. **结构定义**: `struct`/`interface`/`table` + 名称，名称高亮
   - 示例: `struct Position` → "Position"高亮
   - 示例: `table task[id]` → "task"高亮

2. **复杂类型**: 非基本类型作为整体高亮
   - 示例: `list<int>` → 整个类型高亮
   - 示例: `Range` → 整个类型高亮
   - 示例: `RewardItem` → 整个类型高亮

3. **主键字段名称**: 表定义中的主键字段高亮
   - 示例: `table task[id]` → "id"高亮
   - 示例: `taskid:int` → 如果taskid是本table主键，"taskid"高亮

4. **外键引用**: 整个引用链路作为整体高亮
   - 示例: `->item.item` → 整个"->item.item"高亮
   - 示例: `->task` → 整个"->task"高亮
   - 示例: `=>tt[kk]` → 整个"=>tt[kk]"高亮

5. **注释**: 注释符号和内容整体高亮
   - 示例: `// 这是注释` → 整行高亮为注释色

6. **元数据**: 元数据关键字高亮
   - 示例: `(nullable)` → "nullable"高亮
   - 示例: `(pack)` → "pack"高亮

### 3. 跳转到定义

**用法**:
1. 将光标放在要跳转的位置（类型名或外键引用）
2. 按 `F12` 或 `Ctrl+Click`
3. 或右键选择"转到定义"

**支持的跳转**:
- 类型定义: `testField:Position` → 跳转到Position定义
- 外键引用: `taskid:int ->task` → 跳转到task表
- 带键外键: `itemids:list<int> ->item.item` → 跳转到item表的item字段
- 跨模块: `monsterid:int ->other.monster` → 跳转到other模块

### 4. 自动补全

**触发自动补全**:
- 输入类型时: `test:` → 显示可用类型列表
- 输入外键时: `->` → 显示可引用的表名
- 输入元数据时: `(` → 显示元数据关键字

**快捷键**:
- `Ctrl+Space`: 手动触发补全
- `Enter` 或 `Tab`: 确认选择

**补全类型**:
1. **类型补全**: 显示所有可用类型（基本类型 + 自定义类型）
2. **外键补全**: 显示所有可引用的表
3. **元数据补全**: 显示支持的元数据关键字
4. **字段名补全**: 在struct内部显示字段名

### 5. 悬停提示

将鼠标悬停在符号上可查看详细信息：

- **字段悬停**: 显示字段类型、描述
- **外键悬停**: 显示目标表、引用类型
- **类型悬停**: 显示完整定义和字段列表

### 6. 错误提示

扩展会高亮显示语法错误：
- 红色波浪线: 语法错误
- 黄色波浪线: 警告
- 蓝色波浪线: 引用未找到

悬停错误可查看详细说明。

## 示例

### 示例1: 基础struct

```cfg
struct Position {
    x:int;      // X坐标
    y:int;      // Y坐标
}

struct TestDefaultBean {
    testInt:int;
    testBool:bool;
    testString:str;
    testSubBean:Position;  // 将光标放在Position上，按F12可跳转
    testList:list<int> (pack);
}
```

**使用步骤**:
1. 打开此文件
2. 观察语法高亮（Position为青色高亮）
3. 在`testSubBean:Position`上按F12，跳转到Position定义

### 示例2: 接口多态

```cfg
interface completecondition (enumRef='completeconditiontype') {
    struct KillMonster {
        monsterid:int ->other.monster;  // 外键引用，跳转到other模块
        count:int;
    }
}

table completeconditiontype[id] (enum='name') {
    id:int;
    name:str;
}
```

**使用步骤**:
1. 将光标放在`->other.monster`上
2. 按F12跳转到other模块的monster表
3. 在`enumRef='completeconditiontype'`上悬停，查看类型信息

### 示例3: 表和外键

```cfg
table task[taskid] {
    taskid:int ->task.taskextraexp (nullable);  // 外键引用
    name:list<text> (fix=2);
    nexttask:int ->task (nullable);
    completecondition:task.completecondition;   // 接口引用
    exp:int;
}
```

**使用步骤**:
1. 在`->task.taskextraexp`上按F12，跳转到taskextraexp表
2. 在`task.completecondition`上按F12，跳转到completecondition接口
3. 输入`completecondition:`时，自动补全显示类型列表

### 示例4: 跨模块引用

假设有以下目录结构：
```
config/
├── task/
│   └── task.cfg
├── item/
│   └── item.cfg
└── npc/
    └── monster.cfg
```

在task.cfg中引用其他模块：

```cfg
struct Test {
    taskId:int ->task;        // 本模块
    itemId:int ->item.item;   // 跨模块item.item
    monsterId:int ->npc.monster;  // 跨模块npc.monster
}
```

**模块名解析规则**:
- `config/task/task.cfg` → 模块名 `task`
- `config/item/item.cfg` → 模块名 `item`
- `config/npc/monster.cfg` → 模块名 `npc`

## 主题对比

### 默认主题 (default)

```json
{
  // 1. 结构定义
  "structureDefinition": "#0000FF",    // struct xx, interface xx, table xx

  // 2. 复杂结构类型声明
  "complexType": "#267F99",            // 非基本类型 (Position等)

  // 3. 主键字段名称
  "primaryKey": "#C586C0",             // PK字段名

  // 4. 唯一键字段名称
  "uniqueKey": "#C586C0",              // UK字段名

  // 5. 外键引用
  "foreignKey": "#AF00DB",             // -> tt, -> tt[kk], => tt[kk]

  // 6. 注释
  "comment": "#008000",                // 绿色注释

  // 7. 特定元数据
  "metadata": "#808080"                // nullable等元数据
}
```

### 中国古典色主题 (chineseClassical)

```json
{
  // 1. 结构定义
  "structureDefinition": "#1E3A8A",    // 黛青 - struct/interface/table + 名称

  // 2. 复杂结构类型声明
  "complexType": "#0F766E",            // 苍青 - 自定义类型

  // 3. 主键字段名称
  "primaryKey": "#7E22CE",             // 紫棠 - PK字段

  // 4. 唯一键字段名称
  "uniqueKey": "#7E22CE",              // 紫棠 - UK字段

  // 5. 外键引用
  "foreignKey": "#BE185D",             // 桃红 - 外键引用

  // 6. 注释
  "comment": "#166534",                // 竹青 - 注释

  // 7. 特定元数据
  "metadata": "#6B7280"                // 墨灰 - 元数据
}
```

**色名由来**:
- 黛青 (#1E3A8A): 传统青黛染料色，深蓝偏青，象征深邃优雅（结构定义）
- 苍青 (#0F766E): 天空青蓝色，象征广阔与稳重（复杂结构类型）
- 紫棠 (#7E22CE): 紫棠花色，深紫偏粉，用于主键和唯一键
- 桃红 (#BE185D): 桃花粉色，温润柔和，用于外键引用
- 竹青 (#166534): 竹叶青绿色，自然清新，用于注释
- 墨灰 (#6B7280): 墨色配灰，沉稳内敛，用于元数据


## 故障排除

### 问题1: 语法高亮不生效

**原因**: 文件关联未正确设置

**解决方案**:
1. 右键点击.cfg文件
2. 选择"用...打开" → "配置语言方式"
3. 选择"cfg"

或配置设置:
```json
{
  "files.associations": {
    "*.cfg": "cfg"
  }
}
```

### 问题2: 跳转到定义无反应

**原因**: 符号未解析或模块未找到

**解决方案**:
1. 检查引用是否存在: `->target` 的target必须为表名
2. 检查跨模块路径: 确保目标文件存在于正确目录
3. 重新加载窗口: `Ctrl+Shift+P` → "Developer: Reload Window"

### 问题3: 自动补全不显示

**原因**: 补全触发时机未满足

**解决方案**:
1. 手动触发: 按 `Ctrl+Space`
2. 检查上下文: 确保光标在正确的补全位置（类型、外键、元数据）
3. 检查语法: 确保前面的语法正确

### 问题4: 大文件卡顿

**解决方案**:
1. 启用缓存: 设置 `"cfg.enableCache": true`
2. 调整文件大小限制: 设置 `"cfg.maxFileSize": 5242880` (5MB)
3. 分文件: 将大文件拆分为多个小文件

### 问题5: 主题颜色不生效

**解决方案**:
1. 重启VSCode: 关闭并重新打开
2. 检查设置: `Ctrl+,` → 搜索"cfg.theme"
3. 手动设置颜色:
```json
{
  "editor.tokenColorCustomizations": {
    "[*]": {
      "textMateRules": [
        {
          "name": "CFG Keyword",
          "scope": "keyword.control.cfg",
          "settings": {
            "foreground": "#1E3A8A"
          }
        }
      ]
    }
  }
}
```

### 调试模式

启用详细日志:

```json
{
  "cfg.debug": true,
  "cfg.logLevel": "verbose"
}
```

日志位置:
- Windows: `%APPDATA%\Code\logs`
- macOS: `~/Library/Application Support/Code/logs`
- Linux: `~/.config/Code/logs`

## 贡献

欢迎提交Issue和Pull Request！

**开发环境**:
```bash
npm install          # 安装依赖
npm run compile      # 编译
npm run test         # 运行测试
npm run lint         # 代码检查
```

## 支持

如有问题，请：
1. 查看本指南的故障排除部分
2. 提交GitHub Issue
3. 发送邮件至: support@example.com

## 更新日志

### v1.0.0 (2025-11-08)
- 初始发布
- 语法高亮支持
- 跳转到定义
- 自动补全
- 双主题支持（默认 + 中国古典色）
- 跨模块引用解析
