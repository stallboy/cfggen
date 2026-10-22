# CFG 语言支持扩展

一个专为 `.cfg` 配置文件设计的 VSCode 桌面扩展，提供语法高亮、跳转到定义、大纲视图、查找引用功能。

> **项目背景**: 本扩展是 [cfggen 策划配置系统](https://github.com/stallboy/cfggen) 的配套工具，为 cfggen 的配置文件提供完整的 IDE 支持。

## ✨ 功能特性

### 🎨 语法高亮
- **结构定义高亮**：`struct`、`interface`、`table` 名称
- **类型标识符高亮**：自定义类型名称（非基础类型）
- **外键引用高亮**：旧表外键和 `->::path`、`->table::path`、`->@Type::path`、混合来源路径
- **元数据关键字高亮**：`index`、`nullable`、`mustFill`、`enumRef` 等
- **主键字段高亮**：在表定义中的主键字段

### 🔗 跳转到定义
- **跳转到定义**：Ctrl+点击类型名称或外键引用跳转到定义位置
- **多级查找策略**：当前接口内 → 当前文件内 → 模块内 → 根目录
- **祖先类型导航**：支持同一声明中的多个 `@Type` 和 `@Interface.Impl`，跨行与未保存编辑均可用
- **字段路径边界**：内部字段按字段着色；暂不提供逐段字段跳转，避免把字段误认为同名表

### 📋 大纲视图
- **结构层级导航**：显示所有结构定义的层级关系
- **快速定位**：点击大纲项直接跳转到对应定义

### 🔍 查找引用
- **引用查找**：查找类型和表的所有引用位置
- **跨文件支持**：支持跨文件的引用查找
- **深层模块**：递归查找配置根目录下的合法模块，包含三级及更深模块

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
itemId:int ->item;                 // 表主键
itemCode:str ->item[code];         // 表唯一键
phaseKey:str ->@Machine::phases;   // 最近祖先内的对象
->target:[abilityId,machineKey,phaseKey] ->ability::machines.phases;
->origin:[] ->::origin;            // 本 record 固定成员
```

## 🎯 使用技巧

### 跳转到定义
- **类型跳转**：Ctrl+点击类型名称跳转到其定义
- **外键跳转**：Ctrl+点击外键引用跳转到目标表
- **接口内跳转**：在接口内部定义的 struct 优先在当前接口内查找

### 大纲视图
- **结构导航**：使用大纲视图快速浏览文件结构
- **层级展示**：清晰显示接口、结构、表的层级关系

### 查找引用
- **引用查找**：右键点击类型或表名选择"查找所有引用"
- **跨文件分析**：支持分析整个项目的引用关系

## 开发与验证

`npm run generate-parser` 从仓库中 app 的权威 `Cfg.g4` 同步，再生成 TypeScript parser。独立源码包没有 app 目录时使用已携带的 grammar。`npm run check-grammar` 检查两份 grammar 一致。

```powershell
npm test
npm run webpack
```

`npm test` 先生成 parser、检查类型和 lint，再使用本机 VSCode 启动独立 ExtensionHost；不下载编辑器。Windows 默认查找 `C:/Program Files/Microsoft VS Code/Code.exe`，其他位置用 `VSCODE_EXECUTABLE` 指定。测试使用独立 profile 和隐藏窗口，输出日志保存在忽略的 `.vscode-test-*` 目录。

实际测试覆盖新旧语法、真实 combat schema、多来源和跨行导航、类型/表命名空间、接口实现、emoji 的 UTF-16 位置、高亮、查找引用和未保存编辑/undo。

## 🤝 问题反馈

如果您在使用过程中遇到任何问题或有功能建议，请通过以下方式联系我们：
- **扩展相关问题**: 在 [本扩展仓库](https://github.com/stallboy/cfggen) 提交 Issue
- **配置系统问题**: 在 [cfggen 主仓库](https://github.com/stallboy/cfggen) 提交 Issue
- **功能建议**: 在扩展商店页面提交评价

## 🌟 cfggen 生态系统

本扩展是 cfggen 配置系统生态的一部分，与以下组件协同工作：

- **[cfggen 核心工具](https://github.com/stallboy/cfggen)**: 配置系统生成器和运行时
- **cfgeditor**: 基于节点的编辑器和查看器
- **本 VSCode 扩展**: 提供 IDE 支持和开发体验

完整的 cfggen 生态系统提供从配置定义到数据读取、数据编辑和浏览、再到代码生成的全链路解决方案。

## 📄 许可证

MIT License

---

享受编写 CFG 配置文件的愉快体验！ 🎉
