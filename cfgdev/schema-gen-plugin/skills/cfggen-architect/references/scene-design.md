# 场景逻辑系统设计 (Scene Logic System Design)

## Architecture Overview

场景系统（Scene System）定位为遭遇战/演出级的逻辑编排层，位于 GAS（状态与效果）与 AI（自治决策）之上、关卡与任务流程系统之下。它不负责实现基础战斗行为，而是通过驱动既有能力、施加约束、订阅运行时事件，将多个实体在一段时间内组织成可控、可复用、可数据化配置的玩法与演出流程。

### 1.1 层级定位

| 层级 | 系统 | 职责边界 |
|:---|:---|:---|
| **底层** | GAS | 伤害、状态、Tag、Effect、Stat |
| **中层** | AI | 寻路、攻击选择、行为决策 |
| **上层** | Scene | 单场遭遇战/演出的编排 |
| **顶层** | 关卡/任务 | 房间连接、进度存档、全局流转 |

### 场景接口 — 逻辑与空间分离

场景定义（scene_definition）是**纯逻辑编排**，不含任何世界坐标。空间信息通过 signature 注入：


### 核心架构决策

**一切皆树（Act Tree）**。场景定义的执行体是一棵 `rootAct` 树。FSM（状态机）作为树中的一个节点类型（`StateMachine`）存在，而非独立于树的机制。

- **宏观流转**用 `StateMachine` 节点 — Phase 间互斥跳转
- **微观时序**用 `Sequence`/`Parallel` 等控制流 — 帧级动作编排
- **持续监听**用 `Parallel + Loop + WaitForEvent` — 无独立 Monitor 机制

所有运行时逻辑共享同一套生命周期、同一套 abort/RAII 清理路径。

### 执行与终止的关系

```
┌────────────────────────────────────────────┐
│            SceneInstance                   │
│                                            │
│  ┌──────────────────────────────────────┐  │
│  │  rootAct (Act Tree)                  │  │
│  │  ┌────────────┐  ┌───────────────┐   │  │
│  │  │StateMachine│  │ Parallel      │   │  │
│  │  │(Phases)    │  │ (background)  │   │  │
│  │  └────────────┘  └───────────────┘   │  │
│  └──────────────────────────────────────┘  │
│                                            │
│  ┌─────────────────────────────────────┐   │
│  │  Outcome Monitors (always-on)       │   │
│  └─────────────────────────────────────┘   │
└────────────────────────────────────────────┘
```

- **rootAct** 自然走完 ≠ 场景结束
- 场景的生死**由且仅由 Outcome 裁定**
- rootAct 完成后若无 Outcome 匹配，场景进入等待状态

### 1.5 外部通信

场景通过 `signature`（输入参数）接收外部指令，通过 `outcomes`（结局码）向外部汇报结果。两者之间低耦合通信。

---

## Design Principles

1. **复用不重造** — 执行单元直接调用 GAS 的 Effect/Ability，AI 修饰通过 ActStatus 完成。只做编排。

2. **单一执行模型** — 所有逻辑统一为 Act 树。没有树外执行引擎。

3. **执行树非执行栈** — 节点构成树结构，`Parallel` 的并发语义自然表达。每帧只 tick Running 节点。

4. **订阅驱动非轮询** — 等待节点在 `onEnter` 时注册监听，`onExit` 时取消。条件自描述其关心的事件类型。

5. **作用域即生命周期** — 有副作用的操作必须包装在 `WithXXX` 作用域节点中。`onExit` 在 finally 语义下执行，无论正常完成还是强制中止，清理必定发生。

6. **Await 显式声明** — 动作默认 `FireAndForget`。需等待完成必须显式声明 `await: UntilComplete`。

7. **终止与流转分离** — `outcomes` 管场景生死（Terminate），`StateMachine` 内的 `transitions`/`globalTransitions` 管 Phase 流转，职责不交叉。

8. **万物皆变量** — 演员、道具、区域、计数器统一为场景变量，极简心智模型。

9. **上下文继承** — 作用域节点注入 Context Target，子节点默认继承，消除 target 冗余书写。

10. **边界清晰契约分明** — 通过 `signature`（入参）和 `outcomes`（出参）与外部系统通信，不越权。

---

## Variable System

### 冒泡寻址

读取变量时沿树向上冒泡：

```
当前节点 localVars → 父节点 localVars → ... → SceneInstance.globalSceneVars
```

`WithVar` 在 `onEnter` 创建 localVars，`onExit` 销毁。子节点同名变量**遮蔽**而非覆盖父级。

### 写入规则

- `SetSceneVar` → 始终写入 `globalSceneVars`（跨 Phase 可见）
- `WithVar` 内部 → 只影响当前作用域，随退出销毁

---

## Context Target — 上下文继承

### 机制

`ActorSelector` 默认值为 `Inherit {}`。运行时沿树向上冒泡，找到最近的**上下文目标提供者**：

- `WithActorControl` 的 `targets[0]`
- `WithTarget` 节点

### 示例

```
WithActorControl {
    targets: [SceneVar { actorVar: "Cast.Boss" }];
    mode: Immediate;
    body: Sequence { actions: [
        // 以下 target 全部省略，自动继承 Cast.Boss
        PlayAnimation { animName: "entrance_roar"; await: UntilComplete; };
        Dialogue { dialogueId: 5001; await: UntilComplete; };
        ApplyEffect { effect: ...; await: FireAndForget; };
    ]};
};
```

---

## scene_definition

```cfg
table scene_definition[sceneId] (json) {
    sceneId: int;
    name: text;

    // 函数签名（入参声明）
    signature: list<SceneVarDecl>;

    // 结局裁定
    outcomes: list<SceneOutcome>;

    // 执行体（纯粹的一棵树）
    rootAct: Act;
}

struct SceneOutcome {
    condition: SceneCondition;
    resultCode: int;  // 0=失败, 1=成功, ...
}
```

---

## Act — 执行节点体系

`Act` 是场景脚本的唯一执行单元，支持任意嵌套。

### 基础动作

```cfg
interface Act {
    struct None {}

    struct PlayAnimation {
        target: ActorSelector;
        animName: str;
        blendInTime: float;
        await: AwaitMode;
        stopOnAbort: bool;
    }

    struct Dialogue {
        speaker: ActorSelector;
        dialogueId: int ->dialogue;
        await: AwaitMode;
    }

    struct Camera {
        action: CameraAction;
        await: AwaitMode;
    }

    struct ApplyEffect {
        target: ActorSelector;
        effect: Effect;
        await: AwaitMode;
    }

    struct CastAbility {
        caster: ActorSelector;
        abilityId: int ->ability;
        abilityTarget: ActorSelector;
        await: AwaitMode;
    }

    struct SendEvent {
        target: ActorSelector;
        eventTag: str ->event_definition;
        magnitude: float;
        extras: list<VarBinding>;
    }

    struct SpawnActor {
        outputVarKey: str ->var_key;
        archetype: str ->ai_archetype;
        spawnAt: str;
    }

    struct PlayCue {
        cueKey: str ->cue_key;
        playAt: ActorSelector;
    }

    struct MoveTo {
        actor: ActorSelector;
        destination: ActorSelector;
        speedOverride: SceneFloatValue;
        tolerance: float;
        await: AwaitMode;
    }

    struct Interact {
        actor: ActorSelector;
        interactTo: ActorSelector;
        interactionId: int;
        await: AwaitMode;
    }
    
    struct SetSceneVar {
        varKey: str ->var_key;
        op: ModifierOp;
        value: SceneFloatValue;
    }
```

### 等待节点

```cfg
    struct WaitForEvent {
        eventTag: str ->event_definition;
        source: ActorSelector;
        conditions: list<SceneCondition>;
        timeoutSec: float;
        onTimeout: Act;
    }

    struct WaitUntil {
        condition: SceneCondition;
        timeoutSec: float;
        onTimeout: Act;
    }

    struct WaitSeconds {
        duration: SceneFloatValue;
    }
```

### 控制流节点

```cfg
    struct Sequence {
        acts: list<Act>;
    }

    struct Parallel {
        acts: list<Act>;
        policy: ParallelPolicy;
    }

    struct Conditional {
        condition: SceneCondition;
        thenAct: Act;
        elseAct: Act (nullable);
    }

    struct Loop {
        count: int;  // -1 = 无限循环
        body: Act;
    }

    struct RandomSelect {
        candidates: list<WeightedAct>;
    }

    struct StateMachine {
        initialPhase: str ->phase_key (nullable);  // 默认 phases[0]
        phases: list<Phase>;
        globalTransitions: list<Transition>;
    }
```

### 状态机节点相关结构

**StateMachine 作为 Act 节点**：StateMachine 是树中的一个控制流节点，而非独立于树的顶层机制。它可以出现在 rootAct 中的任意位置 — 作为 Parallel 的一个分支、嵌套在 Sequence 中、甚至嵌套在另一个 StateMachine 的 Phase 内。

```cfg
struct Phase {
    phaseKey: str ->phase_key;
    onEnter: Act;
    onExit: Act;
    transitions: list<Transition>;
    autoAdvance: AutoAdvance;
}

struct Transition {
    condition: SceneCondition;
    target: FSMTarget;
}

interface AutoAdvance {
    struct None {}
    struct ToPhase { phaseKey: str ->phase_key; }
    struct ExitSuccess {}
    struct ExitFailure {}
}

interface FSMTarget {
    struct ToPhase { phaseKey: str ->phase_key; }
    struct ExitSuccess {}
    struct ExitFailure {}
}
```

### 作用域节点

```cfg
    struct WithActorControl {
        targets: list<ActorSelector>;
        mode: ActorControlMode;
        softTimeoutSec: float;
        body: Act;
        onAbort: Act;
    }

    struct WithStatus {
        target: ActorSelector;
        statusId: int ->status;
        body: Act;
    }

    struct WithVar {
        varKey: str ->var_key;
        initValue: SceneFloatValue;
        body: Act;
    }

    struct WithTarget {
        target: ActorSelector;
        body: Act;
    }

    struct Cinematic {
        skippable: bool;
        body: Act;
        onSkip: Act;
    }
```

**Cinematic 语义**：
- `skippable: true` 时，玩家触发跳过 → abort body（RAII 清理）→ 执行 onSkip（快进到终态）→ Succeeded
- `skippable: false` 时，等同于直接执行 body
- `onSkip` 应为短逻辑（瞬移到位、设置变量等）

### 复用节点

```cfg
    struct RunScript {
        sharedScriptId: int ->shared_scene_script;
        args: list<SceneVarBinding>;
    }

    struct RunSubScene {
        sceneId: int ->scene_definition;
        args: list<SceneVarBinding>;
        await: AwaitMode;
    }

    struct DebugLog {
        message: str;
        printVars: list<str> ->var_key;
    }
}
```

### 辅助类型

```cfg
enum AwaitMode { FireAndForget; UntilComplete; }
enum ActorControlMode { Immediate; Polite; }
enum ParallelPolicy { WaitAll; WaitAny; }

interface CameraAction {
    struct FocusOn { target: ActorSelector; blendTime: float; }
    struct Cutscene { cutsceneId: str; }
    struct Shake { preset: str; }
    struct Restore { blendTime: float; }
}

struct WeightedAct { weight: float; action: Act; }
```

### shared_scene_script

```cfg
table shared_scene_script[id] (json) {
    id: int;
    name: text;
    parameters: list<SceneVarDecl>;
    action: Act;
}
```

运行时 `RunScript` 创建隐式 localVars 作用域，写入形参默认值后用实参覆盖。

---

## Data Foundation — 原子数据类型

所有 `gameplaytag`、`stat_definition`、`event_definition` 与 GAS 共享同一注册表。

### SceneCondition — 自描述订阅的条件系统

每种原子条件自身携带 `reactToEvents`，声明哪些事件可能改变其求值结果。复合条件自动取子条件的并集。

```cfg
interface SceneCondition {
    // ── 逻辑组合 ──
    struct And { conditions: list<SceneCondition>; }
    struct Or  { conditions: list<SceneCondition>; }
    struct Not { condition: SceneCondition; }

    // ── 原子条件 ──
    struct ActorStatCompare {
        actor: ActorSelector;
        statTag: str ->stat_definition;
        op: CompareOp;
        value: SceneFloatValue;
        // reactTo: ["Event.Stat.Changed:{statTag}"]
    }

    struct ActorHasTags {
        actor: ActorSelector;
        tagQuery: TagQuery;
        // reactTo: ["Event.Tag.Changed"]
    }

    struct ActorIsAlive {
        actor: ActorSelector;
        // reactTo: ["Event.Character.Death", "Event.Character.Revive"]
    }

    struct ActorIsDead {
        actor: ActorSelector;
        // reactTo: ["Event.Character.Death", "Event.Character.Revive"]
    }

    struct SceneVarCompare {
        varKey: str ->var_key;
        op: CompareOp;
        value: SceneFloatValue;
        // reactTo: ["Event.Scene.VarChanged:{varKey}"]
    }

    struct TimeSinceSceneStart {
        op: CompareOp;
        seconds: float;
        // 引擎内部定时器驱动
    }

    struct GroupCountCompare {
        groupVar: str ->var_key;
        countCondition: GroupCountCondition;
        op: CompareOp;
        value: SceneFloatValue;
        // reactTo: ["Event.Character.Death", "Event.Group.MemberChanged:{groupVar}"]
    }

    struct ActorInZone {
        actor: ActorSelector;
        zone: ActorSelector;
        // reactTo: ["Event.Zone.ActorEntered:{zoneVar}", "Event.Zone.ActorExited:{zoneVar}"]
    }

    struct EventReceived {
        eventTag: str ->event_definition;
        source: ActorSelector;
        // reactTo: [eventTag]
    }

    struct CurrentPhaseIs {
        phaseKey: str ->phase_key;
        // reactTo: ["Event.Scene.PhaseChanged"]
    }
}

enum GroupCountCondition { Alive; Dead; Total; }
```

**引擎约定**：每种原子条件实现 `getReactiveEvents()` + `evaluate()`。

### ActorSelector

```cfg
interface ActorSelector {
    struct Inherit {}
    struct None {}
    struct SceneVar { actorVar: str ->var_key; }
    struct GroupMembers { groupVar: str ->var_key; }
    struct GroupQuery {
        groupVar: str ->var_key;
        filter: TargetFilter;
        maxCount: int;
    }
    struct NearestTo {
        referenceActor: ActorSelector;
        filter: TargetFilter;
        maxCount: int;
    }
}
```

### SceneFloatValue

```cfg
interface SceneFloatValue {
    struct Const { value: float; }
    struct FromSceneVar { varKey: str ->var_key; }
    struct Math { op: MathOp; a: SceneFloatValue; b: SceneFloatValue; }
    struct TimeBonusDecay { baseScore: float; decayPerSecond: float; }
    struct ActorStat { actor: ActorSelector; statTag: str ->stat_definition; }
}
```

---

## Runtime Core

### SceneInstance

```java
class SceneInstance {
    SceneDefinition sceneCfg;
    SceneBinding sceneBinding;

    Store globalSceneVars;
    EventBus sceneEventBus;

    float elapsedTime;
    enum SceneState { Pending, Running, Completed, Aborted }
    SceneState state = Pending;

    // 根执行树
    ActInstance rootActInstance;

    // 结局监控
    List<OutcomeMonitor> outcomeMonitors;
    SceneOutcome matchedOutcome = null;

}
```

### ActInstance — 执行树节点基类

```java
abstract class ActInstance<T extends Act> {
    T actionCfg;
    SceneInstance scene;
    ActInstance parent;
    List<ActInstance> children = new ArrayList<>();

    Store localVars = null;
    Entity contextTarget = null;

    enum ActStatus { Pending, Running, Succeeded, Failed, Aborted }
    ActStatus status = Pending;

    // ── 生命周期 ──
    final void enter() {
        status = Running;
        onStart();
    }

    final ActStatus tick(float dt) {
        if (status != Running) return status;
        ActStatus result = onTick(dt);
        if (result != Running) {
            exit(false);
        }
        return result;
    }

    final void abort() {
        if (status != Running) return;
        for (var child : children) child.abort();
        exit(true);
    }

    private void exit(boolean aborted) {
        try {
            onCleanUp(aborted);
        } finally {
            status = aborted ? Aborted : status;
        }
    }

    // ── 子类多态 ──
    protected abstract void onStart();
    protected abstract ActStatus onTick(float dt);
    protected abstract void onCleanUp(boolean aborted);
}
```

### 例子：CinematicActInstance

```java
class CinematicActInstance extends ActInstance<Cinematic> {
    ActInstance bodyInstance;
    boolean skipRequested = false;

    @Override
    protected void onStart() {
        bodyInstance = ActInstance.create(actionCfg.body, scene, this);
        bodyInstance.enter();
        if (actionCfg.skippable) {
            scene.sceneEventBus.subscribe("Event.Input.SkipCinematic", this::onSkipInput);
        }
    }

    @Override
    protected ActStatus onTick(float dt) {
        if (skipRequested) {
            bodyInstance.abort();
            var skipAction = ActInstance.create(actionCfg.onSkip, scene, this);
            skipAction.enter();
            skipAction.tick(0);
            return ActStatus.Succeeded;
        }
        ActStatus s = bodyInstance.tick(dt);
        return s != ActStatus.Running ? s : ActStatus.Running;
    }

    @Override
    protected void onCleanUp(boolean aborted) {
        if (actionCfg.skippable)
            scene.sceneEventBus.unsubscribe("Event.Input.SkipCinematic", this::onSkipInput);
        if (aborted && bodyInstance.status == ActStatus.Running)
            bodyInstance.abort();
    }
}
```

### 8.6 主循环

```java
void SceneInstance.tick(float dt) {
    if (state != Running) return;
    elapsedTime += dt;

    // 1. Tick 根执行树
    if (rootActInstance != null && rootActInstance.status == ActStatus.Running) {
        rootActInstance.tick(dt);
    }

    // 2. 评估 Outcomes（最高优先级，独立于树）
    SceneOutcome matched = evaluateOutcomes();
    if (matched != null) {
        terminate(matched);
    }
}
```

Outcome 评估独立于 rootAct 的 tick，在每帧末尾执行。一旦匹配，abort 整棵 rootAct 树，场景终止。

---

## 与 GAS / AI 系统的集成

场景系统贯彻**不直接越权操作数据**的原则：

| 场景机制 | 对接目标 | 原理 |
|:---|:---|:---|
| `ApplyEffect` | GAS Effect | 调用 `Effects.execute()`，注入场景 Context |
| `WithStatus` | GAS ActStatus | 挂载 StatusInstance，退出时 markPendingKill |
| `SendEvent` | GAS EventBus | 单向广播，GAS Trigger 可被唤醒 |
| `WaitForEvent` | GAS EventBus | 挂起场景，反向聆听战斗结算通知 |
| `WithActorControl(Immediate)` | GAS Tag + AI | 打 `State.Scene.Controlled` Tag，AI 识别后挂起 |
| `WithActorControl(Polite)` | AI 决策池 | 注入高优先级 `SceneDirected` 行为，AI 算分选中后交出控制权 |
| `WithStatus` + `AIBehaviorModifier` | AI 行为池 | 通过 ActStatus 间接修改 AI 的可用行为集 |

---

## 典型模式速查

### Boss 战三阶段

```
rootAct: Parallel { policy: WaitAny; actions: [
    // 主线：状态机驱动阶段流转
    StateMachine {
        phases: [
            Phase {
                phaseKey: "P1_Normal";
                onEnter: Sequence { actions: [
                    Cinematic { skippable: true; body: ...; onSkip: ...; };
                ]};
            },
            Phase {
                phaseKey: "P2_Enraged";
                onEnter: Sequence { actions: [...]; };
            },
            Phase {
                phaseKey: "P3_Final";
                onEnter: Sequence { actions: [...]; };
            }
        ];
        globalTransitions: [
            { condition: ActorStatCompare { actor: SceneVar { actorVar: "Cast.Boss" }; statTag: "Stat.HPPercent"; op: LessEqual; value: Const { value: 0.6 }; }; target: ToPhase { phaseKey: "P2_Enraged"; };},
            { condition: ActorStatCompare { actor: SceneVar { actorVar: "Cast.Boss" }; statTag: "Stat.HPPercent"; op: LessEqual; value: Const { value: 0.3 }; }; target: ToPhase { phaseKey: "P3_Final"; };}
        ];
    },

    // 后台：持续刷小怪
    Loop { count: -1; body: Sequence { actions: [
        WaitSeconds { duration: Const { value: 30.0 }; };
        SpawnActor { ... };
    ]}; }
]};

outcomes: [
    { condition: ActorIsDead { actor: SceneVar { actorVar: "Cast.Boss" }; }; resultCode: 1; },
    { condition: ActorIsDead { actor: SceneVar { actorVar: "Cast.Player" }; }; resultCode: 0; }
];
```

### 持续监听模式

```
Parallel { policy: WaitAll; actions: [
    // 主线脚本
    Sequence { actions: [...]; },

    // 持续监听：玩家进入危险区域时警告
    Loop { count: -1; body:
        WaitForEvent { eventTag: "Event.Zone.ActorEntered"; source: SceneVar { actorVar: "Zone.Danger" };
            conditions: [];
            timeoutSec: 0;
            onTimeout: None {};
        };
        // 触发后执行警告
        PlayCue { cueKey: "Cue.DangerWarning"; playAt: SceneVar { actorVar: "Cast.Player" }; };
    };
]};
```