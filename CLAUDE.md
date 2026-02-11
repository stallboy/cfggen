# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

### 相关文档
- 各子目录的 `CLAUDE.md` 包含详细的架构和开发指南
- 各子目录的 `README.md` 包含构建和使用说明

不要在顶层目录创建gradle

## Windows 环境命令行注意事项

本项目在 Windows 环境下使用 MSYS/Git Bash，执行命令时请注意：

### 路径格式
- **正确**：使用 Unix 风格路径 `/d/work/mygithub/cfggen/app`
- **错误**：使用 Windows 风格路径 `D:\work\mygithub\cfggen\app`

### 执行 .bat 文件
- **正确**：`./gradlew.bat fatjar` 或 `./genjar.bat`
- **错误**：`gradlew.bat fatjar`（找不到命令）
- **错误**：`cmd //c genjar.bat`（某些情况下不工作）

### 常用构建命令示例
```bash
# 构建 cfggen.jar（在 app 目录下）
cd /d/work/mygithub/cfggen/app && ./gradlew.bat fatjar

# 复制 jar 到根目录
cp /d/work/mygithub/cfggen/app/build/libs/cfggen.jar /d/work/mygithub/cfggen/cfggen.jar

# 生成 Go 代码测试
cd /d/work/mygithub/cfggen/example/go && java -jar ../../cfggen.jar -datadir ../config -gen go,dir:.,encoding:UTF-8 -gen bytes
```