# Record 内部对象引用

内嵌对象由父 record 统一维护，但其他对象需要稳定地引用它时，使用内部引用。只有确实需要独立维护、共享发布的对象才拆成表。目标路径写在 schema，数据只提供键，不保存路径字符串或数组位置。

## 完整示例

```cfg
struct Origin { x:int; y:int; }
struct Phase {
    key:str;
    next:str ->@Machine::phases (nullable);
}
struct Machine {
    code:str;
    phases:list<Phase> (index='key');
    initial:str ->@Machine::phases;
}
table ability[id] (json) {
    [code];
    id:int; code:str; origin:Origin;
    machines:list<Machine> (index='code');
    machineKey:str; phaseKey:str;
    ->initial:[machineKey,phaseKey] ->::machines.phases;
    ->spawn:[] ->::origin;
}
struct Binding { abilityId:int; }
struct PhaseUse {
    phaseKey:str;
    ->target:[@AbilityUse::binding.abilityId,@MachineUse::machineKey,phaseKey]
        ->ability::machines.phases;
    ->byCode:[::abilityCode,@MachineUse::machineKey,phaseKey]
        ->ability[code]::machines.phases;
    ->origin:[] ->::origin;
}
struct MachineUse { machineKey:str; targets:list<PhaseUse>; }
struct AbilityUse {
    binding:Binding; machines:list<MachineUse>;
    ->ability:[binding.abilityId] ->ability;
}
table cast[id] (json) {
    id:int; abilityCode:str; origin:Origin; uses:list<AbilityUse>;
}
```

`PhaseUse.target` 从两层祖先和当前对象取得三个源值：记录主键、机器键、阶段键。`byCode` 用源 cast 的 `abilityCode` 查唯一键；读取目标 record 不会改变左侧 `::` 的含义。`origin` 始终指源 cast 的固定对象。`AbilityUse.ability` 则演示固定成员源路径与普通表级终点。

## 选择规则与限制

| 场景 | 写法 |
| --- | --- |
| 普通表主键、唯一键 | `->ability`、`->ability[code]` |
| 本 record 内部对象 | `->::machines.phases` |
| 跨 record 内部对象 | `->ability::machines.phases` |
| 跨 record 唯一键 | `->ability[code]::machines.phases` |
| 最近祖先（包含当前实例） | `->@Machine::phases` |
| 指定接口实现祖先 | `->@Command.StateMachine::phases` |
| 当前字段、固定成员源值 | `phaseKey`、`binding.abilityId` |
| 源根、祖先源值 | `::abilityCode`、`@AbilityUse::binding.abilityId` |
| 固定对象，没有消耗键 | `->spawn:[] ->::origin` |

- `index` 声明在 `list<明确 struct>` 字段上，每个集合实例内唯一；不同机器可以各有 `Idle`，同一机器不能重复。未被引用的索引也要检查。每个 list 字段只有一套索引，复合形式如 `(index='weapon,tier,airborne')`，键顺序有意义。
- `map<K,V>` 使用自然 key，不声明 `index`。键可为 bool/int/long/str，或仅含这些成员的扁平 struct。单个 struct 键消费一个同 schema 类型的完整值，不按形状匹配，也不自动展开。复合索引只能由基本类型字段组成。
- 每经过一个 list/map 都消费相应键，固定 struct 字段不消费键。终点必须是 struct/interface；interface 可作终点，不能穿透它的具体实现。`list<interface>` 不支持 `index`。
- `@Type` 只沿包含关系选择最近匹配实例，缺少目标时不回退到外层同类型对象，也不沿已有外键寻找祖先。实际数据缺少所需祖先始终是错误。
- 源路径中间只能经过固定 struct，不能选集合元素、穿透 interface 或沿派生引用。源键数量和类型必须与目标逐层匹配。
- 新引用的 `nullable` 允许 record、中间集合或最终目标不存在；声明错误、缺失祖先、重复键仍报错。`0`、`false`、空字符串是正常键值，存在就命中。旧语法 nullable 保留历史规则，不通过自动改写旧来源形式改变语义。
- 目标内部路径只适用于单目标 `->`，不扩展 `=>`。需要多个对象时用 `list<引用struct>`，每个元素独立绑定。派生引用不写进 JSON，也不为绕过校验伪造目标。

在 cfggen 仓库内，`example/config/combat` 提供这些类型的实际游戏数据及六种运行时验证；用户项目不一定带有该目录。修改 schema 后先执行现有配置校验。格式化仅调整源码空白，不能作为修复无效路径或迁移旧引用语义的手段。
