# 技能系统架构

本文档旨在提供一套基于 `cfggen` 的现代游戏技能系统设计基准。它剥离了早期 ABE 架构中硬编码和高耦合的缺陷，全面吸收了 Unreal **GAS (Gameplay Ability System)** 的核心精髓，致力于打造一套**高内聚、低耦合、全数据驱动**的工业级配置标准。

---

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

本章节定义构成技能系统的"原子"和"词汇"。先定义词汇，再以此造句。

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
    isPersistent: bool;         // 是否需要存盘 (如 Level, MaxHP 存盘；Shield, MoveSpeed 不存盘)
    displayFormat: StatFormat;  // 表现层契约：UI 拿到这个值该怎么显示？

    // --- 2. 边界与极值约束 (Clamping) ---
    // 决定了 getCurrentValue() 时，数值被框定在什么范围内
    minLimit: StatLimit;
    maxLimit: StatLimit;

    // --- 3. 级联联动策略 ---
    // 当 minLimit 或 maxLimit 所依赖的属性发生变化时，自身如何同步？
    clampMode: StatClampMode;
}

// 极值约束器：支持"固定常数"与"动态属性引用"多态
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

    // 定义该事件 Payload 中预期的变量列表 (用于验证和编辑器提示)
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


### FloatValue

FloatValue 提供多态求值能力，将静态配置转化为上下文敏感的运行时指令，替代硬编码参数。

```cfg
interface FloatValue {
    struct Const { value: float; }
    struct Add { a: FloatValue; b: FloatValue; }
    struct Multiply { a: FloatValue; b: FloatValue; }

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
    struct And { conditions: list<Condition>; }

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

能力是行为的入口(主动技能、被动、普攻、跳跃、喝药)。它不负责具体的伤害数值计算，只负责声明"我是谁"以及"我要执行什么指令序列"。

```cfg
table ability[id] (title='name') {
    id: int;
    name: text;
    description: text;

    // 例: ["Ability.Type.Melee", "Ability.Element.Fire"]
    abilityTags: list<str> ->gameplaytag;
    // 如果玩家身上带有 "State.Debuff.Control" (被控)，则直接拦截释放。
    activationBlockedTags: list<str> ->gameplaytag;
    // 释放瞬间，主动打断自身正在进行的其他技能 (如：闪避打断平A)
    cancelAbilitiesWithTag: list<str> ->gameplaytag;

    costEffect: Effect; // 例: 执行一个扣除 50 EP 的 Effect

    // --- 底层解析时自动转化为 CD Status ---
    cooldownDuration: FloatValue;
    cooldownTag: str ->gameplaytag; // 例如: "Cooldown.Ability.Fireball"
    gcdDuration: FloatValue;
    gcdTag: str ->gameplaytag;      // 例如: "Cooldown.Global"

    effect: Effect;
}
```

### Effect

Effect 是瞬间执行、**无状态**的指令流。

```cfg
interface Effect {
    struct EffectRef {
        sharedEffectId: int ->shared_effect; // 逻辑复用
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
        cuesOnHeal: list<str>;
    }

    // --- 状态操作 (标准与内联双轨制) ---
    // A. 引用标准 Status (适用于需要 UI 图标、多端网络同步的常规状态，如"中毒", "护盾")
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
        target: TargetScan;
        effect: Effect;
    }

    // 局部变量绑定
    struct WithLocalVar {
        varTag: str -> gameplaytag;
        value: FloatValue;
        effect: Effect;
    }

    struct Repeat {
        count: FloatValue;
        indexVarTag: str -> gameplaytag (nullable); // 放入localScope里
        effect: Effect;
    }

    struct Sequence { effects: list<Effect>; }
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

// 将高频复用的effect（如"通用受击"、"标准炎爆"、"系统级浮空"）提取到独立的配置表中，配合 `EffectRef` 在各处调用
table shared_effect[id] (title='name') {
    id: int;
    name: text;
    description: text;
    effect: Effect;
}
```

### Status & Behavior

Status 是定义状态逻辑的静态配置，其实例化为 StatusInstance，承载独立的运行时生命周期与堆叠策略。

```cfg
// 用于定义标准、需要网络同步和 UI 表现的长效状态。
// 对于瞬间转瞬即逝的逻辑锁，请使用 Effect 中的  `ApplyStatusInline`或 `GrantTemporaryTags`。
table status[id] (title='name') {
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
```

---

## Runtime Mechanics

本章节定义上述实体在运行时如何交互、数据如何流转。

### Context

Context 是运行时的载体，连接静态配置与动态执行。

```java
record Context(
    Actor instigator,   // 真正的发起者 (如：玩家)
    Actor effectCauser, // 造成效果的物理实体 (如：玩家发射的火球)
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
    float resolve(float baseValue);  // (Base + ΣAdd) * ΠMul
}

record OverrideOp(
    float value,
    int priority
){}
```

### Stat

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

### global_tag_rules

该表定义了标签之间的原子化交互准则。通过将"状态"与"行为"解耦，它充当了战斗系统的"最高物理法则"，统一处理控制、打断与免疫。

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
    rules: [
        // 范例: 只要宿主拥有 Silence 标签，就封印所有 Spell 标签的技能
        { targetTag: "State.Debuff.Silence", 
          blocks: ["Ability.Type.Spell"] },
        // 范例: 只要宿主拥有 Stun 标签，不仅封印所有技能，还打断正在释放的技能
        { targetTag: "State.Debuff.Stun", 
          blocks: ["Ability.Type"], 
          cancels: ["Ability.Type"] }
    ];
}
```

---

## Advanced Patterns

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
                struct PayloadVarGte { varTag: "Data.Damage.Magnitude"; compareValue: 50.0; }
            ];

            effect: struct WithTarget {
                // 将反击目标设定为当时的攻击者
                target: struct TargetFromPayload { role: Instigator; };
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

本章提供核心架构的伪代码实现，展示运行时各组件如何协作。

### Actor

定义游戏实体及其持有的核心组件。
```java
class Actor {
    long entityId;
    StatusComponent statusComponent;
    StatComponent statComponent;
    EventBus eventBus;
    TagContainer tagContainer;
}
```

### TagContainer

TagContainer 是挂载在 Actor 上的核心运行时组件，负责维护实体当前的所有标签状态。为了满足配置表中严格的树状层级需求与高频的查询性能，底层采用 **"引用计数 + 写入时展开"** 策略。

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

### SafeList

抽离生命周期管理中的"防重入、防并发修改"逻辑，封装为泛型安全容器。所有受控实体（状态、行为零件等)必须实现 `IPendingKill`。
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
- `BehaviorInstance` (业务执行层):细粒度的逻辑实体,实现"生命周期管理"与"具体战斗逻辑"的彻底正交。

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

### EventBus

以 EventTagId 为键,直连底层的 SafeList<TriggerInstance> 监听队列。

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

class TriggerInstance extends BehaviorInstance<Behavior.Trigger> implements IPendingKill {
    void onEventFired(Event event);
    boolean pendingKill;
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