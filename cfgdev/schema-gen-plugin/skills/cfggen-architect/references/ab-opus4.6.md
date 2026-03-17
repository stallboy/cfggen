

我需要认真反省当前版本的问题。让我逐一审视：

**1. recovery 作为一等字段的问题**
后摇本质上是"effect执行完毕后的一段受限时间"。但这个设计有问题：
- 如果是Cast技能，后摇在读条完成+effect执行之后。但如果effect本身有延迟（比如SpawnObj），后摇应该从什么时候开始？
- Channel技能的后摇在引导结束后，但引导结束时还执行了ability.effect，如果这个effect很复杂呢？
- 后摇期间能不能被特定操作取消？当前通过TagRules的block/不block来实现，这没问题。
- 但后摇的"可取消窗口"呢？比如前0.2秒不可取消，后0.3秒可取消？当前做不了。

实际上，recovery就是一段GrantTags的语法糖。但它只是"最简单情况的语法糖"。稍微复杂的后摇（分阶段、可取消窗口）还是得回到Status/Timeline。

**反思结论**：recovery作为便捷字段保留，但要明确它只是语法糖，复杂后摇应使用effect中的手段。

**2. CommitPolicy的问题**
OnComplete和OnFirstExecute的语义在不同模型下含义不同：
- Cast的OnComplete = castTime结束时
- Charged的OnComplete = release时
- Channel的OnComplete = 引导结束时？那和OnActivate有什么大区别？

实际上commit时机和ActivationModel是耦合的。让CommitPolicy独立为enum会造成"无效组合"（如Instant+OnComplete没有意义）。

**反思结论**：commit时机应该内嵌到各ActivationModel中，而不是独立字段。每个模型有自己的默认commit时机和可选项。

**3. InterruptResponse的问题**
- refundCosts和refundCooldown只在"已commit但被打断"时有意义。如果commitPolicy是OnComplete且被打断时还没commit，就根本不需要refund。
- lockoutDuration是一个好概念，但它本质上是一个微状态。

**反思结论**：简化InterruptResponse，去掉和commit时机重复的判断。

**4. allowMovement的问题**
这是一个"引擎层行为"写入了数据配置层。移动约束应该通过Tag来表达，而不是布尔字段。比如castingTags里加一个"State.Immobile"，然后移动系统检查这个tag。

**反思结论**：移除allowMovement，通过castingTags表达。这更符合"Tag是唯一通用语言"的哲学。

**5. 连招实现方式的再思考**
用独立Ability+GrantTags窗口的方案在技术上是干净的，但有一个实际问题：
- 策划体验：三段连招配了三个ability，CD管理、UI显示、技能栏占位都需要额外处理
- CD策略：示例中CD挂在第一段，但如果希望"完成整套连招后才进入CD"呢？
- 连招状态可视化：编辑器中三个分散的ability很难一目了然看出它们是一套连招

**反思结论**：连招确实不应该是ActivationModel原语，但应该在ability层提供"连招组"的轻量级元数据支持，辅助UI和工具，不改变运行时逻辑。

**6. 更深层的反思：Ability层到底应该多厚？**
回顾A文档的哲学："三层正交分离"。Ability是"行为入口"。

如果把太多施法过程逻辑放进Ability，它就不再是"入口"了，而是变成了一个小型状态机引擎。这和Status的Timeline能力重叠。

真正的问题是：**施法过程是Ability的内在能力，还是应该视为一种特殊的Status？**

GAS的答案是：Ability可以有Duration，可以有Task（类似Timeline），Ability本身就是一个有生命周期的实体。

但A文档的设计哲学是Ability轻、Status重。

**反思结论**：保持Ability相对轻量，但必须承认施法过程是Ability不可推卸的职责。关键是找到"最小必要"的Ability层增强。最小必要 = 过程原语 + commit时机 + 打断响应 + 后摇。不要再多了。

**7. 再想一个根本问题：ActivationModel各字段里的cuesDuringXxx**
这些Cue字段和Status的cuesWhileActive逻辑完全一致。这说明施法过程在运行时确实有"持续型Cue"的需求。但如果施法过程用Status实现的话，cuesWhileActive天然就有了。

这揭示了一个设计张力：如果施法过程不走Status，就要在Ability层重新实现Cue的生命周期管理。如果走Status，Ability层又太薄了。

**反思结论**：施法过程的Cue管理应和Status的cuesWhileActive走同一套底层机制。AbilityInstance在生命周期上模拟一个"内部Status"来管理Cue。但这是实现层面的事，配置层面暴露cuesDuringXxx是合理的。

**8. commitPolicy移入模型后的设计**

实际上每个模型的commit选项是不同的：
- Instant：只有OnActivate有意义
- Cast：OnActivate或OnComplete
- Charged：OnActivate或OnRelease（就是OnComplete的语义）
- Channel：OnActivate或OnFirstTick

所以不如：每个需要选择commit时机的模型，内部提供一个commitOnComplete: bool字段。Instant不提供（固定OnActivate）。

这更简洁，避免无效组合。

**9. 各模型tags字段的反思**
castingTags, chargingTags, channelingTags——这三个字段语义完全一样，只是名字不同。都是"过程期间授予宿主的标签"。

能不能统一成一个字段？比如processTags？

可以，但会降低配置可读性。策划看到castingTags立刻知道是读条标签，看到processTags要多想一步。

**反思结论**：保持各模型自己命名的tags字段，语义清晰优先。

**10. 最后一个关键反思：与A文档的一致性**
A文档中Effect有GrantTags节点，有ApplyStatus，有ApplyStatusInline。Ability层新增的recovery本质上就是effect执行完毕后的一个GrantTags。

如果把recovery表达为：

```
postEffect: Effect;  // effect执行完毕后执行的附加效果
```

那策划可以在这里写GrantTags来做后摇，也可以写更复杂的东西。这比硬编码recovery+recoveryTags更灵活，更符合A文档的组合哲学。

但缺点是：失去了"后摇"的框架级语义。编辑器无法识别"这是后摇"从而做特殊展示。

**反思结论**：保留recovery作为快捷字段（语义明确、框架可识别），同时允许在effect中用GrantTags实现更复杂的后摇。recovery是编辑器和框架的"提示"。

让我综合以上反思，形成最终版本。主要改动：
1. 移除allowMovement，改为通过tags表达
2. commit时机内嵌到各模型中，移除独立CommitPolicy
3. 简化InterruptResponse
4. 增加连招组元数据（不改运行时）
5. 更精确的各模型状态转移描述
6. 补充"Ability层与Status层边界判断标准"
7. recovery的定位更明确：语法糖+框架提示

# Ability 施法过程模型（修订版）

## 当前问题

Ability 的结构本质上是：

```
准入检查 → 扣资源 → 执行 Effect → 结束
```

这是一个瞬发模型。但真实游戏中 Ability 的生命周期远比这复杂：

- 法师读条 3 秒被打断，不应该扣蓝
- 战士蓄力越久伤害越高，松手才释放
- 弓箭手引导射击，每 0.3 秒射一箭
- 瞬发技能有 0.2 秒后摇锁操作

当前可以用 Status + Timeline 组合模拟这些过程，但会导致：

1. **语义割裂**：策划认知中是"技能的施法过程"，配置上却变成了"瞬发 Ability 挂载 Status，Status 里用 Timeline 模拟前摇后摇"
2. **框架能力缺失**：打断、commit 时机、施法约束这些通用需求没有框架级支持，每个技能各自用 Tag + Trigger 拼凑
3. **工具无力**：编辑器无法呈现"技能时间轴"，因为框架层不知道 Ability 有阶段概念

---

## 设计原则

1. **瞬发零开销**：`Instant` 与当前行为完全一致
2. **Ability 管过程，Status 管状态**：施法结束后的持续效果（DOT、光环）仍归 Status
3. **Tag 是唯一通用语言**：施法阶段的约束（不可移动、不可施法）全部通过标签表达，不引入 `allowMovement` 之类的布尔开关
4. **最少原语**：只保留不可互相还原的施法原语，复合模式（连招、切换）通过已有机制组合
5. **commit 与模型绑定**：不同模型有不同的合法 commit 选项，不做独立正交

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
    comboGroup: str (nullable);       // 连招组标识，仅用于编辑器/UI 聚合
    // ==========================

    effect: Effect;
}
```

---

## ActivationModel

```cfg
interface ActivationModel {
    struct Instant {}

    struct Cast {
        castTime: FloatValue;
        // 过程标签（如 "State.Casting", "State.Immobile"）
        // 移动约束通过标签表达，移动系统检查 "State.Immobile" 即可
        processTags: list<str> ->gameplaytag;
        cuesDuringCast: list<str> ->cue_key;
        // true = 读条完成时提交，被打断不扣费
        // false = 激活即提交
        commitOnComplete: bool;
    }

    struct Charged {
        minChargeTime: FloatValue;
        maxChargeTime: FloatValue;
        releaseOnMax: bool;
        // 蓄力进度 (0~1) 自动写入 context.instanceState
        chargeProgressVar: str ->var_key;
        processTags: list<str> ->gameplaytag;
        cuesDuringCharge: list<str> ->cue_key;
        // true = 释放时提交（蓄力不足/取消则不扣）
        // false = 激活即提交
        commitOnRelease: bool;
    }

    struct Channel {
        duration: FloatValue;           // -1 = 手动结束
        tickInterval: FloatValue;
        tickEffect: Effect;
        maxTicks: int;                  // -1 = 由 duration/interval 决定
        executeOnStart: bool;
        processTags: list<str> ->gameplaytag;
        cuesDuringChannel: list<str> ->cue_key;
        // true = 首次 tick 时提交
        // false = 激活即提交（默认）
        commitOnFirstTick: bool;
    }
}
```

### 为什么只有四种

每种原语对应一种不可替代的"玩家操作→效果生效"的时间模型：

| 模型 | 玩家操作 | 效果生效时机 |
|---|---|---|
| Instant | 按下 | 立即 |
| Cast | 按下，等待 | 读条完毕 |
| Charged | 按住，松手 | 松手时（进度影响效果） |
| Channel | 按下，持续 | 过程中周期性触发 |

### commit 为什么不独立为字段

每个模型的合法 commit 选项不同：

| 模型 | 选项 | 无效组合 |
|---|---|---|
| Instant | 只有 OnActivate | OnComplete 无意义（无过程） |
| Cast | OnActivate / OnComplete | — |
| Charged | OnActivate / OnRelease | — |
| Channel | OnActivate / OnFirstTick | OnComplete 与引导语义冲突 |

将 commit 内嵌到各模型中，消除了无效组合，策划不会配出"Instant + OnComplete"这种逻辑矛盾。

### 关于 processTags 与移动约束

A 文档的核心哲学是"Tag 是唯一通用语言"。**所有施法期间的行为约束，全部通过标签和 TagRules 表达**，Ability 层不引入 `allowMovement` 等布尔开关。

示例：

```cfg
// 需要站桩施法的技能：
processTags: ["State.Casting", "State.Immobile"]

// 可移动引导的技能：
processTags: ["State.Channeling"]
// 不加 State.Immobile → 移动系统不受限

// 移动系统读取规则：
tag_rules {
    rules: [{
        whenPresent: "State.Immobile";
        blocksAbilities: ["Ability.Type.Movement"];
        description: "不可移动状态下禁止移动类技能";
    }];
}
```

这样做的好处：

- 移动约束、施法约束、攻击约束都走同一套 TagRules
- 新增约束类型（不可旋转、不可跳跃）只需加新 Tag，不改 Ability 表结构
- 项目可自由定义约束粒度

---

## InterruptResponse

**职责边界**：

- **"什么能打断我"** → TagRules（`cancelsAbilities`）
- **"被打断后怎么办"** → InterruptResponse

两者严格分工，InterruptResponse 不重复定义打断条件。

```cfg
struct InterruptResponse {
    // 被打断后的锁定时间（防止立刻重新施法）
    // 框架自动以 GrantTags 的方式挂载 lockoutTag
    lockoutDuration: FloatValue;
    lockoutTag: str ->gameplaytag;   // 如 "State.AbilityLockout"

    // 打断时执行的效果
    onInterruptEffect: Effect (nullable);
}
```

**关于资源退还**：不再提供 `refundCosts` / `refundCooldown` 字段。原因：

- 若 `commitOnComplete = true`（或 `commitOnRelease = true`），被打断时 commit 尚未发生，资源和 CD 本就没有被消耗，无需退还
- 若 commit 已发生（`OnActivate` 或已过 commit 点），被打断理应保留消耗——这是策划选择该 commit 策略时的预期

commit 时机的选择已经隐含了打断时的资源语义，无需再用额外字段重复表达。

---

## RecoveryConfig

后摇是"effect 执行完毕后角色受限的一段时间"。几乎所有动作类游戏都需要，将其提升为 Ability 的一等概念：

```cfg
struct RecoveryConfig {
    duration: FloatValue;                    // 0 = 无后摇
    recoveryTags: list<str> ->gameplaytag;   // 如 ["State.Recovery"]
    cuesDuringRecovery: list<str> ->cue_key;
}
```

**定位澄清**：

`RecoveryConfig` 是"最常见后摇场景"的语法糖，等价于在 `effect` 末尾追加：

```cfg
struct GrantTags {
    grantedTags: ["State.Recovery"];
    duration: struct Const { value: 0.3; };
}
```

它的价值不在功能增量，而在：

1. **框架可识别**：编辑器可以在技能时间轴上显示后摇区间
2. **语义标准化**：策划不需要每个技能手写 GrantTags
3. **动作取消有统一锚点**：TagRules 中对 `State.Recovery` 的规则可一处配置

**超出语法糖范围的复杂后摇**（如分阶段可取消窗口），应回到 `effect` 中用 Timeline 或复合 GrantTags 实现。

---

## 完整执行阶段

所有 ActivationModel 共享同一套阶段框架：

```
CanActivate ──→ Activate ──→ Process ──→ Execute ──→ Recovery ──→ End
                                │
                            Interrupt
                                │
                       InterruptResponse
                                │
                              End
```

### CanActivate（准入检查）

每次 tryActivate 时执行，不改变任何状态：

1. `requiresAll` 条件满足？
2. `costs` 资源充足？（检查但不扣除）
3. `cooldown` 就绪？
4. TagRules 的 `blocksAbilities` 是否拦截当前 `abilityTags`？
5. `maxConcurrent` 未超限？

全部通过才进入 Activate。

### Activate（激活）

- 创建 `AbilityInstance`
- 冻结 `initSnapshot`
- 若 commit 策略为 OnActivate → 立即扣资源、加 CD
- 进入 Process（Instant 跳过此阶段）

### Process（施法过程）

各模型行为不同，后文详述。共同行为：

- 授予 `processTags` → 写入宿主 `TagContainer`
- 播放过程 Cue（`Added` 类型）
- 每帧检测 TagRules 打断条件

### Execute（效果执行）

- 若 commit 尚未发生 → 先 commit（扣资源、加 CD）
- 执行 `ability.effect`
- 广播 `Ability_Executed` 事件

### Recovery（后摇）

- 移除 processTags
- 授予 `recoveryTags`
- 播放后摇 Cue
- 倒计时结束后进入 End

### End（结束）

- 移除所有阶段标签
- 移除所有阶段 Cue
- 广播 `Ability_Completed` 事件
- 销毁 AbilityInstance

### Interrupt（打断，任意阶段均可触发）

- 若在 Process 阶段且尚未 commit → 不消耗资源，不进入 CD
- 若已 commit → 保留消耗（已消费不退）
- 移除所有阶段标签和 Cue
- 执行 `interruptResponse.onInterruptEffect`
- 挂载 `lockoutTag`，持续 `lockoutDuration`
- 广播 `Ability_Interrupted` 事件
- 销毁 AbilityInstance

---

## 各模型状态转移

### Instant

```
activate
    → commit
    → execute(ability.effect)
    → recovery（如配置）
    → end
```

全部在单帧内完成（recovery 除外）。没有 Process 阶段，没有可被打断的窗口（recovery 期间仍可被打断，但 effect 已执行完毕）。

### Cast

```
activate
    → [commitOnComplete=false ? commit : skip]
    → process: 挂 processTags, 每帧累加 phaseElapsed
        │
        ├── phaseElapsed >= castTime
        │       → [commitOnComplete=true ? commit : skip]
        │       → execute(ability.effect)
        │       → recovery → end
        │
        └── interrupt()
                → InterruptResponse → end
```

### Charged

```
activate
    → [commitOnRelease=false ? commit : skip]
    → process: 挂 processTags, 每帧更新 chargeProgress = clamp(elapsed/maxTime, 0, 1)
        │
        ├── release() 且 elapsed >= minChargeTime
        │       → [commitOnRelease=true ? commit : skip]
        │       → execute(ability.effect)  // chargeProgress 可在 Effect 中通过 ContextVar 读取
        │       → recovery → end
        │
        ├── release() 且 elapsed < minChargeTime
        │       → cancel → end（未达最低要求，不 commit，不执行 effect）
        │
        ├── elapsed >= maxChargeTime 且 releaseOnMax=true
        │       → 等同 release()，chargeProgress = 1.0
        │
        └── interrupt()
                → InterruptResponse → end
```

### Channel

```
activate
    → [commitOnFirstTick=false ? commit : skip]
    → process: 挂 processTags
        │   [executeOnStart=true ? 立即执行一次 tickEffect : skip]
        │   每 tickInterval:
        │       [commitOnFirstTick=true 且首次 ? commit : skip]
        │       执行 tickEffect
        │       tickCount++
        │
        ├── tickCount >= maxTicks 或 phaseElapsed >= duration
        │       → execute(ability.effect)  // 引导结束的收尾效果，可为空
        │       → recovery → end
        │
        ├── cancel()（玩家主动停止引导）
        │       → execute(ability.effect) 或 skip（项目决定）
        │       → recovery → end
        │
        └── interrupt()
                → InterruptResponse → end
```

**tickEffect 与 ability.effect 的关系**：

- `tickEffect`：引导过程中每次心跳执行（如射出一支箭）
- `ability.effect`：引导正常结束时执行（如最后一击爆炸），可为空

两者职责正交，不存在替代关系。

---

## 打断判定流程

打断判定**完全复用 TagRules**，不引入任何 Ability 专用机制：

```
1. 施法者进入 Cast 阶段
   → processTags 写入 TagContainer: ["State.Casting", "State.Casting.Spell", "State.Immobile"]

2. 外部事件：施法者被眩晕
   → 获得 "State.Debuff.Control.Stun"

3. TagRules 自动检测：
   whenPresent: "State.Debuff.Control.Stun"
   cancelsAbilities: ["Ability.Type"]
   → 命中当前技能的 "Ability.Type.Spell"

4. 系统调用 AbilityInstance.interrupt()

5. InterruptResponse 执行：
   ├── 挂载 lockoutTag（如 "State.AbilityLockout"）
   ├── 执行 onInterruptEffect
   └── 销毁实例
```

**动作取消**也走同一条路径：

```cfg
// 翻滚技能可以在后摇中激活
tag_rules {
    rules: [{
        whenPresent: "State.Recovery";
        blocksAbilities: ["Ability.Type.Spell", "Ability.Type.Melee"];
        // 不 block "Ability.Type.Dodge" → 翻滚可以打断后摇
    }];
}

// 翻滚技能激活时，通过 RemoveStatusByTag 清理后摇状态
// 或更简单地：翻滚技能自身的 processTags 触发 TagRules 的 purgesTags
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

连招的核心是"多个阶段按条件链接"，每段的消耗、伤害、打断容忍度往往不同。因此每段应是独立 Ability，通过窗口标签串联：

```
[疾风·壹]                [疾风·贰]                [疾风·终]
 Instant                  Instant                  Instant
 effect: Damage           effect: Damage           effect: Damage
         + GrantTags              + GrantTags
         "ComboWindow.2"         "ComboWindow.3"
         duration: 0.6s          duration: 0.7s
 recovery: 0.3s           recovery: 0.4s           recovery: 0.6s
 CD: 5s                   requiresAll:             requiresAll:
                           HasTag ComboWindow.2      HasTag ComboWindow.3
                           CD: 0                     CD: 0
```

这比将 Combo 作为 ActivationModel 原语更灵活：

- 每段独立配 cost / cooldown / interruptResponse / recovery
- TagRules 可独立治理每一段
- 编辑器用普通 Ability 编辑器即可

为了工具和 UI 的可见性，提供 `comboGroup` 元数据字段：

```cfg
ability {
    id: 2010; name: "疾风·壹";
    comboGroup: "ComboGroup.Storm";  // 编辑器据此聚合显示
    ...
}
ability {
    id: 2011; name: "疾风·贰";
    comboGroup: "ComboGroup.Storm";
    ...
}
```

`comboGroup` 仅用于：
- 编辑器中连招技能的分组展示
- UI 技能栏的连招状态显示（项目实现层读取）
- 不参与任何运行时逻辑

**连招 CD 策略**由策划通过 Cooldown 标签自然表达：

```cfg
// 方案 A：起手段 CD 控制整个连招节奏
ability { id: 2010; cooldown: 5.0; ... }  // 只有第一段有 CD
ability { id: 2011; cooldown: 0.0; ... }  // 后续段无独立 CD

// 方案 B：整套完成后才进入 CD（利用 Cooldown 标签）
ability { id: 2012; // 终结段
    effect: struct Sequence { effects: [
        struct Damage { ... },
        // 完成终结段后，给施法者挂 Cooldown 标签
        struct ApplyStatusInline {
            core: {
                grantedTags: ["Cooldown.Combo.Storm"];
                duration: struct Const { value: 5.0; };
            };
        }
    ]};
}
ability { id: 2010; // 起手段
    requiresAll: [
        struct Not { condition: struct HasTags {
            source: struct ContextInstigator {};
            query: { requireAll: ["Cooldown.Combo.Storm"]; };
        }; }
    ];
    cooldown: 0.0;  // 自身无 CD，由 Cooldown 标签控制
    ...
}
```

### 切换型

切换型（如持盾、战斗姿态）本质是两个互斥 Ability + 一个持续 Status：

```cfg
ability {
    id: 3001; name: "举盾";
    abilityTags: ["Ability.Type.Toggle"];
    requiresAll: [
        struct Not { condition: struct HasTags {
            source: struct ContextInstigator {};
            query: { requireAll: ["State.Buff.ShieldStance"]; };
        }; }
    ];
    activation: struct Instant {};
    effect: struct ApplyStatus { statusId: 5010; };
    // Status 5010 内含 Periodic 扣耐、StatModifier 加防、grantedTags 等
}

ability {
    id: 3002; name: "收盾";
    requiresAll: [
        struct HasTags {
            source: struct ContextInstigator {};
            query: { requireAll: ["State.Buff.ShieldStance"]; };
        }
    ];
    activation: struct Instant {};
    effect: struct RemoveStatusByTag {
        query: { requireAll: ["State.Buff.ShieldStance"]; };
        matchGrantedTags: true;
    };
}
```

项目层如果需要"同一按键切换"，由输入系统根据当前 Tag 路由到不同 Ability ID——这是引擎层职责，不是配置层职责。

---

## 与 A 文档其他系统的交互

### Ability vs Status 职责边界

```
Ability（施法过程）              Status（持续状态）
┌─────────────────────┐       ┌─────────────────────┐
│ 控制"效果如何产生"    │       │ 控制"效果如何持续"    │
│ · 前摇 / 蓄力 / 引导 │──→    │ · DOT / HOT          │
│ · commit 时机        │ Apply │ · 光环                │
│ · 打断响应           │Status │ · 被动触发            │
│ · 后摇               │       │ · 属性修饰            │
└─────────────────────┘       └─────────────────────┘
```

**判断标准**：

- 与"玩家操作→效果生效"这段时间有关 → **Ability**
- 效果生效后的持续存在 → **Status**
- 如果存疑，问自己：**"这段逻辑被打断时应该怎么处理？"**。如果答案涉及资源退还、CD 重置 → 它属于 Ability 层

### 施法标签与 TagRules

施法过程中的标签（`processTags` / `recoveryTags` / `lockoutTag`）全部写入施法者的 `TagContainer`，与 Status 的 `grantedTags` 完全共用同一个容器。TagRules 的 block / cancel / immune / purge 规则自然覆盖。

### Effect 层无变化

Effect 保持无状态原子指令集，**不因 Ability 模型增强而新增任何节点**。各模型的效果执行点：

| 模型 | ability.effect 执行时机 | 可额外配置的效果 |
|---|---|---|
| Instant | activate 后立即 | — |
| Cast | 读条完成时 | — |
| Charged | 松手时（chargeProgress 已在 instanceState 中） | — |
| Channel | 引导正常结束时 | tickEffect（过程中每次 tick） |

### 生命周期事件

施法过程的关键时刻通过标准 EventBus 广播，供被动技能和成就系统监听：

```cfg
// 建议在 event_definition 中注册

// Payload 约定：instigator = target = 施法者自身
// extras 中携带 "Var_AbilityId" (int)

"Ability_Activated"       // 通过 CanActivate，进入 Activate
"Ability_Committed"       // 资源已扣除，CD 已开始
"Ability_Executed"        // ability.effect 已执行
"Ability_Completed"       // 正常结束（含后摇结束）
"Ability_Interrupted"     // 被打断
"Ability_Cancelled"       // 主动取消（蓄力未达标、手动停止引导）
```

应用示例：

```cfg
// 被动：被打断后获得短暂韧性
status {
    id: 7002; name: "不屈意志";
    behaviors: [
        struct Trigger {
            listenEvent: "Ability_Interrupted";
            effect: struct GrantTags {
                grantedTags: ["State.Buff.Tenacity"];
                duration: struct Const { value: 2.0; };
            };
            maxTriggers: -1;
            cooldown: 10.0;
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

    boolean tryActivate(int abilityId);
    void tick(float dt);
}

class AbilityInstance implements IPendingKill {
    Ability config;
    Context context;
    Actor owner;

    int phase;             // 阶段索引（Activate=0, Process=1, Execute=2, Recovery=3）
    float phaseElapsed;
    boolean committed;
    boolean pendingKill;

    // 由 AbilityComponent.tick() 驱动
    void tick(float dt);

    // 外部调用
    void release();        // 松手（Charged）
    void cancel();         // 主动取消
    void interrupt();      // 由 TagRules 驱动
}
```

---

## 示例

### 火球术（Cast + commitOnComplete）

```cfg
ability {
    id: 1001; name: "火球术";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire"];
    costs: [{ stat: "MP_Current"; value: struct Const { value: 30.0; }; }];
    cooldown: struct Const { value: 8.0; };

    activation: struct Cast {
        castTime: struct Const { value: 2.0; };
        processTags: ["State.Casting", "State.Casting.Spell", "State.Immobile"];
        cuesDuringCast: ["Cast.Fireball"];
        commitOnComplete: true;
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.5; };
        lockoutTag: "State.AbilityLockout";
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
        moveInfo: { ... };
        cuesWhileActive: ["Projectile.Fireball"];
        dieInfo: [{ ... }];
    };
}
```

### 蓄力重击（Charged + commitOnRelease）

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
        processTags: ["State.Charging", "State.Immobile"];
        cuesDuringCharge: ["Charge.HeavyStrike"];
        commitOnRelease: true;
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.3; };
        lockoutTag: "State.AbilityLockout";
        onInterruptEffect: null;
    };

    recovery: {
        duration: struct Const { value: 0.5; };
        recoveryTags: ["State.Recovery", "State.Recovery.Heavy"];
        cuesDuringRecovery: [];
    };

    // 伤害 = 50 + chargeProgress * 150 (范围 50~200)
    effect: struct WithTargets {
        targets: { shape: struct Sector {
            center: struct ContextInstigator {};
            facingOf: struct ContextInstigator {};
            radius: struct Const { value: 3.0; };
            angle: struct Const { value: 90.0; };
        }; relationTo: struct ContextInstigator {};
           allowedRelations: [Hostile]; maxCount: 5; sort: Nearest; };
        effect: struct Damage {
            damageTags: ["Damage.AttackType.Melee"];
            baseDamage: struct Math {
                op: Add;
                a: struct Const { value: 50.0; };
                b: struct Math {
                    op: Mul;
                    a: struct ContextVar { varKey: "Var_ChargeProgress"; };
                    b: struct Const { value: 150.0; };
                };
            };
            cuesOnHit: ["Hit.HeavyStrike"];
        };
    };
}
```

### 弹幕射击（Channel + 正常结束爆炸）

```cfg
ability {
    id: 2002; name: "弹幕射击";
    abilityTags: ["Ability.Type.Ranged", "Ability.Type.Channel"];
    costs: [{ stat: "MP_Current"; value: struct Const { value: 40.0; }; }];
    cooldown: struct Const { value: 12.0; };

    activation: struct Channel {
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
        processTags: ["State.Channeling"];  // 不含 State.Immobile → 可移动引导
        cuesDuringChannel: ["Channel.Barrage"];
        commitOnFirstTick: false;   // 激活即扣费
    };

    interruptResponse: {
        lockoutDuration: struct Const { value: 0.0; };
        lockoutTag: "State.AbilityLockout";
        onInterruptEffect: null;
    };

    recovery: { duration: struct Const { value: 0.0; }; recoveryTags: []; };

    // 引导正常结束时的收尾爆炸（可选）
    effect: struct WithTargets {
        targets: { shape: struct Sphere {
            center: struct ContextInstigator {};
            radius: struct Const { value: 5.0; };
        }; ... };
        effect: struct Damage {
            damageTags: ["Damage.Type.AoE"];
            baseDamage: struct Const { value: 60.0; };
            cuesOnHit: ["Hit.Barrage.Finale"];
        };
    };
}
```

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

裁剪方式：从 `ActivationModel` 的 interface 中移除不需要的变体，`AbilityInstance` 中对应的 tick 分支不实现。保留的变体不影响已有配置的兼容性。