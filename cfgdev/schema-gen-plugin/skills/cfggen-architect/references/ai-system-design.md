# AI 行为选择系统架构 (AI Behavior Selector)

本文档旨在提供一套与 `GAS` 技能系统无缝对接、高内聚且完全数据驱动的现代 AI 决策架构。它引入了 Bobby Anguelov 在《AI Behavior Selector》中提出的架构核心，摒弃了传统行为树（BT）中结构僵化、难以调试和条件冗余的缺陷，采用**“分而治之 (Divide and Conquer)”** 的扁平化评分机制，致力于打造高扩展性的工业级 AI 管线。

## Philosophy

1. **分而治之 (Divide and Conquer)**：
将 AI 决策严格拆分为两个独立阶段：“我**可以**做什么 (What COULD I do?)” 以及 “我**应该**做什么 (What SHOULD I do?)” 。Goal Generator 负责从世界状态生成纯数据目标 ；Behavior Selector 负责在所有可用目标中挑选最优解 。


2. **目标仅代表可行性 (Availability)**：
目标 (Goal) 的存在仅仅意味着该行为在当前是**可用**的（例如：周围有掩体可用、听到了脚步声）。它**绝不**代表 AI 必须去执行它 。


3. **扁平化与动态评分 (Flat & Dynamic Scoring)**：
彻底摒弃基于树状结构的静态优先级 。所有行为在通过二元准入过滤 (Binary Filter) 后，完全通过动态算分 (Scoring) 决定优先级 。Group 的存在仅仅是为了共享前置条件，不存在任何层级执行结构 。


4. **领域隔离与事件共享 (Domain Isolation & Shared Events)**：
AI 管线的 `AICondition` 与 `AIFloatValue` 在 AST 结构上与战斗管线隔离，确保大脑记忆区 (`AIContext`) 与瞬时结算区 (`Combat Context`) 互不污染（遵循单一职责原则 ）。但底层共享全局 `EventBus` 与只读 `Payload`，确保世界的客观事实统一。


5. **动态插拔修饰 (Dynamic Modification)**：
支持在运行时基于 Tag 路径动态向 AI 大脑压栈或退栈行为修饰包 (Modifier Packages) ，实现零耦合的难度进阶与特殊状态覆写 。



---

## Data Foundation

本章节定义 AI 系统的基础“词汇”。为避免与战斗结算发生空指针与领域污染，AI 拥有自己专属的求值器接口定义。

### AIFloatValue & AICondition

```cfg
// AI 领域专属的动态求值树 (底层求值器严格绑定 AIContext)
interface AIFloatValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: AIFloatValue; b: AIFloatValue; }

    // 空间关系因子 (动态算分核心)
    struct DistanceToTarget { target: AITargetSelector; }
    // 时间衰减因子 (如：新鲜的枪声得分高，随时间流逝得分降低)
    struct TimeSinceGoalCreated { goalTag: str ->ai_goal_definition; }
    
    // 跨域只读桥接：安全提取实体面板属性 (自身或当前仇恨目标)
    struct StatValue { 
        target: AITargetSelector; 
        statTag: str ->stat_definition; 
        capture: StatCaptureMode;
    }
}

interface AICondition {
    struct Const { value: bool; }
    struct And { conditions: list<AICondition>; }
    
    // AI 域专属判定
    struct HasGoal { goalTag: str ->ai_goal_definition; }
    // 自身冷却验证
    struct IsCooldownReady { behaviorId: int ->ai_behavior; }
    // 复用 GAS 系统的 Tag 查询
    struct TargetHasTags { target: AITargetSelector; tagQuery: GameplayTagQuery; }
    
    struct CompareFloat { a: AIFloatValue; op: CompareOp; b: AIFloatValue; }
}

// AI 专属目标选择器 (用于在黑板中精准索敌)
interface AITargetSelector {
    struct Self {}
    struct CurrentCombatTarget {} // 黑板中的主仇恨目标
    struct GoalAssociatedActor { 
        goalTag: str ->ai_goal_definition; // 提取引发该目标的实体 (如声源、掩体)
    }
}

```

### AI Goal Definition

定义 AI 能够产生的“欲望”或“认知”类型。

```cfg
table ai_goal_definition[goalTag] (entry="goalTag") {
    goalTag: str ->gameplaytag; // 强类型约束，例: "AIGoal.Combat", "AIGoal.UseCover", "AIGoal.Investigate"
    description: text;
}

```

---

## Core Entities

本章节定义构成 AI 逻辑管线的主体实体：从感知信息 (Generator) 到行为执行 (Behavior)。

### AI Archetype (AI 行为原型)

每个需要 AI 功能的实体（如怪物、NPC、宠物）在初始化时，都需要引用一条 `ai_archetype` 数据。它是 AI 系统的最上层配置入口。

```cfg
table ai_archetype[id] (json) {
    id: int;
    name: str; // 如: "Goblin_Melee", "Boss_Dragon"
    description: text;

    // --- 1. 感知与目标生成 (What COULD I do?) ---
    // 实例化该 AI 专属的目标生成器集合
    goalGenerators: list<GoalGenerator>;

    // --- 2. 行为决策池 (What SHOULD I do?) ---
    // 引入该 AI 能够执行的顶层行为组。
    // 底层引擎在初始化时，会将这些 Group 及其子 Group 完全展开打平。
    behaviorGroupTags: list<str> ->gameplaytag;

    // --- 3. 兜底行为 (Fallback/Default) ---
    // 当所有普通行为的 RequiredGoals 或 PreConditions 都不满足时，强制执行的行为。
    defaultBehaviorId: int ->ai_behavior; 
}

```

### Goal Generator

目标生成器挂载在 AI Archetype 上，负责将外部世界信息（传感器数据、事件）转化为生命周期受控的 Goal 。

```cfg
interface GoalGenerator {
    // A. 周期扫描型 (如：定期查询空间索引找掩体)
    struct SpatialScan {
        generatedGoalTag: str ->ai_goal_definition;
        scanType: AITargetScan;
        interval: float;
    }

    // B. 事件驱动型 (充当 EventBus 的只读消费者)
    struct OnEvent {
        listenEventTag: str ->event_definition; // 如 "Event.Audio.Gunshot"
        
        // 提取 Payload 中的肇事者或承受者，作为该 Goal 绑定的关联实体
        extractAssociatedActorFrom: PayloadRole; 
        
        generatedGoalTag: str ->ai_goal_definition; // 如 "AIGoal.Investigate"
        duration: float; // Goal 的存活时效 
    }
}

enum PayloadRole { Instigator; Target; }

```

### AI Behavior

行为是独立的、可单独测试的逻辑单元 。它声明了准入条件、算分公式和最终要执行的动作组合。

```cfg
table ai_behavior[id] (json) {
    id: int;
    name: str;
    
    // 行为特征标签，例: ["AIBehavior.Type.Attack", "AIBehavior.Element.Fire"]
    behaviorTags: list<str> ->gameplaytag;
    
    // --- 1. 准入过滤 (Binary Filter) ---
    requiredGoals: list<str> ->ai_goal_definition;
    preConditions: list<AICondition>;
    cooldown: TagCooldown; // 行为独立冷却期 
    
    // --- 2. 动态评分 (Prioritization) ---
    score: AIFloatValue; 

    // --- 3. 行为执行流 (Actuation) --- 
    action: AIAction; 
    
    // --- 4. 控制流标识 ---
    isInterruptible: bool; // 执行期间是否允许被更优的常规行为重选打断
    isHighPriority: bool;  // 是否为特权行为 (受击、坠落)，可无视 isInterruptible 直接抢占
}

// 行为的底层驱动节点
interface AIAction {
    struct CastAbility { 
        abilityId: int ->ability; 
        target: AITargetSelector; 
    }
    struct MoveTo {
        target: AITargetSelector;
        speedStat: str ->stat_definition; 
    }
    struct PlayAnimation {
        animName: str;          // 动画片段名或状态机触发名
        blendInTime: float;     // 融合时间
        waitForCompletion: bool;// 是否等待动画结束才继续执行后续动作
    }
    struct PlayCue {
        cueTag: str ->gameplaytag;   // 引用表现层Cue
        target: AITargetSelector;    // 可选，指定表现作用对象（默认为自身）
    }
    struct Interact {
        target: AITargetSelector;  // 交互对象
        interactionId: int?;       // 可选，指定交互类型（如“打开”、“拾取”）
    }
    struct Speak {
        dialogueId: int;           // 引用对话表
        target: AITargetSelector;  // 对话对象（如玩家）
    }

    struct SetTag { 
        tag: str ->gameplaytag; 
        duration: float; 
    }
    struct RemoveTag {
        tag: str ->gameplaytag;
    }
    
    struct Conditional {
        condition: AICondition;
        thenAction: AIAction;
        elseAction: list<AIAction>;
    }
    struct Loop {
        count: int;
        action: AIAction;
    }
    struct Sequence { 
        actions: list<AIAction>; 
    }
    struct Parallel {
        actions: list<AIAction>;
        policy: ParallelPolicy;   // WaitAll（等待所有完成）或 WaitAny（任一完成即成功）
    }
}

```

### AI Behavior Group

组 (Group) 的唯一目的是**共享 `preConditions**`，以减少数据冗余 。底层引擎会将组完全压平，不存在树状控制流 。

```cfg
table ai_behavior_group[groupTag] (entry="groupTag") {
    groupTag: str ->gameplaytag; // 例: "AIGroup.Combat.Attack"
    sharedConditions: list<AICondition>; 
    behaviors: list<int> ->ai_behavior;
    subGroupTags: list<str> ->gameplaytag; // 嵌套子组也用 Tag 强引用
}

```

### Dynamic Behavior Modification

基于堆栈的动态行为修饰包 ，允许在运行时通过赋予实体一个 Package，瞬间改变其决策池。极其适合处理 Boss 转阶段或特殊异常状态 。

```cfg
table ai_modifier_package[id] {
    id: int;
    name: str; 

    // 【核心强化】复用 GAS 系统的 Tag 查询能力
    // 例如配置屏蔽所有包含 "AIBehavior.Attack" 的行为，实现精准的分类禁制
    disableBehaviorsQuery: GameplayTagQuery;

    // 动态注入新行为到现有的 Group Tag 下
    injectedBehaviors: list<BehaviorInjection>;
}

struct BehaviorInjection {
    targetGroupTag: str ->gameplaytag; 
    behaviorId: int ->ai_behavior;
}

```

---

## Runtime Mechanics

本章节定义 AI 实体在运行时的状态载体与决策循环流转机制。

### AIContext

AIContext 是持久化的宏观记忆体，与战斗系统瞬间生灭的 `Context` 彻底隔离。

```java
class AIContext {
    Actor self; 
    
    // 动态维护的认知库 (What COULD I do)
    Int2ObjectMap<AIGoalInstance> activeGoals; 
    
    // 传感器更新的缓存黑板
    Actor currentCombatTarget;  
    Vector3 lastKnownPosition;  
    
    // 执行状态
    AIBehaviorInstance activeAIBehavior;
    Stack<Integer> modifierPackageStack; // 修饰包堆栈
    Object2FloatMap<Integer> behaviorCooldowns; 
}

class AIGoalInstance {
    int goalTagId;          
    Actor associatedActor;  // 如果由 Event 生成，这里保存 Payload 提取出的肇事者
    Vector3 position;       
    float creationTime;     
}

```

### The Selection Pipeline

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


