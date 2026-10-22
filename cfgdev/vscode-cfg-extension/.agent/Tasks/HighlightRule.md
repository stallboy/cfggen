# 高亮规则

TextMate 负责基础词法高亮，ANTLR visitor 负责精确的语义 token：

- struct/interface/table/enum 的名字：`structureDefinition`。
- 自定义字段类型、每个 `@Type`：`typeIdentifier`。
- 外键目标表：`foreignKey`。
- 内部来源和目标的字段段：标准 `property`。
- metadata 名字（含 index、nullable）、主键、唯一键、enum 成员保留各自类型。

每个名字从 parser context 取 range，经 `SourcePositions` 转成 UTF-16；不按整条外键起止列拼 token，不能让跨行范围、emoji 或后续声明错位。不把字段路径着色成目标表。

语法权威是 app 的 `Cfg.g4`。`npm run generate-parser` 同步并生成，`npm run check-grammar` 检查一致性。不要手改生成 parser 来修特定 fixture。

真实 ExtensionHost 测试解码语义 token，检查其覆盖的原文、字段类别及 emoji 后同一行位置。
