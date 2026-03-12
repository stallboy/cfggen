# AI 行为管线架构 (AI Behavior Pipeline Architecture)

本文档旨在提供一套与 `GAS` 技能系统无缝对接、高内聚且完全数据驱动的现代 AI 决策架构。它引入了 Bobby Anguelov 在《AI Behavior Selector》中提出的架构核心，摒弃了传统行为树（BT）中结构僵化、难以调试和条件冗余的缺陷，采用**“分而治之 (Divide and Conquer)”** 的扁平化评分机制，致力于打造高扩展性的工业级 AI 管线。

## Philosophy

1. **三段式分治架构 (Sense - Think - Act)**
将 AI 管线严格拆分为三个解耦的独立阶段：
    * **目标生成**（我可以做什么？）：处理传感器与事件输入，生成纯数据化的可用目标。
    * **行为抉择**（我应该做什么？）：通过表达式动态算分，从候选池中选出当前优先级最高的行为。
    * **行为执行**（我具体怎么做？）：驱动底层的动作序列（位移、动画、施法），妥善管理节点的运行生命周期、成功/失败状态与高优抢占打断。

2. **数据驱动与动态组合 (Data-Driven & Composition)**
摈弃硬编码。所有基础行为均通过配置组合构建，并支持在运行时动态插拔行为修饰包（Modifier Packages），零耦合应对 Boss 转阶段等极特殊情境。
3. **表达式动态决策 (Expression-Driven Evaluation)**
行为的准入条件（PreConditions）与优先级（Score）完全交由多态求值的表达式驱动，基于属性、距离、时效等变量实时演算，保证极高的扩展性。
4. **极致的可调试性 (White-box Debuggability)**
底层虽为无状态扁平化结构，调试端则基于 Tag 呈现清晰的逻辑目录。支持实时可视化每一个节点的算分、冷却与准入状态，并在执行失败时精准回溯拦截原因。

---


## AIBrainComponent

作为持久化组件挂载在 Actor 上，负责存储记忆并持有执行状态机。

```java
class AIBrainComponent {
    Actor self; 
    
    // 动态维护的认知库 (What COULD I do)
    List<AIGoalInstance> activeGoals;
    
    // 传感器更新的缓存黑板
    Actor currentCombatTarget;
    Vector3 lastKnownPosition;  
    
    // 执行状态
    AIBehaviorInstance activeAIBehavior;
    Stack<Ai_modifier_package> modifierPackageStack;
    Int2FloatMap behaviorCooldowns;
}

class AIGoalInstance {
    int goalTagId;          
    Actor associatedActor;  // 如果由 Event 生成，这里保存 Payload 提取出的肇事者
    Vector3 position;
    float magnitude;
    float creationTime;     
}

class AIBehaviorInstance {
    Ai_behavior config;
    AIContext aiCtx;
    AIGoalInstance associatedGoal;   // 触发该行为的目标

    // AIAction 是静态的配置树（数据），而 taskStack 是这棵树在运行时的执行轨迹（状态）。
    Stack<AITaskInstance> taskStack; //是这棵树在运行时的执行轨迹

    Store localStorage;              // 行为局部存储（由 WithLocalVar 等填充）
    boolean isPaused;                // 是否被高优抢占暂停
}

abstract class AITaskInstance<T extends AIAction> {
    // 引用静态数据，如 MoveTo.radius
    T actionCfg; 
    
    abstract void onStart(AIExecContext ctx);
    abstract TaskStatus tick(float deltaTime, AIExecContext ctx);
    abstract void onEnd(AIExecContext ctx, boolean wasAborted);
}

enum TaskStatus { Running, Success, Failed }
```


## AI Archetype (AI 行为原型)

每个需要 AI 功能的实体（如怪物、NPC、宠物）在初始化时，都需要引用一条 `ai_archetype` 数据。它是 AI 系统的最上层配置入口。

```cfg
table ai_archetype[name] {
    name: str; // 如: "Goblin_Melee", "Boss_Dragon"
    description: text;

    // --- 1. 感知与目标生成 (What COULD I do?) ---
    // 实例化该 AI 专属的目标生成器集合
    goalGenerators: list<str> -> ai_goal_generator;

    // --- 2. 行为决策池 (What SHOULD I do?) ---
    // 引入该 AI 能够执行的顶层行为组。
    // 底层引擎在初始化时，会将这些 Group 及其子 Group 完全展开打平。
    behaviorGroupTags: list<str> ->ai_behavior_group;

    // --- 3. 兜底行为 (Fallback/Default) ---
    // 当所有普通行为的 RequiredGoals 或 PreConditions 都不满足时，强制执行的行为。
    defaultBehaviorId: int ->ai_behavior; 
}
```


## AI Behavior

行为是独立的、可单独测试的逻辑单元 。它声明了准入条件、算分公式和最终要执行的动作组合。

```cfg
table ai_behavior[name] (json) {
    [id];
    name: str;
    id: int;
    description: text;

    behaviorTags: list<str> ->gameplaytag;
    isInterruptible: bool; // 执行期间是否允许被更优的常规行为重选打断
    isHighPriority: bool;  // 是否为特权行为 (受击、坠落)，可无视 isInterruptible 直接抢占

    // 准入过滤 (Binary Filter)
    requiredGoals: list<str> ->ai_goal_definition;
    preConditions: list<ThinkCondition>;
    cooldown: TagCooldown; // 行为独立冷却期 
    
    // 动态评分 (Prioritization)
    score: ScoreValue; 

    // 行为执行流 (Actuation)
    action: Action; 
}
```


## Sense

**Goal Generator**:  目标生成器挂载在 AI Archetype 上，负责将外部世界信息（传感器数据、事件）转化为生命周期受控的 Goal 。

`GoalGenerator` 和 `SenseCondition` 的运行时上下文是 (`AIBrainComponent`)

```cfg
table ai_goal_generator[name] (entry="name") {
    name: str;              // 【主键】例: "Gen_Listen_Gunshot", "Gen_Scan_Cover"
    description: text;      // 策划备注
    generator: GoalGenerator; 
}

table ai_goal_definition[name] (json) {
    name: str;
    description: text;
}

interface GoalGenerator {
    // A. 周期扫描型 (如：定期查询空间索引找掩体)
    struct SpatialScan {
        scanType: SenseTargetScan;
        interval: float;

        generatedGoal: str ->ai_goal_definition;
    }

    // B. 事件驱动型 (充当 EventBus 的只读消费者)
    struct OnEvent {
        listenEventTag: str ->event_definition;
        triggerCondition: SenseCondition;

        generatedGoal: str ->ai_goal_definition; 
        extractActor: SenseTargetSelector; 
        duration: float; // Goal 的存活时效 
    }
}

interface SenseCondition {
    struct Const { value: bool; }
}
```


## Think

用于 ai_behavior 的打分和准入判断。
它的运行时上下文是 (`AIBrainComponent`, `AIGoalInstance`)

```cfg
interface ThinkTarget {
    struct Self {}
    struct CandidateGoalActor {}     // 提取假想目标实体
    struct CandidateGoalLocation {}  // 提取假想目标坐标
    struct BlackboardCombatTarget {} 
}

interface ScoreValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: ScoreValue; b: ScoreValue; }
    struct Distance { from: ThinkTarget; to: ThinkTarget; }
    struct TimeSinceCandidateGoalCreated {} // 目标时效衰减
    struct StatValue { target: ThinkTarget; statTag: str ->stat_definition; }
}

interface ThinkCondition {
    struct And { conditions: list<ThinkCondition>; }
    struct IsCooldownReady { behaviorName: str ->ai_behavior; }
    struct TargetHasTags { target: ThinkTarget; tagQuery: GameplayTagQuery; } // 复用 GAS 标签查询
    struct CompareFloat { a: ScoreValue; op: CompareOp; b: ScoreValue; }
}
```



## Act
它的运行时上下文是 ( `AIBehaviorInstance`)


```cfg

// 行为的底层驱动节点
interface Action {
    // --- 基础动作 ---
    struct CastAbility { 
        abilityId: int ->ability; 
        target: AITargetSelector; 
    }
    struct MoveTo {
        target: AITargetSelector;
        speedStat: str ->stat_definition; 
        tolerance: float;   // 到达距离容差
        stopOnFinish: bool; // 到达后是否停止
    }
    struct PlayAnimation {
        animName: str;          // 动画片段名或状态机触发名
        blendInTime: float;     // 融合时间
        waitForCompletion: bool;// 是否等待动画结束才继续执行后续动作
    }
    struct PlayCue {
        cueTag: str ->gameplaytag;  // 引用表现层Cue
        target: AITargetSelector;   // 可选，指定表现作用对象（默认为自身）
    }
    struct Speak {
        dialogueId: int ->dialogue; // 引用对话表
        target: AITargetSelector;   // 对话对象（如玩家）
    }
    struct Interact {
        target: AITargetSelector; // 交互对象
        interactionId: int;       // 可选，指定交互类型（如“打开”、“拾取”）
    }
    struct Wait {
        duration: AIFloatValue;
    }

    // 压入一个行为修饰包 (通常用于永久转阶段，或持续到被主动 Pop)
    struct PushModifierPackage {
        packageId: str ->ai_modifier_package; // 引用配置表
    }
    
    // 弹出一个指定的行为修饰包，能力系统里的behavior里也可以加上这个
    struct PopModifierPackage {
        packageId: str ->ai_modifier_package;
    }

    // 内联的局部作用域修饰包
    // 语义：在这个 Action 及其子节点执行期间，临时应用这个修饰包。执行完毕自动销毁。
    // 用途：比如 Boss 在“蓄力大招”这 5 秒内，临时屏蔽掉所有“受击躲闪”的决策，实现霸体蓄力。
    struct WithModifierPackage {
        packageId: str ->ai_modifier_package;
        action: Action; // 子动作树
    }

    // --- 标签操作 ---
    struct SetTag { 
        tag: str ->gameplaytag; 
        duration: float; 
    }
    struct RemoveTag {
        tag: str ->gameplaytag;
    }
    
    // --- 控制流 ---
    struct Sequence {
        actions: list<Action>;
    }
    struct Parallel {
        actions: list<Action>;
        policy: ParallelPolicy;   // WaitAll / WaitAny
    }
    struct Conditional {
        condition: AICondition;
        thenAction: Action;
        elseAction: list<Action>;
    }
    struct Loop {
        count: AIFloatValue;       // 循环次数，-1 表示无限
        action: Action;
    }

    // --- 作用域控制 ---
    struct WithTarget {
        target: AITargetSelector;    // 重定向后续动作的目标
        action: Action;
    }
    struct WithLocalVar {
        varTag: str ->gameplaytag;
        value: AIFloatValue;         // 将值存入局部变量
        action: Action;
    }
    // --- 引用共享动作（复用）---
    struct ActionRef {
        sharedActionId: int ->shared_ai_action;   // 指向预定义的共享动作
    }
}

interface AICondition {
    struct Const { value: bool; }
}

interface AIFloatValue {
    struct Const { value: float; }
}

enum ParallelPolicy { WaitAll, WaitAny }

// 共享动作表（用于复用常见动作序列）
table shared_ai_action[name] (json) {
    name: str;
    description: text;
    action: Action;
}
```

## AI Behavior Group

组 (Group) 的唯一目的是**共享 `preConditions**`，以减少数据冗余 。底层引擎会将组完全压平，不存在树状控制流 。

```cfg
table ai_behavior_group[name] {
    name: str;
    description: text;
    sharedConditions: list<AICondition>; 
    behaviors: list<str> ->ai_behavior;
    subGroupTags: list<str> ->ai_behavior_group;
}

```

## Dynamic Behavior Modification

基于堆栈的动态行为修饰包 ，允许在运行时通过赋予实体一个 Package，瞬间改变其决策池。极其适合处理 Boss 转阶段或特殊异常状态 。

```cfg
table ai_modifier_package[name] {
    name: str;
    description: text;
    disableBehaviors: GameplayTagQuery;
    injectedBehaviors: list<BehaviorInjection>;
}

// 动态注入新行为到现有的 Group Tag 下
struct BehaviorInjection {
    targetGroupTag: str ->ai_behavior_group; 
    behaviorId: list<int> ->ai_behavior;
}
```

---


## The Selection Pipeline

引擎 Tick 的执行顺序被严格划分为以下阶段：

1. **感知更新 (Sensor Update & Generation)**：
底层遍历 `GoalGenerator` ，或监听 `EventBus`，新增/刷新/清理过期 `AIGoalInstance`。


2. **高优抢占 (High-Priority Evaluation)**：
每帧遍历 `isHighPriority = true` 的行为（如：受击、击飞）。如果有满足条件的行为，且当前行为处于 `isInterruptible = true`，立刻打断并抢占执行 。


3. **行为重选 (Reselection)**：
基于定时器或关键 Goal 变更时触发，**不中断**当前行为进行后台预演 ：

    * **广度过滤**：遍历所有扁平化行为（合并注入与禁用规则），执行 `requiredGoals` 和 `AICondition` 过滤，得出有效候选池 。

    * **动态算分**：对候选池执行 `AIFloatValue` 算分 。

    * **择优切换**：选取分数最高的行为。如果该行为异于当前运行的行为，且当前行为可被打断，则平滑切换。


4. **执行推进 (Actuation)**：
Tick 驱动选出的 `AIAction` 节点，完成能力释放 (`CastAbility`) 或位移逻辑 。必须妥善处理节点返回的 Failed 状态 ，以便下帧触发重选 。



---

## Examples

### 例A：事件驱动的听觉调查

利用全服统一的共享 `Event` 和 `Payload` 实现的零代码扩展机制。

* **技能配置**：玩家开枪武器释放 `Effect.SendEvent` 派发 `Event.Audio.Gunshot`。
* **AI GoalGenerator**：监听该事件，将 `Payload` 中的 `Instigator` 提取出来，在 AI 大脑中生成一个时效为 5 秒的 `AIGoal.Investigate` 。


* **AI Behavior**：名为 "Investigate_Footstep" 的行为设定准入条件为拥有 `AIGoal.Investigate`。其算分公式利用 `AIFloatValue.DistanceToTarget` 确保距离越近得分越高。最终执行 `Action.MoveTo` 走向目标位置。

### 例B：狂暴阶段的动态行为替换

* **状态控制**：Boss 血量低于 30% 时，被挂载一个 `Status`。该 `Status` 的 `Added` 生命周期事件通过代码向 Boss 的 `AIContext` 压入名为 `Package.Enrage` 的行为修饰包 。


* **修饰结果**：`Package.Enrage` 的配置指令将 `disableBehaviorsQuery` 设定为屏蔽 `AIBehavior.Attack.Normal`，从而屏蔽掉普通平A，并将一个巨型 AOE 的 `AIBehavior` 通过 `injectedBehaviors` 动态注入到攻击序列中 。


* **恢复机制**：狂暴 `Status` 结束时，修饰包出栈 ，AI 瞬间恢复初始决策逻辑。
