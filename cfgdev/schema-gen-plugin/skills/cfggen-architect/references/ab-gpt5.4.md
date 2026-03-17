下面我基于上一版再做一次“反省式优化”。这次重点不是继续加内容，而是**收敛、去重、补边界**，让它更像一段可以并入架构文档的正式规范。

我先说上一版我自己认为还可以继续优化的点：

---

# 上一版还可以改进的地方

## 1. `allowMovement: bool` 还是偏实现细节
它表达的是“施法过程中的移动约束”，但实际项目里往往不是简单 true/false，而是：

- 可自由移动
- 可缓慢移动
- 仅允许转向
- 禁止位移但允许被推动
- 可移动但会打断
- 可移动但伤害降低

所以直接放一个 bool 有点过早收窄了设计空间。

### 优化方向
把它提升为更抽象的“**activation tags / movement policy hook**”，由项目层移动系统解释；基线文档不把运动学规则写死。

---

## 2. `recovery` 是对的，但“前摇/执行/后摇”还不够统一
上一版把：
- Cast / Charged / Channel 的过程
- Recovery 的后摇

分开定义了，但还没把它们统一成一个更清晰的“能力阶段模型”。

换句话说，当前看起来还是“几种 activation model 各自有自己的时序”，而不是“所有 Ability 都映射到统一阶段骨架”。

### 优化方向
统一成：

- **Startup**：生效前过程
- **Execution**：效果生效点/生效期
- **Recovery**：生效后恢复期

不同模型只是这三段的填充方式不同。

这样策划、程序、编辑器、调试工具都更容易对齐。

---

## 3. `Channel` 的终止语义还不够严谨
上一版写了：

- 正常结束时执行 `ability.effect`
- `tickEffect` 在过程中执行

但没完全说清楚以下几种情况：
- 手动取消 channel 时，`ability.effect` 执不执行？
- 被打断时执行不执行？
- duration 到了但一次 tick 刚好还没结算，如何处理？
- `executeOnStart` 是否算第一次 tick？

### 优化方向
把 Channel 的“结束类型”明确化：
- Completed
- Cancelled
- Interrupted

并定义每类结束是否触发收尾效果。

---

## 4. `CommitPolicy` 还缺“检查成功但保留资源”的标准术语
上一版说了：
- CanActivate 检查资源够不够
- 到 commit 时才真正扣

但这个概念在多人游戏和高并发环境里很关键：
> 检查通过 ≠ 资源已保留

如果资源可能被别的逻辑在中途消耗，commit 时仍可能失败。

### 优化方向
明确：
- `CanActivate` 只是**可行性检查**
- `Commit` 是**原子提交**
- 如果 commit 时失败，则进入 `Cancelled` 而不是 `Interrupted`

这能让生命周期更严谨。

---

## 5. 连招方案是对的，但应该明确：这是“推荐模式”，不是“唯一模式”
上一版把 Combo 降级成多个 Ability + 窗口 Tag，这是正确方向。但也要留一句：

> 若某项目强依赖输入链编辑器，也可以在项目裁剪版中提升为更高层的内容模板，但不建议作为基线原语。

这样更中性，不会让人误解成“框架反对 combo”。

---

## 6. Ability 生命周期事件还可以进一步规范 payload
上一版说了 `Var_AbilityId` 放 extras，但这还不够。实际通常还需要：
- 本次实例 ID
- 结束原因
- chargeProgress
- 是否已 commit

### 优化方向
给 Ability 事件约定一组标准 extras key。

---

---

# 优化后的 Ability 施法过程模型

下面给出更收敛的一版。

---

# Ability 施法过程模型

## 设计目标

Ability 层的职责不是承载所有战斗逻辑，而是提供一个统一的“**施法过程骨架**”，回答以下问题：

- 技能何时开始？
- 何时真正提交资源与冷却？
- 何时产生效果？
- 何时会被打断或取消？
- 效果之后是否存在恢复期/后摇？
- 这些阶段如何通过 Tag 暴露给外界系统？

在此原则下：

- **Ability 管过程**
- **Status 管持续存在**
- **Effect 管原子动作**

三者保持正交。

---

## 一、统一阶段骨架

所有 Ability，不论是瞬发、读条、蓄力还是引导，都统一映射到以下阶段：

1. **CanActivate**
   - 仅做可行性检查
   - 不修改任何资源状态
   - 检查：
     - `requiresAll`
     - 当前冷却是否完成
     - 当前资源是否“看起来足够”
     - TagRules 的 `blocksAbilities`

2. **Activate**
   - 创建 `AbilityInstance`
   - 冻结 `initSnapshot`
   - 进入施法流程
   - 根据配置授予阶段标签与 Cue

3. **Startup**
   - 效果生效前的准备阶段
   - 对应：
     - `Instant`：无 Startup
     - `Cast`：读条
     - `Charged`：蓄力
     - `Channel`：引导启动后进入执行期，不需要独立 Startup（基线版）

4. **Execution**
   - 效果生效阶段
   - 可能是：
     - 单次生效（Instant / Cast / Charged）
     - 周期生效（Channel 的 tickEffect）
     - 收尾生效（Channel endEffect）

5. **Recovery**
   - 生效后的恢复/后摇阶段
   - 通过统一字段表达，而不是每个技能手写 `GrantTags`

6. **End**
   - 生命周期结束
   - 释放阶段标签 / Cue
   - 广播结束事件

同时统一三种异常/终止结果：

- **Completed**：正常完成
- **Cancelled**：主动取消或 commit 失败
- **Interrupted**：被外部规则打断

---

## 二、Ability 配置结构

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    description: text;

    abilityTags: list<str> ->gameplaytag;
    requiresAll: list<Condition>;

    costs: list<StatCost>;
    cooldown: FloatValue;

    activation: ActivationModel;
    commitPolicy: CommitPolicy;
    completionPolicy: CompletionPolicy;
    interruptResponse: InterruptResponse;

    // 统一恢复期
    recovery: PhaseSpec;

    effect: Effect;   // 主效果
}
```

这里新增两个关键点：

- `recovery` 不再是零散字段，而是统一成 `PhaseSpec`
- 增加 `completionPolicy`，明确不同结束原因下是否执行收尾效果

---

## 三、PhaseSpec：统一阶段表现

为了让 Startup / Recovery 的结构一致，引入一个通用结构：

```cfg
struct PhaseSpec {
    duration: FloatValue;                 // 0 = 无此阶段
    grantedTags: list<str> ->gameplaytag; // 阶段期间授予宿主
    cuesWhileActive: list<str> ->cue_key;
}
```

这样：

- Cast 的读条标签
- Charged 的蓄力标签
- Recovery 的后摇标签

都可以统一表达。

---

## 四、ActivationModel：只保留真正必要的原语

```cfg
interface ActivationModel {
    struct Instant {}

    struct Cast {
        startup: PhaseSpec;   // duration 即 castTime
    }

    struct Charged {
        startup: PhaseSpec;   // duration = maxChargeTime
        minChargeTime: FloatValue;
        releaseOnMax: bool;
        chargeProgressVar: str ->var_key;
    }

    struct Channel {
        active: PhaseSpec;    // 引导期间的标签/Cue
        duration: FloatValue; // -1 = 手动结束
        tickInterval: FloatValue;
        tickEffect: Effect;
        maxTicks: int;        // -1 = 不额外限制
        executeOnStart: bool;
    }
}
```

---

## 为什么这样更好

### 1. 去掉了 `allowMovement`
因为这不是战斗能力系统最稳定的抽象。不同项目移动规则差异很大。

更推荐做法是：

- 通过阶段标签暴露状态，例如：
  - `State.Casting`
  - `State.Channeling`
  - `State.Recovery`
- 由项目层移动系统、输入系统、动画系统根据这些标签决定：
  - 能否移动
  - 能否转向
  - 能否跳跃
  - 是否减速

这样基线更干净，也避免过度绑定行为层。

---

## 五、CommitPolicy：提交语义

```cfg
enum CommitPolicy {
    OnActivate;      // 激活后立刻提交
    OnStartupEnd;    // Startup 完成时提交
    OnFirstExecute;  // 第一次进入 Execution 时提交
}
```

### 语义说明

- `CanActivate` 只是预检查，不保留资源
- `Commit` 才是真正原子修改：
  - 扣除 cost
  - 进入 cooldown
  - 执行可能的 commit side-effects（若项目扩展）
- 如果 commit 时失败：
  - 本次 Ability 不进入 `Interrupted`
  - 进入 `Cancelled`

### 推荐搭配

| 模型 | 推荐提交时机 |
|---|---|
| Instant | OnActivate |
| Cast | OnStartupEnd |
| Charged | OnStartupEnd（即释放时） |
| Channel | OnActivate 或 OnFirstExecute |

---

## 六、CompletionPolicy：收尾语义

这是这次新增的重点。用于明确：
- 正常结束时是否执行收尾效果
- 取消时是否执行
- 被打断时是否执行

```cfg
struct CompletionPolicy {
    // 主 effect 适用于单次技能；Channel 下常作为 endEffect 理解
    executeEffectOnCompleted: bool;
    executeEffectOnCancelled: bool;
    executeEffectOnInterrupted: bool;
}
```

### 推荐解释

- 对于 `Instant / Cast / Charged`
  - `effect` 通常是主效果
  - 一般在进入 Execution 时就执行
  - `completionPolicy` 多数无特殊意义，可默认只在 Completed 为 true

- 对于 `Channel`
  - `tickEffect` = 持续过程
  - `effect` = 收尾效果 / 结束爆发 / 最终结算
  - 这时 `completionPolicy` 就非常重要：
    - 正常结束是否爆发？
    - 手动取消是否爆发？
    - 被打断是否爆发？

### 例子
一个“持续吸收能量，完整引导结束后爆炸”的技能：

- Completed：执行爆炸
- Cancelled：不爆炸
- Interrupted：不爆炸

---

## 七、InterruptResponse：只管“打断后怎么办”

继续明确职责边界：

- **能否被打断**：由 TagRules + 当前 AbilityTags / 阶段标签共同决定
- **被打断后如何处理**：由这里定义

```cfg
struct InterruptResponse {
    refundCosts: bool;
    refundCooldown: bool;
    lockoutPhase: PhaseSpec (nullable);   // 打断后的短暂僵直/锁定
    onInterruptEffect: Effect (nullable);
}
```

相比上一版，这里把 `lockoutDuration` 升级为 `lockoutPhase`，原因是：
- 打断后往往不仅有时长，还有标签/Cue
- 例如：
  - `State.Interrupted`
  - 被打断的踉跄特效
  - 禁止重新施法 0.5 秒

统一成 PhaseSpec 更一致。

---

## 八、统一结束原因

建议在运行时统一定义：

```java
enum AbilityEndReason {
    Completed,
    Cancelled,
    Interrupted
}
```

### 触发条件建议

#### Completed
- Instant 正常执行完
- Cast 读条结束并执行完主效果
- Charged 达到释放条件并完成主效果
- Channel 达到 duration / tick 上限 / 正常结束规则

#### Cancelled
- 玩家主动取消蓄力/引导
- Charged 在 `minChargeTime` 前松手
- commit 时资源已被其它逻辑抢占导致提交失败
- 目标丢失且项目规则要求取消

#### Interrupted
- 因 TagRules 的 `cancelsAbilities` 被强制中断
- 因外部系统显式调用 interrupt

这样生命周期清晰很多，也更方便事件监听和日志调试。

---

## 九、Ability 生命周期事件标准化

建议把 Ability 事件正式纳入系统标准事件族，并给出统一 extras 约定。

### 事件建议

- `Ability_Activated`
- `Ability_Committed`
- `Ability_Executed`
- `Ability_Completed`
- `Ability_Cancelled`
- `Ability_Interrupted`

### 建议标准 extras

通过 `var_key` 约定以下键：

- `AbilityId`
- `AbilityInstanceId`
- `EndReason`（若事件是结束类）
- `ChargeProgress`（若存在）
- `WasCommitted`（0/1）
- `CurrentPhase`（可选，用于调试/表现）

### 事件语义
- `Ability_Activated`：实例创建成功
- `Ability_Committed`：资源与 CD 原子提交成功
- `Ability_Executed`：主 effect 或一次 tickEffect 被执行
- `Ability_Completed / Cancelled / Interrupted`：互斥，只会发一个最终事件

---

## 十、运行时建议

```java
class AbilityInstance implements IPendingKill {
    Ability config;
    Context context;
    Actor owner;

    AbilityRuntimePhase phase;
    AbilityEndReason endReason; // nullable until end

    float phaseElapsed;
    float totalElapsed;

    boolean committed;
    int executeCount;           // Channel 统计 tick 次数
    boolean pendingKill;

    void tick(float dt);
    void release();   // Charged 专用
    void cancel();    // 主动取消
    void interrupt(); // 外部打断
}

enum AbilityRuntimePhase {
    Startup,
    Execution,
    Recovery,
    Lockout,
    Ended
}
```

这里相比上一版的优化是：

- 不再把 `Casting / Charging / Channeling` 写成运行时 phase
- 运行时 phase 只保留抽象骨架：
  - Startup
  - Execution
  - Recovery
  - Lockout
  - Ended

具体是 Cast 还是 Charged，由 `activation` 类型决定。

这会让：
- 调试面板更统一
- AbilityInstance 代码更好维护
- 将来扩展新 activation model 更容易

---

## 十一、各模型的统一映射

### Instant
- Startup：无
- Execution：一次性执行主 effect
- Recovery：按配置
- End：Completed

### Cast
- Startup：持续 `startup.duration`
- Startup 结束时 commit
- Execution：执行主 effect
- Recovery：按配置
- End：Completed / Interrupted / Cancelled

### Charged
- Startup：持续到 release 或 max time
- 持续更新 `chargeProgressVar`
- 若 release 时 `< minChargeTime` → Cancelled
- 若满足释放条件 → commit → Execution
- Recovery：按配置

### Channel
- Startup：无
- Execution：在 active 阶段内多次执行 tickEffect
- commit：按 `OnActivate` 或 `OnFirstExecute`
- Channel 结束时根据 `completionPolicy` 决定是否执行 `effect`
- Recovery：按配置

---

## 十二、与 Status / Timeline 的边界再明确一次

这是最容易混淆的地方，所以建议在文档里明确写出来。

### 应放在 Ability 层的
- 读条
- 蓄力
- 引导
- 资源提交时机
- 打断响应
- 后摇/恢复期

### 应放在 Status 层的
- 持续 Buff / Debuff
- 周期性伤害/治疗
- 光环
- 被动监听
- 挂载后的长期标签
- 与宿主共同生存的状态逻辑

### 什么时候两者一起用
典型场景：
- 技能先读条（Ability）
- 读条完成后挂一个燃烧 DOT（Status）
- DOT 周期掉血（Periodic）
- 受击时再触发特殊效果（Trigger）

这时 Ability 和 Status 的职责非常清晰，不互相替代。

---

## 十三、连招与切换：作为“推荐模板”，不作为基线原语

### 连招
基线推荐：
- 多个独立 Ability
- 通过 `GrantTags` 产生输入窗口
- 通过 `requiresAll` 判断窗口是否存在

但也补一句：

> 若某项目强依赖复杂输入链编辑器，可以在项目裁剪版中把“ComboChain”提升为内容模板层，而不建议放进 Ability 基线原语层。

### 切换
基线推荐：
- 开启技能：ApplyStatus
- 关闭技能：RemoveStatusByTag
- 是否显示为同一按键，由输入系统决定

这样基线保持小而稳。

---

## 十四、简化后的示例

这次只保留两个最能说明问题的例子。

---

### 例 1：火球术（Cast）

```cfg
ability {
    id: 1001;
    name: "火球术";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire"];

    costs: [
        { stat: "MP_Current"; value: struct Const { value: 30.0; }; }
    ];
    cooldown: struct Const { value: 8.0; };

    activation: struct Cast {
        startup: {
            duration: struct Const { value: 2.0; };
            grantedTags: ["State.Casting", "State.Casting.Spell"];
            cuesWhileActive: ["Cast.Fireball"];
        };
    };

    commitPolicy: OnStartupEnd;

    completionPolicy: {
        executeEffectOnCompleted: true;
        executeEffectOnCancelled: false;
        executeEffectOnInterrupted: false;
    };

    interruptResponse: {
        refundCosts: true;
        refundCooldown: true;
        lockoutPhase: {
            duration: struct Const { value: 0.5; };
            grantedTags: ["State.Interrupted"];
            cuesWhileActive: [];
        };
        onInterruptEffect: struct FireCue { cue: "Cast.Interrupted"; };
    };

    recovery: {
        duration: struct Const { value: 0.3; };
        grantedTags: ["State.Recovery"];
        cuesWhileActive: [];
    };

    effect: struct SpawnObj {
        duration: struct Const { value: 5.0; };
        objTags: ["Obj.Projectile.Fireball"];
        moveInfo: { ... };
        cuesWhileActive: ["Projectile.Fireball"];
        dieInfo: [{ ... }];
    };
}
```

---

### 例 2：弹幕射击（Channel）

```cfg
ability {
    id: 2002;
    name: "弹幕射击";
    abilityTags: ["Ability.Type.Ranged", "Ability.Type.Channel"];

    costs: [
        { stat: "MP_Current"; value: struct Const { value: 40.0; }; }
    ];
    cooldown: struct Const { value: 12.0; };

    activation: struct Channel {
        active: {
            duration: struct Const { value: 4.0; };
            grantedTags: ["State.Channeling"];
            cuesWhileActive: ["Channel.Barrage"];
        };
        duration: struct Const { value: 4.0; };
        tickInterval: struct Const { value: 0.3; };
        tickEffect: struct SpawnObj {
            duration: struct Const { value: 3.0; };
            objTags: ["Obj.Projectile.Arrow"];
            moveInfo: { ... };
            cuesWhileActive: ["Projectile.Arrow.Flight"];
            dieInfo: [{ ... }];
        };
        maxTicks: -1;
        executeOnStart: true;
    };

    commitPolicy: OnActivate;

    completionPolicy: {
        executeEffectOnCompleted: true;    // 正常引导完播收尾
        executeEffectOnCancelled: false;   // 主动取消不播收尾
        executeEffectOnInterrupted: false; // 被打断不播收尾
    };

    interruptResponse: {
        refundCosts: false;
        refundCooldown: false;
        lockoutPhase: null;
        onInterruptEffect: null;
    };

    recovery: {
        duration: struct Const { value: 0.0; };
        grantedTags: [];
        cuesWhileActive: [];
    };

    effect: struct FireCue { cue: "Channel.Barrage.End"; };
}
```

---

## 十五、最终建议总结

如果要把“施法过程模型”真正并入你的大文档，我建议最终落地成以下几个结论：

### 1. Ability 层增强是必要的
因为“施法过程”本身就是 Ability 的天然职责，全部丢给 Status/Timeline 会造成语义割裂。

### 2. 但 Ability 层不应做成重型状态机
基线只保留四种 activation 原语：
- Instant
- Cast
- Charged
- Channel

### 3. 统一阶段骨架比堆模型更重要
所有技能都映射到：
- Startup
- Execution
- Recovery
- End

### 4. 打断判定与打断响应必须分离
- 判定：TagRules
- 响应：InterruptResponse

### 5. 后摇应提升为一等公民
这在动作游戏里非常常见，不应让策划每次手拼。

### 6. 连招/切换不应进入基线原语层
推荐作为模板模式，用多个 Ability + Tags 组合实现。

---

如果你愿意，我下一步可以直接帮你做两种后续工作之一：

1. **把这一节整理成与你原文风格完全一致的“正式文档段落”**  
   即直接写成可插入原文的 `### Ability` 修订版。

2. **继续向前走一步，设计 AbilityRuntime 的伪代码执行流程**  
   包括：
   - `tryActivate`
   - `tick`
   - `commit`
   - `interrupt`
   - `cancel`
   - `complete`
   的标准伪代码。