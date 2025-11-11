# CFG 语言支持扩展

一个专为 `.cfg` 配置文件设计的 VSCode 扩展，提供语法高亮、智能跳转和语义标记功能。

> **项目背景**: 本扩展是 [cfggen 策划配置系统](https://github.com/stallboy/cfggen) 的配套工具，为 cfggen 的配置文件提供完整的 IDE 支持。

## ✨ 功能特性

### 🎨 语法高亮
- **结构定义高亮**：`struct`、`interface`、`table` 名称
- **类型标识符高亮**：自定义类型名称（非基础类型）
- **外键引用高亮**：`->table1`、`=>table1[field2]` 等外键语法
- **元数据关键字高亮**：`nullable`、`mustFill`、`enumRef` 等
- **主键字段高亮**：在表定义中的主键字段

### 🔗 智能跳转
- **跳转到定义**：Ctrl+点击类型名称或外键引用跳转到定义位置
- **多级查找策略**：
  - 当前接口内查找
  - 当前文件内查找
  - 模块内查找（包名映射）
  - 根目录查找

### 🏷️ 语义标记
- 基于解析器的智能高亮，提供更准确的语法标记
- 支持复杂类型表达式（如 `list<int>`、`map<string, int>`）

## 📁 支持的文件类型

- `.cfg`
- `.CFG`
- `.Cfg`

## 🚀 快速开始

1. **安装扩展**：在 VSCode 扩展商店中搜索 "cfg-support" 并安装
2. **打开 CFG 文件**：打开任何 `.cfg` 文件即可享受语法高亮
3. **使用跳转功能**：按住 Ctrl 键并点击类型名称跳转到定义

## 📚 语言特性支持

### 基础类型
- `int`、`float`、`long`、`bool`、`str`、`text`

### 复杂类型
- `list<T>`：列表类型
- `map<K,V>`：映射类型
- 自定义结构类型

### 结构定义
```cfg
struct User {
    id: int;
    name: str;
}

interface GameData {
    struct Item {
        id: int;
        name: str;
    }
}

table player[id] {
    id: int;
    name: str;
} 
```

### 外键引用
```cfg
item: Item ->item.item;          // 单体外键
items: list<Item> =>item.item;   // 列表外键
complex: Item ->item.item[id];   // 带键的外键
```

### 元数据
```cfg
name: str (nullable);           // 可为空
count: int (mustFill);          // 必须填充
```

## 🎯 使用技巧

### 跳转到定义
- **类型跳转**：Ctrl+点击类型名称跳转到其定义
- **外键跳转**：Ctrl+点击外键引用跳转到目标表
- **接口内跳转**：在接口内部定义的 struct 优先在当前接口内查找

## 🔧 配置选项

扩展会自动为 CFG 文件启用以下配置：
- 智能代码提示
- 语义高亮

## 🤝 问题反馈

如果您在使用过程中遇到任何问题或有功能建议，请通过以下方式联系我们：
- **扩展相关问题**: 在 [本扩展仓库](https://github.com/stallboy/cfggen) 提交 Issue
- **配置系统问题**: 在 [cfggen 主仓库](https://github.com/stallboy/cfggen) 提交 Issue
- **功能建议**: 在扩展商店页面提交评价

## 🌟 cfggen 生态系统

本扩展是 cfggen 配置系统生态的一部分，与以下组件协同工作：

- **[cfggen 核心工具](https://github.com/stallboy/cfggen)**: 配置系统生成器和运行时
- **本 VSCode 扩展**: 提供 IDE 支持和开发体验
- **cfgeditor**: 基于节点的编辑器和查看器

完整的 cfggen 生态系统提供从配置定义到数据读取、数据编辑和浏览、再到代码生成的全链路解决方案。

## 📄 许可证

MIT License

---

享受编写 CFG 配置文件的愉快体验！ 🎉