# CFG 扩展结构

- `src/grammar/Cfg.g4` 从 app 权威 grammar 同步，`antlr-ng` 生成 TypeScript parser。
- `src/utils/cfgDocument.ts` 共用 parser 构造和 code point → UTF-16 位置转换。
- `src/highlight` 生成 semantic tokens；`syntaxes/cfg.tmLanguage.json` 提供即时 TextMate 高亮。
- `src/definition` 收集声明和 range 引用列表。定义与查找引用共用 resolver，保持类型/表命名空间边界；跨模块查找不继承来源文件的接口作用域。
- `src/outline` 从同一文档缓存生成大纲。
- `FileCache` 按 TextDocument 实例和 version 缓存，关闭文档清理；未保存修改直接生效。
- `test/suite` 在真实 VSCode ExtensionHost 验证语言行为，`test/run.cjs` 使用已安装的编辑器与独立 profile，不下载程序。

开发命令：`npm run generate-parser`、`npm run check-grammar`、`npm test`、`npm run webpack`、`npm run package`。

新引用支持本 record、跨 record、祖先作用域与独立来源路径的解析、高亮、类型/表导航。字段路径逐段导航暂不实现。扩展为桌面 Node 构建。
