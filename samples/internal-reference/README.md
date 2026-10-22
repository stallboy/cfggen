# Record 内部引用示例

从仓库根目录运行当前构建的生成器：

```powershell
java -jar app/build/libs/cfggen.jar -datadir samples/internal-reference -gen verify -gen bytes,dir:build/reference-bytes -gen java,dir:build/reference-java,pkg:game.config
```

`config.cfg` 中的 `PhaseTarget` 分别从 `AbilityUse`、`MachineUse` 和自身读取三个源键，直接引用 ability record 内的 Phase。两台机器都含有 `Idle`，但各自的 list 索引只在所属集合内唯一。

Combat 的 `Idle → Hit → Idle` 是合法引用环。Movement 的 `nextPhaseKey` 为空字符串，没有匹配 Phase，因此它的 nullable 引用为空。它不会回退到 Combat 的 `Idle`。

Java 生成代码保留原始 phaseKey，并通过 `refTarget()` 返回 Phase；`nullableRefNextPhaseKey()` 返回可能为空的 Phase。其他语言生成器也支持相同声明。含新关系的数据包使用版本 2，需要同步更新生成代码与运行库。

Godot 使用 WeakRef 保存派生引用。调用方须在使用配置期间保留 `ConfigProcessor.load_from_package(...)` 返回的 ConfigStream，以持有整个配置快照。

完整规则见 [内部对象引用设计](../../docs/src/content/docs/core/internal-reference-design.md)。
