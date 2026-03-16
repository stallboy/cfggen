# 能力系统设计 (Ability System Design)

基于 Unreal GAS 核心理念的**全数据驱动**技能系统配置标准。本文档是**架构基准**，具体游戏应在此基础上裁剪和扩展。

## Philosophy

1. **层级化标签驱动**：树状 Tag（如 `State.Debuff.Control.Stun`）是逻辑交互的唯一通用语言。支持父级包含查询——查询 `State.Debuff` 可命中 `State.Debuff.Control.Stun`。

2. **两阶段事件管线**：所有逻辑通信通过 `EventBus` 广播标准消息。`Pre` 阶段注入 `Modifier` 篡改数值（如减伤），`Post` 阶段触发副作用（如反伤）。源与监听者彻底解耦。

3. **逻辑与表现隔离**：逻辑层仅输出 `CueTag`，客户端独立维护 Tag 到资源的映射。

4. **三层正交分离**：`Ability`（行为入口）、`Status`（持续状态容器）、`Effect`（无状态原子指令）严格正交。通过组合构建复杂机制。

---

## Data Foundation


定义系统的"词汇表"——标签、属性、事件的原子定义。

### GameplayTag

```cfg
// Tag字典注册表
// 层级关系通过名称中的 "." 分隔符隐式表达
// 程序启动时自动构建父子索引
table gameplaytag[name] {
    name:str; // 例: "State.Debuff.Control.Stun", "Event.Combat.Damage"
    description: text;
    value: int;
    ancestors: list<int> ->gameplaytag[value]; // 策划不用填，程序初始化时自动填
    [value];
}
```

**命名规范**：

| 前缀 | 用途 | 示例 |
|---|---|---|
| `State.*` | 实体状态标记 | `State.Debuff.Control.Stun` |
| `Stat.*` | 属性标识 | `Stat.Combat.Attack` |
| `Status.*` | status分类标识 | `Status.Type.DOT` |
| `Ability.*` | 技能分类标签 | `Ability.Type.Spell` |
| `Damage.*` | 伤害/治疗分类 | `Damage.Element.Fire` |
| `Event.*` | 事件路由键 | `Event.Combat.Damage.Deal.Pre` |
| `Cue.*` | 表现层路由键 | `Cue.Combat.Hit.Heavy` |

| `Var.*` | 变量键名 | `Var.ChargeTime` |
| `Cooldown.*` | 冷却组键名 | `Cooldown.Ability.Fireball` |


### Stat Definition

```cfg
table stat_definition[statTag] {
    statTag: str ->gameplaytag;
    name: text;

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
}

interface StatLimit {
    struct Const { value: float; }
    // 引用另一个 stat_definition，在运行时去取它的 CurrentValue
    struct StatLink { statTag: str ->stat_definition; }
    struct None {}
}

enum StatClampMode {
    Absolute;          // 绝对值截断 (如：MaxHP 从 100 降到 40，当前 HP 变为 40)
    MaintainPercent;   // 维持百分比 (如：MaxHP 变为原先的 2倍，当前 HP 也按比例乘以 2)
    None;              // 纯粹的自由属性，不进行任何联动调整
}
```

### Event Definition

定义系统中所有可用的事件类型

```cfg
table event_definition[eventTag] (entry='eventTag') {
    eventTag: str ->gameplaytag; // 如 "Event.Combat.Damage"

    // 定义该事件 Payload 中预期的变量列表 (可用于验证和编辑器提示)
    // 告知该事件除了 magnitude 以外，在 extras 里还会塞入哪些 Tag 数据
    expectedVars: list<EventVarDecl>;
}

struct EventVarDecl {
    varTag: str ->gameplaytag; // 如 "Var.Damage.Element"
    type: VarType;
    description: text;
}

enum VarType {
    Float;
    Actor;
}
```

---
## Runtime Core


本章节定义技能系统运行时的核心数据结构与组件。这些概念是连接静态配置与动态执行的桥梁：
- **Context** 封装了执行的完整环境（发起者、目标、局部变量等），所有表达式（`FloatValue`、`Condition`）和实体（`Effect`、`Status`）均依赖它进行动态求值与逻辑演算。
- **Payload** 作为事件通信的标准化载荷，携带动作发生时的瞬时数据（如伤害值、攻击者、受害者），供 `Effect` 和 `Trigger` 在事件响应中读取或修改。

### Context

Context 是运行时的载体，连接静态配置与动态执行。

```java
record Context(
    Actor instigator,   // 真正的发起者 (如：玩家)
    Actor causer, // 造成效果的物理实体 (如：玩家发射的火球)
    ReadOnlyStore initSnapshot, // 冻结的初始参数 (如：蓄力时间,初始锁定的目标)

    // --- 数据流转,target或localScope改变的时候要 new Context ---
    // 对于 StatusInstance，target代表 Status 的 Host (宿主)。
    // 对于 Effect，target代表当前的作用目标。
    Actor target,       // 被 WithTarget 改变
    Store instanceState,// 实例状态 (跨节点共享，随技能销毁)
    Store localScope,   // 局部作用域 (仅对子节点树有效，WithLocalVar时要创建新 localScope)
    int recursionDepth  // 死循环防护
){}

interface ReadOnlyStore {
    float getFloat(int tagId);
    Object getObject(int tagId);
    boolean hasTag(int tagId);
}

interface Store extends ReadOnlyStore {
    void setFloat(int tagId, float value);
    void setObject(int tagId, Object obj);
    void removeTag(int tagId);
}
```

### Payload

Payload 是事件携带的瞬时载荷，Trigger 通过它读取"发生了什么"并注入ChangSet修饰。

```java
record Event(
    Event_definition eventTag,
    Payload payload
){}

record Payload(
    Context sourceContext, // 携带引发该事件的原始技能 Context 引用，用于追溯来源
    Actor instigator,   // Event的绝对发起者 (谁砍的这一刀)
    Actor target,       // Event的绝对承受者 (谁挨的这一刀)

    float magnitude,    // 主值（如伤害量、治疗量）
    Store extras,       // 附加数据，支持 MutatePayloadVar 修改

    // --- Change 收集器（Pre 阶段使用）---
    ChangeSet magnitudeChanges,
    Int2ObjectMap<ChangeSet> extraChanges
){}

// ChangeSet 瞬时态，聚合数值标量，处理载荷最终结算。
class ChangeSet {
    FloatList additives;
    FloatList multipliers;
    OverrideOp override;

    // 优先override，然后(Base + ΣAdd) * ΠMul
    float resolve(float baseValue);
}

record OverrideOp(
    float value,
    int priority
){}
```

### Actor

定义游戏实体及其持有的核心组件。

```java
class Actor {
    StatusComponent statusComponent;
    StatComponent statComponent;
    EventBus eventBus;
    TagContainer tagContainer;
}
```

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

### StatComponent

游戏内所有数值（最大生命、当前血量、攻击力）全部统一为 `Stat` 对象。通过 `StatModifierList` 管线实现临时状态与永久状态的完美隔离。

```java
class StatComponent {
    // 核心存储：TagId -> Stat 实例的映射
    Int2ObjectMap<Stat> stats;
}

class Stat {
    Stat_definition config;

    // 面板属性(如攻击力)通常只读。状态属性(如当前HP)受伤时直接永久扣减此值。
    float baseValue;
    // 真正暴露给战斗公式读取的值，包含了当前所有 Buff 的加成。
    float currentValue;
    // 收集所有依附在该属性上的临时 Buff (如：战吼加攻)
    StatModifierList modifiers;
    boolean isDirty = true;
}

class StatModifierList {
    List<StatModifierInstance> additives;
    List<StatModifierInstance> multipliers;
    StatModifierInstance activeOverride = null; // 选最高优先级的
    int currentOverridePriority = -1;

    // currentValue = override ?? (baseValue + ΣAdd) * ΠMul
    float evaluate(float baseValue);
}

class StatModifierInstance extends StatusInstance<StateModifier> {
    float evaluate();
}
```

### EventBus

以 EventTagId 为键，直连底层的 SafeList<TriggerInstance> 监听队列。

```java
class EventBus {
    Int2ObjectMap<SafeList<TriggerInstance>> listeners;

    void dispatch(Event event) {
        SafeList<TriggerInstance> list = listeners.get(event.eventTagId);
        if (list == null) return;
        list.beginIterate();
        try {
            for (TriggerInstance trigger : list.items) {
                if (!trigger.isPendingKill()) trigger.onEventFired(event);
            }
        } finally {
            list.endIterate();
        }
    }
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
    
    struct StatValue {
        source: TargetSelector;
        statTag: str ->stat_definition;
        capture: StatCaptureMode;
    }

    // 上下文变量，优先localScope，然后instanceState，最后initSnapshot
    struct ContextVar { varTag: str ->gameplaytag; }

    // 事件载荷变量
    struct PayloadMagnitude { }
    struct PayloadVar { varTag: str ->gameplaytag; }

    // Status 层数
    struct CurrentStacks {}

    // 按层数缩放：(baseValue + perStackAdd * stacks) * perStackMul ^ stacks
    struct StackScaling {
        baseValue: float;
        perStackAdd: float;
        perStackMul: float;   // 1.0 = 无乘法缩放
    }
}

enum MathOp { Add; Sub; Mul; Div; Max; Min; }
enum StatCaptureMode { Current; Base; }
```

### Condition

提供执行准入标准，与 FloatValue 配合实现动态逻辑判断。

```cfg
interface Condition {
    struct Const { value: bool; }
    struct And { conditions: list<Condition>; }
    struct Or { conditions: list<Condition>; }
    struct Not { condition: Condition; }
    struct Compare {
        left: FloatValue;
        op: CompareOp;
        right: FloatValue;
    }

    struct HasTags {
        source: TargetSelector;
        query: TagQuery;
    }

    struct PayloadHasVar { varTag: str ->gameplaytag; }

    struct Chance { probability: FloatValue; } // 随机概率
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
    struct ContextVar { varTag: str ->gameplaytag; }

    struct PayloadInstigator {}
    struct PayloadTarget {}
    struct PayloadVar { varTag: str ->gameplaytag; }
}
```

---

## Core Entities

本章节定义系统的主体部分：Ability 是入口，Effect 是动作，Status 是持续状态，逻辑递进。


### Effect

`Effect` 是瞬间执行、**无状态**的指令流。

```cfg
interface Effect {
    struct EffectRef {
        sharedEffectId: int ->shared_effect; // 逻辑复用
        args: list<VarBinding>;
    }
    
    // --- 属性修改 ---
    struct ModifyStat { // 改 baseValue
        statTag: str ->stat_definition; // 如 "Stat.Resource.Stamina"
        op: ModifierOp;
        value: FloatValue;
    }

    //  结算管线触发（伤害/治疗/自定义管线）
    struct ApplyPipeline {
        pipeline: str ->resolution_pipeline;
        magnitude: FloatValue;
        tags: list<str> ->gameplaytag;   // 如 ["Damage.Element.Fire"]
        cuesOnExecute: list<str> ->gameplaytag;
    }

    struct Damage { // 快捷方式
        damageTags: list<str> ->gameplaytag; // 如: ["Damage.Element.Fire", "Damage.AttackType.Melee"]
        baseDamage: FloatValue;
        cuesOnHit: list<str> ->gameplaytag;
    }

    struct Heal { // 快捷方式
        baseHeal: FloatValue;
        cuesOnHeal: list<str> ->gameplaytag;
    }

    // --- 状态操作 ---
    // 引用标准 Status (适用于需要 UI 图标、多端网络同步的常规状态，如“中毒”, “护盾”)
    struct ApplyStatus {
        statusId: int ->status;
        captures: list<ArgCapture>;
    }

    // 内联型 Status (拥有完整功力，适用于无需 UI 显示的一次性专属机制)
    struct ApplyStatusInline {
        core: StatusCore;
        captures: list<ArgCapture>;
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

    // 发送事件
    struct SendEvent {
        eventTag: str ->event_definition;
        magnitude: FloatValue;
        extras: list<VarBinding>;
    }

    // 生成物 (子弹/法阵)
    struct SpawnObj {
        duration: FloatValue;
        objTags: list<str> ->gameplaytag;
        moveInfo: ObjMoveInfo; // 移动，弹道，碰撞在这里定义
        cuesWhileActive: list<str>; // 飞行时的呼啸声、法阵的底图特效
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
        extraTag: str ->gameplaytag;
        op: ModifierOp;
        value: FloatValue;
        overridePriority: int;
    }

    // --- Cue 触发
    struct FireCue {
        cueTag: str ->gameplaytag;
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
}

// 变量绑定
struct VarBinding {
    varTag: str ->gameplaytag;
    value: FloatValue;
}

// 参数捕获 (在StatusInstance里无Event参数，所以在Apply时要capture下来放到instanceState里)
struct ArgCapture {
    // 目标 Key：存储在 StatusInstance内部的 Context.instanceState 中的 Tag
    argTag: str -> gameplaytag;
    // 源 Value：在挂载瞬间，动态算出
    value: FloatValue;
    captureMode: ArgCaptureMode;
}

enum ArgCaptureMode {
    Snapshot; // 捕获挂载瞬间的值（如蓄力伤害）
    Dynamic;  // 每次求值时实时获取（如基于当前攻击力的百分比加成）
}

enum ModifierOp { Add; Mul; Override; }

// 高频复用的effect
table shared_effect[id] (json) {
    id: int;
    name: text;
    description: text;
    effect: Effect;
}
```

### TargetScan

```
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
    
    cuesWhileActive: list<str> ->gameplaytag;
    behaviors: list<Behavior>;
}

interface StackingPolicy {
    // 独立计时：每层独立倒计时
    struct Independent {
        maxStacks: int;
        overflowBehavior: OverflowBehavior;
    }

    // 共享计时：所有层共享一个倒计时
    struct Shared {
        maxStacks: int;
        refreshMode: RefreshMode;
        overflowBehavior: OverflowBehavior;
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

enum OverflowBehavior {
    Reject;             // 丢弃
    ReplaceOldest;      // 替换最早层
    ExecuteOverflow;    // 触发 OnOverflow 行为
}
```

### Behavior

Behavior 是附着在 Status 上的逻辑零件。运行时上下文是 StatusInstance（内含 Context）,Trigger 会接收到事件，上下文会有Payload，所有的事件Payload也都从这而起。


```
interface Behavior {
    // 属性修饰器
    struct StatModifier {
        statTag: str ->stat_definition;
        op: ModifierOp;
        value: FloatValue;
        requiresAll: list<Condition>;
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

        maxTriggers: int; // -1 = 无限
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

### Ability

Ability 是行为的入口点。

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    description: text;

    // 标签分类
    abilityTags: list<str> ->gameplaytag;

    // 准入条件
    requiresAll: list<Condition>;

    // cost
    costs: list<StatCost>;
    cooldown: FloatValue;
    commitEffects: Effect;

    // 效果
    effect: Effect;
}

struct StatCost {
    statTag: str ->stat_definition;
    value: FloatValue;
}
```

---

## combat_settings

本章节定义全局性的战斗规则，这些规则以配置表的形式存在，由底层引擎直接读取，作为战斗系统的“最高物理法则”。

```
table combat_settings[name] {
    name: str;
    tagRules: list<str> ->tag_rules;
    damgePipeline: str -> resolution_pipeline;
    healPipeline: str -> resolution_pipeline;
    maxRecursionDepth: int;
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

    // 打断正在运行且带有以下标签的 Ability
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
    description: text;

    flow: ValueFlow;

    // 事件 Tag（引擎自动广播）
    preEventTag: str ->event_definition;
    postEventTag: str ->event_definition;

    // 判定阶段（Pre 事件之后、属性扣减之前执行）
    checks: list<CheckStage>;

    // 属性扣减/增加管线
    allocations: list<AllocationLayer>;
}

enum ValueFlow {
    Deplete;       // 扣减（伤害类）
    Restore;       // 增加（治疗类）
}

struct AllocationLayer {
    targetStat: str ->stat_definition;
    conversionRate: float;
    allowOverflow: bool;
    onHitCue: str ->gameplaytag (nullable);
    onDepletedCue: str ->gameplaytag (nullable);
}
```

### CheckStage

判定阶段在 Pre Modifier 结算之后、属性扣减之前执行。用于实现闪避、格挡、暴击等核心战斗判定。每个阶段独立求值，互不干扰（可配置互斥关系）。

```cfg
struct CheckStage {
    name: text;

    // 触发条件
    skipIfPayloadHasAny: list<str> ->gameplaytag; // 检查Payload extras

    // 触发概率（从 Context/Payload 中动态取值）
    chance: FloatValue;

    // 判定成功时的效果
    grantPayloadTags: list<str> ->gameplaytag; // 写入Payload.extras
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
ApplyPipeline/Damage/Heal 调用
    ↓
1. 构建 Payload（snapshot magnitude + extras）
    ↓
2. 广播 Pre 事件 → Trigger 注入 Modifier（如减伤 Buff）
    ↓
3. 结算 Pre Modifier → 得到修正后的 magnitude
    ↓
4. 【判定阶段】依次执行 checks 列表：
    │  a. 检查 skipIfPayloadHas（互斥跳过）
    │  b. 掷骰 chance
    │  c. 成功：写入 grantPayloadTags，应用 magnitudeModifier，触发 onSuccessEffects
    ↓
5. 逐层属性扣减/增加（allocations）
    ↓
6. 广播 Post 事件 → Trigger 响应（如反伤、吸血）
```

**预置管线示例（含判定阶段）：**

```
resolution_pipeline {
    name: "StandardPhysicalDamage";
    flow: Deplete;
    preEventTag: "Event.Combat.Damage.Take.Pre";
    postEventTag: "Event.Combat.Damage.Take.Post";

    checks: [
        // 阶段1：闪避判定
        {
            name: "闪避";
            skipIfPayloadHasAny: [];
            chance: struct Math {
                op: Sub;
                a: struct StatValue {
                    source: struct PayloadTarget {};
                    statTag: "Stat.Combat.DodgeRate";
                    capture: Current;
                };
                b: struct StatValue {
                    source: struct PayloadInstigator {};
                    statTag: "Stat.Combat.Accuracy";
                    capture: Current;
                };
            };
            grantPayloadTags: ["Combat.Result.Dodged"];
            magnitudeModifiers: [{ op: Override; value: struct Const { value: 0.0; }; overridePriority: 999; }];
            onSuccessCues: ["Cue.Combat.Dodge"];
        },

        // 阶段2：格挡判定（闪避成功则跳过）
        {
            name: "格挡";
            skipIfPayloadHasAny: ["Combat.Result.Dodged"];
            chance: struct StatValue {
                source: struct PayloadTarget {};
                statTag: "Stat.Combat.BlockRate";
                capture: Current;
            };
            grantPayloadTags: ["Combat.Result.Blocked"];
            magnitudeModifiers: [{
                op: Mul;
                value: struct Math {
                    op: Sub;
                    a: struct Const { value: 1.0; };
                    b: struct StatValue {
                        source: struct PayloadTarget {};
                        statTag: "Stat.Combat.BlockEfficiency";
                        capture: Current;
                    };
                };
                overridePriority: 0;
            }];
            onSuccessCues: ["Cue.Combat.Block"];
        },

        // 阶段3：暴击判定（闪避成功则跳过）
        {
            name: "暴击";
            skipIfPayloadHasAny: ["Combat.Result.Dodged"];
            chance: struct Math {
                op: Sub;
                a: struct StatValue {
                    source: struct PayloadInstigator {};
                    statTag: "Stat.Combat.CritRate";
                    capture: Current;
                };
                b: struct StatValue {
                    source: struct PayloadTarget {};
                    statTag: "Stat.Combat.CritResist";
                    capture: Current;
                };
            };
            grantPayloadTags: ["Combat.Result.Critical"];
            magnitudeModifiers: [{
                op: Mul;
                value: struct StatValue {
                    source: struct PayloadInstigator {};
                    statTag: "Stat.Combat.CritDamage";
                    capture: Current;
                };
                overridePriority: 0;
            }];
            onSuccessCues: ["Cue.Combat.Critical"];
        }
    ];

    allocations: [
        { targetStat: "Stat.Shield.Current"; conversionRate: 1.0; allowOverflow: true;
          onHitCue: "Cue.Hit.Shield"; onDepletedCue: "Cue.Break.Shield"; },
        { targetStat: "Stat.HP.Current"; conversionRate: 1.0;
          onHitCue: "Cue.Hit.Flesh"; }
    ];
}

// 纯粹伤害（无视护盾，无判定阶段）
resolution_pipeline {
    name: "PureDamage";
    flow: Deplete;
    preEventTag: "Event.Combat.PureDamage.Take.Pre";
    postEventTag: "Event.Combat.PureDamage.Take.Post";
    allocations: [
        { targetStat: "Stat.HP.Current"; conversionRate: 1.0; }
    ];
}

// 治疗
resolution_pipeline {
    name: "StandardHeal";
    flow: Restore;
    preEventTag: "Event.Combat.Heal.Pre";
    postEventTag: "Event.Combat.Heal.Post";
    allocations: [
        { targetStat: "Stat.HP.Current"; conversionRate: 1.0;}
    ];
}
```

---

##  Gameplay Cue

客户端表现层。逻辑层仅输出 CueTag，客户端查此表执行视听反馈。

### Cue Schema

```cfg
table cue_registry[cueTag] {
    cueTag: str ->gameplaytag;
    handler: CueHandler;
}

interface CueHandler {
    // 瞬发型：触发即播放，自动回收
    struct Instant {
        vfx: list<VfxEntry>;
        sfx: list<SfxEntry>;
        animTriggers: list<AnimEntry>;
        cameraShakes: list<str>;
        damageTexts: list<DmgTextEntry>;
    }

    // 持续型：随 Status 生命周期创建/销毁
    struct Sustained {
        loopVfx: list<VfxEntry>;
        loopSfx: list<SfxEntry>;
        materialOverrides: list<MaterialEntry>;
        screenFilters: list<str>;
    }
}

struct VfxEntry {
    role: CueRole;
    asset: str;
    attach: VfxAttach;
    socket: str;
    scale: float;
}

enum VfxAttach { WorldStatic; FollowTarget; }

struct SfxEntry {
    role: CueRole;
    event: str;
}

struct AnimEntry {
    role: CueRole;
    trigger: str;
}

struct DmgTextEntry {
    role: CueRole;
    color: str;
    fontSize: int;
    icon: str;
    motion: str;
}

struct MaterialEntry {
    role: CueRole;
    material: str;
    slotIndex: int;     // -1 = 全部
}

enum CueRole { Target; Instigator; Causer }
```

### Cue Runtime

`cue_registry`的上下文是`(CueEvent)`

```java
record CueEvent(
    CueEventType type,
    int cueTagId,
    Actor target,
    Actor instigator,
    Actor causer,
    float magnitude
){}

enum CueEventType { Executed; Added; Removed; }
```

引擎根据 Cue 所在的上下文自动推导生命周期类型：

| 来源 | 推导类型 | 客户端行为 |
|---|---|---|
| `Effect.FireCue` / `ApplyPipeline.cuesOnExecute` / `AllocationLayer.onHitCue`  | `Executed` | 查表调用 Instant 处理器 |
| `Status.cuesWhileActive` | `Added` / `Removed` | 查表调用 Sustained 处理器 |


---

## Implementation Reference

本章提供核心架构的伪代码实现，展示运行时各组件如何协作。此处重点关注具体的技术实现细节，包括生命周期管理、无状态执行器等。

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

### StatusInstance

- `StatusComponent`(宏观调度层):主导 Tick 驱动与堆叠策略 (Stacking) 的全局路由。
- `StatusInstance` (生命周期层):专职接管单体状态的倒计时演算与层数突变,并动态装配底层行为单元。
- `BehaviorInstance` (业务执行层):细粒度的逻辑实体,实现“生命周期管理”与“具体战斗逻辑”的彻底正交。

```java
class StatusComponent {
    SafeList<StatusInstance> statusInstances;
    Actor ownerActor;
    // Tick 驱动倒计时,通过 statusInstances.beginIterate() 安全遍历
}

class StatusInstance implements IPendingKill {
    StatusCore coreConfig;
    Context applyContext;
    StatusComponent ownerComponent;
    // 容纳实例化出来的具体行为（StatModifierInstance, PeriodicInstance, TriggerInstance等）
    List<BehaviorInstance<?>> behaviorInstances;

    float remainingDuration;
    int currentStacks;
    boolean pendingKill;
}

abstract class BehaviorInstance<T extends Behavior> {
    T config;
    StatusInstance parentStatus;
    abstract void onStart();
    abstract void onEnd();
    void tick(float dt) {}
}
```

### Stateless Executors

`Effect`、`FloatValue`、`Condition`、`TargetSelector`、`TargetScan` 作为**无状态**指令节点,本身不维护生命周期,被调用时即时消费上下文结算。

```java
class Effects {
    static void execute(Effect cfg, Context ctx, Payload payload);
}

class FloatValues {
    static float evaluate(FloatValue cfg, Context ctx, Payload payload);
}

class Conditions {
    static boolean test(Condition cfg, Context ctx, Payload payload);
}

class TargetSelectors {
    static Actor select(TargetSelector cfg, Context ctx, Payload payload);
}

class TargetScans {
    static Collection<Actor> scan(TargetScan cfg, Context ctx, Payload payload);
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
    duration: struct Const { value: 5.0; };
    stackingPolicy: struct Single { refreshMode: ResetDuration; };
    behaviors: [
        struct Trigger {
            listenEvent: "Event.Combat.Damage.Take.Pre";
            effect: struct ModifyPayloadMagnitude {
                op: Mul;
                value: struct Const { value: 0.6; };
                overridePriority: 0;
            };
            maxTriggers: -1;
            cooldown: 0;
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
    duration: struct Const { value: -1.0; };
    stackingPolicy: struct Single { refreshMode: KeepDuration; };
    behaviors: [
        struct Trigger {
            listenEvent: "Event.Combat.Damage.Take.Post";
            requiresAll: [
                struct Compare {
                    left: struct PayloadMagnitude {};
                    op: Gte;
                    right: struct Const { value: 50.0; };
                }
            ];
            effect: struct WithTarget {
                target: struct PayloadInstigator {};
                effect: struct ApplyPipeline {
                    pipeline: "PureDamage";
                    target: struct ContextTarget {};
                    magnitude: struct Const { value: 10.0; };
                    tags: ["Damage.Type.Reflected"];
                };
            };
            maxTriggers: -1;
            cooldown: 0;
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
    duration: struct Const { value: 8.0; };
    stackingPolicy: struct Independent {
        maxStacks: 5;
        overflowBehavior: ExecuteOverflow;
    };
    cuesWhileActive: ["Cue.Status.Poison"];
    behaviors: [
        struct Periodic {
            period: struct Const { value: 2.0; };
            executeOnApply: false;
            effect: struct ApplyPipeline {
                pipeline: "PureDamage";
                target: struct ContextTarget {};
                magnitude: struct StackScaling {
                    baseValue: 5.0;
                    perStackAdd: 3.0;
                    perStackMul: 1.0;
                };
                tags: ["Damage.Element.Poison"];
            };
        },
        struct OnOverflow {
            effect: struct Sequence {
                effects: [
                    struct ApplyPipeline {
                        pipeline: "PureDamage";
                        target: struct ContextTarget {};
                        magnitude: struct Const { value: 80.0; };
                        tags: ["Damage.Element.Poison", "Damage.Type.Burst"];
                        cuesOnExecute: ["Cue.Combat.PoisonBurst"];
                    },
                    struct RemoveStatus {
                        target: struct ContextTarget {};
                        statusId: 3001;
                        stacksToRemove: -1;
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
    duration: struct Const { value: -1.0; };
    stackingPolicy: struct Single { refreshMode: KeepDuration; };
    behaviors: [
        struct Trigger {
            listenEvent: "Event.Combat.Damage.Take.Post";
            requiresAll: [
                // 检查 Payload 中是否有暴击标记（由 CheckStage 写入）
                struct PayloadHasVar { varTag: "Combat.Result.Critical"; }
            ];
            effect: struct WithTarget {
                target: struct PayloadTarget {};
                body: struct ApplyPipeline {
                    pipeline: "PureDamage";
                    target: struct ContextTarget {};
                    magnitude: struct Const { value: 25.0; };
                    tags: ["Damage.Type.Bonus"];
                    cuesOnExecute: ["Cue.Combat.CritBonus"];
                };
            };
            maxTriggers: -1;
            cooldown: 1.0;
        }
    ];
}
```