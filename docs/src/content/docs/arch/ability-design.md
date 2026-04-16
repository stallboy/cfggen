---
title: 能力系统设计
sidebar:
  order: 0
---

基于 Unreal GAS 核心理念的**全数据驱动**技能系统配置标准。本文档是**架构基准**，具体游戏应在此基础上裁剪和扩展。


## Architecture Overview

> 本节提供系统的全局视图。建议首次阅读时先通读本节建立心智模型，再进入后续各章的详细定义。

---

### 核心概念速览

| 概念 | 职责 | 生命周期 |
|------|------|----------|
| **Ability** | 行为入口 — 准入检查、资源消耗、触发 Effect | 瞬发执行 |
| **Effect** | 无状态原子指令 — 改属性、挂状态、走结算管线、生成投射物 | 即时执行，不持有任何状态 |
| **Status** | 持续状态容器 — 挂载在 Actor 上，管理时长、堆叠与标签授予 | 有限时长 / 永久，可堆叠 |
| **Behavior** | Status 内部的逻辑零件 — 属性修饰、周期脉冲、事件监听 | 随 Status 生死 |
| **EventBus** | Actor 级事件总线 — 广播携带 Payload 的标准事件 | Actor 组件，常驻 |
| **Pipeline** | 结算管线 — 将 ResolveCombat 展开为多阶段广播与属性扣减 | 单次结算，无持久状态 |
| **TagRules** | 全局交互法则 — 基于标签的免疫、阻止、驱散、打断 | 战斗配置，常驻 |

---

### 执行流全景

```
                         ┌───────────┐
                         │  Ability  │
                         └───────────┘
                               │ execute
                               ▼
            ┌───────────────── Effect ─────────────────┐
            │                    │                     │
     ApplyStatus            ModifyStat          ResolveCombat
            │                    │                     │
            ▼                    ▼                     ▼
     ┌────────────┐      ┌────────────┐        ┌────────────┐
     │   Status   │      │    Stat    │        │  Pipeline  │
     │ ┌────────┐ │      │ Component  │        └──────┬─────┘
     │ │Behavior│ │      └────────────┘          broadcast
     │ └───┬────┘ │                                   │
     └─────┼──────┘                                   ▼
           │                                   ┌────────────┐
      Trigger ─────── listen ────────────────▶ │  EventBus  │
                                               └──────┬─────┘
                                                      │
              ┌───────────────────────────────────────┘
              │ dispatch
              ▼
       Trigger.effect ── execute ──▶ Effect   ◄── 响应式循环


     Status ── grantTags ──▶ TagContainer ──▶ TagRules
                                              (免疫/阻止/驱散/打断)
```

整个系统的驱动核心是这条**响应式循环**：

1. **Ability** 或 **Behavior** 触发 **Effect**
2. Effect 可以走 `ResolveCombat` 进入结算 **Pipeline**
3. Pipeline 在 Pre / Post 两个阶段向 **EventBus** 广播事件（携带 **Payload**）
4. 其他 Status 中的 **Trigger** 监听到事件，执行新的 Effect
5. 新 Effect 可能再次进入管线 → 回到步骤 2

系统通过 `combat_settings.maxDispatchDepth` 限制嵌套深度，防止无限循环（如反伤互相触发）。


### 三层正交原则

系统中的三个实体类型严格正交，各司其职：

| | **Ability** | **Effect** | **Status** |
|---|---|---|---|
| **本质** | 入口 | 指令 | 容器 |
| **是否持有状态** |  | 否（无状态，即时消费） | 是（时长、层数、行为零件） |
| **与 Actor 关系** | | 不关联，只消费 Context | 挂载在 Actor 上 |
| **典型用途** | 主动技能、战术位移 | 扣血、加 Buff、发射投射物 | 持续增益/减益、被动技能、光环 |

**组合规则**：
- Ability 的 `effect` 字段触发 Effect 树
- Effect 中的 `ApplyStatus` 将 Status 挂载到目标身上
- Status 中的 Behavior（Trigger/Periodic/Timeline）在运行时触发新的 Effect
- **被动技能** 不走 Ability，本质是永久 Status（`duration: -1`）

这种正交分离确保每一层只关注自己的职责——Ability 不关心效果如何执行，Effect 不关心状态如何持续，Status 不关心谁触发了它。复杂机制通过**组合**而非继承构建。


### Philosophy

1. **层级化标签驱动**：树状 Tag（如 `State.Debuff.Control.Stun`）是逻辑交互的唯一通用语言。支持父级包含查询——查询 `State.Debuff` 可命中 `State.Debuff.Control.Stun`。

2. **两阶段事件管线**：所有逻辑通信通过 `EventBus` 广播标准消息。`Pre` 阶段注入 `Modifier` 篡改数值（如减伤），`Post` 阶段触发副作用（如反伤）。源与监听者彻底解耦。

3. **逻辑与表现隔离**：逻辑层仅输出 `CueKey`，客户端独立维护 Tag 到资源的映射。

4. **三层正交分离**：`Ability`（行为入口）、`Status`（持续状态）、`Effect`（无状态原子指令）严格正交。通过组合构建复杂机制。

---

## Data Foundation


定义系统的"词汇表"——标签、属性、事件的原子定义。

### gameplaytag

```cfg
// Tag字典注册表
// 层级关系通过名称中的 "." 分隔符隐式表达
// 程序启动时自动构建父子索引
table gameplaytag[tag] {
    tag:str; // 例: "State.Debuff.Control.Stun"
    tagId: int;
    description: text;
    ancestors: list<int> ->gameplaytag[tagId]; // 策划不用填，程序初始化时自动填
    [tagId];
}
```

**命名规范**：

| 前缀 | 用途 | 示例 |
|---|---|---|
| `State.*` | 实体状态标记 | `State.Debuff.Control.Stun` |
| `Status.*` | status分类标签 | `Status.Type.DOT` |
| `Ability.*` | ability分类标签 | `Ability.Type.Spell` |
| `Damage.*` | 伤害/治疗分类 | `Damage.Element.Fire` |
| `Combat.*` |	管线结果标记 & 战斗分类	| `Combat.Result.Dodged, Combat.Result.Critical` |

以上分类都可以想象`ancestors`或`TagQuery`的用武之地

`Event、Stat、Var`等都没有ancestors的需求，都无需是`gameplaytag`。
`Cue`则没有TagQuery的需求，不放`gameplaytag`里，这样表现层与逻辑层也更隔离。

### stat_definition

```cfg
table stat_definition[statKey] {
    statKey: str;
    statId: int;
    description: text;

    defaultValue: float; 
    isPersistent: bool;

    // 边界约束
    minLimit: StatLimit;
    maxLimit: StatLimit;

    // 级联策略
    // 当 minLimit 或 maxLimit 所依赖的属性发生变化时，自身如何同步？
    clampMode: StatClampMode;

    // 归零联动
    // 当 currentValue 降至 minLimit 时，自动向宿主挂载的 Tag
    // 例: HP 归零 -> 挂 "State.Dead"
    onDepletedGrantTag: list<str> ->gameplaytag;
    [statId];
}

interface StatLimit {
    struct Const { value: float; }
    // 引用另一个 stat_definition，在运行时去取它的 CurrentValue
    struct StatLink { stat: str ->stat_definition; }
    struct None {}
}

enum StatClampMode {
    Absolute;          // 绝对值截断 (如：MaxHP 从 100 降到 40，当前 HP 变为 40)
    MaintainPercent;   // 维持百分比 (如：MaxHP 变为原先的 2倍，当前 HP 也按比例乘以 2)
    None;              // 纯粹的自由属性，不进行任何联动调整
}
```

### event_definition

定义系统中所有可用的事件类型

```cfg
table event_definition[eventKey] (entry='eventKey') {
    eventKey: str; // 如 "Combat_Damage_Pre"
    eventId: int;
    description: text;

    // 定义该事件 Payload 中预期的变量列表 (可用于验证和编辑器提示)
    // 告知该事件除了 magnitude 以外，在 extras 里还会塞入哪些 Tag 数据
    expectedVars: list<EventVarDecl>;
    [eventId];
}

struct EventVarDecl {
    varKey: str ->var_key;
    type: VarType;
    description: text;
}

enum VarType {
    Float;
    Actor;
}
```

### cue_key

```
table cue_key_instant[cueKey] {
    cueKey: str -> cue_registry_instant (nullable);
    cueId: int;
    ancestors: list<int> ->cue_key_instant[cueId]; //按从父到祖父顺序排列
    description: text;
    [cueId]; 
}

table cue_key_loop[cueKey] {
    cueKey: str -> cue_registry_loop (nullable);
    cueId: int;
    ancestors: list<int> ->cue_key_loop[cueId]; //按从父到祖父顺序排列
    description: text;
    [cueId]; 
}
```

### var_key

```
table var_key[varKey] {
    varKey: str;
    varId: int;
    description: text;
    [varId];
}
```

---

## Runtime Core

本章节定义技能系统在游戏运行时的**心智模型**：静态配置表（Config）是如何在内存中实例化、存储状态并互相通信的。

### 执行流上下文 (Context & Payload)

Context 和 Payload 是连接静态表达式与动态计算的桥梁，也是贯穿整个技能执行树的“血液”。

#### Context (环境上下文)
`Context` 代表一个行为（Ability/Behavior/SpawnedObj）执行时的**持久化环境**。
- **生命周期**：随 Ability 施放或 StatusInstance 生成或 SpawnedObj 诞生而创建，在其整个存活期内保持活跃。
- **作用域**：当执行流遇到 `WithTarget`、`WithLocalVar` 时，会拷贝并派生新的 Context 子作用域。

```java
record Context(
    Actor instigator,   // 真正的发起者 (如：玩家)

    // 实例状态, 原地被改变，跨节点共享
    // 含激活时写入的 targeting 数据、蓄力进度等
    InstanceState instanceState,
    
    // --- 以下被改变都要 new Context
    // StatusInstance 里存储的context：causer=target=host
    // SpawnedObj 里存储的context：causer=target=self
    Actor causer,  // 造成效果的物理实体 (如：玩家发射的火球)
    // 对于 Effect，target = 当前作用的目标。
    // 对于 Behavior，target = 当前status的宿主host
    Actor target,  // 当前正在结算的目标 (被 WithTarget 改变)
    // 局部作用域：仅对当前及子节点树有效 (由 WithLocalVar 绑定，要建新的ReadOnlyStore对象)
    ReadOnlyStore localScope
){}

class InstanceState implements Store {
    int abilityLevel = 1;   // 技能等级（由 Ability 注入，默认为 1）
    int currentStacks = 1;  // 当前状态层数（由 Status 注入，默认为 1） 
    Actor targetingActor;
    Vec2 targetingPoint;
    Vec2 targetingDir;
}

interface ReadOnlyStore {
    float getFloat(int varId);
    Actor getActor(int varId);
    Vec2 getLocation(int varId);
}

interface Store extends ReadOnlyStore {
    // ...
}
```

#### Payload (瞬时载荷)

Payload 是事件携带的瞬时载荷，Trigger 通过它读取"发生了什么"并注入ChangSet修饰。

**关于 Payload 的作用域与生命周期：**

- **顺流而下**：`Trigger` 监听到事件后，会拿到一个 `Payload`。这个 Payload 会顺着该 Trigger 触发的 `Effect` 动作树一直往下传，沿途节点随时可读。
- **传递边界**：当执行流遇到 `ApplyStatus/SpawnObj` 时，Payload 的传递**终止**。
- **延迟捕获**：为了省性能，系统不在 Trigger 处存数据，而是把在新状态需要用到触发时的瞬时数据（如：按受击伤害来算流血值），**在 `ApplyStatus/WithLocalVar` 节点里通过 `VarBinding` 手动把它截留下来。**

**策划配置红线：**

- 带有 `Payload`的节点（如 `PayloadInstigator`、`PayloadMagnitude`），**只能**在带有事件上下文的 `Effect` 执行链中使用。

- 如果在**没有前置事件**的地方直接用，必定报错。遇到这种情况请严格走标准管线：**前面先用 `VarBinding` 截留 -> 后面再用 `ContextVar` 读取**。

```java
record GameplayEvent(
    int eventId,
    Payload payload
){}

record Payload(
    Actor instigator,   // 发起者 (谁砍的这一刀)
    Actor target,       // 承受者 (谁挨的这一刀)

    float magnitude,    // 主值（如伤害量、治疗量）
    Store extras,       // 附加数据，支持 MutatePayloadVar 修改
    TagContainer payloadTags,

    // --- Change 收集器（Pre 阶段使用）---
    ChangeSet magnitudeChanges,
    Int2ObjectMap<ChangeSet> extraChanges
){}

// ChangeSet 瞬时态，聚合数值标量，处理载荷最终结算。
class ChangeSet {
    float additives;
    float multipliers;
    OverrideOp override;

    // 优先override，然后(Base + ΣAdd) * ΠMul
    float resolve(float baseValue);
}

record OverrideOp(
    float value,
    int priority
){}
```

### 实体与组件架构 (Entity & Components)

游戏内的所有战斗实体（玩家、怪物、甚至复杂的投射物）都被抽象为持有四大核心组件的 `Actor`。

```java
class Actor {
    StatusComponent statusComponent; // 状态机：管理所有 Buff/Debuff
    StatComponent statComponent;     // 属性集：管理血量、攻击力等数字
    EventBus eventBus;               // 神经网：监听与广播战斗事件
    TagContainer tagContainer;       // 基因库：实体的当前特征标签
}
```

#### Status 层级模型 (Component -> Instance -> Behavior)
宏观的倒计时驱动与微观的战斗逻辑被严格分层管理。

```java
// 1. 宏观调度层 (挂载在 Actor 上)
class StatusComponent {
    SafeList<StatusInstance> statusInstances;
    // 负责 Tick 驱动倒计时，以及处理同名 Status 的堆叠/刷新策略 (Stacking Policy)
}

// 2. 生命周期层 (单体 Buff/Debuff 在内存中的活体)
class StatusInstance {
    StatusCore coreConfig;
    Context context;                 // 固化了挂载瞬间的环境 (施法者是谁等)
    float remainingDuration;
    int currentStacks;
    
    // 容纳根据 Config 实例化出来的具体逻辑零件
    List<BehaviorInstance<?>> behaviorInstances; 
}

// 3. 业务执行层 (极细粒度的逻辑单元)
abstract class BehaviorInstance<T extends Behavior> {
    T config;
    StatusInstance parentStatus;
    abstract void onStart();
    abstract void onEnd();
    void tick(float dt) {}
}
```


#### Stat 属性模型
游戏内所有数值统一为 `Stat` 对象。临时状态（Buff加攻击）与永久状态（受击扣血）在此完美融合与隔离。

```java
class StatComponent {
    Int2ObjectMap<Stat> stats;
}

class Stat {
    float baseValue;      // 面板基础值 / 当前真实血量
    float currentValue;   // 暴露给外部读取的最终值 (包含了 modifiers 的修饰)
    
    // 收集所有依附在该属性上的临时状态 (如: 战吼加攻)
    StatModifierList modifiers; 
    boolean isDirty = true;
}

class StatModifierList {
    SafeList<StatModifierInstance> additives;
    SafeList<StatModifierInstance> multipliers;
    StatModifierInstance activeOverride = null; // 选最高优先级的
    int currentOverridePriority = -1;

    // currentValue = override ?? (baseValue + ΣAdd) * ΠMul
    float evaluate(float baseValue);
}

class StatModifierInstance extends StatusInstance<StateModifier> {
    float evaluate();
}
```

#### 瞬态与常态修饰器的对比

| 维度 | StatModifierList (常态属性修饰) | ChangeSet (瞬态载荷修饰) |
|------|-----------------|-----------|
| **生命周期** | 跟随 `StatusInstance` 存活 | 单次 `Payload` 结算管线后即销毁 |
| **作用对象** | `Stat.currentValue` (如：角色攻击力) | `Payload.magnitude` (如：本次火球伤害) |
| **配置来源** | `Behavior.StatModifier` | `Trigger` 触发的 `ModifyPayloadMagnitude` |
| **结算时机** | Stat 标脏时惰性重算 | Pipeline 阶段 4（Pre 结算阶段）聚合求值 |


### 事件总线 (EventBus)

以 `EventTagId` 为键，直连底层的监听队列。

```java
/**
 * 全局战斗沙盘上下文，持有跨实体的共享状态（每个副本/战场一个实例）
 */
class CombatSystem {
    int globalDispatchDepth = 0;
    final int maxDispatchDepth; // 从 combat_settings 读取

    boolean tryEnterDispatch() {
        if (globalDispatchDepth >= maxDispatchDepth) return false;
        globalDispatchDepth++;
        return true;
    }

    void exitDispatch() {
        globalDispatchDepth--;
    }
}

class EventBus {
    Int2ObjectMap<SafeList<TriggerInstance>> listeners;
    final CombatSystem combatSystem;

    void dispatch(GameplayEvent event) {
        if (!combatSystem.tryEnterDispatch()) return; // 全局深度检查
        try {
            SafeList<TriggerInstance> list = listeners.get(event.eventId);
            if (list == null) return;
            list.beginIterate();
            try {
                for (TriggerInstance trigger : list.items) {
                    if (!trigger.isPendingKill()) trigger.onEventFired(event);
                }
            } finally {
                list.endIterate();
            }
        } finally {
            combatSystem.exitDispatch();
        }
    }
}
```
---

## Core Entities

本章节定义系统的主体部分：Ability 是入口，Effect 是动作，Status 是持续状态，逻辑递进。

### Ability

Ability 是整个系统的**逻辑入口点**。它负责处理玩家或 AI 的主动意图，并将其转化为具体的战斗效果。

**核心职责：**
1. **执行准入检查**：验证冷却（Cooldown）、资源消耗（Cost）及自定义激活条件。
2. **定义生命周期**：管理从按键触发到效果结算的全过程（含前摇、蓄力、引导等阶段）。
3. **驱动效果执行**：在特定时机触发 `Effect` 树。

**配置标准：**
为了支持动作游戏复杂的生命周期管理，Ability 的详细表结构、施法模型（Startup/Charge/Channel）、瞄准逻辑以及完整的运行时状态机已拆分为独立专题文档。

> **关于 Ability 的详细配置定义、生命周期及状态机，请严格参考：`ability-cast.md`**

**设计红线：**
* **主动性**：只有主动触发的行为（含 Dash、瞬移等）才使用 Ability。
* **被动解耦**：被动技能严禁使用 Ability 实现，本质应为挂载在 Actor 上的永久 `Status`。
* **无状态性**：Ability 配置本身是静态的，所有运行时数据（如当前蓄力时长）必须存储在 `AbilityInstance` 的 `context.instanceState` 中。

### Effect

`Effect` 是瞬间执行、**无状态**的指令流。

```cfg
interface Effect {
    struct None {
    }
    
    // --- 属性修改 ---
    struct ModifyStat { // 改 baseValue
        stat: str ->stat_definition; // 如 "Resource_Stamina"
        op: ModifierOp;
        value: FloatValue;
    }

    //  结算管线触发（伤害/治疗/自定义管线）
    struct ResolveCombat {
        pipeline: str ->resolution_pipeline;
        magnitude: FloatValue;
        tags: list<str> ->gameplaytag;   // 如 ["Damage.Element.Fire"]
        cues: list<str> ->cue_key_instant;
    }

    // --- 状态操作 ---
    // 引用标准 Status (适用于需要 UI 图标、多端网络同步的常规状态，如“中毒”, “护盾”)
    struct ApplyStatus {
        statusId: int ->status;
        bindings: list<VarBinding>; // 等价于WithLocalVar
    }

    // 内联型 Status (拥有完整功力，适用于无需 UI 显示的一次性专属机制)
    struct ApplyStatusInline {
        core: StatusCore;
        bindings: list<VarBinding>;
    }

    // 快捷方式：极简内联微状态 (适用于冲锋时的零点几秒霸体等纯逻辑阻断状态)
    struct GrantTags {
        grantedTags: list<str> ->gameplaytag;
        duration: FloatValue;
    }

    struct RemoveStatusByTag {
        query: TagQuery;
        matchStatusTags: bool;  // 匹配 "它是什么" (如: 魔法, 中毒)
        matchGrantedTags: bool; // 匹配 "它造成了什么" (如: 眩晕, 沉默)
    }

    struct RemoveStatus {
        anyIds: list<int> ->status;
    }

    // 瞬移
    struct Teleport {
        destination: LocationSelector;
    }

    // 发送事件
    struct SendEvent {
        event: str ->event_definition;
        magnitude: FloatValue;
        extras: list<VarBinding>;
    }

    // 生成物 (子弹/法阵)
    struct SpawnObj {
        duration: FloatValue;
        objTags: list<str> ->gameplaytag;
        moveInfo: ObjMoveInfo; // 移动，弹道，碰撞在这里定义
        cuesWhileActive: list<str> ->cue_key_loop; // 飞行时的呼啸声、法阵的底图特效
        effectsOnCreate: list<Effect>; // 诞生时：瞬间触发的逻辑 (如：落地瞬间的拉扯，伤害的定时触发）
        dieInfo: list<ObjDieInfo>;  // 生成物消失的条件、结算
    }

    // --- 载荷篡改（仅在 Pre 阶段 Trigger 中有效）
    struct ModifyPayloadMagnitude {
        op: ModifierOp;
        value: FloatValue;
        overridePriority: int; // op为Override才起效
    }

    struct ModifyPayloadExtra {
        varKey: str ->var_key;
        op: ModifierOp;
        value: FloatValue;
        overridePriority: int;
    }

    struct GrantPayloadTag {
        tags: list<str> ->gameplaytag;
    }

    // --- Cue 触发
    struct FireCue {
        cues: list<str> ->cue_key_instant;
        magnitude: FloatValue;
    }

    // --- 作用域 & 控制流 ---
    
    struct Sequence { effects: list<Effect>; 

    struct Conditional {
        condition: Condition;
        thenEffect: Effect;
        otherwise: list<Effect>;
    }

    struct Repeat {
        count: FloatValue; // 取下界
        indexVarTag: str -> gameplaytag (nullable); // 放入localScope里
        effect: Effect;
    }

    struct WithTarget {
        target: TargetSelector;
        effect: Effect;
    }

    struct WithTargets {
        targets: TargetScan;
        effect: Effect;
    }

    struct WithLocalVar {
        bindings: list<VarBinding>;
        effect: Effect;
    }
    
    struct RunEffect {
        sharedEffectId: int ->shared_effect; // 逻辑复用
        bindings: list<VarBinding>;
    }
}

struct VarBinding {
    varKey: str ->var_key;
    value: VarValue;
    bindMode: BindMode;
}

interface VarValue {
    struct Float { value: FloatValue; }
    struct Actor { selector: TargetSelector; }
    struct Location { selector: LocationSelector; }
}

enum BindMode {
    // 传值 (Pass-by-Value)
    // 行为：在绑定的瞬间立即求值，将算出的具体数字、实体引用固化在 Store 中。
    // 适用：事件传参、截留施法瞬间的攻击力。
    Snapshot; 

    // 传名/闭包 (Pass-by-Name / Thunk)
    // 行为：不立即求值，而是把 VarValue 的配置和当前 Context 存入 Store。每次读取时，重新计算。
    // 适用：Status 中随施法者当前攻击力实时变化的持续伤害 (DOT)；或者每次读取时获取实体的最新坐标。
    Dynamic;  
}

enum ModifierOp { Add; Mul; Override; }

// 高频复用的effect
table shared_effect[effectId] (json) {
    effectId: int;
    name: text;
    description: text;
    effect: Effect;
}
```

### Status

Status 是持续状态的容器，挂载在 Actor 上，拥有独立的生命周期、堆叠策略和行为零件。

```cfg
table status[id] (json) {
    id: int;
    name: text;
    description: text;
    icon: str;

    stackingPolicy: StackingPolicy;
    core: StatusCore;
}

struct StatusCore {
    statusTags: list<str> ->gameplaytag;  // Status 自身分类（用于外部查询/移除）
    grantedTags: list<str> ->gameplaytag; // 存在期间授予宿主的标签
    
    duration: FloatValue;           // -1 = 永久
    
    cuesWhileActive: list<str> ->cue_key_loop;
    behaviors: list<Behavior>;
}

interface StackingPolicy {
    // 独立计时：每层独立倒计时
    struct Independent {
        maxStacks: int;
        overflowPolicy: OverflowPolicy;
    }

    // 共享计时：所有层共享一个倒计时
    struct Shared {
        maxStacks: int;
        refreshMode: RefreshMode;
        overflowPolicy: OverflowPolicy;
    }

    // 不可叠加
    struct Single {
        refreshMode: RefreshMode;
    }
}

enum RefreshMode {
    ResetDuration;
    ExtendDuration;
    KeepDuration;
}

enum OverflowPolicy {
    Reject;             // 丢弃
    ReplaceOldest;      // 替换最早层
    TriggerOnOverflow;  // 触发 OnOverflow 行为
}
```

### Behavior

Behavior 是附着在 Status 上的逻辑零件。其运行在 `StatusInstance`（内含 `Context`）内。

```cfg
interface Behavior {
    // 属性修饰器
    struct StatModifier {
        stat: str ->stat_definition;
        op: ModifierOp;
        value: FloatValue;
        overridePriority: int;
        requiresAll: list<Condition>;
    }

    // 根骨骼运动控制 (接管角色的物理移动)
    struct RootMotion {
        // 运动模式
        motionType: MotionType;
        
        // 当移动过程中物理胶囊体撞到别的 Actor 时触发的瞬间逻辑
        onSweepHit: OnSweepHitAction (nullable);
    }

    // 周期性触发 (DOT/HOT)
    struct Periodic {
        period: FloatValue;
        executeOnApply: bool;
        effect: Effect;
    }

    // 时间轴
    struct Timeline {
        phases: list<TimelinePhase>;
    }

    // 光环
    struct Aura {
        scan: TargetScan; 
        grantedStatusId: int ->status; 
        updateInterval: FloatValue; 
    }

    // 事件触发器
    struct Trigger {
        listenEvent: str ->event_definition;
        requiresAll: list<Condition>;
        effect: Effect;

        maxTriggers: int;
        cooldown: FloatValue;
    }

    // 生命周期钩子
    struct OnApply { effect: Effect; }
    struct OnStackChange { effect: Effect; }
    struct OnOverflow { effect: Effect; }
    struct OnExpire { effect: Effect; }
    struct OnInterrupt { effect: Effect; }
    struct OnRemove { effect: Effect; }
}

struct TimelinePhase {
    duration: FloatValue;                  // 持续多久
    grantedTags: list<str> ->gameplaytag;  // 期间给宿主贴什么标签（暴露给 tag_rules 和外界）
    effect: list<Effect>;                  // 期间做什么具体动作
}
```

---

## Expression Layer

### FloatValue

提供多态求值能力，将静态配置转化为上下文敏感的运行时指令，替代硬编码参数。

```cfg
interface FloatValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: FloatValue; b: FloatValue; }
    struct Actor {
        source: TargetSelector;
        value: ActorFloatValue;
    }

    struct StatValue { // 取Current
        source: TargetSelector;
        stat: str ->stat_definition;
    }

    // 上下文变量，优先localScope，然后instanceState
    struct ContextVar { varKey: str ->var_key; }

    // 事件载荷变量
    struct PayloadMagnitude { }
    struct PayloadVar { varKey: str ->var_key; }

    // Status 层数
    struct CurrentStacks {}

    // 按层数缩放：(baseValue + perStackAdd * stacks) * perStackMul ^ stacks
    struct StackScaling {
        baseValue: float;
        perStackAdd: float;
        perStackMul: float;   // 1.0 = 无乘法缩放
    }
}

interface ActorFloatValue {
    struct Math { op: MathOp; a: ActorFloatValue; b: ActorFloatValue; }
    struct StatValue { stat: str ->stat_definition; }
    struct StatBaseValue { stat: str ->stat_definition; }
}

enum MathOp { Add; Sub; Mul; Div; Max; Min; }
```

### Condition

提供执行准入标准，与 FloatValue 配合实现动态逻辑判断。

```cfg
interface Condition {
    struct Const { value: bool; }
    struct And { args: list<Condition>; }
    struct Or { args: list<Condition>; }
    struct Not { arg: Condition; }
    struct Compare {
        left: FloatValue;
        op: CompareOp;
        right: FloatValue;
    }
    struct Actor {
        source: TargetSelector;
        cond: ActorCondition;
    }
    struct HasTags {
        source: TargetSelector;
        query: TagQuery;
    }

    struct PayloadHasTag { query: TagQuery; }
    
    struct Chance { probability: FloatValue; } // 随机概率
}

interface ActorCondition {
    struct And { args: list<ActorCondition>; }
    struct Or { args: list<ActorCondition>; }
    struct Not { arg: ActorCondition; }
    struct Compare {
        left: ActorFloatValue;
        op: CompareOp;
        right: ActorFloatValue;
    }
    struct HasTags {
        query: TagQuery;
    }
}

enum CompareOp {
    Gt; Gte; Lt; Lte; Eq; Neq;
}

struct TagQuery {
    requireAll: list<str> ->gameplaytag; // 必须全部包含
    requireAny: list<str> ->gameplaytag; // 包含其中之一即可生效 (最常用)
    exclude: list<str> ->gameplaytag;    // 包含任何一个则拦截/失效
}
```

### TargetSelector
用于动态选取一个目标实体。
```cfg
interface TargetSelector {
    struct ContextTarget {}
    struct ContextInstigator {}
    struct ContextCauser {}
    struct ContextVar { varKey: str ->var_key; }

    struct PayloadInstigator {}
    struct PayloadTarget {}
    struct PayloadVar { varKey: str ->var_key; }
}
```

### LocationSelector
```cfg
interface LocationSelector {
    struct ActorLocation {
        target: TargetSelector;
    }
    
    // 某个实体的前方/偏移位置 (如：闪现到目标背后 2 米)
    struct ActorOffset {
        target: TargetSelector;
        forwardOffset: FloatValue;
        rightOffset: FloatValue;
        upOffset: FloatValue;
    }

    // 从 Context/Payload 的变量中读取保存的坐标
    struct ContextVar { varKey: str ->var_key; }
    struct PayloadVar { varKey: str ->var_key; }
}
```

### TargetScan
```cfg
struct TargetScan {
    shape: ScanShape;

    relationTo: TargetSelector;
    allowedRelations: list<Relation>;
    tagQuery: TagQuery;
    exclude: list<TargetSelector>; 

    maxCount: int; // -1 = 无限
    sort: SortStrategy;
}

interface ScanShape {
    struct Sphere {
        center: TargetSelector;
        radius: FloatValue;
    }

    struct Sector {
        center: TargetSelector;
        facingOf: TargetSelector;   // 取该实体的朝向作为扇形正方向
        radius: FloatValue;
        angle: FloatValue;
    }

    struct Box {
        center: TargetSelector;
        facingOf: TargetSelector;
        width: FloatValue;
        length: FloatValue;
    }

    struct PartyOf {
        source: TargetSelector;
    }
}

enum Relation { Self; Friendly; Hostile; Neutral; }
enum SortStrategy { Nearest; Farthest; HpPercentAsc; HpPercentDesc; Random; None; }
```

---

## combat_settings

本章节定义全局性的战斗规则，这些规则以配置表的形式存在，由底层引擎直接读取，作为战斗系统的“最高物理法则”。

```cfg
table combat_settings[name] {
    name: str;
    tagRules: list<str> ->tag_rules;

    maxDispatchDepth: int;       // 事件派发嵌套深度上限（防反伤死循环）
    maxStatusCountPerActor: int; // 单个 Actor 的 StatusInstance 上限
    maxEffectChainLength: int;   // 单次 Effect 执行链的节点数上限（防 Repeat/Sequence 过长）
    maxScanTargets: int;         // TargetScan 全局兜底上限（覆盖单个配置中的 maxCount）
    maxConcurrentAbilitiesPerActor: int; // 兜底并发限制
}
```


### tag_rules

该表定义了标签之间的原子化交互准则。通过将“状态”与“行为”解耦，它充当了战斗系统的“最高物理法则”，统一处理控制、打断与免疫。

```cfg
table tag_rules[name] (json){
    name: str;
    rules: list<TagRule>;
}

struct TagRule {
    // 当宿主拥有此标签时...
    whenPresent: str ->gameplaytag;

    // 阻止激活带有以下标签的 Ability
    blocksAbilities: list<str> ->gameplaytag;
    
	// 硬打断：打断正在运行的前摇/蓄力/引导，并触发惩罚（onInterrupt） 
	interruptsAbilities: list<str> ->gameplaytag; 
	
	// 软取消：无惩罚地取消正在运行的前摇/蓄力/引导，或提前结束任何技能的后摇 
	cancelsAbilities: list<str> ->gameplaytag;
	
    // 免疫：拒绝挂载 grantedTags 含以下标签的 Status
    immuneToTags: list<str> ->gameplaytag;

    // 驱散：获得 whenPresent 瞬间，移除 grantedTags 含以下标签的 Status
    purgesTags: list<str> ->gameplaytag;

    description: text;
}
```

**示例：**

```
tag_rules {
    name: "CoreCombatRules";
    rules: [
        { whenPresent: "State.Debuff.Silence";
          blocksAbilities: ["Ability.Type.Spell"];
          description: "沉默封印法术"; },

        { whenPresent: "State.Debuff.Control.Stun";
          blocksAbilities: ["Ability.Type"];
          cancelsAbilities: ["Ability.Type"];
          description: "眩晕封印+打断所有技能"; },

        { whenPresent: "State.Buff.Hyperarmor";
          immuneToTags: ["State.Debuff.Control"];
          description: "霸体免疫控制"; },

        { whenPresent: "State.Buff.Purify";
          purgesTags: ["State.Debuff"];
          description: "净化驱散所有减益"; }
    ];
}
```

### resolution_pipeline

将伤害/治疗的结算逻辑配置化。引擎按 Pipeline 定义执行多阶段事件广播与逐层属性扣减。

```cfg
table resolution_pipeline[name] (json) {
    name: str;
    pipelineId: int;
    description: text;

    flow: ValueFlow;

    // ===== 双视角事件 =====
    // 施加者视角（广播到 instigator 的 EventBus）
    dealPreEvent:  str ->event_definition;
    dealPostEvent: str ->event_definition;

    // 承受者视角（广播到 target 的 EventBus）
    takePreEvent:  str ->event_definition;
    takePostEvent: str ->event_definition;

    // 判定阶段
    checks: list<CheckStage>;

    // 属性扣减/增加管线
    allocations: list<AllocationLayer>;
    [pipelineId];
}

enum ValueFlow {
    Deplete;       // 扣减（伤害类）
    Restore;       // 增加（治疗类）
}

struct AllocationLayer {
    targetStat: str ->stat_definition;
    conversionRate: float;
    allowOverflow: bool;
    onHitCue: list<str> ->cue_key_instant;
    onDepletedCue: list<str> ->cue_key_instant;
}
```

### CheckStage

判定阶段在 Pre Modifier 结算之后、属性扣减之前执行。用于实现闪避、格挡、暴击等核心战斗判定。每个阶段独立求值，互不干扰（可配置互斥关系）。

```cfg
struct CheckStage {
    name: text;

    // 触发条件
    skipIfPayloadHasAny: list<str> ->gameplaytag; // 检查Payload.payloadTags

    // 触发概率（从 Context/Payload 中动态取值）
    chance: FloatValue;

    // 判定成功时的效果
    grantPayloadTags: list<str> ->gameplaytag; // 写入Payload.payloadTags
    magnitudeModifiers: List<MagnitudeModifier>;
    onSuccessEffects: List<Effect>;
}

struct MagnitudeModifier {
    op: ModifierOp;
    value: FloatValue;
    overridePriority: int;       // 仅 Override 时生效
}
```

**设计说明：**

判定阶段的执行时机在管线中被精确定义：

```
ResolveCombat 调用
    │
    ▼
1.  构建 Payload（snapshot magnitude + extras + tags）
    │
    ▼
2.  广播 Deal.Pre → instigator 的 EventBus
    │   攻击者挂的 Buff 在此注入 Modifier（如：增伤、穿甲）
    │
    ▼
3.  广播 Take.Pre → target 的 EventBus
    │   防御者挂的 Buff 在此注入 Modifier（如：减伤、格挡加成）
    │
    ▼
4.  结算全部 Pre Modifier → 得到修正后的 magnitude
    │
    ▼
5.  【判定阶段 CheckStage】依次执行 checks 列表
    │   a. skipIfPayloadHasAny 互斥检查
    │   b. 掷骰 chance
    │   c. 成功：写入 grantPayloadTags，应用 magnitudeModifier
    │
    ▼
6.  逐层属性扣减/增加（allocations）
    │
    ▼
7.  广播 Deal.Post → instigator 的 EventBus
    │   攻击者响应（如：吸血、击杀奖励、连击计数）
    │
    ▼
8.  广播 Take.Post → target 的 EventBus
        防御者响应（如：反伤、受击触发护盾）
```

**预置管线示例（含判定阶段）：**

```
resolution_pipeline {
    name: "StandardPhysicalDamage";
    flow: Deplete;

    dealPreEvent:  "Combat_Damage_Deal_Pre";
    dealPostEvent: "Combat_Damage_Deal_Post";
    takePreEvent:  "Combat_Damage_Take_Pre";
    takePostEvent: "Combat_Damage_Take_Post";

    checks: [
        // 阶段1：闪避
        {
            name: "闪避";
            chance: Math { op: Sub;
                a: StatValue { source: PayloadTarget {}; stat: "Combat_DodgeRate"; };
                b: StatValue { source: PayloadInstigator {}; stat: "Combat_Accuracy"; };
            };
            grantPayloadTags: ["Combat.Result.Dodged"];
            magnitudeModifiers: [{ op: Override; value: Const { value: 0.0; }; overridePriority: 999; }];
        },

        // 阶段2：格挡
        {
            name: "格挡";
            skipIfPayloadHasAny: ["Combat.Result.Dodged"];
            chance: StatValue { source: PayloadTarget {}; stat: "Combat_BlockRate"; };
            grantPayloadTags: ["Combat.Result.Blocked"];
            magnitudeModifiers: [{ op: Mul;
                value: Math { op: Sub;
                    a: Const { value: 1.0; };
                    b: StatValue { source: PayloadTarget {}; stat: "Combat_BlockEfficiency"; };
                };
            }];
        },

        // 阶段3：暴击
        {
            name: "暴击";
            skipIfPayloadHasAny: ["Combat.Result.Dodged"];
            chance: Math { op: Sub;
                a: StatValue { source: PayloadInstigator {}; stat: "Combat_CritRate"; };
                b: StatValue { source: PayloadTarget {}; stat: "Combat_CritResist"; };
            };
            grantPayloadTags: ["Combat.Result.Critical"];
            magnitudeModifiers: [{ op: Mul;
                value: StatValue { source: PayloadInstigator {}; stat: "Combat_CritDamage"; };
            }];
        }
    ];

    allocations: [
        { targetStat: "Shield_Current"; conversionRate: 1.0; allowOverflow: true;
          onHitCue: ["Hit.Shield"]; onDepletedCue: ["Break.Shield"]; },
        { targetStat: "HP_Current"; conversionRate: 1.0;
          onHitCue: ["Hit.Flesh"]; }
    ];
}

resolution_pipeline {
    name: "PureDamage";
    flow: Deplete;
    dealPreEvent:  "Combat_Damage_Deal_Pre";
    dealPostEvent: "Combat_Damage_Deal_Post";
    takePreEvent:  "Combat_Damage_Take_Pre";
    takePostEvent: "Combat_Damage_Take_Post";
    // 无 checks — 纯伤害不参与闪避/格挡/暴击
    allocations: [
        { targetStat: "HP_Current"; conversionRate: 1.0; }
    ];
}

resolution_pipeline {
    name: "StandardHeal";
    flow: Restore;
    dealPreEvent:  "Combat_Heal_Give_Pre";   // 治疗者视角：治疗量增幅
    dealPostEvent: "Combat_Heal_Give_Post";  // 治疗者视角：治疗后触发
    takePreEvent:  "Combat_Heal_Take_Pre";   // 受疗者视角：受疗量增幅
    takePostEvent: "Combat_Heal_Take_Post";  // 受疗者视角：受疗后触发
    allocations: [
        { targetStat: "HP_Current"; conversionRate: 1.0; }
    ];
}
```

---

## 表现层设计 Cue

表现层（客户端）系统。其核心逻辑是：逻辑层输出**意图 (CueKey)**、**强度 (Magnitude)** 与 **语义上下文 (Tags)**，客户端据此自动寻址并执行视听反馈。

> **关于资产匹配加权算法、材质栈管理及飘字聚合策略，请参考专项文档：`cue-design.md`**

---

## Implementation Reference

本章提供核心架构的伪代码实现，展示运行时各组件如何协作。此处重点关注具体的技术实现细节，包括生命周期管理、无状态执行器等。

如需网络同步，请一定参考 **`ability-net.md`**。

### TagContainer

底层采用 **"引用计数 + 写入时展开"** 策略，实现 O(1) 的父级包含查询。

```java
class TagContainer {
    // 核心：TagID -> 引用计数
    Int2IntMap tagCounts;

    // 写入时展开
    // 当添加子级 Tag (如 Stun) 时，会同时递增其所有父级 (Control, Debuff) 的计数。
    // 这使得运行时查询 "State.Debuff" 时，只需简单查询 Map 中是否存在该 Key (O(1))
    void addTag(int tagId) 
    void removeTag(int tagId);
    boolean hasTag(int tagId);
}
```

### SafeList

抽离生命周期管理中的“防重入、防并发修改”逻辑，封装为泛型安全容器。所有受控实体（状态、行为零件等)必须实现 `IPendingKill`。
在遍历迭代（ `beginIterate`/`endIterate` ) 期间,组件采用**双缓冲并发隔离**:新增元素会被拦截送入 `pendingAdds` 缓冲队列,而销毁操作仅做 `isPendingKill = true` 的逻辑**软删除**。
迭代彻底结束后,容器自动进行**延迟硬清理 (`compact`)**,采用 Swap-Remove 策略以 O(1) 的时间复杂度完成数组缝合,避免 `ArrayList` 的整体内存搬运。

```java
interface IPendingKill {
    boolean isPendingKill();
    void markPendingKill();
}

class SafeList<T extends IPendingKill> {
    List<T> items;
    List<T> pendingAdds; // 缓冲在迭代期间被添加的新元素
    int iterateDepth;    // 嵌套深度计数器,防并发修改锁
    boolean needsCompaction; // 脏标记:是否存在需要清理的 pendingKill 实例

    void add(T item);
    void remove(T item); // 仅做软删除: item.setPendingKill(true)
    void beginIterate(); // 迭代上锁: iterateDepth++
    void endIterate();   // 解锁。若归零,触发 flushPendingAdds 与 compact()
    void compact();      // Swap-Remove 物理清理
}
```

### Stateless Executors

`Effect`、`FloatValue`、`Condition`、`TargetSelector`、`TargetScan` 作为**无状态**指令节点,本身不维护生命周期,被调用时即时消费上下文结算。

```java
class FloatValues {
    static float evaluate(FloatValue cfg, Context ctx, Payload payload);
}

class ActorFloatValues {
    static float evaluate(FloatValue cfg, Actor actor);
}

class Conditions {
    static boolean test(Condition cfg, Context ctx, Payload payload);
}

class ActorConditions {
    static boolean test(Condition cfg, Actor actor);
}

class TargetSelectors {
    static Actor select(TargetSelector cfg, Context ctx, Payload payload);
}

class TargetScans {
    static Collection<Actor> scan(TargetScan cfg, Context ctx, Payload payload);
}

class Effects {
    static void execute(Effect cfg, Context ctx, Payload payload);
}
```

---

## Examples

### 例A：盾墙减伤（Pre 阶段拦截）

```
status {
    id: 5001;
    name: "盾墙";
    grantedTags: ["State.Buff.ShieldWall"];
    duration: Const { value: 5.0; };
    stackingPolicy: Single { refreshMode: ResetDuration; };
    behaviors: [
        Trigger {
            listenEvent: "Combat_Damage_Take_Pre";
            effect: ModifyPayloadMagnitude {
                op: Mul;
                value: Const { value: 0.6; };
                overridePriority: 0;
            };
        }
    ];
}
```

### 例B：反伤装甲（Post 阶段响应）

```
status {
    id: 4001;
    name: "反刺被动";
    grantedTags: ["State.Passive.ThornArmor"];
    duration: Const { value: -1.0; };
    stackingPolicy: Single { refreshMode: KeepDuration; };
    behaviors: [
        Trigger {
            listenEvent: "Combat_Damage_Take_Post";
            requiresAll: [
                Compare {
                    left: PayloadMagnitude {};
                    op: Gte;
                    right: Const { value: 50.0; };
                }
            ];
            effect: WithTarget {
                target: PayloadInstigator {};
                effect: ResolveCombat {
                    pipeline: "PureDamage";
                    magnitude: Const { value: 10.0; };
                    tags: ["Damage.Type.Reflected"];
                };
            };
        }
    ];
}
```

### 例C：叠毒（独立计时 DOT + 溢出爆发）

```
status {
    id: 3001;
    name: "剧毒";
    statusTags: ["Status.Type.DOT"];
    grantedTags: ["State.Debuff.Poison"];
    duration: Const { value: 8.0; };
    stackingPolicy: Independent {
        maxStacks: 5;
        overflowPolicy: TriggerOnOverflow;
    };
    cuesWhileActive: ["Status.Poison"];
    behaviors: [
        Periodic {
            period: Const { value: 2.0; };
            executeOnApply: false;
            effect: ResolveCombat {
                pipeline: "PureDamage";
                magnitude: StackScaling {
                    baseValue: 5.0;
                    perStackAdd: 3.0;
                    perStackMul: 1.0;
                };
                tags: ["Damage.Element.Poison"];
            };
        },
        OnOverflow {
            effect: Sequence {
                effects: [
                    ResolveCombat {
                        pipeline: "PureDamage";
                        magnitude: Const { value: 80.0; };
                        tags: ["Damage.Element.Poison", "Damage.Type.Burst"];
                        cuesOnExecute: ["Combat.PoisonBurst"];
                    },
                    RemoveStatus {
                        statusId: 3001;
                    }
                ];
            };
        }
    ];
}
```


### 例D：暴击增伤被动（利用 CheckStage 标记）

```
// 被动：当你的攻击暴击时，额外造成一次固定伤害
status {
    id: 6001;
    name: "暴击追伤";
    grantedTags: ["State.Passive.CritFollowup"];
    duration: Const { value: -1.0; };
    stackingPolicy: Single { refreshMode: KeepDuration; };
    behaviors: [
        Trigger {
            listenEvent: "Combat_Damage_Deal_Post";
            requiresAll: [
                // 检查 Payload 中是否有暴击标记（由 CheckStage 写入）
                PayloadHasTag { query: { requireAll: ["Combat.Result.Critical"];};}
            ];
            effect: WithTarget {
                target: PayloadTarget {};
                effect: ResolveCombat {
                    pipeline: "PureDamage";
                    magnitude: Const { value: 25.0; };
                    tags: ["Damage.Type.Bonus"];
                    cuesOnExecute: ["Combat.CritBonus"];
                };
            };
        }
    ];
}
```