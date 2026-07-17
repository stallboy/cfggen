---
title: 数据基础 Data Foundation
sidebar:
  order: 1
---

定义系统的"词汇表"——标签、属性、事件、表现键、变量的原子定义。

---

## gameplaytag

Tag 字典注册表——系统的**语义分类骨架**。gameplaytag 是带层级关系的字符串标签，用于把"这个实体是什么、处于什么状态、这次结算带了什么"分类表达。

gameplaytag 的核心特性是**层级查询**：标签名以 `.` 分隔（如 `Actor.Debuff.Control.Stun`），启动期自动构建父子索引（`ancestors` 字段），因此查 `Actor.Debuff` 能一键命中所有减益。但**层级查询是 gameplaytag 的能力，不是它的准入门槛**——`Combat.*` 这类标签大多被精确匹配消费（`requireAll: ["Combat.Result.Critical"]` 逐个点名），很少用到父级查询，却仍属于 gameplaytag。真正区分的是**消费方式**：gameplaytag 里的东西被当"标签语义"挂载与查询命中（`requireAll`/`requireAny`/`grantedTags`/`PayloadHasTag` 等）；而 `event_definition`/`stat_definition`/`resource_definition`/`var_key`/`cue_key` 走**外键名引用**（`->xxx`），各有专属引用机制，故不进 gameplaytag（其中 `Cue` 还额外因表现层与逻辑层隔离不参与 TagQuery）。

```cfg
table gameplaytag[tag] {
    tag:str; // 例: "Actor.Debuff.Control.Stun"
    tagId: int;
    description: text;
    ancestors: list<int> ->gameplaytag[tagId]; // 策划不用填，程序初始化时自动构建
    [tagId];
}
```

### 命名规范

**总原则**：

1. **前缀（第一段）= 主体域**：标签描述的是"谁"。一看前缀即知归属。
2. **第二段 = 性质分组**：域内按性质分类，便于 `ancestors` 父级查询与 `TagQuery` 命中（查 `Actor.Debuff` 一键命中所有减益）。

#### 主体域总表

| 前缀 | 主体 | 语义维度 | 标签挂在哪 | 例子 |
|---|---|---|---|---|
| `Actor.*` | Actor（运行时实体） | **动态局面**（运行时生灭） | Actor 的 TagContainer | `Debuff.Control.Stun` |
| `Char.*` | Actor 的**固有身份** | **固有属性**（配置期定，基本不变） | Actor 的 TagContainer | `Role.Player`、`Role.Enemy` |
| `Status.*` | Status 配置自身 | 固有分类（"它是什么"） | Status 配置（statusTags） | `Type.DOT`、`Purge.Curse` |
| `Ability.*` | Ability 配置自身 | 固有分类（"它是什么"） | Ability 配置（abilityTags） | `Type.Spell`、`Cancel.Move` |
| `Combat.*` | 一次结算事件（Payload） | 结算属性（"这击带了什么"） | Payload | `Result.Critical` |

> **`Actor.*` 与 `Char.*` 的区别（关键）**：两者都挂在 Actor 的 TagContainer，但语义维度正交——
> - `Char.*` 是**固有身份**（这个 Actor"是什么"：玩家/敌人/种族/职业），配置期确定，基本不随战斗变化。
> - `Actor.*` 是**动态局面**（这个 Actor"现在怎么了"：中毒/施法中/移动中），运行时频繁生灭，由 `Status.grantedTags` 推送或 `GrantTag` 直挂。


#### 各域二级规范

**`Actor.*` — Actor 动态局面**

| 二级 | 含义 | 例子 |
|---|---|---|
| `Actor.Buff.*` | 增益局面 | `SuperArmor`（霸体）、`ShieldWall`、`Invincible`、`Purify`（净化） |
| `Actor.Debuff.*` | 减益局面（控制统一归 `Control`） | `Poison`、`Burn`、`Slow`、`Vulnerable`（易伤）；`Control.Stun`/`Silence` |
| `Actor.Cast.*` | 施法生命周期阶段 | `Startup.Spell`、`Charging`、`Channel`、`Casting`、`Recovery` |
| `Actor.Motion.*` | 运动行为 | `Moving`、`Dodging`、`Immobile` |
| `Actor.Life.*` | 生死 | `Dead`（`combat_settings.deadTag` 的统一锚点） |
| `Actor.Lockout.*` | 锁定 | `Ability` |
| `Actor.Passive.*` | 被动标记 | `ThornArmor`（反甲）、`LifeSteal`（吸血）、`CritFollowup`（暴击追伤） |
| `Actor.Scene.*` | 被场景演出接管 | `Controlled`（`WithActorControl` 打此 tag，AI 识别后挂起） |
| `Actor.Phase.*` | Boss 战斗阶段 | `Enrage`（狂暴） |

**`Char.*` — Actor 固有身份**

| 二级 | 含义 | 例子 |
|---|---|---|
| `Char.Role.*` | 阵营/角色类型 | `Player`、`Enemy` |
| `Char.Race.*` | 种族（可扩展） | `Goblin` |
| `Char.Class.*` | 职业（可扩展） | `Warrior` |

> `Char.*` 主要用于 `TargetScan.tagQuery` 区分目标（如"只扫玩家"）。它与 `TargetScan.relation`（`Self`/`Friendly`/`Hostile`/`Neutral`，运行时阵营关系枚举）正交——`relation` 是物理关系计算，`Char.*` 是配置身份标记。

**`Status.*` — Status 固有分类（statusTags）**

| 二级 | 含义 | 例子 |
|---|---|---|
| `Status.Type.*` | 机制类型 | `DOT`（持续伤害）、`HOT`（持续治疗）、`Shield`、`Aura`（光环）、`Stance`（姿态） |
| `Status.Purge.*` | 驱散等级（配合 `tag_rules.purgesTags` / `PurgeStatusByTag`） | `Basic`、`Curse`（诅咒）、`Undispellable`（不可驱散） |

> **statusTags 与 grantedTags 分工（Buff/Debuff 不在 statusTags 复刻）**：`Status.Type.*` 只描述"机制类型"（DOT/HOT/Shield...），**不另设 Buff/Debuff 分类**——这一维由 `grantedTags`（即 `Actor.Buff.*`/`Actor.Debuff.*`）表达。因为 `Effect.PurgeStatusByTag` 同时提供 `matchStatusTags`（按"它是什么"驱散）与 `matchGrantedTags`（按"它造成什么局面"驱散），要驱散所有减益直接用 `matchGrantedTags` 命中 `Actor.Debuff` 即可，无需在 statusTags 再维一份 Buff/Debuff。两轴正交、各管一类查询，单一职责不重复。

**`Ability.*` — Ability 固有分类（abilityTags）**

| 二级 | 含义 | 例子 |
|---|---|---|
| `Ability.Category.*` | 大类 | `AutoAttack`、`Ultimate`（大招） |
| `Ability.Type.*` | 形态/触发类型 | `Spell`、`Melee`、`Ranged`、`Movement` |
| `Ability.Cancel.*` | 可取消性 | `Move`（前摇可被移动软取消） |

**`Combat.*` — 结算事件标签（Payload）**

| 二级 | 含义 | 例子 |
|---|---|---|
| `Combat.DamageType.*` | 伤害元素/类型（造成伤害的属性） | `Fire`、`Ice`、`Poison`、`Lightning`、`Holy`、`Dark`、`Physical` |
| `Combat.EffectType.*` | 非伤害结算效果（治疗/护盾吸收） | `Heal`、`Shield` |
| `Combat.Origin.*` | 攻击形态（怎么打的） | `Melee`、`Ranged`、`Burst`、`Area`、`AoE`、`Blunt`（钝击）、`Pierce`（穿刺） |
| `Combat.Flags.*` | 成因标记（这次结算怎么来的） | `Reflected`（反伤）、`Bonus`（追伤）、`Proc`（被动触发） |
| `Combat.Result.*` | 判定结果 | `Critical`（暴击）、`Dodged`（闪避）、`Blocked`（格挡）、`Miss` |

> **DamageType vs EffectType vs Origin vs Flags**：四者并列、语义正交——
> - `DamageType.*` = **伤害的元素属性**（这一击是火/冰/物理…）。
> - `EffectType.*` = **非伤害结算效果**（治疗、护盾吸收）。
> - `Origin.*` = **攻击形态**（近战/远程/爆发/范围），描述"怎么打的"。
> - `Flags.*` = **成因标记**（反伤/追伤/触发），描述"这次结算怎么来的"。


---

## stat_definition

属性的原子定义表。一个 stat 是一个**有上下界的数值量**（如 `Attack`、`MoveSpeed`、`HP_Max`），供 Ability / AI / 场景演出通过 `stat: str ->stat_definition` 引用。运行期由 `StatComponent` 持有每个 Actor 的属性实例与修饰链，本表只定义"这个属性叫什么、默认多少、边界在哪"。

```cfg
table stat_definition[statKey] (enum='statKey') {
    statKey: str;
    statId: int;
    description: text;
    defaultValue: float;

    // 静态硬边界
    hardMin: float;
    hardMax: float;
    [statId];
}
```

- `statKey` 是全系统引用键（配置里写 `->stat_definition` 用的就是它）；`statId` 是压缩后的整数 id，运行期容器用它做数组下标。
- `hardMin` / `hardMax` 是该属性的**物理硬边界**——任何修饰叠加后都 clamp 在此区间，防止配置错误产生负攻击力或溢出。它区别于 `resource_definition.clampMax`（资源叠满的上限）。
- 命名约定：`HP_Max` / `HP_Current` 这类后缀区分"上限属性"与"当前量"，`resource_definition` 据此建立 clamp 联动（见下节）。

---

## resource_definition

资源的原子定义表。一个 resource 是一个**会消耗/回复、有"当前量"语义的量**（如 `HP`、`MP`、怒气），由 `ResourceComponent` 持有运行期实例。它与 `stat_definition` 的区别：stat 是"属性数值"（攻击力、速度），resource 是"池子里的余量"（生命值、蓝量）。资源的关键特性是**与属性联动**——上限 clamp、归零触发业务态。

```
table resource_definition[resKey] (enum='resKey') {
    resKey: str;
    resId: int;
    description: text;

    // 上限联动：clampMax 指向某个 stat，资源当前量永远不超过该 stat 的值。
    // 例：HP_Current 的 clampMax 指向 HP_Max。
    // 当 HP_Max 变化时，HP_Current 自动 clamp 到新上限。
    // 反向不联动：当 HP_Max 降低导致越界时，HP_Current 不做变化——降上限不直接扣血，是业务逻辑而非物理特性。
    clampMax: str ->stat_definition;

    // 归零联动（资源自身的物理特性）：当 currentValue 降至 0 时，自动向宿主挂载的 Tag。
    // 例: HP 归零 -> 挂 "Actor.Life.Dead"
    // 注意：挂 Dead tag 不等于对象回收，见 runtime-lifecycle §存在性 vs 业务生死。
    onDepletedGrantTag: str ->gameplaytag (nullable);
    [resId];
}
```

- `clampMax` 建立了 resource → stat 的单向依赖：resource 读取 stat 作上限，配置期必须保证引用的 stat 存在（`HP_Max`）。
- `onDepletedGrantTag` 是"资源归零"到"业务生死"的桥梁——它在 `Actor.Life.Dead` 这类 tag 上落地，把数值态翻译成 tag 态，之后业务再决定是否走 `killActor`。

---

## event_definition

定义系统中所有可用的事件类型（如 `Damage_Pre`、`Damage_Post`、`OnHit`）。事件是 GAS 的解耦骨干——Ability / Status / Effect / 场景演出通过 `event: str ->event_definition` 订阅与广播事件，彼此不直接调用。一个事件携带一个 `magnitude`（主数值，如伤害量）外加一组 `extras`（附加变量），本表的职责就是**登记事件名 + 声明它携带哪些附加变量**。

```cfg
table event_definition[eventKey] (entry='eventKey') {
    eventKey: str; // 如 "Damage_Pre"
    eventId: int;
    description: text;

    // 声明该事件 Payload 中预期的附加变量（除 magnitude 外）。
    // 用途：编辑器提示与契约校验——告知该事件在 extras 里还会塞入哪些变量、什么类型。
    expectedVars: list<EventVarDecl>;
    [eventId];
}

// 一个附加变量的声明：名字指向 var_key，类型告知读取方如何解释。
struct EventVarDecl {
    varKey: str ->var_key;
    type: VarType;
    description: text;
}

enum VarType {
    Float;  // 数值量（如伤害系数）
    Actor;  // Actor 引用（如伤害来源、目标）
}
```

- `eventKey` 是订阅/广播时引用的逻辑名；`eventId` 是压缩整数 id。
- `expectedVars` 不参与运行期派发逻辑，它是**契约说明**：声明"`Damage_Pre` 会带一个 Float 型的 `DamageAmount` 和一个 Actor 型的 `Source`"，让配置者在写 Trigger 时知道能读哪些变量。运行期 `extras` 实际塞什么由派发方决定，配置校验期对照本表检查一致性。

---

## cue_key

表现意图的**逻辑命名表**。Ability / Status / 场景演出不直接引用具体特效/音效资产，而是引用一个 `cueKey`（如 `"Hit.Fireball"`），由表现层在注册表里翻译为具体资源请求（见 [Cue 表现系统](./cue-design.md)）。这是"瘦配置、胖资产"的落点——逻辑层只管"发生了什么意图"，资产怎么播归表现层。

cue_key 按**生命周期**分两张表，对应 Cue 的双轨：

```
table cue_key_pulse[cueKey] {
    cueKey: str -> cue_registry_pulse (nullable);
    cueId: int;
    description: text;
    [cueId];
}

table cue_key_state[cueKey] {
    cueKey: str -> cue_registry_state (nullable);
    cueId: int;
    description: text;
    [cueId];
}
```

- **`cue_key_pulse`（脉冲/瞬时轨）**：fire-and-forget 的瞬时事件（命中溅血、爆炸、飘字、一击动作）。逻辑层 `FireCue(cueKey)` 触发一次即结束。
- **`cue_key_state`（状态/持续轨）**：宿主绑定、随 Status/Ability 同生共死的持续表现（Buff 光环、引导施法、循环音）。Status 用 `cuesWhileActive: list<str> ->cue_key_state` 挂载，存在期间持续播放。

两张表都引用各自注册表（`->cue_registry_pulse/state`），`nullable` 的含义：**cue_key 是逻辑命名，可以先登记、晚于注册表填充**——配置阶段允许一个 cueKey 暂时没有对应的资源注册（表现未做出来时逻辑层照常引用），运行期命中空注册则跳过。这把"逻辑意图"与"表现资源"彻底解耦。

---

## var_key

变量的**命名槽注册表**。GAS 里大量需要"读写一个具名数值/引用"的场景——技能计数器（第几次触发）、上下文变量（`ContextVar`）、结算载荷变量（`PayloadVar`）——这些变量不在配置里就地命名，而是统一指向 `var_key` 里登记的条目。本表是这些变量名的唯一来源，避免拼写漂移与跨配置不一致。

```
table var_key[varKey] {
    varKey: str;
    varId: int;
    description: text;
    [varId];
}
```

- 一个 `varKey` 就是一个"变量名 + 类型槽"的声明，运行期由 `localScope`（Effect 树局部）/ `instanceState`（Ability 实例级）/ `combat_settings`（全局）按分层查找读写（见 [施法生命周期](./ability-cast.md) 的 ContextVar 解析）。
- 典型用途：
  - **周期/重复计数器**：`Periodic.indexVarTag`、`Repeat.indexVarTag` 指向一个 var_key，记录"这是第几次触发"，写入 localScope 供 effect 内 `ContextVar` 读取。
  - **跨段传值**：`ContextVar` / `PayloadVar` 通过 var_key 在 Effect 树各层与事件 Payload 间传递数值（如把伤害量、目标 Actor 传给下游）。

