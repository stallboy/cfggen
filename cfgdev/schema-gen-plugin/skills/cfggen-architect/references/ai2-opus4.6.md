

# 评审意见

## 一、总体评价

文档展现了对现代 AI 架构的深刻理解，Sense-Think-Act 三段式分治、扁平化评分替代行为树、与 GAS 的协同设计都是正确方向。但作为"可裁剪的架构基准"，存在以下系统性问题：

---

## 二、结构性问题

### 1. 感知层（Sense）过于单薄，与 Think/Act 层不对称

**问题**：GoalGenerator 只有 `SpatialScan` 和 `OnEvent` 两种，且缺少核心感知概念——**记忆衰减、威胁评估、感知通道（视觉/听觉/受击）**。实际游戏中"我看到了什么"和"我听到了什么"的处理差异极大，但当前设计将其压扁为同一个 SpatialScan。

**建议**：引入 `SenseChannel`（感知通道）概念，不同通道有不同的更新频率、范围参数和衰减策略。GoalGenerator 绑定到具体的 SenseChannel 上。

### 2. GoalGenerator 与 GoalDefinition 的关系混乱

**问题**：`GoalGenerator` 生成 `GoalInstance`，但 `GoalDefinition` 定义了 `validConditions`。一个 GoalDefinition 可以被多个 Generator 引用，但 Generator 的 `generatedGoal` 是写死的单一引用。这导致：
- 如果我想让"视觉发现敌人"和"听到枪声定位敌人"都生成同一种 `Goal.Combat.Enemy`，需要两个 Generator 各引用一次——没问题
- 但如果我想让一个 Generator 根据条件生成不同类型的 Goal——做不到

**建议**：Goal 的类型判定应由 Tag 驱动而非表 ID 硬绑定。Generator 产出的 GoalInstance 携带 Tag 集合，Behavior 的 `requiredGoals` 改为 TagQuery 匹配。

### 3. Think 层的表达式体系与 GAS 完全割裂

**问题**：AI 定义了独立的 `ScoreValue`、`PreCondition`、`AICondition`、`AIFloatValue`、`ThinkActor`、`ThinkLocation`——这些与 GAS 的 `FloatValue`、`Condition`、`TargetSelector` 功能高度重叠但类型完全不兼容。策划需要学两套"语言"，且无法在 AI 条件中直接复用 GAS 表达式。

**建议**：AI 层的表达式应建立在 GAS 的 `FloatValue`/`Condition` 之上，通过扩展（而非重建）的方式添加 AI 特有的求值节点（如 `Distance`、`GoalAge`、`IsActiveBehavior`）。

### 4. Act 层的 AITask 与 GAS 的 Effect 职责边界模糊

**问题**：
- `AITask.DoEffect` 执行 GAS 的 Effect——但 Effect 是瞬间无状态的，而 AITask 有 Running/Success/Failed 生命周期
- `AITask.CastAbility` 触发 Ability——但 Ability 内部的 Effect 可能包含 `ApplyStatus`，而 Status 的 `Behavior.Trigger` 又可能通过 `AIBehaviorModifier` 反向影响 AI 决策
- `AITask.PlayAnimation` / `AITask.PlayCue` 直接操作表现层——违反了 GAS 文档中"逻辑与表现隔离"的核心原则

**建议**：
- `DoEffect` 应明确为"瞬间执行，不等待"，与 `CastAbility`（可等待完成）严格区分
- 删除 `PlayAnimation` 和 `PlayCue`——动画和特效应由 Ability/Status 的 Cue 系统驱动，AI 不应直接触碰表现层
- 如果 AI 需要"播放嘲讽动画"这种非战斗表现，应通过 `CastAbility` 触发一个"嘲讽"Ability，由 Ability 内部挂 Cue

### 5. AbortCondition 是 Condition 的子集却单独定义

**问题**：`AbortCondition` 只有三个硬编码变体（`TargetIsDead`、`DistanceGreaterThan`、`SelfHasAnyTags`），无法组合、无法复用 PreCondition 的逻辑。实际游戏中中止条件可能很复杂（"目标死亡 OR (距离>15 AND 自身血量<20%)"）。

**建议**：AbortCondition 直接复用 Condition 体系，不单独定义。

### 6. 行为修饰（BehaviorModifier）的注入粒度过粗

**问题**：`BehaviorInjection` 将行为注入到 `ai_behavior_group`——但 Group 在文档中被定义为"仅用于共享条件的扁平化工具"，底层会被压平。向一个"不存在于运行时"的概念注入行为，语义矛盾。

**建议**：修饰器直接操作行为池本身（启用/禁用特定 behaviorTag 的行为，或直接注入行为列表），不经过 Group 中转。

### 7. 决策流程中缺少"无行为可选"的处理

**问题**：文档定义了 `defaultBehavior` 作为兜底，但决策流程的五个阶段中没有明确说明何时回退到 defaultBehavior。如果所有候选行为都在冷却中，或所有 preConditions 都不满足，系统行为未定义。

### 8. AIBehaviorInstance 的 boundGoals 设计有问题

**问题**：`boundGoals` 是 `Int2ObjectMap<AIGoalInstance>`，Key 是 GoalDefinition 的 ID。但一个行为可能需要同类型的多个 Goal（如"选最近的两个掩体"），当前结构只能绑定每种类型的一个 Goal。

---

## 三、细节问题

1. `SenseTargetScan` 和 `SenseCondition` 被引用但从未定义
2. `AITargetSelector` 被 AITask 大量使用但从未定义，且与 GAS 的 `TargetSelector` 关系不明
3. `ai_archetype` 的 `goalGenerators` 引用类型是 `list<str> -> ai_goal_generator`，但 `ai_goal_generator` 的主键是 `name: str`，应为一致的引用方式
4. `scoreInertia` 在决策流程中被多次提及但从未在任何配置表中定义
5. `global_ai_settings` 的 `abortConditions` 使用了 `AbortCondition` 类型，但全局中止的语义（检查 Actor Tag）与行为级中止（检查目标状态）不同，不应混用同一类型
6. `Speak` 和 `Interact` 作为 AITask 直接嵌入——这些是高度游戏特定的，不适合放在"基准模板"中
7. `ai_behavior` 同时有 `[behaviorId]` 自增和 `behaviorId: int` 手动字段，冲突

---

# AI 行为系统设计 (AI Behavior System Design)

本文档定义了一套与**能力系统设计**无缝集成的 AI 行为系统。以 **Sense-Think-Act** 三段式分治为骨架，通过**扁平化评分机制**替代行为树的僵化结构，与 GAS 共享表达式体系，实现高内聚、低耦合的数据驱动 AI 管线。

---

## Philosophy

1. **三段式分治 (Sense → Think → Act)**
   - **感知**（我能看到什么？）：通道化传感器产出标准化 Goal
   - **决策**（我该做什么？）：扁平化评分从候选池选出最优行为
   - **执行**（我怎么做？）：任务栈驱动动作序列，管理生命周期与打断

2. **与 GAS 共享表达式层**
   AI 的条件判断与数值求值**直接复用** GAS 的 `Condition` 和 `FloatValue` 体系，并通过扩展节点补充 AI 特有的求值能力（距离、Goal 属性、行为惯性等）。策划只需学一套语言。

3. **数据驱动与动态组合**
   所有行为通过配置组合构建，运行时通过行为修饰器动态增删决策池，零耦合应对 Boss 转阶段等特殊情境。

4. **双轨打断协同**
   - **外部宏观掐断**：GAS 的 Tag 系统驱动全局/局部中止
   - **内部主动冒泡**：Task 执行失败正常出栈，触发冷却后平滑重选

5. **逻辑与表现隔离**
   AI 层严禁直接操作动画/特效/音效。所有表现需求通过 `CastAbility` 触发 Ability，由 Ability 内部的 Cue 系统驱动。

---

## Part 1: Expression Layer Extensions

AI 系统**直接复用** GAS 的 `FloatValue`、`Condition`、`TargetSelector` 体系。本章仅定义 AI 特有的扩展节点。

### 1.1 FloatValue 扩展

以下节点追加到 GAS 的 `FloatValue` 接口中：

```cfg
// 追加到 ability-design 的 FloatValue interface
interface FloatValue {
    // ... (保留 GAS 原有的所有节点) ...

    // === AI 扩展 ===

    // 两个 Actor 之间的距离
    struct Distance {
        from: TargetSelector;
        to: TargetSelector;
    }

    // Goal 实例的存活时间（秒）
    struct GoalAge {
        goalTag: str ->gameplaytag;
    }

    // Goal 实例携带的强度值
    struct GoalMagnitude {
        goalTag: str ->gameplaytag;
    }

    // 当前行为惯性加分：如果正在执行此行为返回 trueValue，否则返回 falseValue
    struct BehaviorInertia {
        trueValue: float;
        falseValue: float;
    }

    // 到指定位置的寻路代价（由导航系统求值）
    struct PathCost {
        from: TargetSelector;
        to: TargetSelector;
    }
}
```

### 1.2 TargetSelector 扩展

```cfg
// 追加到 ability-design 的 TargetSelector interface
interface TargetSelector {
    // ... (保留 GAS 原有的所有节点) ...

    // === AI 扩展 ===

    // 当前 AI 大脑的战斗目标
    struct AICombatTarget {}

    // 当前行为绑定的 Goal 关联的 Actor
    struct GoalActor {
        goalTag: str ->gameplaytag;
    }

    // 当前行为绑定的 Goal 的位置（作为虚拟 Actor 处理）
    struct GoalLocation {
        goalTag: str ->gameplaytag;
    }
}
```

### 1.3 Condition 扩展

```cfg
// 追加到 ability-design 的 Condition interface
interface Condition {
    // ... (保留 GAS 原有的所有节点) ...

    // === AI 扩展 ===

    // 大脑中是否存在匹配指定 Tag 的活跃 Goal
    struct HasGoal {
        goalTag: str ->gameplaytag;
    }

    // 检查指定 Ability 是否可激活（冷却/资源/Tag 准入）
    struct CanActivateAbility {
        abilityId: int ->ability;
    }

    // 当前是否正在执行指定行为
    struct IsExecutingBehavior {
        behaviorId: int ->ai_behavior;
    }

    // 目标是否存活（非 pendingKill 且不带死亡标签）
    struct TargetIsAlive {
        target: TargetSelector;
    }

    // 对目标是否有视线
    struct HasLineOfSight {
        from: TargetSelector;
        to: TargetSelector;
    }
}
```

**设计说明**：通过扩展而非重建，AI 策划可以自由组合 GAS 的 `Compare`、`HasTags`、`And`/`Or`/`Not` 与 AI 特有的 `HasGoal`、`CanActivateAbility` 等节点。例如：

```
// "血量低于30% 且 治疗技能可用 且 大脑中有安全掩体目标"
struct And {
    conditions: [
        struct Compare {
            left: struct StatOf { source: struct ContextTarget {}; statTag: "Stat.HP.Percent"; capture: Current; };
            op: Lt;
            right: struct Const { value: 0.3; };
        },
        struct CanActivateAbility { abilityId: 2001; },
        struct HasGoal { goalTag: "Goal.Cover"; }
    ];
}
```

---

## Part 2: Runtime Core

### 2.1 AIBrainComponent

AI 大脑，挂载在 Actor 上，驱动感知→决策→执行的完整循环。

```java
class AIBrainComponent {
    Actor self;
    AiArchetype archetypeCfg;

    // --- 感知认知库 ---
    // Key: goalTag 的 enumId, Value: 该类型下的所有活跃 Goal 实例
    Int2ObjectMap<List<AIGoalInstance>> activeGoals;
    Actor combatTarget;            // 当前锁定的战斗目标

    // --- 决策状态 ---
    AIBehaviorInstance activeBehavior;
    float currentBehaviorElapsed;  // 当前行为已执行时间

    // --- 动态修饰 ---
    List<AIBehaviorModifierRef> behaviorModifiers;

    // --- 冷却追踪 ---
    Int2FloatMap behaviorCooldowns; // behaviorId -> 剩余冷却时间

    // --- GAS 桥接 ---
    // 构建供表达式求值的 Context
    // target = self, instigator = self
    Context buildEvalContext();
}
```

### 2.2 AIGoalInstance

感知阶段的产物——AI 当前感知到的潜在交互目标。

```java
class AIGoalInstance {
    // --- 标签化类型标识 ---
    int goalTagId;           // 如 "Goal.Combat.Enemy", "Goal.Cover.Flank"
    
    // --- 关联数据 ---
    Actor associatedActor;   // 关联实体（可为 null，如"可疑声源"只有位置）
    Vector3 position;        // 目标位置
    float magnitude;         // 强度/威胁值（由 Generator 计算）
    
    // --- 生命周期 ---
    float creationTime;
    float expirationTime;    // -1 = 永不自动过期
    int sourceChannelId;     // 产出该 Goal 的感知通道 ID
}
```

### 2.3 AIBehaviorInstance

行为在运行时的具体实例，承载执行状态。

```java
class AIBehaviorInstance {
    AiBehavior behaviorCfg;
    AIBrainComponent brain;

    // 绑定的 Goal 实例（决策阶段完成绑定）
    // Key: goalTag enumId, Value: 选出的具体 Goal 实例
    Int2ObjectMap<AIGoalInstance> boundGoals;

    // 任务执行栈
    Stack<AITaskInstance> taskStack;

    // 行为局部存储
    Store localStorage;
}
```

### 2.4 AITaskInstance

任务节点的运行时实例。

```java
abstract class AITaskInstance<T> {
    T taskCfg;

    abstract void onStart(AIBehaviorInstance ctx);
    abstract TaskStatus tick(AIBehaviorInstance ctx, float dt);
    abstract void onEnd(AIBehaviorInstance ctx, boolean wasAborted);
}

enum TaskStatus { Running; Success; Failed; }
```

---

## Part 3: Sense Layer（感知层）

### 3.1 Sense Channel

感知通道定义了 AI 获取信息的方式，不同通道有不同的物理特性。

```cfg
table ai_sense_channel[id] (json) {
    id: int;
    name: text;                      // 如 "Vision", "Hearing", "Damage"
    description: text;

    updateInterval: float;           // 更新频率（秒），0 = 每帧
    
    // 通道类型
    channelType: SenseChannelType;
}

interface SenseChannelType {
    // 空间扫描型（视觉/听觉等需要主动扫描周围的感知）
    struct Spatial {
        shape: SenseShape;
        filter: TargetFilter;        // 复用 GAS 的 TargetFilter
    }

    // 事件驱动型（受击/队友求援等被动接收的信息）
    struct EventDriven {
        listenEvent: str ->event_definition;
        // 事件触发后的额外过滤条件
        filterCondition: Condition (nullable);
    }
}

interface SenseShape {
    struct Sphere {
        radius: FloatValue;
    }
    struct Sector {
        radius: FloatValue;
        halfAngle: FloatValue;       // 半角（度），如视野 60° 配 30
    }
    // 听觉通常为球形 + 衰减，由 Sphere + magnitude 衰减实现
}
```

### 3.2 Goal Generator

GoalGenerator 绑定到感知通道上，将感知结果转化为标准化的 Goal 实例。

```cfg
table ai_goal_generator[id] (json) {
    id: int;
    name: text;
    description: text;

    // 绑定的感知通道
    channelId: int ->ai_sense_channel;

    // 产出的 Goal 的标签（Tag 驱动的类型标识）
    goalTag: str ->gameplaytag;

    // Goal 的存活时效（秒），-1 = 永久（直到被校验器剔除）
    goalDuration: float;

    // 强度计算公式（如：基于距离衰减、基于威胁等级）
    magnitudeFormula: FloatValue;

    // 生成条件：通道产出原始数据后，满足此条件才生成 Goal
    generateCondition: Condition (nullable);

    // 存活校验：每次感知更新时检查，不满足则剔除
    validators: list<GoalValidator>;
}

interface GoalValidator {
    // Actor 有效性（非 pendingKill）
    struct ActorIsValid {}

    // 标签校验
    struct ActorHasTags {
        query: TagQuery;
    }

    // 距离约束
    struct WithinDistance {
        maxDistance: FloatValue;
    }

    // 视线校验
    struct HasLineOfSight {}

    // 自定义条件（复用 Condition）
    struct CustomCondition {
        condition: Condition;
    }
}
```

### 3.3 Goal Tag 注册

Goal 的类型完全由 Tag 标识，不需要独立的定义表。Tag 层级自然支持模糊匹配。

```
// 在 gameplaytag 表中注册
"Goal.Combat.Enemy"
"Goal.Combat.Enemy.Melee"
"Goal.Combat.Enemy.Ranged"
"Goal.Cover"
"Goal.Cover.Flank"
"Goal.Cover.Retreat"
"Goal.Patrol.Waypoint"
"Goal.Alert.Sound"
"Goal.Alert.Visual"
"Goal.Interact.HealthPack"
```

**设计说明**：行为的 `requiredGoals` 使用 TagQuery 匹配，查询 `Goal.Combat.Enemy` 可命中 `Goal.Combat.Enemy.Melee` 和 `Goal.Combat.Enemy.Ranged`。这比通过表 ID 硬绑定灵活得多。

---

## Part 4: Think Layer（决策层）

### 4.1 Behavior

行为是决策的核心单元——声明准入条件、评分公式和执行任务。

```cfg
table ai_behavior[id] (json) {
    id: int;
    name: text;
    description: text;

    // --- 分类标签 ---
    behaviorTags: list<str> ->gameplaytag;   // 如 ["AI.Behavior.Combat.Melee"]

    // --- 抢占控制 ---
    // 0 = 常规行为（走 Score 算分）
    // >0 = 特权行为（数值越大优先级越高）
    interruptPriority: int;

    // 常规行为执行期间，是否允许被更高分的常规行为重选打断
    isInterruptible: bool;

    // 最小承诺时间（秒）：在此期间拒绝常规重选打断
    minCommitmentTime: float;

    // --- 准入条件 (Binary Gate) ---
    // 需要大脑中存在匹配这些 Tag 的活跃 Goal
    requiredGoals: list<GoalRequirement>;

    // 前置条件（复用 GAS Condition，And 语义）
    preConditions: list<Condition>;

    // 行为独立冷却（秒）
    cooldown: float;

    // --- 动态评分 ---
    score: FloatValue;

    // --- 惯性加分 ---
    // 当前行为正在执行时，评分自动叠加此值以防抖
    scoreInertia: float;

    // --- 执行 ---
    task: AITask;

    // --- 局部中止条件 ---
    // 复用 Condition，Or 语义：任一为 true 则立即清空任务栈触发重选
    abortConditions: list<Condition>;
}

struct GoalRequirement {
    goalQuery: TagQuery;            // 如 requireAny: ["Goal.Combat.Enemy"]
    // 当存在多个匹配 Goal 时，如何选一个绑定到行为实例
    selection: GoalSelection;
}

enum GoalSelection {
    Nearest;            // 距离最近
    HighestMagnitude;   // 强度/威胁最高
    MostRecent;         // 最新发现的
    Random;
}
```

### 4.2 Behavior Group

Group 的唯一目的是**共享 preConditions** 以减少配置冗余。底层引擎将其完全压平——Group 不存在于运行时。

```cfg
table ai_behavior_group[id] (json) {
    id: int;
    name: text;
    description: text;

    // 共享的前置条件（会被合并到组内每个行为的 preConditions 中）
    sharedConditions: list<Condition>;

    behaviors: list<int> ->ai_behavior;
    subGroups: list<int> ->ai_behavior_group;
}
```

### 4.3 Archetype

AI 实体的顶层配置入口。

```cfg
table ai_archetype[id] (json) {
    id: int;
    name: text;
    description: text;

    // 感知配置
    goalGenerators: list<int> ->ai_goal_generator;

    // 决策池
    behaviorGroups: list<int> ->ai_behavior_group;

    // 兜底行为（所有候选行为都不可用时执行）
    defaultBehaviorId: int ->ai_behavior;
}
```

---

## Part 5: Act Layer（执行层）

### 5.1 AITask

AITask 是行为执行的逻辑单元，基于任务栈驱动。引擎 Tick 永远只执行栈顶节点。

**核心原则**：
- AI 层**严禁直接操作表现层**（动画/特效/音效）
- 需要表现反馈的动作一律通过 `CastAbility` 触发，由 Ability 内部的 Cue 系统驱动
- `DoEffect` 仅用于无需等待的瞬间逻辑（如挂 Status、修改属性）

```cfg
interface AITask {
    // ============================================================
    //  原子动作
    // ============================================================

    // 释放技能（可等待完成）
    struct CastAbility {
        abilityId: int ->ability;
        target: TargetSelector;          // 复用 GAS TargetSelector（含 AI 扩展）
        waitForCompletion: bool;         // true = Running 直到技能执行完毕
    }

    // 执行瞬间 Effect（不等待，立即 Success）
    struct DoEffect {
        effect: Effect;
    }

    // 移动到目标
    struct MoveTo {
        target: TargetSelector;
        speed: FloatValue;
        tolerance: float;               // 到达距离容差
        timeout: float;                  // 超时自动 Failed（秒），-1 = 无超时
    }

    // 等待
    struct Wait {
        duration: FloatValue;
    }

    // 设置战斗目标
    struct SetCombatTarget {
        target: TargetSelector;
    }

    // 清除战斗目标
    struct ClearCombatTarget {}

    // ============================================================
    //  控制流
    // ============================================================

    struct Sequence {
        tasks: list<AITask>;
    }

    struct Selector {
        tasks: list<AITask>;             // 依次尝试，首个 Success 即终止
    }

    struct Parallel {
        tasks: list<AITask>;
        policy: ParallelPolicy;
    }

    struct Conditional {
        condition: Condition;            // 复用 GAS Condition
        then: AITask;
        otherwise: AITask (nullable);
    }

    struct Loop {
        count: FloatValue;               // -1 = 无限循环
        body: AITask;
    }

    // 作用域控制
    struct WithLocalVar {
        bindings: list<VarBinding>;      // 复用 GAS VarBinding
        task: AITask;
    }

    // 引用共享
    struct TaskRef {
        refId: int ->shared_ai_task;
        args: list<VarBinding>;
    }

    // 显式返回
    struct ReturnSuccess {}
    struct ReturnFailed {}
}

enum ParallelPolicy {
    WaitAll;        // 所有子任务 Success 才 Success，任一 Failed 则 Failed
    WaitAny;        // 任一 Success 即 Success
}

table shared_ai_task[id] (json) {
    id: int;
    name: text;
    description: text;
    task: AITask;
}
```

---

## Part 6: Behavior Modifier（动态行为修饰）

运行时通过 GAS 的 Status 系统动态修改 AI 决策池。修饰器的生命周期完全绑定到 Status，杜绝状态残留。

### 6.1 Modifier Definition

```cfg
table ai_behavior_modifier[id] (json) {
    id: int;
    name: text;
    description: text;

    // 禁用匹配这些 Tag 的行为
    disableBehaviorTags: TagQuery;

    // 注入新行为到决策池
    injectedBehaviors: list<int> ->ai_behavior;

    // 覆写全局参数（如：狂暴时缩短决策间隔）
    overrides: list<ModifierOverride>;
}

struct ModifierOverride {
    paramTag: str ->gameplaytag;     // 如 "AI.Param.DecisionInterval"
    value: FloatValue;
}
```

### 6.2 GAS 集成

在 GAS 的 `Behavior` 接口中追加 AI 修饰节点：

```cfg
// 追加到 ability-design 的 Behavior interface
interface Behavior {
    // ... (保留 GAS 原有的所有节点) ...

    // === AI 扩展 ===

    // Status 存在期间，向宿主 AI 注入行为修饰器
    struct AIBehaviorModifier {
        modifierId: int ->ai_behavior_modifier;
    }
}
```

**使用示例**：Boss 狂暴状态

```
status {
    id: 9001;
    name: "狂暴";
    grantedTags: ["State.Phase.Enrage"];
    duration: struct Const { value: -1.0; };
    stackingPolicy: struct Single { refreshMode: KeepDuration; };
    cuesWhileActive: ["Cue.Status.Enrage.Aura"];
    behaviors: [
        // 攻击力翻倍
        struct StatModifier {
            statTag: "Stat.Combat.Attack";
            op: Mul;
            value: struct Const { value: 2.0; };
        },
        // AI 决策池覆写
        struct AIBehaviorModifier {
            modifierId: 5001;  // 禁用普通平A，注入全屏AOE
        }
    ];
}
```

---

## Part 7: Global Settings

```cfg
table global_ai_settings[name] {
    name: str;

    // 全局强制中止条件（Condition，Or 语义）
    // 任一命中则挂起大脑，清空任务栈
    // 典型配置：宿主带有 "State.Dead" 或 "State.Debuff.Control.Stun"
    globalAbortConditions: list<Condition>;

    // 决策更新间隔（秒）
    // 非每帧决策，降低 CPU 开销
    decisionInterval: float;

    // 行为失败后的微冷却惩罚（秒）
    // 防止失败行为立即被重选导致抖动
    failurePenaltyCooldown: float;

    // 感知更新的全局时间抖动范围（秒）
    // 防止所有 AI 同帧更新感知导致的性能尖峰
    perceptionJitter: float;
}
```

---

## Part 8: Decision Flow

引擎 Tick 的执行顺序严格划分为五个阶段：

### Phase 1: 全局法则仲裁 (Global Override)

Tick 的第一道绝对防线。校验 `global_ai_settings.globalAbortConditions`。

- **命中**：强制清空任务栈，挂起大脑本帧所有后续 AI 逻辑。AI 物理宕机，躯体表现全盘交由 GAS 驱动。
- **未命中**：继续。

### Phase 2: 局部失效拦截 (Local Abort)

若大脑未宕机且存在 `activeBehavior`，校验其 `abortConditions`。

- **命中**：清空任务栈，`activeBehavior` 置空，触发该行为的冷却惩罚。
- **未命中**：继续。

### Phase 3: 感知更新 (Perception Update)

按各感知通道的 `updateInterval` 驱动 GoalGenerator：
1. 空间扫描型通道执行范围查询
2. 事件驱动型通道消费 EventBus 队列
3. Generator 根据 `generateCondition` 过滤，通过 `magnitudeFormula` 计算强度
4. 产出 GoalInstance 写入 `activeGoals`
5. 对所有已有 Goal 执行 `validators` 存活校验，剔除失效目标
6. 清理已过期（超过 `goalDuration`）的 Goal

### Phase 4: 决策与抢占 (Decision & Preemption)

收集所有通过 `requiredGoals` 和 `preConditions` 准入、且不在冷却中的候选行为。

#### Step A: 特权仲裁

筛选 `interruptPriority > 0` 的候选行为，取最高优先级组。

- **跨级碾压**：最高优先级 **严格大于** 当前行为优先级 → 立即抢占，清空旧任务栈
- **同级择优**：多个候选共享最高优先级（或与当前行为同级） → 计算 Score，最高分胜出（当前行为自动叠加 `scoreInertia`）

#### Step B: 常规重选

若特权未触发抢占，且当前行为满足（`isInterruptible == true` 且 `currentBehaviorElapsed >= minCommitmentTime`）：

- 对所有 `interruptPriority == 0` 的候选行为计算 Score
- 当前行为的 Score 自动叠加 `scoreInertia`
- 候选行为的 Score 必须**严格大于**当前行为的加持后总分才能夺取控制权

#### Step C: 兜底

若无 `activeBehavior` 且无候选行为通过准入 → 激活 `defaultBehaviorId`。

### Phase 5: 执行推进 (Actuation)

驱动 `activeBehavior.taskStack` 栈顶的 AITask：

- **Running**：维持现状
- **Success**：栈顶出栈，下一帧驱动新栈顶。整个栈空 → 行为正常完成，触发重选
- **Failed**：清空整个任务栈，触发该行为的冷却惩罚 + `failurePenaltyCooldown`，下一帧重选

---

## Part 9: Cooperation Architecture

AI 系统与 GAS 的协同遵循**单一事实来源**原则：

| 场景 | 驱动源 | 决策者 | 底层机制 | 示例 |
|---|---|---|---|---|
| **硬控 (Hard CC)** | 外部 GAS | GAS Tag | `globalAbortConditions` 命中，大脑物理宕机 | 死亡、眩晕、冰冻。AI 停止一切思考。 |
| **战术中断 (Soft)** | 内部 AI | AI 算分 | `abortConditions` 触发重选，或高分行为抢占 | 目标跑远放弃、紧急闪避、切流。 |
| **转阶段 (Phase)** | AI 决策 → GAS 托管 | AI 主导决策，GAS 接管状态 | 特权行为执行演出 → DoEffect 挂 Status → Status 内 AIBehaviorModifier 覆写决策池 | Boss 狂暴。AI 主动释放，GAS 长效托管。 |

---

## Part 10: Examples

### 例A：近战哥布林 AI

```
// === 感知通道 ===
ai_sense_channel {
    id: 1;
    name: "GoblinVision";
    updateInterval: 0.3;
    channelType: struct Spatial {
        shape: struct Sector { radius: struct Const { value: 20.0; }; halfAngle: struct Const { value: 60.0; }; };
        filter: {
            relationTo: struct ContextTarget {};
            allowedRelations: [Hostile];
            maxCount: 5;
            sort: Nearest;
        };
    };
}

ai_sense_channel {
    id: 2;
    name: "GoblinDamageReception";
    updateInterval: 0;
    channelType: struct EventDriven {
        listenEvent: "Event.Combat.Damage.Take.Post";
    };
}

// === 目标生成器 ===
ai_goal_generator {
    id: 1;
    name: "Gen_Vision_Enemy";
    channelId: 1;
    goalTag: "Goal.Combat.Enemy.Melee";
    goalDuration: 5.0;
    magnitudeFormula: struct Math {
        op: Div;
        a: struct Const { value: 100.0; };
        b: struct Math {
            op: Add;
            a: struct Distance { from: struct ContextTarget {}; to: struct ContextInstigator {}; };
            b: struct Const { value: 1.0; };
        };
    };
    validators: [
        struct ActorIsValid {},
        struct ActorHasTags { query: { exclude: ["State.Dead"]; }; },
        struct WithinDistance { maxDistance: struct Const { value: 30.0; }; }
    ];
}

ai_goal_generator {
    id: 2;
    name: "Gen_TakeDamage_Attacker";
    channelId: 2;
    goalTag: "Goal.Combat.Enemy.Melee";
    goalDuration: 8.0;
    magnitudeFormula: struct PayloadMagnitude {};
    generateCondition: struct TargetIsAlive { target: struct PayloadInstigator {}; };
    validators: [
        struct ActorIsValid {}
    ];
}

// === 行为 ===
ai_behavior {
    id: 101;
    name: "Goblin_Chase";
    behaviorTags: ["AI.Behavior.Combat.Chase"];
    interruptPriority: 0;
    isInterruptible: true;
    minCommitmentTime: 0.5;
    requiredGoals: [
        { goalQuery: { requireAny: ["Goal.Combat.Enemy"]; }; selection: Nearest; }
    ];
    preConditions: [
        struct Compare {
            left: struct Distance { from: struct ContextTarget {}; to: struct GoalActor { goalTag: "Goal.Combat.Enemy"; }; };
            op: Gt;
            right: struct Const { value: 2.0; };
        }
    ];
    cooldown: 0;
    score: struct Math {
        op: Sub;
        a: struct Const { value: 60.0; };
        b: struct Distance { from: struct ContextTarget {}; to: struct GoalActor { goalTag: "Goal.Combat.Enemy"; }; };
    };
    scoreInertia: 10.0;
    task: struct Sequence {
        tasks: [
            struct SetCombatTarget { target: struct GoalActor { goalTag: "Goal.Combat.Enemy"; }; },
            struct MoveTo {
                target: struct AICombatTarget {};
                speed: struct StatOf { source: struct ContextTarget {}; statTag: "Stat.Movement.Speed"; capture: Current; };
                tolerance: 1.5;
                timeout: 10.0;
            }
        ];
    };
    abortConditions: [
        struct Not { condition: struct TargetIsAlive { target: struct AICombatTarget {}; }; },
        struct Compare {
            left: struct Distance { from: struct ContextTarget {}; to: struct AICombatTarget {}; };
            op: Gt;
            right: struct Const { value: 25.0; };
        }
    ];
}

ai_behavior {
    id: 102;
    name: "Goblin_MeleeAttack";
    behaviorTags: ["AI.Behavior.Combat.Attack"];
    interruptPriority: 0;
    isInterruptible: false;
    minCommitmentTime: 0;
    requiredGoals: [
        { goalQuery: { requireAny: ["Goal.Combat.Enemy"]; }; selection: Nearest; }
    ];
    preConditions: [
        struct Compare {
            left: struct Distance { from: struct ContextTarget {}; to: struct GoalActor { goalTag: "Goal.Combat.Enemy"; }; };
            op: Lte;
            right: struct Const { value: 2.0; };
        },
        struct CanActivateAbility { abilityId: 1; }
    ];
    cooldown: 0.8;
    score: struct Const { value: 80.0; };
    scoreInertia: 5.0;
    task: struct CastAbility {
        abilityId: 1;  // 普通攻击
        target: struct AICombatTarget {};
        waitForCompletion: true;
    };
    abortConditions: [];
}

ai_behavior {
    id: 100;
    name: "Goblin_Idle";
    behaviorTags: ["AI.Behavior.Idle"];
    interruptPriority: 0;
    isInterruptible: true;
    minCommitmentTime: 0;
    requiredGoals: [];
    preConditions: [];
    cooldown: 0;
    score: struct Const { value: 1.0; };
    scoreInertia: 0;
    task: struct Wait { duration: struct Const { value: 2.0; }; };
    abortConditions: [];
}

// === Archetype ===
ai_archetype {
    id: 1;
    name: "Goblin_Melee";
    goalGenerators: [1, 2];
    behaviorGroups: [1];
    defaultBehaviorId: 100;
}

ai_behavior_group {
    id: 1;
    name: "Goblin_Combat";
    sharedConditions: [
        struct HasTags { source: struct ContextTarget {}; query: { exclude: ["State.Dead"]; }; }
    ];
    behaviors: [101, 102];
    subGroups: [];
}
```

### 例B：Boss 转阶段

```
// 特权行为：进入狂暴
ai_behavior {
    id: 201;
    name: "Boss_Enter_Enrage";
    behaviorTags: ["AI.Behavior.Phase.Enrage"];
    interruptPriority: 90;           // 极高特权
    isInterruptible: false;
    minCommitmentTime: 0;
    requiredGoals: [];
    preConditions: [
        // 血量 <= 30%
        struct Compare {
            left: struct StatOf { source: struct ContextTarget {}; statTag: "Stat.HP.Percent"; capture: Current; };
            op: Lte;
            right: struct Const { value: 0.3; };
        },
        // 未在狂暴状态
        struct Not {
            condition: struct HasTags { source: struct ContextTarget {}; query: { requireAny: ["State.Phase.Enrage"]; }; };
        }
    ];
    cooldown: 9999;                  // 只触发一次
    score: struct Const { value: 100.0; };
    scoreInertia: 0;
    task: struct Sequence {
        tasks: [
            // 1. 给自身挂临时霸体
            struct DoEffect {
                effect: struct GrantTags {
                    target: struct ContextTarget {};
                    tags: ["State.Buff.Hyperarmor"];
                    duration: struct Const { value: 5.0; };
                };
            },
            // 2. 播放狂暴怒吼（通过 Ability 驱动动画+Cue）
            struct CastAbility {
                abilityId: 9001;     // "怒吼"Ability，内部挂 Cue
                target: struct ContextTarget {};
                waitForCompletion: true;
            },
            // 3. 挂载长效狂暴 Status（内含 AIBehaviorModifier）
            struct DoEffect {
                effect: struct ApplyStatus {
                    target: struct ContextTarget {};
                    statusId: 9001;  // "狂暴" Status
                    captures: [];
                };
            }
        ];
    };
    abortConditions: [];
}

// 行为修饰器：狂暴阶段覆写决策池
ai_behavior_modifier {
    id: 5001;
    name: "Enrage_Modifier";
    disableBehaviorTags: { requireAny: ["AI.Behavior.Combat.Attack.Normal"]; };
    injectedBehaviors: [301, 302];   // 全屏AOE、三连斩等狂暴招式
}
```

### 例C：巡逻与警戒

```
// 巡逻路点 Goal Generator（无需感知通道，由程序初始化注入）
ai_goal_generator {
    id: 10;
    name: "Gen_PatrolWaypoints";
    channelId: 3;                    // 一个 interval=0 的虚拟 Spatial 通道
    goalTag: "Goal.Patrol.Waypoint";
    goalDuration: -1;
    magnitudeFormula: struct Const { value: 1.0; };
    validators: [];
}

ai_behavior {
    id: 501;
    name: "Patrol";
    behaviorTags: ["AI.Behavior.Patrol"];
    interruptPriority: 0;
    isInterruptible: true;
    minCommitmentTime: 0;
    requiredGoals: [
        { goalQuery: { requireAny: ["Goal.Patrol.Waypoint"]; }; selection: Nearest; }
    ];
    preConditions: [
        // 没有战斗目标时才巡逻
        struct Not { condition: struct HasGoal { goalTag: "Goal.Combat.Enemy"; }; }
    ];
    cooldown: 0;
    score: struct Const { value: 10.0; };
    scoreInertia: 5.0;
    task: struct Sequence {
        tasks: [
            struct MoveTo {
                target: struct GoalLocation { goalTag: "Goal.Patrol.Waypoint"; };
                speed: struct Const { value: 3.0; };
                tolerance: 1.0;
                timeout: 30.0;
            },
            struct Wait { duration: struct Const { value: 3.0; }; }
        ];
    };
    abortConditions: [
        // 发现敌人立即中断巡逻
        struct HasGoal { goalTag: "Goal.Combat.Enemy"; }
    ];
}
```