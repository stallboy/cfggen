# C# 配置与 Mod 示例

在仓库根目录执行：

```powershell
.\app\gradlew.bat -p app fatJar
.\example\cs\gencs.bat
.\example\cs\run.bat
```

`gencs.bat` 从 `../config` 生成基础配置与 C# 类型，再使用共享 schema 从 `../config_mod` 导出增量包 `demo-mod.bytes`。运行包保留 XOR `xyz`；`Program.ReadPackage` 先解码再交给 `ConfigPackage.Read`。构建 Mod 时使用的未加密依赖位于 `obj/base-pack/base.bytes`。

核心加载过程：

```csharp
var original = Processor.LoadPackagesAndSetMgr(basePack);
var modified = Processor.LoadPackages(basePack, mod);
var oldMonster = DMonster.Get(original, 1);
var modMonster = DMonster.Get(modified, 1);

ConfigMgr.SetMgr(modified);
var added = DMonster.Get(1001);
var oldRank = DRank.Green.Info(original);
var modRank = DRank.Green.Info();
```

示例展示替换、新增、未修改记录、跨包引用、固定 enum 详情缓存及默认实例切换，并输出：

```text
monster 1: base x=1, mod x=100
monster 2 unchanged: x=33
green: base=中品, mod=Mod Green
task 1001: [Mod hunt, Defeat the mod monster], target monster=1001, exp=9000
monster 1 sources=[demo-mod]
monster 1001 sources=[demo-mod]
```

`origins` 只记录 Mod 的新增与覆盖，省去基础包每条记录的来源项。未改动的基础记录和不存在的记录，`explain` / `Explain` 均返回空列表；记录是否存在通过表查询判断。多个 Mod 的来源按加载顺序排列。

数据目录说明和无需重新编译的 Mod 导出命令见 [config_mod/README.md](../config_mod/README.md)。
