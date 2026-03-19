


# AI 行为系统设计

本文档定义了一套与 `能力系统设计` 无缝集成的 AI 行为系统。设计以数据驱动为核心，汲取 Bobby Anguelov《AI Behavior Selector》的理念，通过扁平化评分机制替代传统行为树，解决调试困难与条件冗余问题。

## Philosophy

1. **三段式分治 (Sense - Think - Act)**
   - **Sense**：处理传感器与事件输入，生成数据化目标。
   - **Think**：表达式动态算分，选出最优行为。
   - **Act**：驱动动作序列，管理执行生命周期与抢占打断。

2. **数据驱动与动态组合**
   行为通过配置组合构建，运行时支持动态插拔行为修饰，零耦合应对 Boss 转阶段等特殊情境。

3. **单 Goal 驱动**
   每个行为由唯一 Goal 驱动决策。执行期若需额外目标（如"搬桶砸人"中的敌人），通过 `RuntimeQuery` 或 `WithLocalVar` 实时获取，严格区分决策动机与执行参数。

4. **表达式动态决策**
   准入条件与优先级评分完全由多态表达式驱动，基于属性、距离、时效等变量实时演算。

5. **白盒可调试性**
   底层为无状态扁平评分，调试端基于 Group 呈现逻辑目录，支持实时可视化算分、冷却与准入状态。

---

## Runtime Core

AI 运行时由四层实例自顶向下逐级持有：

- **AIBrainComponent**：挂载于 Actor 的中枢组件。维护认知库（`activeGoalsByType`，按类型分桶）、当前执行体（`activeBehavior`）、行为修饰栈（`behaviorModifiers`）与冷却表（`behaviorCooldowns`）。不硬编码任何感知字段，所有感知信息统一由 Goal 表达。

- **AIGoalInstance**：感知阶段的产物。由 `GoalGenerator` 生成，携带目标类型、关联实体、位置、强度与时间戳，作为决策阶段的候选输入。

- **AIBehaviorInstance**：行为的运行时实例。封装静态配置（`ai_behavior`）、绑定的唯一驱动目标（`boundGoal`）、任务栈（`taskStack`）与局部存储（`localStorage`）。

- **AITaskInstance**：任务栈中的执行单元。持有静态配置（`taskCfg`），实现 `onStart` / `tick` / `onEnd` 生命周期，由栈顶驱动推进。


```java
class AIBrainComponent {
    Actor self;

     // 认知记忆库：按 GoalType 分桶存储所有未失效的客观刺激源
    Int2ObjectMap<List<AIGoalInstance>> activeGoalsByType;

    // 执行状态
    AIBehaviorInstance activeBehavior;
    List<AIBehaviorModifierRef> behaviorModifiers;
    Int2FloatMap behaviorCooldowns;
}

class AIGoalInstance {
    Ai_goal_definition goalCfg;
    Actor associatedActor;  // 关联实体（可为 null，如纯位置型 Goal）
    Vector3 position;
    float magnitude;
    float creationTime;
}

class AIBehaviorInstance {
    Ai_behavior behaviorCfg;
    AIBrainComponent aiBrain;

    AIGoalInstance boundGoal;          // 动机锚点，可为null
    Stack<AITaskInstance> taskStack;   // 扁平化执行栈
    Store localStorage;                // 战术变量寄存器
}

abstract class AITaskInstance<T extends AITask> {
    T taskCfg;

    abstract void onStart(AIBehaviorInstance ctx);
    abstract TaskStatus tick(AIBehaviorInstance ctx, float deltaTime);
    abstract void onEnd(AIBehaviorInstance ctx, boolean wasAborted);
}

enum TaskStatus { Running, Success, Failed }

class AIBehaviorModifierRef {
    Ai_behavior_modifier modifierCfg;
    int refCount;
}
```

---

## 配置定义

### global_ai_settings

全局 AI 设置表，作为单例在引擎初始化时加载。它定义了全游戏所有 AI 必须遵守的"最高物理法则"（如：死亡或被硬控时强制瘫痪）。

```cfg
table global_ai_settings[name] (entry="name") {
    name: str; // 如: "default"

    // 全局强制中止条件
    // 引擎 Tick 时优先校验。只要满足其一（如宿主带有 "State.Debuff.Control"），
    // 且当前行为没有豁免权，则立刻清空任务栈并瘫痪大脑。
    abortConditions: list<AbortCondition>;
}
```

### ai_archetype

每个需要 AI 功能的实体（如怪物、NPC、宠物）在初始化时，都需要引用一条 `ai_archetype` 数据。它是 AI 系统的最上层配置入口。

```cfg
table ai_archetype[name] {
    name: str;          // 如: "Goblin_Melee", "Boss_Dragon"
    description: text;

    // 感知与目标生成 (What COULD I do?)
    goalGenerators: list<str> -> ai_goal_generator;

    // 行为决策池 (What SHOULD I do?)
    behaviorGroups: list<str> -> ai_behavior_group;

    // 兜底行为 (Fallback/Default)
    defaultBehavior: str -> ai_behavior;
}
```

### ai_behavior

行为是独立的、可单独测试的逻辑单元。它声明了准入条件、算分公式和最终要执行的动作组合。

```cfg
table ai_behavior[name] (json) {
    [behaviorId];
    name: str;
    behaviorId: int;
    description: text;

    behaviorTags: list<str> -> gameplaytag;

    // 常规行为在执行期间，是否允许被更高分的常规行为平滑重选打断
    isInterruptible: bool;

    // 抢占优先级
    // 0 表示常规行为（走普通的 Score 算分重选流程）。
    // >0 表示特权行为，数值越大优先级越高（如：死亡=100, 坠落/击飞=80, 重受击=50, 轻受击=20）。
    // 优先判断优先级，优先级相同时判断 score
    interruptPriority: int;

    // 最小承诺时间。在这个时间内，哪怕 isInterruptible 为 true，
    // 且其他行为分数更高，也强制拒绝被打断（除非是特权抢占）。
    minCommitmentTime: float;

    // ─── 准入过滤 (Binary Filter) ───
    // 驱动该行为的唯一 Goal 需求（null 表示无需 Goal，如 Idle、死亡反应）
    requiredGoal: GoalRequirement;
    preConditions: list<PreCondition>;  // AND 语义
    cooldown: float;                    // 行为独立冷却期

    // ─── 动态评分 (Prioritization) ───
    score: ScoreValue;

    // ─── 行为执行 (Actuation) ───
    task: AITask;

    // ─── 中止条件 ───
    // 只要有任意一个返回 True (OR)，立刻清空 Task 栈并触发重选
    abortConditions: list<AbortCondition>;
}

interface GoalRequirement {
    struct None {}
    struct Require {
        goalDef: str -> ai_goal_definition;
        selector: GoalSelector;
    }
}

enum GoalSelector {
    Nearest;    // 距离自身最近
    Strongest;  // magnitude 最大
    MostRecent; // creationTime 最新
    Oldest;     // 存在最久
}
```

### ai_behavior_group

组 (Group) 的唯一目的是**共享 `sharedConditions`**，以减少配置冗余。

```cfg
table ai_behavior_group[name] {
    name: str;
    description: text;
    sharedConditions: list<PreCondition>;
    behaviors: list<str> -> ai_behavior;
    subGroups: list<str> -> ai_behavior_group;
}
```

---

## 感知阶段（Sense）

`GoalGenerator` 挂载在 AI Archetype 上，负责将外部世界信息（传感器数据、事件）转化为 Goal。

### 上下文定义

**SenseContext = { Actor self, Event? event }**

✅ 可访问：self（自身的物理坐标、朝向、Tag 等）、event（外部传入的事件载荷，如受击事件）。

**ValidatorContext = { Actor self, AIGoalInstance goal }**

专门用于 GoalValidator 的上下文。用于在每帧或周期性 Tick 时，基于客观物理世界（距离、存活状态、视线）校验已有 Goal 的合法性。


### 配置定义

```cfg
table ai_goal_generator[name] (json) {
    name: str;              // 如: "Gen_Scan_Enemy", "Gen_Listen_Gunshot"
    description: text;
    generator: GoalGenerator;
}

table ai_goal_definition[name] {
    [goalId];
    name: str;
    goalId: int;
    description: text;

    // 存活校验条件 (AND 语义)
    // 每帧感知更新时，如果此条件组返回 false，大脑立即将该 Goal 判定为失效并剔除。
    validConditions: list<GoalValidator>;
}

interface GoalGenerator {
    // 周期扫描型：定期查询空间索引
    struct SpatialScan {
        query: SenseTargetQuery;
        interval: float;              // 扫描间隔（秒）
        generatedGoal: str -> ai_goal_definition;
    }

    // 事件驱动型：监听 EventBus
    struct OnEvent {
        listenEventTag: str -> event_definition;
        triggerCondition: SenseCondition;
        generatedGoal: str -> ai_goal_definition;
        extractActor: SenseActorExtractor;
        duration: float;               // Goal 存活时效（秒）
    }
}

// ─── Sense 阶段的完整查询（几何 + Sense 上下文绑定）───
interface SenseTargetQuery {
    struct SphereScan {
        center: SenseLocation;              // Sense 专属
        shape: SpatialQueryShape.Sphere;
    }
    struct ConeScan {
        origin: SenseLocation;              // Sense 专属
        direction: SenseDirection;          // Sense 专属
        shape: SpatialQueryShape.Cone;
    }
}

// Sense 专属位置（上下文：Actor self, Event? event）
interface SenseLocation {
    struct Self {}
    struct EventSourcePosition {}
}

interface SenseDirection {
    struct SelfForward {}
}

// ─── 空间查询几何定义（阶段无关，纯数据）───
// 描述"查什么形状、过滤什么、取几个"，不关心"圆心/原点从哪来"
interface SpatialQueryShape {
    struct Sphere {
        radius: float;
        requiredTags: TagQuery;
        sortBy: SenseSortPolicy;
        maxResults: int;
    }
    struct Cone {
        angle: float;                  // 半角（度）
        range: float;
        requiredTags: TagQuery;
        sortBy: SenseSortPolicy;
        maxResults: int;
    }
}

enum SenseSortPolicy {
    Nearest;
    Strongest;
    MostRecent;
}

// 条件：决定事件是否应生成 Goal
interface SenseCondition {
    struct Const { value: bool; }
    struct And { conditions: list<SenseCondition>; }
    struct Or  { conditions: list<SenseCondition>; }
    struct Not { condition: SenseCondition; }

    struct EventSourceHasTags {
        tagQuery: TagQuery;
    }
    struct EventSourceDistance {
        op: CompareOp;
        value: float;
    }
    struct SelfHasTags {
        tagQuery: TagQuery;
    }
}

// Actor 提取器：从事件 payload 中提取关联实体
interface SenseActorExtractor {
    struct EventSource {}              // 事件的发送者
    struct EventPayloadActor {         // 从 payload 指定字段提取
        fieldName: str;
    }
    struct Null {}                     // 无关联 Actor（纯位置型 Goal）
}

// Goal 存活校验
interface GoalValidator {
    struct TargetIsValid {}            // associatedActor 未被销毁
    struct TargetHasTags {
        requirements: TagQuery;
    }
    struct DistanceLessThan {
        maxDistance: float;
    }
    struct HasLineOfSight {}
}
```

---

## 思考阶段（Think）

`PreCondition`、`ScoreValue` 用于 `ai_behavior` 的准入判断和打分。采用单 Goal 驱动。

### 上下文定义

**ThinkContext = { AIBrainComponent brain, Ai_behavior behavior, AIGoalInstance? candidateGoal }**
- ✅ 可访问：`brain` 全部字段（包括 `activeGoalsByType`，只读）、当前评估的 `behavior` 配置、候选绑定的 `candidateGoal`
- ❌ 不可修改：`activeGoalsByType`（只读消费）


### 配置定义

```cfg
interface PreCondition {
    struct And { conditions: list<PreCondition>; }
    struct Or  { conditions: list<PreCondition>; }
    struct Not { condition: PreCondition; }
    struct TargetHasTags {
        target: ThinkActor;
        tagQuery: TagQuery;
    }
    struct CanActivateAbility {
        abilityId: int -> ability;
    }
    struct Compare {
        a: ScoreValue;
        op: CompareOp;
        b: ScoreValue;
    }
    // 检查 activeGoalsByType 中是否存在指定类型的 Goal 实例
    struct GoalTypeExists {
        goalDef: str -> ai_goal_definition;
    }
    // 检查指定行为是否处于冷却中
    struct IsOnCooldown {
        behaviorId: int -> ai_behavior;
    }
}

interface ScoreValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: ScoreValue; b: ScoreValue; }
    struct Clamp { value: ScoreValue; min: float; max: float; }

    struct Distance {
        from: ThinkLocation;
        to: ThinkLocation;
    }
    struct GoalAge {}              // 取 candidateGoal.creationTime 计算存活时长
    struct GoalMagnitude {}        // 取 candidateGoal.magnitude
    struct StatValue {
        target: ThinkActor;
        statTag: str -> stat_definition;
    }
    struct IsActiveBehavior {
        trueValue: float;          // 如果当前正在执行该行为，返回此加分
        falseValue: float;         // 否则返回此值
    }
    struct ActiveBehaviorElapsedTime {}  // 当前行为的已执行时间
}

// Think 阶段的目标引用（单 Goal 驱动，无需指定 goal 参数）
interface ThinkActor {
    struct Self {}
    struct BoundGoalActor {}       // candidateGoal.associatedActor
}

interface ThinkLocation {
    struct Self {}
    struct BoundGoalActor {}       // candidateGoal.associatedActor 的位置
    struct BoundGoalLocation {}    // candidateGoal.position
}
```

---

## 行动阶段（Act）

`AITask` 将 `ai_behavior` 转化为具体动作序列。执行基于**任务栈**推进：复合节点（Sequence、Conditional 等）将子任务压入栈顶，引擎 Tick 始终只驱动栈顶节点（Parallel 驻留栈顶时内部维护并行子实例）。

行为打断采用**双轨协同**：

1. **外部中止 (Abort)**：Tick 前大脑校验 `abortConditions`，命中则强制清空任务栈，广播 `onEnd(wasAborted = true)` 并触发重选。
2. **内部冒泡 (Failed)**：微观执行受阻（寻路失败、GAS 拦截等）时，任务主动冒泡 `Failed` 正常出栈，触发冷却惩罚后平滑重选。


### 上下文定义

**ActContext = AIBehaviorInstance**
- ✅ 可访问：`behaviorInstance.aiBrain`、`behaviorInstance.boundGoal`、`behaviorInstance.localStorage`
- `AbortCondition` 也使用此上下文（因为校验时行为已在执行中）


### 配置定义

```cfg
interface AITask {
    struct None {
    }
    // ─── 基础动作 ───
    struct CastAbility {
        abilityId: int -> ability;
        target: AITargetSelector;
    }
    struct DoEffect {
        effect: Effect;
        target: AITargetSelector;
    }
    struct MoveTo {
        target: AITargetSelector;
        speedStat: str -> stat_definition;
        tolerance: float;              // 到达距离容差
        stopOnFinish: bool;            // 到达后是否停止
    }
    struct PlayAnimation {
        animName: str;
        blendInTime: float;
        waitForCompletion: bool;
    }
    struct PlayCue {
        cueTag: str -> gameplaytag;
        target: AITargetSelector;
    }
    struct Speak {
        dialogueId: int -> dialogue;
        target: AITargetSelector;
    }
    struct Interact {
        target: AITargetSelector;
        interactionId: int;
    }
    struct Wait {
        duration: AIFloatValue;
    }

    // ─── 控制流 ───
    struct Sequence {
        tasks: list<AITask>;
    }
    struct Parallel {
        tasks: list<AITask>;
        policy: ParallelPolicy;
    }
    struct Conditional {
        condition: AICondition;
        thenTask: AITask;
        elseTask: AITask;
    }
    struct Loop {
        count: AIFloatValue;            // -1 = 无限
        task: AITask;
    }
    struct ReturnFailed {}
    struct ReturnSuccess {}

    // ─── 作用域控制 ───
    struct WithLocalVar {
        varKey: str -> var_key;
        value: AILocalVarSource;
        task: AITask;
    }

    // ─── 引用共享 ───
    struct TaskRef {
        sharedTask: str -> shared_ai_task;
    }
}

// 局部变量数据源（支持存储数值和 Actor）
interface AILocalVarSource {
    struct FloatValue {
        value: AIFloatValue;
    }
    struct ActorFromQuery {
        query: ActSpatialQuery;
        pickPolicy: SenseSortPolicy;
    }
    struct ActorFromGoalType {
        goalDef: str -> ai_goal_definition;
        selector: GoalSelector;
    }
}

enum ParallelPolicy {
    WaitAll;
    WaitAny;
}

table shared_ai_task[name] (json) {
    name: str;
    description: text;
    task: AITask;
}

// 目标选择器：从执行上下文中解析出一个 Actor 或位置
interface AITargetSelector {
    struct Self {}                     // behaviorInstance.aiBrain.self
    struct BoundGoalActor {}           // behaviorInstance.boundGoal.associatedActor
    struct BoundGoalLocation {}        // behaviorInstance.boundGoal.position
    struct LocalVarActor {
        varTag: str -> gameplaytag;
    }
    // 运行时空间查询（用于执行期的战术微操，如寻找闪避位置）
    struct RuntimeQuery {
        query: ActSpatialQuery;
        pickPolicy: SenseSortPolicy;
    }
}

// ─── Act 阶段的空间查询（几何 + Act 上下文绑定）───
interface ActSpatialQuery {
    struct SphereScan {
        center: AITargetSelector;           // Act 专属，全功能
        shape: SpatialQueryShape.Sphere;
    }
    struct ConeScan {
        origin: AITargetSelector;           // Act 专属
        direction: ActDirection;            // Act 专属
        shape: SpatialQueryShape.Cone;
    }
}

// Act 专属方向
interface ActDirection {
    struct ActorForward {
        actor: AITargetSelector;
    }
}


// 执行期条件判断
interface AICondition {
    struct Const { value: bool; }
    struct And { conditions: list<AICondition>; }
    struct Or  { conditions: list<AICondition>; }
    struct Not { condition: AICondition; }

    struct Compare {
        a: AIFloatValue;
        op: CompareOp;
        b: AIFloatValue;
    }
    struct TargetHasTags {
        target: AITargetSelector;
        tagQuery: TagQuery;
    }
    struct TargetIsValid {
        target: AITargetSelector;
    }
    struct HasLineOfSight {
        from: AITargetSelector;
        to: AITargetSelector;
    }
}

// 执行期数值
interface AIFloatValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: AIFloatValue; b: AIFloatValue; }
    struct RandomRange { min: float; max: float; }

    struct StatValue {
        target: AITargetSelector;
        statTag: str -> stat_definition;
    }
    struct Distance {
        from: AITargetSelector;
        to: AITargetSelector;
    }
    struct LocalVar {
        varTag: str -> gameplaytag;
    }
    struct BoundGoalMagnitude {}    // behaviorInstance.boundGoal.magnitude
}

// ─── 中止条件（挂在 ai_behavior 上，使用 Act 阶段上下文）───
interface AbortCondition {
    struct TargetIsDead {
        target: AITargetSelector;
    }
    struct DistanceGreaterThan {
        from: AITargetSelector;
        to: AITargetSelector;
        maxDistance: float;
    }
    struct SelfHasAnyTags {
        tags: list<str> -> gameplaytag;
    }
    // 通用桥接：任何 AICondition 都可以作为中止条件
    struct Custom {
        condition: AICondition;
    }
}
```

---

## 动态行为修饰

允许在运行时通过赋予实体一个 `ai_behavior_modifier`，瞬间改变其决策池。极其适合处理 Boss 转阶段或特殊异常状态。

```cfg
table ai_behavior_modifier[name] {
    name: str;
    description: text;
    disableBehaviorsQuery: TagQuery;
    injectedBehaviors: list<BehaviorInjection>;
}

struct BehaviorInjection {
    targetGroup: str -> ai_behavior_group;
    behaviors: list<str> -> ai_behavior;
}
```

在 `ability-design` 的 `Behavior` 接口中扩展如下结构：

```cfg
interface Behavior {
    struct AIBehaviorModifier {
        modifier: str -> ai_behavior_modifier;
    }
}
```

**设计考量**：`ai_behavior_modifier` 的生命周期绑定于 GAS Status 的作用域，挂载即生效、卸载即清零，从根本上杜绝状态残留。这也是 AITask 中不引入 Add/RemoveAIBehaviorModifier 节点的原因。

---

## 决策流程

引擎 Tick 严格划分为五个阶段：

**1. 全局法则仲裁 (Global Override)**
校验 `global_ai_settings` 中的全局硬控条件（如眩晕、死亡 Tag）。命中则清空任务栈并**挂起本帧全部后续 AI 逻辑**，躯体表现交由 GAS 接管。

**2. 局部失效拦截 (Local Abort)**
校验 `activeBehavior` 的 `abortConditions`。命中则清空任务栈，置空 `activeBehavior`。

**3. 感知更新 (Perception Update)**
驱动 `GoalGenerator` 刷新候选目标，执行 `GoalValidator` 剔除失效 Goal，维护认知库一致性。

**4. 决策与抢占 (Decision & Preemption)**

遍历候选行为，逐一执行准入评估：

```
for each candidate behavior:
  A. Goal 绑定：requiredGoal 非空时，从 activeGoalsByType 按 GoalSelector 选出 candidateGoal；为空则跳过
  B. 构建 ThinkContext { brain, behavior, candidateGoal }
  C. 评估 preConditions + sharedConditions（AND），任一不满足则跳过
  D. 校验 cooldown，冷却中则跳过
  E. 计算 score
```

通过准入的候选按以下规则仲裁：

- **特权仲裁 (Priority First)**：存在 `interruptPriority > 0` 的候选时，取最高优先级组。严格高于当前行为优先级则立即抢占；同级则按 Score 择优，当前行为叠加 `scoreInertia` 防抖。

- **常规重选 (Score Second)**：特权未触发时，若当前行为可打断（`isInterruptible && 已过 minCommitmentTime`），对常规队列算分。候选须严格超过当前行为含惯性加分的总分方可夺权。

- 胜出行为的 `candidateGoal` 写入 `AIBehaviorInstance.boundGoal`。

**5. 执行推进 (Actuation)**
驱动 `activeBehavior` 任务栈栈顶节点 Tick：

- **Running**：维持推进。
- **Success**：出栈，栈空则行为完成，下帧重选。
- **Failed**：清空任务栈，触发冷却惩罚，下帧重新决策。

---

## 协同架构：打断与控制流派

AI 系统与 GAS 通过配置协同，贯彻单一数据源原则：**每类状态只有唯一归属系统，跨系统通过 Tag 只读消费，绝不冗余复制。** GAS 管"身体处于什么状态"，AI 管"接下来想做什么"。

| 场景 | 状态归属 | 机制 | 案例 |
| :--- | :--- | :--- | :--- |
| **物理受控 (Hard CC)** | GAS 独占控制状态 | AI 不维护控制标记，仅读取 GAS Tag 做 `global_ai_settings` 全局拦截，清空执行栈并挂起大脑 Tick。 | 死亡、眩晕、冰冻、击飞 |
| **战术中断 (Soft Interrupt)** | AI 独占战术意图 | `abortConditions` 顺滑重选或 `interruptPriority` 高优抢占，GAS 无感知。 | 放弃追击、紧急闪避、切换目标 |
| **转阶段 (Phase)** | GAS 独占阶段状态 | AI 特权行为执行演出后，将状态托管给 GAS Status。`AIBehaviorModifier` 生命周期绑定于此 Status，卸载即原子清零。AI 通过读取 Tag 防重复触发。 | Boss 狂暴：霸体怒吼 → 注入新技能池，随战斗重置安全卸载 |


## 数据流向

```
Sense 产出 AIGoalInstance
  → 写入 brain.activeGoalsByType
    → Think 通过 GoalRequirement 消费，选出 candidateGoal
      → 胜出后写入 AIBehaviorInstance.boundGoal
        → Act 通过 BoundGoalActor / BoundGoalLocation 消费
```


---

## Examples

### Boss 狂暴转阶段（AI 决策 + GAS 托管）

将"转阶段"设计为 AI 主动寻机的战术动作而非被动触发，避免扣血瞬间强行切断当前动画的表现突变。

1. **决策**：特权行为 `Boss_Enter_Phase2`（高 `interruptPriority`），前置条件查询血量 ≤ 30% 且自身无 `State.Phase.Enrage` Tag（防重复）。无需 `requiredGoal`。

2. **演出**：Sequence 首节点通过 `DoEffect` 挂载临时霸体 Status（GAS `blockTags` 拦截控制），随后 `PlayAnimation` 播放怒吼动画。

3. **移交**：演出结束，`DoEffect` 挂载长效 `Status_Enrage`，AI 战术动作完成，控制权移交 GAS。

4. **托管**：`Status_Enrage` 作为唯一数据源驱动后续逻辑：`StatModifier` 翻倍攻击力；`cuesWhileActive` 播放特效；关联 `AIBehaviorModifier` 覆写行为池（屏蔽平 A、注入全屏 AOE）并赋予 `State.Phase.Enrage` Tag。Status 销毁时所有效果原子化卸载，AI 恢复初始逻辑。

### 搬桶砸人（单 Goal 驱动 + RuntimeQuery）

展示决策动机与执行参数的分离：行为由 `Goal.Item.Barrel` 驱动决策，执行期通过 `ActorFromQuery` 实时获取投掷目标。

```cfg
name: "Throw_Barrel_At_Enemy"
requiredGoal: { goalDef: "Goal.Item.Barrel", selector: Nearest {} }
preConditions: [
    { GoalTypeExists { goalDef: "Goal.Combat.Enemy" } }
]
score: Const { value: 40.0 }
task: Sequence {
    tasks: [
        MoveTo { target: BoundGoalActor {}, tolerance: 1.5, stopOnFinish: true },
        Interact { target: BoundGoalActor {}, interactionId: 1 },
        WithLocalVar {
            varTag: "Var.ThrowTarget",
            value: ActorFromQuery {
                query: SphereScan {
                    center: Self {},
                    shape: Sphere {
                        radius: 20.0,
                        requiredTags: "Faction.Enemy AND Character.Alive",
                        sortBy: Nearest,
                        maxResults: 1
                    }
                },
                pickPolicy: Nearest
            },
            task: CastAbility {
                abilityId: "Ability_ThrowBarrel",
                target: LocalVarActor { varTag: "Var.ThrowTarget" }
            }
        }
    ]
}
abortConditions: [
    { TargetIsDead { target: BoundGoalActor {} } }
]
```

数据流追溯：
```
SpatialScan 发现桶 → Goal(Barrel, actor=桶A)
  → Think: GoalRequirement 绑定桶A, GoalTypeExists 确认有敌人
    → Act: BoundGoalActor=桶A, ActorFromQuery=敌人B
      → CastAbility 以敌人B为目标投掷
```