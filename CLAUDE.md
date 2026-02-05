# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目结构

这是一个策划配表系统，包含对象数据库浏览器、编辑器和程序访问代码生成器。

- **`app/`** - 核心配置生成器 (cfggen)，Java 项目，负责从 Excel/CSV/JSON 读取配置数据并生成多语言代码
- **`cfgeditor/`** - 可视化配置编辑器，React + TypeScript + Tauri 桌面应用
- **`cfgdev/`** - 开发工具集，包含 Claude Code 插件和 VSCode 扩展
  - `schema-gen-plugin/` - Claude Code 插件，根据自然语言生成 .cfg schema
  - `vscode-cfg-extension/` - VSCode 扩展，提供 .cfg 文件语法高亮和跳转

- **`example/`** - 多语言代码生成测试示例

- **`samples/`** - 实际游戏系统配置示例（技能、触发器、剧情对话等）

- **`docs/`** - 项目文档

### 相关文档
- 各子目录的 `CLAUDE.md` 包含详细的架构和开发指南
- 各子目录的 `README.md` 包含构建和使用说明
