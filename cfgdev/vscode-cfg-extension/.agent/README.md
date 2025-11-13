# .agent 文档索引

本目录包含VSCode CFG扩展项目的所有重要文档，按功能模块组织。

## 目录结构

```
.agent/
├── README.md              # 本文档 - 文档索引
├── System/                # 系统状态文档
│   └── System.md          # 系统架构、技术栈、项目结构
├── Tasks/                 # 功能任务文档
│   ├── HighlightRule.md       # 语法高亮规则
│   └── JumpRule.md            # 跳转规则
└── ref/                   # 参考文档
    ├── antlr4ng.md            # ANTLR4 TypeScript运行时文档
    ├── vscode-Bundling.md     # VSCode扩展打包指南
    └── vscode-Semantic-Highlight-Guide.md  # VSCode语义高亮指南
```

## 更新策略

1. **功能开发后**: 更新相关Tasks文档
2. **架构变更后**: 更新System文档
3. **新工具引入**: 添加ref参考文档
4. **定期维护**: 确保所有文档保持最新

## 快速导航

- 🏗️ **系统架构**: 查看[System.md](System/System.md)
- 🎨 **高亮规则**: 查看[HighlightRule.md](Tasks/HighlightRule.md)
- 🔗 **跳转和引用**: 查看[JumpRule.md](Tasks/JumpRule.md)
- 📚 **技术参考**: 查看[ref/](ref/)目录

## 维护说明

所有文档应保持简洁明了，专注于核心信息。在实现新功能或修改现有功能后，请及时更新相关文档以反映最新状态。