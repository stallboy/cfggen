
让我认真反省当前版本的每一个设计决策。

**1. InterruptResponse 的 lockoutTag 问题**

当前设计让每个技能自己配 lockoutTag，但实际上：
- 99% 的技能用的是同一个 lockoutTag（如 "State.AbilityLockout"）
- 让每个技能都配一遍是冗余的
- 但确实存在少数情况需要不同的 lockout tag（如法术被打断和近战被打断可能有不同的后续约束）

反思结论：lockoutTag 应该有全局默认值，InterruptResponse 只需配 duration 和可选的 onInterruptEffect。如果需要自定义 lockoutTag，可以通过 onInterruptEffect 中的 GrantTags 来实现。

实际上，仔细想想，InterruptResponse 本身可能就过度设计了。lockoutDuration + lockoutTag 本质就是一个 GrantTags。onInterruptEffect 已经可以做到一切。那 InterruptResponse 是否应该简化为只有一个 onInterruptEffect？

但是如果这样做，lockoutDuration 这个极高频需求就得每个技能都手写 GrantTags，反而更啰嗦。

平衡方案：InterruptResponse 保留 lockoutDuration（语法糖），lockoutTag 走全局默认，onInterruptEffect 处理自定义逻辑。

**2. RecoveryConfig 和 InterruptResponse 的对称性问题**

recovery 有 duration + tags + cues
interruptResponse 有 lockoutDuration + lockoutTag + onInterruptEffect

两者结构不对称，但功能逻辑是相似的——都是"某个事件后进入一段受限时间"。这种不对称会让策划困惑。

反思结论：统一风格。recovery 和 interrupt lockout 都是"事件后的受限阶段"，但语义不同（recovery 是正常结束后的硬直，lockout 是被打断后的惩罚）。保持分离是正确的，但结构应更对称。

**3. 再审视 processTags 的命名**

我在上一版统一了所有模型的 tags 字段为 processTags，理由是"语义相同"。但这有一个实际问题：

当策划看到 Charged 模型里的 processTags，他能直觉理解这是"蓄力期间的标签"吗？相比 chargingTags，processTags 更抽象。

但如果每个模型用不同名字（castingTags/chargingTags/channelingTags），配置表结构就不统一了。

实际上这个问题在 interface 的多态设计中天然被解决了——因为 Cast 结构体里的字段名可以用 castingTags，Charged 里用 chargingTags，它们本来就在不同的 struct 里，不需要统一名字。

反思结论：恢复各模型独有的语义化命名。`Cast.castingTags`, `Charged.chargingTags`, `Channel.channelingTags`。

**4. comboGroup 的必要性再审视**

我加了 comboGroup 作为"仅用于编辑器/UI 的元数据"。但这破坏了一个原则：配置表中的字段应该有运行时语义，纯工具层的分组关系应该在工具层管理。

而且如果连招是通过 requiresAll + HasTags 串联的，编辑器完全可以通过静态分析配置依赖关系来自动推导连招组，不需要手工维护 comboGroup。

反思结论：移除 comboGroup。如果编辑器需要分组，应通过分析 requiresAll 中的 HasTags 依赖链来自动推导，或在编辑器的元数据层管理，不污染运行时配置表。

**5. Channel 的 ability.effect 执行时机问题**

当前设计：Channel 引导正常结束时执行 ability.effect（如终结爆炸）。但有一个边界问题：

如果玩家主动取消引导（cancel），ability.effect 要不要执行？当前描述是"项目决定"，这不够明确。

实际上，cancel 和 interrupt 有本质区别：
- interrupt：外部强制中断，通常不执行
- cancel：玩家主动选择停止，在某些游戏中应该执行（如：引导够久了主动收手，仍然触发终结效果），在另一些中不应该执行

反思结论：Channel 需要一个 executeOnCancel: bool 字段来明确这个行为。

**6. recovery 在 Channel 中的语义**

Channel 有一个特殊情况：引导本身就是一段时间，引导结束后再进入 recovery。但很多游戏里引导技能结束后没有后摇，直接可以行动。

当前设计允许 recovery.duration = 0 来表达这个，没问题。但需要明确：recovery 的计时从什么时候开始？

- Instant/Cast/Charged：从 ability.effect 执行完毕后开始
- Channel：从引导结束（且 ability.effect 执行完毕）后开始

这个在当前文档的"完整执行阶段"部分已经隐含了，但没有明确写出 recovery 计时起点的定义。

反思结论：在执行阶段描述中补充"Recovery 阶段始终从 Execute 阶段结束后开始"。

**7. 深层反思：Ability 的并发与互斥**

当前只有 AbilityComponent.maxConcurrent 来控制并发，但实际游戏中的互斥关系更复杂：

- 同类技能互斥（两个法术不能同时读条）
- 不同类技能可并发（移动技能 + 自动攻击可以同时进行）
- 某些技能可以打断同类（新法术取消旧法术的读条）

maxConcurrent 是一个全局开关，不够精确。

但如果要做完整的并发互斥矩阵，复杂度会大幅上升。这是否超出了"最小必要"的范围？

反思结论：不在 Ability 层做复杂的互斥矩阵。已有的 TagRules（blocksAbilities/cancelsAbilities）完全可以处理这些场景。关键是把 abilityTags 标好。maxConcurrent 保留为简单的全局上限即可。

但应该在文档中明确这个思路：**技能的互斥关系通过 TagRules 管理，不通过 Ability 的专用字段管理。**

**8. 关于 costs 的动态性**

当前 costs 是 list<StatCost>，每个 StatCost 的 value 是 FloatValue（可以动态求值）。这很好。

但有一个边界问题：CanActivate 阶段检查资源是否足够时，会 evaluate FloatValue。如果这个 FloatValue 依赖 Context 中的动态状态（如当前蓄力层数），那么检查时的值和实际 commit 时的值可能不同。

反思结论：这在当前设计中不是问题，因为 costs.value 通常是静态值或简单属性引用。但应在文档中说明：costs 的检查和扣除使用同一个 Context snapshot，避免 TOCTOU 问题。

**9. CanActivate 的检查顺序**

当前列了5个检查项，但没有明确顺序。顺序在某些情况下影响用户体验（应该先提示"冷却中"还是先提示"蓝不够"？）。

反思结论：明确检查顺序，并说明项目可以调整顺序以匹配 UI 反馈需求。

**10. 最关键的反思：这个设计是否增加了过多耦合？**

回到 A 文档的哲学："三层正交分离"。我增加的 ActivationModel 让 Ability 从"入口"变成了"入口 + 过程管理器"。这是否破坏了正交性？

仔细想：
- Effect 仍然无状态、原子化 ✓
- Status 仍然管持续状态 ✓
- Ability 现在管"激活条件 + 施法过程 + 效果入口" → 这三件事都是"行为入口"的合理延伸

施法过程与 Effect 正交：过程控制的是"何时"执行，Effect 控制的是"执行什么"。
施法过程与 Status 正交：过程跟随 Ability 生命周期，Status 跟随自身生命周期。

所以正交性没有被破坏，只是 Ability 的"入口"定义被合理扩展了。

**11. 版本间差异的清理**

B版和C版之间有一些残留的不一致：
- B版有AbilityPhase枚举，C版用了int phase。应该明确。
- B版的示例更完整但过长，C版更精练。应保持精练。
- 连招示例可以更简洁，当前三个完整ability配置太长，应该用伪配置+注释。

**12. 还有一个重要遗漏：Ability 与动画/蒙太奇的关系**

施法过程在实际游戏中几乎总是与动画同步的。但当前设计完全没有涉及动画。

这是否应该在 Ability 层处理？

不。动画是表现层，应该通过 Cue 系统处理。processTags/cuesDuringXxx 已经提供了足够的信息让表现层驱动动画。

但应在文档中明确这个边界：**Ability 层不直接控制动画。动画系统应监听 Ability 的阶段标签和 Cue 事件来驱动状态机。**

**13. 关于 CanActivate 失败的反馈**

当前设计只说了检查通过/不通过，但没有说失败时如何反馈给调用者。这是输入系统需要的信息（应该显示"蓝不够"还是"冷却中"？）。

反思结论：这属于项目实现层的 UI 逻辑，不属于配置基准。但可以提一句：tryActivate 应该返回失败原因枚举，而不仅是 bool。

现在综合所有反思，形成最终版：

**主要改动清单：**
1. 恢复各模型的语义化 tags 命名
2. 简化 InterruptResponse（移除 lockoutTag，只保留 lockoutDuration + onInterruptEffect）
3. 移除 comboGroup
4. Channel 增加 executeOnCancel
5. 明确 recovery 计时起点
6. 明确 CanActivate 检查顺序
7. 明确 Ability 与动画的边界
8. 明确技能互斥通过 TagRules 管理
9. 精简示例
10. 补充"设计决策记录"章节，解释关键取舍的理由

# Ability 施法过程模型（终稿）

## 当前问题

Ability 的结构本质上是：

```
准入检查 → 扣资源 → 执行 Effect → 结束
```

这是一个瞬发模型。但真实游戏的施法过程远比这复杂——读条、蓄力、引导、后摇，每种都涉及打断、资源提交时机、行为约束等通用需求。

当前可以用 Status + Timeline 组合模拟这些过程，但会导致语义割裂（策划认知中是"技能过程"，配置上变成"瞬发 Ability 挂 Status"）、框架能力缺失（打断/commit 每个技能自己拼）、工具无力（编辑器无法呈现技能时间轴）。

---

## 设计原则

1. **瞬发零开销**：`Instant` 与现有行为完全一致，不被复杂模型拖累
2. **Ability 管过程，Status 管状态**：施法结束后的持续效果仍归 Status
3. **Tag 是唯一约束语言**：施法期间的行为约束（不可移动、不可施法）全部通过标签 + TagRules 表达，不引入布尔开关
4. **最少原语**：只保留不可互相还原的四种施法模型，复合模式（连招、切换）通过已有机制组合
5. **commit 与模型绑定**：消除无效组合
6. **表现层隔离**：Ability 不直接控制动画，动画系统应监听阶段标签和 Cue 事件来驱动状态机

---

## Ability 表修订

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    description: text;

    abilityTags: list<str> ->gameplaytag;
    requiresAll: list<Condition>;
    costs: list<StatCost>;
    cooldown: FloatValue;

    // ========== 新增 ==========
    activation: ActivationModel;
    interruptResponse: InterruptResponse;
    recovery: RecoveryConfig;
    // ==========================

    effect: Effect;
}
```

**新增字段说明**：

| 字段 | 职责 |
|---|---|
| `activation` | 施法的时间模型（Instant/Cast/Charged/Channel） |
| `interruptResponse` | 被打断后的惩罚与反馈 |
| `recovery` | 后摇阶段配置 |

**未新增的字段及理由**：

| 被排除的概念 | 理由 |
|---|---|
| `allowMovement: bool` | 通过标签表达（如在 tags 中加入 `State.Immobile`），移动系统检查该标签 |
| `comboGroup: str` | 编辑器可通过分析 `requiresAll` 的 `HasTags` 依赖链自动推导连招组 |
| 独立的 `CommitPolicy` 枚举 | 内嵌到各模型中，消除无效组合（如 Instant + OnComplete） |

---

## ActivationModel

```cfg
interface ActivationModel {
    struct Instant {}

    struct Cast {
        castTime: FloatValue;
        castingTags: list<str> ->gameplaytag;
        cuesDuringCast: list<str> ->cue_key;
        // true = 读条完成时提交资源（被打断不扣费）
        // false = 激活即提交
        commitOnComplete: bool;
    }

    struct Charged {
        minChargeTime: FloatValue;
        maxChargeTime: FloatValue;
        releaseOnMax: bool;
        // 蓄力进度 (0~1) 自动写入 context.instanceState
        chargeProgressVar: str ->var_key;
        chargingTags: list<str> ->gameplaytag;
        cuesDuringCharge: list<str> ->cue_key;
        // true = 释放时提交资源（蓄力不足/取消不扣费）
        // false = 激活即提交
        commitOnRelease: bool;
    }

    struct Channel {
        duration: FloatValue;           // -1 = 手动结束
        tickInterval: FloatValue;
        tickEffect: Effect;             // 每次 tick 执行（如射出一支箭）
        maxTicks: int;                  // -1 = 由 duration/interval 决定
        executeOnStart: bool;           // 立即执行首次 tickEffect
        executeOnCancel: bool;          // 主动取消时是否执行 ability.effect
        channelingTags: list<str> ->gameplaytag;
        cuesDuringChannel: list<str> ->cue_key;
        // true = 首次 tick 时提交资源
        // false = 激活即提交（默认）
        commitOnFirstTick: bool;
    }
}
```

### 为什么只有四种

每种原语对应一种不可替代的"玩家操作 → 效果生效"的时间模型：

| 模型 | 玩家操作 | 效果生效时机 |
|---|---|---|
| Instant | 按下 | 立即 |
| Cast | 按下，等待 | 读条完毕 |
| Charged | 按住，松手 | 松手时（进度影响效果） |
| Channel | 按下，持续 | 过程中周期性触发 |

### commit 内嵌到模型的原因

各模型的合法 commit 选项不同：

| 模型 | 合法选项 | 无效组合 |
|---|---|---|
| Instant | OnActivate（唯一） | OnComplete 无意义 |
| Cast | OnActivate / OnComplete | — |
| Charged | OnActivate / OnRelease | — |
| Channel | OnActivate / OnFirstTick | OnComplete 语义模糊 |

每个模型用语义明确的布尔字段（`commitOnComplete` / `commitOnRelease` / `commitOnFirstTick`）替代跨模型的枚举，策划不可能配出逻辑矛盾的组合。Instant 没有此字段，始终 OnActivate。

### 行为约束通过标签表达

移动、施法、攻击等约束不在 Ability 层用布尔开关定义，而是全部通过 processTags + TagRules 表达：

```cfg
// 需要站桩施法：
castingTags: ["State.Casting", "State.Casting.Spell", "State.Immobile"]

// 可移动引导：
channelingTags: ["State.Channeling"]
// 不含 State.Immobile → 移动系统不限制

// TagRules 中的移动约束规则（全局一处配置）
tag_rules {
    rules: [{
        whenPresent: "State.Immobile";
        blocksAbilities: ["Ability.Type.Movement"];
        description: "不可移动状态下禁止移动类技能";
    }];
}
```

好处：新增约束类型（不可旋转、不可跳跃）只需加新 Tag + TagRule，不改 Ability 表结构。

---

## InterruptResponse

**职责边界**：

- **"什么能打断我"** → TagRules（`cancelsAbilities`），集中管理
- **"被打断后怎么办"** → InterruptResponse，各技能独立配置

```cfg
struct InterruptResponse {
    // 打断后的锁定时间（防止立刻重新施法）
    // 内部实现为 GrantTags，使用 combat_settings 中的默认 lockoutTag
    lockoutDuration: FloatValue;

    // 打断时执行的额外效果（如：失败特效、硬直、自定义惩罚）
    onInterruptEffect: Effect (nullable);
}
```

**资源退还不需要额外字段**：

commit 时机的选择已经隐含了打断时的资源语义：

| commit 策略 | 被打断时 |
|---|---|
| OnActivate（commit 已发生） | 资源已消耗，不退还 |
| OnComplete / OnRelease / OnFirstTick（commit 未发生） | 资源尚未消耗，无需退还 |

两种情况都不需要 `refundCosts` 字段。策划选择 commit 策略时，就已经决定了打断的经济后果。

**lockoutTag 采用全局默认**：

99% 的技能使用相同的 lockout 标签。在 `combat_settings` 中配置默认值：

```cfg
table combat_settings[name] {
    ...
    defaultLockoutTag: str ->gameplaytag;  // 如 "State.AbilityLockout"
}
```

少数需要自定义 lockout 行为的技能，可在 `onInterruptEffect` 中用 `GrantTags` 实现差异化。

---

## RecoveryConfig

```cfg
struct RecoveryConfig {
    duration: FloatValue;                    // 0 = 无后摇
    recoveryTags: list<str> ->gameplaytag;   // 如 ["State.Recovery"]
    cuesDuringRecovery: list<str> ->cue_key;
}
```

**定位**：`RecoveryConfig` 是"最常见后摇场景"的语法糖。等价于在 effect 末尾追加一个 `GrantTags`，但将其提升为 Ability 一等概念有三点不可替代的价值：

1. **框架可识别**：编辑器可在技能时间轴上渲染后摇区间
2. **语义标准化**：策划不需要每个技能手写 GrantTags，后摇配置一目了然
3. **动作取消有统一锚点**：TagRules 中对 `State.Recovery` 的规则一处配置，覆盖所有技能

超出语法糖范围的复杂后摇（如前半段不可取消、后半段可取消）应回到 effect 中用组合手段实现。

**计时起点**：Recovery 始终从 Execute 阶段完成后开始。

---

## 完整执行阶段

### 阶段流程

```
CanActivate ──→ Activate ──→ Process ──→ Execute ──→ Recovery ──→ End
                                │                                    
                           Interrupt ──→ InterruptResponse ──→ End  
```

### CanActivate

检查顺序（从快到慢，尽早拒绝）：

1. TagRules 的 `blocksAbilities` 是否拦截当前 `abilityTags`
2. `cooldown` 就绪
3. `costs` 资源充足（检查但不扣除）
4. `requiresAll` 条件满足
5. `maxConcurrent` 未超限

全部通过才进入 Activate。`tryActivate` 返回失败原因（而非简单 bool），供 UI 层反馈。

**TOCTOU 保证**：costs 的检查和实际 commit 使用同一份 Context snapshot，避免检查通过后资源被其他逻辑扣减导致的不一致。

### Activate

- 创建 `AbilityInstance`
- 冻结 `initSnapshot`
- 若 commit 策略为 OnActivate（Instant / 或其他模型的默认选项） → 立即扣资源、启动 CD
- 广播 `Ability_Activated` 事件
- 进入 Process（Instant 跳过此阶段）

### Process

各模型行为不同（见下一节）。共同行为：

- 授予 processTags → 写入宿主 TagContainer
- 播放过程 Cue（Sustained 类型）
- 每帧接受 TagRules 打断检测

### Execute

- 若 commit 尚未发生（commitOnComplete / commitOnRelease / commitOnFirstTick 场景） → **先 commit**
- 移除 processTags、移除过程 Cue
- 执行 `ability.effect`
- 广播 `Ability_Executed` 事件

### Recovery

- 授予 `recoveryTags`
- 播放后摇 Cue
- 倒计时结束 → 移除标签和 Cue → 进入 End
- Recovery 期间仍可被 TagRules 打断（此时 effect 已执行完毕，打断仅中止后摇）

### End

- 清理所有阶段标签和 Cue
- 广播 `Ability_Completed` 事件
- 销毁 AbilityInstance

### Interrupt（任意可打断阶段触发）

- 若 commit 尚未发生 → 不消耗资源，不启动 CD
- 若 commit 已发生 → 保留消耗
- 清理所有阶段标签和 Cue
- 挂载 lockoutTag，持续 `lockoutDuration`
- 执行 `onInterruptEffect`
- 广播 `Ability_Interrupted` 事件
- 销毁 AbilityInstance

---

## 各模型状态转移

### Instant

```
Activate
    → commit
    → Execute: ability.effect
    → Recovery（如配置）
    → End
```

单帧完成 commit + execute。无 Process 阶段，无可打断窗口。

### Cast

```
Activate
    → [commitOnComplete=false ? commit : skip]
    → Process: 挂 castingTags, 每帧累加 phaseElapsed
        │
        ├── phaseElapsed >= castTime
        │       → Execute（含延迟 commit）
        │       → Recovery → End
        │
        └── interrupt()
                → InterruptResponse → End
```

### Charged

```
Activate
    → [commitOnRelease=false ? commit : skip]
    → Process: 挂 chargingTags
    │   每帧: chargeProgress = clamp(elapsed / maxChargeTime, 0, 1)
    │         写入 context.instanceState[chargeProgressVar]
        │
        ├── release() 且 elapsed >= minChargeTime
        │       → Execute（chargeProgress 可在 Effect 中通过 ContextVar 读取）
        │       → Recovery → End
        │
        ├── release() 且 elapsed < minChargeTime
        │       → Cancel: 广播 Ability_Cancelled → End（不 commit，不执行 effect）
        │
        ├── elapsed >= maxChargeTime 且 releaseOnMax=true
        │       → 等同 release()，chargeProgress = 1.0
        │
        └── interrupt()
                → InterruptResponse → End
```

### Channel

```
Activate
    → [commitOnFirstTick=false ? commit : skip]
    → Process: 挂 channelingTags
    │   [executeOnStart=true ? 执行首次 tickEffect : skip]
    │   每 tickInterval:
    │       [commitOnFirstTick=true 且首次 ? commit : skip]
    │       执行 tickEffect
    │       tickCount++
        │
        ├── tickCount >= maxTicks 或 phaseElapsed >= duration
        │       → Execute: ability.effect（引导收尾效果，可为空）
        │       → Recovery → End
        │
        ├── cancel()（玩家主动停止）
        │       → [executeOnCancel=true ? Execute : skip]
        │       → 广播 Ability_Cancelled
        │       → Recovery → End
        │
        └── interrupt()
                → InterruptResponse → End
```

`tickEffect` 与 `ability.effect` 的关系：

| 字段 | 执行时机 | 典型用途 |
|---|---|---|
| `tickEffect` | 引导期间每次心跳 | 射出一支箭、每秒治疗一次 |
| `ability.effect` | 引导正常结束时 | 收尾爆炸、结算总伤害，可为空 |

---

## 打断判定

打断判定完全复用 TagRules，不引入 Ability 专用机制：

```
1. 施法者进入 Cast 阶段
   → castingTags 写入 TagContainer: ["State.Casting", "State.Casting.Spell"]

2. 施法者被眩晕
   → 获得 "State.Debuff.Control.Stun"

3. TagRules 自动检测：
   whenPresent: "State.Debuff.Control.Stun"
   cancelsAbilities: ["Ability.Type"]   → 命中 "Ability.Type.Spell"

4. AbilityInstance.interrupt() 被调用

5. InterruptResponse 执行
```

**技能互斥**也走同一条路径，不需要专用字段：

```cfg
// 读条中的法术会被新法术打断
tag_rules {
    rules: [{
        whenPresent: "State.Casting.Spell";
        cancelsAbilities: ["Ability.Type.Spell"];
        description: "新法术打断旧法术的读条";
    }];
}
```

**动作取消后摇**：

```cfg
tag_rules {
    rules: [{
        whenPresent: "State.Dodging";
        purgesTags: ["State.Recovery"];
        description: "翻滚驱散后摇";
    }];
}
```

---

## 复合模式实现指引

### 连招

每段为独立 Ability，通过窗口标签串联：

```
[疾风·壹] (id:2010)          [疾风·贰] (id:2011)          [疾风·终] (id:2012)
 Instant                      Instant                      Instant
 cost: 10 耐                  cost: 10 耐                  cost: 15 耐
 CD: 5s                       CD: 0 (无独立CD)              CD: 0
                               requiresAll:                 requiresAll:
                                 HasTag "ComboWindow.S2"      HasTag "ComboWindow.S3"
 effect:                      effect:                      effect:
   Damage(30)                   Damage(40)                   Damage(80)
   + GrantTags                  + GrantTags                  + GrantTags
     "ComboWindow.S2"            "ComboWindow.S3"             "State.Buff.Hyperarmor"
     dur: 0.6s                   dur: 0.7s                    dur: 0.3s
 recovery: 0.3s               recovery: 0.4s               recovery: 0.6s
```

每段独立配 cost / cooldown / interruptResponse / recovery。TagRules 独立治理每段。编辑器通过分析 `requiresAll` 中的 `HasTags` 依赖链自动推导连招组关系，无需额外元数据。

**CD 策略变体**：

| 策略 | 实现方式 |
|---|---|
| 起手段 CD 控制节奏 | 第一段 CD=5s，后续段 CD=0 |
| 完成全套后进入 CD | 终结段 effect 中 ApplyStatusInline 挂 Cooldown 标签，起手段 requiresAll 检查该标签不存在 |
| 每段独立 CD | 每段各自配 CD |

### 切换型

两个互斥 Ability + 一个持续 Status：

```cfg
// 举盾（requiresAll: 没有盾姿态标签 → 可激活）
ability { id:3001; activation: Instant;
          effect: ApplyStatus { statusId: 5010; }; }
// Status 5010: grantedTags=["State.Buff.ShieldStance"], 含 Periodic 扣耐、StatModifier 加防

// 收盾（requiresAll: 有盾姿态标签 → 可激活）
ability { id:3002; activation: Instant;
          effect: RemoveStatusByTag { query:{requireAll:["State.Buff.ShieldStance"]}; }; }
```

同一按键的路由由输入系统根据当前 Tag 决定——引擎层职责，不是配置层职责。

---

## 生命周期事件

施法过程的关键时刻通过标准 EventBus 广播：

```cfg
// event_definition 中注册
// Payload 约定：instigator = target = 施法者
// extras 中携带 "Var_AbilityId" (int)

"Ability_Activated"       // 进入 Activate
"Ability_Committed"       // 资源已扣除，CD 已开始
"Ability_Executed"        // ability.effect 已执行
"Ability_Completed"       // 正常结束（含后摇结束）
"Ability_Interrupted"     // 被打断
"Ability_Cancelled"       // 主动取消
```

应用：

```cfg
// 被打断后获得短暂韧性
status {
    id: 7002; name: "不屈意志";
    behaviors: [
        struct Trigger {
            listenEvent: "Ability_Interrupted";
            effect: struct GrantTags {
                grantedTags: ["State.Buff.Tenacity"];
                duration: struct Const { value: 2.0; };
            };
            maxTriggers: -1; cooldown: 10.0;
        }
    ];
}

// 成功施放法术后回蓝
status {
    id: 7003; name: "奥术回响";
    behaviors: [
        struct Trigger {
            listenEvent: "Ability_Completed";
            // 可通过 PayloadVar 读取 Var_AbilityId 做进一步筛选
            effect: struct ModifyStat {
                stat: "MP_Current"; op: Add;
                value: struct Const { value: 15.0; };
            };
            maxTriggers: -1; cooldown: 0;
        }
    ];
}
```

---

## 运行时

```java
class AbilityComponent {
    Actor owner;
    List<Ability> grantedAbilities;
    SafeList<AbilityInstance> activeInstances;
    int maxConcurrent;   // 1 = 标准互斥, -1 = 无限

    ActivateResult tryActivate(int abilityId);
    void tick(float dt);
}

enum ActivateResult {
    Success,
    Blocked,           // TagRules 阻止
    OnCooldown,
    InsufficientCost,
    ConditionFailed,
    MaxConcurrent
}

class AbilityInstance implements IPendingKill {
    Ability config;
    Context context;
    Actor owner;

    AbilityPhase phase;
    float phaseElapsed;
    boolean committed;
    boolean pendingKill;

    void tick(float dt);    // AbilityComponent 驱动
    void release();         // Charged 专用
    void cancel();          // 主动取消
    void interrupt();       // TagRules 驱动
}

enum AbilityPhase {
    Activating,    // 初始化帧
    Processing,    // 施法过程中（Cast/Charged/Channel）
    Executing,     // 执行 effect
    Recovering,    // 后摇
    Ended          // 终态
}
```

---

## 示例

### 火球术（Cast + 延迟 commit）

```cfg
ability {
    id: 1001; name: "火球术";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire"];
    costs: [{ stat: "MP_Current"; value: struct Const { value: 30.0; }; }];
    cooldown: struct Const { value: 8.0; };

    activation: struct Cast {
        castTime: struct Const { value: 2.0; };
        castingTags: ["State.Casting", "State.Casting.Spell", "State.Immobile"];
        cuesDuringCast: ["Cast.Fireball"];
        commitOnComplete: true;   // 读条完成才扣蓝，被打断不扣
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.5; };
        onInterruptEffect: struct FireCue { cue: "Cast.Interrupted"; };
    };

    recovery: {
        duration: struct Const { value: 0.3; };
        recoveryTags: ["State.Recovery"];
        cuesDuringRecovery: [];
    };

    effect: struct SpawnObj { ... };   // 发射火球
}
```

### 蓄力重击（Charged + chargeProgress 影响伤害）

```cfg
ability {
    id: 2001; name: "蓄力重击";
    abilityTags: ["Ability.Type.Melee", "Ability.Type.Charged"];
    costs: [{ stat: "Stamina_Current"; value: struct Const { value: 20.0; }; }];
    cooldown: struct Const { value: 6.0; };

    activation: struct Charged {
        minChargeTime: struct Const { value: 0.5; };
        maxChargeTime: struct Const { value: 3.0; };
        releaseOnMax: true;
        chargeProgressVar: "Var_ChargeProgress";
        chargingTags: ["State.Charging", "State.Immobile"];
        cuesDuringCharge: ["Charge.HeavyStrike"];
        commitOnRelease: true;
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.3; };
        onInterruptEffect: null;
    };

    recovery: {
        duration: struct Const { value: 0.5; };
        recoveryTags: ["State.Recovery", "State.Recovery.Heavy"];
        cuesDuringRecovery: [];
    };

    // 伤害 = 50 + chargeProgress × 150（范围 50~200）
    effect: struct WithTargets {
        targets: { shape: struct Sector { ... }; ... };
        effect: struct Damage {
            damageTags: ["Damage.AttackType.Melee"];
            baseDamage: struct Math {
                op: Add;
                a: struct Const { value: 50.0; };
                b: struct Math { op: Mul;
                    a: struct ContextVar { varKey: "Var_ChargeProgress"; };
                    b: struct Const { value: 150.0; };
                };
            };
            cuesOnHit: ["Hit.HeavyStrike"];
        };
    };
}
```

### 弹幕射击（Channel + 结束收尾效果）

```cfg
ability {
    id: 2002; name: "弹幕射击";
    abilityTags: ["Ability.Type.Ranged", "Ability.Type.Channel"];
    costs: [{ stat: "MP_Current"; value: struct Const { value: 40.0; }; }];
    cooldown: struct Const { value: 12.0; };

    activation: struct Channel {
        duration: struct Const { value: 4.0; };
        tickInterval: struct Const { value: 0.3; };
        tickEffect: struct SpawnObj { ... };  // 每 tick 射出一箭
        maxTicks: -1;
        executeOnStart: true;
        executeOnCancel: false;   // 主动取消不触发收尾效果
        channelingTags: ["State.Channeling"];  // 无 State.Immobile → 可移动引导
        cuesDuringChannel: ["Channel.Barrage"];
        commitOnFirstTick: false;
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.0; };
        onInterruptEffect: null;
    };

    recovery: { duration: struct Const { value: 0.0; }; recoveryTags: []; cuesDuringRecovery: []; };

    // 引导正常结束的收尾爆炸
    effect: struct WithTargets {
        targets: { shape: struct Sphere { ... }; ... };
        effect: struct Damage {
            damageTags: ["Damage.Type.AoE"];
            baseDamage: struct Const { value: 60.0; };
            cuesOnHit: ["Hit.Barrage.Finale"];
        };
    };
}
```

---

## 设计决策记录

本节记录关键取舍的理由，供项目裁剪时参考。

### 为什么连招不作为 ActivationModel 原语

连招的本质是"多个行为按条件链接"。每段的消耗、打断容忍度、后摇通常不同。如果作为原语，要么：
- 在单个 Ability 内嵌入多段独立配置，结构膨胀
- 引入"输入窗口"等与引擎输入系统深度耦合的概念

用独立 Ability + GrantTags 窗口串联，每段保持完整的 Ability 语义，TagRules 和框架能力自然覆盖。编辑器通过依赖分析自动推导连招关系。

### 为什么切换型不作为 ActivationModel 原语

切换型是"Instant Ability + 持续 Status"的直接组合。作为原语不增加任何表达力，只增加概念数量。输入层的"同键切换"路由是引擎职责。

### 为什么不提供 Manual commit

Manual commit 需要在 Effect 树中插入 `CommitAbility` 节点来触发资源扣除。这破坏了 Effect 的无状态原子性——Effect 不应该知道自己处于哪个 Ability 的生命周期中。如果确需精确控制 commit 时机，可以通过拆分为多段 Ability（前段不扣费，关键段扣费）或 Status + Trigger 组合实现。

### 为什么 InterruptResponse 没有 refundCosts

commit 时机的选择已经隐含了打断的经济后果。commitOnComplete=true 意味着"读条被打断不扣费"，这是策划选择该策略时的直接预期。额外提供 refundCosts 会创造出语义矛盾的组合（commitOnActivate + refundCosts=true 意味着"已扣但退还"，这不如一开始就选 commitOnComplete）。

### 为什么 Ability 不直接控制动画

动画是表现层。Ability 层的职责是输出"当前处于什么阶段"（通过 processTags）和"应该播放什么效果"（通过 Cue）。动画状态机应监听这些信号来驱动播放，而不是被 Ability 直接调用。这保持了 A 文档"逻辑与表现隔离"的核心原则。

### maxConcurrent 为什么只是简单整数

技能的并发互斥关系通过 TagRules 管理（`blocksAbilities` / `cancelsAbilities`）。maxConcurrent 只是一个全局安全上限，不是互斥策略的表达手段。完整的互斥矩阵会显著增加配置复杂度，且 TagRules 已经覆盖了绝大多数场景。

---

## 项目裁剪

| 项目类型 | 推荐保留 | 说明 |
|---|---|---|
| 回合制 / 卡牌 | Instant | 无实时施法过程 |
| 轻度 ARPG | Instant + Cast | 蓄力和引导按需加 |
| 重度动作 / 魂系 | Instant + Charged | 后摇和连招（独立 Ability 组合）是核心 |
| MOBA | Instant + Cast + Channel | 标准三件套 |
| MMO | 全部 | 按职业裁剪 |
| 射击 | Instant + Channel + Charged | Cast 通常不需要 |

裁剪方式：从 `ActivationModel` 的 interface 中移除不需要的变体，AbilityInstance 中对应的 tick 分支不实现。已有配置向前兼容。