# AI 行为系统设计 (AI Behavior System Design)

本文档定义了一套与 `能力系统设计` 无缝集成的现代 AI 行为系统设计。该设计以 高内聚、数据驱动 为核心，汲取 Bobby Anguelov 在《AI Behavior Selector》中提出的先进理念，通过 扁平化评分机制 替代传统行为树的僵化结构，彻底解决调试困难与条件冗余问题，为构建高扩展性的工业级 AI 管线奠定基础。

## Philosophy

1. **三段式分治架构 (Sense - Think - Act)**
将 AI 管线严格拆分为三个解耦的独立阶段：
    * **目标生成**（我可以做什么？）：处理传感器与事件输入，生成纯数据化的可用目标。
    * **行为抉择**（我应该做什么？）：通过表达式动态算分，从候选池中选出当前优先级最高的行为。
    * **行为执行**（我具体怎么做？）：驱动底层的动作序列（位移、动画、施法），妥善管理节点的运行生命周期、成功/失败状态与高优抢占打断。

2. **数据驱动与动态组合 (Data-Driven & Composition)**
摈弃硬编码。所有基础行为均通过配置组合构建，并支持在运行时动态插拔行为修饰，零耦合应对 Boss 转阶段等极特殊情境。
3. **表达式动态决策 (Expression-Driven Evaluation)**
行为的准入条件（PreConditions）与优先级（Score）完全交由多态求值的表达式驱动，基于属性、距离、时效等变量实时演算，保证极高的扩展性。
4. **极致的可调试性 (White-box Debuggability)**
底层虽为无状态扁平化结构，调试端则基于 Group 呈现清晰的逻辑目录。支持实时可视化每一个节点的算分、冷却与准入状态，并在执行失败时精准回溯拦截原因。

---

## Runtime Core

本章节定义 AI 系统运行时的核心数据结构与组件。这些实例化对象在 AI 生命周期中动态创建、更新与销毁，共同支撑感知、决策与执行的完整流程。

- **AIBrainComponent**：作为 AI 的大脑，挂载在 Actor 上，负责维护 AI 的运行时状态。它持有当前活跃的目标列表（activeGoals）、战斗目标与最后已知位置等感知信息，以及正在执行的行为实例（activeBehavior）。此外，它管理行为修饰（behaviorModifiers）用于动态修改决策池，并记录各行为的冷却时间（behaviorCooldowns）。该组件是 AI 系统的中枢，驱动感知更新、行为重选与执行推进。

- **AIGoalInstance**：代表 AI 当前感知到的潜在交互目标（如可疑位置、可攻击的敌人、可互动的物件）。它是“感知阶段”的产物，由 `GoalGenerator` 根据传感器数据或事件生成，包含目标类型标签、关联实体、位置坐标、强度值及创建时间等信息，作为后续行为决策的候选输入。

- **AIBehaviorInstance**：是 AI 行为在运行时的具体实例，封装了静态配置（`ai_behavior`）与动态执行状态。它关联触发该行为的目标（`associatedGoal`），维护一个任务栈（`taskStack`）以推进执行流程，并提供局部存储（`localStorage`）供行为内各任务共享数据。同时，它记录了行为的暂停状态，以支持高优抢占机制。

- **AITaskInstance**：是行为树中具体任务节点的运行时实例，对应静态配置中的 AITask。每个任务实例持有其静态配置（taskCfg），并实现生命周期方法：任务实例由行为实例的任务栈（taskStack）管理，通过栈顶驱动执行。

```java
class AIBrainComponent {
    Actor self; 
    
    // 动态维护的认知库 (What COULD I do)
    List<AIGoalInstance> activeGoals;
    Actor currentCombatTarget;
    Vector3 lastKnownPosition;  
    
    // 执行状态
    AIBehaviorInstance activeBehavior;
    List<AIBehaviorModifierRef> behaviorModifiers;
    Int2FloatMap behaviorCooldowns;
}

class AIGoalInstance {
    Ai_goal_definition goalCfg;
    Actor associatedActor;  // 如果由 Event 生成，这里保存 Payload 提取出的肇事者
    Vector3 position;
    float magnitude;
    float creationTime;
}

class AIBehaviorInstance {
    Ai_behavior behaviorCfg;
    AIBrainComponent aiBrain;

    // Key: Goal 定义的 ID (如 "Goal.Item.Barrel")
    // Value: 经过算分选出的那一个具体的 Goal 实例
    Int2ObjectMap<AIGoalInstance> boundGoals;
    
    // AITask 是静态的配置树（数据），而 taskStack 是这棵树在运行时的执行轨迹（状态）。
    Stack<AITaskInstance> taskStack; //是这棵树在运行时的执行轨迹

    Store localStorage;              // 行为局部存储（由 WithLocalVar 等填充）
}

abstract class AITaskInstance<T extends AITask> {
    T taskCfg; 
    
    abstract void onStart(AIBehaviorInstance ctx);
    abstract TaskStatus tick(AIBehaviorInstance ctx, float deltaTime);
    abstract void onEnd(AIBehaviorInstance ctx, boolean wasAborted);
}

class AIBehaviorModifierRef {
    Ai_behavior_modifier modifierCfg;
    int refCount;
}

enum TaskStatus { Running, Success, Failed }
```

---

## 配置定义

### global_ai_settings
全局 AI 设置表，作为单例在引擎初始化时加载。它定义了全游戏所有 AI 必须遵守的“最高物理法则”（如：死亡或被硬控时强制瘫痪），彻底消灭了各实体间的规则冗余。

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
    name: str; // 如: "Goblin_Melee", "Boss_Dragon"
    description: text;

    //  感知与目标生成 (What COULD I do?)
    goalGenerators: list<str> -> ai_goal_generator;

    // 行为决策池 (What SHOULD I do?) 
    behaviorGroups: list<str> ->ai_behavior_group;

    // 兜底行为 (Fallback/Default)
    defaultBehavior: str ->ai_behavior; 
}
```


### ai_behavior

行为是独立的、可单独测试的逻辑单元 。它声明了准入条件、算分公式和最终要执行的动作组合。

```cfg
table ai_behavior[name] (json) {
    [behaviorId];
    name: str;
    behaviorId: int;
    description: text;

    behaviorTags: list<str> ->gameplaytag;
    // 常规行为在执行期间，是否允许被更高分的【常规行为】平滑重选打断
    isInterruptible: bool; 
    // 抢占优先级
    // 0 表示常规行为（走普通的 Score 算分重选流程）。
    // >0 表示特权行为，数值越大优先级越高（如：死亡=100, 坠落/击飞=80, 重受击=50, 轻受击=20）。
    // 优先判断优先级，优先级相同时判断score
    interruptPriority: int;

    // 最小承诺时间。在这个时间内，哪怕 isInterruptible 为 true，
    // 且其他行为分数更高，也强制拒绝被打断（除非是 isHighPriority 的抢占）。
    minCommitmentTime: float;

    // 准入过滤 (Binary Filter)
    requiredGoals: list<str> ->ai_goal_definition;
    preConditions: list<PreCondition>; // And语义
    cooldown: float; // 行为独立冷却期
    
    // 动态评分 (Prioritization)
    score: ScoreValue; 

    // 行为执行 (Actuation)
    task: AITask; 
    // 中止条件, 只要有任意一个返回 True (OR)，立刻清空 Task 栈并触发重选
    abortConditions:list<AbortCondition>;
}
```

### ai_behavior_group

组 (Group) 的唯一目的是**共享 `preConditions**`，以减少配置冗余 。

```cfg
table ai_behavior_group[name] {
    name: str;
    description: text;
    sharedConditions: list<AICondition>; 
    behaviors: list<str> ->ai_behavior;
    subGroups: list<str> ->ai_behavior_group;
}
```

--- 

## 感知阶段（GoalGenerator）

`GoalGenerator`挂载在 AI Archetype 上，负责将外部世界信息（传感器数据、事件）转化为 Goal 。
运行时上下文是 (`AIBrainComponent`)，但不能访问`AIBrainComponent`的`activeGoals`

```cfg
table ai_goal_generator[name] (json) {
    name: str;              // 【主键】例: "Gen_Listen_Gunshot", "Gen_Scan_Cover"
    description: text;      // 策划备注
    generator: GoalGenerator; 
}

table ai_goal_definition[name] {
    [goalId];
    name: str;
    goalId: int;
    description: text;

    // 存活校验条件 (And 语义)
    // 每帧感知更新时，如果此条件组返回 false，大脑立即将该 Goal 判定为失效并剔除。
    // 实现上可以考虑用事件驱动，更高效
    validConditions: list<GoalValidator>;
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

interface GoalValidator {
    struct TargetIsValid { } // 底层校验 associatedActor 是否已被销毁 (isPendingKill)
    struct TargetHasTags {
        requirements: GameplayTagQuery; // 比如要求掩体不能拥有 "State.Destroyed" 标签
    }
    struct DistanceLessThan { // 距离约束 (防风筝机制)
        maxDistance: float;   // 目标如果跑出 100 米，自动放弃该 Goal
    }
    struct HasLineOfSight { } // 视线校验
}

interface SenseCondition {
    struct Const { value: bool; }
    // ...
}
```


## 思考阶段（PreCondition & ScoreValue）

`PreCondition`、`ScoreValue`用于 ai_behavior 的准入判断和打分。
运行时上下文是 (`AIBrainComponent`, `Ai_behavior`)

```cfg
interface ScoreValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: ScoreValue; b: ScoreValue; }
    struct Distance { 
        from: ThinkLocation; 
        to: ThinkLocation; 
    }
    struct GoalAge {
        goal: str ->ai_goal_definition;
    }
    struct StatValue {
        target: ThinkActor;
        statTag: str ->stat_definition; 
    }
    struct IsActiveBehavior {
        trueValue: float;            // 如果正在执行，返回这个额外加分（如 15.0）
        falseValue: float;           // 否则返回 0
    }
}

interface PreCondition {
    struct And { conditions: list<PreCondition>; }
    struct TargetHasTags { 
        target: ThinkActor; 
        tagQuery: GameplayTagQuery; 
    }
    struct CanActivateAbility {
        abilityId: int ->ability; // 只查自身状态
    }
    struct Compare { 
        a: ScoreValue; 
        op: CompareOp; 
        b: ScoreValue; 
    }
}

interface ThinkActor {
    struct Self {}
    struct CurrentCombatTarget {}
    struct GoalActor {
        goal: str ->ai_goal_definition;
    }
}

interface ThinkLocation {
    struct Self {}
    struct CurrentCombatTarget {}
    struct GoalActor {
        goal: str ->ai_goal_definition;
    }
    struct GoalLocation {
        goal: str ->ai_goal_definition;
    }
}
```

## 行动阶段（AITask）

运行时上下文是 (`AIBehaviorInstance`)

`AITask`是行为执行的逻辑单元，负责将 `ai_behavior` 转化为具体的动作序列。底层执行并不采用传统的树状递归遍历，而是基于**任务栈（taskStack）** 推进：复合节点（如 Sequence、Conditional）负责将子任务压入栈顶，引擎 Tick 永远只执行当前栈顶的活跃节点（Parallel 节点驻留在栈顶时，内部维护一组并行的子任务实例），极大提升了执行性能与状态管理的清晰度。

为解耦“微观控制流”与“宏观物理法则”，`AITask` 摒弃了对全局状态的持续校验，转而与 `ai_behavior` 采用 **双轨制打断协同**：

1. **外部宏观掐断 (Abort)**：Tick 驱动前，大脑优先校验 `abortConditions`。一旦环境突变（如目标死亡、距离过远、自身受控），大脑即刻强制清空任务栈，广播 `onEnd(wasAborted = true)` 清理底层状态，并触发紧急重选。
2. **内部主动冒泡 (Failed)**：当微观执行受阻（如寻路失败、被 GAS 拦截），或命中配置的 `ReturnFailed` 卫语句时，`AITask` 主动向上冒泡 `TaskStatus.Failed` 正常出栈，交由大脑触发微冷却惩罚后平滑重选。


```cfg
// 行为的底层驱动节点
interface AITask {
    // --- 基础动作 ---
    struct CastAbility { 
        abilityId: int ->ability; 
        target: AITargetSelector; 
    }
    struct DoEffect {
        effect: Effect;
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

    
    // --- 控制流 ---
    struct Sequence {
        tasks: list<AITask>;
    }
    struct Parallel {
        tasks: list<AITask>;
        policy: ParallelPolicy;   // WaitAll / WaitAny
    }
    struct Conditional {
        condition: AICondition;
        thenTask: AITask;
        elseTaskSeq: list<AITask>;
    }
    struct Loop {
        count: AIFloatValue;       // 循环次数，-1 表示无限
        task: AITask;
    }
    struct ReturnFailed {}
    struct ReturnSuccess {}

    // --- 作用域控制 ---
    struct WithLocalVar {
        varTag: str ->gameplaytag;
        value: AIFloatValue;         // 将值存入局部变量
        task: AITask;
    }

    // --- 引用共享 ---
    struct TaskRef {
        sharedTask: str ->shared_ai_task;
    }
}

interface AbortCondition {
    struct TargetIsDead { 
        target: AITargetSelector; 
    }
    struct DistanceGreaterThan {
        from: AITargetSelector;
        to: AITargetSelector;
        maxDistance: float; // 比如超过 15 米就放弃追砍
    }
    struct SelfHasAnyTags {
        tags: list<str> ->gameplaytag; // 比如 ["State.Debuff.Stun", "State.Debuff.Freeze"]
    }
}

interface AICondition {
    struct Const { value: bool; }
}

interface AIFloatValue {
    struct Const { value: float; }
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
```


## 动态行为修饰

允许在运行时通过赋予实体一个`ai_behavior_modifier`，瞬间改变其决策池。极其适合处理 Boss 转阶段或特殊异常状态 。

```cfg
table ai_behavior_modifier[name] {
    name: str;
    description: text;
    disableBehaviorsQuery: GameplayTagQuery;
    injectedBehaviors: list<BehaviorInjection>;
}

// 动态注入新行为到现有的 Group Tag 下
struct BehaviorInjection {
    targetGroup: str ->ai_behavior_group; 
    behaviors: list<str> ->ai_behavior;
}
```

在 `ability-design` 的 `Behavior` 接口中扩展如下结构：

```cfg
    struct AIBehaviorModifier {
        modifier: str ->ai_behavior_modifier;
    }
```
**设计考量**：
将宏观的 AI 行为修饰权交由 GAS 的 Status 体系接管，使得 ai_behavior_modifier 的挂载与卸载完全依附于标准的上下文生命周期。这种“作用域即生命周期”的闭环设计，从根本上杜绝了状态残留（Orphaned State）问题，这也是我们在 AITask 中不引入 Add/RemoveAIBehaviorModifier 节点的原因。

---


## 决策流程

引擎 Tick 的执行顺序被严格划分为以下五个阶段，确保底层物理法则的绝对控制权与战术决策的平滑流转：

**1. 全局法则仲裁 (Global Override)**
**Tick 的第一道绝对防线。** 优先校验单例 `global_ai_settings` 中的全局硬控法则（如宿主是否带有“眩晕”或“死亡”标签）。

* 若命中，大脑立刻强制清空任务栈，并**直接挂起本帧后续所有 AI 逻辑 (Return)**。此时 AI 物理宕机，躯体表现全盘交由 GAS 接管。

**2. 局部失效拦截 (Local Abort)**
若大脑未宕机，校验当前活跃行为 (`activeBehavior`) 的 `abortConditions`（如目标超出追击范围）。

* 若命中，立即清空任务栈并将 `activeBehavior` 置空。

**3. 感知更新 (Perception Update)**
底层驱动 `GoalGenerator` 或消费 `EventBus` 事件以刷新候选目标（Goal）。执行 `GoalValidator` 存活校验，实时剔除物理上已不合法（如已死亡、被销毁）的 `AIGoalInstance`，维持大脑认知库的纯洁性。

**4. 决策与抢占 (Decision & Preemption)**
提取所有通过 `requiredGoals` 和 `preConditions` 准入校验的候选行为，将其划分为**特权队列 (`interruptPriority > 0`)** 与 **常规队列 (`interruptPriority == 0`)**，并执行两段式严格仲裁：

* **Step A: 特权绝对仲裁 (Priority First)**
若特权队列非空，提取 `interruptPriority` 最高的一组行为。
* **跨级碾压**：若最高优先级严格大于当前行为优先级，立刻触发抢占，清空旧任务栈。
* **同级择优**：若存在多个候选行为共享该最高优先级（或与当前行为同级），则计算它们的 `Score`，分高者胜出。若当前行为参与竞标，自动叠加 `scoreInertia`（惯性加分）以防抖。


* **Step B: 常规算分重选 (Score Second)**
若特权未触发抢占，且当前行为允许被打断（`isInterruptible == true` 且执行时长已度过 `minCommitmentTime`），系统对常规队列进行广度算分。当前行为在算分时自动叠加 `scoreInertia`，其他候选行为的最终得分必须**严格大于 (>)** 当前行为的加持后总分，才能成功夺取控制权并重置执行栈。

**5. 执行推进 (Actuation)**
确立合法的 `activeBehavior` 后，纯粹地调用其内部任务栈 (`taskStack`) 栈顶的 `AITask` 节点进行 Tick 驱动：
* 返回 **Running**：维持现状，推进进度。
* 返回 **Success**：栈顶任务出栈，下一帧驱动新栈顶。
* 返回 **Failed**：主动冒泡，彻底清空任务栈，触发该行为的独立冷却 (Cooldown) 惩罚，交由下一帧重新进行广度决策。

---

## 协同架构：打断与控制流派

本 AI 系统与底层的 GAS（Gameplay Ability System）通过配置实现无缝协同。我们坚持“单一事实来源（Single Source of Truth）”原则，将打断与控制严格划分为以下三种流派混用，确保逻辑与表现的极致解耦：

| 场景分类 | 驱动源头 | 决策者 | 底层机制 | 适用案例 |
| :--- | :--- | :--- | :--- | :--- |
| **物理受控 (Hard CC)** | 外部伤害 / 机制打击 (GAS) | GAS (Tag 系统) | `global_ai_settings` 触发全局拦截，强制清空执行栈并**直接挂起大脑的 Tick 循环 (物理宕机)**。| 死亡、冰冻、眩晕、击飞浮空。此时 AI 大脑停止一切思考与动作，躯体表现全盘交由 GAS 的 Cue 驱动。 |
| **战术中断 (Soft Interrupt)** | 内部感知判断 (AI 大脑) | AI 算分系统 | 触发局部 `abortConditions` 触发本帧顺滑重选，或利用 `interruptPriority` 在 AI 内部进行高优行为抢占。 | 目标跑远放弃追击、紧急闪避、发现更残血目标切流。GAS 毫不知情，纯属 AI 主观战术意图。 |
| **转阶段/机制锁 (Phase)** | 内部状态感知 (AI) -> 状态持久化 (GAS) | AI 算分系统主导决策，GAS 负责状态托管 | AI 通过特权行为（高 `interruptPriority` ）主动执行转阶段前摇，结束后呼叫 GAS 给自身挂载长效 `Status`。由该 Status 内部的 `AIBehaviorModifier` 动态修改后续行为池。 | Boss 30%血量狂暴。AI 寻机主动释放霸体怒吼，结束后由 GAS 接管狂暴状态，原子化地注入全屏大招并替换平 A，状态随战斗重置安全卸载。 |

---

## Examples: 协同架构实战

**例：Boss 狂暴转阶段（AI 主动决策 + GAS 状态托管）**

为避免底层被动扣血瞬间触发转阶段导致的“表现突变”（如滞空时强行切断当前动画），系统将“转阶段”设计为 AI 主动寻机的战术动作，并彻底贯彻“单一事实来源”原则，将长效机制交由 GAS 统一接管。

* **1. 战术决策 (The Decision)**：
配置特权行为 `Boss_Enter_Phase2`（设定极高的 `interruptPriority` 以跨级抢占）。其 `preConditions` 实时查询血量（如 `StatValue("Stat.HP.Percent") <= 0.3`），并校验自身不包含 `"State.Phase.Enrage"` 标签（防重复触发）。
* **2. 物理免控与演出 (Empowerment & Performance)**：
行为激活后，任务栈推进 `Sequence` 复合节点。为确保转阶段演出绝对安全，首个节点通过 `DoEffect` 为自身挂载临时的“霸体” Status（利用 GAS 底层的 `blockTags` 机制，物理拦截所有 `State.Debuff.Control` 标签，而非在 AI 层开后门）。随后推进 `PlayAnimation`，播放极具压迫感的“狂暴怒吼”动作。
* **3. 状态移交 (The Handover)**：
演出结束，任务栈推进至下一节点，通过 `DoEffect` 为自身正式挂载名为 `Status_Enrage` 的长效 GAS 状态。至此，AI 的转阶段“战术动作”执行完毕，系统机制控制权全盘移交 GAS。
* **4. 机制托管与闭环 (Persistence & Resolution)**：
`Status_Enrage` 存在期间，作为唯一数据源向下驱动所有狂暴逻辑：
* **数值与表现**：通过内部的 `StatModifier` 翻倍攻击力，通过 `cuesWhileActive` 循环播放周身煞气与红光特效。
* **决策池覆写**：通过关联的 `AIBehaviorModifier` 动态屏蔽普通平 A，并将巨型全屏 AOE 行为注入当前决策池。同时赋予宿主 `"State.Phase.Enrage"` 标签。
* **安全卸载**：若战斗重置或触发特殊机制导致该 Status 被销毁，依附其上的数值 Buff、表现 Cue 及 AI 行为修饰包将**原子化出栈卸载**。AI 瞬间恢复一阶段初始逻辑，从根本上杜绝状态残留导致的 Bug。