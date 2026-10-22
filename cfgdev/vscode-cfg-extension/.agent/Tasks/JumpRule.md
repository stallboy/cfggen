# 定义与查找引用

`LocationVisitor` 只记录可导航的名字：字段类型、外键目标表和每个 `@Type`。内部字段路径不作为表记录，首版不实现逐段字段 resolver。

`FileDefinitionAndRef.references` 是 range 列表，可以包含同一行多个类型/祖先和跨行声明。所有 range 经 `SourcePositions` 从 ANTLR code point 转为 VSCode UTF-16；不能直接使用 token column。

`CfgDefinitionProvider.resolve` 是定义与引用查找共用的解析入口：

1. 类型优先查当前 interface 内实现，再查当前文件；表引用只接受 table/enum，不能被同名 struct 遮蔽。
2. 限定名从当前目录和包含 `config.cfg` 的根目录查模块。`@Interface.Impl` 识别为实现类型；限定模块中的实现也使用同一规则。
3. 无限定名字最后查根 `config.cfg`。

模块目录名规则由 `ModuleResolver` 与后端保持一致：首字符必须为英文字母，汉字后缀按 Unicode Han script 识别，JSON/辅助目录及其子目录不参与模块遍历。查找引用解析候选名字后比较实际定义文件和 range，不把不同作用域中的同名类型混为一谈。

查找引用递归遍历合法模块目录，不设置固定层数上限；后端 schema 允许任意深度的点分模块，三级及更深模块不能只支持正向导航而漏掉反向引用。

缓存使用打开的 `TextDocument` 实例和 version，读取当前 buffer；关闭文档清理缓存。mtime/size 不能识别未保存编辑，禁止恢复为只查磁盘时间。

`npm test` 在真实 ExtensionHost 中验证导航、查找引用、命名空间、非 BMP 文本和未保存编辑。
