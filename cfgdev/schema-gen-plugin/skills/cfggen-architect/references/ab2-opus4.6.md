
# 能力系统设计 (Ability System Design)

基于 Unreal GAS 核心理念的**全数据驱动**技能系统配置标准。本文档是**架构基准**，具体游戏应在此基础上裁剪和扩展。

---

## Philosophy

1. **层级化标签驱动**：树状 Tag（如 `State.Debuff.Control.Stun`）是逻辑交互的唯一通用语言。支持父级包含查询——查询 `State.Debuff` 可命中 `State.Debuff.Control.Stun`。

2. **两阶段事件管线**：所有逻辑通信通过 `EventBus` 广播标准消息。`Pre` 阶段注入 `Modifier` 篡改数值（如减伤），`Post` 阶段触发副作用（如反伤）。源与监听者彻底解耦。

3. **逻辑与表现隔离**：逻辑层仅输出 `CueTag`，客户端独立维护 Tag 到资源的映射。

4. **三层正交分离**：`Ability`（行为入口）、`Status`（持续状态容器）、`Effect`（无状态原子指令）严格正交。通过组合构建复杂机制。

5. **Context/Payload 双轨数据流**：`Context` 携带技能执行的完整环境（发起者、目标、局部变量），`Payload` 携带事件发生时的瞬时快照（攻击者、受害者、伤害值）。两条轨道各司其职，在 Trigger 响应中交汇——Context 提供"我是谁"，Payload 提供"发生了什么"。

---

## Part 1: Data Foundation

定义系统的"词汇表"——标签、属性、事件的原子定义。

### 1.1 GameplayTag

```cfg
// Tag 字典注册表
// 所有使用到的 Tag 必须在此注册声明
// 层级关系通过名称中的 "." 分隔符隐式表达
// 程序启动时自动构建父子索引
table gameplaytag[name] {
    name: str;          // 如 "State.Debuff.Control.Stun"
    description: text;
    [enumId];           // 自增整型ID，用于运行时高效查找
}
```

**命名规范**（由工具链强制校验）：

| 前缀 | 用途 | 示例 |
|---|---|---|
| `State.*` | 实体状态标记 | `State.Debuff.Control.Stun` |
| `Ability.*` | 技能分类标签 | `Ability.Type.Spell` |
| `Damage.*` | 伤害/治疗分类 | `Damage.Element.Fire` |
| `Event.*` | 事件路由键 | `Event.Combat.Damage.Deal.Pre` |
| `Cue.*` | 表现层路由键 | `Cue.Combat.Hit.Heavy` |
| `Stat.*` | 属性标识 | `Stat.Combat.Attack` |
| `Var.*` | 变量键名 | `Var.ChargeTime` |
| `Cooldown.*` | 冷却组键名 | `Cooldown.Ability.Fireball` |

### 1.2 Stat Definition

```cfg
table stat_definition[statTag] {
    statTag: str ->gameplaytag;    // 如 "Stat.Combat.Attack"
    name: text;

    defaultValue: float;
    isPersistent: bool;

    // --- 边界约束 ---
    minLimit: StatLimit;
    maxLimit: StatLimit;

    // --- 级联策略 ---
    clampMode: StatClampMode;

    // --- 归零联动 ---
    // 当 currentValue 降至 minLimit 时，自动向宿主挂载的 Tag
    // 例: HP 归零 -> 挂 "State.Dead"
    onDepletedGrantTag: str ->gameplaytag (nullable);
}

interface StatLimit {
    struct Const { value: float; }
    struct StatRef { statTag: str ->stat_definition; }
    struct None {}
}

enum StatClampMode {
    Absolute;           // 截断到新上限
    MaintainPercent;    // 维持百分比
    None;               // 不联动
}
```

### 1.3 Event Definition

```cfg
table event_definition[eventTag] {
    eventTag: str ->gameplaytag;   // 如 "Event.Combat.Damage.Take"
    description: text;

    // 声明该事件 Payload 中携带的标准变量
    expectedVars: list<EventVarDecl>;

    // 是否支持两阶段 (Pre/Post) 处理
    // true: 引擎自动派生 .Pre 和 .Post 子事件
    hasTwoPhase: bool;
}

struct EventVarDecl {
    varTag: str ->gameplaytag;     // 如 "Var.Damage.Element"
    type: VarType;
    description: text;
}

enum VarType {
    Float;
    Int;
    ActorRef;
    TagId;
}
```

---

## Part 2: Runtime Core

定义运行时的核心数据结构。Context 和 Payload 是两条独立的数据轨道，在 Trigger 响应中交汇。

### 2.1 Context

Context 封装技能/状态执行的完整环境。所有 Effect 和表达式依赖它进行求值。

```java
record Context(
    Actor instigator,       // 效果的最终发起者（如：玩家）
    Actor causer,           // 效果的物理媒介（如：火球）
    ReadOnlyStore snapshot, // 初始化时冻结的快照参数

    // --- 可变数据（改变时创建新 Context）---
    Actor target,           // 当前作用目标，由 WithTarget 改变
    Store instanceState,    // StatusInstance 级状态，跨节点共享
    Store localScope,       // 局部作用域，WithLocalVar 时创建新层
    int recursionDepth      // 死循环防护
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

### 2.2 Payload

Payload 是事件携带的瞬时载荷。Trigger 通过它读取"发生了什么"并注入修饰。

```java
record Payload(
    Context sourceContext,  // 引发该事件的原始技能 Context（用于追溯来源）
    Actor instigator,       // 事件的绝对发起者（谁打的这一刀）
    Actor target,           // 事件的绝对承受者（谁挨的这一刀）

    float magnitude,        // 主值（如伤害量、治疗量）
    Store extras,           // 附加数据（如元素类型、暴击标记）

    // --- Modifier 收集器（Pre 阶段使用）---
    ChangeSet magnitudeChanges,
    Int2ObjectMap<ChangeSet> extraChanges
){}

class ChangeSet {
    FloatList additives;
    FloatList multipliers;
    OverrideOp override;

    // 结算优先级: override > (base + ΣAdd) * ΠMul
    float resolve(float baseValue);
}

record OverrideOp(float value, int priority){}
```

### 2.3 Actor & Components

```java
class Actor {
    TagContainer tags;
    StatComponent stats;
    StatusComponent statuses;
    EventBus eventBus;
}
```

### 2.4 TagContainer

底层采用 **"引用计数 + 写入时展开"** 策略，实现 O(1) 的父级包含查询。

```java
class TagContainer {
    Int2IntMap refCounts;  // tagEnumId -> count

    void addTag(int tagId) {
        for (int id : TagRegistry.selfAndAncestors(tagId))
            refCounts.merge(id, 1, Integer::sum);
    }

    void removeTag(int tagId) {
        for (int id : TagRegistry.selfAndAncestors(tagId)) {
            int n = refCounts.merge(id, -1, Integer::sum);
            if (n <= 0) refCounts.remove(id);
        }
    }

    boolean hasTag(int tagId) {
        return refCounts.containsKey(tagId);
    }
}
```

### 2.5 StatComponent

```java
class StatComponent {
    Int2ObjectMap<Stat> stats;
}

class Stat {
    StatDefinition config;
    float baseValue;
    float currentValue;
    StatModifierList modifiers;
    boolean isDirty = true;
}

class StatModifierList {
    List<StatModifierInstance> additives;
    List<StatModifierInstance> multipliers;
    StatModifierInstance activeOverride;
    int currentOverridePriority = -1;

    // currentValue = override ?? (baseValue + ΣAdd) * ΠMul
    float evaluate(float baseValue);
}
```

### 2.6 EventBus

```java
class EventBus {
    Int2ObjectMap<SafeList<TriggerInstance>> listeners;

    void dispatch(int eventTagId, Context ctx, Payload payload) {
        SafeList<TriggerInstance> list = listeners.get(eventTagId);
        if (list == null) return;
        list.beginIterate();
        try {
            for (TriggerInstance t : list.items)
                if (!t.isPendingKill()) t.onEvent(ctx, payload);
        } finally {
            list.endIterate();
        }
    }
}
```

---

## Part 3: Expression Layer

运行时求值的表达式与条件系统。运行时上下文始终是 `(Context, Payload)`——Context 提供"我是谁"，Payload 提供"发生了什么"（非事件响应场景下 Payload 为 null）。

### 3.1 TargetSelector

```cfg
interface TargetSelector {
    // --- Context 维度 ---
    struct ContextTarget {}
    struct ContextInstigator {}
    struct ContextCauser {}
    struct ContextVar { varTag: str ->gameplaytag; }

    // --- Payload 维度 ---
    struct PayloadInstigator {}
    struct PayloadTarget {}
    struct PayloadVar { varTag: str ->gameplaytag; }
}
```

### 3.2 FloatValue

```cfg
interface FloatValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: FloatValue; b: FloatValue; }

    struct StatOf {
        source: TargetSelector;
        statTag: str ->stat_definition;
        capture: StatCaptureMode;
    }

    // 上下文变量：优先 localScope -> instanceState -> snapshot
    struct ContextVar { varTag: str ->gameplaytag; }

    // 事件载荷读取
    struct PayloadMagnitude {}
    struct PayloadVar { varTag: str ->gameplaytag; }

    // Status 层数
    struct CurrentStacks {}

    // 按层数缩放：(baseValue + perStackAdd * stacks) * perStackMul ^ stacks
    struct StackScaling {
        baseValue: float;
        perStackAdd: float;
        perStackMul: float;   // 1.0 = 无乘法缩放
    }

    // 条件转数值：true -> 1.0, false -> 0.0
    struct ConditionToFloat { condition: Condition; }
}

enum MathOp { Add; Sub; Mul; Div; Max; Min; }
enum StatCaptureMode { Current; Base; }
```

### 3.3 Condition

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

    // 检查 Actor 身上的标签
    struct HasTags {
        source: TargetSelector;
        query: TagQuery;
    }

    // 检查 Payload extras 中是否携带特定标签/变量
    struct PayloadHasVar {
        varTag: str ->gameplaytag;
    }

    // 随机概率
    struct Chance { probability: FloatValue; }
}

enum CompareOp { Gt; Gte; Lt; Lte; Eq; Neq; }

struct TagQuery {
    requireAll: list<str> ->gameplaytag;
    requireAny: list<str> ->gameplaytag;
    exclude: list<str> ->gameplaytag;
}
```

---

## Part 4: Core Entities

### 4.1 Effect

`Effect` 是瞬间执行、**无状态**的指令节点。既包含原子操作，也包含控制流编排。其运行时上下文是 `(Context, Payload)`。

```cfg
interface Effect {
    // ============================================================
    //  引用复用
    // ============================================================
    struct EffectRef {
        refId: int ->shared_effect;
        args: list<VarBinding>;
    }

    // ============================================================
    //  属性操作
    // ============================================================
    struct ModifyStat {
        target: TargetSelector;
        statTag: str ->stat_definition;
        op: ModifierOp;
        value: FloatValue;
    }

    // ============================================================
    //  结算管线触发（伤害/治疗/自定义管线）
    // ============================================================
    struct ApplyPipeline {
        pipelineId: int ->effect_pipeline;
        target: TargetSelector;
        magnitude: FloatValue;
        tags: list<str> ->gameplaytag;   // 如 ["Damage.Element.Fire"]
        cuesOnExecute: list<str> ->gameplaytag;
    }

    // ============================================================
    //  状态操作
    // ============================================================
    struct ApplyStatus {
        target: TargetSelector;
        statusId: int ->status;
        captures: list<ArgCapture>;
    }

    struct RemoveStatus {
        target: TargetSelector;
        statusId: int ->status;
        stacksToRemove: int;    // -1 = 全部移除
    }

    struct RemoveStatusByTag {
        target: TargetSelector;
        withAnyTags: list<str> ->gameplaytag;
    }

    // 快捷方式：极简临时标签
    struct GrantTags {
        target: TargetSelector;
        tags: list<str> ->gameplaytag;
        duration: FloatValue;
    }

    // ============================================================
    //  事件广播
    // ============================================================
    struct SendEvent {
        target: TargetSelector;
        eventTag: str ->event_definition;
        magnitude: FloatValue;
        extras: list<VarBinding>;
    }

    // ============================================================
    //  载荷篡改（仅在 Pre 阶段 Trigger 中有效）
    // ============================================================
    struct ModifyPayloadMagnitude {
        op: ModifierOp;
        value: FloatValue;
        overridePriority: int;
    }

    struct ModifyPayloadExtra {
        extraTag: str ->gameplaytag;
        op: ModifierOp;
        value: FloatValue;
        overridePriority: int;
    }

    // ============================================================
    //  Cue 触发
    // ============================================================
    struct FireCue {
        cueTag: str ->gameplaytag;
        target: TargetSelector;
        magnitude: FloatValue;
    }

    // ============================================================
    //  控制流 & 作用域
    // ============================================================
    struct Sequence { effects: list<Effect>; }

    struct Conditional {
        condition: Condition;
        then: Effect;
        otherwise: Effect (nullable);
    }

    struct Repeat {
        count: FloatValue;
        indexVar: str ->gameplaytag (nullable); // 注入 localScope
        effect: Effect;
    }

    struct WithTarget {
        target: TargetSelector;
        effect: Effect;
    }

    struct WithTargets {
        scan: TargetScan;
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

// 参数捕获（ApplyStatus 时从当前上下文捕获值存入 StatusInstance）
struct ArgCapture {
    argTag: str ->gameplaytag;
    value: FloatValue;
    captureMode: ArgCaptureMode;
}

enum ArgCaptureMode {
    Snapshot;    // 捕获挂载瞬间的值
    Dynamic;     // 每次求值时实时计算
}

enum ModifierOp { Add; Mul; Override; }

// 高频复用的 Effect
table shared_effect[id] (json) {
    id: int;
    name: text;
    description: text;
    effect: Effect;
}
```

### 4.2 TargetScan

```cfg
interface TargetScan {
    struct Sphere {
        center: TargetSelector;
        radius: FloatValue;
        filter: TargetFilter;
    }

    struct Sector {
        center: TargetSelector;
        facingOf: TargetSelector;   // 取该实体的朝向作为扇形正方向
        radius: FloatValue;
        angle: FloatValue;
        filter: TargetFilter;
    }

    struct Box {
        center: TargetSelector;
        facingOf: TargetSelector;
        width: FloatValue;
        length: FloatValue;
        filter: TargetFilter;
    }

    struct PartyOf {
        source: TargetSelector;
        filter: TargetFilter;
    }
}

struct TargetFilter {
    relationTo: TargetSelector;
    allowedRelations: list<Relation>;
    tagQuery: TagQuery;
    exclude: list<TargetSelector>;
    maxCount: int;              // -1 = 无限
    sort: SortStrategy;
}

enum Relation { Self; Friendly; Hostile; Neutral; }
enum SortStrategy { Nearest; Farthest; HpPercentAsc; HpPercentDesc; Random; None; }
```

### 4.3 Effect Pipeline

将伤害/治疗的结算逻辑配置化。引擎按 Pipeline 定义执行多阶段事件广播与逐层属性扣减。

```cfg
table effect_pipeline[id] (json) {
    id: int;
    name: text;         // 如 "StandardDamage", "PureDamage", "StandardHeal"
    description: text;

    direction: PipelineDirection;

    // 事件 Tag（引擎自动广播）
    preEventTag: str ->event_definition;
    postEventTag: str ->event_definition;

    // 判定阶段（Pre 事件之后、属性扣减之前执行）
    judgments: list<JudgmentStage>;

    // 属性扣减/增加管线
    layers: list<PipelineLayer>;
}

enum PipelineDirection {
    Decrease;       // 扣减（伤害类）
    Increase;       // 增加（治疗类）
}

struct PipelineLayer {
    targetStat: str ->stat_definition;
    conversionRate: float;
    allowOverflow: bool;
    onHitCue: str ->gameplaytag (nullable);
    onBreakCue: str ->gameplaytag (nullable);
}
```

### 4.4 Judgment Stage（判定阶段）

判定阶段在 Pre Modifier 结算之后、属性扣减之前执行。用于实现闪避、格挡、暴击等核心战斗判定。每个阶段独立求值，互不干扰（可配置互斥关系）。

```cfg
struct JudgmentStage {
    name: text;                          // 策划备注："闪避判定"/"暴击判定"

    // --- 触发条件 ---
    // 需要 Payload extras 中不包含某些标记（互斥控制）
    // 例：已闪避则跳过暴击判定
    skipIfPayloadHas: list<str> ->gameplaytag;

    // 触发概率（从 Context/Payload 中动态取值）
    chance: FloatValue;

    // --- 判定成功时的效果 ---
    // 向 Payload.extras 写入标记（供后续阶段检查或 Post 事件读取）
    grantPayloadTags: list<str> ->gameplaytag;

    // 对 magnitude 的修饰
    magnitudeModifier: JudgmentModifier (nullable);

    // 判定成功时触发的瞬间效果（如闪避成功给自己加个 buff）
    onSuccessEffect: Effect (nullable);

    // 判定成功时触发的 Cue
    onSuccessCue: str ->gameplaytag (nullable);
}

struct JudgmentModifier {
    op: ModifierOp;
    value: FloatValue;
    overridePriority: int;       // 仅 Override 时生效
}
```

**设计说明：**

判定阶段的执行时机在管线中被精确定义：

```
ApplyPipeline 调用
    ↓
1. 构建 Payload（snapshot magnitude + extras）
    ↓
2. 广播 Pre 事件 → Trigger 注入 Modifier（如减伤 Buff）
    ↓
3. 结算 Pre Modifier → 得到修正后的 magnitude
    ↓
4. 【判定阶段】依次执行 judgments 列表：
    │  a. 检查 skipIfPayloadHas（互斥跳过）
    │  b. 掷骰 chance
    │  c. 成功：写入 grantPayloadTags，应用 magnitudeModifier，触发 onSuccessEffect/Cue
    ↓
5. 逐层属性扣减/增加（layers）
    ↓
6. 广播 Post 事件 → Trigger 响应（如反伤、吸血）
```

**预置管线示例（含判定阶段）：**

```
effect_pipeline {
    id: 1;
    name: "StandardPhysicalDamage";
    direction: Decrease;
    preEventTag: "Event.Combat.Damage.Take.Pre";
    postEventTag: "Event.Combat.Damage.Take.Post";

    judgments: [
        // 阶段1：闪避判定
        {
            name: "闪避";
            skipIfPayloadHas: [];
            chance: struct Math {
                op: Sub;
                a: struct StatOf {
                    source: struct PayloadTarget {};
                    statTag: "Stat.Combat.DodgeRate";
                    capture: Current;
                };
                b: struct StatOf {
                    source: struct PayloadInstigator {};
                    statTag: "Stat.Combat.Accuracy";
                    capture: Current;
                };
            };
            grantPayloadTags: ["Combat.Result.Dodged"];
            magnitudeModifier: { op: Override; value: struct Const { value: 0.0; }; overridePriority: 999; };
            onSuccessCue: "Cue.Combat.Dodge";
        },

        // 阶段2：格挡判定（闪避成功则跳过）
        {
            name: "格挡";
            skipIfPayloadHas: ["Combat.Result.Dodged"];
            chance: struct StatOf {
                source: struct PayloadTarget {};
                statTag: "Stat.Combat.BlockRate";
                capture: Current;
            };
            grantPayloadTags: ["Combat.Result.Blocked"];
            magnitudeModifier: {
                op: Mul;
                value: struct Math {
                    op: Sub;
                    a: struct Const { value: 1.0; };
                    b: struct StatOf {
                        source: struct PayloadTarget {};
                        statTag: "Stat.Combat.BlockEfficiency";
                        capture: Current;
                    };
                };
                overridePriority: 0;
            };
            onSuccessCue: "Cue.Combat.Block";
        },

        // 阶段3：暴击判定（闪避成功则跳过）
        {
            name: "暴击";
            skipIfPayloadHas: ["Combat.Result.Dodged"];
            chance: struct Math {
                op: Sub;
                a: struct StatOf {
                    source: struct PayloadInstigator {};
                    statTag: "Stat.Combat.CritRate";
                    capture: Current;
                };
                b: struct StatOf {
                    source: struct PayloadTarget {};
                    statTag: "Stat.Combat.CritResist";
                    capture: Current;
                };
            };
            grantPayloadTags: ["Combat.Result.Critical"];
            magnitudeModifier: {
                op: Mul;
                value: struct StatOf {
                    source: struct PayloadInstigator {};
                    statTag: "Stat.Combat.CritDamage";
                    capture: Current;
                };
                overridePriority: 0;
            };
            onSuccessCue: "Cue.Combat.Critical";
        }
    ];

    layers: [
        { targetStat: "Stat.Shield.Current"; conversionRate: 1.0; allowOverflow: true;
          onHitCue: "Cue.Hit.Shield"; onBreakCue: "Cue.Break.Shield"; },
        { targetStat: "Stat.HP.Current"; conversionRate: 1.0; allowOverflow: false;
          onHitCue: "Cue.Hit.Flesh"; }
    ];
}

// 纯粹伤害（无视护盾，无判定阶段）
effect_pipeline {
    id: 2;
    name: "PureDamage";
    direction: Decrease;
    preEventTag: "Event.Combat.PureDamage.Take.Pre";
    postEventTag: "Event.Combat.PureDamage.Take.Post";
    judgments: [];
    layers: [
        { targetStat: "Stat.HP.Current"; conversionRate: 1.0; allowOverflow: false; }
    ];
}

// 治疗
effect_pipeline {
    id: 3;
    name: "StandardHeal";
    direction: Increase;
    preEventTag: "Event.Combat.Heal.Pre";
    postEventTag: "Event.Combat.Heal.Post";
    judgments: [];
    layers: [
        { targetStat: "Stat.HP.Current"; conversionRate: 1.0; allowOverflow: false; }
    ];
}
```

### 4.5 Status

Status 是持续状态的容器，挂载在 Actor 上，拥有独立的生命周期、堆叠策略和行为零件。

```cfg
table status[id] (json) {
    id: int;
    name: text;
    description: text;
    icon: str;

    // --- 标签声明 ---
    statusTags: list<str> ->gameplaytag;     // Status 自身分类（用于外部查询/移除）
    grantedTags: list<str> ->gameplaytag;    // 存在期间授予宿主的标签

    // --- 生命周期 ---
    duration: FloatValue;           // -1 = 永久
    stackingPolicy: StackingPolicy;

    // --- 表现 ---
    cuesWhileActive: list<str> ->gameplaytag;

    // --- 行为零件 ---
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

### 4.6 Behavior

Behavior 是附着在 Status 上的逻辑零件。运行时上下文是 `StatusInstance`（内含 `Context`）。

```cfg
interface Behavior {
    // 属性修饰器
    struct StatModifier {
        statTag: str ->stat_definition;
        op: ModifierOp;
        value: FloatValue;                  // 支持 CurrentStacks / StackScaling
        activeCondition: Condition (nullable);
    }

    // 周期性触发
    struct Periodic {
        period: FloatValue;
        executeOnApply: bool;
        effect: Effect;
    }

    // 事件触发器
    struct Trigger {
        listenEvent: str ->event_definition;
        phase: EventPhase;
        conditions: list<Condition>;
        effect: Effect;
        maxTriggers: int;           // -1 = 无限
        cooldown: float;
    }

    // 生命周期钩子
    struct OnApply { effect: Effect; }
    struct OnRemove { effect: Effect; }
    struct OnStackChange { effect: Effect; }
    struct OnOverflow { effect: Effect; }
}

enum EventPhase { Pre; Post; }
```

### 4.7 Ability

Ability 是行为的入口点。

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    description: text;

    // --- 标签分类 ---
    abilityTags: list<str> ->gameplaytag;

    // --- 激活条件 ---
    activationRequired: TagQuery;
    activationBlocked: TagQuery;

    // --- 互斥与打断 ---
    cancelAbilitiesWithTags: list<str> ->gameplaytag;

    // --- 资源消耗 ---
    costs: list<StatCost>;

    // --- 冷却 ---
    cooldowns: list<CooldownEntry>;

    // --- 执行体 ---
    effect: Effect;
}

struct StatCost {
    statTag: str ->stat_definition;
    value: FloatValue;
}

struct CooldownEntry {
    cooldownTag: str ->gameplaytag;
    duration: FloatValue;
}
```

---

## Part 5: Global Rules

### 5.1 Tag Interaction Rules

所有标签间的交互规则统一收口于此。Status 自身只声明 `grantedTags`，不再自行配置 block/cancel。

```cfg
table tag_rules[id] (json) {
    id: int;
    name: text;
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
    id: 1; name: "CoreCombatRules";
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

### 5.2 Global Settings

```cfg
table global_settings[name] {
    name: str;
    healthStat: str ->stat_definition;
    maxRecursionDepth: int;
}
```

---

## Part 6: Gameplay Cue

客户端表现层。逻辑层仅输出 CueTag，客户端查此表执行视听反馈。

### 6.1 Cue Schema

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

enum CueRole { Target; Instigator; }
```

### 6.2 Cue Runtime

引擎根据 Cue 所在的上下文自动推导生命周期类型：

| 来源 | 推导类型 | 客户端行为 |
|---|---|---|
| `Effect.FireCue` / `ApplyPipeline.cuesOnExecute` / `PipelineLayer.onHitCue` / `JudgmentStage.onSuccessCue` | `Executed` | 查表调用 Instant 处理器 |
| `Status.cuesWhileActive` | `Added` / `Removed` | 查表调用 Sustained 处理器 |

```java
record CueEvent(
    CueEventType type,      // Executed / Added / Removed
    int cueTagId,
    Actor target,
    Actor instigator,
    Actor causer,
    float magnitude
){}

enum CueEventType { Executed; Added; Removed; }
```

---

## Part 7: Implementation Reference

### 7.1 SafeList

```java
interface IPendingKill {
    boolean isPendingKill();
    void markPendingKill();
}

class SafeList<T extends IPendingKill> {
    List<T> items;
    List<T> pendingAdds;
    int iterDepth;
    boolean dirty;

    void add(T item) {
        if (iterDepth > 0) pendingAdds.add(item);
        else items.add(item);
    }

    void remove(T item) {
        item.markPendingKill();
        dirty = true;
    }

    void beginIterate() { iterDepth++; }

    void endIterate() {
        if (--iterDepth == 0) {
            if (!pendingAdds.isEmpty()) {
                items.addAll(pendingAdds);
                pendingAdds.clear();
            }
            if (dirty) { compact(); dirty = false; }
        }
    }

    private void compact() {
        int w = 0;
        for (int r = 0; r < items.size(); r++) {
            if (!items.get(r).isPendingKill()) {
                if (w != r) items.set(w, items.get(r));
                w++;
            }
        }
        items.subList(w, items.size()).clear();
    }
}
```

### 7.2 StatusInstance & BehaviorInstance

```java
class StatusComponent {
    SafeList<StatusInstance> instances;
    Actor owner;

    void tick(float dt) {
        instances.beginIterate();
        try {
            for (StatusInstance si : instances.items)
                if (!si.isPendingKill()) si.tick(dt);
        } finally {
            instances.endIterate();
        }
    }

    StatusInstance applyStatus(int statusId, Context sourceCtx, List<ArgCapture> captures) {
        StatusConfig cfg = StatusRegistry.get(statusId);

        // 1. 免疫检查
        if (TagRuleEngine.isImmune(owner, cfg.grantedTags)) return null;

        // 2. 查找已有实例 -> 堆叠处理
        StatusInstance existing = findByStatusId(statusId);
        if (existing != null)
            return existing.handleStacking(sourceCtx, captures);

        // 3. 创建新实例
        StatusInstance si = new StatusInstance(cfg, sourceCtx, captures, this);
        si.start();
        instances.add(si);
        return si;
    }
}

class StatusInstance implements IPendingKill {
    StatusConfig config;
    Context ctx;
    List<BehaviorInstance<?>> behaviors;

    float remainingDuration;
    int currentStacks;
    boolean pendingKill;

    void start() {
        // 授予标签
        for (int tagId : config.grantedTags)
            ctx.target().tags.addTag(tagId);

        // 驱散
        TagRuleEngine.onTagsGranted(ctx.target(), config.grantedTags);

        // 实例化行为零件
        for (Behavior b : config.behaviors)
            behaviors.add(BehaviorFactory.create(b, this));

        // Cue
        for (int cueTag : config.cuesWhileActive)
            CueManager.send(CueEventType.Added, cueTag, ctx);

        // 启动行为
        for (BehaviorInstance<?> bi : behaviors)
            bi.onStart();
    }

    void tick(float dt) {
        if (remainingDuration > 0) {
            remainingDuration -= dt;
            if (remainingDuration <= 0) { expire(); return; }
        }
        for (BehaviorInstance<?> bi : behaviors)
            bi.tick(dt);
    }

    void expire() {
        for (BehaviorInstance<?> bi : behaviors)
            bi.onEnd();

        for (int tagId : config.grantedTags)
            ctx.target().tags.removeTag(tagId);

        for (int cueTag : config.cuesWhileActive)
            CueManager.send(CueEventType.Removed, cueTag, ctx);

        markPendingKill();
    }
}

abstract class BehaviorInstance<T> {
    T config;
    StatusInstance parent;
    abstract void onStart();
    abstract void onEnd();
    void tick(float dt) {} // 默认空实现
}
```

### 7.3 Pipeline Execution

```java
class PipelineExecutor {

    static void execute(EffectPipelineConfig pipeline, Context ctx, Payload payload) {

        // ---- 1. Pre 阶段 ----
        ctx.target().eventBus.dispatch(pipeline.preEventTagId, ctx, payload);

        // ---- 2. 结算 Pre Modifier ----
        float magnitude = payload.magnitudeChanges().resolve(payload.magnitude());
        magnitude = Math.max(0, magnitude);

        // ---- 3. 判定阶段 (Judgment) ----
        for (JudgmentStage js : pipeline.judgments) {
            // 3a. 互斥跳过
            boolean skip = false;
            for (int skipTag : js.skipIfPayloadHas) {
                if (payload.extras().hasTag(skipTag)) { skip = true; break; }
            }
            if (skip) continue;

            // 3b. 掷骰
            float chance = FloatValues.evaluate(js.chance, ctx, payload);
            if (Math.random() >= chance) continue;

            // 3c. 判定成功
            for (int tag : js.grantPayloadTags)
                payload.extras().setFloat(tag, 1.0f);

            if (js.magnitudeModifier != null) {
                JudgmentModifier jm = js.magnitudeModifier;
                payload.magnitudeChanges().inject(jm.op, 
                    FloatValues.evaluate(jm.value, ctx, payload), jm.overridePriority);
            }

            if (js.onSuccessEffect != null)
                Effects.execute(js.onSuccessEffect, ctx, payload);

            if (js.onSuccessCue != null)
                CueManager.fire(js.onSuccessCue, ctx, magnitude);
        }

        // ---- 4. 重新结算（判定阶段可能注入了新 Modifier）----
        magnitude = payload.magnitudeChanges().resolve(payload.magnitude());
        magnitude = Math.max(0, magnitude);

        // ---- 5. 逐层扣减/增加 ----
        float remaining = magnitude;
        Actor target = ctx.target();

        for (PipelineLayer layer : pipeline.layers) {
            if (remaining <= 0) break;

            Stat stat = target.stats.getStat(layer.targetStatTag);
            float effective = remaining * layer.conversionRate;
            float before = stat.getCurrentValue();

            if (pipeline.direction == PipelineDirection.Decrease) {
                float actual = Math.min(effective, before - stat.getMinValue());
                stat.modifyBase(-actual);

                if (actual > 0 && layer.onHitCue != null)
                    CueManager.fire(layer.onHitCue, ctx, actual);
                if (stat.getCurrentValue() <= stat.getMinValue() && layer.onBreakCue != null)
                    CueManager.fire(layer.onBreakCue, ctx, actual);

                remaining = layer.allowOverflow ? (effective - actual) / layer.conversionRate : 0;
            } else {
                float headroom = stat.getMaxValue() - before;
                float actual = Math.min(effective, headroom);
                stat.modifyBase(actual);
                remaining = layer.allowOverflow ? (effective - actual) / layer.conversionRate : 0;
            }
        }

        // ---- 6. 更新 Payload 最终值并广播 Post 事件 ----
        payload = payload.withFinalMagnitude(magnitude);
        ctx.target().eventBus.dispatch(pipeline.postEventTagId, ctx, payload);
    }
}
```

### 7.4 Stateless Executors

```java
class Effects {
    static void execute(Effect cfg, Context ctx, Payload payload) {
        switch (cfg) {
            case Effect.ModifyStat ms -> {
                Actor t = Targets.resolve(ms.target, ctx, payload);
                float v = FloatValues.evaluate(ms.value, ctx, payload);
                t.stats.modify(ms.statTag, ms.op, v);
            }
            case Effect.ApplyPipeline ap -> {
                Actor t = Targets.resolve(ap.target, ctx, payload);
                float mag = FloatValues.evaluate(ap.magnitude, ctx, payload);
                Context targetCtx = ctx.withTarget(t);
                Payload p = Payload.create(ctx, ctx.instigator(), t, mag, ap.tags);
                PipelineExecutor.execute(PipelineRegistry.get(ap.pipelineId), targetCtx, p);
                for (int cue : ap.cuesOnExecute)
                    CueManager.fire(cue, targetCtx, mag);
            }
            case Effect.ApplyStatus as -> {
                Actor t = Targets.resolve(as.target, ctx, payload);
                t.statuses.applyStatus(as.statusId, ctx, as.captures);
            }
            case Effect.ModifyPayloadMagnitude mpm -> {
                if (payload == null) return;
                float v = FloatValues.evaluate(mpm.value, ctx, payload);
                payload.magnitudeChanges().inject(mpm.op, v, mpm.overridePriority);
            }
            case Effect.Sequence seq -> {
                for (Effect e : seq.effects)
                    execute(e, ctx, payload);
            }
            case Effect.Conditional cond -> {
                if (Conditions.test(cond.condition, ctx, payload))
                    execute(cond.then, ctx, payload);
                else if (cond.otherwise != null)
                    execute(cond.otherwise, ctx, payload);
            }
            case Effect.WithTarget wt -> {
                Actor t = Targets.resolve(wt.target, ctx, payload);
                execute(wt.effect, ctx.withTarget(t), payload);
            }
            case Effect.WithTargets wts -> {
                List<Actor> targets = Scans.resolve(wts.scan, ctx, payload);
                for (Actor t : targets)
                    execute(wts.effect, ctx.withTarget(t), payload);
            }
            case Effect.WithLocalVar wlv -> {
                Context inner = ctx.pushLocalScope();
                for (VarBinding b : wlv.bindings)
                    inner.localScope().setFloat(b.varTag,
                        FloatValues.evaluate(b.value, ctx, payload));
                execute(wlv.effect, inner, payload);
            }
            case Effect.Repeat rep -> {
                int count = (int) FloatValues.evaluate(rep.count, ctx, payload);
                for (int i = 0; i < count; i++) {
                    Context loopCtx = ctx.pushLocalScope();
                    if (rep.indexVar != null)
                        loopCtx.localScope().setFloat(rep.indexVar, i);
                    execute(rep.effect, loopCtx, payload);
                }
            }
            case Effect.EffectRef ref -> {
                SharedEffect shared = SharedEffectRegistry.get(ref.refId);
                Context refCtx = ctx.pushLocalScope();
                for (VarBinding b : ref.args)
                    refCtx.localScope().setFloat(b.varTag,
                        FloatValues.evaluate(b.value, ctx, payload));
                execute(shared.effect, refCtx, payload);
            }
            // ... RemoveStatus, GrantTags, SendEvent, FireCue 等
        }
    }
}

class FloatValues {
    static float evaluate(FloatValue fv, Context ctx, Payload payload) {
        return switch (fv) {
            case FloatValue.Const c -> c.value;
            case FloatValue.Math m -> {
                float a = evaluate(m.a, ctx, payload);
                float b = evaluate(m.b, ctx, payload);
                yield switch (m.op) {
                    case Add -> a + b;
                    case Sub -> a - b;
                    case Mul -> a * b;
                    case Div -> b != 0 ? a / b : 0;
                    case Max -> Math.max(a, b);
                    case Min -> Math.min(a, b);
                };
            }
            case FloatValue.StatOf s -> {
                Actor source = Targets.resolve(s.source, ctx, payload);
                Stat stat = source.stats.getStat(s.statTag);
                yield s.capture == Current ? stat.getCurrentValue() : stat.getBaseValue();
            }
            case FloatValue.ContextVar v -> {
                // 分层查找: localScope -> instanceState -> snapshot
                Store local = ctx.localScope();
                if (local != null && local.hasTag(v.varTag)) yield local.getFloat(v.varTag);
                Store inst = ctx.instanceState();
                if (inst != null && inst.hasTag(v.varTag)) yield inst.getFloat(v.varTag);
                ReadOnlyStore snap = ctx.snapshot();
                if (snap != null && snap.hasTag(v.varTag)) yield snap.getFloat(v.varTag);
                yield 0f;
            }
            case FloatValue.PayloadMagnitude pm -> {
                yield payload != null ? payload.magnitude() : 0f;
            }
            case FloatValue.PayloadVar pv -> {
                yield payload != null ? payload.extras().getFloat(pv.varTag) : 0f;
            }
            case FloatValue.CurrentStacks cs -> {
                yield ctx.instanceState() != null
                    ? ctx.instanceState().getFloat(TagRegistry.STACK_COUNT) : 1f;
            }
            case FloatValue.StackScaling ss -> {
                int stacks = (int) evaluate(new FloatValue.CurrentStacks(), ctx, payload);
                yield (ss.baseValue + ss.perStackAdd * stacks) * (float) Math.pow(ss.perStackMul, stacks);
            }
            case FloatValue.ConditionToFloat cf -> {
                yield Conditions.test(cf.condition, ctx, payload) ? 1f : 0f;
            }
        };
    }
}

class Conditions {
    static boolean test(Condition cond, Context ctx, Payload payload) {
        return switch (cond) {
            case Condition.Const c -> c.value;
            case Condition.And a -> a.conditions.stream().allMatch(c -> test(c, ctx, payload));
            case Condition.Or o -> o.conditions.stream().anyMatch(c -> test(c, ctx, payload));
            case Condition.Not n -> !test(n.condition, ctx, payload);
            case Condition.Compare cmp -> {
                float l = FloatValues.evaluate(cmp.left, ctx, payload);
                float r = FloatValues.evaluate(cmp.right, ctx, payload);
                yield switch (cmp.op) {
                    case Gt -> l > r;
                    case Gte -> l >= r;
                    case Lt -> l < r;
                    case Lte -> l <= r;
                    case Eq -> Math.abs(l - r) < 1e-4f;
                    case Neq -> Math.abs(l - r) >= 1e-4f;
                };
            }
            case Condition.HasTags ht -> {
                Actor source = Targets.resolve(ht.source, ctx, payload);
                yield TagQueries.test(ht.query, source.tags);
            }
            case Condition.PayloadHasVar phv -> {
                yield payload != null && payload.extras().hasTag(phv.varTag);
            }
            case Condition.Chance ch -> {
                yield Math.random() < FloatValues.evaluate(ch.probability, ctx, payload);
            }
        };
    }
}

class Targets {
    static Actor resolve(TargetSelector sel, Context ctx, Payload payload) {
        return switch (sel) {
            case TargetSelector.ContextTarget ct -> ctx.target();
            case TargetSelector.ContextInstigator ci -> ctx.instigator();
            case TargetSelector.ContextCauser cc -> ctx.causer();
            case TargetSelector.ContextVar cv ->
                (Actor) ctx.localScope().getObject(cv.varTag); // 分层查找
            case TargetSelector.PayloadInstigator pi ->
                payload != null ? payload.instigator() : ctx.instigator();
            case TargetSelector.PayloadTarget pt ->
                payload != null ? payload.target() : ctx.target();
            case TargetSelector.PayloadVar pv ->
                payload != null ? (Actor) payload.extras().getObject(pv.varTag) : null;
        };
    }
}
```

---

## Part 8: Examples

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
            listenEvent: "Event.Combat.Damage.Take";
            phase: Pre;
            conditions: [];
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
            listenEvent: "Event.Combat.Damage.Take";
            phase: Post;
            conditions: [
                struct Compare {
                    left: struct PayloadMagnitude {};
                    op: Gte;
                    right: struct Const { value: 50.0; };
                }
            ];
            effect: struct WithTarget {
                target: struct PayloadInstigator {};
                body: struct ApplyPipeline {
                    pipelineId: 2;   // PureDamage
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
                pipelineId: 2;
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
                        pipelineId: 2;
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

### 例D：火球术（完整技能）

```
ability {
    id: 1001;
    name: "火球术";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire"];

    activationBlocked: {
        requireAny: ["State.Debuff.Silence", "State.Debuff.Control"];
    };

    costs: [
        { statTag: "Stat.Resource.MP"; value: struct Const { value: 30.0; }; }
    ];

    cooldowns: [
        { cooldownTag: "Cooldown.Ability.Fireball";
          duration: struct Const { value: 6.0; }; }
    ];

    effect: struct Sequence {
        effects: [
            // 主目标伤害
            struct ApplyPipeline {
                pipelineId: 1;   // StandardPhysicalDamage（含闪避/格挡/暴击判定）
                target: struct ContextTarget {};
                magnitude: struct Math {
                    op: Mul;
                    a: struct StatOf {
                        source: struct ContextInstigator {};
                        statTag: "Stat.Combat.MagicAttack";
                        capture: Current;
                    };
                    b: struct Const { value: 2.5; };
                };
                tags: ["Damage.Element.Fire", "Damage.Type.Spell"];
                cuesOnExecute: ["Cue.Combat.Hit.Fireball"];
            },
            // AOE 溅射
            struct WithTargets {
                scan: struct Sphere {
                    center: struct ContextTarget {};
                    radius: struct Const { value: 5.0; };
                    filter: {
                        relationTo: struct ContextInstigator {};
                        allowedRelations: [Hostile];
                        exclude: [struct ContextTarget {}];
                        maxCount: -1;
                        sort: None;
                    };
                };
                effect: struct ApplyPipeline {
                    pipelineId: 1;
                    target: struct ContextTarget {};
                    magnitude: struct Math {
                        op: Mul;
                        a: struct StatOf {
                            source: struct ContextInstigator {};
                            statTag: "Stat.Combat.MagicAttack";
                            capture: Current;
                        };
                        b: struct Const { value: 1.25; };
                    };
                    tags: ["Damage.Element.Fire", "Damage.Type.Spell", "Damage.Type.AOE"];
                };
            },
            // 挂灼烧 DOT
            struct ApplyStatus {
                target: struct ContextTarget {};
                statusId: 2001;
                captures: [];
            }
        ];
    };
}
```

### 例E：暴击增伤被动（利用 Judgment 标记）

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
            listenEvent: "Event.Combat.Damage.Take";
            phase: Post;
            conditions: [
                // 检查 Payload 中是否有暴击标记（由 JudgmentStage 写入）
                struct PayloadHasVar { varTag: "Combat.Result.Critical"; }
            ];
            effect: struct WithTarget {
                target: struct PayloadTarget {};
                body: struct ApplyPipeline {
                    pipelineId: 2;   // PureDamage
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