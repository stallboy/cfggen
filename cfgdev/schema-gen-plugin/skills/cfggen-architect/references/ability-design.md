# 能力系统设计 (Ability System Design)

本文档旨在提供一套基于 `cfggen` 的现代游戏技能系统设计基准。它剥离了早期 ABE 架构中硬编码和高耦合的缺陷，全面吸收了 Unreal **GAS (Gameplay Ability System)** 的核心精髓，致力于打造一套**高内聚、低耦合、全数据驱动**的工业级配置标准。

## Philosophy

1.  **层级化标签驱动**：
    使用具有严格树状层级关系的 Tag（如 `State.Debuff.Control.Stun`）作为逻辑交互的**唯一通用语言**，统一管理状态互斥、免疫拦截与技能分类。支持父级包含查询与子级精确判定。

2.  **事件管线与载荷修饰**：
    所有的逻辑通信必须通过 `EventBus` 广播带有 `Tag` (路由键) 和 `Payload` (上下文容器) 的标准消息。重点在于**两阶段提交**机制：通过 `Pre` 事件注入 `Modifier` 实现数值篡改（如减伤），通过 `Post` 事件实现副作用触发（如反伤），彻底解耦事件源与监听者。

3.  **逻辑与表现隔离**：
    逻辑层（服务器权威）严禁持有任何表现资源（特效、音效、模型）的引用，仅输出带有生命周期事件的 Cue Tag。客户端表现系统独立维护 Tag 到具体资源的映射，依据事件的生命周期自行调度播放与销毁，实现逻辑计算与视听表现的彻底解耦。

4.  **原子化节点组合**：
    保持 **Ability** (行为入口/需求声明)、**Status** (有状态的持续上下文) 与 **Effect** (无状态的瞬间原子指令) 的严格正交分离。拒绝庞大的继承体系，通过组合构建复杂机制，确保配置的高复用性与低耦合度。

---

## Data Foundation

本章节定义构成技能系统的“原子”和“词汇”。先定义词汇，再以此造句。

### GameplayTag

```cfg
// Tag 字典注册表 (所有使用到的 Tag 必须在此声明)
table gameplaytag[name] {
    name:str; // 例: "State.Debuff.Control.Stun", "Event.Combat.Damage"
    description: text;
    value: int;
    ancestors: list<int> ->gameplaytag[value]; // 策划不用填，程序初始化时自动填
    [value];
}
```

### Stat Definition

```cfg
table stat_definition[statTag] {
    statTag: str ->gameplaytag;
    name: text;                 // 策划备注/UI显示名称

    // --- 1. 基础行为契约 ---
    defaultValue: float;        // 实体初始化时的保底默认值
    isPersistent: bool;         // 是否需要存盘
    displayFormat: StatFormat;  // 表现层契约：UI 拿到这个值该怎么显示？

    // --- 2. 边界与极值约束 (Clamping) ---
    // 决定了 getCurrentValue() 时，数值被框定在什么范围内
    minLimit: StatLimit;
    maxLimit: StatLimit;

    // --- 3. 级联联动策略 ---
    // 当 minLimit 或 maxLimit 所依赖的属性发生变化时，自身如何同步？
    clampMode: StatClampMode;
}

// 极值约束器：支持“固定常数”与“动态属性引用”多态
interface StatLimit {
    struct Const { value: float; }
    // 引用另一个 stat_definition，在运行时去取它的 CurrentValue
    struct StatLink { statTag: str ->stat_definition; }
    struct None {} // 无限制
}

enum StatFormat {
    Flat;       // 绝对数值 (如：1500)
    Percent;    // 百分比 (如：0.15，UI层自动渲染为 15%)
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
    expectedVars: list<EventVarInfo>;
}

struct EventVarInfo {
    varTag: str ->gameplaytag; // 如 "Data.Damage.Element"
    type: VarType;
    name: text;
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

Payload 是事件携带的载荷，Modifier 是对载荷的修饰机制。

```java
record Event(
    int eventTagId,
    Payload payload
){}

record Payload(
    Context sourceContext, // 携带引发该事件的原始技能 Context 引用，用于追溯来源
    Actor instigator,   // Event的绝对发起者 (谁砍的这一刀)
    Actor target,       // Event的绝对承受者 (谁挨的这一刀)

    float magnitude,    // 主值（如伤害量、治疗量）
    Store extras,       // 附加存储，支持 MutatePayloadVar 修改

    ChangeSet magnitudeChanges, // 收集对主值的修改
    Int2ObjectMap<ChangeSet> extraChanges // 收集对副属性的修改
){}

// ChangeSet 瞬时态，聚合数值标量，处理载荷最终结算。
class ChangeSet {
    FloatList additives;
    FloatList multipliers;
    OverrideOp override;
    float resolve(float baseValue);  // 优先override，然后(Base + ΣAdd) * ΠMul
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

TagContainer 是挂载在 Actor 上的核心运行时组件，负责维护实体当前的所有标签状态。为了满足配置表中严格的树状层级需求与高频的查询性能，底层采用 **“引用计数 + 写入时展开”** 策略。

```java
class TagContainer {
    // 核心：TagID -> 引用计数
    Int2IntMap tagCounts;

    // 写入时展开
    // 当添加子级 Tag (如 Stun) 时，会同时递增其所有父级 (Control, Debuff) 的计数。
    // 这使得运行时查询 "State.Debuff" 时，只需简单查询 Map 中是否存在该 Key (O(1))，
    // 而无需遍历检查现有标签的父级关系，极大优化了 GameplayTagQuery 的判定速度。
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

// StatModifierList 支持动态增删，维护长时状态，服务于属性面板计算。
class StatModifierList {
    List<StatModifierInstance> additives;
    List<StatModifierInstance> multipliers;
    StatModifierInstance activeOverride = null; // 选最高优先级的
    int currentOverridePriority = -1;
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

本章节定义在运行时求值的表达式和条件接口。它们的运行时上下文是(`Context`, `Payload`)。

### TargetSelector

TargetSelector 用于动态选取一个目标实体。

```cfg
interface TargetSelector {
    // --- 1. Context 维度 (从当前技能/状态的运行上下文中取) ---
    struct ContextTarget {}     // 技能当前瞄准的准星目标
    struct ContextInstigator {} // 技能的绝对发起者 (如：玩家)
    struct ContextCauser {}     // 造成效果的物理媒介 (如：飞行中的子弹/地上的火墙)
    struct ContextVar {
        actorVarTag: str -> gameplaytag; 
    }

    // --- 2. Payload 维度 (从拦截到的 Event 事件载荷中取) ---
    struct PayloadInstigator {} // 肇事者 (比如引发受击事件的攻击方)
    struct PayloadTarget {}     // 承受者 (比如受击事件中的受害者)
    struct PayloadVar {
        actorVarTag: str -> gameplaytag; 
    }
}
```

### FloatValue

FloatValue 提供多态求值能力，将静态配置转化为上下文敏感的运行时指令，替代硬编码参数。

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
}

enum MathOp {
    Add;
    Subtract; 
    Multiply;
    Divide;
    Max;
    Min;
}

enum StatCaptureMode {
    Current;
    Base;
}
```

### Condition

Condition 提供执行准入标准，与 FloatValue 配合实现动态逻辑判断。

```cfg
interface Condition {
    struct Const { value: bool; }
    struct Math { op: BoolOp; conditions: list<Condition>; }
    struct Not { condition: Condition; }
    
    struct HasTags {
        source: TargetSelector;
        requirements: GameplayTagQuery;
    }

    struct PayloadMagnitudeGte {
        compareValue: float;
    }
    struct PayloadVarGte {
        varTag: str ->gameplaytag;
        compareValue: float;
    }
}

enum BoolOp {
    And;
    Or;
}

struct GameplayTagQuery {
    requireTagsAll: list<str> ->gameplaytag; // 必须全部包含
    requireTagsAny: list<str> ->gameplaytag; // 包含其中之一即可生效 (最常用)
    ignoreTags: list<str> ->gameplaytag;     // 包含任何一个则拦截/失效
}
```

---

## Core Entities

本章节定义系统的主体部分：Ability 是入口，Effect 是动作，Status 是持续状态，逻辑递进。

### Ability

能力是行为的入口(主动技能、被动、普攻、跳跃、喝药)。它不负责具体的伤害数值计算，只负责声明“我是谁”以及“我要执行什么指令序列”。

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    description: text;

    // 例: ["Ability.Type.Melee", "Ability.Element.Fire"]
    abilityTags: list<str> ->gameplaytag;
    // 如果玩家身上带有 "State.Debuff.Control" (被控)，则直接拦截释放。
    activationBlockedTags: list<str> ->gameplaytag;
    // 必须拥有某 Tag 才能释放，如变身形态技能
    activationRequiredTags: list<str> ->gameplaytag;
    // 释放瞬间，主动打断自身正在进行的其他技能 (如：闪避打断平A)
    cancelAbilitiesWithTag: list<str> ->gameplaytag;

    costs: list<StatCost>;
    selfCooldown: float;
    cooldowns: list<TagCooldown>
    commitEffects: Effect; // 额外的代价

    effect: Effect;
}

// 资源消耗声明
struct StatCost {
    statTag: str ->stat_definition; // 如 "Stat.Resource.EP"
    value: FloatValue;              // 消耗量 (支持静态值或基于最大生命的百分比等)
}

// 冷却声明
struct TagCooldown {
    // 冷却状态的标识 (宿主身上带有此 Tag 则代表在 CD 中, 实现：挂载了一个隐藏的 StatusInstance)
    cooldownTag: str ->gameplaytag; // 如 "State.Cooldown.Skill_Fireball"
    duration: FloatValue;           // 如 15.0 (支持被装备减CD属性修饰)
}
```

### Effect

`Effect` 是瞬间执行、**无状态**的指令流。其运行时上下文是`Context`

```cfg
interface Effect {
    struct EffectRef {
        sharedEffectId: int ->shared_effect; // 逻辑复用
    }
    
    // --- 属性修改 ---
    struct ModifyStat { // 改 baseValue
        statTag: str ->stat_definition; // 如 "Stat.Resource.Stamina"
        op: ModifierOp;
        value: FloatValue;
    }

    // 伤害。执行流水线: Snapshot属性 -> 广播 Event.*.Pre (触发 ModifyPayload)
    // -> 计算最终数值 -> 扣血 -> 广播 Event.*.Post (触发反伤/吸血)
    struct Damage {
        damageTags: list<str> ->gameplaytag; // 如: ["Damage.Element.Fire", "Damage.AttackType.Melee"]
        baseDamage: FloatValue;
        cuesOnHit: list<str> ->gameplaytag;
    }

    // 治疗
    struct Heal {
        baseHeal: FloatValue;
        cuesOnHeal: list<str> ->gameplaytag;
    }

    // --- 状态操作 ---
    // A. 引用标准 Status (适用于需要 UI 图标、多端网络同步的常规状态，如“中毒”, “护盾”)
    struct ApplyStatus {
        statusId: int ->status;
        captures: list<ArgCapture>;
    }

    // B. 内联型 Status (拥有完整功力，适用于无需 UI 显示的一次性专属机制)
    struct ApplyStatusInline {
        core: StatusCore;
        captures: list<ArgCapture>;
    }

    // C. 快捷方式：极简内联微状态 (适用于冲锋时的零点几秒霸体等纯逻辑阻断状态)
    struct GrantTemporaryTags {
        duration: FloatValue;
        grantedTags: list<str> ->gameplaytag;
    }

    // 发送事件
    struct SendEvent {
        eventTag: str ->event_definition; // 强引用定义表
        magnitude: FloatValue;
        extras: list<struct { tag: str ->gameplaytag; value: FloatValue; }>;
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

    // --- 事件载荷前置篡改 ---
    // 向 Payload 的 ModifierStack 中注入修饰器，实现免伤、暴击抵抗等机制
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

    // --- 作用域 & 控制流 ---
    // 目标重定向
    struct WithTarget {
        target: TargetSelector;
        effect: Effect;
    }

    // 群体作用域
    struct WithTargets {
        targets: TargetScan;
        effect: Effect;
    }

    // 局部变量绑定
    struct WithLocalVar {
        varTag: str -> gameplaytag;
        value: FloatValue;
        effect: Effect;
    }

    struct Repeat {
        count: FloatValue; // 取下界
        indexVarTag: str -> gameplaytag (nullable); // 放入localScope里
        effect: Effect;
    }

    struct Conditional {
        condition: Condition;
        effect: Effect;
        elseEffect: list<Effect>;
    }

    struct Sequence { effects: list<Effect>; }
}

// 将高频复用的effect（如“通用受击”、“标准炎爆”、“系统级浮空”）提取到独立的配置表中，配合 `EffectRef` 在各处调用
table shared_effect[id] (json) {
    id: int;
    name: text;
    description: text;
    effect: Effect;
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

enum ModifierOp {
    Add;
    Multiply;
    Override;
}

interface TargetScan {
    // 圆形/球体 (如：以火球 ContextCauser 为中心爆炸)
    struct Sphere {
        center: TargetSelector; // 圆心是谁？(极大提升灵活性，比如以目标为圆心)
        radius: FloatValue;
        filter: TargetFilter;
    }
    // 扇形/锥形 (如：战士面向前方 90 度劈砍)
    struct Sector {
        center: TargetSelector; 
        forwardDir: TargetSelector; // 决定朝向的实体 (通常是 ContextInstigator 面向)
        radius: FloatValue;
        angle: FloatValue;          // 角度，如 90.0, 120.0
        filter: TargetFilter;
    }
    // 矩形/Box (如：激光束、剑气、冲锋路径上的敌人)
    struct Box {
        center: TargetSelector;
        forwardDir: TargetSelector;
        width: FloatValue;
        length: FloatValue;
        filter: TargetFilter;
    }
    
    // 全队扫描 (如：奶妈开大招，给全队加血)
    struct PartyMembers {
        source: TargetSelector; // 取谁的队伍？
        filter: TargetFilter;
    }
}

struct TargetFilter {
    // 1. 阵营过滤
    // 参照物是谁？(通常是 ContextInstigator，即判定“扫描到的目标和施法者是什么关系”)
    relationSource: TargetSelector; 
    allowedRelations: list<TeamRelation>;

    // 2. 状态/标签过滤 (复用你文档里的 GameplayTagQuery)
    // 如：必须包含 "State.Alive" 且不包含 "State.Invincible"
    tagQuery: GameplayTagQuery;

    // 3. 动态黑名单 (极大提升机制扩展性)
    // 传入一组单体 TargetSelector，如果扫出来的人在这里面，直接剔除。
    // 应用场景：防重复弹射、或者避免 AOE 炸到主目标(主目标在 WithTarget 里单独算伤害)。
    excludeTargets: list<TargetSelector>; 

    // 4. 目标上限与优选策略
    maxCount: int;          // 最大生效人数 (如 -1 代表无限制，3 代表最多劈 3 个)
    sortType: SortTarget;   // 当超出上限时，怎么挑人？
}

enum TeamRelation {
    Self;       // 自身 (非常重要，有时要防误伤自己)
    Friendly;   // 友方
    Hostile;    // 敌方
    Neutral;    // 中立 (如：场景木桶、野怪)
}

enum SortTarget {
    DistanceAsc;    // 距离由近到远 (配合空间扫描使用)
    HpPercentAsc;   // 优先打血量百分比最低的 (斩杀机制优选)
    Random;         // 随机选 (比如闪电链随机乱劈)
}
```

### Status & Behavior

Status 是定义状态逻辑的静态配置，其实例化为 StatusInstance，承载独立的运行时生命周期与堆叠策略。`Behavior` 的运行时上下文是`StatusInstance` (内含`Context`)。

```cfg
// 用于定义标准、需要网络同步和 UI 表现的长效状态。
// 对于瞬间转瞬即逝的逻辑锁，请使用 Effect 中的  `ApplyStatusInline`或 `GrantTemporaryTags`。
table status[id] (json) {
    id: int;
    name: text;
    description: text;
    stackingPolicy: StackingPolicy;
    core: StatusCore;
}

struct StatusCore {
    duration:FloatValue; // 持续时间，-1 为永久 (被动技能)
    // 存在期间给宿主挂载的 Tag (如: 挂载 "State.Buff.Invincible" 实现无敌)
    grantedTags: list<str> ->gameplaytag;
    // 存在期间，免疫/阻挡试图挂载的新 Tag (如: 挂载 "State.Debuff" 实现霸体免控)
    blockTags: list<str> ->gameplaytag;
    // 挂载瞬间，立刻驱散宿主身上已有的 Tag (如: 驱散 "State.Debuff" 实现净化)
    cancelTags: list<str> ->gameplaytag;

    cuesWhileActive: list<str> ->gameplaytag; // 表现层解耦
    behaviors: list<Behavior>;
}

interface Behavior {
    // 基于 Tag 的动态属性修饰，向Stat 运算管线中注入修饰器
    struct StatModifier {
        targetTagQuery: GameplayTagQuery; // 满足此 Tag 环境才生效 (如：对流血敌人特攻)
        statTag: str ->stat_definition; // 例: "Stat.Combat.Attack", "Stat.CDR.Fire"
        op: ModifierOp;
        value: FloatValue;
    }

    // 周期性触发 (DOT/HOT)
    struct Periodic {
        period: FloatValue;
        executeImmediately: bool;
        effect: Effect;
    }

    // 事件触发器，监听全局 EventBus 上的特定事件并作出反应
    struct Trigger {
        // 如 "Event.Combat.Damage.Take" 或 "Event.Character.Dodge"
        listenEventTag: str ->event_definition;
        conditions: list<Condition>; // 前置条件 (通常提取 Payload 数据进行合法性校验)
        effect: Effect; // 条件满足后执行的瞬间动作

        // 自我限制与终末遗言 (完全属于 Behavior 内部逻辑)
        maxTriggers: int;
        onLimitReached: Effect;
    }
}

interface StackingPolicy {
    // 传统叠加 (Standard)：层数增加，且所有层共享最长的那个时间。
    struct Standard {
        maxStacks: int;
    }
    // 覆盖刷新：重置倒计时。
    struct Override {}
    // 时长累加：增加持续时间。
    struct AccumulateDuration { 
        maxDurationLimit: float; // 例如最多累加到 30 秒
    }
    // 独立计时：每层独立衰减。
    struct Independent {
        maxStacks: int;
    }
    // 拒绝叠加：新状态直接被免疫/丢弃 (如：不可刷新的特殊机制锁)
    struct Reject {}
}
```

---

## Global Rules

本章节定义全局性的战斗规则，这些规则以配置表的形式存在，由底层引擎直接读取，作为战斗系统的“最高物理法则”。

### global_tag_rules

该表定义了标签之间的原子化交互准则。通过将“状态”与“行为”解耦，它充当了战斗系统的“最高物理法则”，统一处理控制、打断与免疫。

```cfg
table global_tag_rules[name] (entry='name'){
    name: str;
    rules: list<TagRelationshipRule>;
}

struct TagRelationshipRule {
    // 触发源标签 (通常是宿主身上的状态标签，如 State.Debuff.Silence)
    sourceTag: str ->gameplaytag;

    // --- 激活拦截 (Activation Blocking) ---
    // 当宿主拥有 targetTag 时，禁止激活带有以下任意标签的 Ability
    blocks: list<str> ->gameplaytag;

    // --- 强制中断 (Ongoing Cancellation) ---
    // 当宿主获得 targetTag 的瞬间，立即打断正在运行且带有以下任意标签的 Ability
    cancels: list<str> ->gameplaytag;

    // --- 标签免疫 (Tag Immunity) ---
    // 当宿主拥有 targetTag 时，任何试图挂载到宿主身上的新 Status，
    // 如果其 GrantedTags 包含以下标签，则直接挂载失败
    immunes: list<str> ->gameplaytag;

    comment: text;
}
```

例子：
```
table global_tag_rules {
    name: "defaultRules"
    rules: [
        // 范例: 只要宿主拥有 Silence 标签，就封印所有 Spell 标签的技能
        { sourceTag: "State.Debuff.Silence", 
          blocks: ["Ability.Type.Spell"] },
        // 范例: 只要宿主拥有 Stun 标签，不仅封印所有技能，还打断正在释放的技能
        { sourceTag: "State.Debuff.Stun", 
          blocks: ["Ability.Type"], 
          cancels: ["Ability.Type"] }
    ];
}
```

### global_combat_settings

定义了全局统一的伤害与治疗（Damage & Heal）结算管线，负责将最终数值按规则逐层作用于目标的具体属性（如护盾、生命），并自动联动相关的视听表现与死亡判定。

```
table global_combat_settings[name] (entry="name") {
    name: str;
    damageLayers: list<DamageLayer>; //伤害扣减管线, 依次扣减

    healthStatTag: str ->stat_definition; // 例: "Stat.HP.Current"
    
    // 当 healthStat 归零时，引擎自动向宿主派发的 Tag (用于触发死亡表现或复活逻辑)
    deathTag: str ->gameplaytag; // 例: "State.Dead" 或 "State.Downed"
}

struct DamageLayer {
    targetStat: str ->stat_definition; 
    conversionRate: float; // 1点伤害扣除多少点该属性。如魔法盾可能配置为 1.5
    
    // 允许溢出标志
    // true: 该层属性扣到 0 后，未被抵消的剩余伤害继续向下击穿。
    // false: 伤害结界（锁血/锁盾），单次伤害最多只能打破该层，多余伤害直接作废。
    allowOverflow: bool;

    // 受击表现：当这一层被扣减时，触发什么类型的受击反馈？
    // 例: "Cue.Combat.Hit.Shield", "Cue.Combat.Hit.Armor", "Cue.Combat.Hit.Flesh"
    hitCueTag: str ->gameplaytag (nullable);
    
    // 破盾表现：当这一层的数值刚好被本次伤害清零时，额外触发什么反馈？
    // 例: "Cue.Combat.Break.Armor"
    breakCueTag: str ->gameplaytag (nullable);
}
```

---

## Examples

本章节展示如何使用 Trigger 实现复杂机制，是全系统协同工作的演示。

### Trigger

Trigger 是彻底消灭硬编码（如写死在代码里的 `OnHit`, `OnTakeDamage`）的终极武器。
任何有意义的动作发生后，引擎底层必须向全局 EventBus 广播一个带有 Tag 和 Payload 的事件。监听和响应这些事件的职责全部收口在 `Trigger` 节点中。

### 例A：盾墙减伤

* **底层机制**：监听 `Event.*.Pre`，向 Payload 的主轨注入一个 `Multiply(0.6)` 的 Modifier。由于是延期求值，绝对不会与其他加法/减法 Buff 产生冲突。

```
table status {
    id: 5001; name: "盾墙"; duration: 5.0;
    core: struct StatusCore {
        behavior: struct Trigger {
            listenEventTag: "Event.Combat.Damage.Take.Pre"; // 拦截阶段
            effect: struct ModifyPayloadMagnitude {
                op: Multiply;
                value: struct Const { value: 0.6; };
            };
        };
    };
}
```

### 例B：反伤装甲

* **底层机制**：监听 `Event.*.Post`，如果伤害 >=50，抓取 Payload 中的发起者执行反击。

```
table status {
    id: 4001;
    name: "反刺被动";

    core: struct StatusCore {
        duration: -1.0;
        behavior: struct Trigger {
            listenEventTag: "Event.Combat.Damage.Take.Post"; // 结算完毕阶段

            conditions: [
                struct PayloadMagnitudeGte { compareValue: 50.0; }
            ];

            effect: struct WithTarget {
                // 将反击目标设定为当时的攻击者
                target: struct PayloadInstigator {};
                effect: struct Damage {
                    baseDamage: struct Const { value: 10.0; };
                };
            };
        };
    };
}
```

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
    void setPendingKill(boolean pending);
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
}
```

### Stateless Executors

`Effect`、`FloatValue`、`Condition` 作为**无状态**指令节点,本身不维护生命周期,被调用时即时消费上下文结算。

```java
class Effects {
    static void execute(Effect cfg, Context ctx, Event evt);
}

class FloatValues {
    static float evaluate(FloatValue cfg, Context ctx, Event evt);
}

class Conditions {
    static boolean test(Condition cfg, Context ctx, Event evt);
}
```

---

## Gameplay Cue

客户端维护一张纯表现的注册表 `client_cue_registry`，组装视听反馈。

### Cue Schema

```cfg
// 表现层映射清单 (纯客户端使用，服务器不加载)
table client_cue_registry[cueTag] (entry="cueTag") {
    cueTag: str ->gameplaytag; // 强引用，例: "Cue.Combat.Hit.Heavy"
    handler: CueHandler;
}

interface CueHandler {
    // A. 瞬发型 (Instant): 触发即播放，客户端不维护状态，由系统池自动回收。
    // 适用于：受击、爆炸、挥砍瞬间、破盾
    struct Instant {
        vfx: list<VfxConfig>;                   // 特效集合 (例：爆血 + 刀光碎片)
        sfxEvents: list<SfxConfig>;             // 音效事件集合 (支持 Wwise/FMOD 参数化化音频)
        animTriggers: list<AnimTriggerConfig>;  // 动画触发器集合
        cameraShakes: list<str>;                // 摄像机震动配置集合 (全局指令，无需分 Role)
        damageTexts: list<DamageTextConfig>;    // 飘字设定集合
    }

    // B. 持续型 (Sustained): 随状态挂载而生成，随状态移除而销毁。
    // 客户端需将其登记入 Active 字典，确保状态结束时安全销毁/淡出。
    // 适用于：中毒冒泡、无敌金身材质、残血屏幕发红
    struct Sustained {
        loopingVfx: list<VfxConfig>;                     // 循环特效集合
        loopingSfxEvents: list<SfxConfig>;               // 循环音效集合
        materialOverrides: list<MaterialOverrideConfig>; // 材质覆写集合
        screenFilters: list<str>;                        // 屏幕空间后处理集合
    }
}

// --- 基础表现组件 ---

struct VfxConfig {
    role: CueRole;
    assetPath: str;             // 特效预制体路径
    attachRule: VfxAttachRule;
    attachSocket: str;          // 绑定的骨骼挂点（如 "Chest", "Root"）
    scale: float;
}

enum VfxAttachRule {
    KeepWorldPosition; // 瞬发原地留存（如：地上的爆炸坑，不随目标移动）
    SnapToTarget;      // 绑定并跟随目标移动（如：身上的燃烧火苗）
}

struct SfxConfig {
    role: CueRole;     // 决定声音的 3D 发声源在谁身上
    eventName: str;    // 音效事件路径 (如 "event:/Combat/Hit_Heavy")
}

struct AnimTriggerConfig {
    role: CueRole;     
    triggerName: str;  // 触发目标动画图的节点 (如 "HitReact_Heavy")
}

struct DamageTextConfig {
    role: CueRole;
    colorCode: str;    // 颜色代码 (如 "#FF0000")
    fontSize: int;
    prefixIcon: str;   // 前缀小图标 (如 "Icon_Crit")
    motionStyle: str;  // 运动轨迹枚举 (如 "PopUp", "Gravity")
}

struct MaterialOverrideConfig {
    role: CueRole;
    materialPath: str;    // 材质资产路径
    targetSlotIndex: int; // 替换第几个材质槽位 (-1 代表替换全部)
}

enum CueRole {
    Target;       // 默认值：表现作用于受击者/状态承受者
    Instigator;   // 表现作用于攻击方/技能发起者
}
```

### Runtime

在运行时，逻辑层不需要策划手动配置 Cue 的生命周期类型，底层引擎将根据 Cue 所在的**上下文位置**自动推导并广播网络事件：

* **挂载在 `Effect` 节点中（或 `DamageLayer` 结算时）**：
    * 语义：瞬间行为。
    * 推导：底层触发 `Executed` 事件。客户端查表调用 `Instant` 处理器，对象池“阅后即焚”。


* **挂载在 `Status` 的 `cuesWhileActive` 中**：
    * 语义：状态绑定行为。
    * 推导：底层在状态添加时触发 `Added` 事件，在状态销毁时触发 `Removed` 事件。客户端查表调用 `Sustained` 处理器，生成特效后存入 `activeSustainedCues` 字典，收到移除指令时清理。

```java
record GameplayCueEvent (
    CueEventType eventType, // 自动推导: Executed / Added / Removed
    int cueTagId,           // "Cue.Combat.Hit.AbyssalStrike" 对应的整型ID
    Actor target,           // 作用目标
    Actor instigator,       // 发起者 (用于 Instigator Role 的表现回溯)
    Actor causer,
    float magnitude         // 表现强度 (传递给 UI 飘字数值或音频引擎的 RTPC 参数)
){}
```

### Example

**“深渊重击”**（攻击造成目标流血，同时自身获得吸血特效与重击硬直后摇）。

```cfg
table client_cue_registry {
    [1001] {
        cueTag: "Cue.Combat.Hit.AbyssalStrike";
        handler: struct Instant {
            
            // 动画：目标播放受击硬直，攻击方(Instigator)播放武器弹刀/重击后摇
            animTriggers: [
                { role: Target, triggerName: "Hit_Heavy_Stagger" },
                { role: Instigator, triggerName: "Attack_Recoil_Heavy" }
            ];

            // 特效：目标身上爆黑血，攻击方身上爆出绿色恢复流光
            vfx: [
                { role: Target, assetPath: "VFX_DarkBlood", attachRule: SnapToTarget, attachSocket: "Chest", scale: 1.0 },
                { role: Instigator, assetPath: "VFX_Heal_Burst", attachRule: SnapToTarget, attachSocket: "Root", scale: 1.0 }
            ];

            // 飘字：目标头上爆红字，攻击方头上爆绿色回血数字
            damageTexts: [
                { role: Target, colorCode: "#FF0000", fontSize: 40, prefixIcon: "Sword", motionStyle: "PopUp" },
                { role: Instigator, colorCode: "#00FF00", fontSize: 24, prefixIcon: "Cross", motionStyle: "FloatUp" }
            ];

            // 音效：发声源在受击目标身上
            sfxEvents: [
                { role: Target, eventName: "event:/SFX/Combat/Abyssal_Hit" }
            ];
            
            cameraShakes: ["CamShake_HeavyImpact"];
        }
    }
}
```