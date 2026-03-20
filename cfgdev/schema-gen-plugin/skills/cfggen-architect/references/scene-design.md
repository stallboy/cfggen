# 场景逻辑系统设计 (Scene Logic System Design)

本文档定义一套与 `ability-design` 和 `ai-design` 无缝集成的场景逻辑系统。它解决的核心问题是：**如何用数据驱动的方式，编排多实体在一段时间内的协同行为序列，并与 GAS 和 AI 系统深度联动**。

## Philosophy

1. **复用，不重造**：
   `SceneAction` 的执行单元直接调用 GAS 的 `Effect`、`Ability`，AI 的行为修饰通过 `AIBehaviorModifier` 完成。场景系统不引入平行的战斗逻辑体系，只做编排。

2. **执行树，非执行栈**：
   所有动作节点构成一棵 **ActionContext 树**，复合节点（`Sequence`、`Parallel`）与子节点共存于树中，每帧只 tick 处于 Running 状态的节点。这使 `Parallel` 的并发语义可以被自然表达，而执行栈天然无法做到这一点。

3. **订阅驱动，非轮询**：
   所有等待节点（`WaitForEvent`、`WaitUntil`）在 `onEnter` 时向 `EventBus` 注册监听，在 `onExit` 时必须取消注册。条件的重新评估由事件推送触发，而非每帧主动求值。这是解决 ECA 时代"全局变量轮询"问题的根本手段。

4. **作用域即生命周期**：
   所有有副作用的操作（占用 Actor 控制权、挂载临时 Status、声明局部变量）都必须包装在对应的作用域节点中。作用域节点的 `onExit` 在 `finally` 语义下执行——无论正常完成还是被外部中止，清理动作一定发生，从根本上杜绝状态残留（Orphaned State）。

5. **await 语义显式声明**：
   `PlayAnimation`、`Dialogue`、`MoveTo` 等动作节点默认是 fire-and-forget（发出指令立刻返回）。如需等待完成，必须在配表中显式声明 `await: UntilComplete`，引擎通过 EventBus 订阅完成事件实现，而非硬编码时长。这直接解决了 ECA 时代 `Wait 3.0 seconds` 与动画时长脱节的顽疾。

6. **结局统一终止语义**：
   场景的所有终止路径（Boss 死亡、玩家死亡、超时）统一通过 `outcomes` 配置，不再设立 `abortConditions` 和 `timeLimit` 两个平行字段，避免语义重叠和重复填写。

---

## Data Foundation

本章节定义场景系统使用的原子数据类型。所有 `gameplaytag`、`stat_definition`、`event_definition` 均与 `ability-design` 共享同一张注册表，不重复定义。

### SceneCondition

`SceneCondition` 的运行时上下文是 `SceneInstance`，用于 `outcome.condition`、`trigger.condition`、`WaitUntil.condition` 等场合。

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
        tagQuery: TagQuery;
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

    // 场景已运行时长（由 SceneInstance 内部计时器驱动，不需要 reactToEvents）
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

所有有"持续时间"语义的动作节点都必须声明此枚举，这是解决 fire-and-forget 歧义的核心设计。

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

    // 当前已运行时长（供 TimeSinceSceneStart 条件使用）
    float elapsedTime = 0;

    // 已匹配但 terminateScene=false 的结局记录（用于多目标评分）
    List<SceneOutcome> matchedOutcomes = new ArrayList<>();

    SceneState state = Pending;

    // 每帧由引擎调用
    void tick(float dt) {
        if (state != Running) return;
        elapsedTime += dt;

        // 检查 terminateScene=true 的 outcome（含超时、败北等）
        // TimeSinceSceneStart 类条件由此处每帧主动检查，其余 outcome
        // 通过 EventBus 回调触发，不在此处轮询
        checkTerminatingOutcomes();
        if (state != Running) return;

        // Tick 执行树根节点
        if (rootContext.status == Pending) {
            rootContext.onEnter();
            rootContext.status = Running;
        }

        ActionContext.Status s = rootContext.tick(dt);

        if (s == Succeeded || s == Failed) {
            finalize(s);
        }
    }

    // 解析 SceneActorSelector 为实际 Actor
    Actor resolveActor(SceneActorSelector selector) { ... }

    // 生成供 Effect/Ability 执行使用的 GAS Context
    Context makeContext(Actor target) {
        return new Context(
            instigator: resolveActor(SlotActor("Slot.Player")),
            causer:     null,
            target:     target,
            instanceState: sceneVars,  // 场景变量作为 instanceState 注入
            ...
        );
    }

    void abort() {
        rootContext.abort();
        finalize(Failed);
    }

    void finalize(ActionContext.Status terminalStatus) {
        SceneOutcome outcome = pickHighestPriorityOutcome(terminalStatus);
        if (outcome != null) applyOutcome(outcome);
        state = (terminalStatus == Succeeded) ? Completed : Aborted;
    }
}

enum SceneState { Pending; Running; Completed; Aborted; }
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
    outcomes: list<SceneOutcome>;

    // 场景变量初始化
    initVars: list<SceneVarInit>;
}

struct SceneTrigger {
    condition: SceneCondition;
    // 声明哪些事件可能导致条件变化，引擎只在这些事件发生时重新 evaluate
    // 不声明 reactToEvents 的条件将被每帧轮询（编辑器警告）
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
    varKey: str ->var_key;
    initValue: float;
}

struct SceneOutcome {
    outcomeTag: str ->gameplaytag; // 如 "Scene.Result.Victory"

    // 触发条件
    condition: SceneCondition;
    // 声明哪些事件可能导致条件变化（EventBus 订阅驱动重新评估）
    // TimeSinceSceneStart 类条件不需要填写，由 SceneInstance 内部计时器驱动
    reactToEvents: list<str> ->event_definition;

    // true = 匹配后立即终止场景（如失败、超时）
    // false = 记录结果但继续执行（如多目标评分场景）
    terminateScene: bool;

    // 并发匹配时的优先级（数值越大越优先）
    // 建议：死亡=100, 超时=50, 胜利=10
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
    // 运行时：onEnter 订阅，onExit 取消订阅，不轮询
    struct WaitForEvent {
        eventTag:   str ->event_definition;
        source:     SceneActorSelector (nullable); // null = 监听所有来源
        conditions: list<SceneCondition>;
        timeoutSec: float;
        // 超时后执行的备用动作（null = 静默超时，返回 Failed）
        onTimeout:  SceneAction (nullable);
    }

    // 等待条件为真
    // 运行时：onEnter 时立即 evaluate 一次；随后只在 reactToEvents 事件触发时重新 evaluate
    struct WaitUntil {
        condition:      SceneCondition;
        // !! 必须填写：策划声明哪些事件可能改变此条件的值
        // 引擎不做每帧轮询，只在这些事件发生时重新 evaluate
        reactToEvents:  list<str> ->event_definition;
        timeoutSec:     float;
    }

    // 纯时长等待（降级方案，建议优先使用 WaitForEvent）
    struct WaitSeconds {
        duration: float;
    }

    // ═══════════════════════════════
    // 控制流节点
    // ═══════════════════════════════

    // 顺序执行：子节点依次执行，任意一个 Failed 则整体 Failed
    struct Sequence { actions: list<SceneAction>; }

    // 并行执行
    // WaitAll：等所有子节点完成
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

    struct ScriptRef {
        sharedScriptId: int ->shared_scene_script;
    }
}

// 高频复用的场景脚本片段（如通用 Boss 入场演出、宝箱开启流程）
table shared_scene_script[id] (json) {
    id: int;
    name: text;
    description: text;
    action: SceneAction;
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
```

### 等待节点

```java
// PlayAnimationContext：await 语义的完整实现
// 核心：不再是 Wait 3.0 seconds，而是订阅 Event.Actor.AnimComplete
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
        // !! 必须取消订阅，防止 SceneInstance 销毁后事件仍然回调
        if (completionListener != null) { completionListener.unsubscribe(); completionListener = null; }
        if (aborted && config.stopOnAbort) {
            Actor target = scene.resolveActor(config.target);
            target.eventBus.dispatch(Event.of("Event.Actor.StopAnim",
                Payload.with(TAG_ANIM_NAME, config.animName)));
        }
    }
}

// WaitForEventContext：EventBus 订阅驱动，不轮询
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
                    // 用超时 Action 替换自身后继续 Running
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

// WaitUntilContext：事件推送重新 evaluate，不做每帧轮询
class WaitUntilContext extends ActionContext {
    private List<EventListener> listeners = new ArrayList<>();
    private boolean conditionMet = false;
    private float   elapsed = 0;

    @Override void onEnter() {
        // 立即 evaluate 一次
        conditionMet = eval(config.condition);
        if (conditionMet) return;

        // 订阅所有可能导致条件变化的事件
        // 策划通过 reactToEvents 显式声明，引擎不做盲目轮询
        for (String tag : config.reactToEvents) {
            listeners.add(scene.globalEventBus.subscribe(tag,
                evt -> { if (!conditionMet) conditionMet = eval(config.condition); }));
        }
    }

    @Override Status tick(float dt) {
        if (conditionMet) return Succeeded;
        elapsed += dt;
        if (config.timeoutSec > 0 && elapsed >= config.timeoutSec) return Failed;
        return Running;
    }

    @Override void onExit(boolean aborted) {
        listeners.forEach(EventListener::unsubscribe); listeners.clear();
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
            // 通过 GAS 授予控制 Tag
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

    // !! 最重要的方法：无论如何都必须归还控制权
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
                    await: UntilComplete;   // 等动画真正播完，不再猜时长
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
                struct WaitUntil {
                    condition: struct ActorStatCompare {
                        actor: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        statTag: "Stat.HP.Percent"; op: LessEqual; value: 0.3;
                    };
                    // 只在伤害结算后重新 evaluate，不做每帧轮询
                    reactToEvents: ["Event.Combat.Damage.Post"];
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
                // 霸体保护 + 嘶吼动画：等动画真正播完
                struct WithStatus {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    statusId: 4010; // Status: 霸体（blockTags: ["State.Debuff.Control"]）
                    body: struct PlayAnimation {
                        target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        animName: "phase2_roar"; blendInTime: 0.2;
                        await: UntilComplete; stopOnAbort: false;
                    };
                };
                struct Dialogue {
                    speaker: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    dialogueId: 5002; await: UntilComplete;
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
                struct WaitSeconds { duration: 1.0; };
                struct Camera { action: struct Restore { blendTime: 0.8; }; await: Immediate; };
                // 挂载飞行 Status：内含 AIBehaviorModifier，自动替换 P2 行为池
                // cancelTags 移除 Phase1，grantedTags 授予 Phase2
                struct DoEffect {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    effect: struct ApplyStatus { statusId: 4002; captures: []; };
                    await: Immediate;
                };
                // TakeActorControl 结束 → 移除 State.Scene.Controlled → AI 恢复
                // AI 发现 P2 行为池已注入，直接进入第二阶段
            ]};
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
        struct WaitSeconds { duration: 3.0; };
        struct Camera { action: struct Restore { blendTime: 1.5; }; await: Immediate; };
    ]};

    outcomes: [
        {
            outcomeTag: "Scene.Result.Victory";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.MagmaLord"; }; };
            reactToEvents: ["Event.Character.Death"];
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
            reactToEvents: ["Event.Character.Death"];
            terminateScene: true; priority: 100; // 玩家死亡优先级最高
            rewardEffects: [];
            broadcastEvent: "Event.Scene.PlayerDefeated";
        },
        {
            outcomeTag: "Scene.Result.Timeout";
            condition: struct TimeSinceSceneStart { op: GreaterEqual; seconds: 600.0; };
            reactToEvents: []; // 由 SceneInstance.tick 内部计时器驱动
            terminateScene: true; priority: 50;
            rewardEffects: [];
            broadcastEvent: null;
        }
    ];
}
```

### 例 B：护送任务（多目标评分 + 可选结局）

展示 `terminateScene: false` 的多目标记录模式，以及 `trigger` 字段的动态激活用法。

```cfg
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
        // 前置任务完成才可见（加载时静态检查）
        struct ActorHasTags {
            actor: struct SlotActor { slotTag: "Slot.Player"; };
            tagQuery: { requireTagsAll: ["Quest.EscortPrerequisite.Done"]; };
        };
    ];
    trigger: struct SceneTrigger {
        // 玩家走到护送起点附近时动态激活
        condition: struct ActorStatCompare {
            actor: struct SlotActor { slotTag: "Slot.Player"; };
            statTag: "Stat.DistanceTo.EscortStart"; op: LessEqual; value: 5.0;
        };
        reactToEvents: ["Event.Actor.PositionUpdated"];
    };

    initVars: [
        { varTag: "Var.Scene.MerchantsAlive"; initValue: 1.0 },
        { varTag: "Var.Scene.EnemiesKilled";  initValue: 0.0 }
    ];

    script: struct Sequence { actions: [

        // 出发对话（软夺权，等商人走完当前步骤后自然开口）
        struct TakeActorControl {
            targets: [struct SlotActor { slotTag: "Slot.Merchant"; }];
            mode: Polite; softTimeoutSec: 3.0;
            body: struct Dialogue {
                speaker: struct SlotActor { slotTag: "Slot.Merchant"; };
                dialogueId: 6001; await: UntilComplete;
            };
            onAbort: null;
        };

        // 护送主体：并行监听"到达终点"和"商人死亡"
        struct Parallel {
            policy: WaitAny;
            actions: [
                struct WaitUntil {
                    condition: struct ActorStatCompare {
                        actor: struct SlotActor { slotTag: "Slot.Merchant"; };
                        statTag: "Stat.DistanceTo.EscortGoal"; op: LessEqual; value: 3.0;
                    };
                    reactToEvents: ["Event.Actor.PositionUpdated"];
                    timeoutSec: -1.0;
                };
                struct WaitForEvent {
                    eventTag: "Event.Character.Death";
                    source: struct SlotActor { slotTag: "Slot.Merchant"; };
                    conditions: []; timeoutSec: -1.0;
                };
                // 持续统计击杀数（不终止循环，由外部 WaitAny 中止）
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
            reactToEvents: ["Event.Actor.PositionUpdated"];
            terminateScene: true; priority: 10;
            rewardEffects: [
                { target: struct SlotActor { slotTag: "Slot.Player"; };
                  effect: struct ModifyStat {
                      statTag: "Stat.Currency.Gold"; op: Add;
                      value: struct Const { value: 300.0; };
                  }; }
            ];
            // 评分：基础分 + 击杀加分 - 时间衰减
            scoreFormula: struct Math {
                op: Add;
                a: struct TimeBonusDecay { baseScore: 5000.0; decayPerSecond: 5.0; };
                b: struct Math {
                    op: Multiple;
                    a: struct SceneVarValue { varTag: "Var.Scene.EnemiesKilled"; };
                    b: struct Const { value: 100.0; };
                };
            };
            broadcastEvent: "Event.Quest.EscortComplete";
        },
        {
            outcomeTag: "Scene.Result.MerchantDied";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.Merchant"; }; };
            reactToEvents: ["Event.Character.Death"];
            terminateScene: true; priority: 100;
            rewardEffects: [];
            broadcastEvent: "Event.Quest.EscortFailed";
        }
    ];
}
```