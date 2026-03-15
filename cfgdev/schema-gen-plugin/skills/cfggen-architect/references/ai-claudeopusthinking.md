

# 重新评估：作为架构参考文档的设计质量

---

## 一、核心架构判断

### 做对的事

**Sense-Think-Act 三段管线**是正确的工业共识，你的实现没有走偏。

**扁平评分替代行为树控制流**这个决策非常关键且正确。行为树最大的工程问题不是性能，而是：条件重复散布在不同层级、调试时无法一眼看出"为什么选了这个行为"、新增行为必须找到树里的正确插入位置。你用 Group 压平 + Score 排序，从根上消除了这些问题。

**双轨打断（外部 Abort vs 内部 Failed）** 是这份文档最精彩的架构设计之一。它清晰地回答了一个困扰大量 AI 系统的问题："谁有权终止当前行为？"。你的回答是：
- 环境突变 → 外部强制掐断
- 执行受阻 → 内部主动上报

这比传统行为树的"Decorator 条件 abort"干净得多。

**AI BehaviorModifier 生命周期绑定 GAS Status** 这个决策极具远见。它用"作用域即生命周期"一句话解决了状态残留这个最常见的 AI bug 类别。

---

## 二、架构层面的真正问题

### 问题1：条件/表达式系统的分裂是架构缺陷，不是细节问题

你现在有四套并行的判断体系：

| 阶段 | 条件类型 | 数值类型 |
|------|----------|----------|
| 感知 | `SenseCondition` | — |
| 决策 | `PreCondition` | `ScoreValue` |
| 执行 | `AICondition` | `AIFloatValue` |
| 中止 | `AbortCondition` | — |

这不是"各游戏按需增删字段"能解决的问题——它反映的是**架构层对"上下文"这个概念缺乏统一建模**。

核心矛盾在于：这四套系统做的事本质相同（在某个上下文里求值一个 bool 或 float），但你为每个阶段单独造了一套语言。当某个具体游戏的策划问"我能不能在 AbortCondition 里检查属性值？"，答案只能是"不行，那个接口没这个能力"——但这不是游戏需求不合理，而是架构没统一。

**建议**：文档应当传达一个明确的架构原则——**AI 系统内部应只有一套 Bool 表达式和一套 Float 表达式**。不同阶段的差异不在表达式语法上，而在**可用的上下文变量**上。具体地：

```
统一表达式引擎
├── AIBoolExpr   (一套语法)
└── AIFloatExpr  (一套语法)

不同阶段提供不同的"变量供应商"：
├── 感知阶段：可访问 Event Payload、空间查询结果
├── 决策阶段：可访问 Brain、候选 Goal 绑定、Stat
├── 执行阶段：可访问 BehaviorInstance、localStorage
└── 中止阶段：可访问 Brain、当前 Goal 绑定、Stat
```

这样做的好处是：每个具体游戏只需要扩展一套表达式的节点类型，而不是维护四套。

---

### 问题2：Goal 绑定与行为评估的关系在概念层未闭合

这不是"缺个字段"的问题，而是文档在阐述决策流程时跳过了一个关键的概念环节。

你的决策流程写的是：

> 提取所有通过 `requiredGoals` 和 `preConditions` 准入校验的候选行为

但这里隐含了一个未说清的前提：**一个行为的 Score 往往依赖于它将要绑定的那个 Goal 实例**。

举例：
- "追击"行为的 Score 取决于"与目标敌人的距离"
- "找掩体"行为的 Score 取决于"最近掩体的质量"
- 同一个"攻击"行为，面对不同敌人 Goal 实例，分数可能完全不同

也就是说，评分不是 `f(Brain, Behavior)`，而是 `f(Brain, Behavior, GoalBinding)`。

文档目前没有把这个概念说清楚。这会导致不同团队在落地时做出不一致的理解：
- 有人会先选 Goal 再评分
- 有人会先评分再绑 Goal
- 有人会为每个 Goal 实例生成一个行为候选

**建议**：在架构层明确一个概念——**行为候选（BehaviorCandidate）**——它是 `(Behavior配置, Goal绑定方案)` 的组合体。决策阶段评估的不是行为本身，而是行为候选。这个概念不需要具体定义什么字段，但需要在文档的 Philosophy 或决策流程中明确阐述，否则后续实现必然分歧。

---

### 问题3：暂停/恢复机制与整体架构哲学矛盾

你的文档在 Runtime Core 里引入了 `isPaused`，在协同表里提到了"暂停"。但这与你整体的设计哲学存在张力：

你的系统最大的优势之一，是行为之间**没有复杂的状态依赖关系**——每个行为是独立的、可独立测试的逻辑单元。一旦引入通用的暂停/恢复，就意味着：
- 每种 Task 都需要考虑"被暂停后恢复时状态是否还合法"
- 恢复时 Goal 可能已经失效
- 恢复时世界状态可能已经完全不同
- 测试复杂度指数级增长

这会严重损害你"独立、可测试、扁平"的架构优势。

**建议**：在架构原则中明确表态——**默认策略是 Abort-and-Reselect，不支持通用暂停恢复**。如果特定游戏确实需要"行为挂起"（如 Boss 被打断后接续之前的行为），应作为该游戏的特化扩展来处理，而不是作为基础架构的默认能力。从 Runtime Core 中移除 `isPaused`，避免给实现者错误的暗示。

---

### 问题4：文档没有阐明"算分系统的已知陷阱与应对策略"

作为一份架构参考文档，你选择了 Utility AI 的评分路线，但没有讨论这条路线的**已知工程陷阱**。任何基于此文档做具体游戏的团队都会遇到这些问题，文档应该提前给出指导：

**陷阱1：分数不可比性**
"追击"打 80 分和"喝药"打 80 分，这两个 80 分有可比性吗？如果所有行为的分数在同一个无量纲空间里竞争，策划很快会失去对数值的控制。

**陷阱2：分数调参地狱**
新增一个行为后，为了不打乱现有平衡，需要反复调整多个行为的分数参数。行为越多，参数空间越不可控。

**建议**：在 Philosophy 中增加一段"评分设计指导"，至少提及以下策略供各项目参考：
- **归一化**：所有 ScoreValue 最终映射到 [0, 1] 区间
- **响应曲线**：用曲线而非线性公式映射输入到分数，这是 Dave Mark（GDC Utility AI 系列）反复强调的核心工具
- **分数语义约定**：建议团队建立分数含义的契约（如 0.8+ = 紧急, 0.5 = 常规, 0.2 = 闲置填充）
- **惯性与滞回**：你已经有 `scoreInertia` 和 `minCommitmentTime`，但应在 Philosophy 层解释"为什么需要它们"

---

### 问题5：感知阶段（GoalGenerator）的抽象层级偏低

你目前的 GoalGenerator 只有两种：`SpatialScan` 和 `OnEvent`。作为参考架构，这个抽象层级过于具体化了——它更像是两个具体实现，而非一个可扩展的概念框架。

实际游戏中的感知来源远不止这两种：
- 队友请求支援（社交信号）
- 自身血量低于阈值（内省）
- 场景脚本注入（导演系统）
- 仇恨表变化（RPG 特有）

**建议**：GoalGenerator 在概念层应该更抽象。你的 Philosophy 里写的是"处理传感器与事件输入，生成纯数据化的可用目标"，但 schema 没有贯彻这个抽象度。建议将 `SpatialScan` 和 `OnEvent` 定位为两个**示例实现**，而非穷举。或者重新审视 GoalGenerator 本身是否应该是一个更开放的接口，只定义"输入什么、输出什么"的契约，而不是枚举所有可能的感知类型。

---

### 问题6：Task 的执行模型在概念层缺一个关键说明

你选择了"Task 栈"替代"树递归"，并且说：

> 引擎 Tick 永远只执行当前栈顶的活跃节点

这是一个非常好的架构决策。但文档缺少一段关键的概念说明：**复合节点（Sequence, Parallel, Loop）在栈模型中的语义**。

这不是实现细节——它是架构文档应当传达的核心执行模型。因为不同的理解会导致完全不同的实现：

- **理解A**：Sequence 压栈时，将所有子任务逆序压入。栈顶永远是叶子节点。
- **理解B**：Sequence 自身驻留栈顶，内部维护一个 index 指针，每次 tick 推进一个子任务。

这两种理解对 Parallel 的处理方式截然不同，对 abort 清理的语义也完全不同。

**建议**：用一段简短的概念描述（不需要代码）说明你期望的栈推进语义。比如：

> 复合节点驻留栈顶，作为调度器存在。它在每帧 tick 中决定将哪个子任务实例化并压入栈顶执行。当子任务完成出栈后，控制权回到复合节点，由它决定下一步动作（推进下一子任务，或自身完成出栈）。Parallel 节点驻留栈顶期间，内部并行驱动多个子任务实例。

---

## 三、文档定位建议

你说这份文档是"结合具体游戏需求来增删修改"的参考基准。那它应该更清晰地分层：

1. **不可变的架构原则**（Philosophy 层）：任何游戏都应遵守
2. **推荐的概念模型**（概念层）：Goal / BehaviorCandidate / Task栈 的语义契约
3. **可定制的 Schema 示例**（配置层）：具体字段、具体节点类型

目前文档在这三层之间跳跃得太快。Philosophy 一结束，立刻就进入了 Runtime Core 的 Java 类定义和具体配置表。建议在 Philosophy 和 Schema 之间加一个**概念模型**章节，用纯文字描述清楚：
- 什么是 Goal、它的生命周期语义
- 什么是 BehaviorCandidate、评估流程
- Task 栈的推进语义
- 表达式引擎的统一原则

这样具体游戏团队就知道：**原则不能改、概念模型尽量不改、Schema 随便改**。

---

## 四、总结

| 层面 | 评价 |
|------|------|
| 架构方向 | ✅ 非常好，扁平评分 + 双轨打断 + GAS 协同是成熟的工业方案 |
| 概念完整性 | ⚠️ Goal绑定与评估的关系、表达式统一性、栈执行语义需要补充概念说明 |
| 哲学一致性 | ⚠️ `isPaused` 与"独立可测试"哲学矛盾，需要取舍表态 |
| 作为参考文档 | ⚠️ 缺少"原则-概念-示例"的分层，容易让读者把示例当规范 |
| 工程可落地性 | ✅ 决策流程5阶段写得清晰，协同架构表格一目了然 |

**一句话**：架构骨架是对的，需要补的不是更多字段，而是**中间层的概念契约**和**对已知陷阱的设计指导**。




# 统一条件/表达式系统方案

## 问题本质

上下文不同不是分裂语法的理由。类比：GLSL 只有一套表达式语法，但 Vertex Shader 能访问 `gl_VertexID`，Fragment Shader 能访问 `gl_FragCoord`。差异在于**可用变量**，不在语法。

你现在的做法相当于为每个 Shader 阶段各造了一门语言。

---

## 核心洞察

```
一套语法 × 不同阶段的变量供给 = 统一表达式引擎
```

每个阶段构造同一个 `AIEvalContext`，只是填充不同的槽位。表达式节点在求值时，访问到未填充的槽位则返回安全默认值（float→0, bool→false, actor→null）并记录警告。策划编辑器根据当前字段所属阶段，过滤可选节点类型。

---

## 运行时结构

### AIEvalContext

```java
class AIEvalContext {
    // ═══ 永远可用（所有阶段）═══
    final AIBrainComponent brain; // 包含 self Actor, currentCombatTarget

    // ═══ Think / Act / Abort 可用 ═══
    // Think阶段：候选行为提议的绑定方案
    // Act/Abort阶段：活跃行为实例的已确认绑定
    @Nullable final Int2ObjectMap<AIGoalInstance> goalBindings;
    @Nullable final Ai_behavior behaviorConfig;

    // ═══ 仅 Act 可用 ═══
    @Nullable final AIBehaviorInstance behaviorInstance; // localStorage, elapsed time

    // ═══ 仅 Sense.OnEvent 可用 ═══
    @Nullable final Event triggerEvent; // payload, magnitude, instigator, target
}
```

### 各阶段如何构造 Context

| 阶段 | brain | goalBindings | behaviorConfig | behaviorInstance | triggerEvent |
|------|-------|-------------|----------------|-----------------|--------------|
| **Sense** (SpatialScan) | ✅ | — | — | — | — |
| **Sense** (OnEvent) | ✅ | — | — | — | ✅ |
| **Think** (PreCondition/Score) | ✅ | ✅ 候选绑定 | ✅ | — | — |
| **Abort** | ✅ | ✅ 活跃绑定 | ✅ | ✅ | — |
| **Act** (Task内部) | ✅ | ✅ 活跃绑定 | ✅ | ✅ | — |

### AIBehaviorCandidate

Think 阶段需要一个中间结构，将"行为配置 + 绑定方案"打包后送入评估：

```java
class AIBehaviorCandidate {
    Ai_behavior config;
    Int2ObjectMap<AIGoalInstance> goalBindings; // 为本次评估选出的 Goal 实例
    float finalScore;

    AIEvalContext buildEvalContext(AIBrainComponent brain) {
        return new AIEvalContext(brain, this.goalBindings, this.config, null, null);
    }
}
```

---

## 统一选择器

### AIActorSelector

```cfg
interface AIActorSelector {
    // --- 永远可用 ---
    struct Self {}
    struct CombatTarget {}

    // --- 需要 goalBindings (Think/Act/Abort) ---
    struct GoalActor { goal: str ->ai_goal_definition; }

    // --- 需要 triggerEvent (Sense.OnEvent) ---
    struct EventInstigator {}
    struct EventTarget {}

    // --- 需要 behaviorInstance (Act) ---
    struct LocalVar { varTag: str ->gameplaytag; }
}
```

### AILocationSelector

```cfg
interface AILocationSelector {
    struct ActorPos { actor: AIActorSelector; }
    struct GoalPos { goal: str ->ai_goal_definition; }
    struct LocalVar { varTag: str ->gameplaytag; }
}
```

---

## 统一表达式

### AIFloatExpr

```cfg
interface AIFloatExpr {
    // ═══ 纯计算（永远可用）═══
    struct Const { value: float; }
    struct Math { op: MathOp; a: AIFloatExpr; b: AIFloatExpr; }
    struct Clamp { value: AIFloatExpr; min: float; max: float; }
    struct RandomRange { min: float; max: float; }
    struct Select {
        condition: AIBoolExpr;
        trueValue: AIFloatExpr;
        falseValue: AIFloatExpr;
    }

    // ═══ Actor 派生（需要 AIActorSelector 能解析）═══
    struct StatValue {
        actor: AIActorSelector;
        statTag: str ->stat_definition;
    }
    struct Distance {
        from: AILocationSelector;
        to: AILocationSelector;
    }

    // ═══ Goal 派生（需要 goalBindings）═══
    struct GoalMagnitude { goal: str ->ai_goal_definition; }
    struct GoalAge { goal: str ->ai_goal_definition; }

    // ═══ 行为元信息（需要 behaviorConfig）═══
    struct IsActiveBehaviorBonus {
        trueValue: float;
        falseValue: float;
    }

    // ═══ 行为实例派生（需要 behaviorInstance）═══
    struct BehaviorElapsedTime {}
    struct LocalVar { varTag: str ->gameplaytag; }

    // ═══ 事件派生（需要 triggerEvent）═══
    struct EventMagnitude {}
    struct EventVar { varTag: str ->gameplaytag; }
}
```

### AIBoolExpr

```cfg
interface AIBoolExpr {
    // ═══ 逻辑运算（永远可用）═══
    struct Const { value: bool; }
    struct Not { expr: AIBoolExpr; }
    struct And { exprs: list<AIBoolExpr>; }
    struct Or { exprs: list<AIBoolExpr>; }
    struct Compare {
        a: AIFloatExpr;
        op: CompareOp;
        b: AIFloatExpr;
    }

    // ═══ Actor 派生 ═══
    struct ActorHasTags {
        actor: AIActorSelector;
        query: GameplayTagQuery;
    }
    struct ActorIsValid {
        actor: AIActorSelector;
    }
    struct HasLineOfSight {
        from: AIActorSelector;
        to: AIActorSelector;
    }

    // ═══ 能力查询（永远可用，查 brain.self）═══
    struct CanActivateAbility {
        abilityId: int ->ability;
    }

    // ═══ Goal 存在性（需要 brain.activeGoals）═══
    struct HasGoal {
        goal: str ->ai_goal_definition;
    }
}
```

---

## 各阶段接入

原来四套独立类型，现在全部替换为统一类型，**只是字段名不同**：

```cfg
// ═══ Sense ═══
interface GoalGenerator {
    struct OnEvent {
        listenEventTag: str ->event_definition;
        triggerCondition: AIBoolExpr;   // 替代 SenseCondition
        // ...
    }
}

// ═══ Think ═══
table ai_behavior {
    preConditions: list<AIBoolExpr>;    // 替代 PreCondition
    score: AIFloatExpr;                 // 替代 ScoreValue
    abortConditions: list<AIBoolExpr>;  // 替代 AbortCondition
    // ...
}

table ai_behavior_group {
    sharedPreConditions: list<AIBoolExpr>; // 统一
    // ...
}

// ═══ Act ═══
interface AITask {
    struct Conditional {
        condition: AIBoolExpr;          // 替代 AICondition
        thenTask: AITask;
        elseTaskSeq: list<AITask>;
    }
    struct Wait {
        duration: AIFloatExpr;          // 替代 AIFloatValue
    }
    struct Loop {
        count: AIFloatExpr;
        task: AITask;
    }
}
```

**一套语法，覆盖所有阶段。**

---

## 校验规则

每个表达式节点标注其**最低阶段需求**。编辑器根据当前字段所属阶段过滤不合法节点：

| 节点 | 最低需求 | Sense | Think | Abort | Act |
|------|----------|-------|-------|-------|-----|
| `Const`, `Math`, `Compare` | 无 | ✅ | ✅ | ✅ | ✅ |
| `StatValue(Self)`, `Distance`, `ActorHasTags(Self)` | brain | ✅ | ✅ | ✅ | ✅ |
| `HasGoal`, `CanActivateAbility` | brain | ✅ | ✅ | ✅ | ✅ |
| `EventMagnitude`, `EventInstigator` | triggerEvent | ✅ | — | — | — |
| `GoalActor`, `GoalMagnitude`, `GoalAge` | goalBindings | — | ✅ | ✅ | ✅ |
| `IsActiveBehaviorBonus` | behaviorConfig | — | ✅ | ✅ | ✅ |
| `LocalVar`, `BehaviorElapsedTime` | behaviorInstance | — | — | ✅ | ✅ |

运行时兜底：访问未填充槽位 → 返回默认值 + 输出开发期警告日志。生产环境静默降级，不崩溃。

---

## 与 GAS 表达式系统的边界

AI 表达式（`AIFloatExpr` / `AIBoolExpr`）与 GAS 表达式（`FloatValue` / `Condition`）是**两套独立系统**，不合并。原因：

- GAS 的 Context 是 `(Instigator, Target, Payload)` —— 围绕"一次技能施放"
- AI 的 Context 是 `(Brain, GoalBindings, BehaviorInstance)` —— 围绕"一次行为决策"

它们偶尔交汇的唯一接口是 `CanActivateAbility` 和 `StatValue` —— 这两个节点AI侧直接访问 Actor 上的 GAS 组件读取数据，不走 GAS 的表达式求值链路。

两套系统各自完整、各自封闭、互不侵入。