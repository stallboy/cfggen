# Record 内部对象引用验收示例

本模块使用火球术状态机、施法指令、连招奖励和战斗事件时间轴，覆盖[内部对象引用设计](../../../docs/src/content/docs/core/internal-reference-design.md)的各类寻址、源键和目标键。所有提交的数据均可通过校验，并与 `example/config` 原有的装备、任务和 AI 配置一起参与验收。

## 数据与覆盖范围

| 文件 | 游戏含义 |
| --- | --- |
| `combat.cfg` | 技能状态机、施法和事件的类型及引用声明 |
| `_ability/1001.json` | 火球术的 Combat、Movement 两套状态机；各自持有自己的 Phase |
| `_cast/1.json` | 一次施法；技能 ID 在上层 Binding，机器键在中层，阶段键在叶节点 |
| `_phasebook/starter.json` | 按学派、阶数、空中状态选择招式，另有代号与版本的唯一键 |
| `_branchreward/hit.json` | 以完整 PhaseKey 选择命中分支的奖励 |
| `timeline.csv` | CSV pack 事件映射；map value 为 Damage、Heal、None 多态效果 |
| `_encounter/1.json` | 递归嵌套的行为状态机；Jump 按最近的 StateMachine 绑定 |

| 设计情况 | 本模块中的声明与数据 |
| --- | --- |
| 本 record 起点、多层 list | `ability.initial ->::machines.phases`，依次使用机器和阶段键 |
| 外部 record 主键 | `PhaseUse.target ->ability::machines.phases` |
| 外部 record 唯一键 | `PhaseUse.byCode ->ability[code]::machines.phases` |
| 最近祖先与当前实例自身 | `Phase.next` 与 `Machine.initial` 都使用 `->@Machine::phases` |
| 指定 interface 实现为祖先 | `Command.Jump ->@Command.StateMachine::phases`；内层未命中不回退到外层 |
| 当前对象字段源键 | `Machine` 的各个行内引用，以及 `PhaseUse.phaseKey` |
| 当前对象固定成员源路径 | `AbilityUse.ability` 的 `[binding.abilityId]`，目标为普通表 record |
| 源根 record 字段 | `PhaseUse.byCode` 的 `::abilityCode`；跨 record 查找仍使用施法记录中的值 |
| 祖先字段及固定成员源路径 | `PhaseUse.target` 混用 `@AbilityUse::binding.abilityId`、`@MachineUse::machineKey` 和 `phaseKey` |
| 零源键、固定 struct 终点 | `ability.spawn ->::origin`；`PhaseUse.origin` 仍指向源施法 record 的 origin |
| 固定成员中间路径、interface 终点 | `Phase.exit ->@Machine::onExit.effect` |
| 五种 list 索引键 | `phases/impacts/frames/stances/variants` 分别使用 str、int、long、bool、扁平 struct 键 |
| 复合 list 索引 | `combos` 的 `weapon,tier,airborne`，保留键顺序 |
| 五种 map 自然键 | `timeline` 的 `events/channels/slots/clock/modes` 分别使用 struct、str、int、long、bool 键 |
| 复合表主键、复合唯一键 | `cast.book`、`cast.bookByCode` 分别指向同一个招式 entry |
| struct 表主键 | `cast.reward` 使用一个完整的 PhaseKey，按内容匹配 |
| 新源路径、普通表级终点 | `AbilityUse.ability` 与 `cast.bookRecord`，保留整个目标 record 的身份 |
| 整条引用 nullable | `cast.optional` 分别缺少 record、机器、阶段；第四项使用合法空字符串命中阶段 |
| 合法的 false、0、空字符串 | 默认连招键 `("",0,false)`；int/long/bool 键也包含零或 false |
| 同名键与循环引用 | 两台机器各有 Idle；Combat 的 Idle→Hit→Idle；Movement 缺少 Hit，不能借用 Combat 的阶段 |
| 相同值、空对象与包含身份 | 两个 Anchor、多个空 AnchorUse 和 None 效果保留不同包含位置的身份 |
| 声明后未被引用的索引 | `Machine.markers` 仍须检查局部唯一性 |
| tag 依赖裁剪 | `Combo.tier` 带 server 标记；`-server` 移除整套复合索引及其依赖引用 |

其中 `9007199254740993` 刻意超过 JavaScript number 的精确整数范围，用于验证 long 寻址不会误命中相邻键。空字符串阶段与空武器表示默认分支，0 阶和 false 表示基础、地面状态；它们都是实际键值。

## 自动验证

在仓库根目录执行：

```powershell
java --sun-misc-unsafe-memory-access=allow -jar app/build/libs/cfggen.jar -datadir example/config -gen verify
```

`app/src/test/java/configgen/value/CombatReferenceExampleTest.java` 直接复制本模块运行 15 项测试，检查目标对象身份、局部作用域、键类型、nullable 和裁剪，并在临时副本中构造九种非法变体：未使用索引重复、必需目标缺失、祖先缺失、record 未命中时仍缺少源作用域、struct map key 重复、struct 键类型错误、容器源值、无索引 list、穿过 interface。预期失败的数据不会写回示例目录。

`InternalReferenceFixture` 同样复制本模块，供 Java、C#、TypeScript、Go、Lua 和 Godot 的实际生成、编译与加载测试使用。Lua 还会用完整 `example/config` 运行原有 `example/lua/test.lua`，验证 Metadata、增删、旧外键与空容器只读行为。

在 `app` 目录执行全部运行时验收：

```powershell
.\gradlew.bat test dotnetTest otherRuntimeTest godotTest luaTest
```

这些任务需要相应语言的工具链；路径配置见[实现交付记录](../../../docs/src/content/docs/core/internal-reference-implementation.md)。常规示例生成前先运行 `app/gradlew.bat fatJar`，再运行各语言目录的生成脚本，更新代码、数据及运行库。

TypeScript 的三个示例均使用 ES2020，生成的 long 字段和查询键为 bigint。Lua 示例要求支持 64 位整数的 Lua 5.3+；仓库旧的 `example/lua/lua.exe` 是 Lua 5.1，不能运行本模块。两个 Lua `run.bat` 默认查找 PATH 中的 `lua54`，也可以先配置实际可执行文件路径：

```powershell
$env:CFGGEN_LUA = 'C:\tools\lua54.exe'
```

Lua 的复合键及 struct 键通过 `get(...)` 按完整内容查询。`all` 可用于遍历，不依赖其复合键的内部编码。
