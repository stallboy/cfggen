# 增量 Mod 配置示例

此目录使用 `../config` 中的全部结构定义，只提供自己修改或新增的数据，不复制 `.cfg` 文件。导出包名为 `demo-mod`，声明依赖 `base`；这些信息通过生成参数写入 `demo-mod.bytes` 的 metadata。

| 数据文件 | 改动 |
| --- | --- |
| `other/monster.csv` | 整条覆盖怪物 1，首个位置改为 `(100,200,300)`；新增怪物 1001，引用基础包的掉落数据 |
| `equip/rank.csv` | 只覆盖 `green` 的详情，将显示名改为 `Mod Green`；保留符号名和 ID 1 |
| `task/task.csv` | 新增任务 1001，目标为怪物 1001，后续任务引用基础包的任务 1 |

没有提供的表和记录继续使用基础包数据，例如怪物 2。相同主键按整条记录替换，CSV 每一行仍须包含这条记录所需的全部字段。

在仓库根目录首次构建并运行：

```powershell
.\app\gradlew.bat -p app fatJar
.\example\java\genjava.bat
.\example\java\run.bat
.\example\cs\gencs.bat
.\example\cs\run.bat
```

两个生成脚本分别导出各自的基础包和 `demo-mod.bytes`，Mod 只生成数据。Java 使用 `own:-noserver`；C# 使用完整结构并保留原有 XOR `xyz` 示例，因此分别构建匹配的包。C# 构建用的未加密基础包位于被忽略的 `example/cs/obj/base-pack/base.bytes`。

修改本目录数据后，可只重新导出 Mod 并运行已有程序，不重新生成配置类或编译业务代码。

Java，在 `example/java` 目录执行：

```powershell
java --sun-misc-unsafe-memory-access=allow -jar ../../app/build/libs/cfggen.jar -datadir ../config_mod -schema ../config -gen bytes,own:-noserver,pack:demo-mod,requires:base,deps:base.bytes
java -jar build/libs/java.jar
```

C#，在 `example/cs` 目录执行：

```powershell
java --sun-misc-unsafe-memory-access=allow -jar ../../app/build/libs/cfggen.jar -datadir ../config_mod -schema ../config -gen bytes,pack:demo-mod,requires:base,deps:obj/base-pack/base.bytes,cipher:xyz
dotnet run --no-build
```

`-schema ../config` 固定共享结构；`deps` 提供构建时需要的基础数据，以验证组合后的引用和固定符号；导出文件只包含本目录的 4 条记录。基础配置发生变化时，先重新运行对应示例的完整生成脚本，更新基础包和构建依赖。

加载时显式传入顺序 `base, demo-mod`。两个示例保留基础实例，用实例接口查询原数据；构建组合实例不会自动切换默认配置，调用 `ConfigMgr.setMgr` / `SetMgr` 后，静态查询才访问 Mod 数据。Java 的 REPL 也默认查看组合实例，例如 `get other.monster 1001`。
