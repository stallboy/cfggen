---
layout: page
title: 配表系统
nav_order: 2
---

# 🗂️ 配表系统

对象数据库结构定义、程序访问代码生成

1. 📝 定义对象结构
2. 📊 使用excel编辑、或直接编辑json、或使用基于节点的编辑器编辑，可以浏览所有对象。
3. 🚀 生成访问代码

[快速开始 →](./00.quickstart.md){: .btn .btn-primary .fs-5 .mb-4 .mb-md-0 } [View it on GitHub][cfggen repo]{: .btn .fs-5 .mb-4 .mb-md-0 }

## ✨ 主要功能

* 🔗 通过配置外键，检测数据一致性

* 💻 通过生成代码，来访问类型化数据，生成外键引用，生成entry、enum，支持java、c#、lua、go、typescript

* 🏗️ 支持多态结构、嵌套结构，可在一个单元格里写任意复杂的结构数据，让excel有了xml的灵活性

* 🛡️ 生成java注重安全

* 💾 生成lua注重内存大小

---

## 📚 文档导航

- [快速开始](./00.quickstart.md) - 5分钟上手教程
- [命令行](./01.usage.md) - 完整命令行参考与构建测试
- [Schema定义](./03.schema.md) - CFG结构定义语法
- [表格数据](./04.tableData.md) - 表格数据编辑说明
- [代码生成](./05.codegen.md) - 多语言代码生成详解
- [其他元数据](./07.otherMetadatas.md) - 标签、术语等元数据
- [配置编辑器](../cfgeditor/) - 可视化配置编辑工具
- [AI 生成](../aigen/) - 使用AI自动生成配置数据

[cfggen repo]: https://github.com/stallboy/cfggen