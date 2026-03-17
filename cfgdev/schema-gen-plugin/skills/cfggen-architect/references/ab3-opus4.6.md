

我来系统性地审视整个 Ability 设计，逐一检查每个决策点。

---

## 逐项审查

### 1. ActivationModel 四原语——是否足够且正交

**Instant / Cast / Charged / Channel** 四种模型对应四种不可互换的"玩家操作→效果生效"时间拓扑。

审查结论：**四种足够且正交**。但有一个被忽视的边界：

- **Cast 和 Charged 的区别**：Cast 是"等待固定时间，自动触发"；Charged 是"持续按住，手动释放"。两者的输入模式完全不同，不可互相模拟。✓
- **Channel 和 Charged 的区别**：Channel 在过程中产出效果（tickEffect），Charged 在结束时产出效果。✓
- **是否缺少"蓄力引导混合型"**（如：按住蓄力，蓄满后自动进入引导阶段）：这可以通过 Charged（releaseOnMax=true）的 effect 中 ApplyStatusInline 含 Periodic 来模拟。不需要原语。✓

**无修改。**

### 2. commitOnXxx 布尔字段 vs 枚举

当前每个模型用语义化布尔（`commitOnComplete`、`commitOnRelease`、`commitOnFirstTick`）。优点是不可能产生非法组合。

但有一个问题：**布尔的默认值语义不明确**。`commitOnComplete: false` 意味着"OnActivate commit"——策划需要反向理解。

改进方案：不用布尔，用每个模型独立的枚举：

```cfg
enum CastCommitPolicy { OnActivate; OnComplete; }
enum ChargedCommitPolicy { OnActivate; OnRelease; }
enum ChannelCommitPolicy { OnActivate; OnFirstTick; }
```

这样策划直接选正向语义，不用理解 false = OnActivate。

**需要修改。**

### 3. InterruptResponse 的 lockoutDuration

当前设计：`lockoutDuration: FloatValue` + `onInterruptEffect: Effect (nullable)`。

问题一：`(nullable)` 用于非引用类型。应改为可选结构或允许空 Effect（如 `struct Sequence { effects: []; }`）。

但实际上这里有更深的问题。回到 A 文档的 interface 语法——`(nullable)` 只修饰 `->` 外键引用。`onInterruptEffect` 是内联的 `Effect`（interface 类型），不是外键引用。对于"可以没有"的 Effect，正确的表达应该是：策划不配此字段时，运行时默认为空操作。

这是配置工具层的问题（字段可选性），不是类型系统的问题。在 cfg 定义中，可以通过"字段不出现在 json 中时使用默认值"来处理。

问题二：lockoutDuration = 0 是否等价于"无 lockout"？是的。那 lockoutDuration 的默认值应该是 0，表示无惩罚。

问题三：InterruptResponse 整体是否应该是可选的？如果 lockoutDuration = 0 且无 onInterruptEffect，整个结构是空操作。但要求每个 Ability 都写一个空的 InterruptResponse 是冗余的。

改进方案：InterruptResponse 应该有零值默认（lockoutDuration = 0, onInterruptEffect = 空），策划不配就是"被打断无惩罚"。这在 cfg 工具层通过字段默认值实现，不需要改类型系统。

**需要小改：明确默认值语义。**

### 4. RecoveryConfig 的默认值问题

同上，`duration = 0` 表示无后摇。整个 RecoveryConfig 应有零值默认。

但 RecoveryConfig 中有 `recoveryTags` 和 `cuesDuringRecovery`，当 duration = 0 时它们无意义。策划可能会困惑："我填了 recoveryTags 但没填 duration，为什么没效果？"

改进方案：如果 duration = 0（或未配置），运行时跳过整个 Recovery 阶段，忽略 tags 和 cues。文档中明确这个行为。

**无结构改动，补充说明。**

### 5. Channel.executeOnCancel 的边界

当前设计：`executeOnCancel: bool` 控制主动取消时是否执行 `ability.effect`。

问题：如果 `executeOnCancel = true`，取消时执行的 `ability.effect` 是用当前的 Context 还是快照的？当前 Context，因为 Context.instanceState 是持续更新的（如引导期间的 tickCount）。这是正确的——策划可以在 effect 中通过 ContextVar 读取引导了多少 tick 来决定收尾效果的强度。

但还有一个问题：cancel 后是否进入 Recovery？当前设计是"cancel → Recovery → End"。这意味着主动取消后仍有后摇。

这合理吗？取决于游戏设计。有些游戏主动取消引导不应有后摇（因为是玩家主动选择，不应惩罚）。有些游戏主动取消仍有后摇（防止无限引导+取消的节奏滥用）。

当前设计是"cancel 后进入 Recovery"，这是偏保守的选择。如果项目不想要取消后的后摇，可以把 recovery.duration 设为 0。但这会导致正常结束也没有后摇。

改进方案：Channel 增加 `recoveryOnCancel: bool`，默认 true。false 时取消直接 End，跳过 Recovery。

等等，这是否过度设计了？策划可以通过以下方式区分：

- 正常结束：recovery.duration = 0.5
- 主动取消：通过 cancel 路径跳过 Recovery

当前流程图已经显示 cancel 路径走到 Recovery。如果要跳过，需要改流程图。这确实是一个合理的分支。

但再想想——如果大多数游戏的取消都不要后摇，那默认行为应该是"cancel 跳过 Recovery"。只有特殊情况才需要取消后也有后摇。

**需要修改：Channel 的 cancel 路径默认跳过 Recovery。**

不对，让我再想想。Cancel 和 Interrupt 的区别：

- Interrupt（外部打断）：惩罚性质，走 InterruptResponse（lockout），**不走 Recovery**
- Cancel（主动取消）：中性行为，**是否走 Recovery 取决于游戏设计**

当前设计：
- Interrupt → InterruptResponse → End（不走 Recovery）✓
- Cancel → Recovery → End

如果取消也不走 Recovery，那 Cancel 和 Interrupt 的区别只剩 lockout 和 onInterruptEffect。这是合理的——它们本质就是"玩家自愿 vs 被迫"的区别，惩罚程度不同。

最终决定：**Cancel 默认不走 Recovery**。如果策划需要取消后也有后摇，可以在 Ability_Cancelled 事件的 Trigger 中用 GrantTags 实现。这与"少数情况通过组合实现"的原则一致。

但等等，Cast 和 Charged 的 cancel 呢？

- Cast 没有 cancel（玩家不能主动取消读条——如果需要取消，那就是 Channel 的语义了）。不对，很多游戏允许移动来取消读条。但移动取消读条实际上是通过 TagRules 实现的（移动产生标签 → cancelsAbilities 打断读条），这走的是 Interrupt 路径而非 Cancel。
- Charged 有 cancel（release 时 elapsed < minChargeTime 视为 cancel）。当前设计这个分支是"Cancel → End"，不走 Recovery。这合理——蓄力不足就是"没打出来"，不应有后摇。

所以 Cancel 不走 Recovery 是全局一致的行为。只有 Channel 当前设计里 cancel 走了 Recovery，需要改正。

**需要修改：Channel 的 cancel 路径改为不走 Recovery。**

### 6. TargetingRequirement 的位置问题

当前 targeting 放在 Ability 表中，这意味着每个技能都携带目标选择信息。

问题：targeting 信息在哪里被消费？

1. **引擎/UI 层**：读取 targeting 类型，触发对应的目标选择 UI（光标、指示器、方向圈）
2. **CanActivate**：验证选择结果的合法性
3. **Activate**：将选择结果写入 initSnapshot

这三步都在 Ability 激活前/时，之后 Effect 层通过 ContextVar 读取，完全不知道 targeting 的存在。**正交性完好。**

### 7. TargetingVarConfig 的必要性

当前设计在 combat_settings 中定义三个全局 var key。

问题：这三个 key 是否真的全局统一就够了？

考虑场景：一个技能需要先选一个目标，再选一个地点（如"传送队友到指定位置"——先选队友，再选地点）。

当前设计只支持一种 TargetingRequirement（SingleTarget / PointTarget / DirectionTarget），不支持复合选择。

这是否是一个必须解决的问题？

大多数游戏的技能目标选择都是单一类型的。复合选择（选目标+选地点）在实际游戏中极少见，通常可以拆分为两段操作：

- "选中队友"是第一个 Ability 的 SingleTarget
- "选择传送点"是第二个 Ability 的 PointTarget
- 两者通过 Tag/Status 串联

或者更常见的做法：选中队友后，技能自动以队友脚下为默认目标点，无需二次选择。

**不改。复合选择通过 Ability 拆分或项目自定义实现。**

### 8. onTargetLost 在 Process 阶段的性能

每帧对 SingleTarget 执行距离/Tag 验证。这在大多数情况下开销可忽略（单次向量距离计算 + Tag 查询），但如果 TagQuery 很复杂...

实际上 TagQuery 的 hasTag 是 O(1)（TagContainer 的设计保证），距离计算也是 O(1)。所以每帧一次完全可接受。

**无问题。**

### 9. Retarget 的歧义

`Retarget` 策略说"复用同一约束扫描最近合法目标"。这隐含了一次 TargetScan 操作。但 TargetingRequirement 中没有 TargetScan 的完整配置（如 maxCount、sort 等）。

Retarget 的语义是明确的：
- shape = Sphere(center=施法者, radius=maxRange)
- allowedRelations = 与 SingleTarget.allowedRelations 相同
- tagQuery = 与 SingleTarget.tagQuery 相同
- maxCount = 1
- sort = Nearest

这些都可以从 SingleTarget 的字段中推导，不需要额外配置。

**无问题。**

### 10. TargetingRequirement.PointTarget 中 requireLineOfSight 的实现

这个字段要求引擎做射线检测。但射线检测的规则（忽略哪些碰撞层、穿透什么障碍）是引擎层决定的，不是配置层决定的。

`requireLineOfSight` 只是一个布尔开关，告诉引擎"是否需要做这个检查"。具体的检查逻辑是引擎实现细节。

**无问题。**

### 11. CanActivate 中 targeting 验证的时机

当前方案把 targeting 验证放在 CanActivate 的最后一步。但实际上引擎可能需要在其他检查通过后才触发目标选择 UI。

这意味着 CanActivate 实际上可能是两阶段的：

1. 预检查（1-5）：判断技能是否可以"开始选择目标"
2. 目标选择 UI（玩家操作）
3. 后检查（6）：验证选择结果

但这是引擎层的实现策略，不是配置层的关注点。配置层只需要声明"需要什么类型的目标"和"什么约束"。引擎层决定何时收集、如何验证。

我需要重新思考：CanActivate 列表中的 targeting 验证是否应该删除？

不，保留它是正确的。它表达的是"完整的激活条件"，实现者可以决定各步的执行顺序和时机。配置文档列出的顺序只是推荐的逻辑顺序。

**无结构改动，明确说明顺序是推荐而非强制。**

### 12. 冲锋示例中 DirectionTarget 的消费方式

示例中冲锋的 effect 是一个 GrantTags + WithTargets AOE。但 directionVar 在哪里被消费？

```cfg
targeting: struct DirectionTarget {};
effect: struct Sequence { effects: [
    struct GrantTags { ... },  // 霸体
    struct WithTargets { ... } // AOE
]; };
```

AOE 的 center 是 `ContextInstigator`，形状是 Sphere。但冲锋应该是"朝 direction 位移一段距离"——这个位移逻辑在 effect 中没有体现。

这暴露了一个问题：**位移/移动不是当前 Effect 系统的原子操作**。

当前系统的 Effect 列表中没有"让施法者移动到某处"或"让施法者朝某方向冲刺"的指令。位移通常由引擎的移动系统处理，不在技能配置层。

冲锋的实际实现可能是：
1. Ability 层：GrantTags（霸体）+ ApplyStatus（包含移动行为的 Status）
2. Status 的 Behavior 驱动移动系统（这超出了当前 A 文档的 Effect 定义范围）

或者更直接地：冲锋的位移是 SpawnObj 的一种特殊情况——施法者本身就是"生成物"，沿 direction 移动。

但这确实超出了当前文档的范围。示例中的冲锋是一个"简化示例"，真实实现需要引擎的移动系统支持。

改进方案：**修改冲锋示例**，移除 DirectionTarget 的示例（因为 direction 的消费涉及引擎移动系统，超出配置层），或简化为"方向射击"（更直观——朝某方向发射子弹）。

实际上 direction 的消费方式就是：SpawnObj 的 moveInfo 中读取 directionVar 作为发射方向。这在文档中已有提及（"引擎的移动系统直接消费"）。

好，让我重新审视示例。冲锋示例确实有问题——它展示了 AOE 但没展示 direction 的消费。改为方向射击更合适。

但冲锋是 DirectionTarget 最直觉的用例...

最终决定：**保留冲锋示例但简化**，明确标注"位移逻辑由引擎移动系统根据 directionVar 执行，超出配置层范围"。将 AOE 改为冲刺终点的效果。

### 13. TargetLostPolicy 遗漏的边界

`TargetLostPolicy.Continue` 说"保留快照继续执行"。但如果目标已死亡，快照中的 Actor 引用指向一个已销毁的实体。后续 Effect 执行时（如 ApplyPipeline）对一个死亡目标应用伤害，会发生什么？

这需要引擎层保证：对已死亡 Actor 的操作是安全的（空操作或自动跳过）。这是引擎实现的保证，不是配置层的关注点。

**无结构改动，补充实现说明。**

### 14. 再审视 Ability.effect 中的 Damage 引用

蓄力重击示例中使用了 `struct Damage`，但 A 文档的 Effect interface 中没有 `Damage` 变体。伤害是通过 `ApplyPipeline` 实现的。

**示例错误，需要修正为 ApplyPipeline。**

### 15. AbilityInstance 和 AbilityComponent 的关系

当前设计：
- `AbilityComponent` 持有 `grantedAbilities`（技能列表）和 `activeInstances`（运行中的实例）
- `maxConcurrent` 控制同时运行的实例数

问题：`grantedAbilities` 是 `List<Ability>`，这意味着技能是通过表引用持有的。但 `activeInstances` 是 `SafeList<AbilityInstance>`。

`maxConcurrent = 1` 意味着标准互斥（同时只能有一个技能在施法）。当新技能激活时，旧技能怎么处理？

当前没有明确：
- 是 reject 新技能？
- 还是 cancel 旧技能？
- 还是看 TagRules？

TagRules 的 `cancelsAbilities` 已经处理了"新技能打断旧技能"的逻辑。`maxConcurrent` 应该只在 TagRules 不适用时作为最终安全阀——当 activeInstances.size >= maxConcurrent 时，reject 新技能。

**补充说明：maxConcurrent 满时 reject 新技能（返回 MaxConcurrent），不自动打断旧技能。打断逻辑走 TagRules。**

### 16. Ability 生命周期事件的 Payload 问题

当前说"Payload 约定：instigator = target = 施法者"。但这些事件通过标准 EventBus 广播，Payload 必须携带 Context sourceContext 引用。

问题：Ability 生命周期事件的 Payload 与 Combat 事件的 Payload 有不同的数据预期。Combat 事件有 magnitude、extras；Ability 事件主要需要 abilityId。

当前设计中 Payload.extras 是 Store 类型，可以自由放 abilityId。这是可行的。

但 Payload.magnitude 在 Ability 事件中无意义（或可以设为 0）。这不是问题——magnitude 是 float，0 是合法的。

**无结构改动。**

### 17. 连招示例中 CD 的问题

示例中第一段 CD=5s，后续段 CD=0。但后续段有独立的 `requiresAll: HasTag "ComboWindow.S2"`——如果窗口标签消失（0.6s后），即使 CD=0 也无法激活。

这是正确的——窗口标签就是连招的"隐式 CD"。但如果玩家在窗口内连续按两次第二段（0.6s 内按两次），会不会触发两次？

会的，因为 CD=0。但第二次按的时候 ComboWindow.S2 已经因为第一次按的第二段执行后被替换为 ComboWindow.S3（如果第二段的 effect 中 GrantTags 是 S3 而非 S2），所以实际上不会触发两次。

等等，GrantTags 的 "ComboWindow.S2" 是第一段 effect 赋予的，第二段激活后不会自动移除 S2。需要在第二段的 effect 中显式 RemoveStatusByTag 移除 S2，或者依赖 S2 的自然过期。

实际上 S2 的 duration=0.6s，是通过 GrantTags（内部是一个微 Status 的语法糖）实现的。第二段激活时，S2 还在有效期内。第二段执行后赋予 S3（duration=0.7s）。但 S2 仍然存在直到 0.6s 到期。

如果在 S2 存在期间（比如 0.3s 时触发了第二段），S2 还剩 0.3s，在这 0.3s 内再按第二段还是能触发。这是否是问题？

是的，这是连招窗口设计中的常见边界。解决方案：

1. 第二段的 effect 中先 RemoveStatusByTag 移除 ComboWindow.S2
2. 或者第二段配 CD > 0（如 0.5s）
3. 或者用 tag_rules 互斥

最简单的方案是第二段 effect 中先移除 S2 的标签。这在连招示例中应该体现。

**修改连招示例，补充移除前置窗口标签的逻辑。**

但其实想想，最干净的方案是让连招窗口用 status 而不是 GrantTags，这样每段 Ability 可以在 requiresAll 中检查 status，在 effect 中 RemoveStatus + GrantTags 新窗口。

或者更简单——GrantTags 是引用计数的。第二段的 effect 开始时就 GrantTags "ComboWindow.S3"，而 ComboWindow.S2 自然过期。如果第二段的 requiresAll 要求 S2 存在，那第二段只能在 S2 存在时触发一次（因为触发后，下一帧 requiresAll 仍然通过——S2 还没过期）。

这确实是个问题。最可靠的方案：**连招段应该有一个极短的 CD（如 0.1s），防止双击触发**。或者在 effect 中用 GrantTags 给施法者一个极短的"连招锁"标签，用 TagRules 阻止重复激活。

这是项目层的细节，不需要在架构文档中详细展开。但示例应该展示最佳实践。

**修改连招示例，每段加极短 CD 或用 RemoveStatusByTag 清理前置窗口。**

### 18. 弹幕射击示例的问题

示例中用了 `struct SpawnObj { ... }` 作为 tickEffect，但 SpawnObj 的字段（duration、moveInfo 等）被省略。这可以用 `...` 标记，但示例应展示 tickEffect 的典型用法——它通常是一个伤害效果或简单 SpawnObj。

更好的示例：tickEffect 直接是 ApplyPipeline（每 tick 对一个目标造成伤害），这样更清晰。

**修改弹幕射击示例。**

### 19. 关于 Ability.costs 的动态消耗

当前 costs 是 `list<StatCost>`，每个 StatCost 有一个 FloatValue。这在 CanActivate 时检查，在 commit 时扣除。

但有些技能的消耗是动态的（如：消耗当前 HP 的 10%）。当前设计支持这个——FloatValue 可以引用 StatValue。

问题：如果检查时 HP = 100（10% = 10，足够），但在 Cast 过程中 HP 降到 5（10% = 0.5），commit 时应扣多少？

当前设计说"costs 的检查和 commit 使用同一份 Context snapshot"。但 snapshot 是在 Activate 时冻结的——如果 commit 是 OnComplete（Cast 完成后），中间 HP 变化了，snapshot 仍然是 Activate 时的值。

这是否正确？取决于设计意图：

- **方案 A（当前）**：使用 Activate 时的 snapshot → 扣除的是 Activate 时的 10%（10点），即使 HP 已降至 5 也扣 10（可能导致 HP 为负）。
- **方案 B**：commit 时重新求值 → 扣除 commit 时的 10%（0.5点），但可能与 CanActivate 时的检查不一致。

方案 A 的问题：可能扣出负数。
方案 B 的问题：TOCTOU 不一致。

实际上大多数游戏采用方案 B（commit 时重新求值）+ 额外的"二次检查"（commit 前再检查一次资源是否足够，不够则 cancel）。

但这增加了 commit 的复杂度。当前文档明确说"使用同一份 snapshot"是为了避免 TOCTOU。

**最终决定**：保留 snapshot 方案（方案 A），但补充说明：stat_definition 的 minLimit 会自然截断负值。如果 HP 的 minLimit = 0，扣除后不会低于 0。这已经是 stat_definition 的设计保证。

**无结构改动，补充说明。**

实际上再想想——**snapshot 的是 cost 的值还是 stat 的值？**

snapshot 的是 cost.value 的求值结果（FloatValue 在 Activate 时被求值一次，结果存入 initSnapshot？不，当前设计没有明确说 cost.value 会被 snapshot）。

让我重新理解 Context 的 initSnapshot：它冻结的是"初始参数"（如蓄力时间、初始锁定目标），不是 cost 值。

那 cost 什么时候被求值？应该是 commit 时。如果 commit 发生在 Activate 时（OnActivate），那就是 Activate 时求值。如果 commit 发生在 OnComplete，那就是 Complete 时求值。

这意味着动态 cost（如 10% HP）会在 commit 时刻求值，取的是当时的 HP 值。这是方案 B。

那 CanActivate 和 commit 之间确实有 TOCTOU 风险。但这个风险在实际游戏中通常可接受——如果 HP 在读条期间降至不够扣的程度，大多数游戏选择：
1. 仍然扣（扣到 minLimit 截断）
2. 或 cancel 技能

最简单的做法：commit 时直接扣，stat 的 minLimit 保证不越界。不做二次检查。

**保持现有设计，不额外复杂化。但在文档中明确：costs.value 在 commit 时刻求值。**

### 20. Recovery 期间被打断的行为

当前设计说"Recovery 期间仍可被 TagRules 打断"。打断后走 InterruptResponse 吗？

如果 ability.effect 已经执行完毕，只是在后摇中，被打断的后果是什么？

- lockoutDuration？不合理——后摇被打断通常意味着"后摇被取消了"，不应该再惩罚一个 lockout。
- onInterruptEffect？也不合理——effect 已经执行完了。

当前设计的 Interrupt 路径："清理所有阶段标签和 Cue → 挂载 lockoutTag → 执行 onInterruptEffect → End"。

如果在 Recovery 阶段被打断，合理的行为应该是：
- 清理 recoveryTags 和 recoveryCues
- **不**挂 lockoutTag（effect 已执行，不应惩罚）
- **不**执行 onInterruptEffect（同上）
- 直接 End

这本质上是"后摇被取消"而非"技能被打断"。

但如果要区分 Process 阶段的打断和 Recovery 阶段的打断，需要在 interrupt() 中检查当前 phase。这增加了运行时复杂度。

替代方案：Recovery 阶段的打断不走 AbilityInstance.interrupt()，而是走 TagRules 的 purgesTags。因为 recoveryTags（如 State.Recovery）是通过 GrantTags 实现的微 Status，purgesTags 直接移除这个微 Status 就等于取消了后摇，不需要触发 AbilityInstance 的 interrupt 逻辑。

等等，但 recovery 是 AbilityInstance 管理的阶段，不是独立的 Status。如果用 GrantTags 实现（内部是微 Status），那微 Status 被 purge 后，AbilityInstance 怎么知道 recovery 结束了？

这涉及 AbilityInstance 和微 Status 的生命周期同步问题。

让我重新思考 Recovery 的实现：

**方案 A（当前隐含的）**：Recovery 是 AbilityInstance 的一个阶段，AbilityInstance.tick() 自己倒计时。recoveryTags 和 cues 由 AbilityInstance 管理。

在这个方案下，TagRules 的 cancelsAbilities 可以打断 Recovery 阶段的 AbilityInstance。但这走的是 interrupt() 路径。

改进：在 interrupt() 中，如果 phase == Recovering，跳过 lockout 和 onInterruptEffect，直接 End。

**方案 B**：Recovery 由 AbilityInstance 在 Execute 完成后通过 GrantTags 实现（等效于 ApplyStatusInline）。AbilityInstance 立即进入 End。后摇的生命周期由微 Status 管理。

在这个方案下，后摇的取消是 Status 层的事（通过 TagRules purgesTags 或 RemoveStatusByTag），与 Ability 完全解耦。

方案 B 更优雅——它让 AbilityInstance 的生命周期在 Execute 后就结束了，Recovery 变成一个普通的 Status。但这与"Recovery 是 Ability 一等概念"的设计目标矛盾。

实际上从策划视角，Recovery 作为 Ability 一等概念的价值在于：
1. 编辑器时间轴可视化
2. 配置集中化
3. 框架统一管理

这些价值在方案 B 中仍然存在——编辑器仍然在 Ability 的 Recovery 字段中配置，只是运行时实现为 Status。

但方案 B 增加了实现复杂度（AbilityInstance 需要在 Execute 后创建微 Status）并且让调试更困难（策划配的是 Ability.recovery，运行时看到的是一个匿名 Status）。

**最终决定：保持方案 A**，但在 interrupt() 中增加 phase 检查：

```java
void interrupt() {
    if (phase == Recovering) {
        // Recovery 阶段被打断：仅清理标签，不惩罚
        cleanup();
        phase = Ended;
        // 广播 Ability_Completed（因为 effect 已经成功执行）
        return;
    }
    // 其他阶段的标准打断逻辑
    ...
}
```

**需要修改：明确 Recovery 阶段打断的特殊处理。**

### 21. 审视 targeting + activation 的组合有效性

| Targeting | Instant | Cast | Charged | Channel |
|---|---|---|---|---|
| None | ✓ 自 buff/AOE | ✓ 自身读条 buff | △ 少见 | ✓ 持续 AOE |
| SingleTarget | ✓ 即时攻击 | ✓ 读条治疗 | ✓ 蓄力打击 | ✓ 引导射击 |
| PointTarget | ✓ 即时 AOE | ✓ 读条 AOE | △ 少见 | ✓ 引导轰炸 |
| DirectionTarget | ✓ 冲锋 | △ 少见 | ✓ 蓄力射线 | ✓ 引导扫射 |

所有组合都有合理的用例或至少不会产生逻辑矛盾。标记 △ 的组合虽少见但不违法。**无需限制组合。**

### 22. costs 和 cooldown 在多段连招中的问题

连招每段独立配 cost 和 cooldown。但 cooldown 的计时起点是什么？

当前 Ability 的 cooldown 在 commit 时启动。如果第一段 commit 时启动 CD=5s，第二段 CD=0，那在第一段 CD 期间（5s 内），第一段无法再次使用。但如果第二段没在窗口期内触发，连招中断后第一段的 CD 仍在倒计时中。

这是正确的行为——第一段的 CD 控制连招的整体节奏。

**无问题。**

### 23. 审视 Ability 表的完整字段列表

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    description: text;
    abilityTags: list<str> ->gameplaytag;
    requiresAll: list<Condition>;
    costs: list<StatCost>;
    cooldown: FloatValue;
    targeting: TargetingRequirement;
    activation: ActivationModel;
    interruptResponse: InterruptResponse;
    recovery: RecoveryConfig;
    effect: Effect;
}
```

每个字段有明确的职责，无冗余。**确认最终字段列表。**

### 24. 最后的一致性检查

- **commitEffects 去哪了？** A 文档原 Ability 有 `commitEffects: Effect`。在新设计中这个字段消失了。commitEffects 的意图是"commit 时执行的额外效果"（如：CD 启动可以视为 commitEffect）。

实际上 commitEffects 在新设计中不再需要——commit 就是"扣资源 + 启动 CD"，是框架行为，不需要额外 Effect。如果需要在 commit 时做额外逻辑（如：扣资源时触发某个被动），可以通过事件监听 `Ability_Committed` 实现。

**确认移除 commitEffects。**

---

## 汇总改动清单

1. **commit 策略**：布尔字段改为模型专用枚举（`CastCommitPolicy` / `ChargedCommitPolicy` / `ChannelCommitPolicy`）
2. **Channel.cancel 路径**：不走 Recovery，直接 End
3. **Recovery 阶段打断**：不执行 lockout 和 onInterruptEffect，广播 Completed 而非 Interrupted
4. **连招示例**：每段加极短 CD 或清理前置窗口标签
5. **示例修正**：Damage 改为 ApplyPipeline；弹幕示例的 tickEffect 具体化
6. **文档说明补充**：costs.value 在 commit 时求值；Recovery 计时起点；maxConcurrent 满时 reject；CanActivate 顺序为推荐而非强制
7. **默认值语义**：InterruptResponse 和 RecoveryConfig 的零值默认行为
8. **移除 commitEffects**

---

现在输出完整的最终文档。

---

# Ability 施法过程模型

## 当前问题

A 文档的 Ability 结构本质上是：

```
准入检查 → 扣资源 → 执行 Effect → 结束
```

这是一个瞬发模型。但真实游戏的施法过程远比这复杂——读条、蓄力、引导、后摇，每种都涉及打断、资源提交时机、行为约束等通用需求。

当前可以用 Status + Timeline 组合模拟这些过程，但会导致：
- **语义割裂**：策划认知中的"技能过程"，配置上变成"瞬发 Ability 挂 Status"
- **框架能力缺失**：打断 / commit 每个技能自己拼
- **工具无力**：编辑器无法呈现技能时间轴

---

## 设计原则

1. **瞬发零开销**：`Instant` 与现有行为完全一致，不被复杂模型拖累
2. **Ability 管过程，Status 管状态**：施法结束后的持续效果仍归 Status
3. **Tag 是唯一约束语言**：施法期间的行为约束（不可移动、不可施法）全部通过标签 + TagRules 表达，不引入布尔开关
4. **最少原语**：只保留不可互相还原的四种施法模型，复合模式（连招、切换）通过已有机制组合
5. **commit 与模型绑定**：每个模型使用独立的 commit 枚举，消除无效组合
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

    targeting: TargetingRequirement;
    activation: ActivationModel;
    interruptResponse: InterruptResponse;
    recovery: RecoveryConfig;

    effect: Effect;
}
```

**字段职责**：

| 字段 | 职责 |
|---|---|
| `targeting` | 声明需要什么外部输入（目标/地点/方向），引擎据此驱动选择 UI |
| `activation` | 施法的时间模型（Instant / Cast / Charged / Channel） |
| `interruptResponse` | 被打断后的惩罚与反馈 |
| `recovery` | 后摇阶段配置 |

**未新增的字段及理由**：

| 被排除的概念 | 理由 |
|---|---|
| `allowMovement: bool` | 通过标签表达（如 `State.Immobile`），移动系统检查该标签 |
| `comboGroup: str` | 编辑器可通过分析 `requiresAll` 的 `HasTags` 依赖链自动推导连招组 |
| `commitEffects: Effect` | commit 是框架行为（扣资源 + 启动 CD），额外逻辑通过 `Ability_Committed` 事件监听 |
| 独立的 `CommitPolicy` 枚举 | 内嵌到各模型为独立枚举，消除无效组合 |

---

## TargetingRequirement

```cfg
interface TargetingRequirement {
    struct None {}

    struct SingleTarget {
        allowedRelations: list<Relation>;
        tagQuery: TagQuery;
        maxRange: FloatValue;
        onTargetLost: TargetLostPolicy;
    }

    struct PointTarget {
        maxRange: FloatValue;
        requireLineOfSight: bool;
    }

    struct DirectionTarget {}
}

enum TargetLostPolicy {
    Cancel;
    Retarget;
    Continue;
}
```

**设计说明**：

- **`struct None {}`**：无需外部目标输入（自身 AOE、自 buff）。自我施法通过 Effect 中的 `ContextInstigator` 引用施法者。
- **`DirectionTarget` 为空**：方向是归一化向量，无需距离 / 关系 / Tag 验证。
- **`onTargetLost` 仅对 `SingleTarget` 有意义**：`PointTarget` 和 `DirectionTarget` 在激活时冻结为标量数据，不存在"丢失"。
- **`Retarget` 的隐含行为**：复用同一 `SingleTarget` 的约束条件，按最近距离选取替代目标，写入 `instanceState[targetVar]`（覆盖 `initSnapshot` 中的原始值）。
- **`Continue` 安全保证**：引擎须确保对已死亡 / 已销毁 Actor 的操作是安全的空操作。

### 全局目标变量

99% 的技能向相同的 var key 写入目标数据。逐技能配置是冗余的，由 `combat_settings` 统一定义：

```cfg
// combat_settings 扩展
struct TargetingVarConfig {
    targetVar: str ->var_key;      // 如 "Var_SelectedTarget"（存 Actor）
    pointVar: str ->var_key;       // 如 "Var_TargetPoint"（存 Position）
    directionVar: str ->var_key;   // 如 "Var_TargetDirection"（存 Vector）
}
```

引擎在 Ability 激活时将选择结果写入 `initSnapshot`，所有 Effect 通过 `ContextVar` 统一读取。

### 与现有机制的正交关系

| 机制 | 职责 | 时机 | 数据来源 |
|---|---|---|---|
| `TargetingRequirement` | 声明需要什么输入，验证合法性 | Ability 激活前 | 玩家操作 |
| `TargetSelector` | 从 Context / Payload 中选取**一个**已知实体 | Effect 执行时 | initSnapshot / instanceState / Payload |
| `TargetScan` | 在空间中实时扫描**多个**实体 | Effect 执行时 | 物理世界 |

三者严格正交：Targeting 注入初始数据 → TargetSelector 从中读取 → TargetScan 在此基础上扩展空间查询。

### 数据流

```
玩家操作                    引擎/UI 层                              Ability 层
─────────                  ──────────                              ──────────
                     读取 ability.targeting 类型
                           │
选中敌人 ──→  验证 allowedRelations / tagQuery / maxRange
              通过 → 写入 initSnapshot[settings.targetingVars.targetVar]
                           │
点击地面 ──→  验证 maxRange / lineOfSight
              通过 → 写入 initSnapshot[settings.targetingVars.pointVar]
                           │
拖拽方向 ──→  归一化 → 写入 initSnapshot[settings.targetingVars.directionVar]
                           │
              struct None → 不收集任何输入
                           │
                           ▼
                    CanActivate 检查（targeting 验证已在此前完成）
                           │
                           ▼
                    Activate: 冻结 initSnapshot
                           │
                           ▼
                    Effect 中通过 ContextVar 读取
```

---

## ActivationModel

```cfg
interface ActivationModel {
    struct Instant {}

    struct Cast {
        castTime: FloatValue;
        castingTags: list<str> ->gameplaytag;
        cuesDuringCast: list<str> ->cue_key;
        commitPolicy: CastCommitPolicy;
    }

    struct Charged {
        minChargeTime: FloatValue;
        maxChargeTime: FloatValue;
        releaseOnMax: bool;
        chargeProgressVar: str ->var_key;
        chargingTags: list<str> ->gameplaytag;
        cuesDuringCharge: list<str> ->cue_key;
        commitPolicy: ChargedCommitPolicy;
    }

    struct Channel {
        duration: FloatValue;
        tickInterval: FloatValue;
        tickEffect: Effect;
        maxTicks: int;
        executeOnStart: bool;
        executeOnCancel: bool;
        channelingTags: list<str> ->gameplaytag;
        cuesDuringChannel: list<str> ->cue_key;
        commitPolicy: ChannelCommitPolicy;
    }
}

enum CastCommitPolicy {
    OnActivate;    // 激活即扣资源（被打断不退费）
    OnComplete;    // 读条完成时扣资源（被打断不扣费）
}

enum ChargedCommitPolicy {
    OnActivate;    // 激活即扣资源
    OnRelease;     // 释放时扣资源（蓄力不足/取消不扣费）
}

enum ChannelCommitPolicy {
    OnActivate;    // 激活即扣资源（默认）
    OnFirstTick;   // 首次 tick 时扣资源
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

每个模型用语义明确的枚举替代跨模型的统一枚举，策划不可能配出逻辑矛盾的组合：

| 模型 | 合法选项 | Instant 无此字段 |
|---|---|---|
| Cast | OnActivate / OnComplete | — |
| Charged | OnActivate / OnRelease | — |
| Channel | OnActivate / OnFirstTick | — |

Instant 始终 OnActivate，没有 commitPolicy 字段。

### 行为约束通过标签表达

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
    lockoutDuration: FloatValue;
    onInterruptEffect: Effect;
}
```

**默认值语义**：`lockoutDuration` 默认为 0（无锁定），`onInterruptEffect` 默认为空操作。策划不配则等同于"被打断无惩罚"。

**lockoutTag 采用全局默认**：

```cfg
// combat_settings 扩展
defaultLockoutTag: str ->gameplaytag;  // 如 "State.AbilityLockout"
```

少数需要自定义 lockout 行为的技能，在 `onInterruptEffect` 中用 `GrantTags` 实现差异化。

**资源退还不需要额外字段**：commit 时机的选择已经隐含了打断时的经济后果。`OnComplete` = "读条被打断不扣费"，`OnActivate` = "已扣不退"。不会出现语义矛盾。

---

## RecoveryConfig

```cfg
struct RecoveryConfig {
    duration: FloatValue;
    recoveryTags: list<str> ->gameplaytag;
    cuesDuringRecovery: list<str> ->cue_key;
}
```

**默认值语义**：`duration` 默认为 0（无后摇），运行时跳过整个 Recovery 阶段，忽略 tags 和 cues。

**定位**：`RecoveryConfig` 是"最常见后摇场景"的语法糖。等价于在 effect 末尾追加一个 `GrantTags`，但将其提升为 Ability 一等概念有三点不可替代的价值：

1. **框架可识别**：编辑器可在技能时间轴上渲染后摇区间
2. **语义标准化**：策划不需要每个技能手写 GrantTags
3. **动作取消有统一锚点**：TagRules 中对 `State.Recovery` 的规则一处配置，覆盖所有技能

超出语法糖范围的复杂后摇（如前半段不可取消、后半段可取消）应回到 effect 中用组合手段实现。

---

## 完整执行阶段

### 阶段流程

```
CanActivate ──→ Activate ──→ Process ──→ Execute ──→ Recovery ──→ End
                                │                        │
                           Interrupt                Interrupt
                                │                        │
                                ▼                        ▼
                       InterruptResponse          直接 End（无惩罚）
                                │
                                ▼
                               End
```

### CanActivate

推荐检查顺序（从快到慢，尽早拒绝）：

1. TagRules 的 `blocksAbilities` 是否拦截当前 `abilityTags`
2. `cooldown` 就绪
3. `costs` 资源充足（检查但不扣除）
4. `requiresAll` 条件满足
5. `maxConcurrent` 未超限（达到上限时 reject，不自动打断旧技能）
6. `targeting` 验证（目标 / 地点 / 方向已由引擎收集且合法）

全部通过才进入 Activate。`tryActivate` 返回失败原因枚举（`ActivateResult`），供 UI 层反馈。顺序可由项目调整以匹配 UI 反馈优先级。

**Targeting 验证放在最后**：它依赖引擎 / UI 层的前置交互（选目标、点地面），引擎可以在步骤 1-5 通过后再触发目标选择 UI（如技能指示器），选择完成后才执行步骤 6。

### Activate

- 创建 `AbilityInstance`
- 冻结 `initSnapshot`（含 targeting 选择结果）
- 若 commitPolicy 为 OnActivate（Instant / 或其他模型选择 OnActivate） → 立即 commit（扣资源、启动 CD）
- 广播 `Ability_Activated` 事件
- 进入 Process（Instant 跳过此阶段直接进入 Execute）

### Process

各模型行为不同（见"各模型状态转移"）。共同行为：

- 授予阶段标签（castingTags / chargingTags / channelingTags） → 写入宿主 TagContainer
- 播放过程 Cue（Sustained 类型）
- 每帧接受 TagRules 打断检测
- 若 targeting 为 `SingleTarget`，每帧执行目标追踪验证（见"Process 阶段目标追踪"）

### Execute

- 若 commit 尚未发生 → **先 commit**（扣资源、启动 CD）
- 移除阶段标签和过程 Cue
- 执行 `ability.effect`
- 广播 `Ability_Executed` 事件

**costs.value 在 commit 时刻求值**。对于延迟 commit（OnComplete / OnRelease / OnFirstTick），求值发生在该时刻而非 Activate 时。stat_definition 的 minLimit 保证属性值不会越界。

### Recovery

- 仅在 `recovery.duration > 0` 时进入此阶段
- 授予 `recoveryTags`，播放后摇 Cue
- 倒计时结束 → 移除标签和 Cue → 进入 End

**Recovery 阶段被打断的特殊处理**：ability.effect 已成功执行，打断仅中止后摇。此时**不执行** InterruptResponse（无 lockout、无 onInterruptEffect），直接清理标签并广播 `Ability_Completed`（而非 `Ability_Interrupted`）。

### End

- 清理所有残留的阶段标签和 Cue
- 广播 `Ability_Completed` 事件
- 销毁 AbilityInstance

### Interrupt（Process 阶段触发）

- 若 commit 尚未发生 → 不消耗资源，不启动 CD
- 若 commit 已发生 → 保留消耗
- 清理所有阶段标签和 Cue
- 挂载 `defaultLockoutTag`，持续 `lockoutDuration`
- 执行 `onInterruptEffect`
- 广播 `Ability_Interrupted` 事件
- 销毁 AbilityInstance

---

## Process 阶段目标追踪

仅 `SingleTarget` 在 Process 阶段执行持续验证：

```
Process 每帧:
    1. 正常阶段逻辑（累加时间、更新蓄力进度等）
    2. 若 targeting 为 SingleTarget:
        a. 取 target = ContextVar 求值 targetVar
        b. 验证: target 存活 ∧ 在 maxRange 内 ∧ 满足 tagQuery
        c. 若失败 → 按 onTargetLost 处理:
           - Cancel   → cancel()
           - Retarget → 用同一约束扫描最近合法目标
                        写入 instanceState[targetVar]
           - Continue → 不处理
```

`PointTarget` / `DirectionTarget` 激活时冻结为标量，Process 期间不做验证。

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
    → [OnActivate ? commit : skip]
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
    → [OnActivate ? commit : skip]
    → Process: 挂 chargingTags
    │   每帧: chargeProgress = clamp(elapsed / maxChargeTime, 0, 1)
    │         写入 context.instanceState[chargeProgressVar]
        │
        ├── release() 且 elapsed >= minChargeTime
        │       → Execute（chargeProgress 可在 Effect 中通过 ContextVar 读取）
        │       → Recovery → End
        │
        ├── release() 且 elapsed < minChargeTime
        │       → Cancel → End（不 commit，不执行 effect，不走 Recovery）
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
    → [OnActivate ? commit : skip]
    → Process: 挂 channelingTags
    │   [executeOnStart=true ? 执行首次 tickEffect : skip]
    │   每 tickInterval:
    │       [OnFirstTick 且首次 ? commit : skip]
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
        │       → End（不走 Recovery）
        │
        └── interrupt()
                → InterruptResponse → End
```

**Cancel 不走 Recovery**：主动取消是玩家意愿行为，不应有额外惩罚。如需取消后摇，可在 `Ability_Cancelled` 事件的 Trigger 中用 `GrantTags` 实现。

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

**技能互斥**也走同一条路径：

```cfg
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
 CD: 5s                       CD: 0.1s (防双击)             CD: 0.1s
                               requiresAll:                 requiresAll:
                                 HasTag "ComboWindow.S2"      HasTag "ComboWindow.S3"

 effect:                      effect:                      effect:
   RemoveStatusByTag           RemoveStatusByTag             Damage(80)
     {requireAll:                {requireAll:                + GrantTags
       ["ComboWindow"]}           ["ComboWindow"]}            "State.Buff.Hyperarmor"
   + Damage(30)                + Damage(40)                   dur: 0.3s
   + GrantTags                 + GrantTags
     "ComboWindow.S2"           "ComboWindow.S3"
     dur: 0.6s                  dur: 0.7s

 recovery: 0.3s               recovery: 0.4s               recovery: 0.6s
```

**关键细节**：

- 每段 effect 先 `RemoveStatusByTag` 清理前置窗口标签，防止在窗口期内重复触发
- 后续段配极短 CD（0.1s）作为防双击保险
- 每段独立配 cost / cooldown / interruptResponse / recovery，TagRules 独立治理每段
- 编辑器通过分析 `requiresAll` 中的 `HasTags` 依赖链自动推导连招组关系，无需额外元数据

**CD 策略变体**：

| 策略 | 实现方式 |
|---|---|
| 起手段 CD 控制节奏 | 第一段 CD=5s，后续段 CD=0.1s |
| 完成全套后进入 CD | 终结段 effect 中 GrantTags 挂 Cooldown 标签，起手段 requiresAll 检查该标签不存在 |
| 每段独立 CD | 每段各自配 CD |

### 切换型

两个互斥 Ability + 一个持续 Status：

```cfg
// 举盾（requiresAll: 没有盾姿态标签 → 可激活）
ability { id:3001; activation: struct Instant {};
          effect: ApplyStatus { statusId: 5010; }; }
// Status 5010: grantedTags=["State.Buff.ShieldStance"], 含 Periodic 扣耐、StatModifier 加防

// 收盾（requiresAll: 有盾姿态标签 → 可激活）
ability { id:3002; activation: struct Instant {};
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
"Ability_Interrupted"     // 被打断（仅 Process 阶段）
"Ability_Cancelled"       // 主动取消（Charged 蓄力不足 / Channel 主动停止）
```

应用示例：

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
    Blocked,
    OnCooldown,
    InsufficientCost,
    ConditionFailed,
    MaxConcurrent,
    TargetInvalid
}

class AbilityInstance implements IPendingKill {
    Ability config;
    Context context;
    Actor owner;

    AbilityPhase phase;
    float phaseElapsed;
    boolean committed;
    boolean pendingKill;

    void tick(float dt);
    void release();         // Charged 专用
    void cancel();          // 主动取消
    void interrupt();       // TagRules 驱动
}

enum AbilityPhase {
    Activating,
    Processing,
    Executing,
    Recovering,
    Ended
}
```

---

## 示例

### 火球术（Cast + 延迟 commit + 指向性目标）

```cfg
ability {
    id: 1001; name: "火球术";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire"];
    costs: [{ stat: "MP_Current"; value: struct Const { value: 30.0; }; }];
    cooldown: struct Const { value: 8.0; };

    targeting: struct SingleTarget {
        allowedRelations: [Hostile];
        tagQuery: {};
        maxRange: struct Const { value: 30.0; };
        onTargetLost: Cancel;
    };

    activation: struct Cast {
        castTime: struct Const { value: 2.0; };
        castingTags: ["State.Casting", "State.Casting.Spell", "State.Immobile"];
        cuesDuringCast: ["Cast.Fireball"];
        commitPolicy: OnComplete;
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

    effect: struct SpawnObj {
        duration: struct Const { value: 5.0; };
        objTags: ["Obj.Projectile.Fireball"];
        moveInfo: { /* 引擎朝 Var_SelectedTarget 追踪飞行 */ };
        cuesWhileActive: ["Projectile.Fireball"];
        effectsOnCreate: [];
        dieInfo: [{ /* 碰撞后 ApplyPipeline "StandardPhysicalDamage" */ }];
    };
}
```

### 蓄力重击（Charged + chargeProgress 影响伤害）

```cfg
ability {
    id: 2001; name: "蓄力重击";
    abilityTags: ["Ability.Type.Melee", "Ability.Type.Charged"];
    costs: [{ stat: "Stamina_Current"; value: struct Const { value: 20.0; }; }];
    cooldown: struct Const { value: 6.0; };

    targeting: struct None {};

    activation: struct Charged {
        minChargeTime: struct Const { value: 0.5; };
        maxChargeTime: struct Const { value: 3.0; };
        releaseOnMax: true;
        chargeProgressVar: "Var_ChargeProgress";
        chargingTags: ["State.Charging", "State.Immobile"];
        cuesDuringCharge: ["Charge.HeavyStrike"];
        commitPolicy: OnRelease;
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.3; };
    };

    recovery: {
        duration: struct Const { value: 0.5; };
        recoveryTags: ["State.Recovery", "State.Recovery.Heavy"];
        cuesDuringRecovery: [];
    };

    // 伤害 = 50 + chargeProgress × 150（范围 50~200）
    effect: struct WithTargets {
        targets: {
            shape: struct Sector {
                center: struct ContextInstigator {};
                facingOf: struct ContextInstigator {};
                radius: struct Const { value: 4.0; };
                angle: struct Const { value: 120.0; };
            };
            relationTo: struct ContextInstigator {};
            allowedRelations: [Hostile];
            tagQuery: {};
            exclude: [];
            maxCount: 5;
            sort: Nearest;
        };
        effect: struct ApplyPipeline {
            pipeline: "StandardPhysicalDamage";
            magnitude: struct Math {
                op: Add;
                a: struct Const { value: 50.0; };
                b: struct Math { op: Mul;
                    a: struct ContextVar { varKey: "Var_ChargeProgress"; };
                    b: struct Const { value: 150.0; };
                };
            };
            tags: ["Damage.AttackType.Melee"];
            cuesOnExecute: ["Hit.HeavyStrike"];
        };
    };
}
```

### 弹幕射击（Channel + 收尾效果）

```cfg
ability {
    id: 2002; name: "弹幕射击";
    abilityTags: ["Ability.Type.Ranged", "Ability.Type.Channel"];
    costs: [{ stat: "MP_Current"; value: struct Const { value: 40.0; }; }];
    cooldown: struct Const { value: 12.0; };

    targeting: struct SingleTarget {
        allowedRelations: [Hostile];
        tagQuery: {};
        maxRange: struct Const { value: 25.0; };
        onTargetLost: Retarget;
    };

    activation: struct Channel {
        duration: struct Const { value: 4.0; };
        tickInterval: struct Const { value: 0.3; };
        // 每 tick 对目标造成伤害
        tickEffect: struct WithTarget {
            target: struct ContextVar { varKey: "Var_SelectedTarget"; };
            effect: struct ApplyPipeline {
                pipeline: "StandardPhysicalDamage";
                magnitude: struct Const { value: 15.0; };
                tags: ["Damage.AttackType.Ranged"];
                cuesOnExecute: ["Hit.Arrow"];
            };
        };
        maxTicks: -1;
        executeOnStart: true;
        executeOnCancel: false;
        channelingTags: ["State.Channeling"];    // 无 State.Immobile → 可移动引导
        cuesDuringChannel: ["Channel.Barrage"];
        commitPolicy: OnActivate;
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.0; };
    };

    recovery: {
        duration: struct Const { value: 0.0; };
        recoveryTags: [];
        cuesDuringRecovery: [];
    };

    // 引导正常结束的收尾爆炸
    effect: struct WithTarget {
        target: struct ContextVar { varKey: "Var_SelectedTarget"; };
        effect: struct ApplyPipeline {
            pipeline: "StandardPhysicalDamage";
            magnitude: struct Const { value: 60.0; };
            tags: ["Damage.Type.AoE"];
            cuesOnExecute: ["Hit.Barrage.Finale"];
        };
    };
}
```

### 火雨（Cast + 落点目标）

```cfg
ability {
    id: 1003; name: "火雨";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire"];
    costs: [{ stat: "MP_Current"; value: struct Const { value: 60.0; }; }];
    cooldown: struct Const { value: 15.0; };

    targeting: struct PointTarget {
        maxRange: struct Const { value: 25.0; };
        requireLineOfSight: false;
    };

    activation: struct Cast {
        castTime: struct Const { value: 2.0; };
        castingTags: ["State.Casting", "State.Casting.Spell", "State.Immobile"];
        cuesDuringCast: ["Cast.FireRain"];
        commitPolicy: OnComplete;
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.5; };
    };

    recovery: {
        duration: struct Const { value: 0.0; };
        recoveryTags: [];
        cuesDuringRecovery: [];
    };

    // pointVar 由引擎在 SpawnObj 中消费作为生成位置
    effect: struct SpawnObj {
        duration: struct Const { value: 6.0; };
        objTags: ["Obj.Zone.FireRain"];
        moveInfo: { /* 静态：生成在 Var_TargetPoint */ };
        cuesWhileActive: ["Zone.FireRain"];
        effectsOnCreate: [
            // 周期性 AOE 伤害通过内嵌 ApplyStatusInline 实现
        ];
        dieInfo: [];
    };
}
```

### 战吼（Instant + 无目标 + 自身 AOE）

```cfg
ability {
    id: 1005; name: "战吼";
    abilityTags: ["Ability.Type.Melee"];
    costs: [{ stat: "Stamina_Current"; value: struct Const { value: 15.0; }; }];
    cooldown: struct Const { value: 20.0; };

    targeting: struct None {};
    activation: struct Instant {};

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.0; };
    };

    recovery: {
        duration: struct Const { value: 0.0; };
        recoveryTags: [];
        cuesDuringRecovery: [];
    };

    effect: struct Sequence { effects: [
        struct FireCue { cue: "Ability.Warcry"; },
        struct WithTargets {
            targets: {
                shape: struct Sphere {
                    center: struct ContextInstigator {};
                    radius: struct Const { value: 10.0; };
                };
                relationTo: struct ContextInstigator {};
                allowedRelations: [Hostile];
                tagQuery: {};
                exclude: [];
                maxCount: -1;
                sort: None;
            };
            effect: struct ApplyStatus { statusId: 8001; captures: []; };
        }
    ]; };
}
```

---

## 设计决策记录

### 为什么连招不作为 ActivationModel 原语

连招的本质是"多个行为按条件链接"。每段的消耗、打断容忍度、后摇通常不同。如果作为原语，要么在单个 Ability 内嵌入多段独立配置（结构膨胀），要么引入"输入窗口"等与引擎输入系统深度耦合的概念。用独立 Ability + GrantTags 窗口串联，每段保持完整的 Ability 语义，TagRules 和框架能力自然覆盖。

### 为什么切换型不作为 ActivationModel 原语

切换型是"Instant Ability + 持续 Status"的直接组合。作为原语不增加任何表达力，只增加概念数量。输入层的"同键切换"路由是引擎职责。

### 为什么不提供 Manual commit

Manual commit 需要在 Effect 树中插入 `CommitAbility` 节点来触发资源扣除。这破坏了 Effect 的无状态原子性——Effect 不应该知道自己处于哪个 Ability 的生命周期中。如果确需精确控制 commit 时机，可以通过拆分为多段 Ability 或 Status + Trigger 组合实现。

### 为什么 InterruptResponse 没有 refundCosts

commit 时机的选择已经隐含了打断的经济后果。`OnComplete` = "读条被打断不扣费"是策划选择该策略时的直接预期。提供 `refundCosts` 会创造语义矛盾的组合（`OnActivate` + `refundCosts=true` 等价于应该一开始就选 `OnComplete`）。

### 为什么 Ability 不直接控制动画

动画是表现层。Ability 层的职责是输出"当前处于什么阶段"（通过阶段标签）和"应该播放什么效果"（通过 Cue）。动画状态机应监听这些信号来驱动播放，保持 A 文档"逻辑与表现隔离"的核心原则。

### maxConcurrent 为什么只是简单整数

技能的并发互斥关系通过 TagRules 管理（`blocksAbilities` / `cancelsAbilities`）。`maxConcurrent` 只是全局安全上限——达到上限时 reject 新技能，不自动打断旧技能。打断逻辑始终走 TagRules。

### Recovery 阶段打断为什么不走 InterruptResponse

ability.effect 已成功执行，打断仅中止后摇。此时 lockout 和 onInterruptEffect 都是不合理的惩罚——策划预期的"被打断惩罚"针对的是"技能没放出来"的情况，而非"技能放完了在收招"。

### Cancel 为什么不走 Recovery

主动取消是玩家意愿行为（蓄力不足松手、主动停止引导）。额外施加后摇是反直觉的惩罚。如果特定游戏设计需要取消后摇，可以在 `Ability_Cancelled` 事件的 Trigger 中用 `GrantTags` 实现。

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

裁剪方式：从 `ActivationModel` 的 interface 中移除不需要的变体，`AbilityInstance` 中对应的 tick 分支不实现。已有配置向前兼容。

---

## combat_settings 完整修订

```cfg
table combat_settings[name] {
    name: str;
    tagRules: list<str> ->tag_rules;

    maxDispatchDepth: int;
    maxStatusCountPerActor: int;
    maxEffectChainLength: int;
    maxScanTargets: int;

    defaultLockoutTag: str ->gameplaytag;
    targetingVars: TargetingVarConfig;
}

struct TargetingVarConfig {
    targetVar: str ->var_key;
    pointVar: str ->var_key;
    directionVar: str ->var_key;
}
```