# Java 新格式示例

生成器、生成的 Java 代码和支持源码统一使用 JDK 25；示例 Gradle 工具链也是 JDK 25。

在仓库根目录运行：

```powershell
.\app\gradlew.bat -p app fatJar
.\example\java\genjava.bat
.\example\java\run.bat
```

`genjava.bat` 使用 `app/build/libs/cfggen.jar`，更新此示例的生成类型和支持源码。`-gen bytes` 使用默认的新格式和 `pack=base`；不再需要 `schema` flag。

`LoadConfig.java` 的加载入口是：

```java
try (ConfigInput input = new ConfigInput(Path.of("config.bytes"))) {
    ConfigPackage pack = ConfigPackage.read(input);
    ConfigMgrLoader.loadPackagesAndSetMgr(pack);
}
```

加载器检查包版本、依赖和 schema，建立索引、解析引用并初始化扩展后，才发布默认实例。示例再次加载同一包，检查新旧 ConfigMgr 和表记录相互独立；旧对象仍可通过原实例查询。运行输出包含：

```text
config.bytes version=1, metadata={defaultLanguage=default, pack=base}
reload: independent instances, original task=...
task 1 sources=[base]
```

执行 `.\example\java\repl.bat` 可交互查看：

```text
bytes eq
get other.monster 1
schema monster
q
```

`bytes` 读取新文件，`get` / `query` 查看已加载实例；查看器支持只读索引和 seq 列表。

旧数据须重新导出；导出器、加载器和查看器仅支持当前版本的容器。

`configgen.genjava.ConfigLoader<K, T>` 是共享 Java 支持类，不在 `config` 包重复生成，仅在加载期间持有合并记录。`ConfigMgr` 不保存输入字节；旧 `copyFrom` / `loadPartialAndSetMgr` 已移除，更新数据时重新构建实例后设置默认实例。没有 `ConfigNode` 接口；生成器仅为包含 `javaBase` 的类型生成扩展收集方法，加载器直接调用 `onConfigReady()`。

每次加载创建普通的 `LoadValueErrs` 对象，无 `ThreadLocal` 或静态错误状态。`Error` 是 sealed interface，具体 record 保存各自参数，例如 `MissingReference(source, field, target, key)`、`DuplicateRecord(table, pack, key)`、`InvalidSeq`、`ChangedSymbolId`。错误直接包含实际参数，无 `Row` 包装或 `at` 上下文，通用 `add/fail` 均为 private。可恢复错误自动聚合；无法继续解码的错误立即中止。加载失败时捕获 `ConfigErr`，通过 `exception.getErrors()` 取得不可变快照，使用 `instanceof` 或模式 switch 处理；无需 `start`、清理或重置。

`LoadValueErrs` 的分类方法只记录参数；缺失引用和符号由生成代码判断后调用 `missingReference(...)` / `missingSymbol(...)`，schema 双向兼容检查由 `ConfigPackage.validateSchema(...)` 执行。enum 和 entry 的详情引用在各自 `ConfigMgr` 中按固定符号下标缓存，`Rank.WHITE.ref()` 读取默认实例和数组元素，`Rank.WHITE.ref(config)` 直接读取指定实例的缓存；切换默认实例不会逐个修改全局符号。

手动读取统一使用 `pack.block(".other.monster")`，字符串池和文本池由包内部延迟解析并缓存。需要指定语言顺序时使用 `pack.block(".other.monster", "en", "zh_cn")`。`pack.block("schema")` 等内部块不加载池；附加语言资源使用 `pack.attach(resource...)`，返回新包。生成类型按需 import `LoadValueErrs`，正文使用短类名，同名冲突时使用全限定名。


```java
try {
    ConfigMgrLoader.loadPackagesAndSetMgr(pack);
} catch (ConfigErr failure) {
    for (var error : failure.getErrors()) {
        if (error instanceof LoadValueErrs.MissingReference ref) {
            System.err.println(ref.source() + "." + ref.field() + " -> " + ref.target() + " " + ref.key());
        }
    }
}
```
