---
title: Ability 施法过程
sidebar:
  order: 1
---

基于全数据驱动理念与深度架构推演，本模型旨在彻底解决动作游戏与 RPG 中复杂的技能生命周期管理问题。

## 设计原则

1. **瞬发零开销**：`Instant` 模型等同于极简的"扣资源-触发"行为，不被复杂的生命周期拖累。
2. **生命周期原生托管**：前摇（Startup）、引导（Channel）甚至后摇（Recovery），都是技能**真实占用角色时间**的生命周期阶段，必须由 `AbilityInstance` 原生接管，以保证动作取消、UI 表现、并发锁的精确性。
3. **打断与取消双轨语义**：TagRules 提供 `interruptsAbilities`（硬打断，有惩罚）和 `cancelsAbilities`（软取消，无惩罚）两种打断动词。技能在前摇时被眩晕是"打断"，在后摇时被翻滚截断是"动作取消"，底层逻辑完全自洽。
4. **最少时间原语**：只保留不可互相还原的四种时间模型（Instant/Startup/Charge/Channel）。连招、形态切换等复合需求通过"标签 + 组合"实现。

---

## Ability 表结构

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    description: text;

    abilityTags: list<str> ->gameplaytag;
    
    // 准入检查
    activationConditions: list<Condition>;
    costs: list<StatCost>;
    cooldown: FloatValue;

    // 瞄准与输入要求
    targeting: TargetingRequirement;
    
    // 施法方式
    castMode: CastMode;

    // 技能的核心动作（出伤、发子弹、挂 Buff 等）
    effect: Effect;

    // Processing 阶段被 interruptsAbilities 打断时的惩罚动作
    onInterrupt: list<Effect>;

    // 技能主体逻辑执行完毕后的收招阶段
    recovery: RecoveryConfig;
}

struct StatCost {
    stat: str ->stat_definition;
    value: FloatValue;
}
```

---

## 瞄准与输入 (Targeting)

`TargetingRequirement` 定义了技能激活前需要收集什么输入，以及在持续施法期间对输入的容忍策略。

```cfg
interface TargetingRequirement {
    // 无需外部输入（如：战吼、自身Buff）
    struct None {}

    // 需要选中一个实体
    struct SingleTarget {
        allowedRelations: list<Relation>;
        tagQuery: TagQuery;
        maxRange: FloatValue;
        onTargetLost: TargetLostPolicy;
    }

    // 需要指定一个地点（如：火雨）
    struct PointTarget {
        maxRange: FloatValue;
    }

    // 需要指定一个方向（如：非指向性冲刺、扫射射线）
    struct DirectionalTarget {}
}

enum TargetLostPolicy {
    Cancel;     // 目标死亡或超出范围，触发 cancel
    Continue;   // 继续施法（对空放）
}
```

### 动态 Targeting 数据流

引擎在 `Activate` 时，将瞄准数据写入 `instanceState` 中。
引擎在 `Processing` 阶段（蓄力、引导期间）**每帧根据玩家鼠标/摇杆实时更新这些变量**。当结算 `effect` 时，读取到的永远是玩家动作那一刻的最精确输入。

| Targeting 类型 | context.target 初始值 | instanceState 写入 |
|---|---|---|
| `None` | 施法者自身 | 无 |
| `SingleTarget` | 选中的目标 Actor | `targetingActor` = 选中目标 |
| `PointTarget` | 施法者自身 | `targetingPoint` = 选中地点 |
| `DirectionalTarget`| 施法者自身 | `targetingDir` = 选中方向 |

**SingleTarget 的 Processing 阶段追踪**：每帧验证目标存活 ∧ 目标在 maxRange 内 ∧ 目标满足 tagQuery。验证失败时按 `onTargetLost` 处理（`Cancel` 触发 cancel，`Continue` 不处理）。`PointTarget` 和 `DirectionalTarget` 的数据为标量，不存在"丢失"概念，无需追踪。

---

## 施法方式 (CastMode)

```cfg
interface CastMode {
    // 瞬发模型
    struct Instant {}

    // 前摇模型
    struct Startup {
        startupTime: FloatValue;
        
        startupTags: list<str> ->gameplaytag;
        cuesDuringStartup: list<str> ->cue_key;
        commitPolicy: StartupCommitPolicy;
    }

    // 蓄力模型（按住蓄力，松手触发）
    struct Charge {
        minChargeTime: FloatValue;
        maxChargeTime: FloatValue;
        releaseOnMax: bool;
        chargeProgressVar: str ->var_key; // 将 0~1 的蓄力进度写入 instanceState
        
        chargingTags: list<str> ->gameplaytag;
        cuesDuringCharge: list<str> ->cue_key;
        commitPolicy: ChargeCommitPolicy;
    }

    // 引导模型（持续触发）
    struct Channel {
        duration: FloatValue;
        tickInterval: FloatValue;  // 心跳间隔，执行effect逻辑
        maxTicks: int;       // -1 = 无限，由 duration 截断
        tickOnStart: bool;   // 是否在激活瞬间立即触发首个 tick
        finisherEffect: list<Effect>; // 收尾技
        
        channelingTags: list<str> ->gameplaytag;
        cuesDuringChannel: list<str> ->cue_key;
        commitPolicy: ChannelCommitPolicy;
    }
}

// Commit 决定了【扣除 costs + 启动 cooldown】发生的精确时刻
enum StartupCommitPolicy {
    OnActivate;     // 激活即扣（被打断不退费）
    OnComplete;     // 前摇完成瞬间扣（被打断白嫖）
}
enum ChargeCommitPolicy {
    OnActivate;     // 激活即扣
    OnRelease;      // 松手且达到 minChargeTime 时扣（蓄力不足取消不扣费）
}
enum ChannelCommitPolicy {
    OnActivate;     // 激活即扣
    OnFirstTick;    // 发生首次 tick 时扣（tickOnStart=true 的立即执行算作首次 tick）
}
```

---

## 生命周期与状态机 (Lifecycle)

Ability 在运行时的四大核心阶段（Phase）：`Activating`、`Processing`、`Executing`、`Recovering`。

```text
  UI / 输入层
      │
[ CanActivate ] ── 构建 context，验证 CD / 资源 / 状态 / 目标合法性
      │
      ▼
 [ Activating ] ── 创建 AbilityInstance
      │            * [ 若 CommitPolicy == OnActivate，在此处 Commit ]
      ▼
 [ Processing ] ── (前摇/蓄力/引导) 挂载约束标签，接受打断检测，动态更新瞄准
      │                │                     │
      │          interruptsAbilities     cancelsAbilities
      │           (硬打断)                (软取消)
      │                │                     │
      │                ▼                     ▼
      │         执行 onInterrupt       直接清理 ──▶ End
      │                │
      │                ▼
      │               End
      ▼
      │            * [ 延迟的 Commit 通常在此处或释放瞬间发生 ]
 [ Executing ] ── 瞬间结算 ability.effect
      │
      ▼
 [ Recovering ] ── 挂载 recoveryTags 进入后摇倒计时
      │                │                     │
      │          interruptsAbilities     cancelsAbilities
      │           (硬打断)                (软取消)
      │                │                     │
      │                └──────┬───────────────┘
      │                       ▼
      │              直接清理 ──▶ End
      │              (Recovery 阶段始终无惩罚，两种打断动词效果相同)
      ▼
   [ Ended ] ── 正常销毁
```

### CanActivate

推荐检查顺序（从快到慢，尽早拒绝）：

1. TagRules 的 `blocksAbilities` 是否拦截当前 `abilityTags`
2. `cooldown` 就绪
3. `costs` 资源充足（检查但不扣除）
4. `activationConditions` 条件满足
5. `maxConcurrentAbilitiesPerActor` 未超限
6. `targeting` 验证（目标/地点/方向已由引擎收集且合法）

全部通过才进入 Activating。Targeting 验证放在最后，引擎可在步骤 1-5 通过后再触发目标选择 UI。

### 打断 (Interrupt) 与 取消 (Cancel)

| 方面 | interrupt | cancel |
|---|---|---|
| 触发源 | TagRules 的 `interruptsAbilities` | TagRules 的 `cancelsAbilities`、玩家主动操作、TargetLost |
| 执行 ability.effect | 否 | 否 |
| 执行 onInterrupt | **是**（仅 Processing 阶段） | **否** |
| 已 commit 资源 | 保留 | 保留 |
| 未 commit 资源 | 不扣 | 不扣 |
| 广播事件 | `Ability_Interrupted` | `Ability_Cancelled` |
| Recovery 阶段行为 | 等同 cancel（无惩罚） | 直接清理结束 |

**关键规则**：Recovery 阶段（ability.effect 已成功执行）无论被 `interruptsAbilities` 还是 `cancelsAbilities` 命中，均视为动作取消——不执行 `onInterrupt`，广播 `Ability_Completed`。

---

## Recovery 阶段（后摇）

若 duration <= 0 则直接跳过此阶段。
必须由系统原生托管后摇，以保证 UI 占用状态准确，防范"动作未完但逻辑可重入"的穿透 Bug。

```cfg
struct RecoveryConfig {
    duration: FloatValue;
    recoveryTags: list<str> ->gameplaytag;   // 如 ["State.Recovery"]
    cuesDuringRecovery: list<str> ->cue_key; 
}
```

通过 `recoveryTags` 与 `tag_rules` 的组合，策划可精确控制每个技能后摇的"硬度"：

| 后摇类型 | recoveryTags | 表现 |
|---|---|---|
| 轻型后摇 | `["State.Recovery"]` | 不能攻击/施法，但可以翻滚取消 |
| 重型后摇 | `["State.Recovery.Heavy"]` | 不能攻击/施法/翻滚，必须等后摇结束 |
| 无后摇 | duration=0，跳过 Recovery | 即时释放下一个动作 |

---

## 状态与 TagRules 的交互示例
TagRules 的完整定义见 `ability-design.md`。此处展示其 `interruptsAbilities`（硬打断）、`cancelsAbilities`（软取消）和`blocksAbilities`（施法约束）在施法生命周期中的具体运用示例：

```
tag_rules {
    name: "CoreCombatRules";
    rules: [
        // 硬控打断
        { whenPresent: "State.Debuff.Control.Stun";
          interruptsAbilities: ["Ability.Type"];
          blocksAbilities: ["Ability.Type"];
          description: "眩晕：硬打断并封锁所有技能"; },

        { whenPresent: "State.Debuff.Silence";
          interruptsAbilities: ["Ability.Type.Spell"];
          blocksAbilities: ["Ability.Type.Spell"];
          description: "沉默：硬打断并封锁法术类技能"; },

        // 软取消
        { whenPresent: "State.Dodging";
          cancelsAbilities: ["Ability.Type"];
          description: "翻滚：软取消任何技能（含后摇）"; },

        { whenPresent: "State.Moving";
          cancelsAbilities: ["Ability.Startup.MoveCancel"];
          description: "移动：软取消标记为可移动取消的前摇技能"; },

        // 施法约束
        { whenPresent: "State.Recovery";
          blocksAbilities: ["Ability.Type.Spell", "Ability.Type.Melee"];
          description: "后摇期间禁止攻击和施法"; },

        { whenPresent: "State.Recovery.Heavy";
          blocksAbilities: ["Ability.Type.Movement"];
          description: "重型后摇期间禁止移动类技能"; }
    ];
}
```

### 技能配置示例：移动取消前摇

```
// 可被移动取消的治疗术
ability {
    id: 1001; name: "治疗术";
    abilityTags: ["Ability.Type.Spell", "Ability.Startup.MoveCancel"];
    //         ▲ 标记为可被移动取消

    castMode: Startup {
        startupTime: Const { value: 2.5; };
        startupTags: ["State.Startup.Spell"];
        //         ▲ 不含 State.Immobile → 移动不被 block
        //           但 TagRules: State.Moving cancelsAbilities Ability.Startup.MoveCancel
        //           → 玩家一动，此技能被 cancel（无惩罚，不扣费）
        cuesDuringStartup: ["Startup.Heal"];
        commitPolicy: OnComplete;
    };

    effect: ...;
}

// 站桩带前摇的火球术（移动直接被禁止）
ability {
    id: 1002; name: "火球术";
    abilityTags: ["Ability.Type.Spell"];
    //         ▲ 没有 Ability.Startup.MoveCancel → 移动不会取消此技能

    castMode: Startup {
        startupTime: Const { value: 2.0; };
        startupTags: ["State.Startup.Spell", "State.Immobile"];
        //         ▲ State.Immobile → TagRules blocks Ability.Type.Movement → 按不动
        cuesDuringStartup: ["Startup.Fireball"];
        commitPolicy: OnComplete;
    };

    effect: ...;
    onInterrupt: [ GrantTags {
            grantedTags: ["State.AbilityLockout"];
            duration: Const { value: 0.5; };
        },
        FireCue { cue: "Startup.Interrupted"; }
    ];
    recovery: { duration: Const { value: 0.3; }; recoveryTags: ["State.Recovery"];};
}
```

---

## 生命周期广播事件

引擎在 EventBus 中广播技能关键节点。`Payload` 约定：`instigator` = `target` = 施法者自身。

| 事件名 | 触发时机 | 典型用途 |
|---|---|---|
| `Ability_Activated` | 进入 Activating 阶段 | 触发"准备施法时获得霸体"被动 |
| `Ability_Committed` | 实际扣除资源并启动 CD 的瞬间 | 触发"消耗法力时回血"被动 |
| `Ability_Executed` | `ability.effect` 执行完毕 | 触发"释放法术后强化下一次普攻"被动 |
| `Ability_Interrupted` | Processing 阶段被 `interruptsAbilities` 命中 | 触发"被打断时获得激怒"被动 |
| `Ability_Cancelled` | 玩家主动停止 / `cancelsAbilities` 命中 / TargetLost | UI 提示"蓄力失败" |
| `Ability_Completed` | Executing 阶段完成后最终退出（无论 Recovery 是否被取消）| |

---

## 复合模式：连招设计指引

连招不作为底层原语，通过"多段独立 Ability + Tag 窗口"实现。

```text
[普攻一段] (id:1001)               [普攻二段] (id:1002)
 Instant                           Instant
 CD: 0.0s                          CD: 0.0s
                                   activationConditions: 
                                     HasTag "Combo.Attack.S2"

 recovery:                         recovery:
   duration: 0.6s                    duration: 0.8s
   recoveryTags: ["State.Recovery"]  recoveryTags: ["State.Recovery"]

 effect:                           effect:
   + Damage(50)                      + RemoveStatusByTag ["Combo.Attack"]
   + GrantTags                       + Damage(80)
     "Combo.Attack.S2"               + GrantTags
     duration: 1.0s                    "Combo.Attack.S3"
                                       duration: 1.0s
```

**逻辑解剖**：一段普攻挥出后，产生 0.6s 后摇（不可走位），但同时抛出 1.0s 的 `Combo.Attack.S2` 窗口标签。在这 1.0s 内按下攻击，二段普攻释放，TagRules 自动 cancel 掉一段普攻的 Recovery，实现丝滑派生。

---

## 设计决策记录

### 为什么 interruptsAbilities 和 cancelsAbilities 需要分开

旧版只有一个 `cancelsAbilities`，无法区分"眩晕打断前摇（应有惩罚）"和"翻滚取消后摇（不应有惩罚）"。拆分后：
- 策划可以精确控制"移动取消前摇"这类柔性中断——不触发 `onInterrupt`，不扣费
- Recovery 阶段两种动词效果相同（均为无惩罚清理），保持了语义一致性
- 硬控（眩晕/沉默）用 `interruptsAbilities`，玩家主动行为（翻滚/移动）用 `cancelsAbilities`，职责清晰

### 为什么连招不作为 CastMode 原语

连招的本质是"多个行为按条件链接"。每段的消耗、打断容忍度、后摇通常不同。用独立 Ability + GrantTags 窗口串联，每段保持完整的 Ability 语义，TagRules 自然覆盖。

### 为什么切换型不作为 CastMode 原语

切换型是"Instant Ability + 持续 Status"的直接组合。作为原语不增加表达力。

### Recovery 阶段为什么不区分 interrupt 和 cancel

ability.effect 已成功执行，"被打断惩罚"的语义不适用。无论何种外力终止后摇，玩家的核心诉求都是"尽快恢复行动自由"。统一为无惩罚清理，消除了策划的认知负担。

---

## 运行时结构参考

```java
class AbilityComponent {
    Actor owner;
    List<Ability> grantedAbilities;
    SafeList<AbilityInstance> activeInstances;

    ActivateResult tryActivate(int abilityId);
    void tick(float dt);
}

enum ActivateResult {
    Success; BlockedByTags; OnCooldown; InsufficientCost; 
    ConditionFailed; MaxConcurrent; TargetInvalid;
}

class AbilityInstance implements IPendingKill {
    Ability config;
    Context context;
    Actor owner;

    AbilityPhase phase;
    float phaseElapsed;
    boolean isCommitted;
    boolean pendingKill;

    void tick(float dt);
    
    // 由输入/引擎系统调用
    void release();     // Charge: 玩家松手
    void cancel();      // 玩家主动取消 / TagRules cancelsAbilities 驱动
    void interrupt();   // TagRules interruptsAbilities 驱动
}

enum AbilityPhase {
    Activating;
    Processing;
    Executing;
    Recovering;
    Ended;
}
```