# Java 配置与 Mod 示例

生成器、生成的 Java 代码和支持源码统一使用 JDK 25；示例 Gradle 工具链也是 JDK 25。

在仓库根目录运行：

```powershell
.\app\gradlew.bat -p app fatJar
.\example\java\genjava.bat
.\example\java\run.bat
```

`genjava.bat` 使用 `app/build/libs/cfggen.jar`，更新此示例的生成类型和支持源码，导出基础包 `base.bytes`，再从 `../config_mod` 导出增量包 `demo-mod.bytes`。两者使用同一结构与 `own:-noserver` 字段筛选；Mod 只生成数据。

`LoadConfig.java` 的加载入口是：

```java
ConfigPackage pack = ConfigPackage.read(Path.of("base.bytes"));
ConfigMgrLoader.loadPackagesAndSetMgr(pack);
```

`ConfigPackage.read(Path)` 直接读取并解析文件，无需手动创建读取器或关闭资源。

加载器检查包版本、依赖和 schema，建立索引、解析引用并初始化扩展后，才发布默认实例。随后 `showMod` 按显式顺序加载基础包和 Mod：

```java
ConfigMgr original = ConfigMgr.getMgr();
ConfigMgr modified = ConfigMgrLoader.loadPackages(base, mod);
Monster oldMonster = original.getOtherMonster(1);
Monster modMonster = modified.getOtherMonster(1);

ConfigMgr.setMgr(modified);
Monster added = Monster.get(1001);
var oldRank = Rank.GREEN.ref(original);
var modRank = Rank.GREEN.ref();
```

示例展示替换、新增、未修改记录、跨包引用、固定 enum 详情缓存及默认实例切换，基础实例仍可独立查询。运行输出包含：

```text
base.bytes version=1, metadata={defaultLanguage=default, pack=base}
monster 1: base x=1, mod x=100
monster 2 unchanged: x=33
green: base=中品, mod=Mod Green
task 1001: [Mod hunt, Defeat the mod monster], target monster=1001, exp=9000
monster 1 sources=[demo-mod]
monster 1001 sources=[demo-mod]
```

`origins` 只记录 Mod 的新增与覆盖，省去基础包每条记录的来源项。未改动的基础记录和不存在的记录，`explain` / `Explain` 均返回空列表；记录是否存在通过表查询判断。多个 Mod 的来源按加载顺序排列。

执行 `.\example\java\repl.bat` 可交互查看：

```text
bytes eq
get other.monster 1
get other.monster 1001
schema monster
q
```

`bytes` 读取新文件，`get` / `query` 查看已加载实例；查看器支持只读索引和 seq 列表。

Mod 数据和只重新导出数据、不编译业务代码的命令见 [config_mod/README.md](../config_mod/README.md)。REPL 的对象查询使用组合实例，`bytes` 仍查看基础包文件。

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
