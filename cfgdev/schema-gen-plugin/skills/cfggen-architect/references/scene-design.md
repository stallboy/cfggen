

# 场景逻辑系统设计 (Scene Logic System Design)

本文档定义一套与 `ability-design` (GAS) 和 `ai-design` (AI) 无缝集成的场景逻辑系统。它解决的核心问题是：**如何用数据驱动的方式，编排多实体在一段时间内的协同行为序列，并与底层物理/状态系统和中层 AI 决策系统深度联动**。

---

## Architecture Overview

场景系统（Scene System）作为游戏逻辑的最高层**编排者（Orchestrator）**，坐落在 GAS（躯体与状态）和 AI（动机与自治行为）之上。它自身不创造基础动作，而是通过向外下达指令、向内订阅事件，将分散的实体组合成有起承转合的沉浸式演出与玩法流程。

```
+-----------------------------------------------------------------+
|                   Scene Definition (Static)                     |
|                                                                 |
|  [ Actor Slots ]  [ Main Script ]  [ Side Triggers ] [ Outcomes]|
+-----------------------------------------------------------------+
                                | Instantiates
                                v
+-----------------------------------------------------------------+
|                   Scene Instance (Runtime)                      |
|                                                                 |
|  +-----------------------+        +--------------------------+  |
|  | Context & Environment |        | Parallel Monitors        |  |
|  | - Scene Variables     |        | - SideTrigger Instances  |  |
|  | - Event Subscriptions |        | - Outcome Evaluators     |  |
|  +-----------------------+        +--------------------------+  |
|                                                                 |
|  +-----------------------------------------------------------+  |
|  | SceneActionInstance Tree (Execution Flow)                 |  |
|  |                                                           |  |
|  |  [ Control Flow ] -> Sequence, Parallel, Conditional      |  |
|  |  [ Execution ]    -> SpawnActor, PlayAnimation, SetVar    |  |
|  |  [ Wait Nodes ]   -> WaitUntil, WaitForEvent              |  |
|  |  [ Scope Nodes ]  -> WithActorControl (RAII Lifecycle)    |  |
|  +-----------------------------------------------------------+  |
+-----------------------------------------------------------------+
```

### 角色定位模型

*   **GAS**：客观物理世界法则（重力、伤害、眩晕、血量）。
*   **AI**：实体个体的求生与战斗本能（寻找掩体、攻击最近敌人）。
*   **Scene**：不可见的神明之手（编排命运的相遇、强加剧情锁、判定试炼成败）。

---

## Philosophy

1. **复用，不重造**：场景系统的执行单元直接调用 GAS 的 `Effect`、`Ability`，AI 的行为修饰通过 `Status` 或注入行为完成。不引入平行的战斗逻辑体系，只做编排。
2. **执行树，非执行栈**：所有动作节点构成一棵 **SceneActionInstance 树**，复合节点与子节点共存于树中，每帧只 tick 处于 Running 状态的节点。这使 `Parallel` 的并发语义自然表达，而传统执行栈无法做到。
3. **订阅驱动，非轮询**：所有等待节点在 `onEnter` 时向 `EventBus` 注册监听，在 `onExit` 时取消注册。重新评估由事件推送触发，而非每帧主动求值。彻底告别 ECA 时代的"全局变量轮询"。
4. **作用域即生命周期 (RAII)**：有副作用的操作（占用控制权、挂载临时状态、局部变量）必须包装在对应的 `WithXXX` 作用域节点中。`onExit` 在 `finally` 语义下执行——无论正常完成还是被强制中止，清理必定发生，从根本上杜绝状态残留（Orphaned State）。
5. **Await 语义显式声明**：`PlayAnimation`、`MoveTo` 等动作默认是 `FireAndForget`（发出指令立刻返回）。如需等待完成，必须显式声明 `await: UntilComplete`，引擎通过订阅完成事件实现阻塞。直接解决"Wait 3.0 seconds 与动画时长脱节"的顽疾。
6. **结局统一终止语义**：所有终止路径（Boss 死亡、玩家死亡、超时）统一归口于 `outcomes` 列表，采用优先级仲裁机制，避免 `abortConditions` 和 `timeLimit` 的语义重叠。

---

## Runtime Core

### SceneInstance

场景运行时主体。它维护了槽位映射、局部变量树以及主控 Tick。

```java
class SceneInstance {
    Scene_definition sceneCfg;

    // 2. 全局环境
    Store globalSceneVars;
    EventBus globalEventBus;

    // 3. 运行状态
    float elapsedTime;
    enum SceneState { Pending; Running; Completed; Aborted; }
    SceneState state = Pending;

    // 4. 执行实例
    SceneActionInstance rootAction;                // 主轴树根节点
    List<SideTriggerInstance> sideTriggers;          // 旁支触发器状态机
    List<SceneActionInstance> runningSideActions;  // 旁支触发器孵化出的活跃小树

    //结局监控 ───
    List<OutcomeListener> outcomeListeners;
    List<SceneOutcome> matchedOutcomes = new ArrayList<>();
}
```

### SceneActionInstance

执行树的节点基类，是场景系统运行时的核心抽象。

```java
abstract class SceneActionInstance<T extends SceneAction> {
    T actionCfg;      // 绑定的静态配置
    SceneInstance scene;        // 所属的场景大实例
    SceneActionInstance parent; // 父实例
    List<SceneActionInstance> children = new ArrayList<>();

    Store localVars = null;        // 局部变量寄存器
    enum Status { Pending, Running, Succeeded, Failed, Aborted }
    Status status = Pending;       // 实例运行状态

    // 生命周期与状态机流转
    final void enter() { ... }
    final Status tickInstance(float dt) { ... } // 替代原 tickContext
    final void abort() { ... }
    private void exit(boolean aborted) { ... }

    // 供具体节点实现的多态方法
    protected abstract void onStart();
    protected abstract Status onTick(float dt);
    protected abstract void onCleanUp(boolean aborted);

    // 工厂方法：通过 SceneAction 配置孵化出对应的 SceneActionInstance
    static SceneActionInstance create(SceneAction cfg, SceneInstance scene, SceneActionInstance parent) { ... }
}
```

---

## 配置定义

### scene_definition

场景定义表，是整个场景系统的最上层配置入口。

```cfg
table scene_definition[sceneId] (json) {
    sceneId: int;
    name: text;
    sceneTags: list<str> ->gameplaytag;

    actorSlots: list<SceneActorSlot>;
    environmentAnchors: list<SceneEnvironmentAnchor>;

    // 静态前置条件：加载时检查一次
    prerequisites: list<SceneCondition>;

    // 场景激活条件（满足则实例化该场景）
    activation: SceneActivation;

    // 1. 主轴时间线
    // 负责强时序的演出、Boss 阶段转换、核心对话。
    // 依然是一棵单根的树，一眼看清起承转合。
    script: SceneAction;

    // 2. 旁支触发器
    // 负责在场景运行期间，处理非线性的、碎片的规则。
    // 例如："只要触碰陷阱就扣血"、"每死一个小怪就加分"
    sideTriggers: list<SceneSideTrigger>;

    // 结局定义：统一承载终止条件、奖励、计分与超时
    outcomes: list<SceneOutcome>;

    // 场景变量初始化
    initVars: list<SceneVarInit>;
}

struct SceneActivation {
    condition: SceneCondition;
    // 声明哪些事件可能导致条件变化，引擎只在这些事件发生时重新 evaluate
    reactToEvents: list<str> ->event_definition;
}

struct SceneSideTrigger {
    // 触发条件
    condition: SceneCondition;
    reactToEvents: list<str> ->event_definition;

    // 是否可以重复触发？
    // false = 触发一次后该 trigger 永久失效
    // true = 每次条件满足且不在 cooling down 时均可触发
    isRepeatable: bool;
    cooldownSec: float;

    // 触发后执行的动作树。
    // 架构约束：为了避免与主 script 冲突，建议这里的 action
    // 尽量是 FireAndForget 的短逻辑 (如 DoEffect, SendEvent, SetVar)，
    // 或者对与主轴无关的环境实体操作。
    action: SceneAction;
}

struct SceneActorSlot {
    slotVar: str ->var_key;
    aiArchetype: str ->ai_archetype;
    spawnPoint: str;
    initialStatuses: list<int> ->status;
    statOverrides: list<SceneStatOverride>;
    removeOnDeath: bool;
}

struct SceneEnvironmentAnchor {
    slotVar: str ->var_key;
    spawnPoint: str;
    initialStatuses: list<int> ->status;
}

struct SceneStatOverride {
    statKey: str ->stat_definition;
    op: ModifierOp;
    value: float;
}

struct SceneVarInit {
    varKey: str ->var_key;
    initValue: float;
}

struct SceneOutcome {
    outcomeTag: str ->gameplaytag;
    condition: SceneCondition;
    reactToEvents: list<str> ->event_definition;

    // true = 匹配后立即终止场景（如失败、超时）
    // false = 记录结果但继续执行（如多目标评分场景）
    terminateScene: bool;

    // 并发匹配时的优先级冲裁（数值越大越优先）
    // 建议：死亡=100, 超时=50, 胜利=10
    priority: int;

    rewards: list<SceneReward>;
}


struct SceneReward {
    // 指向经济系统的专属掉落包 ID（Reward / Drop / Loot Table）
    rewardPackId: int -> reward_pack;

    // 发放对象（通常是玩家，或者全体参与者）
    rewardTarget: SceneActorSelector;
}
```


---

## SceneAction 节点

`SceneAction` 是场景脚本的执行单元，支持任意嵌套组合。

```cfg
interface SceneAction {


    // --- 基础动作节点
    struct PlayAnimation {
        target: SceneActorSelector;
        animName: str;
        blendInTime: float;
        await: AwaitMode;
        stopOnAbort: bool;
    }

    struct Dialogue {
        speaker: SceneActorSelector;
        dialogueId: int ->dialogue;
        await: AwaitMode;
    }

    struct Camera {
        action: CameraAction;
        await: AwaitMode; // 通常 FireAndForget
    }

    struct DoEffect {
        target: SceneActorSelector;
        effect: Effect;
        await: AwaitMode;
    }

    struct CastAbility {
        caster: SceneActorSelector;
        abilityId: int ->ability;
        target: SceneActorSelector;
        await: AwaitMode;
    }

    struct SendEvent {
        target: SceneActorSelector; // 使用 None 表示全局广播
        eventTag: str ->event_definition;
        magnitude: float;
        extras: list<VarBinding>;
    }

    struct SpawnActor {
        outputVarKey: str ->var_key;
        aiArchetype: str ->ai_archetype (nullable);
        spawnPoint: str;
        initialStatuses: list<int> ->status;
    }

    struct SetSceneVar {
        varKey: str ->var_key;
        op: ModifierOp;
        value: float;
    }

    struct PlayCue {
        cueKey: str -> cue_key;
        playAt: SceneActorSelector;  // None = 全局 Cue（如屏幕震动、全局 UI 提示）
    }

    struct MoveTo {
        actor: SceneActorSelector;     // 谁在移动
        target: SceneActorSelector;     // 移动到哪（Actor 位置 或 锚点位置）
        speedOverride: SceneFloatValue;    // 移动速度覆写，Const{-1} = 使用实体默认速度
        tolerance: float;                  // 到达距离容差
        await: AwaitMode;
    }

    struct Interact {
        actor: SceneActorSelector; // 谁发起交互
        interactTo: SceneActorSelector; // 与谁交互
        interactionId: int;
        await: AwaitMode;
    }


    // --- 等待节点

    // 运行时：onEnter 订阅，onExit 取消订阅，不轮询
    struct WaitForEvent {
        eventTag: str ->event_definition;
        source: SceneActorSelector; // None = 监听所有来源
        conditions: list<SceneCondition>;
        timeoutSec: float;
        onTimeout: SceneAction (nullable);
    }

    // 运行时：onEnter 求值一次；随后只在 reactToEvents 触发时重新求值
    struct WaitUntil {
        condition: SceneCondition;
        reactToEvents: list<str> ->event_definition;
        timeoutSec: float;
    }

    struct WaitSeconds {
        duration: SceneFloatValue;
    }

    // --- 控制流节点

    struct Sequence { actions: list<SceneAction>; }

    // WaitAll: 均 Succeeded 时成功
    // WaitAny: 任意 Succeeded 时成功，并自动 Abort 其他 Running 子节点
    struct Parallel {
        actions: list<SceneAction>;
        policy: ParallelPolicy;
    }

    struct Conditional {
        condition: SceneCondition;
        thenAction: SceneAction;
        elseAction: SceneAction (nullable);
    }

    struct Loop {
        count: int; // -1 表示无限循环
        body: SceneAction;
    }

    // --- 作用域节点，退出时保证执行清理，绝不残留

    // 控制权作用域：结束后自动归还控制权给 AI
    struct WithActorControl {
        targets: list<SceneActorSelector>;
        mode: ActorControlMode;
        softTimeoutSec: float;
        body: SceneAction;
        onAbort: SceneAction;   // 清理动作，强制 FireAndForget
    }

    // 状态作用域：结束后精确移除挂载的该 Status 实例
    struct WithStatus {
        target: SceneActorSelector;
        statusId: int ->status;
        body: SceneAction;
    }

    // 变量作用域
    struct WithVar {
        varKey: str ->var_key;
        initValue: float;
        body: SceneAction;
    }


    // --- 实用与复用节点

    struct ScriptRef {
        sharedScriptId: int ->shared_scene_script;
    }

    // 调用独立的子场景（如播片、特殊小游戏）
    struct RunSubScene {
        sceneId: int ->scene_definition;
        await: AwaitMode;
    }

    // 调试辅助
    struct DebugLog {
        message: str;
        printVars: list<str> ->var_tag;
    }
}

enum AwaitMode {
    // 发出指令，立刻返回 Succeeded，不等待动作物理完成（发射后不管）
    // 适用：Camera切换、DoEffect瞬间生效
    FireAndForget;

    // 挂起节点，等待引擎广播该动作的专属完成事件后才返回 Succeeded
    // 适用：PlayAnimation(等播完)、Dialogue(等点掉)、CastAbility(等技能结束)
    UntilComplete;
}

enum ActorControlMode {
    // 立即授予 State.Scene.Controlled Tag，触发全局拦截，AI 当帧物理宕机
    Immediate;
    // 注入高优先行为，等 AI 在 minCommitmentTime 后主动让权。超时则降级为 Immediate
    Polite;
}

interface CameraAction {
    struct FocusOn { target: SceneActorSelector; blendTime: float; }
    struct Cutscene { cutsceneId: str; }
    struct Shake { preset: str; }
    struct Restore { blendTime: float; }
}

table shared_scene_script[id] (json) {
    id: int;
    name: text;
    action: SceneAction;
}
```
---

## Data Foundation

本章节定义原子数据类型。所有 `gameplaytag`、`stat_definition`、`event_definition` 均与 `ability-design` 共享同一张注册表。

```cfg
interface SceneCondition {
    struct And { conditions: list<SceneCondition>; }
    struct Or { conditions: list<SceneCondition>; }
    struct Not { condition: SceneCondition; }

    struct ActorStatCompare {
        actor: SceneActorSelector;
        statTag: str ->stat_definition;
        op: CompareOp;
        value: float;
    }
    struct ActorHasTags {
        actor: SceneActorSelector;
        tagQuery: GameplayTagQuery;
    }
    struct ActorIsAlive { actor: SceneActorSelector; }
    struct ActorIsDead { actor: SceneActorSelector; }
    struct SlotIsActive { slotTag: str ->gameplaytag; }
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

interface SceneActorSelector {
    struct None {}  // 显式空目标，替代 Nullable，避免运行时 NPE
    struct SlotActor { slotTag: str ->gameplaytag; }
    struct AllInSlot { slotTag: str ->gameplaytag; }
    struct SceneVar { actorVar: str ->var_key; }
    struct NearestToSlot {
        referenceSlot: str ->var_key;
        filter: TargetFilter;
        maxCount: int;
    }
}

interface SceneFloatValue {
    struct Const { value: float; }
    struct SceneVarValue { varTag: str ->gameplaytag; }
    struct Math { op: MathOp; a: SceneFloatValue; b: SceneFloatValue; }
    // 线性时间衰减：value = base - decayPerSecond * elapsedTime
    struct TimeBonusDecay {
        baseScore: float;
        decayPerSecond: float;
    }
}
```

---

## 与 GAS / AI 系统的集成关系

场景系统通过以下接口与两个系统交互，贯彻**不直接越权操作数据**的原则。

| 场景节点 / 机制 | 对接目标 | 原理闭环 |
| :--- | :--- | :--- |
| `DoEffect` | GAS Effect | 直接调用 `Effects.execute()`，注入场景 Context。 |
| `WithStatus` | GAS Status | 挂载 `StatusInstance`，退出时调用 `markPendingKill()`。如果该 Status 内部包含 `AIBehaviorModifier`，即可顺畅实现**场景改变 AI 池**，场景不直接调 AI。 |
| `SendEvent` | GAS EventBus | 单向广播（FireAndForget），GAS Trigger 可被唤醒。 |
| `WaitForEvent` | GAS EventBus | 挂起场景自身，反向聆听战斗结算通知。 |
| `WithActorControl(Immediate)`| GAS Tag + AI | 给实体打上 `State.Scene.Controlled`。AI 的 `global_ai_settings` 识别此 Tag，强制清空任务栈挂起大脑。 |
| `WithActorControl(Polite)`| AI 决策池 | 往 AI 的决策池中注入一个 `interruptPriority` 极高的 `SceneDirected` 行为。AI 算分选中后交出躯体。 |

---

## Examples

### 例 A：炎魔领主 Boss 战（转阶段与 AI 托管）

展示一阶段自治 AI、转阶段 `WithActorControl` 硬夺权、二阶段通过 `WithStatus` 替换行为池。

```
scene_definition {
    sceneId: 7001;
    name: "炎魔领主试炼";
    sceneTags: ["Scene.Type.Boss"];

    actorSlots: [
        {
            slotVar: "Var.Slot.MagmaLord";
            aiArchetype: "Boss_MagmaLord";
            spawnPoint: "Boss_Arena_Center";
            initialStatuses: [];
            statOverrides: [];
            removeOnDeath: true;
        },
        {
            slotVar: "Var.Slot.Player";
            aiArchetype: null;
            spawnPoint: "Arena_Entrance";
            initialStatuses: [];
            statOverrides: [];
            removeOnDeath: false;
        }
    ];

    environmentAnchors: [
        {
            slotVar: "Var.Slot.ArenaCenter";
            spawnPoint: "Arena_Center_Marker";
            initialStatuses: [];
        }
    ];

    initVars: [
        { varKey: "Var.Scene.Phase"; initValue: 1.0; }
    ];

    script: Sequence { actions: [

        // ① 入场演出（硬夺权）
        WithActorControl {
            targets: [SceneVar { actorVar: "Var.Slot.MagmaLord"; }];
            mode: Immediate;
            softTimeoutSec: 0.0;
            body: Sequence { actions: [
                Camera { action: FocusOn { target: SceneVar { actorVar: "Var.Slot.MagmaLord"; }; blendTime: 1.0; }; await: FireAndForget; };
                PlayAnimation {
                    target: SceneVar { actorVar: "Var.Slot.MagmaLord"; };
                    animName: "entrance_roar";
                    blendInTime: 0.3;
                    await: UntilComplete;
                    stopOnAbort: true;
                };
                Dialogue { speaker: SceneVar { actorVar: "Var.Slot.MagmaLord"; }; dialogueId: 5001; await: UntilComplete; };
                Camera { action: Restore { blendTime: 0.8; }; await: FireAndForget; };
            ]};
            onAbort: PlayAnimation { target: SceneVar { actorVar: "Var.Slot.MagmaLord"; }; animName: "idle"; blendInTime: 0.2; await: FireAndForget; stopOnAbort: false; };
        };

        // 授予 P1 标记，AI 接管战斗
        DoEffect {
            target: SceneVar { actorVar: "Var.Slot.MagmaLord"; };
            effect: GrantTemporaryTags { duration: Const { value: -1.0; }; grantedTags: ["Scene.MagmaLord.Phase1"]; };
            await: FireAndForget;
        };

        // ② 第一阶段：等待 HP 下降
        WaitUntil {
            condition: ActorStatCompare {
                actor: SceneVar { actorVar: "Var.Slot.MagmaLord"; };
                statTag: "Stat.HP.Percent";
                op: LessEqual;
                value: 0.3;
            };
            reactToEvents: ["Event.Combat.Damage.Post"];
            timeoutSec: -1.0;
        };

        // ③ 转阶段过场
        WithActorControl {
            targets: [SceneVar { actorVar: "Var.Slot.MagmaLord"; }];
            mode: Immediate;
            softTimeoutSec: 0.0;
            body: Sequence { actions: [
                SetSceneVar { varKey: "Var.Scene.Phase"; op: Override; value: 2.0; };
                PlayCue { cueKey: "Cue.UI.BossAlert.MagmaLord.PhaseChange"; playAt: None {}; };

                // 霸体保护下播嘶吼动画
                WithStatus {
                    target: SceneVar { actorVar: "Var.Slot.MagmaLord"; };
                    statusId: 4010; // 霸体保护 Status ID
                    body: PlayAnimation {
                        target: SceneVar { actorVar: "Var.Slot.MagmaLord"; };
                        animName: "phase2_roar";
                        blendInTime: 0.2;
                        await: UntilComplete;
                        stopOnAbort: false;
                    };
                };
                
                // 彻底挂载飞行 Status，内部包含了 AIBehaviorModifier 注入 P2 行为池
                DoEffect {
                    target: SceneVar { actorVar: "Var.Slot.MagmaLord"; };
                    effect: ApplyStatus { statusId: 4002; captures: []; };
                    await: FireAndForget;
                };
            ]};
            onAbort: null;
        };

        // ④ 剩余时间交由玩家与 AI 战斗，直到 Outcomes 触发...
        WaitForEvent {
            eventTag: "Event.Character.Death";
            source: SceneVar { actorVar: "Var.Slot.MagmaLord"; };
        };
    ]};

    outcomes: [
        {
            outcomeTag: "Scene.Result.Victory";
            condition: ActorIsDead { actor: SceneVar { actorVar: "Var.Slot.MagmaLord"; }; };
            reactToEvents: ["Event.Character.Death"];
            terminateScene: true;
            priority: 10;
            rewards: [
                { rewardPackId: 9001; rewardTarget: SceneVar { actorVar: "Var.Slot.Player"; }; }
            ];
        },
        {
            outcomeTag: "Scene.Result.Defeat";
            condition: ActorIsDead { actor: SceneVar { actorVar: "Var.Slot.Player"; }; };
            reactToEvents: ["Event.Character.Death"];
            terminateScene: true;
            priority: 100;
        }
    ];
}
```