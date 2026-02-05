# 📚 策划配表系统文档

[![Built with Starlight](https://astro.badg.es/v2/built-with-starlight/tiny.svg)](https://starlight.astro.build)

这是策划配表系统（cfggen）的官方文档站点，基于 [Astro](https://astro.build) 和 [Starlight](https://starlight.astro.build) 构建。

## 📖 文档内容

文档站点包含以下主要部分：

- **[配表系统 (cfggen)](src/content/docs/cfggen/)** - 核心配置生成器文档
  - 快速开始指南
  - Schema 定义语法
  - 主键、枚举、外键配置
  - 表格映射规则
  - 命令行使用说明

- **[编辑器 (cfgeditor)](src/content/docs/cfgeditor/)** - 可视化配置编辑器文档
  - 安装和启动指南
  - 界面功能介绍
  - 基本操作说明
  - 高级功能使用

- **[AI 生成 (aigen)](src/content/docs/aigen/)** - AI 辅助配置生成文档
  - AI 生成功能概述
  - 配置文件详解
  - 工作流程说明
  - 最佳实践

## 🚀 本地开发

### 安装依赖

```bash
pnpm install
```

### 启动开发服务器

```bash
pnpm dev
```

访问 `http://localhost:4321` 查看文档

### 构建生产版本

```bash
pnpm build
```

构建输出位于 `dist/` 目录

### 预览构建结果

```bash
pnpm preview
```

## 📁 项目结构

```
.
├── public/              # 静态资源文件
├── src/
│   ├── assets/         # 图片等资源
│   ├── content/
│   │   └── docs/       # 文档内容（.md 文件）
│   └── content.config.ts
├── astro.config.mjs    # Astro 配置
├── package.json
└── tsconfig.json
```

Starlight 会自动将 `src/content/docs/` 目录下的 `.md` 或 `.mdx` 文件转换为文档页面。

## ✍️ 编辑文档

1. 在 `src/content/docs/` 对应目录下创建或编辑 `.md` 文件
2. 每个文档文件需要包含 frontmatter（标题、描述等）
3. 图片资源放在 `src/assets/` 目录，使用相对路径引用
4. 运行 `pnpm dev` 实时预览修改效果

### 文档 Frontmatter 示例

```yaml
---
title: 文档标题
description: 文档描述
sidebar:
  order: 1  # 侧边栏排序
---
```

## 🌐 部署

文档可以部署到任何静态网站托管服务：

- **GitHub Pages** - 免费，适合开源项目
- **Vercel** - 零配置部署
- **Netlify** - 支持 CI/CD
- **自托管** - 使用 `pnpm build` 构建后部署 `dist/` 目录

## 🔗 相关链接

- [主项目 README](../README.md)
- [配表系统 (cfggen)](../app/)
- [配置编辑器 (cfgeditor)](../cfgeditor/)
- [开发工具 (cfgdev)](../cfgdev/)
- [在线文档](https://stallboy.github.io/cfggen)

## 📚 更多资源

- [Starlight 文档](https://starlight.astro.build/)
- [Astro 文档](https://docs.astro.build)
- [Astro Discord](https://astro.build/chat)
