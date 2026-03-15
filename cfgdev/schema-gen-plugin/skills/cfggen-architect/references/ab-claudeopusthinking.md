

# 能力系统设计评审

## 总体评价

这是一份成熟度很高的技能系统设计文档。核心架构——**Ability(入口) / Effect(瞬时指令) / Status(持续状态)** 的三元正交、两阶段 Pre/Post 事件管线、层级 Tag 驱动、DamageLayer 多层结算——都体现了对 GAS 的深入理解和本土化改造。数据驱动程度高，耦合度低，方向完全正确。

以下按严重程度分级列出问题。

---

## 一、关键设计缺口（会阻塞核心玩法实现）

### 1. 缺少时序控制节点——Effect 无法表达"时间线"

这是**最严重的缺口**。文档将 Effect 定义为"瞬间执行、无状态的指令流"，但现实中绝大多数技能都有时序需求：

```
蓄力斩：前摇 0.8s → 判定伤害 → 后摇 0.5s（可被闪避取消）
三段斩：挥砍1 → 等待 0.3s → 挥砍2 → 等待 0.3s → 挥砍3
引导法术：每 0.5s 造成一次伤害，持续 3s，期间可被打断
```

当前 Effect 树中没有 `Wait`、`WaitForAnimEvent`、`WaitForInput` 等时序节点。`Periodic` 藏在 Status/Behavior 里，但 Ability 的主 effect 字段执行完就结束了，无法驱动多阶段技能释放流程。

GAS 通过 **AbilityTask**（异步子任务）解决此问题。建议在 Effect 层补充：

```cfg
interface Effect {
    // ... 现有节点 ...

    // 延迟：等待一段时间后继续执行后续 Effect
    struct Delay {
        duration: FloatValue;
        effect: Effect;
    }

    // 等待动画事件（如攻击动画播到"伤害判定帧"时触发）
    struct WaitAnimNotify {
        notifyName: str;    // 动画通知名
        timeout: float;     // 超时保护
        effect: Effect;
    }

    // 引导型：持续执行，期间可被打断
    struct Channel {
        duration: FloatValue;
        tickInterval: FloatValue;
        effectOnTick: Effect;
        effectOnComplete: Effect;
        effectOnInterrupt: Effect;
        // 引导期间给自身挂的 Tag（如 "State.Channeling"）
        grantedTagsWhileActive: list<str> ->gameplaytag;
    }
}
```

一旦引入时序节点，Effect 就不再是纯粹"无状态"的了——至少 `Delay`/`Channel` 需要一个运行时句柄来管理取消和生命周期。这意味着需要为 Ability 引入一个 **ActiveAbilityInstance** 概念来承载执行中的时序状态。

### 2. SpawnObj 引用了未定义的结构体

`ObjMoveInfo` 和 `ObjDieInfo` 在文档中被引用但从未定义。弹道系统（抛物线、追踪、弹射）和碰撞判定是投射物类技能的核心，缺失这部分配置等于弹道类技能无法实现。

```cfg
// 建议补充的定义
interface ObjMoveInfo {
    struct Projectile {
        speed: FloatValue;
        trajectory: Trajectory;
        pierceCount: int;       // 穿透次数，-1=无限
        hitRadius: float;       // 碰撞半径
    }
    struct Static {}            // 原地不动（法阵）
    struct FollowTarget {       // 跟随某目标
        target: TargetSelector;
        speed: FloatValue;
    }
}

enum Trajectory {
    Linear;         // 直线
    Parabolic;      // 抛物线
    Homing;         // 追踪
}

struct ObjDieInfo {
    condition: ObjDieCondition;
    effectOnDie: Effect;        // 消亡时执行
    cuesOnDie: list<str> ->gameplaytag;
}

interface ObjDieCondition {
    struct OnHit {}             // 碰到目标后
    struct OnExpire {}          // 持续时间结束
    struct OnMaxPierce {}       // 穿透次数耗尽
}
```

### 3. 缺少 RemoveStatus / PlayCue 等基础 Effect 节点

当前 Effect 能 `ApplyStatus` 但不能**主动移除**一个指定状态。`cancelTags` 是基于 Tag 的宽泛清除，无法精确移除"ID=5001的盾墙"而不影响其他同 Tag 状态。

同样，缺少独立的 `PlayCue` 节点。当前 Cue 只能挂在 Damage/Heal/Status 上，但"技能释放瞬间播一个施法特效"这种与伤害/状态无关的纯表现需求无处安放。

```cfg
interface Effect {
    // 按 ID 精确移除状态
    struct RemoveStatus {
        statusId: int ->status;
        stacksToRemove: int; // -1 = 全部移除
    }
    // 按 Tag 模糊移除（已有 cancelTags 覆盖，但独立出来更灵活）
    struct RemoveStatusByTag {
        tag: str ->gameplaytag;
    }
    // 独立的 Cue 播放
    struct PlayCue {
        cueTag: str ->gameplaytag;
        magnitude: FloatValue;
    }
}
```

---

## 二、结构性问题（不影响原型但会影响中后期）

### 4. Pre 事件 Modifier 的执行顺序未定义

当两个 Trigger 同时监听 `Event.Combat.Damage.Take.Pre` 并都向 Payload 注入 Modifier 时，执行顺序取决于 `SafeList` 中的注册顺序（本质上是 Status 的挂载时序），这是**隐式的、不确定的**。

对于 Add/Multiply 这类可交换运算影响不大，但如果未来出现顺序敏感的逻辑，会成为隐患。

**建议**：为 Trigger 增加 `priority: int` 字段，EventBus dispatch 时按优先级排序。

### 5. Condition 系统表达力不足

当前 Condition 只有 `PayloadMagnitudeGte` 和 `PayloadVarGte`，缺少最基本的数值比较能力。"目标血量低于 30% 时触发斩杀"这类常见需求无法直接表达。

**建议**：引入通用的 `FloatCompare`：

```cfg
interface Condition {
    // ...现有...
    struct FloatCompare {
        left: FloatValue;
        op: CompareOp;
        right: FloatValue;
    }
}

enum CompareOp {
    Gt; Gte; Lt; Lte; Eq; Neq;
}
```

这样 `PayloadMagnitudeGte` 和 `PayloadVarGte` 变成 `FloatCompare` 的语法糖，可以考虑是否保留。用 `FloatCompare` + `StatValue` 即可表达任意属性比较。

### 6. shared_effect 不支持参数化——限制复用上限

当前 `EffectRef` 是硬引用，无法传参。这意味着"造成 X 点火焰伤害"如果 X 不同，就要复制一份 shared_effect。

**建议**：为 `EffectRef` 增加参数绑定：

```cfg
struct EffectRef {
    sharedEffectId: int ->shared_effect;
    // 调用时注入的参数，写入 localScope
    args: list<struct { argTag: str ->gameplaytag; value: FloatValue; }>;
}
```

shared_effect 内部通过 `ContextVar` 读取这些参数。这类似函数调用传参。

### 7. Status 缺少显式的 onApply / onRemove 钩子

当前 StatusCore 有 `grantedTags`/`cancelTags`/`blockTags` 做挂载/移除时的 Tag 操作，`Periodic` 做周期逻辑，`Trigger` 做事件响应。但缺少**挂载瞬间执行一次**和**移除瞬间执行一次**的 Effect 钩子。

例如："护盾挂上时播放特效+语音"可以用 Cue 覆盖，但"护盾被打破时给敌人施加一个反弹状态"需要 onRemove 钩子。

```cfg
struct StatusCore {
    // ...现有...
    effectOnApply: Effect;   // 挂载时执行一次
    effectOnRemove: Effect;  // 移除时执行一次（无论是超时、驱散还是死亡）
    effectOnExpire: Effect;  // 仅自然到期时执行
}
```

### 8. StackingPolicy 中 Standard 缺少满层回调

Standard 模式 `maxStacks: 3` 的毒素叠满时触发"毒爆"是极常见的设计模式，但当前无法配置。

```cfg
struct Standard {
    maxStacks: int;
    onMaxStacksReached: Effect;  // 达到满层时触发
    clearOnMaxTrigger: bool;     // 触发后是否清空层数
}
```

### 9. global_tag_rules 与 StatusCore 的职责存在重叠

`StatusCore.blockTags` 和 `StatusCore.cancelTags` 做的事情与 `global_tag_rules` 中的 `immunes` 和 `cancels` 高度重叠。当两者同时存在且规则冲突时，优先级不明确。

**建议**：明确优先级链路并在文档中声明，例如：
> `global_tag_rules` 是全局物理法则（先判），`StatusCore.blockTags` 是个体级免疫（后判），两者取**并集**。

或者考虑是否将 `StatusCore.blockTags/cancelTags` 收归 `global_tag_rules`，Status 只保留 `grantedTags`，简化心智模型。

---

## 三、细节改进建议

### 10. GameplayTag 表设计

`[value]` 作为 unique index 但需要策划手动分配 int 值，容易冲突且维护成本高。建议：
- 要么由工具链根据 `name` 自动生成 hash 值
- 要么直接用 `name` 的字符串 hash 作为运行时 ID，配置层只写字符串

### 11. Stat 联动的触发时机不明确

`StatClampMode.MaintainPercent` 要求在 MaxHP 变化时同步调整 CurrentHP。但文档没有说明这个联动是在 `Stat.recalculate()` 时还是在 `StatModifier` 增删时触发。如果一个 Buff 同时修改了 MaxHP 和 CurrentHP，两者的更新顺序会影响最终结果。

**建议**：明确定义 stat 依赖图的拓扑排序规则或标注更新时机。

### 12. EventBus 缺少事件消费/拦截机制

当前所有 Trigger 都会收到事件，无法"消费"它以阻止后续 Trigger 响应。例如"完美格挡"应当消费伤害事件，阻止其他减伤 Buff 再处理（因为伤害已经被完全挡住了）。

可以在 Payload 中增加 `consumed: bool` 标记，或通过 `ModifyPayloadMagnitude(Override, 0)` 变相实现。

### 13. Ability 缺少"技能实例策略"

同一个 Ability 连续按两次会怎样？当前没有定义是：
- **不可重复激活**（第二次直接无视）
- **重新激活**（取消第一次，重新开始）
- **可叠加实例**（两个独立执行）

GAS 有 `InstancingPolicy`，建议补充。

### 14. 没有属性变化事件

当 Stat 的 currentValue 变化时，没有自动广播事件的机制。但 UI 血条、被动触发（"血量低于 30% 时激活"）等都依赖属性变化通知。

**建议**：在 `stat_definition` 中增加 `onChangeEventTag` 或在引擎层统一约定属性变化时广播 `Event.Stat.Changed.{StatTag}`。

### 15. TargetScan 缺少射线检测

作为 hitscan 武器（如狙击枪）或激光的基础，建议补充：

```cfg
struct Ray {
    origin: TargetSelector;
    direction: TargetSelector;
    length: FloatValue;
    filter: TargetFilter;
}
```

---

## 四、总结

| 类别 | 数量 | 关键项 |
|---|---|---|
| 关键缺口 | 3 | 时序控制、SpawnObj 未定义、基础 Effect 缺失 |
| 结构问题 | 6 | Modifier 排序、Condition 表达力、参数化复用、生命周期钩子等 |
| 细节改进 | 6 | Tag 生成、Stat 联动、事件消费、实例策略等 |

**最优先处理**的是**时序控制节点**——没有它，连一个带前后摇的普通攻击都无法纯数据驱动地配置出来，整套 Effect 系统会退化为只能描述"瞬发技能"的子集。其次是补全 SpawnObj 的弹道定义和基础 Effect 节点。结构性问题建议在原型验证后、正式生产前逐步修正。