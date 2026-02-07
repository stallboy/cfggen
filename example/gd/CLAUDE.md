# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目说明

这是 cfggen 配置生成器的 **GDScript (Godot) 代码生成示例项目**，展示如何从配置表生成类型安全的 GDScript 配置访问代码。

## 目录结构

- **`config/`** - 自动生成的配置代码目录（**不要手工修改**）
  - `ConfigProcessor.gd` - 配置处理器，负责加载所有配置表
  - `ConfigLoader.gd` - 配置加载器入口
  - `ConfigErrors.gd` - 错误和警告收集器
  - `ConfigStream.gd` - 二进制配置读取流
  - `Data*.gd` - 各配置表的数据类
  - `Ai/`, `Equip/`, `Other/`, `Task/` - 按模块分组的配置类
- **`config.bytes`** - 二进制配置数据文件
- **`project.godot`** - Godot 项目配置文件

## 代码生成

### 重新生成代码

```bash
gengd.bat
```

该命令会：
1. 根据 `../config/` 中的 `.cfg` schema 定义生成 GDScript 代码到 `config/` 目录
2. 生成二进制配置数据到 `config.bytes`

### 生成命令详解

```bash
java -jar ../cfggen.jar -datadir config -gen gd,own:-nogd,dir:gd/config,encoding:UTF-8 -gen bytes,own:-nogd,file=gd/config.bytes,stringpool
```

- `-datadir config` - 配置数据源目录
- `-gen gd,own:-nogd,dir:gd/config` - 生成 GDScript 代码到 `gd/config/`
- `-gen bytes,file=gd/config.bytes,stringpool` - 生成二进制数据文件（启用字符串池优化）
  - `stringpool` 参数启用字符串池，可以显著减少文件大小
  - 字符串池会将重复的字符串只存储一份，数据中存储索引而非实际字符串
  - 启用后需要在代码中先调用 `stream.read_string_pool()` 读取字符串池

## 配置系统架构

### 核心加载流程

```
ConfigLoader.load_from_bytes(config.bytes)
    ↓
创建 ConfigProcessor
    ↓
从 ConfigStream 读取配置表名和数据
    ↓
调用各 Data*_init_from_stream() 初始化
    ↓
调用 _resolve_refs() 解析外键引用
    ↓
收集错误到 ConfigErrors
```

### 数据类结构

每个配置表生成一个数据类，包含：

- **静态存储** - `static var _data: Dictionary[int, DataType]`
- **静态查询** - `static func find(id: int) -> DataType`
- **静态枚举** - 对于有枚举字段的表，生成静态实例（如 `DataEquip_Ability.Attack`）
- **外键引用** - 以 `Ref` 或 `NullableRef` 前缀命名的引用属性
- **多态结构** - 嵌套对象使用独立的 create() 和 _resolve() 方法

示例：
```gdscript
# 主键查询
var item = DataEquip_Jewelry.find(1001)

# 访问外键引用
var ability = item.RefKeyAbility  # 返回 DataEquip_Ability 实例

# 静态枚举访问
var attack_ability = DataEquip_Ability.Attack
```

### 外键引用解析

- 所有外键引用在加载完成后通过 `_resolve_refs()` 统一解析
- 引用为空时会记录错误到 `ConfigErrors`
- 支持可空外键（NullableRef 前缀）

### 多态和嵌套结构

- 嵌套结构（如 `DataLevelrank`）在父类的 `create()` 中创建
- 多态结构（如 `DataTask_Completecondition`）根据类型字段创建具体子类
- 嵌套对象也需要实现 `_resolve()` 方法来解析自己的外键

## 在 Godot 中使用

```gdscript
# 加载配置
var file = FileAccess.open("res://config.bytes", FileAccess.READ)
var bytes = file.get_buffer(file.get_length())

# 创建流并读取字符串池（如果启用了 stringpool）
var stream = ConfigStream.new(bytes)
stream.read_string_pool()  # 必须在加载配置前调用

# 加载配置数据
ConfigLoader.load_from_stream(stream)

# 检查错误
var errors = ConfigLoader.get_errors()
if errors.get_error_count() > 0:
    errors.print_all()

# 查询数据
var jewelry = DataEquip_Jewelry.find(1001)
print(jewelry.name)
print(jewelry.RefKeyAbility.name)
```

## 重要约束

1. **config/ 目录完全由 cfggen 生成，不要手工修改**
2. 如需修改配置结构，应修改 `../config/` 中的 `.cfg` schema 文件
3. 如需修改生成逻辑，应修改 `../../app/` 中的 cfggen 代码生成器
4. `config.bytes` 必须与生成的代码版本匹配

## 配置数据源

配置 schema 定义位于 `../config/` 目录，包括：
- `ai.cfg` - AI 相关配置
- `equip.cfg` - 装备相关配置
- `other.cfg` - 其他配置（怪物、掉落等）
- `task.cfg` - 任务相关配置
