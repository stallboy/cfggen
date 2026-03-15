整体评价：**方向非常对，和你前面的 Ability/GAS 设计也基本同频**。这套 AI 方案最大的优点是：

- 没有回到传统行为树那套“层层条件门 + 黑板共享污染 + 调试困难”的老路；
- 明确把 **Sense / Think / Act** 拆开；
- 决策层采用**扁平候选 + 动态算分**，这对工业项目非常实用；
- 和 GAS 的边界划分总体清晰，尤其是 **硬控归 GAS，战术切换归 AI，长期机制状态归 GAS 托管**，这个原则是很健康的。

但如果从“能落地成稳定工程系统”的角度看，这份设计目前还存在一些**关键语义未闭合、若干运行时模型不够完整、若干表结构有歧义**的问题。下面我按“优点 / 风险 / 改进建议”来讲。

---

# 一、总体优点

## 1. 核心分层是正确的

你把 AI 分成：

- **GoalGenerator**：生成“可交互目标”
- **Behavior**：基于目标和状态做选择
- **Task**：负责执行

这是非常好的。因为它避免了行为树里常见的问题：

- 感知逻辑写在树节点里，复用差
- 决策逻辑和执行逻辑缠在一起
- 条件重复出现，维护成本高

你这里的设计使得：
- 感知更新可以独立优化
- 决策是纯评估问题
- 执行是纯控制流问题

这对调试、扩展和性能优化都友好。

---

## 2. 行为组只是“共享条件目录”而不是控制流节点，这很对

`ai_behavior_group` 被定义为**仅用于共享条件和调试分组**，底层会被压平。这是个很成熟的设计。

传统树状 Group 一旦承担控制流职责，就会出现：
- 父节点短路逻辑影响子节点
- 同一个条件在不同分支重复
- 调试时难看清“为什么没选中”

你这里把组的职责收缩到：
- 共享 `sharedConditions`
- 给编辑器和调试器提供逻辑目录

这是对的。

不过这里后面我会指出一个字段命名和语义问题：你写的是 `subGroupTags: list<str> ->ai_behavior_group;`，这个字段名不太对，而且递归展开规则要明确定义。

---

## 3. 抢占模型比“纯分数重选”更成熟

你没有把所有行为都扔进一个 score 桶里硬比，而是明确区分：

- `interruptPriority > 0`：特权行为
- `interruptPriority == 0`：常规行为

再加上：
- `isInterruptible`
- `minCommitmentTime`
- `scoreInertia`

这比很多 Utility AI 方案成熟得多。因为实际项目里纯 score 系统最大的问题就是：

- 抖动
- 乒乓切换
- 动作承诺感不足
- 高优事件不够“硬”

你已经意识到了这些问题，并给出了机制，这是很好的。

---

## 4. 与 GAS 的边界意识很强

你这里最值得肯定的点之一，是没有让 AI 去“接管角色物理现实”。

例如：
- 眩晕、死亡、冻结 → `global_ai_settings.abortConditions`
- 霸体、转阶段、行为池切换 → 交给 GAS Status 驱动
- AI Modifier 生命周期 → 绑定在 GAS Status 上，而不是 AITask 显式 Add/Remove

这套边界划分很健康。因为项目里最容易出事故的地方，就是 AI、动画、技能、受击系统都在抢控制权。你现在至少在架构哲学上，避免了这个方向性错误。

---

# 二、当前最主要的问题

---

## 1. `AIGoalInstance` 生命周期定义还不够完整

这是我认为目前 **P0 级别** 的问题之一。

你定义了：

```java
class AIGoalInstance {
    Ai_goal_definition goalCfg;
    Actor associatedActor;
    Vector3 position;
    float magnitude;
    float creationTime;
}
```

但这里缺了几个非常关键的东西：

### 问题1：Goal 的唯一性和去重策略不明确

例如：
- 同一个敌人被扫描到 10 次，是 10 个 Goal，还是 1 个 Goal 刷新？
- 同一个枪声事件连发 3 次，是叠加 magnitude，还是更新 creationTime？
- 空间扫描类 Goal 是“全集缓存”还是“每次重建”？

如果不定义，运行时会很混乱。尤其你的决策是基于 Goal 的，Goal 池污染会直接导致行为评估异常。

### 建议

给 `AIGoalInstance` 明确 identity 和 refresh 规则：

```java
class AIGoalInstance {
    int runtimeGoalId;          // 唯一运行时ID
    Ai_goal_definition goalCfg;
    Actor associatedActor;
    Vector3 position;
    float magnitude;

    float creationTime;
    float lastRefreshTime;
    float expirationTime;       // -1 = 永不过期，或由 validator 决定

    GoalSourceType sourceType;  // SpatialScan / Event
}
```

并在 `GoalGenerator` 或 `ai_goal_definition` 上增加去重策略：

```cfg
enum GoalMergePolicy {
    RefreshExisting;   // 同 key 刷新已有 Goal
    ReplaceExisting;   // 删除旧 Goal，创建新 Goal
    StackMagnitude;    // magnitude 叠加
    AllowMultiple;     // 允许并存多个实例
}

struct GoalIdentity {
    byActor: bool;
    byLocationCell: bool;
    byGoalTypeOnly: bool;
}
```

否则“感知层”会成为未来 bug 高发区。

---

## 2. Goal 和 Behavior 的绑定关系还有歧义

你现在的行为实例里有：

```java
Int2ObjectMap<AIGoalInstance> boundGoals;
```

这个设计方向是对的，说明你已经意识到一个行为可能需要绑定多个不同种类的 Goal。

但是当前文档没说清楚：

### 问题1：`requiredGoals` 是“存在即可”还是“需要选出最佳实例”？

例如一个行为：
- 需要 `Goal.Enemy`
- 需要 `Goal.Cover`

那系统是：
- 只验证这两类 Goal 各至少有一个？
- 还是要从每类 Goal 中选一个最优实例绑定到 `boundGoals`？

### 问题2：如果每类 Goal 有多个候选，选择规则是什么？

比如：
- 最近敌人
- 最老的可疑声源
- 分数最高的掩体点

当前没有定义“Goal 选优器”，这会导致行为评估和执行之间不一致。

### 建议

对 `requiredGoals` 增加绑定规则：

```cfg
struct RequiredGoalBinding {
    goal: str ->ai_goal_definition;
    select: GoalSelectPolicy;
    required: bool;
}

enum GoalSelectPolicy {
    HighestMagnitude;
    NearestToSelf;
    NearestToCurrentTarget;
    Oldest;
    Newest;
    Random;
}
```

然后：

```cfg
requiredGoals: list<RequiredGoalBinding>;
```

否则 `GoalActor(goal)` 和 `GoalLocation(goal)` 的解析来源并不稳定。

---

## 3. 决策层缺少“候选行为评估上下文”的显式模型

你当前写的是：

- `PreCondition`、`ScoreValue` 运行时上下文是 (`AIBrainComponent`, `Ai_behavior`)

但这其实不够。

因为很多评分本质上依赖于“**该行为如果绑定了某个 Goal 实例后**”的上下文。例如：

- 距离最近的掩体
- 年龄最大的警戒点
- 与当前敌人最接近的治疗包

这些不是单靠 `AIBrainComponent` 和 `Ai_behavior` 就能算出来的，必须有一个“候选绑定方案”的中间层。

### 建议

引入一个中间运行时概念：

```java
class AIBehaviorCandidate {
    Ai_behavior behaviorCfg;
    AIBrainComponent brain;
    Int2ObjectMap<AIGoalInstance> boundGoals; // 为这次评估选出的绑定方案
    float score;
    int interruptPriority;
}
```

然后准入和评分上下文应是：

- `PreCondition` / `ScoreValue`：(`AIBrainComponent`, `AIBehaviorCandidate`)

否则你现在很多表达式虽然看起来能写，但实现时会发现“没有确定的 Goal 绑定”。

这是非常关键的闭环问题。

---

## 4. `ai_behavior_group` 字段有明显问题

你写的是：

```cfg
table ai_behavior_group[name] {
    name: str;
    description: text;
    sharedConditions: list<AICondition>; 
    behaviors: list<str> ->ai_behavior;
    subGroupTags: list<str> ->ai_behavior_group;
}
```

### 问题1：类型不一致

前面 `ai_behavior.preConditions` 用的是 `PreCondition`，这里却是 `AICondition`。  
但你后面 `AITask.Conditional` 也用了 `AICondition`，且 `AICondition` 只有：

```cfg
interface AICondition {
    struct Const { value: bool; }
}
```

这显然不闭合。

### 问题2：字段名 `subGroupTags` 不对

它引用的是 `ai_behavior_group`，不是 tag。

### 建议

统一成：

```cfg
table ai_behavior_group[name] {
    name: str;
    description: text;
    sharedPreConditions: list<PreCondition>;
    behaviors: list<str> ->ai_behavior;
    subGroups: list<str> ->ai_behavior_group;
}
```

然后 `AICondition` 应考虑废弃，或与 `PreCondition` 合并成一个统一条件系统。

---

## 5. 条件系统现在是割裂的

目前你有三套条件：

- `SenseCondition`
- `PreCondition`
- `AICondition`
- 还有 `AbortCondition`

这会在工程里快速失控。

### 问题

这些条件系统从用途上看确实属于不同层，但它们的表达能力大量重叠。现在的问题是：

- `AICondition` 几乎空壳
- `PreCondition` 已经相对可用
- `SenseCondition` 又重新起一套
- `AbortCondition` 也是特化的一套

这样会导致：
- 表达式能力不一致
- 策划学习成本高
- 编辑器实现复杂
- 很多功能要复制四遍

### 建议：统一成一套“AI Bool Expression”

推荐思路：

- 保留不同入口名字，但底层统一为一套表达式体系
- 例如统一成 `AIBoolValue` / `AICondition`

```cfg
interface AIBoolValue {
    struct Const { value: bool; }
    struct Not { value: AIBoolValue; }
    struct And { values: list<AIBoolValue>; }
    struct Or { values: list<AIBoolValue>; }

    struct CompareFloat {
        a: AIFloatValue;
        op: CompareOp;
        b: AIFloatValue;
    }

    struct ActorHasTags {
        actor: AIActorSelector;
        query: GameplayTagQuery;
    }

    struct IsValidActor {
        actor: AIActorSelector;
    }

    struct HasLineOfSight {
        from: AIActorSelector;
        to: AIActorSelector;
    }
}
```

然后：
- `PreCondition` = `AIBoolValue`
- `SenseCondition` = `AIBoolValue`
- `AbortCondition` = `AIBoolValue`

只是在文档层约定不同使用场景。

这是我非常建议的一个方向。

---

## 6. `ScoreValue` 表达能力还不够，且命名不统一

现在 `ScoreValue` 有：

- Const
- Math
- Distance
- GoalAge
- StatValue
- IsActiveBehavior

这还不够支撑复杂项目，至少缺：

- 条件分支
- 归一化/Clamp
- 反转
- 曲线映射
- 局部变量读取
- Goal 的 magnitude
- 当前行为运行时长
- 冷却剩余时间
- 随机扰动

### 建议至少补这些

```cfg
interface ScoreValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: ScoreValue; b: ScoreValue; }

    struct Clamp {
        value: ScoreValue;
        min: ScoreValue;
        max: ScoreValue;
    }

    struct Select {
        condition: AIBoolValue;
        trueValue: ScoreValue;
        falseValue: ScoreValue;
    }

    struct GoalMagnitude {
        goal: str ->ai_goal_definition;
    }

    struct BehaviorElapsedTime { }
    struct CooldownRemaining {
        behavior: str ->ai_behavior;
    }

    struct LocalVar {
        varTag: str ->gameplaytag;
    }

    struct RandomRange {
        min: ScoreValue;
        max: ScoreValue;
        stablePerTick: bool;
    }

    struct Curve {
        input: ScoreValue;
        curveId: int ->curve_config;
    }
}
```

否则到后面你会不断加特化节点。

---

## 7. AITask 执行栈模型还没完全闭合

你已经提出“基于任务栈推进，不做树递归”——这很好。  
但当前任务实例模型仍不够完整。

### 关键问题1：复合节点实例化状态在哪？

例如：

```cfg
struct Sequence { tasks: list<AITask>; }
struct Loop { count: AIFloatValue; task: AITask; }
struct Parallel { tasks: list<AITask>; policy: ParallelPolicy; }
```

这些节点都是**有内部状态**的：

- Sequence 要知道当前执行到第几个子节点
- Loop 要知道当前循环了几次
- Parallel 要知道每个子任务的状态

但 `AITaskInstance` 目前只有：

```java
abstract class AITaskInstance<T extends AITask> {
    T taskCfg; 
    abstract void onStart(...)
    abstract TaskStatus tick(...)
    abstract void onEnd(...)
}
```

这还不够表达复合节点的状态。

### 建议

明确复合节点各自有独立实例字段，例如：

```java
class SequenceTaskInstance extends AITaskInstance<AITask.Sequence> {
    int currentIndex;
}

class LoopTaskInstance extends AITaskInstance<AITask.Loop> {
    int currentIteration;
    int maxIteration;
}

class ParallelTaskInstance extends AITaskInstance<AITask.Parallel> {
    List<AITaskInstance> children;
    BitSet completed;
}
```

并在文档中明确：
- `taskStack` 存的是“活跃路径”还是“所有活跃节点”
- `Parallel` 驻留栈顶时，其 children 是不是独立 tick
- 子任务失败时父任务如何处理

---

## 8. `Parallel` 节点语义不完整

目前只有：

```cfg
policy: ParallelPolicy; // WaitAll / WaitAny
```

但项目里并行节点最关键的问题是**失败传播策略**。例如：

- 一个子任务失败，是否整体失败？
- `WaitAny` 下一个成功就结束，那其他 Running 子任务要不要 abort？
- `PlayAnimation + MoveTo` 这种并行，如果动画结束了但移动没结束怎么办？

### 建议补充：

```cfg
struct Parallel {
    tasks: list<AITask>;
    successPolicy: ParallelSuccessPolicy; // All / Any
    failurePolicy: ParallelFailurePolicy; // AnyFail / AllFail / IgnoreFail
    abortRemainingOnResolve: bool;
}
```

---

## 9. `CastAbility` 成败语义不清晰

```cfg
struct CastAbility { 
    abilityId: int ->ability; 
    target: AITargetSelector; 
}
```

这个节点在运行时非常关键，但现在没定义清楚：

- 是“发起施法请求成功”就算 Success？
- 还是“Ability 真正进入激活态”算 Success？
- 还是“技能完整释放结束”算 Success？
- 如果被 GAS 的 activationBlockedTags 拦截，是 Failed 还是 Running 后失败？

### 建议明确三种常见模式

```cfg
struct CastAbility {
    abilityId: int ->ability;
    target: AITargetSelector;
    completionPolicy: AbilityTaskCompletionPolicy;
}

enum AbilityTaskCompletionPolicy {
    RequestAccepted;     // 请求进入系统即 Success
    AbilityActivated;    // 真正激活成功才 Success
    AbilityFinished;     // 整个技能完成才 Success
}
```

否则 AI 执行层会和 Ability 层产生大量模糊 bug。

---

## 10. 暂停/抢占恢复模型不够清楚

你有：

```java
boolean isPaused;
```

这是个危险信号。因为“暂停一个行为”不是一个 bool 能定义清楚的。

### 问题

当高优行为抢占时，原行为是：
- 直接销毁？
- 挂起等待恢复？
- 只允许部分任务恢复？
- `MoveTo` 这类持续动作恢复后是否重建寻路？
- `CastAbility` 这类任务被暂停后还能恢复吗？

### 建议

先明确架构原则：  
**默认不要做通用暂停恢复**，而是只做：
- 常规重选 = abort 旧行为
- 特殊需要恢复的行为 = 显式声明可挂起

否则恢复语义非常复杂。

推荐增加：

```cfg
table ai_behavior {
    ...
    preemptionPolicy: PreemptionPolicy;
}

enum PreemptionPolicy {
    AbortAndRestart;   // 默认，最稳
    SuspendAndResume;  // 少量特殊行为使用
}
```

然后只有明确支持恢复的 Task 才实现 suspend/resume 生命周期。

否则 `isPaused` 最后会变成半残系统。

---

# 三、与 GAS 协同部分的意见

---

## 1. 总体方向是正确的

你把协同分成：

- Hard CC：GAS 主导
- Soft Interrupt：AI 主导
- Phase：AI 决策，GAS 托管

这是很清晰的。

---

## 2. 但文中提到的 `ignoreGlobalAborts` 没有定义

在“转阶段/机制锁”那段里你写了：

> AI 通过特权行为（高 `interruptPriority` + `ignoreGlobalAborts` 霸体）主动执行转阶段前摇

但你的 `ai_behavior` 并没有这个字段。

这说明设计意图和数据结构还没完全对齐。

### 这里我建议谨慎

其实按你的哲学，**不建议真的存在一个裸的 `ignoreGlobalAborts` 行为字段**。  
因为这很容易开后门，让 AI 绕过 GAS 的物理法则。

更合理的是：

- 全局 abort 仍然生效；
- 但该行为在开始时立刻通过 `DoEffect` 给自己挂一个“霸体/免控状态”；
- 因为宿主已经被 GAS 物理层保护，所以不会再触发那些 abort tag。

也就是说：
- **不靠 AI 行为字段绕规则**
- **靠 GAS tag 物理改变规则前提**

这和你前面能力系统的理念是一致的。

所以我建议：
- 删除文中 `ignoreGlobalAborts` 的提法
- 明确声明：**AI 不拥有绕过全局法则的权限，除非先通过 GAS 获得合法状态豁免**

这会更稳。

---

## 3. `AIBehaviorModifier` 放在 GAS Behavior 里是对的，但接口还不完整

你写的是在 ability design 里的 `Behavior` 接口中扩展：

```cfg
struct AIBehaviorModifier {
    modifier: str ->ai_behavior_modifier;
}
```

方向是对的，但缺少两个东西：

### 缺少作用对象

是对：
- Status 宿主生效？
- Instigator 生效？
- Context.target 生效？

按你现有风格，应该明确 target。

### 缺少堆叠策略

多个 Status 同时挂同一个 modifier 怎么办？

- refCount++？
- 同名 modifier 合并？
- 按来源隔离？

你虽然在运行时用了：

```java
class AIBehaviorModifierRef {
    Ai_behavior_modifier modifierCfg;
    int refCount;
}
```

但配置层没明确这些语义。

建议至少补充说明：
- modifier 以 `name` 为 key 合并引用计数
- 相同 modifier 重复挂载只增 refCount
- 卸载时 refCount 归零才移除

这个要在文档写清楚。

---

# 四、执行层还缺的一些关键节点

---

## 1. 缺少“设置 / 读取目标”的任务辅助能力

你现在有 `WithLocalVar`，但行为执行里经常需要：

- 记住某个目标
- 切换当前 combat target
- 把选中的 GoalActor 写入 localStorage

否则很多复合行为会写得别扭。

### 建议增加

```cfg
struct SetLocalActor {
    varTag: str ->gameplaytag;
    target: AITargetSelector;
}

struct SetCurrentCombatTarget {
    target: AITargetSelector;
}
```

---

## 2. 缺少“失败可恢复”的控制流节点

很多 AI 任务需要：
- 尝试接近
- 失败后换备选方案
- 或者某个节点失败时走降级分支

你现在只有 `ReturnFailed`，但缺少 `Try/Else` 这种局部恢复控制。

### 建议增加

```cfg
struct Try {
    task: AITask;
    fallbackTask: AITask;
}
```

或者给 `Sequence` 增加失败策略，但我更建议显式节点。

---

## 3. 缺少“等待事件”的任务节点

这个很重要。  
既然你前面能力系统里坚持“时序靠状态/事件”，AI 这边也一样，很多执行逻辑不是单纯 `Wait(duration)`，而是：

- 等动画通知
- 等能力结束
- 等目标进入范围
- 等对话完成

### 建议增加

```cfg
struct WaitEvent {
    eventTag: str ->event_definition;
    timeout: AIFloatValue;
    successCondition: AIBoolValue;
}
```

这个节点会大幅提升 AI 执行层的工程可用性。

---

# 五、调试性方面还可以更进一步

你的哲学里强调白盒调试，这很好。但当前配置结构还没把调试需求显式化。

## 建议增加运行时调试快照结构

例如：

```java
class AIDebugSnapshot {
    List<GoalDebugInfo> goals;
    List<BehaviorCandidateDebugInfo> candidates;
    ActiveBehaviorDebugInfo activeBehavior;
    String lastAbortReason;
    String lastFailureReason;
}
```

其中：
- 每个 Goal 显示来源、年龄、是否通过 validator
- 每个 behavior 显示：
  - requiredGoals 是否满足
  - preConditions 哪个失败
  - 最终 score
  - 冷却状态
  - 是否因 minCommitment 被压制
- active task stack 的当前路径

这套系统如果做出来，会非常强。

---

# 六、我建议的重点修正项

按优先级排：

## P0
### 1. 引入 `AIBehaviorCandidate`
闭合“行为评估时 Goal 绑定”的运行时模型。

### 2. 明确 Goal 的去重、刷新、过期、合并规则
否则感知层会混乱。

### 3. 统一条件系统
至少把 `AICondition / PreCondition / SenseCondition / AbortCondition` 的底层表达式统一。

### 4. 明确 `CastAbility`、`Parallel`、`taskStack` 的执行语义
这些都是核心落地点。

---

## P1
### 5. 明确抢占策略：默认 Abort，不默认 Pause/Resume
不要让 `isPaused` 变成技术债。

### 6. 扩展 ScoreValue / AIFloatValue 的表达能力
否则后面一定会不断加特化节点。

### 7. 修正 `ai_behavior_group` 的字段命名与类型问题
这是文档一致性问题，但最好现在就修。

---

## P2
### 8. 增加 WaitEvent / Try / SetLocalActor 等高频任务节点
能显著提升实战配置体验。

### 9. 补全调试可观测性设计
不是功能必须，但会决定这个系统最终是否“真的好用”。

---

# 七、总评

如果只看设计方向，我会给这套 AI 系统 **8.5/10**。  
它最大的优点是：

- 没有迷信传统行为树；
- Utility/Score 化思路正确；
- 与 GAS 的边界意识强；
- 生命周期托管思路成熟。

但如果看“能否稳定进入大规模制作”，目前还有几个必须补的闭环：

- **Goal 的身份与绑定**
- **候选行为评估上下文**
- **条件系统统一**
- **Task 执行语义补完**
- **抢占/恢复语义收敛**

如果你愿意，我下一步可以继续帮你做两件事里的任意一件：

1. **直接把这份 AI 文档修订成一个更严谨的正式版 schema**
2. **专门补一章《AI Runtime Semantics》**，把 Goal 刷新、行为评估、抢占、Task 栈推进的语义全部写严谨

这两个里，第二个最值钱。


你这个反驳是成立的，而且是关键点。

我上一条里说“统一成一套表达式系统”有点过度理想化了。  
**问题不在于语法统一不统一，而在于：不同阶段的表达式所面对的运行时事实确实不同。**

比如：

- **Sense** 阶段面对的是“感知输入流 / 事件 / 扫描结果”，它不应该天然能看到行为局部变量。
- **Think** 阶段面对的是“大脑状态 + 候选目标绑定 + 决策上下文”，它不应该直接依赖某个 Task 的执行栈细节。
- **Act** 阶段面对的是“当前行为实例 + Task 局部状态 + 执行中的目标”，它需要的是控制流局部上下文。
- **Abort** 阶段其实是 Think 的一个特殊子集，但它要求**稳定、低成本、可快速判定**，不适合无限开放。

所以真正应该做的，不是“把所有条件接口硬合并”，而是：

> **统一表达式内核，分化运行时上下文视图。**

也就是：
- 语义层尽量统一
- 上下文访问能力按阶段裁剪
- 每个阶段暴露自己的 Selector / Value / Condition 皮肤
- 但底层共享一套求值模型与实现方式

这个方向既保留了阶段边界，也避免了四套系统彻底分叉。

下面我给出一个更合理的完整方案。

---

# 一、核心结论

建议采用：

## 方案：**统一求值框架 + 分阶段上下文 + 分层表达式门面**

分三层：

### 1. 底层统一：Evaluator Core
定义统一的抽象求值范式：

- `BoolExpr<TContext>`
- `FloatExpr<TContext>`
- `ActorExpr<TContext>`
- `LocationExpr<TContext>`

这是**程序实现层**的统一核心。

---

### 2. 中层分化：阶段上下文（Runtime Context）
每个阶段有自己的上下文类型：

- `SenseContext`
- `ThinkContext`
- `ActContext`

必要时：
- `AbortContext` 不单独存在，直接是 `ThinkContext` 的受限子视图

---

### 3. 上层配置：按阶段暴露不同接口
配置层仍然保留：
- `SenseCondition / SenseFloatValue / SenseTargetSelector`
- `PreCondition / ScoreValue / ThinkActor / ThinkLocation`
- `AICondition / AIFloatValue / AITargetSelector`
- `AbortCondition`

但这些只是**阶段专属 DSL 门面**，底层映射到统一 evaluator。

---

这样做的好处是：

- 策划层仍然感受到清晰边界，不会乱用上下文
- 程序层不会维护四套完全独立的解释器
- 文档层可以把“表达式系统”讲清楚，而不是散成四套孤岛
- 后续扩展时，只需要新增某阶段上下文可见的节点，而不是复制所有逻辑

---

# 二、先确定运行时结构

这是关键。  
表达式系统长什么样，取决于运行时上下文长什么样。

---

## 2.1 AIBrainComponent 保持“长期认知状态”

这个层级没有问题，但建议从“存储一切”改成“只存长期稳定状态”。

```java
class AIBrainComponent {
    Actor self;

    // 长期认知
    List<AIGoalInstance> activeGoals;
    Actor currentCombatTarget;
    Vector3 lastKnownPosition;

    // 执行态
    AIBehaviorInstance activeBehavior;

    // 动态修饰
    List<AIBehaviorModifierRef> behaviorModifiers;

    // 冷却与决策辅助
    Int2FloatMap behaviorCooldowns;
    Int2FloatMap behaviorLastScores;   // 可选，调试/惯性
}
```

---

## 2.2 GoalInstance 明确是“感知产物”，不是执行态

```java
class AIGoalInstance {
    Ai_goal_definition goalCfg;

    Actor associatedActor;
    Vector3 position;

    float magnitude;
    float creationTime;
    float lastRefreshTime;
}
```

它的职责只有：
- 表示一个可供决策使用的候选目标事实
- 不参与执行局部状态

---

## 2.3 引入 AIBehaviorCandidate：决策阶段的核心对象

这是必须有的中间层。

```java
class AIBehaviorCandidate {
    Ai_behavior behaviorCfg;
    AIBrainComponent brain;

    // 本次候选绑定的 Goal 解
    Int2ObjectMap<AIGoalInstance> boundGoals;

    float computedScore;
    int interruptPriority;
}
```

这代表：

> 决策阶段评估的不是“行为配置本身”，而是“行为 + 一组候选 Goal 绑定”的一个候选解。

这样很多问题自然解决：

- `GoalActor(goal)` 到底取哪个实例？→ 候选里已经绑定好了
- score 是对行为还是对目标打分？→ 对 candidate 打分
- 同一行为可否因为不同目标形成多个候选？→ 可以

---

## 2.4 AIBehaviorInstance：执行阶段上下文

```java
class AIBehaviorInstance {
    Ai_behavior behaviorCfg;
    AIBrainComponent brain;

    Int2ObjectMap<AIGoalInstance> boundGoals; // 从 candidate 固化而来

    Stack<AITaskInstance> taskStack;
    Store localStorage;

    float startTime;
    boolean pendingAbort;
}
```

注意这里我建议：

- **去掉 `isPaused`**
- 默认架构是 **abort + reselect**
- 如果以后某个项目真需要 suspend/resume，再作为特化扩展

因为一旦把 pause 作为基础能力，整个执行模型会变脏。

---

# 三、确定分阶段运行时 Context

这是表达式系统的根。

---

## 3.1 SenseContext

感知阶段需要的，不是大脑全状态，而是“当前生成过程的上下文”。

```java
record SenseContext(
    AIBrainComponent brain,     // 可读 self / 当前认知，但不能直接写 activeGoals
    GoalGenerator generatorCfg,

    // 当前输入源（二选一或都为空）
    Event sensedEvent,          // OnEvent 型生成器时有效
    Object scanHit,             // SpatialScan 的原始命中结果（可抽象成接口）

    float currentTime
) {}
```

### 设计原则
- 可读 `brain.self`
- 可读一些长期事实（比如 currentCombatTarget）
- **不可直接访问 activeGoals 进行自循环依赖**
- 面向“输入解释”，不是面向“行为选择”

---

## 3.2 ThinkContext

决策阶段要围绕 candidate 来展开。

```java
record ThinkContext(
    AIBrainComponent brain,
    AIBehaviorCandidate candidate,
    float currentTime
) {}
```

可访问：
- self
- 当前 activeBehavior
- 所有 activeGoals（如需要全局比较）
- candidate.boundGoals
- behavior cooldown
- stats/tags
- currentCombatTarget

这是决策表达式最核心的上下文。

---

## 3.3 ActContext

执行阶段的上下文必须围绕 BehaviorInstance。

```java
record ActContext(
    AIBehaviorInstance behavior,
    AITaskInstance currentTask,
    float currentTime,
    float deltaTime
) {}
```

可访问：
- behavior.boundGoals
- behavior.localStorage
- behavior.brain
- 当前 task
- 执行中的中间状态

---

## 3.4 AbortContext：不要单独建类型，视为 ThinkContext 子集

`AbortCondition` 本质上是：

> 对当前 activeBehavior 的快速合法性检查

所以完全可以定义为：

```java
record AbortContext(
    AIBrainComponent brain,
    AIBehaviorInstance activeBehavior,
    float currentTime
) {}
```

但从架构上更建议：

- `AbortCondition` 复用 Think/Act 的一部分表达能力
- 但文档上约束：AbortCondition 应保持低成本、无副作用、可快速判定

即：

- 可以是独立门面
- 但底层没必要完全独立一套引擎

---

# 四、表达式系统怎么定

现在进入关键部分。

---

## 4.1 不做“所有阶段一套裸接口”，而是做两层模型

---

## 底层统一抽象

程序实现层：

```java
interface BoolExpr<C> {
    boolean eval(C ctx);
}

interface FloatExpr<C> {
    float eval(C ctx);
}

interface ActorExpr<C> {
    Actor eval(C ctx);
}

interface LocationExpr<C> {
    Vector3 eval(C ctx);
}
```

以及一些统一的组合节点：
- Const
- Not / And / Or
- Compare
- Math
- Clamp
- Select

这些是**跨阶段复用**的。

---

## 上层阶段门面

配置层保留四套名字，但本质只是不同上下文皮肤。

### 感知阶段
- `SenseCondition`
- `SenseFloatValue`
- `SenseTargetSelector`

### 决策阶段
- `PreCondition`
- `ScoreValue`
- `ThinkActor`
- `ThinkLocation`

### 执行阶段
- `AICondition`
- `AIFloatValue`
- `AITargetSelector`

### 中止阶段
- `AbortCondition`

这样策划仍能感知到：
- 我现在在感知系统里写东西
- 我现在在决策系统里写东西

但程序实现是统一的 evaluator 架构。

---

# 五、具体建议：条件/表达式系统如何重构

---

## 5.1 感知阶段

### 保留独立的 SenseCondition
因为它访问的是输入事件/扫描结果，不适合和决策层混在一起。

```cfg
interface SenseTargetSelector {
    struct EventInstigator {}
    struct EventTarget {}
    struct EventPayloadActor { actorVarTag: str ->gameplaytag; }

    struct ScanHitActor {}
    struct ScanHitLocation {}

    struct Self {}
    struct CurrentCombatTarget {}
}

interface SenseFloatValue {
    struct Const { value: float; }
    struct EventMagnitude {}
    struct DistanceFromSelfToScanHit {}
}

interface SenseCondition {
    struct Const { value: bool; }
    struct Not { condition: SenseCondition; }
    struct And { conditions: list<SenseCondition>; }
    struct Or  { conditions: list<SenseCondition>; }

    struct Compare {
        a: SenseFloatValue;
        op: CompareOp;
        b: SenseFloatValue;
    }

    struct TargetHasTags {
        target: SenseTargetSelector;
        query: GameplayTagQuery;
    }

    struct HasLineOfSightToScanHit {}
}
```

### 原则
感知阶段只关心：
- 这条输入要不要转成 Goal
- 如何从输入中抽出 Actor / Location / magnitude

---

## 5.2 决策阶段：PreCondition 与 ScoreValue 共享同一套 Think 上下文

这是最核心的一层。

建议明确：

- `PreCondition` 是 bool 表达式
- `ScoreValue` 是 float 表达式
- 两者共用同一套 selector/value 基础件

### Think 侧统一原子源

```cfg
interface ThinkActor {
    struct Self {}
    struct CurrentCombatTarget {}
    struct ActiveBehaviorTarget {} // 如果当前行为有主目标，可选
    struct GoalActor { goal: str ->ai_goal_definition; }
}

interface ThinkLocation {
    struct Self {}
    struct CurrentCombatTarget {}
    struct GoalActor { goal: str ->ai_goal_definition; }
    struct GoalLocation { goal: str ->ai_goal_definition; }
}

interface ThinkFloatValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: ThinkFloatValue; b: ThinkFloatValue; }

    struct Distance {
        from: ThinkLocation;
        to: ThinkLocation;
    }

    struct GoalAge {
        goal: str ->ai_goal_definition;
    }

    struct GoalMagnitude {
        goal: str ->ai_goal_definition;
    }

    struct StatValue {
        target: ThinkActor;
        statTag: str ->stat_definition;
    }

    struct CooldownRemaining {
        behavior: str ->ai_behavior;
    }

    struct IsActiveBehavior {
        trueValue: float;
        falseValue: float;
    }
}
```

### 决策布尔表达式

```cfg
interface PreCondition {
    struct Const { value: bool; }
    struct Not { condition: PreCondition; }
    struct And { conditions: list<PreCondition>; }
    struct Or { conditions: list<PreCondition>; }

    struct Compare {
        a: ThinkFloatValue;
        op: CompareOp;
        b: ThinkFloatValue;
    }

    struct TargetHasTags {
        target: ThinkActor;
        query: GameplayTagQuery;
    }

    struct CanActivateAbility {
        abilityId: int ->ability;
    }
}
```

### ScoreValue
`ScoreValue` 则只专注于 float 求值：

```cfg
interface ScoreValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: ScoreValue; b: ScoreValue; }

    struct FromThinkFloat {
        value: ThinkFloatValue;
    }

    struct Select {
        condition: PreCondition;
        trueValue: ScoreValue;
        falseValue: ScoreValue;
    }

    struct Clamp {
        value: ScoreValue;
        min: ScoreValue;
        max: ScoreValue;
    }
}
```

### 这样做的好处
- 决策层不再“布尔一套、浮点一套、来源又一套”地割裂
- `PreCondition` 和 `ScoreValue` 共享原子源
- 扩展时只要补 `ThinkFloatValue/ThinkActor/ThinkLocation`，两边一起受益

---

## 5.3 执行阶段：AICondition / AIFloatValue / AITargetSelector 独立保留

这一层必须独立，因为它面对的是 BehaviorInstance 和 localStorage。

### AITargetSelector

```cfg
interface AITargetSelector {
    struct Self {}
    struct CurrentCombatTarget {}

    struct BoundGoalActor {
        goal: str ->ai_goal_definition;
    }

    struct BoundGoalLocation {
        goal: str ->ai_goal_definition;
    }

    struct LocalActorVar {
        actorVarTag: str ->gameplaytag;
    }
}
```

### AIFloatValue

```cfg
interface AIFloatValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: AIFloatValue; b: AIFloatValue; }

    struct LocalVar {
        varTag: str ->gameplaytag;
    }

    struct StatValue {
        target: AITargetSelector;
        statTag: str ->stat_definition;
    }

    struct Distance {
        from: AITargetSelector;
        to: AITargetSelector;
    }

    struct BehaviorElapsedTime {}
}
```

### AICondition

```cfg
interface AICondition {
    struct Const { value: bool; }
    struct Not { condition: AICondition; }
    struct And { conditions: list<AICondition>; }
    struct Or { conditions: list<AICondition>; }

    struct Compare {
        a: AIFloatValue;
        op: CompareOp;
        b: AIFloatValue;
    }

    struct TargetHasTags {
        target: AITargetSelector;
        query: GameplayTagQuery;
    }
}
```

---

## 5.4 AbortCondition：保留独立门面，但复用决策原子源

这是一个折中方案。

AbortCondition 不建议完全开放成“任意 AICondition”，因为：
- 成本不可控
- 容易把复杂逻辑塞进高频 abort 检查
- 会让“局部失效拦截”失去轻量特征

所以建议：

### 方式
- `AbortCondition` 配置上保留独立接口
- 但其可访问的数据源**和 ThinkContext 接近**
- 能力子集化

```cfg
interface AbortCondition {
    struct Const { value: bool; }
    struct Not { condition: AbortCondition; }
    struct Or { conditions: list<AbortCondition>; }
    struct And { conditions: list<AbortCondition>; }

    struct Compare {
        a: AbortFloatValue;
        op: CompareOp;
        b: AbortFloatValue;
    }

    struct TargetHasTags {
        target: AbortActor;
        query: GameplayTagQuery;
    }
}

interface AbortActor {
    struct Self {}
    struct CurrentCombatTarget {}
    struct BoundGoalActor { goal: str ->ai_goal_definition; }
}

interface AbortFloatValue {
    struct Const { value: float; }
    struct DistanceBetween {
        from: AbortActor;
        to: AbortActor;
    }
    struct StatValue {
        target: AbortActor;
        statTag: str ->stat_definition;
    }
}
```

这保持了 Abort 的轻量语义。

---

# 六、最终建议：文档中怎么表述这件事

你应该在 AI 文档中明确写出一段设计原则，大意如下：

---

## Expression System Philosophy（建议新增章节）

AI 系统中的感知、决策、执行与中止判定虽然都依赖“条件/数值表达式”，但它们所面对的运行时上下文并不相同。因此，本系统不采用“单一裸表达式接口横跨全阶段”的设计，而采用：

### 1. 统一求值范式
所有表达式本质上分为：
- Bool Expression
- Float Expression
- Actor Selector
- Location Selector

### 2. 分阶段上下文视图
不同阶段暴露不同的上下文读取权限：
- Sense：读取感知输入与事件
- Think：读取大脑状态与候选 Goal 绑定
- Act：读取行为实例、局部变量与执行态
- Abort：读取当前行为的轻量失效判定信息

### 3. 分阶段 DSL 门面
配置层仍按阶段提供专用接口（SenseCondition、PreCondition、AICondition、AbortCondition），以确保策划不会误用不属于该阶段的上下文能力；但底层程序实现共享统一的表达式求值框架，降低维护成本并保证扩展一致性。

---

# 七、最终结论

所以，重新思考后的结论不是：

- “四套条件系统应该硬合并”  
也不是
- “四套系统完全独立，各自发展”

而是：

## 正确方案
**统一表达式内核，分化上下文，保留阶段门面。**

---

# 八、给你的最终落地建议

如果你要把这份文档修成更稳的版本，我建议你做这三件事：

### 1. 先补“运行时上下文”章节
明确：
- `SenseContext`
- `ThinkContext`
- `ActContext`
- `AIBehaviorCandidate`

### 2. 再补“表达式系统原则”章节
明确：
- 统一求值范式
- 分阶段可见性
- 阶段 DSL 只是门面

### 3. 重构现有条件/数值接口
按下面结构收敛：

- Sense：
  - `SenseTargetSelector`
  - `SenseFloatValue`
  - `SenseCondition`

- Think：
  - `ThinkActor`
  - `ThinkLocation`
  - `ThinkFloatValue`
  - `PreCondition`
  - `ScoreValue`

- Act：
  - `AITargetSelector`
  - `AIFloatValue`
  - `AICondition`

- Abort：
  - `AbortActor`
  - `AbortFloatValue`
  - `AbortCondition`

这是我认为最稳、最符合你整个设计哲学的一套方案。

如果你愿意，我下一步可以直接帮你把这部分**改写成正式文档版本**，按你的文风输出成一节完整的《Runtime Context & Expression Layer》。