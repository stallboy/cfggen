

# 场景逻辑系统设计评审

---

## 一、架构层面做得非常好的地方

### 1. "编排层，不是逻辑层"——这是整份文档最重要的定位决策

场景系统最容易犯的错误就是膨胀成"第二个战斗系统"——自己搞一套伤害公式、自己管属性修改、自己维护状态机。你明确拒绝了这条路，场景只做编排，战斗逻辑复用 GAS Effect，AI 逻辑复用 AI Behavior，表现走 Cue。

这意味着当战斗公式修改时，场景脚本不需要跟着改；当 AI 行为调优时，场景不需要知道细节。这个边界感决定了这套系统的长期可维护性。

### 2. 作用域即生命周期 + finally 语义

`TakeActorControl`、`WithStatus`、`WithVar` 这组作用域节点的设计，是这份文档最有工程价值的部分。

场景系统在工业项目中产出的 bug 里，至少有一半是状态残留：控制权没归还、临时 buff 没移除、变量没清理。你用"作用域节点的 onExit 在 finally 语义下必定执行"这一条规则，从架构层杜绝了这整类问题。

更关键的是，当 `Parallel(WaitAny)` 的一个分支成功导致另一个分支被 abort 时，abort 调用链会逆序清理所有 Running 子节点的作用域。这个嵌套场景是很多场景系统处理不好的，你的实现是正确的。

### 3. 执行树而非执行栈——对 Parallel 语义的正确选择

文档明确指出执行栈无法自然表达并发语义，这个判断是对的。栈模型下 Parallel 只能伪装成"驻留栈顶的特殊节点，内部维护一组子任务"，这在概念上不干净，在 abort 清理上容易出错。

树模型下，Parallel 和它的 children 共存于树中，父节点驱动子节点 tick，abort 时逆序遍历——概念清晰，实现稳健。

### 4. AwaitMode 的显式声明

把"这个动作是发完就走还是等完成"从隐式约定提升为显式配置字段，解决了 ECA 时代最经典的一类 bug：`Wait 3.0` 与动画实际时长不匹配。

配合订阅完成事件（而非硬编码等待时长）的实现方式，这个设计是扎实的。

### 5. Outcome 统一终止模型

把超时、失败、胜利全部统一为 `outcomes` 列表，用 `condition + priority + terminateScene` 组合表达，比分设 `abortConditions / timeLimit / winCondition` 三套独立机制干净得多。特别是 `terminateScene: false` 支持的"记录但不终止"模式，为多目标评分场景留出了优雅的空间。

---

## 二、架构层面需要重新审视的问题

### 问题1：`reactToEvents` 的正确性负担被低估了

这是整份文档最需要重新思考的架构决策。

你的 Philosophy 第3条是"订阅驱动，非轮询"，要求策划在 `WaitUntil` 和 `SceneOutcome` 上显式声明 `reactToEvents`。这个方向本身没错，但文档对它的代价估计不足。

**核心矛盾**：条件可能是复合的，而事件依赖是隐式传递的。

考虑这个条件：

```
And(
    ActorStatCompare(MagmaLord, HP, <= 0.3),
    ActorHasTags(MagmaLord, "State.Phase1")
)
```

策划需要列出所有可能改变这两个子条件的事件。HP 变化来自 `Event.Combat.Damage.Post`，Tag 变化来自 `Event.Status.Applied` 或 `Event.Status.Removed`。漏填任何一个，条件就可能在应该触发时沉默不触发——而且这类 bug 极难调试，因为"什么都没发生"不会留下日志。

**更深层的问题**：当条件依赖链变深时，策划需要理解整个数据流。比如 HP 被一个 Status 的 StatModifier 间接修改，那 `reactToEvents` 应该填 `Event.Status.Applied` 还是 `Event.Combat.Damage.Post`？

**建议**：Philosophy 应当对这个权衡做更诚实的表态。我建议的方向是：

- 将 `reactToEvents` 重新定位为**性能优化手段**，而非正确性要求
- 默认行为应当是安全的（能正确触发），性能优化是可选的
- 提供两种策略让具体游戏选择：

```
策略A（保守）：条件默认每帧检查，reactToEvents 是可选优化提示
策略B（激进）：条件默认不检查，必须声明 reactToEvents（当前方案）
```

并在文档中明确说明策略B的适用前提：**团队有能力保证事件声明的完备性，且有工具链辅助校验**。

或者从技术层面提供一个折中方案：**Dirty Flag 模式**。底层系统在 Stat 变化或 Tag 变化时自动标记相关条件为 dirty，下一帧重新评估。这样策划不需要手动枚举事件，系统自动追踪依赖。这比每帧轮询高效，又比手动声明安全。

### 问题2：Script 完成与 Outcome 触发的交互模型存在歧义

当前流程：

```
每帧 tick:
  1. checkTerminatingOutcomes()  → 可能 abort + finalize
  2. rootContext.tick()           → 可能自然完成 → finalize
```

这引出几个未说清的问题：

**场景 A**：脚本自然走完（所有 action succeeded），但没有任何 outcome 的条件匹配。发生什么？

看代码：`finalize(Succeeded)` 调用 `pickHighestPriorityOutcome(Succeeded)`。如果没有匹配的 outcome，返回 null，场景就"无结局地结束了"。这是否合法？应不应该要求至少有一个 outcome 一定会匹配？

**场景 B**：脚本还在 Running，一个 `terminateScene: true` 的 outcome 触发了。abort 清理完作用域后，`finalize(Failed)` 被调用。此时 `pickHighestPriorityOutcome` 选的是触发 abort 的那个 outcome，还是所有已匹配 outcome 中 priority 最高的？

**场景 C**：两个 `terminateScene: true` 的 outcome 在同一帧同时满足条件。选哪个？

这些不是实现细节——它们是**语义模型**的一部分。文档应该明确阐述：

> Outcome 的匹配是**持续**的：每当 reactToEvents 中的事件触发时，系统重新评估所有 outcome 的 condition。已匹配的 outcome 被记录到 `matchedOutcomes` 列表中。
> 
> 当需要终止场景时（无论是 `terminateScene: true` 的 outcome 命中，还是脚本自然完成），系统从 `matchedOutcomes` 中选取 `priority` 最高的那一个作为最终结局。如果没有任何匹配的 outcome，场景以 `null` 结局结束——具体游戏应视需要添加一个兜底 outcome。

### 问题3：缺少对场景脚本容错/恢复的架构表态

当前的错误传播模型是严格的：叶子节点 Failed → 父级 Sequence Failed → 可能一路冒泡到根节点 → 整个场景 Failed。

但场景脚本和战斗逻辑有本质区别——**场景不应该因为一个非关键动作的失败而整体崩溃**。

例如：
- `CastAbility` 失败（技能被 CD 挡住）→ Sequence Failed → 整段演出中止？
- `PlayAnimation` 目标实体已被销毁 → 场景崩溃？
- `Dialogue` 被玩家跳过 → Failed 还是 Succeeded？

文档应该在 Philosophy 层面对此表态。有两个方向：

**方向A**：提供 `Fallback` / `Try` 节点，让策划显式处理失败。

```cfg
struct Try {
    body: SceneAction;
    fallback: SceneAction;  // body Failed 时执行
}
```

**方向B**：让部分节点支持"软失败"策略——失败时记录日志但返回 Succeeded，不中断流程。

无论选哪个方向，这个问题都应该在架构层面有所表态，否则每个落地团队会各自发明不同的容错方案。

### 问题4：共享脚本（ScriptRef）缺乏参数化能力

`shared_scene_script` 目前只是一段可复用的 SceneAction，没有参数传递机制。但实际使用中，共享脚本最大的价值在于参数化复用：

- "通用 Boss 入场演出"需要知道"哪个槽位是 Boss"
- "通用宝箱开启"需要知道"奖励是什么"
- "通用对话流程"需要知道"对话 ID 是多少"

没有参数化，共享脚本的复用能力会大打折扣。策划要么不用 ScriptRef，要么通过约定场景变量名来做隐式参数传递（这非常脆弱）。

**建议**：在 `ScriptRef` 上增加参数绑定机制。概念上可以复用 `WithVar` 的思路：

```cfg
struct ScriptRef {
    sharedScriptId: int ->shared_scene_script;
    args: list<ScriptArg>;
}

struct ScriptArg {
    varTag: str ->gameplaytag;   // 共享脚本内部通过这个 tag 读取
    value: SceneArgValue;        // 调用方绑定的值
}

interface SceneArgValue {
    struct FloatConst { value: float; }
    struct SlotRef { slotTag: str ->gameplaytag; }
    struct VarRef { varTag: str ->gameplaytag; }
}
```

这样"通用 Boss 入场演出"就可以声明"我需要一个 `Param.BossSlot` 参数"，调用方绑定具体的槽位 tag。

### 问题5：场景与场景之间的关系未建模

当前设计中，每个 `scene_definition` 是完全独立的。但实际游戏中，场景之间存在大量关系：

- **嵌套**：大副本场景包含多个小 Boss 战场景
- **链式**：完成场景 A 后自动触发场景 B
- **中断与恢复**：过场动画（场景）打断战斗（另一个场景），结束后恢复

文档不需要给出完整的场景编排方案，但应该在 Philosophy 层面明确**这份设计的边界**：

> 本设计定义的是**单个场景内部**的编排逻辑。场景之间的编排（触发链、嵌套、中断恢复）属于更上层的关卡/任务系统的职责，不在本文档范围内。场景通过 `broadcastEvent` 向外部系统输出结果，外部系统通过 `trigger` 或显式调用激活新场景。

这一句话就够了，但它必须说出来，否则读者会困惑"两个场景如何协作"。

### 问题6：`WaitSeconds` 被不公正地贬低了

文档将 `WaitSeconds` 定义为"降级方案，建议优先使用 WaitForEvent"。这个态度有问题。

ECA 时代的真正问题不是 `Wait 3 seconds` 本身，而是**用 `Wait 3 seconds` 代替"等待动画完成"**。当动画时长改了但 Wait 没跟着改，就出 bug。

但纯粹的时间等待在场景编排中是完全合法的需求：

- 转场后停顿 1 秒营造紧张感
- Boss 嘶吼后留 2 秒给玩家反应时间
- 镜头切换后保持 3 秒供玩家观察

这些等待的语义就是"等一段时间"，不是某个事件的代理。把它标记为降级方案会误导策划。

**建议**：去掉"降级方案"的措辞。Philosophy 应该表达的是：

> 当等待的语义是"某个动作完成"时，必须使用事件订阅（AwaitMode.UntilComplete），禁止用硬编码时长替代。当等待的语义确实就是"一段时间"时，WaitSeconds 是正确的选择。

---

## 三、Philosophy 层面的建议

### 补充原则：明确声明设计边界

建议增加第 7 条 Philosophy：

> **7. 单场景原子性**：
> 本系统定义单个场景内部的编排逻辑。场景对外是原子的——它有明确的开始、结束和结局。场景之间的编排（链式触发、嵌套、中断恢复）属于上层系统的职责。场景通过 outcome 的 broadcastEvent 输出结果，外部系统通过 trigger 或显式调用控制场景生命周期。

### 修正原则3的措辞

当前："所有等待节点在 onEnter 时向 EventBus 注册监听...条件的重新评估由事件推送触发，而非每帧主动求值。"

建议改为：

> **订阅驱动优先**：
> 等待节点（WaitForEvent、WaitUntil）优先采用 EventBus 订阅机制，在事件触发时重新评估条件。策划通过 reactToEvents 声明可能导致条件变化的事件源。对于无法穷举事件源的复杂条件，系统退化为定频轮询（而非每帧），并在编辑器中提示策划优化。

### 修正原则5关于 WaitSeconds 的暗示

当前："如需等待完成，必须在配表中显式声明 await: UntilComplete...这直接解决了 ECA 时代 Wait 3.0 seconds 与动画时长脱节的顽疾。"

这段话本身没问题，但不应该让读者推导出"WaitSeconds 是错误的"。

---

## 四、总结

| 层面 | 评价 |
|------|------|
| 架构定位 | ✅ "编排层不是逻辑层"是最正确的决策 |
| 生命周期管理 | ✅ 作用域 + finally 语义是工业级方案 |
| 执行模型 | ✅ 树模型对并发语义的处理是正确的 |
| 与 GAS/AI 集成 | ✅ 控制权双模式设计成熟 |
| 事件驱动策略 | ⚠️ reactToEvents 的正确性负担需要更诚实的表态 |
| 终止/结局模型 | ⚠️ Script 完成与 Outcome 的交互语义需要显式说明 |
| 容错能力 | ⚠️ 缺少对失败恢复的架构表态 |
| 复用能力 | ⚠️ 共享脚本缺乏参数化，复用受限 |
| 设计边界 | ⚠️ 未明确声明不处理场景间编排 |

**一句话**：这是三份设计文档中工程实操感最强的一份。作用域生命周期管理、AwaitMode、控制权双模式这几个设计可以直接落地。需要补的主要是几个语义模型的显式说明，以及对 reactToEvents 这个核心决策的权衡表态。



# 场景逻辑系统设计 (Scene Logic System Design)

本文档定义一套与 `ability-design` 和 `ai-design` 无缝集成的场景逻辑系统。它解决的核心问题是：**如何用数据驱动的方式，编排多实体在一段时间内的协同行为序列，并与 GAS 和 AI 系统深度联动**。

## Philosophy

1. **复用，不重造**：
   `SceneAction` 的执行单元直接调用 GAS 的 `Effect`、`Ability`，AI 的行为修饰通过 `AIBehaviorModifier` 完成。场景系统不引入平行的战斗逻辑体系，只做编排。

2. **执行树，非执行栈**：
   所有动作节点构成一棵 **ActionContext 树**，复合节点（`Sequence`、`Parallel`）与子节点共存于树中，每帧只 tick 处于 Running 状态的节点。这使 `Parallel` 的并发语义可以被自然表达，而执行栈天然无法做到这一点。

3. **依赖追踪优先，显式订阅可选**：
   等待节点（`WaitUntil`）和结局条件（`SceneOutcome`）的重新评估，默认由系统自动追踪条件所依赖的属性（Stat）和标签（Tag）变化，通过 Dirty Flag 标记在下一帧重新求值，无需策划手动枚举事件依赖。`reactToEvents` 作为可选优化保留：当条件依赖非 Stat/Tag 的自定义事件时（如 `Event.Scene.MinionDied`），策划可显式声明以获得精确的事件驱动评估。纯时间条件（`TimeSinceSceneStart`）始终由内部计时器驱动。

4. **作用域即生命周期**：
   所有有副作用的操作（占用 Actor 控制权、挂载临时 Status、声明局部变量）都必须包装在对应的作用域节点中。作用域节点的 `onExit` 在 `finally` 语义下执行——无论正常完成还是被外部中止，清理动作一定发生，从根本上杜绝状态残留（Orphaned State）。

5. **await 语义显式声明**：
   `PlayAnimation`、`Dialogue`、`MoveTo` 等动作节点默认是 fire-and-forget（发出指令立刻返回）。如需等待完成，必须在配表中显式声明 `await: UntilComplete`，引擎通过 EventBus 订阅完成事件实现，而非硬编码时长。`WaitSeconds` 适用于语义确实为"等一段时间"的场景（如转场停顿、留给玩家的反应时间），不应被用于替代动画或动作的完成等待。

6. **结局统一终止语义**：
   场景的所有终止路径（Boss 死亡、玩家死亡、超时）统一通过 `outcomes` 配置，不再设立 `abortConditions` 和 `timeLimit` 两个平行字段，避免语义重叠和重复填写。

7. **渐进容错**：
   场景编排与战斗逻辑有本质区别——一个非关键动作的失败（如对话被跳过、动画目标已销毁）不应导致整段演出崩溃。系统提供 `Try` 节点用于显式包装可失败的动作，策划可选择静默吞掉失败或执行降级方案，将容错决策留在配置层而非硬编码在引擎中。

8. **单场景原子性**：
   本系统定义单个场景内部的编排逻辑。场景对外是原子的——它有明确的开始、结束和结局。场景之间的编排（链式触发、嵌套、中断恢复）属于上层关卡/任务系统的职责，不在本文档范围内。场景通过 `outcome` 的 `broadcastEvent` 输出结果，外部系统通过 `trigger` 或显式调用控制场景生命周期。

---

## Data Foundation

本章节定义场景系统使用的原子数据类型。所有 `gameplaytag`、`stat_definition`、`event_definition` 均与 `ability-design` 共享同一张注册表，不重复定义。

### SceneCondition

`SceneCondition` 的运行时上下文是 `SceneInstance`，用于 `outcome.condition`、`WaitUntil.condition`、`Conditional.condition` 等场合。

```cfg
interface SceneCondition {
    struct And { conditions: list<SceneCondition>; }
    struct Or  { conditions: list<SceneCondition>; }
    struct Not { condition: SceneCondition; }

    // 槽位实体的属性值比较
    struct ActorStatCompare {
        actor: SceneActorSelector;
        statTag: str ->stat_definition;
        op: CompareOp;
        value: float;
    }

    // 槽位实体的 Tag 状态（复用 GAS 标签体系）
    struct ActorHasTags {
        actor: SceneActorSelector;
        tagQuery: GameplayTagQuery;
    }

    struct ActorIsAlive { actor: SceneActorSelector; }
    struct ActorIsDead  { actor: SceneActorSelector; }

    // 槽位是否已激活（处理可选参与者）
    struct SlotIsActive { slotTag: str ->gameplaytag; }

    // 场景变量比较
    struct SceneVarCompare {
        varTag: str ->gameplaytag;
        op: CompareOp;
        value: float;
    }

    // 场景已运行时长（由 SceneInstance 内部计时器驱动）
    struct TimeSinceSceneStart {
        op: CompareOp;
        seconds: float;
    }
}
```

### SceneActorSelector

`SceneActorSelector` 的运行时上下文是 `SceneInstance`，用于从场景中动态解析目标实体。

```cfg
interface SceneActorSelector {
    // 通过槽位 Tag 取单个实体（最常用）
    struct SlotActor  { slotTag: str ->gameplaytag; }

    // 取槽位内所有实体（支持多人槽）
    struct AllInSlot  { slotTag: str ->gameplaytag; }

    // 从场景变量中取（如 SpawnActor 输出后注册的动态实体）
    struct SceneVar   { actorVarTag: str ->gameplaytag; }

    // 按距离筛选（复用 ability-design 的 TargetFilter）
    struct NearestToSlot {
        referenceSlot: str ->gameplaytag;
        filter: TargetFilter;
        maxCount: int;
    }
}
```

### SceneScoreFormula

用于 `SceneOutcome` 的评分计算，支持基于时间、场景变量的动态公式。

```cfg
interface SceneScoreFormula {
    struct Const { value: float; }
    struct SceneVarValue { varTag: str ->gameplaytag; }
    struct Math { op: MathOp; a: SceneScoreFormula; b: SceneScoreFormula; }
    // 线性时间衰减：score = base - decayPerSecond * elapsedTime
    struct TimeBonusDecay {
        baseScore: float;
        decayPerSecond: float;
    }
}
```

### AwaitMode

所有有"持续时间"语义的动作节点都必须声明此枚举，这是消除 fire-and-forget 歧义的核心设计。

```cfg
enum AwaitMode {
    // 发出指令，立刻返回 Succeeded，不等待完成
    // 适用：SendEvent、Camera（镜头切换）、DoEffect
    Immediate;

    // 等待引擎广播该动作的完成事件后才返回 Succeeded
    // 引擎通过订阅对应的 Event.Actor.AnimComplete /
    // Event.UI.DialogueComplete 等实现，不轮询
    // 适用：PlayAnimation、Dialogue、CastAbility
    UntilComplete;
}
```

---

## Runtime Core

本章节定义场景系统的运行时核心数据结构，是连接静态配置与动态执行的桥梁。

### SceneInstance

场景运行时主体，由场景管理器在触发条件满足时实例化。

```java
class SceneInstance {
    scene_definition config;

    // 槽位到实体的实时映射
    // key: slotTagId, value: Actor 实例
    Int2ObjectMap<Actor>  slotMap;
    Int2ObjectMap<Actor>  anchorMap;  // 环境锚点独立存储

    // 场景局部变量（复用 GAS 的 Store 接口）
    Store sceneVars;

    // 执行树根节点
    ActionContext rootContext;

    // 当前已运行时长
    float elapsedTime = 0;

    // 已匹配的结局记录（包含 terminateScene=true 和 false 的）
    List<SceneOutcome> matchedOutcomes = new ArrayList<>();

    // Outcome 条件脏标记（由依赖追踪或 reactToEvents 设置）
    BitSet outcomeDirtyFlags;

    SceneState state = Pending;

    // 每帧由引擎调用
    void tick(float dt) {
        if (state != Running) return;
        elapsedTime += dt;

        // 1. 重新评估被标记为脏的 Outcome 条件
        evaluateDirtyOutcomes();

        // 2. 检查是否有 terminateScene=true 的 Outcome 被匹配
        SceneOutcome terminating = findHighestPriorityTerminating();
        if (terminating != null) {
            rootContext.abort();
            resolveOutcome(terminating);
            return;
        }

        // 3. Tick 执行树
        if (rootContext.status == Pending) {
            rootContext.onEnter();
            rootContext.status = Running;
        }

        ActionContext.Status s = rootContext.tick(dt);

        if (s == Succeeded || s == Failed) {
            resolveOutcome(pickBestOutcome());
        }
    }

    // 解析 SceneActorSelector 为实际 Actor
    Actor resolveActor(SceneActorSelector selector) { ... }
    List<Actor> resolveActors(SceneActorSelector selector) { ... }

    // 生成供 Effect/Ability 执行使用的 GAS Context
    Context makeContext(Actor target) {
        return new Context(
            instigator: resolveActor(SlotActor("Slot.Player")),
            causer:     null,
            target:     target,
            instanceState: sceneVars,
            ...
        );
    }

    void abort() {
        rootContext.abort();
        resolveOutcome(pickBestOutcome());
    }
}

enum SceneState { Pending; Running; Completed; Aborted; }
```

### 终止与结局裁定语义

场景终止存在两条触发路径，两者最终汇入统一的结局裁定流程：

**路径 A：Outcome 主动终止**
某个 `terminateScene: true` 的 Outcome 条件满足时，系统立即 abort 执行树，进入结局裁定。

**路径 B：脚本自然完成**
执行树根节点返回 Succeeded 或 Failed，进入结局裁定。

**结局裁定流程**：

1. 系统收集所有当前条件满足的 Outcome（包括 `terminateScene: true` 和 `false` 的全部已匹配记录）。
2. 按 `priority` 降序排列。
3. 选取 priority 最高的 Outcome 作为最终结局。
4. 执行最终结局的 `rewardEffects`，计算 `scoreFormula`，广播 `broadcastEvent`。
5. 如果没有任何 Outcome 匹配，场景以"无结局"状态结束。**具体游戏应添加一条兜底 Outcome（如 `priority: 0` 的默认结局）避免此情况**。

**同帧冲突**：如果同一帧内多个 `terminateScene: true` 的 Outcome 同时满足，取 `priority` 最高者。建议 priority 约定：死亡=100, 超时=50, 胜利=10。

### 条件依赖追踪机制

场景条件（`SceneCondition`）的重新评估采用**自动依赖追踪 + 可选显式声明**的混合策略：

**默认行为（Dirty Flag 自动追踪）**：系统在条件注册时静态分析其引用的 Stat 和 Tag 依赖，自动在相关 Actor 的 `StatComponent` 和 `TagContainer` 上注册变更监听。当依赖数据发生变化时，条件被标记为脏（dirty），下一帧重新评估。策划无需手动枚举事件，系统保证正确性。

**显式声明（reactToEvents 优化提示）**：当条件依赖非 Stat/Tag 的自定义事件（如 `Event.Scene.MinionDied`）时，策划显式声明 `reactToEvents`，系统改为精确的事件订阅驱动。

**时间条件**：`TimeSinceSceneStart` 由 `SceneInstance` 内部计时器每帧驱动，不依赖上述两种机制。

```java
// 条件依赖分析器（引擎内部）
class ConditionDependencyAnalyzer {
    // 静态分析 SceneCondition 引用的 Stat/Tag 依赖
    // 返回 (ActorSelector, StatTagId) 对的集合
    static Set<Dependency> extractDependencies(SceneCondition condition) { ... }
}

record Dependency(
    SceneActorSelector actor,
    int statOrTagId,
    DependencyType type // Stat or Tag
) {}
```

### ActionContext

执行树的节点基类，是场景系统运行时的核心抽象。

```java
abstract class ActionContext {
    SceneAction     config;
    SceneInstance   scene;
    ActionContext   parent;
    List<ActionContext> children = new ArrayList<>();

    enum Status { Pending, Running, Succeeded, Failed, Aborted }
    Status status = Pending;

    // 节点进入时调用：初始化 EventBus 订阅、发出指令
    abstract void onEnter();

    // 每帧驱动：推进执行，返回当前状态
    // 只有处于 Running 状态的节点会被 tick
    abstract Status tick(float dt);

    // 节点退出时调用：取消所有 EventBus 订阅，清理副作用
    // !! 无论正常结束(aborted=false)还是被强制中止(aborted=true)都必须调用
    abstract void onExit(boolean aborted);

    // 外部强制中止（由父节点或 SceneInstance 调用）
    final void abort() {
        if (status != Running && status != Pending) return;
        // 逆序中止所有 Running 子节点（保证清理顺序正确）
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).status == Running)
                children.get(i).abort();
        }
        onExit(true);
        status = Aborted;
    }

    // 工厂方法：根据配置类型分发到对应子类
    static ActionContext create(SceneAction cfg, SceneInstance scene, ActionContext parent) { ... }
}
```

---

## 配置定义

### scene_definition

场景定义表，是整个场景系统的最上层配置入口。

```cfg
table scene_definition[id] (json) {
    id: int;
    name: text;
    description: text;
    sceneTags: list<str> ->gameplaytag; // 如 "Scene.Type.Boss", "Scene.Type.Escort"

    // 参与者：有 AI、有战斗逻辑的实体
    actorSlots: list<SceneActorSlot>;

    // 环境锚点：无 AI，仅用于承载环境 Status 或接收 SendEvent
    // 例：竞技场中央岩浆区、石门、陷阱触发点
    environmentAnchors: list<SceneEnvironmentAnchor>;

    // 静态前置条件：加载时检查一次，不满足则该场景对当前玩家不可用
    // 例：玩家等级、前置任务完成状态
    prerequisites: list<SceneCondition>;

    // 动态激活触发器：条件满足时激活场景，开始执行 script
    // null = 由外部系统（任务系统、关卡管理器）显式调用激活
    trigger: SceneTrigger (nullable);

    // 核心脚本（主线序列）
    script: SceneAction;

    // 结局定义：统一承载所有终止条件、奖励与超时
    // 建议至少包含一条兜底 Outcome（如 priority: 0 的默认结局）
    outcomes: list<SceneOutcome>;

    // 场景变量初始化
    initVars: list<SceneVarInit>;
}

struct SceneTrigger {
    condition: SceneCondition;
    // 可选：显式声明哪些事件可能导致条件变化
    // 不填写时，系统通过 Dirty Flag 自动追踪 Stat/Tag 依赖
    reactToEvents: list<str> ->event_definition;
}

struct SceneActorSlot {
    slotTag:         str ->gameplaytag;
    aiArchetype:     str ->ai_archetype;
    spawnPoint:      str;               // 关卡编辑器中的生成点 ID
    initialStatuses: list<int> ->status;
    statOverrides:   list<SceneStatOverride>;
    // true = 该实体死亡后自动注销此槽位，SlotIsActive 返回 false
    removeOnDeath:   bool;
}

struct SceneEnvironmentAnchor {
    slotTag:         str ->gameplaytag;
    spawnPoint:      str;
    initialStatuses: list<int> ->status;
}

struct SceneStatOverride {
    statTag: str ->stat_definition;
    op:      ModifierOp;
    value:   float;
}

struct SceneVarInit {
    varTag:    str ->gameplaytag;
    initValue: float;
}

struct SceneOutcome {
    outcomeTag: str ->gameplaytag; // 如 "Scene.Result.Victory"

    // 触发条件
    condition: SceneCondition;

    // 可选：显式声明哪些事件可能导致条件变化
    // 不填写时，系统通过 Dirty Flag 自动追踪条件所依赖的 Stat/Tag 变化
    // 用于条件依赖非 Stat/Tag 自定义事件的场景
    // TimeSinceSceneStart 类条件始终由内部计时器驱动，无需填写
    reactToEvents: list<str> ->event_definition;

    // true = 匹配后立即终止场景（如失败、超时）
    // false = 记录结果但继续执行（如多目标评分场景）
    terminateScene: bool;

    // 并发匹配时的优先级（数值越大越优先）
    // 建议约定：死亡=100, 超时=50, 胜利=10, 兜底=0
    priority: int;

    rewardEffects: list<SceneRewardEntry>;
    scoreFormula:  SceneScoreFormula (nullable);
    broadcastEvent: str ->event_definition (nullable);
}

struct SceneRewardEntry {
    target: SceneActorSelector;
    effect: Effect; // 直接复用 ability-design 的 Effect 接口
}
```

---

## SceneAction 节点

`SceneAction` 是场景脚本的执行单元，构成可任意组合的节点树。每帧只 tick 处于 Running 状态的节点。

```cfg
interface SceneAction {

    // ═══════════════════════════════
    // 基础动作节点
    // ═══════════════════════════════

    // 播放角色动画
    // await: UntilComplete = 订阅 Event.Actor.AnimComplete 后才返回 Succeeded
    // await: Immediate     = 发出指令立刻返回，不等动画结束
    struct PlayAnimation {
        target:      SceneActorSelector;
        animName:    str;
        blendInTime: float;
        await:       AwaitMode;
        // 被外部 Abort 时是否强制停止动画
        stopOnAbort: bool;
    }

    // 播放对话
    // await: UntilComplete = 订阅 Event.UI.DialogueComplete 后才返回 Succeeded
    struct Dialogue {
        speaker:    SceneActorSelector;
        dialogueId: int ->dialogue;
        await:      AwaitMode;
    }

    // 镜头指令（默认 Immediate，镜头切换不阻塞逻辑）
    struct Camera {
        action: CameraAction;
        await:  AwaitMode; // 通常 Immediate
    }

    // 执行 GAS Effect（直接复用 ability-design 的 Effect 接口）
    struct DoEffect {
        target: SceneActorSelector;
        effect: Effect;
        await:  AwaitMode; // 通常 Immediate
    }

    // 让槽位实体释放 Ability
    // await: UntilComplete = 订阅 Event.Ability.Complete 后才返回 Succeeded
    struct CastAbility {
        caster:    SceneActorSelector;
        abilityId: int ->ability;
        target:    SceneActorSelector (nullable);
        await:     AwaitMode;
    }

    // 向 GAS EventBus 广播事件（始终 Immediate，不等响应者）
    struct SendEvent {
        target:   SceneActorSelector (nullable); // null = 全局广播
        eventTag: str ->event_definition;
        magnitude: float;
        extras:   list<struct { tag: str ->gameplaytag; value: float; }>;
    }

    // 在场景中生成新实体，并将其注册到指定场景变量
    struct SpawnActor {
        outputVarTag:    str ->gameplaytag;  // 生成后注册到 sceneVars，供后续节点引用
        aiArchetype:     str ->ai_archetype (nullable);
        spawnPoint:      str;
        initialStatuses: list<int> ->status;
    }

    // 修改场景变量
    struct SetSceneVar {
        varTag: str ->gameplaytag;
        op:     ModifierOp;
        value:  float;
    }

    // 显示 UI 提示（如 Boss 技能预警文字）
    struct ShowHint {
        hintKey:   str;    // i18n key
        duration:  float;
        hintStyle: HintStyle;
    }

    // ═══════════════════════════════
    // 等待节点
    // ═══════════════════════════════

    // 等待 EventBus 上的特定事件
    // 运行时：onEnter 订阅，onExit 取消订阅
    struct WaitForEvent {
        eventTag:   str ->event_definition;
        source:     SceneActorSelector (nullable); // null = 监听所有来源
        conditions: list<SceneCondition>;
        timeoutSec: float;
        // 超时后执行的备用动作（null = 静默超时，返回 Failed）
        onTimeout:  SceneAction (nullable);
    }

    // 等待条件为真
    // 运行时：onEnter 时立即评估一次；随后由依赖追踪或 reactToEvents 驱动重新评估
    struct WaitUntil {
        condition:      SceneCondition;
        // 可选：显式声明哪些事件可能改变此条件的值
        // 不填写时，系统通过 Dirty Flag 自动追踪条件所依赖的 Stat/Tag 变化
        reactToEvents:  list<str> ->event_definition;
        timeoutSec:     float;
    }

    // 纯时长等待
    // 适用于语义确实为"等一段时间"的场景：转场停顿、留给玩家反应时间等
    // 不应用于替代动画或动作的完成等待——那是 AwaitMode.UntilComplete 的职责
    struct WaitSeconds {
        duration: float;
    }

    // ═══════════════════════════════
    // 控制流节点
    // ═══════════════════════════════

    // 顺序执行：子节点依次执行，任意一个 Failed 则整体 Failed
    struct Sequence { actions: list<SceneAction>; }

    // 并行执行
    // WaitAll：等所有子节点完成，任一 Failed 则整体 Failed
    // WaitAny：任意一个 Succeeded 则整体 Succeeded，中止其余 Running 子节点
    struct Parallel {
        actions: list<SceneAction>;
        policy:  ParallelPolicy; // WaitAll | WaitAny
    }

    struct Conditional {
        condition:  SceneCondition;
        then:       SceneAction;
        else:       SceneAction (nullable);
    }

    // count = -1 无限循环，由外部 Parallel WaitAny 或 Abort 终止
    struct Loop {
        count: int;
        body:  SceneAction;
    }

    // 容错节点：包装可失败的动作
    // body Succeeded → Try 返回 Succeeded
    // body Failed    → 执行 fallback（如有），返回 fallback 的结果
    //                → fallback 为 null 时，静默吞掉失败，返回 Succeeded
    struct Try {
        body:     SceneAction;
        fallback: SceneAction (nullable);
    }

    // ═══════════════════════════════
    // 作用域节点
    // 所有有副作用需要清理的操作都必须包装在此
    // onExit 在 finally 语义下执行，保证清理一定发生
    // ═══════════════════════════════

    // Actor 控制权作用域
    // body 结束后自动归还所有 Actor 的控制权
    struct TakeActorControl {
        targets:       list<SceneActorSelector>; // 支持批量锁定
        mode:          ActorControlMode;
        softTimeoutSec: float;                   // Polite 模式的超时升级阈值
        body:          SceneAction;
        // Abort 时的清理动作（如：恢复 idle 动画），fire-and-forget
        onAbort:       SceneAction (nullable);
    }

    // Status 生命周期作用域：body 结束后精确移除该 Status 实例
    struct WithStatus {
        target:   SceneActorSelector;
        statusId: int ->status;
        body:     SceneAction;
    }

    // 场景局部变量作用域（与 GAS WithLocalVar 同构）
    struct WithVar {
        varTag:    str ->gameplaytag;
        initValue: float;
        body:      SceneAction;
    }

    // ═══════════════════════════════
    // 复用
    // ═══════════════════════════════

    // 引用共享脚本片段，支持参数化绑定
    struct ScriptRef {
        sharedScriptId: int ->shared_scene_script;
        // 将调用方的值绑定到共享脚本声明的参数上
        // 运行时：在执行前将参数写入一个临时的局部变量作用域
        args: list<ScriptArg>;
    }
}

// 高频复用的场景脚本片段（如通用 Boss 入场演出、宝箱开启流程）
table shared_scene_script[id] (json) {
    id: int;
    name: text;
    description: text;
    // 声明此脚本期望的输入参数（供编辑器校验和文档提示）
    params: list<ScriptParam>;
    action: SceneAction;
}

struct ScriptParam {
    paramTag: str ->gameplaytag; // 脚本内部通过此 tag 从 sceneVars 读取
    type: ScriptParamType;
    description: text;
}

enum ScriptParamType {
    Float;
    Actor;
}

struct ScriptArg {
    paramTag: str ->gameplaytag; // 对应 ScriptParam.paramTag
    source:   ScriptArgSource;
}

interface ScriptArgSource {
    struct FloatConst { value: float; }
    struct SlotActor  { slotTag: str ->gameplaytag; }
    struct SceneVar   { varTag: str ->gameplaytag; }
}

enum ActorControlMode {
    // 立即授予 State.Scene.Controlled Tag，AI 当帧物理宕机
    Immediate;
    // 注入高优先级 SceneDirected 行为，等 AI 在 minCommitmentTime 后主动让权
    // softTimeoutSec 内未让权则自动升级为 Immediate
    Polite;
}

enum HintStyle {
    Normal;
    Warning;
    BossSkillAlert;
}

interface CameraAction {
    struct FocusOn { target: SceneActorSelector; blendTime: float; }
    struct Cutscene { cutsceneId: str; }
    struct Shake    { preset: str; }
    struct Restore  { blendTime: float; }
}
```

---

## 与 GAS / AI 系统的集成关系

场景系统不独立实现战斗或 AI 逻辑，而是作为**编排层**，通过以下接口与两个系统交互。

| 场景节点 | 对接目标 | 机制 |
| :--- | :--- | :--- |
| `DoEffect` | GAS Effect 系统 | 直接调用 `Effects.execute()`，`scene.makeContext()` 注入场景 instigator/target |
| `CastAbility` | GAS Ability 系统 | 调用槽位实体的 AbilityComponent |
| `WithStatus` | GAS Status 系统 | 挂载/精确移除 StatusInstance，生命周期与作用域节点绑定 |
| `SendEvent` | GAS EventBus | 场景→战斗的单向广播，GAS Trigger 可监听 |
| `WaitForEvent` | GAS EventBus | 战斗→场景的反向通知（如击杀、技能命中完成后推进剧情）|
| `TakeActorControl` (Immediate) | GAS Tag 系统 | 授予 `State.Scene.Controlled`，被 `global_ai_settings` 拦截，AI 物理宕机 |
| `TakeActorControl` (Polite) | AI 行为系统 | 注入高优先级 `SceneDirected` 行为，AI 主动让权 |
| `SpawnActor` | AI Archetype | 生成实体并初始化 `AIBrainComponent` |
| 场景生命周期 | GAS EventBus | 场景开始/结束自动广播，任务系统、成就系统可监听 |

### Actor 控制权冲突：两种夺权模式

当场景需要控制一个正在自治运行 AI 的 Actor 时，存在控制权冲突。根据对视觉过渡的要求选择模式：

**Immediate（硬夺权）**：适用于 `Dialogue`、`Cutscene`、需要完全锁定姿态的演出。

```
1. TakeActorControl.onEnter → 调用 GAS GrantTemporaryTags("State.Scene.Controlled")
2. AI 在下一帧 global_ai_settings 校验命中 → 物理宕机，清空任务栈
3. SceneInstance 独占驱动 Actor
4. TakeActorControl.onExit (finally) → 移除 State.Scene.Controlled
5. AI 大脑恢复，下一帧重新进入算分流程
```

**Polite（软夺权）**：适用于 `MoveTo`、`CastAbility`，需要 AI 自然过渡，避免动画撕裂。

```
1. TakeActorControl.onEnter → 注入极高优先级的 SceneDirected 行为到 AI 决策池
2. AI 完成当前 minCommitmentTime → 算分选中 SceneDirected → 授予 State.Scene.Directed
3. SceneInstance 检测到 State.Scene.Directed → 开始执行 body
4. softTimeoutSec 内未让权 → 自动升级为 Immediate
5. TakeActorControl.onExit (finally) → 撤回注入，移除 State.Scene.Directed
6. AI 恢复原有决策池
```

**Tag 引用计数的安全性**：当场景的 `State.Scene.Controlled` 与玩家技能施加的 `State.Debuff.Stun` 同时作用于一个 Actor 时，`TagContainer` 的写入时展开 + 引用计数机制保证：Stun 结束只移除 Stun 的引用，`State.Scene.Controlled` 依然有效，AI 不会提前复活。

---

## Implementation Reference

本章提供核心节点的运行时实现伪代码。

### 复合节点

```java
// SequenceContext：顺序执行，每帧推进一步
class SequenceContext extends ActionContext {
    int cursor = 0;

    @Override void onEnter() { /* 子节点在 tick 中按需初始化 */ }

    @Override Status tick(float dt) {
        if (cursor >= children.size()) return Succeeded;

        ActionContext cur = children.get(cursor);
        if (cur.status == Pending) {
            cur.onEnter();
            cur.status = Running;
        }

        Status s = cur.tick(dt);
        if (s == Succeeded) {
            cur.onExit(false); cur.status = Succeeded; cursor++;
            return cursor >= children.size() ? Succeeded : Running;
        }
        if (s == Failed) {
            cur.onExit(false); cur.status = Failed;
            return Failed;
        }
        return Running;
    }

    @Override void onExit(boolean aborted) {
        if (aborted && cursor < children.size()) {
            ActionContext cur = children.get(cursor);
            if (cur.status == Running) cur.abort();
        }
    }
}

// ParallelContext：并发执行，按 policy 决定完成时机
class ParallelContext extends ActionContext {
    ParallelPolicy policy;

    @Override void onEnter() {
        // 所有子节点同时启动
        for (ActionContext child : children) {
            child.onEnter(); child.status = Running;
        }
    }

    @Override Status tick(float dt) {
        int running = 0; boolean anyFailed = false;

        for (ActionContext child : children) {
            if (child.status != Running) continue;
            Status s = child.tick(dt);
            if (s == Succeeded) { child.onExit(false); child.status = Succeeded; }
            else if (s == Failed) { child.onExit(false); child.status = Failed; anyFailed = true; }
            else running++;
        }

        if (policy == WaitAny) {
            if (children.stream().anyMatch(c -> c.status == Succeeded)) {
                abortRunningChildren(); return Succeeded;
            }
        }
        if (policy == WaitAll && running == 0)
            return anyFailed ? Failed : Succeeded;

        return Running;
    }

    @Override void onExit(boolean aborted) { if (aborted) abortRunningChildren(); }

    private void abortRunningChildren() {
        for (int i = children.size()-1; i >= 0; i--)
            if (children.get(i).status == Running) children.get(i).abort();
    }
}

// TryContext：容错包装
class TryContext extends ActionContext {
    ActionContext bodyCtx;
    ActionContext fallbackCtx;
    boolean inFallback = false;

    @Override void onEnter() {
        bodyCtx = ActionContext.create(config.body, scene, this);
        bodyCtx.onEnter(); bodyCtx.status = Running;
    }

    @Override Status tick(float dt) {
        if (!inFallback) {
            Status s = bodyCtx.tick(dt);
            if (s == Succeeded) { bodyCtx.onExit(false); return Succeeded; }
            if (s == Failed) {
                bodyCtx.onExit(false);
                if (config.fallback != null) {
                    inFallback = true;
                    fallbackCtx = ActionContext.create(config.fallback, scene, this);
                    fallbackCtx.onEnter(); fallbackCtx.status = Running;
                    return Running;
                }
                return Succeeded; // null fallback = 静默吞掉失败
            }
            return Running;
        } else {
            Status s = fallbackCtx.tick(dt);
            if (s == Succeeded) { fallbackCtx.onExit(false); return Succeeded; }
            if (s == Failed) { fallbackCtx.onExit(false); return Failed; }
            return Running;
        }
    }

    @Override void onExit(boolean aborted) {
        if (aborted) {
            if (!inFallback && bodyCtx.status == Running) bodyCtx.abort();
            if (inFallback && fallbackCtx != null && fallbackCtx.status == Running) fallbackCtx.abort();
        }
    }
}
```

### 等待节点

```java
// PlayAnimationContext：await 语义的完整实现
class PlayAnimationContext extends ActionContext {
    private EventListener completionListener;
    private boolean       signalReceived = false;

    @Override void onEnter() {
        Actor target = scene.resolveActor(config.target);

        // 发出动画指令（始终 fire-and-forget）
        target.eventBus.dispatch(Event.of("Event.Actor.PlayAnim",
            Payload.with(TAG_ANIM_NAME, config.animName)
                   .with(TAG_BLEND_IN, config.blendInTime)));

        // 仅在 UntilComplete 时订阅完成事件
        if (config.await == UntilComplete) {
            completionListener = target.eventBus.subscribe(
                "Event.Actor.AnimComplete",
                evt -> {
                    if (evt.payload.getString(TAG_ANIM_NAME).equals(config.animName))
                        signalReceived = true;
                });
        }
    }

    @Override Status tick(float dt) {
        return switch (config.await) {
            case Immediate    -> Succeeded;
            case UntilComplete -> signalReceived ? Succeeded : Running;
        };
    }

    @Override void onExit(boolean aborted) {
        if (completionListener != null) { completionListener.unsubscribe(); completionListener = null; }
        if (aborted && config.stopOnAbort) {
            Actor target = scene.resolveActor(config.target);
            target.eventBus.dispatch(Event.of("Event.Actor.StopAnim",
                Payload.with(TAG_ANIM_NAME, config.animName)));
        }
    }
}

// WaitForEventContext：EventBus 订阅驱动
class WaitForEventContext extends ActionContext {
    private EventListener listener;
    private boolean       signalReceived = false;
    private float         elapsed = 0;

    @Override void onEnter() {
        EventBus bus = config.source != null
            ? scene.resolveActor(config.source).eventBus
            : scene.globalEventBus;

        listener = bus.subscribe(config.eventTag, evt -> {
            if (!signalReceived && evaluateAll(config.conditions, evt))
                signalReceived = true;
        });
    }

    @Override Status tick(float dt) {
        if (signalReceived) return Succeeded;
        if (config.timeoutSec > 0) {
            elapsed += dt;
            if (elapsed >= config.timeoutSec) {
                if (config.onTimeout != null) {
                    replaceWith(config.onTimeout); return Running;
                }
                return Failed;
            }
        }
        return Running;
    }

    @Override void onExit(boolean aborted) {
        if (listener != null) { listener.unsubscribe(); listener = null; }
    }
}

// WaitUntilContext：依赖追踪 + 可选显式订阅
class WaitUntilContext extends ActionContext {
    private List<Subscription> subscriptions = new ArrayList<>();
    private boolean conditionDirty = false;
    private boolean conditionMet = false;
    private float   elapsed = 0;

    @Override void onEnter() {
        // 立即评估一次
        conditionMet = eval(config.condition);
        if (conditionMet) return;

        if (config.reactToEvents != null && !config.reactToEvents.isEmpty()) {
            // 策划显式声明了事件源：精确订阅
            for (String tag : config.reactToEvents) {
                subscriptions.add(scene.globalEventBus.subscribe(tag,
                    evt -> conditionDirty = true));
            }
        } else {
            // 自动依赖追踪：分析条件引用的 Stat/Tag，注册变更监听
            Set<Dependency> deps = ConditionDependencyAnalyzer
                .extractDependencies(config.condition);
            for (Dependency dep : deps) {
                Actor actor = scene.resolveActor(dep.actor());
                if (dep.type() == DependencyType.Stat) {
                    subscriptions.add(actor.statComponent.onChanged(dep.statOrTagId(),
                        () -> conditionDirty = true));
                } else {
                    subscriptions.add(actor.tagContainer.onChanged(dep.statOrTagId(),
                        () -> conditionDirty = true));
                }
            }
        }
    }

    @Override Status tick(float dt) {
        if (conditionMet) return Succeeded;
        if (conditionDirty) {
            conditionDirty = false;
            conditionMet = eval(config.condition);
            if (conditionMet) return Succeeded;
        }
        elapsed += dt;
        if (config.timeoutSec > 0 && elapsed >= config.timeoutSec) return Failed;
        return Running;
    }

    @Override void onExit(boolean aborted) {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
    }
}
```

### 作用域节点

```java
// TakeActorControlContext：finally 语义保证控制权归还
class TakeActorControlContext extends ActionContext {
    private List<Actor> lockedActors   = new ArrayList<>();
    private ActionContext bodyContext   = null;
    private boolean  controlGranted    = false;
    private float    softTimer         = 0;

    @Override void onEnter() {
        if (config.mode == Immediate) {
            grantControl(resolveAll(config.targets));
        } else {
            // Polite：向每个 Actor 注入高优 SceneDirected 行为
            resolveAll(config.targets).forEach(this::injectSceneDirectedBehavior);
        }
    }

    private void grantControl(List<Actor> targets) {
        for (Actor a : targets) {
            Effects.execute(new GrantTemporaryTags(PERMANENT, List.of("State.Scene.Controlled")),
                scene.makeContext(a));
            lockedActors.add(a);
        }
        controlGranted = true;
        bodyContext = ActionContext.create(config.body, scene, this);
        bodyContext.onEnter(); bodyContext.status = Running;
    }

    @Override Status tick(float dt) {
        if (!controlGranted) {
            softTimer += dt;
            boolean allYielded = resolveAll(config.targets)
                .stream().allMatch(a -> a.tagContainer.hasTag(TAG_SCENE_DIRECTED));
            if (allYielded || softTimer >= config.softTimeoutSec)
                grantControl(resolveAll(config.targets));
            return Running;
        }
        return bodyContext.tick(dt);
    }

    // 最重要的方法：无论如何都必须归还控制权
    @Override void onExit(boolean aborted) {
        // 1. 先中止 body
        if (bodyContext != null && bodyContext.status == Running) bodyContext.abort();

        // 2. 归还所有 Actor 控制权
        for (Actor a : lockedActors) {
            removeTag(a, "State.Scene.Controlled");
            removeSceneDirectedBehavior(a);
        }

        // 3. Abort 时执行清理动作（fire-and-forget，不阻塞）
        if (aborted && config.onAbort != null)
            scene.registerFireAndForget(ActionContext.create(config.onAbort, scene, null));
    }
}

// WithStatusContext：Status 生命周期与作用域绑定
class WithStatusContext extends ActionContext {
    private StatusInstance statusInst;
    private ActionContext  bodyContext;

    @Override void onEnter() {
        Actor target = scene.resolveActor(config.target);
        statusInst = target.statusComponent.apply(config.statusId);
        bodyContext = ActionContext.create(config.body, scene, this);
        bodyContext.onEnter(); bodyContext.status = Running;
    }

    @Override Status tick(float dt) { return bodyContext.tick(dt); }

    @Override void onExit(boolean aborted) {
        if (bodyContext != null && bodyContext.status == Running) bodyContext.abort();
        // 精确移除此实例，不影响同类型的其他 StatusInstance
        if (statusInst != null && !statusInst.isPendingKill())
            statusInst.setPendingKill(true);
    }
}

// ScriptRefContext：参数化共享脚本执行
class ScriptRefContext extends ActionContext {
    private ActionContext bodyContext;
    private Store savedVars; // 备份被覆写的变量

    @Override void onEnter() {
        savedVars = new Store();
        // 将参数绑定写入 sceneVars（相当于隐式 WithVar）
        for (ScriptArg arg : config.args) {
            // 备份原值
            if (scene.sceneVars.hasTag(arg.paramTag))
                savedVars.set(arg.paramTag, scene.sceneVars.get(arg.paramTag));
            // 写入绑定值
            Object value = resolveArgSource(arg.source);
            scene.sceneVars.set(arg.paramTag, value);
        }

        shared_scene_script shared = lookupSharedScript(config.sharedScriptId);
        bodyContext = ActionContext.create(shared.action, scene, this);
        bodyContext.onEnter(); bodyContext.status = Running;
    }

    @Override Status tick(float dt) { return bodyContext.tick(dt); }

    @Override void onExit(boolean aborted) {
        if (bodyContext != null && bodyContext.status == Running) bodyContext.abort();
        // 恢复被覆写的变量
        for (ScriptArg arg : config.args) {
            if (savedVars.hasTag(arg.paramTag))
                scene.sceneVars.set(arg.paramTag, savedVars.get(arg.paramTag));
            else
                scene.sceneVars.removeTag(arg.paramTag);
        }
    }
}
```

---

## Examples

### 例 A：炎魔领主 Boss 战（双阶段）

展示一阶段自治 AI、转阶段硬夺权过场、二阶段行为池替换的完整配表写法。

```cfg
table scene_definition {
    id: 7001; name: "炎魔领主试炼";
    sceneTags: ["Scene.Type.Boss"];

    actorSlots: [
        { slotTag: "Slot.MagmaLord"; aiArchetype: "Boss_MagmaLord";
          spawnPoint: "Boss_Arena_Center"; removeOnDeath: true; },
        { slotTag: "Slot.Player"; spawnPoint: "Arena_Entrance"; removeOnDeath: false; }
    ];
    environmentAnchors: [
        { slotTag: "Slot.ArenaCenter"; spawnPoint: "Arena_Center_Marker"; initialStatuses: []; }
    ];

    prerequisites: [];
    trigger: null; // 由关卡管理器显式调用激活

    initVars: [
        { varTag: "Var.Scene.Phase"; initValue: 1.0 }
    ];

    script: struct Sequence { actions: [

        // ① 入场演出（硬夺权，Boss 不允许在对白期间继续移动）
        struct TakeActorControl {
            targets: [struct SlotActor { slotTag: "Slot.MagmaLord"; }];
            mode: Immediate; softTimeoutSec: 0.0;
            body: struct Sequence { actions: [
                struct Camera { action: struct FocusOn {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; }; blendTime: 1.0; };
                    await: Immediate; };
                struct PlayAnimation {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    animName: "entrance_roar"; blendInTime: 0.3;
                    await: UntilComplete;
                    stopOnAbort: true;
                };
                struct Dialogue {
                    speaker: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    dialogueId: 5001; await: UntilComplete;
                };
                struct Camera { action: struct Restore { blendTime: 0.8; }; await: Immediate; };
            ]};
            onAbort: struct PlayAnimation {
                target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                animName: "idle"; blendInTime: 0.5; await: Immediate; stopOnAbort: false;
            };
        };

        // 授予 P1 标记，AI 开始自治
        struct DoEffect {
            target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
            effect: struct GrantTemporaryTags {
                duration: struct Const { value: -1.0; };
                grantedTags: ["Scene.MagmaLord.Phase1"];
            };
            await: Immediate;
        };

        // ② 第一阶段：并行监听 HP 触发和计数任务
        // WaitAny：HP 条件满足后整个 Parallel 结束，Loop 分支自动中止
        struct Parallel {
            policy: WaitAny;
            actions: [
                // HP 条件：依赖 Stat，系统自动追踪，无需填 reactToEvents
                struct WaitUntil {
                    condition: struct ActorStatCompare {
                        actor: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        statTag: "Stat.HP.Percent"; op: LessEqual; value: 0.3;
                    };
                    reactToEvents: []; // 空 = 自动依赖追踪
                    timeoutSec: -1.0;
                };
                // 持续监听小怪死亡，维护 MinionCount 供 AI 召唤条件使用
                struct Loop {
                    count: -1;
                    body: struct Sequence { actions: [
                        struct WaitForEvent {
                            eventTag: "Event.Scene.MinionDied";
                            source: null; conditions: []; timeoutSec: -1.0;
                        };
                        struct SetSceneVar {
                            varTag: "Var.Scene.MinionKillCount"; op: Add; value: 1.0;
                        };
                    ]};
                };
            ];
        };

        // ③ 转阶段过场（硬夺权）
        struct TakeActorControl {
            targets: [struct SlotActor { slotTag: "Slot.MagmaLord"; }];
            mode: Immediate; softTimeoutSec: 0.0;
            body: struct Sequence { actions: [
                struct SetSceneVar { varTag: "Var.Scene.Phase"; op: Override; value: 2.0; };
                struct Parallel {
                    policy: WaitAll;
                    actions: [
                        struct Camera { action: struct FocusOn {
                            target: struct SlotActor { slotTag: "Slot.MagmaLord"; }; blendTime: 0.5; };
                            await: Immediate; };
                        struct ShowHint {
                            hintKey: "Boss.MagmaLord.PhaseChange"; duration: 5.0;
                            hintStyle: BossSkillAlert;
                        };
                    ];
                };
                // 霸体保护 + 嘶吼动画
                struct WithStatus {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    statusId: 4010; // Status: 霸体（blockTags: ["State.Debuff.Control"]）
                    body: struct PlayAnimation {
                        target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        animName: "phase2_roar"; blendInTime: 0.2;
                        await: UntilComplete; stopOnAbort: false;
                    };
                };
                // 对话可能因玩家跳过而失败，用 Try 包装避免中断转阶段流程
                struct Try {
                    body: struct Dialogue {
                        speaker: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        dialogueId: 5002; await: UntilComplete;
                    };
                    fallback: null; // 跳过就跳过，不影响后续流程
                };
                // 环境变化并行触发
                struct Parallel {
                    policy: WaitAll;
                    actions: [
                        struct SendEvent {
                            target: struct SlotActor { slotTag: "Slot.ArenaCenter"; };
                            eventTag: "Event.Environment.StoneDoor.Close";
                            magnitude: 0.0; extras: [];
                        };
                        struct DoEffect {
                            target: struct SlotActor { slotTag: "Slot.ArenaCenter"; };
                            effect: struct ApplyStatus { statusId: 4003; captures: []; };
                            await: Immediate;
                        };
                    ];
                };
                struct WaitSeconds { duration: 1.0; }; // 留给玩家的视觉缓冲
                struct Camera { action: struct Restore { blendTime: 0.8; }; await: Immediate; };
                // 挂载 P2 Status：内含 AIBehaviorModifier，自动替换行为池
                struct DoEffect {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    effect: struct ApplyStatus { statusId: 4002; captures: []; };
                    await: Immediate;
                };
            ]};
            onAbort: null;
        };

        // ④ 第二阶段：等待 Boss 死亡（AI 完全自治）
        struct WaitForEvent {
            eventTag: "Event.Character.Death";
            source: struct SlotActor { slotTag: "Slot.MagmaLord"; };
            conditions: []; timeoutSec: -1.0;
        };

        // ⑤ 胜利演出
        struct Parallel {
            policy: WaitAll;
            actions: [
                struct Camera { action: struct FocusOn {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; }; blendTime: 0.5; };
                    await: Immediate; };
                struct DoEffect {
                    target: struct SlotActor { slotTag: "Slot.Player"; };
                    effect: struct SendEvent {
                        eventTag: "Event.UI.ShowVictory"; magnitude: 0.0; extras: [];
                    };
                    await: Immediate;
                };
            ];
        };
        struct WaitSeconds { duration: 3.0; }; // 胜利画面停留
        struct Camera { action: struct Restore { blendTime: 1.5; }; await: Immediate; };
    ]};

    outcomes: [
        {
            outcomeTag: "Scene.Result.Victory";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.MagmaLord"; }; };
            reactToEvents: []; // ActorIsDead 依赖 Tag，自动追踪
            terminateScene: true; priority: 10;
            rewardEffects: [
                { target: struct SlotActor { slotTag: "Slot.Player"; };
                  effect: struct ModifyStat {
                      statTag: "Stat.Currency.Gold"; op: Add;
                      value: struct Const { value: 1500.0; };
                  }; }
            ];
            scoreFormula: struct TimeBonusDecay { baseScore: 10000.0; decayPerSecond: 8.0; };
            broadcastEvent: "Event.Scene.MagmaLordDefeated";
        },
        {
            outcomeTag: "Scene.Result.Defeat";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.Player"; }; };
            reactToEvents: [];
            terminateScene: true; priority: 100;
            rewardEffects: [];
            broadcastEvent: "Event.Scene.PlayerDefeated";
        },
        {
            outcomeTag: "Scene.Result.Timeout";
            condition: struct TimeSinceSceneStart { op: GreaterEqual; seconds: 600.0; };
            reactToEvents: []; // 时间条件由内部计时器驱动
            terminateScene: true; priority: 50;
            rewardEffects: [];
            broadcastEvent: null;
        }
    ];
}
```

### 例 B：护送任务（多目标评分 + 共享脚本参数化）

展示 `terminateScene: false` 的多目标记录模式、`trigger` 的动态激活，以及 `ScriptRef` 的参数化复用。

```cfg
// 共享脚本：通用 NPC 出发对话流程
table shared_scene_script {
    id: 9001; name: "通用NPC出发对话";
    params: [
        { paramTag: "Param.Speaker"; type: Actor; description: "说话的 NPC 槽位"; },
        { paramTag: "Param.DialogueId"; type: Float; description: "对话表 ID"; }
    ];
    action: struct TakeActorControl {
        targets: [struct SceneVar { actorVarTag: "Param.Speaker"; }];
        mode: Polite; softTimeoutSec: 3.0;
        body: struct Try {
            body: struct Dialogue {
                speaker: struct SceneVar { actorVarTag: "Param.Speaker"; };
                dialogueId: 6001; // 运行时可通过 Param.DialogueId 间接查表
                await: UntilComplete;
            };
            fallback: null;
        };
        onAbort: null;
    };
}

table scene_definition {
    id: 7002; name: "护送商人";
    sceneTags: ["Scene.Type.Escort"];

    actorSlots: [
        { slotTag: "Slot.Merchant"; aiArchetype: "NPC_Merchant";
          spawnPoint: "Escort_Start"; removeOnDeath: true; },
        { slotTag: "Slot.Player"; spawnPoint: "Escort_Start"; removeOnDeath: false; }
    ];
    environmentAnchors: [
        { slotTag: "Slot.EscortGoal"; spawnPoint: "Escort_End_Marker"; initialStatuses: []; }
    ];

    prerequisites: [
        struct ActorHasTags {
            actor: struct SlotActor { slotTag: "Slot.Player"; };
            tagQuery: { requireTagsAll: ["Quest.EscortPrerequisite.Done"]; };
        };
    ];
    trigger: struct SceneTrigger {
        condition: struct ActorStatCompare {
            actor: struct SlotActor { slotTag: "Slot.Player"; };
            statTag: "Stat.DistanceTo.EscortStart"; op: LessEqual; value: 5.0;
        };
        reactToEvents: []; // 距离依赖 Stat，自动追踪
    };

    initVars: [
        { varTag: "Var.Scene.MerchantsAlive"; initValue: 1.0 },
        { varTag: "Var.Scene.EnemiesKilled";  initValue: 0.0 }
    ];

    script: struct Sequence { actions: [

        // 出发对话（复用共享脚本，参数化绑定）
        struct ScriptRef {
            sharedScriptId: 9001;
            args: [
                { paramTag: "Param.Speaker";
                  source: struct SlotActor { slotTag: "Slot.Merchant"; }; },
                { paramTag: "Param.DialogueId";
                  source: struct FloatConst { value: 6001.0; }; }
            ];
        };

        // 护送主体：并行监听"到达终点"和"商人死亡"
        struct Parallel {
            policy: WaitAny;
            actions: [
                // 距离条件：自动追踪 Stat 变化
                struct WaitUntil {
                    condition: struct ActorStatCompare {
                        actor: struct SlotActor { slotTag: "Slot.Merchant"; };
                        statTag: "Stat.DistanceTo.EscortGoal"; op: LessEqual; value: 3.0;
                    };
                    reactToEvents: [];
                    timeoutSec: -1.0;
                };
                struct WaitForEvent {
                    eventTag: "Event.Character.Death";
                    source: struct SlotActor { slotTag: "Slot.Merchant"; };
                    conditions: []; timeoutSec: -1.0;
                };
                // 持续统计击杀数（由外部 WaitAny 中止）
                struct Loop {
                    count: -1;
                    body: struct Sequence { actions: [
                        struct WaitForEvent {
                            eventTag: "Event.Character.Death";
                            source: null;
                            conditions: [
                                struct ActorHasTags {
                                    actor: struct SceneVar { actorVarTag: "Data.KilledActor"; };
                                    tagQuery: { requireTagsAny: ["Faction.Bandit"]; };
                                };
                            ];
                            timeoutSec: -1.0;
                        };
                        struct SetSceneVar {
                            varTag: "Var.Scene.EnemiesKilled"; op: Add; value: 1.0;
                        };
                    ]};
                };
            ];
        };
    ]};

    outcomes: [
        {
            outcomeTag: "Scene.Result.EscortSuccess";
            condition: struct ActorStatCompare {
                actor: struct SlotActor { slotTag: "Slot.Merchant"; };
                statTag: "Stat.DistanceTo.EscortGoal"; op: LessEqual; value: 3.0;
            };
            reactToEvents: [];
            terminateScene: true; priority: 10;
            rewardEffects: [
                { target: struct SlotActor { slotTag: "Slot.Player"; };
                  effect: struct ModifyStat {
                      statTag: "Stat.Currency.Gold"; op: Add;
                      value: struct Const { value: 300.0; };
                  }; }
            ];
            scoreFormula: struct Math {
                op: Add;
                a: struct TimeBonusDecay { baseScore: 5000.0; decayPerSecond: 5.0; };
                b: struct Math {
                    op: Multiply;
                    a: struct SceneVarValue { varTag: "Var.Scene.EnemiesKilled"; };
                    b: struct Const { value: 100.0; };
                };
            };
            broadcastEvent: "Event.Quest.EscortComplete";
        },
        {
            outcomeTag: "Scene.Result.MerchantDied";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.Merchant"; }; };
            reactToEvents: [];
            terminateScene: true; priority: 100;
            rewardEffects: [];
            broadcastEvent: "Event.Quest.EscortFailed";
        }
    ];
}
```