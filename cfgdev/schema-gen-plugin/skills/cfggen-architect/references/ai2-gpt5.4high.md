下面我分两部分回答：

1. **先从设计思想和工程落地角度，评估你这份文档的优点与需要调整的地方。**
2. **再给出一版重构后的完整 AI 行为系统设计文档。**

---

# 一、对原设计的评估与修改建议

先说结论：**这份设计的方向是对的，而且整体质量已经很高**。  
尤其是下面这几个核心判断，我认为是非常成熟的：

## 这份设计里最值得保留的部分

### 1) Sense / Think / Act 分治是正确的
这是整套设计最强的骨架。  
你已经明确把：

- 感知：我看到了什么
- 决策：我应该干什么
- 执行：我具体怎么做

拆开了。这个拆法比很多把“条件判断、目标选择、执行细节”全塞进行为树里的方案强太多，后续无论是调试、扩展还是与 GAS 协作都会更稳定。

---

### 2) 扁平化算分替代大层级行为树，是对工业项目更友好的
你已经非常明确地站在了 **Selector/Utility 风格** 而不是传统 BT 风格，这一点我很认同。

原因很简单：

- 行为树适合描述流程
- 但不适合做大规模“当前最优行为选择”
- 扁平候选 + 动态算分，更适合复杂战斗 AI、Boss、多状态 NPC

尤其你引入：

- `preConditions`
- `score`
- `interruptPriority`
- `minCommitmentTime`
- `isInterruptible`

这套机制，已经很接近一个可落地的现代行为选择器了。

---

### 3) “Abort” 与 “Task Failed” 分流，非常好
你已经把两类中断区分出来了：

- **外部环境失效**：Abort
- **内部执行失败**：Failed

这是非常重要的设计，不然系统会把“目标死了”和“寻路失败了”混成一类，导致调试非常痛苦。

---

### 4) 行为修饰器挂在 GAS/Status 生命周期上，是一个对的工程选择
这点非常关键。  
`AIBehaviorModifier` 不由 AITask 自己 Add/Remove，而是交给 GAS 状态体系托管，这样能天然解决：

- 状态残留
- 生命周期漂移
- 战斗重置不干净
- Boss 转阶段卸载不完整

这是一种很成熟的“作用域即生命周期”思路。

---

### 5) 你对“白盒调试”的重视是正确的
很多 AI 设计只关心“能不能跑”，不关心“怎么调”。  
而工业项目里，**不能调的 AI 等于不能用**。

你文档里反复强调：

- 行为是否准入
- 为什么分数高
- 为什么被拦截
- 为什么失败
- 当前栈在跑什么

这个方向完全正确，应该继续加强。

---

## 需要调整的核心问题

下面说重点：  
原文档的问题，不在于“少几个字段”，而在于**有些概念还没彻底分层，几处边界还不够稳，工程化后容易膨胀**。

---

## 1) `AIGoal` 现在承担了太多职责，建议拆成“工作记忆中的可行动项”
当前文档里，Goal 既像：

- 感知产物
- 记忆单元
- 决策输入
- 目标绑定载体

这会导致一个问题：  
**Goal 的语义太宽了**。  
比如“听到枪声的位置”“当前敌人”“可用掩体”“被打断后应闪避”这些东西，虽然都能叫 Goal，但它们实际上不完全同类。

### 建议
不要推翻 Goal 概念，但要把它明确成：

> **AIGoal 是工作记忆里可直接参与行为选择的‘可行动候选项’。**

也就是说：

- 原始感知事件是输入
- Goal 是经过整理、归一化、可参与决策的运行时实体

这样语义就稳了。

---

## 2) 条件/算分/目标选择接口碎片化了，建议统一成一套“类型化表达式系统”
原文里有：

- `PreCondition`
- `ScoreValue`
- `AbortCondition`
- `AICondition`
- `SenseCondition`
- `GoalValidator`
- `AIFloatValue`
- `AITargetSelector`

这些概念本质上都在做一件事：

> **根据上下文求值**

如果每类都单独做接口，短期看清晰，长期一定爆炸：

- 编辑器要做很多套节点
- 运行时要维护很多求值器
- 调试器要重复实现一堆显示逻辑
- 新增一个“距离”节点，要接好多地方

### 建议
统一成一套 **强类型表达式系统**：

- `BoolExpr`
- `FloatExpr`
- `ActorExpr`
- `LocationExpr`
- `TargetExpr`

然后：

- precondition 用 `BoolExpr`
- score 用 `FloatExpr`
- abort 用 `BoolExpr`
- validator 用 `BoolExpr`
- task 参数用 `ActorExpr/LocationExpr/TargetExpr`
- wait duration 用 `FloatExpr`

这会让系统一下子变得更干净。

---

## 3) `requiredGoals` 太粗，缺少“候选绑定”设计
这是我认为原文档里最需要补强的地方之一。

现在 `requiredGoals: list<goalDef>` 的意思是：

- 这个行为需要哪些 goal 类型

但**没有定义如何从 activeGoals 里选出哪一个具体 Goal 实例**。  
尤其当行为需要多个目标时，问题更明显：

- 需要“敌人 + 掩体”
- 需要“敌人 + 可落点”
- 需要“受伤盟友 + 安全位置”

如果不定义候选绑定过程，后面所有 score 都会变得含糊。

### 建议
把 `requiredGoals` 升级成显式的 **GoalSlot / Binding** 机制：

- 行为声明自己需要哪些“槽位”
- 每个槽位从哪类 goal 里选
- 用什么规则给该槽位排序
- 每个槽位取前 N 个
- 最后生成有限个绑定组合，逐个算分

这样“行为选择”选中的其实不是某个行为定义，而是：

> **一个行为定义 + 一组具体绑定目标 = 一个候选行为实例**

这会极大提升可解释性和调试性。

---

## 4) 当前 Brain 运行时结构过度偏向战斗实体，建议改成通用工作记忆
原文里的：

- `currentCombatTarget`
- `lastKnownPosition`

这些字段很常见，但它们是**具体游戏语义**，不应该直接焊死在大脑核心结构里。  
否则系统会天然偏战斗 AI，不适合 NPC、宠物、机关、潜行敌人等更多类型。

### 建议
把 Brain 改成：

- `goals`
- `actorSlots`
- `locationSlots`
- `scalarSlots`

即一个**通用工作记忆**。

然后再约定一些标准槽位，比如：

- `Memory.Actor.CombatTarget`
- `Memory.Location.LastKnownEnemyPos`

这样既保留便利性，也不会把运行时核心做死。

---

## 5) 感知刷新缺少“合并 / 去重 / 时效”规则
当前 GoalGenerator 能生成 Goal，但没有明确：

- 相同事件如何刷新
- 相同目标如何去重
- TTL 如何续命
- magnitude 如何叠加
- 超过上限如何淘汰

这在工程里非常关键。否则 AI 的 `activeGoals` 很快就会变成垃圾场。

### 建议
给 GoalDefinition 增加运行规则，例如：

- `ttl`
- `mergePolicy`
- `maxInstances`

例如：

- 按 actor 合并
- 按位置格子合并
- 刷新时长
- 强度叠加
- 超过上限时丢最旧/最弱

---

## 6) “每帧全量算分”在工程上太贵，建议改成“脏标记 + 节流重思考”
原文中完整的五阶段 Tick 很清晰，但如果严格每帧：

- 校验全部 Goal
- 扫全部行为
- 绑定全部候选
- 算全部 Score

那在复杂战斗里会非常贵。

### 建议
引入：

- `thinkInterval`
- `decisionDirty`

规则改成：

- **执行层** 每帧 Tick
- **全局硬控** 每帧检查
- **局部 Abort** 每帧检查
- **感知** 按 generator interval / event 更新
- **决策重选** 仅在到达 thinkInterval 或 dirty 时进行
- **高优抢占** 可保留每帧快速检查

这会更适合真实项目。

---

## 7) 打断防抖只提了 inertia，但没有形成完整“滞回”设计
你已经提到了 `scoreInertia`，这是对的，但不够。

在工业落地时，建议把防抖做成完整的三件套：

1. `commitmentMinTime`
2. `stayBonus`
3. `switchThreshold`

即：

- 最短承诺时间内不许切
- 当前行为重复竞标时加分
- 新行为必须超过一定阈值才可切换

这样抖动会明显减少。

---

## 8) 异步任务的取消契约还不够明确
`MoveTo`、`CastAbility`、`PlayAnimation` 这些任务都不是“一帧完成”的。  
所以执行系统真正难的地方不是 Tick，而是：

- 被打断时如何取消
- 取消后如何清理句柄
- 如果底层动作不可取消怎么办
- `onEnd(wasAborted)` 的语义边界是什么

### 建议
明确所有异步任务都必须实现统一契约：

- `onEnter`
- `tick`
- `onExit(reason)`

其中 `reason` 至少区分：

- Success
- Failed
- Aborted
- Replaced

并规定每个 task 对移动系统 / 动画系统 / GAS 请求都要持有句柄并可清理。

---

## 9) Group 只是共享条件还不够，建议加入“挂载点 / 编译扁平化”概念
你现在的 Group 已经说了“仅用于组织和共享条件”，方向没问题。  
但如果要支持长期扩展，建议更进一步：

- Group/Set 只作为**作者组织结构**
- 运行时完全扁平化
- Modifier 不直接引用具体 group 名称，而是引用 **mount tag / 挂载点标签**

原因是：  
**Modifier 如果直接指向具体 Group 资源，会让基础资源重构变得很脆。**

### 建议
引入：

- `ai_behavior_set`
- `mountTags`

Modifier 通过挂载点注入，而不是耦合到某个具体资产名。

---

## 10) 文档中“转阶段霸体”的正确实现，应该完全依赖 GAS 免控，不要给 AI 隐式豁免
原文示例里有一个很好的思想：

> 转阶段演出要安全，不要被硬控打断

这是对的。  
但这个安全性应该来自 **GAS 状态免控**，而不是 AI 层引入“忽略全局中止”的后门。

### 建议
保持一条原则：

> **AI 不拥有绕过全局硬控的特权。**
> 只有 GAS 标签系统可以决定某个单位此刻是否会被眩晕/冻结/击飞中断。

这样“单一事实来源”才真正成立。

---

## 11) 还需要补一块：编译期校验与调试快照
这类系统真正能否维护，不只看运行时，还看工具链。

建议明确加入：

### 编译期校验
- BehaviorSet 循环引用
- TaskRef 循环引用
- Modifier 注入目标不存在
- 行为需要的 Goal 没有任何 Generator 可能产出
- 表达式类型错误
- 不可达行为警告

### 运行时调试快照
- 本次 think 所有候选
- 每个候选绑定了哪些 goal
- 哪个 precondition 失败
- score 原始值/加成值/最终值
- cooldown 剩余
- 当前任务栈
- 最近 abort reason / fail reason

---

---

# 二、重构后的完整设计文档（新版）

下面给出一版我建议的**重构版设计文档**。  
这不是逐字段修修补补，而是基于你原本的思想，做了一次更适合工程落地的重组。

---

# AI 行为系统设计 v2

本设计用于构建一套与能力系统（GAS / Ability System）深度协同的现代 AI 行为系统。  
系统以 **Sense - Think - Act** 为主线，以 **扁平化行为选择 + 数据驱动执行** 为核心，强调：

- 高内聚、低耦合
- 可热插拔扩展
- 可视化调试
- 工业级可维护性
- 与 GAS 的单一事实来源协同

---

## 1. 设计目标

### 1.1 核心目标
1. **将“看见什么”“想做什么”“怎么做”彻底拆开。**
2. **用扁平化候选行为 + 动态算分替代大型行为树选择逻辑。**
3. **让行为切换、失败、打断、冷却具备清晰运行时语义。**
4. **让 AI 状态修改依附于 GAS/Status 生命周期，杜绝残留状态。**
5. **保证运行时白盒可调试。**

---

## 2. 总体架构

系统分为四层：

1. **Perception / Goal Generation**
   - 从传感器、事件、空间查询中生成可行动 Goal

2. **Working Memory**
   - 存放 Goal、命名槽位、临时认知结果
   - 是 AI 的统一认知层

3. **Decision / Selection**
   - 将行为集压平成候选池
   - 为每个行为绑定具体 Goal
   - 计算准入与分数
   - 执行抢占与重选

4. **Execution / Task Stack**
   - 将被选中的行为实例推进为具体动作
   - 负责异步任务生命周期、失败冒泡、被动中止清理

---

## 3. 运行时核心

```java
class AIBrainComponent {
    Actor self;

    AIMemory memory;                        // 工作记忆
    AIBehaviorExecution activeExecution;    // 当前执行中的行为
    List<AIBehaviorModifierRef> modifiers;  // 外部注入的行为修饰
    Int2FloatMap behaviorCooldownEndTime;   // behaviorId -> endTime
    Tag2FloatMap sharedCooldownEndTime;     // shared cooldown group
    float nextThinkTime;
    bool decisionDirty;

    AIDebugSnapshot lastDecisionSnapshot;   // 调试快照
}

class AIMemory {
    List<AIGoalInstance> goals;

    Tag2ActorMap actorSlots;        // 如 Memory.Actor.CombatTarget
    Tag2Vector3Map locationSlots;   // 如 Memory.Location.LastKnownTargetPos
    Tag2FloatMap scalarSlots;       // 如 threat、alertness、homeRadius 等
}

class AIGoalInstance {
    Ai_goal_definition goalCfg;

    Actor associatedActor;
    Vector3 position;
    float magnitude;

    long sourceKey;         // 用于合并/去重
    float createTime;
    float lastRefreshTime;
    float expireTime;
}

class AICandidateBehavior {
    Ai_behavior behaviorCfg;
    Map<String, AIGoalInstance> boundGoals; // slotName -> goal
    float baseScore;
    float finalScore;
    int priorityTier;

    bool passedPreconditions;
    String rejectReason; // 调试信息
}

class AIBehaviorExecution {
    AICandidateBehavior selectedCandidate;
    AIBrainComponent brain;

    float startTime;
    Stack<AITaskRuntime> taskStack;
    Store localStorage;

    bool isFinishing;
}

abstract class AITaskRuntime<T extends AITask> {
    T taskCfg;

    abstract void onEnter(AIBehaviorExecution ctx);
    abstract TaskStatus tick(AIBehaviorExecution ctx, float dt);
    abstract void onExit(AIBehaviorExecution ctx, TaskEndReason reason);
}

enum TaskStatus { Running, Success, Failed }
enum TaskEndReason { Success, Failed, Aborted, Replaced }

class AIBehaviorModifierRef {
    Ai_behavior_modifier modifierCfg;
    int refCount;
}
```

---

## 4. 配置资产

---

### 4.1 global_ai_settings

定义全局硬规则与默认策略。

```cfg
table global_ai_settings[name] (entry="name") {
    name: str;

    // 任意一个成立即挂起 AI 大脑本帧逻辑
    suspendConditions: list<BoolExpr>; // OR 语义

    // 默认思考频率
    thinkIntervalDefault: float;

    // 高优行为是否允许每帧快速重仲裁
    emergencyRecheckEveryFrame: bool;

    // 默认防抖参数
    defaultStayBonus: float;
    defaultSwitchThreshold: float;

    // 默认失败冷却
    defaultFailureCooldown: float;
}
```

---

### 4.2 ai_archetype

每个 AI 实体的顶层入口。

```cfg
table ai_archetype[name] {
    name: str;
    description: text;

    // 感知输入
    goalGenerators: list<str> -> ai_goal_generator;

    // 基础行为集合
    behaviorRoots: list<str> -> ai_behavior_set;

    // 无合法候选时的兜底行为
    fallbackBehavior: str -> ai_behavior;

    // 可覆盖全局默认思考频率
    thinkInterval: float;
}
```

---

### 4.3 ai_goal_definition

Goal 是“工作记忆中的可行动项”，不是原始感知事件。

```cfg
table ai_goal_definition[name] {
    [goalId];
    name: str;
    goalId: int;
    description: text;

    // 默认存活时长
    ttl: float;

    // 合并策略
    mergePolicy: GoalMergePolicy;

    // 同类 Goal 的最大保留数
    maxInstances: int;

    // 持续合法性校验，AND 语义
    validConditions: list<BoolExpr>;
}

enum GoalMergePolicy {
    RefreshByActor;      // 同 actor 刷新
    RefreshByLocation;   // 同位置/格子刷新
    ReplaceAll;          // 新 Goal 覆盖旧 Goal
    StackMagnitude;      // 强度叠加
    AllowDuplicates;     // 允许重复
}
```

---

### 4.4 ai_goal_generator

GoalGenerator 负责“产出 Goal Upsert 请求”，而不是直接修改 activeGoals。  
它可以读取 **只读 memory snapshot**，用于去重、刷新、相对计算，但不能直接写运行时状态。

```cfg
table ai_goal_generator[name] (json) {
    name: str;
    description: text;
    generator: GoalGenerator;
}

interface GoalGenerator {
    // 周期查询
    struct SpatialScan {
        interval: float;
        query: SenseQuery;
        generatedGoal: str -> ai_goal_definition;
        magnitude: FloatExpr;
    }

    // 事件监听
    struct ListenEvent {
        eventTag: str ->event_definition;
        filter: BoolExpr;
        generatedGoal: str ->ai_goal_definition;
        magnitude: FloatExpr;
        durationOverride: FloatExpr; // 可选
    }

    // 基于已有记忆派生
    struct DerivedGoal {
        interval: float;
        sourceGoal: str ->ai_goal_definition;
        filter: BoolExpr;
        generatedGoal: str ->ai_goal_definition;
        magnitude: FloatExpr;
    }
}

// 具体查询类型按游戏扩展
interface SenseQuery {}
```

---

### 4.5 ai_behavior_set

行为集是**作者组织结构**，运行时会完全压平。  
它负责共享条件与挂载点，不参与树状控制流。

```cfg
table ai_behavior_set[name] {
    name: str;
    description: text;

    // 用于 modifier 注入的挂载标签
    mountTags: list<str> -> gameplaytag;

    // 共享准入条件
    sharedPreConditions: list<BoolExpr>; // AND

    behaviors: list<BehaviorEntry>;
    children: list<str> -> ai_behavior_set;
}

struct BehaviorEntry {
    behavior: str -> ai_behavior;
    scoreBias: float; // 可选额外偏置
}
```

---

### 4.6 ai_behavior

行为定义不再只声明“需要哪些 Goal 类型”，而是显式声明 **GoalSlot**。

```cfg
table ai_behavior[name] (json) {
    [behaviorId];
    name: str;
    behaviorId: int;
    description: text;

    behaviorTags: list<str> -> gameplaytag;

    // 抢占层级：0=常规行为，>0=反应/特权行为
    priorityTier: int;

    // 常规打断规则
    interruptible: bool;
    commitmentMinTime: float;

    // 防抖
    stayBonus: float;       // 当前行为再次参与竞标时加分
    switchThreshold: float; // 新行为必须超出当前多少分才允许切换

    // 冷却
    cooldown: CooldownPolicy;

    // 目标绑定
    goalSlots: list<GoalSlot>;
    maxBindingCandidates: int;

    // 准入与算分
    preConditions: list<BoolExpr>; // AND
    score: FloatExpr;

    // 行为执行期间的本地中止条件
    localAbortConditions: list<BoolExpr>; // OR

    // 执行逻辑
    task: AITask;
}

struct CooldownPolicy {
    onSuccess: float;
    onFailure: float;
    onAbort: float;
    sharedGroupTag: str -> gameplaytag;
}

struct GoalSlot {
    slotName: str;                       // 如 "enemy", "cover"
    goal: str -> ai_goal_definition;
    optional: bool;

    // 该槽位过滤条件
    filters: list<BoolExpr>;

    // 槽位内部排序规则
    rankScore: FloatExpr;

    // 每槽取前 N 个，之后做有限组合
    topN: int;
}
```

---

### 4.7 ai_behavior_modifier

运行时动态修改决策池。  
推荐由 GAS Status/Effect 挂载与卸载。

```cfg
table ai_behavior_modifier[name] {
    name: str;
    description: text;

    // 禁用已有行为
    disableBehaviorTagsQuery: GameplayTagQuery;

    // 追加整个行为集
    addBehaviorSets: list<str> -> ai_behavior_set;

    // 按挂载点注入行为
    injections: list<BehaviorInjection>;
}

struct BehaviorInjection {
    mountTag: str -> gameplaytag;
    behaviors: list<str> -> ai_behavior;
}
```

---

### 4.8 shared_ai_task

```cfg
table shared_ai_task[name] (json) {
    name: str;
    description: text;
    task: AITask;
}
```

---

## 5. 通用表达式系统

这是新版设计的关键。  
所有“求值”统一归到强类型表达式系统。

---

### 5.1 BoolExpr

```cfg
interface BoolExpr {
    struct Const { value: bool; }
    struct And { values: list<BoolExpr>; }
    struct Or { values: list<BoolExpr>; }
    struct Not { value: BoolExpr; }

    struct Compare {
        a: FloatExpr;
        op: CompareOp;
        b: FloatExpr;
    }

    struct HasTags {
        target: ActorExpr;
        query: GameplayTagQuery;
    }

    struct ExistsGoalSlot {
        slotName: str;
    }

    struct CanActivateAbility {
        abilityId: int -> ability;
    }

    struct HasLineOfSight {
        from: ActorExpr;
        to: ActorExpr;
    }

    struct IsBehaviorOnCooldown {
        behavior: str -> ai_behavior;
    }
}
```

---

### 5.2 FloatExpr

```cfg
interface FloatExpr {
    struct Const { value: float; }

    struct Add { a: FloatExpr; b: FloatExpr; }
    struct Sub { a: FloatExpr; b: FloatExpr; }
    struct Mul { a: FloatExpr; b: FloatExpr; }
    struct Div { a: FloatExpr; b: FloatExpr; }

    struct Clamp {
        value: FloatExpr;
        min: float;
        max: float;
    }

    struct Distance {
        from: LocationExpr;
        to: LocationExpr;
    }

    struct StatValue {
        target: ActorExpr;
        statTag: str -> stat_definition;
    }

    struct GoalAge {
        slotName: str;
    }

    struct GoalMagnitude {
        slotName: str;
    }

    struct Select {
        when: BoolExpr;
        thenValue: FloatExpr;
        elseValue: FloatExpr;
    }

    struct Curve {
        input: FloatExpr;
        curve: str -> curve_asset;
    }
}
```

---

### 5.3 ActorExpr / LocationExpr / TargetExpr

```cfg
interface ActorExpr {
    struct Self {}
    struct BoundGoalActor { slotName: str; }
    struct MemoryActor { slotTag: str -> gameplaytag; }
}

interface LocationExpr {
    struct Self {}
    struct ActorLocation { actor: ActorExpr; }
    struct BoundGoalLocation { slotName: str; }
    struct MemoryLocation { slotTag: str -> gameplaytag; }
}

interface TargetExpr {
    struct Actor { actor: ActorExpr; }
    struct Location { location: LocationExpr; }
}
```

> 说明：  
> `CurrentCombatTarget`、`LastKnownPosition` 之类常用概念，建议作为标准 `MemorySlot` 约定实现，而不是硬编码在核心结构里。

---

## 6. 感知与工作记忆

---

### 6.1 Goal 生命周期
Goal 进入 Memory 后，遵循以下规则：

1. **生成**：由 Generator 发出 upsert 请求
2. **合并**：根据 `mergePolicy` 去重/刷新/叠加
3. **裁剪**：超过 `maxInstances` 时淘汰最差/最旧项
4. **验证**：每次感知更新后跑 `validConditions`
5. **过期**：超过 `expireTime` 自动剔除

---

### 6.2 记忆槽位
AI 维护通用命名槽位：

- `actorSlots`
- `locationSlots`
- `scalarSlots`

用途：

- 保存当前战斗目标
- 保存最后已知位置
- 保存警戒值、威胁值、家园点等

建议约定标准槽位标签，但不焊死在 Brain 结构中。

---

### 6.3 何时标记 decisionDirty
当以下事件发生时，设置 `decisionDirty = true`：

- Goal 被新增 / 刷新 / 移除
- Modifier 添加或移除
- 共享冷却结束
- 关键标签变化
- 关键属性变化
- 当前行为结束 / 失败 / 被打断
- `commitmentMinTime` 到期

---

## 7. 决策与行为选择

---

### 7.1 候选池构建
每次 Think 时：

1. 将 `behaviorRoots` 扁平化
2. 应用 `behaviorModifiers`
3. 继承并折叠 `sharedPreConditions`
4. 得到最终行为条目列表

运行时**不遍历 Group 树做控制流**，树只是作者组织结构。

---

### 7.2 Goal 绑定
对每个行为：

1. 对每个 `GoalSlot`，从 Memory 中取出对应 Goal 类型
2. 用 `filters` 过滤
3. 用 `rankScore` 排序
4. 每槽保留前 `topN`
5. 做有限笛卡尔组合，最多 `maxBindingCandidates`
6. 每一个组合生成一个 `AICandidateBehavior`

也就是说，真正参与竞标的是：

> **行为定义 + 一组具体目标绑定**

---

### 7.3 准入与算分
每个候选依次执行：

1. 冷却检查
2. 槽位完整性检查（非 optional 槽位必须绑定成功）
3. `sharedPreConditions`
4. `behavior.preConditions`
5. 计算 `score`

最终：

```text
finalScore = score
           + entry.scoreBias
           + (如果它是当前行为，则加 stayBonus)
```

---

### 7.4 抢占与重选规则

#### A. 特权行为（priorityTier > 0）
- 先按 `priorityTier` 选最高层
- 若最高层级 **严格大于** 当前行为层级，则立即抢占
- 若层级相同，则按 `finalScore` 比较
- 若当前行为不可打断且仍在 commitment 内，同层不切，跨层可切

#### B. 常规行为（priorityTier == 0）
仅在以下条件成立时允许重选：

- 当前无行为；或
- 当前行为 `interruptible == true` 且已度过 `commitmentMinTime`

切换条件：

```text
newScore > currentScore + switchThreshold
```

若不满足，继续执行当前行为。

---

### 7.5 Fallback 行为
当没有任何合法候选行为时：

- 若当前行为仍在执行，则继续执行
- 若当前行为为空，则启动 `fallbackBehavior`

Fallback 不参与正常竞标，只在“无解”时兜底。

---

## 8. 执行模型

---

### 8.1 基本原则
执行层只关心：

- 当前行为实例是什么
- 当前任务栈怎么推进
- 被中止时如何清理
- 执行失败时如何上报

执行层**不负责大范围重新思考**。

---

### 8.2 行为开始 / 结束
- **开始**：创建 `AIBehaviorExecution`，冻结本次选中的 boundGoals
- **成功结束**：任务栈清空，应用 `cooldown.onSuccess`
- **失败结束**：应用 `cooldown.onFailure`
- **被抢占/中止**：调用所有活跃任务 `onExit(Aborted/Replaced)`，应用 `cooldown.onAbort`

---

### 8.3 AITask 定义

```cfg
interface AITask {
    // 基础动作
    struct MoveTo {
        target: TargetExpr;
        acceptanceRadius: float;
        speedStat: str -> stat_definition;
        stopOnFinish: bool;
    }

    struct CastAbility {
        abilityId: int -> ability;
        target: TargetExpr;
        waitForCompletion: bool;
        failIfBlocked: bool;
    }

    struct PlayAnimation {
        animName: str;
        blendInTime: float;
        waitForCompletion: bool;
    }

    struct DoEffect {
        effect: Effect;
        target: TargetExpr;
    }

    struct Speak {
        dialogueId: int -> dialogue;
        target: TargetExpr;
    }

    struct Interact {
        target: TargetExpr;
        interactionId: int;
    }

    struct Wait {
        duration: FloatExpr;
    }

    struct WaitUntil {
        condition: BoolExpr;
        timeout: float;
        failOnTimeout: bool;
    }

    // 控制流
    struct Sequence {
        tasks: list<AITask>;
    }

    struct Parallel {
        tasks: list<AITask>;
        policy: ParallelPolicy;
    }

    struct If {
        condition: BoolExpr;
        thenTask: AITask;
        elseTask: AITask;
    }

    struct Loop {
        iterations: int; // -1 为无限
        task: AITask;
    }

    struct ReturnSuccess {}
    struct ReturnFailed {}

    // 局部变量
    struct WithLocalVar {
        varTag: str -> gameplaytag;
        value: FloatExpr;
        task: AITask;
    }

    // 引用共享任务
    struct TaskRef {
        sharedTask: str -> shared_ai_task;
    }
}

enum ParallelPolicy {
    WaitAll;
    WaitAny;
}
```

---

### 8.4 异步任务契约
所有异步任务（移动、施法、动画、交互）必须满足：

- `onEnter` 时申请外部系统句柄
- `tick` 时轮询状态
- `onExit` 时释放句柄或发送取消请求

这样系统才能可靠处理中断、抢占、死亡、场景重置。

---

### 8.5 Abort 与 Failed 的边界
- **Abort**：环境不再允许继续，比如目标死了、距离超限、硬控生效
- **Failed**：动作自己没做成，比如寻路失败、技能被 GAS 拦截、等待超时

Abort 是战略性中止，Failed 是执行性失败。

---

## 9. 与 GAS 的协同原则

---

### 9.1 单一事实来源
所有“是否受控、是否免控、是否死亡、是否转阶段状态激活”  
都应以 **GAS Tag / Status** 为唯一事实来源。

AI 不应私自维护一份平行真相。

---

### 9.2 三类中断协作

| 类型 | 来源 | 决策者 | 机制 |
|---|---|---|---|
| 硬控 / 死亡 | GAS | GAS | 命中 `suspendConditions`，AI 本帧挂起 |
| 战术中断 | AI | AI Selector | `localAbortConditions` / `priorityTier` 抢占 |
| 阶段切换 | AI 决策 + GAS 托管 | AI 选行为，GAS 持久化 | 行为执行演出，结束后挂载 Phase Status + Modifier |

---

### 9.3 关键原则
**不存在 AI 层“忽略全局硬控”的后门。**  
如果某次演出必须霸体安全，正确做法是：

- 行为先通过 `DoEffect` 给自己挂临时免控 Status
- GAS 通过 `blockTags / immunity` 拦截控制标签
- AI 只感知结果，不绕规则

---

## 10. Tick 流程

推荐的 Tick 顺序如下：

### 1. 全局挂起检查
先检查 `global_ai_settings.suspendConditions`：

- 若命中：
  - 若当前有执行行为，强制 Abort 并清栈
  - 本帧直接 return

---

### 2. 感知与记忆维护
- 执行到期的 Generator
- 消费 EventBus 事件
- 处理 Goal upsert / merge
- 清理过期 Goal
- 执行 Goal `validConditions`

若 Memory 发生变化，则 `decisionDirty = true`

---

### 3. 当前行为局部中止检查
若存在 `activeExecution`，检查其 `localAbortConditions`：

- 任意一个成立：
  - 清理任务栈
  - 应用 `cooldown.onAbort`
  - activeExecution 置空
  - `decisionDirty = true`

---

### 4. 决策重选
当满足以下任一条件时触发 Think：

- `Time >= nextThinkTime`
- `decisionDirty == true`
- 存在更高优层级的快速抢占需求

执行：

1. 构建有效行为池
2. 生成候选绑定
3. 准入过滤
4. 计算分数
5. 执行层级仲裁与防抖
6. 必要时切换 activeExecution

---

### 5. 执行推进
若存在 `activeExecution`：

- Tick 当前栈顶任务
- `Running`：维持
- `Success`：出栈继续
- `Failed`：整行为失败结束，应用 `cooldown.onFailure`

若行为所有任务完成，则行为成功结束，应用 `cooldown.onSuccess`

---

## 11. 调试与工具链

---

### 11.1 运行时调试面板至少应显示
1. 当前 Memory Goals 列表
2. 当前命名槽位
3. 当前行为与执行时长
4. 当前任务栈
5. 本次 Think 的全部候选行为
6. 每个候选的：
   - bound goals
   - precondition 结果
   - score 明细
   - cooldown 状态
   - reject reason
7. 最近一次 Abort / Failed 原因

---

### 11.2 编译期校验
加载配置时应校验：

- BehaviorSet 是否循环引用
- TaskRef 是否循环引用
- Modifier 的 mountTag 是否能命中任何行为集
- 行为 goalSlots 是否存在可用 GoalDefinition
- 表达式类型是否合法
- ability / stat / tag / dialogue 引用是否存在

---

### 11.3 编译优化
建议在资源加载阶段完成：

- BehaviorSet 扁平化
- 共享条件继承折叠
- 表达式编译
- 引用解析
- 常量折叠

运行时只做求值，不做结构性分析。

---

## 12. 性能与工程约束

1. **Act 每帧，Think 节流**
2. **GoalSlot 组合必须有上限**
3. **Score 建议归一化到固定区间**
4. **Generator 尽量事件驱动，少做大范围每帧扫描**
5. **AI 通常应运行在服务器侧**
6. **调试快照允许按需采样，避免正式服高开销**

---

## 13. 示例：Boss 狂暴转阶段

### 行为定义思路
`Boss_Enter_Enrage`

- `priorityTier = 90`
- `preConditions`
  - HP <= 30%
  - 自身不带 `State.Phase.Enrage`
- `task`
  1. `DoEffect(Self, Status_TempSuperArmor)`
  2. `PlayAnimation("Roar_Enrage", waitForCompletion=true)`
  3. `DoEffect(Self, Status_Enrage)`
  4. `ReturnSuccess`

### Status_Enrage 负责
- 提升攻击属性
- 挂持续特效
- 添加 `State.Phase.Enrage`
- 附带 `AIBehaviorModifier`
  - 禁用一阶段普通行为
  - 注入二阶段技能组

### 关键点
转阶段是否可被控制打断，不由 AI 自己决定。  
如果这段演出必须安全，就让 `Status_TempSuperArmor` 在 GAS 层提供免控。  
AI 只负责“决定什么时候转”，不负责“篡改物理法则”。

---

# 三、总结

如果一句话总结这次修改：

> **原文档的核心思想是对的，但需要把“Goal 绑定、统一表达式、工作记忆、行为集挂载点、异步任务契约、节流重思考、编译期校验”这几块补齐，系统才会真正从“好想法”变成“可长期维护的工业级方案”。**

如果你愿意，我下一步可以继续帮你做两件事中的任意一种：

1. **把这版 v2 文档继续细化成“可直接给程序实现的详细接口文档”**
2. **基于这版 v2，再给你写一套“示例配置资产”**  
   比如：
   - 近战哥布林
   - 远程弓手
   - 巡逻 NPC
   - Boss 两阶段转场

如果你要，我建议下一步直接做第 2 个，会更容易验证这套设计是否真的顺手。