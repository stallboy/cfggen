## 项目结构

这是一个策划配表系统，包含对象数据库浏览器、编辑器和程序访问代码生成器。

- **`app/`** - 核心配置生成器 (cfggen)，Java 项目，负责从 Excel/CSV/JSON 读取配置数据并生成多语言代码
  - 构建：运行 `genjar.bat` 即可生成 cfggen.jar
- **`cfgeditor/`** - 可视化配置编辑器，React + TypeScript + Tauri 桌面应用
- **`cfgdev/`** - 开发工具集，包含 Claude Code 插件和 VSCode 扩展
  - `schema-gen-plugin/` - Claude Code 插件，根据自然语言生成 .cfg schema
  - `vscode-cfg-extension/` - VSCode 扩展，提供 .cfg 文件语法高亮和跳转

- **`example/`** - 多语言代码生成测试示例

- **`samples/`** - 实际游戏系统配置示例（技能、触发器、剧情对话等）

- **`docs/`** - 项目文档

### 提交礼仪

- 提交标题使用“小写模块标志: 中文标题”；标题说明和正文使用中文，代码标识符、命令及专有名词可保留原文。
- 一个 bug 一个提交；提交信息简述问题证据、关键修复和实际验证，未执行的不写。
