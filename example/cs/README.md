# C# 配置与 Mod 示例

在仓库根目录执行：

```powershell
.\app\gradlew.bat -p app fatJar
.\example\cs\gencs.bat
.\example\cs\run.bat
.\example\cs_ls\gencs_ls.bat
.\example\cs_ls\run.bat
.\example\cs_ls_client\gencs_ls_client.bat
.\example\cs_ls_client\run.bat
```

三个项目均以 .NET 10 作为示例运行宿主，并将编译警告视为错误。`cs_ls_client` 保留 `LangVersion=9.0` 和 `unity` 生成选项，验证生成代码的 C# 9 语法兼容性；宿主框架版本不代表 Unity 的运行时版本要求。脚本会定位到自身目录，可以从仓库根目录直接调用。

| 示例 | 文本模式 | 运行验证 |
| --- | --- | --- |
| `cs` | 默认文本 | 基础配置、Mod、独立快照、默认实例切换、enum 详情和来源 |
| `cs_ls` | 服务端多语言文本 | 中文和英文同时读取 |
| `cs_ls_client` | 客户端动态语言 | 同一个文本对象切换中文、英文，再切回中文 |

三个入口均调用共享的 `ExampleChecks.cs`。它检查旧配置的 record 外键与 enum 详情，以及 `combat` 的 record 内部引用：五种 list 索引键、五种 map 键、混合祖先来源、复合和 struct 主键、固定成员、循环引用、合法空键、nullable、空对象身份和最近接口作用域。任一断言失败都会使运行脚本返回非零状态。

生成脚本会先更新示例使用的 `Config/Loader.cs` 运行库，再生成类型和数据。生成器在应用项目中默认保留已有运行库，升级后应同步该文件；示例脚本自动完成这一步。

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
