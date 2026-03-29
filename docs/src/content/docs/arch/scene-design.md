---
title: 场景系统设计
sidebar:
  order: 20
---

场景系统（Scene System）定位为遭遇战/演出级的逻辑编排层，位于 GAS（状态与效果）与 AI（自治决策）之上、关卡与任务流程系统之下。它不负责实现基础战斗行为，而是通过驱动既有能力、施加约束、订阅运行时事件，将多个实体在一段时间内组织成可控、可复用、可数据化配置的玩法与演出流程。

## Architecture Overview

### 层级定位

| 层级 | 系统 | 职责边界 |
|:---|:---|:---|
| **底层** | GAS | 伤害、状态、Tag、Effect、Stat |
| **中层** | AI | 寻路、攻击选择、行为决策 |
| **上层** | Scene （我在这）| 单场遭遇战/演出的编排 |
| **顶层** | 关卡/任务 | 房间连接、进度存档、全局流转 |

### 场景接口 — 逻辑与空间分离

场景定义（scene_definition）是**纯逻辑编排**，不含任何世界坐标。空间信息通过 signature 注入。

### 核心架构决策

**一切皆树（Act Tree）**。场景定义的执行体是一棵 `rootAct` 树。FSM（状态机）作为树中的一个节点类型（`StateMachine`）存在，而非独立于树的机制。

- **宏观流转**用 `StateMachine` 节点 — Phase 间互斥跳转
- **微观时序**用 `Sequence`/`Parallel` 等控制流 — 帧级动作编排
- **持续监听**用 `Parallel + Loop + WaitForEvent` — 无独立 Monitor 机制

所有运行时逻辑共享同一套生命周期、同一套 abort/RAII 清理路径。


### 外部通信

场景通过 `signature`（输入参数）接收外部指令，通过 `outcomes`（结局码）向外部汇报结果。两者之间低耦合通信。


### Philosophy

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


## Runtime Core

### SceneInstance

Outcome 评估独立于 rootAct 的 tick，在每帧开始执行。一旦匹配，abort 整棵 rootAct 树，场景终止。

```java
class SceneInstance {
    Scene_definition sceneCfg;
    
    // 实例状态, 原地被改变，跨节点共享
    Store instanceState; 
    // 根执行树
    ActInstance<?> rootAct;
    // 结局监控
    List<OutcomeMonitor> outcomeMonitors;
    SceneContext ctx;

    void tick(float dt) {
        if (rootAct != null) {
            rootAct.tick(ctx, dt);
        }
        SceneOutcome matched = evaluateOutcomes();
        if (matched != null) {
            terminate(matched);
        }

    }
}
```

### SceneContext

```java
record SceneContext(
    SceneInstance scene,

    // WithActorControl时，计算targets，建新的SceneContext
    List<Actor> targets; 
    // 局部作用域：仅对当前及子节点树有效
    // WithLocalVar时，建新的localScope和SceneContext
    ReadOnlyStore localScope 
) {}
```

## scene_definition

```cfg
table scene_definition[sceneId] (json) {
    sceneId: int;
    name: text;

    // 函数签名（入参声明）
    signature: list<SceneVarDecl>;

    // 结局裁定，按列表顺序，首个满足的 outcome 胜出
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

### Act

```cfg
interface Act {
    // --- 基础节点
    struct None {}

    struct Camera {
        action: CameraAction;
        await: AwaitMode;
    }

    struct ApplyEffect {
        target: ActorSelector;
        effect: Effect;
    }

    struct SendEvent {
        target: ActorSelector;
        event: str ->event_definition;
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

    struct DebugLog {
        message: str;
        printVars: list<str> ->var_key;
    }

    struct SetSceneVar { // 设置到instanceState里，共享
        bindings: list<SceneVarBinding>;
    }

    struct RunScript { // 复用
        sharedScriptId: int ->shared_scene_script;
        args: list<SceneVarBinding>;
        await: AwaitMode;
    }

    // --- 等待节点
    struct WaitForEvent {
        event: str ->event_definition;
        source: ActorSelector;
        conditions: list<SceneCondition>;
        extractPayloads: list<VarBindingByPayload>;
        timeoutSec: float;
        onTimeout: Act;
        body: Act;
    }

    struct WaitUntil {
        condition: SceneCondition;
        timeoutSec: float;
        onTimeout: Act;
    }

    struct WaitSeconds {
        duration: SceneFloatValue;
    }

    // --- 控制流
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

    // --- 作用域
    struct WithActorControl {
        targets: ActorSelector;
        mode: ActorControlMode;
        body: Act;
    }

    struct WithStatus {
        target: ActorSelector;
        statusId: int ->status;
        body: Act;
    }

    struct WithLocalVar {  // 新建localScope，SceneContext
        bindings: list<SceneVarBinding>;
        body: Act;
    }

    struct Cinematic {
        skippable: bool;
        body: Act;
        onSkip: Act;
    }

    // --- 专属的“傀儡”行为，必须在WithActorControl的body里
    struct PlayAnimation {
        animName: str;
        blendInTime: float;
        await: AwaitMode;
    }

    struct MoveTo {
        destination: ActorSelector;
        speedOverride: SceneFloatValue;
        tolerance: float;
        await: AwaitMode;
    }

    struct CastAbility {
        abilityId: int ->ability;
        abilityTarget: ActorSelector;
        await: AwaitMode;
    }
    
    struct Interact {
        interactTo: ActorSelector;
        interactionId: int;
        await: AwaitMode;
    }

    struct Dialogue {
        dialogueId: int ->dialogue;
        await: AwaitMode;
    }
}

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

### Binding相关

```cfg
struct SceneVarBinding {
    varKey: str ->var_key;
    value: SceneVarValue;
}

interface SceneVarValue {
    struct Float { value: SceneFloatValue; }
    struct Actors { selector: ActorSelector; }
}

struct VarBindingByPayload {
    writeToVar: str ->var_key; // 写入到场景的变量名
    payloadKey: str;           // 事件中的参数名
}
```

### StateMachine相关

StateMachine 是树中的一个控制流节点，而非独立于树的顶层机制。它可以出现在 rootAct 中的任意位置 — 作为 Parallel 的一个分支、嵌套在 Sequence 中、甚至嵌套在另一个 StateMachine 的 Phase 内。

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
    target: FsmTarget;
}

interface AutoAdvance {
    struct None {}
    struct ToPhase { phaseKey: str ->phase_key; }
    struct ExitSuccess {} // 为了能退出StateMachine
    struct ExitFailure {}
}

interface FsmTarget {
    struct ToPhase { phaseKey: str ->phase_key; }
    struct ExitSuccess {}
    struct ExitFailure {}
}
```


### Cinematic 语义
- `skippable: true` 时，玩家触发跳过 → abort body（RAII 清理）→ 执行 onSkip（快进到终态）→ Succeeded
- `skippable: false` 时，等同于直接执行 body
- `onSkip` 应为短逻辑（瞬移到位、设置变量等）






运行时 `RunScript` 创建隐式 localVars 作用域，写入形参默认值后用实参覆盖。

---

## Data Foundation

所有 `gameplaytag`、`stat_definition`、`event_definition` 与 GAS 共享同一注册表。

### SceneCondition 

自描述订阅的条件系统，其实现接口为：

```java
class SceneConditions {
    static boolean test(SceneCondition cfg, SceneContext ctx);

    // 每种原子条件自身携带 `reactToEvents`，
    // 声明哪些事件可能改变其求值结果。复合条件自动取子条件的并集。
    static list<Event_defintion> getReactiveEvents(SceneCondition cfg);
}
```


```cfg
interface SceneCondition {
    // ── 逻辑组合 ──
    struct And { conditions: list<SceneCondition>; }
    struct Or  { conditions: list<SceneCondition>; }
    struct Not { condition: SceneCondition; }

    // ── 原子条件 ──
    struct ActorStatCompare {
        actor: ActorSelector;
        quantifier: Quantifier;
        stat: str ->stat_definition;
        op: CompareOp;
        value: SceneFloatValue;
        // reactTo: ["Stat_Changed:{stat}"]
    }

    struct ActorHasTags {
        actor: ActorSelector;
        quantifier: Quantifier;
        tagQuery: TagQuery;
        // reactTo: ["Tag_Changed"]
    }

    struct ActorIsAlive {
        actor: ActorSelector;
        quantifier: Quantifier;
        // reactTo: ["Character_Death", "Character_Revive"]
    }

    struct ActorIsDead {
        actor: ActorSelector;
        quantifier: Quantifier;
        // reactTo: ["Character_Death", "Character_Revive"]
    }

    struct ActorInZone {
        actor: ActorSelector;
        quantifier: Quantifier;
        zone: ActorSelector;     // zone 始终取 [0]
        // reactTo: ["Zone_ActorEntered:{zoneVar}", "Zone_ActorExited:{zoneVar}"]
    }

    struct SceneVarCompare {
        varKey: str ->var_key;
        op: CompareOp;
        value: SceneFloatValue;
        // reactTo: ["Scene_VarChanged:{varKey}"]
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
        // reactTo: ["Character_Death", "Group_MemberChanged:{groupVar}"]
    }

    struct CurrentPhaseIs {
        phaseKey: str ->phase_key;
        // reactTo: ["Scene_PhaseChanged"]
    }

    struct RootActFinished {
        expectedStatus: ActFinishStatus;
        // reactTo: ["Scene_RootActFinished"]
    }
}

enum Quantifier { Any; All; }  // 默认 All
enum GroupCountCondition { Alive; Dead; Total; }
enum ActFinishStatus { Success; Failed; }
```

### ActorSelector

返回actor列表，接口如下：
```java
class ActorSelectors {
    static List<Actor> select(ActorSelector cfg, SceneContext ctx);
}
```

```cfg
interface ActorSelector {
    struct ContextTargets {}
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

根据节点在架构中的定位，Actor 的目标获取分为四大类场景：

**1. 宏观节点：需要单个 Actor / 位置（ForOne 语义）**
通过 `ActorSelector` 显式查询，取求值结果的 `list[0]`，若 list 为空则报错或跳过当前节点。
* `SpawnActor.spawnAt`（只在一个位置生成）
* `Camera.FocusOn.target`（镜头只能聚焦一个主目标）

**2. 宏观节点：允许多个 Actor（ForEach 语义）**
通过 `ActorSelector` 显式查询，对求值结果 `list` 中的每个 Actor 逐一执行。
* `WithActorControl.targets`（劫持列表中的所有目标）
* `ApplyEffect.target`（给列表中的每个目标挂载状态）
* `SendEvent.target`（给列表中的每个目标发送事件）

**3. 躯干控制节点：隐式消费上下文（Implicit Target）**
像 `MoveTo`、`PlayAnimation`、`Interact` 等躯体动作**没有** `target` 字段！它们必须被包裹在 `WithActorControl` 内部，并直接读取被附身的 `SceneContext.targets` 执行。
* **隐式 ForEach**：`PlayAnimation`、`MoveTo` 会让所有受控者一起播动画、一起走（适合群体检阅/走位演出）。
* **隐式 ForOne**：`Dialogue`、`Interact` 在逻辑上强依赖单体表现，引擎底层自动取 `targets[0]` 执行。

**4. 条件判断：需要量化（Quantifier 语义）**
在 `SceneCondition` 中，当面对可能有多个 Actor 的查询结果时，必须引入显式的 `Quantifier` 来消除歧义。
* `ActorIsDead { quantifier: All, ... }`（是否全死了）
* `ActorHasTags { quantifier: Any, ... }`（是否其中任何一个带有该 Tag）

例如
```
WithActorControl {
    targets: SceneVar { actorVar: "Cast.Boss" };
    mode: Immediate;
    body: Sequence { actions: [
        PlayAnimation { animName: "entrance_roar"; await: UntilComplete; };
        Dialogue { dialogueId: 5001; await: UntilComplete; };
        ApplyEffect { effect: ...; await: FireAndForget; };
    ]};
};
```

### SceneFloatValue

接口为：
```java
class SceneFloatValues {
    static float evaluate(SceneFloatValue cfg, SceneContext ctx);
}
```

```cfg
interface SceneFloatValue {
    struct Const { value: float; }
    struct FromSceneVar { varKey: str ->var_key; }
    struct Math { op: MathOp; a: SceneFloatValue; b: SceneFloatValue; }
    struct TimeBonusDecay { baseScore: float; decayPerSecond: float; }
    struct ActorStat { actor: ActorSelector; stat: str ->stat_definition; }
}
```

---

## 实现细节

### ActInstance

```java
enum ActStatus { Pending, Running, Success, Failed, Aborted }

abstract class ActInstance<T extends Act> {
    T actCfg;

    private ActStatus status = ActStatus.Pending;

    public final ActStatus tick(SceneContext ctx, float deltaTime) {
        if (status == ActStatus.Pending) {
            status = ActStatus.Running;
            onStart(ctx); 
        }

        if (status != ActStatus.Running) {
            return status;
        }
        
        status = onTick(ctx, deltaTime); 
        
        if (status != ActStatus.Running) {
            onEnd(ctx, false);
        }
        return status;
    }

    public final void abort(SceneContext ctx) {
        if (status == ActStatus.Pending) {
            status = ActStatus.Aborted;
            return;
        }
        if (status != ActStatus.Running) return;
        var children = getActiveChildren();
        if (children != null) {
            for (var child : children) {
                if (child != null) child.abort(ctx);
            }
        }

        onEnd(ctx, true);
        status = ActStatus.Aborted;
    }

    abstract void onStart(SceneContext ctx);
    abstract ActStatus onTick(SceneContext ctx, float deltaTime);
    abstract void onEnd(SceneContext ctx, boolean wasAborted);

    protected Iterable<ActInstance<?>> getActiveChildren() {
        return null; 
    }
}
```

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
| `WithStatus` + `AIModifier` | AI 行为池 | 通过 ActStatus 间接修改 AI 的可用行为集 |

---

## 典型模式速查

### Boss 战三阶段

```
rootAct: Parallel { policy: WaitAny; acts: [
    // 主线：状态机驱动阶段流转
    StateMachine {
        phases: [
            Phase {
                phaseKey: "P1_Normal";
                onEnter: Sequence { acts: [
                    Cinematic { skippable: true; body: ...; onSkip: ...; };
                ]};
            },
            Phase {
                phaseKey: "P2_Enraged";
                onEnter: Sequence { acts: [...]; };
            },
            Phase {
                phaseKey: "P3_Final";
                onEnter: Sequence { acts: [...]; };
            }
        ];
        globalTransitions: [
            { condition: ActorStatCompare { actor: SceneVar { actorVar: "Cast.Boss" }; 
                stat: "Stat.HPPercent"; op: LessEqual; value: Const { value: 0.6 }; }; 
                target: ToPhase { phaseKey: "P2_Enraged"; };},
            { condition: ActorStatCompare { actor: SceneVar { actorVar: "Cast.Boss" }; 
                stat: "Stat.HPPercent"; op: LessEqual; value: Const { value: 0.3 }; }; 
                target: ToPhase { phaseKey: "P3_Final"; };}
        ];
    },

    // 后台：持续刷小怪
    Loop { count: -1; body: Sequence { acts: [
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
Parallel { policy: WaitAll; acts: [
    // 主线脚本
    Sequence { acts: [...]; },

    // 持续监听：玩家进入危险区域时警告
    Loop { count: -1; body:
        WaitForEvent { event: "Zone_ActorEntered";
            source: SceneVar { actorVar: "Zone.Danger" };
        };
        // 触发后执行警告
        PlayCue { cueKey: "Cue.DangerWarning"; playAt: SceneVar { actorVar: "Cast.Player" }; };
    };
]};
```