# 能力系统设计评审

## 总体评价

这是一份**高质量的工业级技能系统设计文档**。架构思路清晰，对 GAS 的精髓提炼到位，数据驱动的理念贯彻得很彻底。以下从多个维度给出具体的评价与改进建议。

---

## 一、架构层面的优点

1. **三层正交分离（Ability / Status / Effect）** 做得非常干净，职责边界清晰
2. **两阶段事件提交（Pre/Post）** 是处理伤害修饰的正确范式
3. **Tag 层级展开 + 引用计数** 的运行时方案兼顾了查询性能和语义完整性
4. **SafeList 双缓冲** 解决了游戏系统中最常见的迭代期修改问题
5. **Cue 的生命周期自动推导** 是一个很好的设计决策，减少了策划的心智负担

---

## 二、关键设计缺陷与改进建议

### 2.1 伤害管线缺少关键环节

**问题**：`Damage` Effect 描述了 `Pre → 计算 → 扣血 → Post` 的流程，但缺少几个关键阶段：

- **没有暴击/闪避/格挡的判定阶段**。当前设计中这些核心战斗机制无处安放。
- **没有伤害类型的减免映射**（如火抗、物抗）。`damageTags` 标记了伤害属性，但没有配置化的方式将其与抗性属性关联。

**建议**：在 `global_combat_settings` 中增加伤害结算管线的阶段定义：

```cfg
// 在 global_combat_settings 中增加
struct DamageFormula {
    // 抗性映射：伤害Tag -> 抗性Stat -> 减伤公式
    resistMappings: list<ResistMapping>;
    // 结算阶段定义
    phases: list<DamagePhase>;
}

struct ResistMapping {
    damageTag: str ->gameplaytag;    // "Damage.Element.Fire"
    resistStat: str ->stat_definition; // "Stat.Resist.Fire"
    formula: FloatValue; // 如: 1 - resist / (resist + 100)
}

enum DamagePhase {
    CritRoll;      // 暴击判定
    DodgeRoll;     // 闪避判定  
    BlockRoll;     // 格挡判定
    ResistCalc;    // 抗性计算
    PreModifier;   // Pre 事件广播
    FinalClamp;    // 最终值钳制
}
```

或者你的意图是让暴击、闪避全部通过 `Trigger + ModifyPayload` 实现？如果是，**需要在文档中明确说明这一设计意图**，并给出示例，否则实现者会困惑。

---

### 2.2 Effect 缺少异步/时序控制能力

**问题**：Effect 被定义为"瞬间执行、无状态的指令流"，但 `Sequence` 只是顺序执行，没有时序控制。实际游戏中大量技能需要：

- 延迟执行（蓄力后释放）
- 等待动画事件（挥刀到命中帧再出伤害）
- 分段多次执行（三段斩的每一击）

**当前的 `Repeat` 是瞬间循环，不是分时循环。**

**建议**：要么承认 Effect 层不处理时序（由 Ability 的外部驱动层如行为树/时间轴控制），要么增加最小化的时序节点：

```cfg
// 方案A：增加时序节点
interface Effect {
    // ...existing...
    struct Delay {
        duration: FloatValue;
        effect: Effect;
    }
    struct WaitEvent {
        eventTag: str ->event_definition;
        timeout: FloatValue;
        effect: Effect;
        timeoutEffect: Effect;
    }
}
```

```cfg
// 方案B：在 Ability 中引入 Phase/Timeline 概念（推荐）
struct AbilityPhase {
    animMontage: str;           // 动画资源引用
    commitPoints: list<CommitPoint>; // 动画事件触发点
}

struct CommitPoint {
    notifyName: str;  // 动画通知名
    effect: Effect;   // 到达该帧时执行的 Effect
}
```

**方案B更符合你的"逻辑与表现隔离"原则**——动画驱动时序，Effect 保持瞬时无状态。但需要在 Ability 层补充时间轴结构。

---

### 2.3 SpawnObj 设计过于粗糙

**问题**：`SpawnObj` 是整个系统中**最复杂的 Effect 类型**，但定义最简略：

- `moveInfo: ObjMoveInfo` 和 `dieInfo: list<ObjDieInfo>` 没有给出定义
- 生成物本身是否是一个 Actor？是否有自己的 StatusComponent 和 EventBus？
- 碰撞回调（如子弹命中）如何与 Effect 系统衔接？
- 穿透次数、弹射逻辑等常见需求无处配置

**建议**：至少补全核心接口定义：

```cfg
interface ObjMoveInfo {
    struct Projectile {
        speed: FloatValue;
        maxRange: FloatValue;
        gravityScale: float;
        homingTarget: TargetSelector (nullable);
        homingStrength: float;
    }
    struct Static {
        // 法阵/陷阱
    }
    struct FollowTarget {
        target: TargetSelector;
        offset: Vec3;
    }
}

struct ObjDieInfo {
    dieCondition: ObjDieCondition;
    effectOnDie: Effect;
    cueOnDie: list<str> ->gameplaytag;
}

interface ObjDieCondition {
    struct Timeout {} // duration 到期
    struct MaxHits { count: int; } // 穿透次数耗尽
    struct OnFirstHit {} // 碰撞即消
    struct ExternalEvent { eventTag: str ->event_definition; }
}

// 碰撞发生时的回调
struct ObjCollisionPolicy {
    filter: TargetFilter;
    effectOnHit: Effect; // 对被碰撞者执行
    hitInterval: float;  // 同一目标再次生效的冷却 (防止一帧多次命中)
    pierceCount: int;    // -1 = 无限穿透
}
```

---

### 2.4 StackingPolicy 与 StatusInstance 的交互不完整

**问题**：定义了5种堆叠策略，但缺少以下关键信息：

- **不同来源（Instigator）的堆叠隔离**？例如：A 和 B 同时对 C 挂中毒，是各自独立计层，还是共享层数？
- **层数变化时的回调**？如"叠满3层引爆"这种常见机制，当前无法优雅表达
- **Standard 策略中，层数对 Behavior 的影响**？StatModifier 的 value 是否自动乘以层数？还是需要策划用 `FloatValue` 手动引用层数？

**建议**：

```cfg
interface StackingPolicy {
    struct Standard {
        maxStacks: int;
        stackCountSource: StackSource; // 新增
        stackApplicationBehavior: StackBehavior; // 新增
    }
    // ...其他不变
}

enum StackSource {
    AnySource;        // 所有来源共享层数
    PerInstigator;    // 每个施加者独立计层
}

enum StackBehavior {
    RefreshDuration;       // 新层刷新总时长
    AddDuration;           // 新层追加时长
    NoDurationChange;      // 层数增加但时长不变
}
```

同时在 `FloatValue` 中增加：

```cfg
interface FloatValue {
    // ...existing...
    struct StackCount {} // 读取当前 StatusInstance 的层数
}
```

并在 `Behavior` 中增加层数阈值触发器：

```cfg
interface Behavior {
    // ...existing...
    struct OnStackThreshold {
        threshold: int;
        comparison: CompareOp; // Gte, Eq, etc.
        effect: Effect;
        consumeStacks: int; // 触发后消耗多少层 (0=不消耗, -1=全部消耗)
    }
}
```

---

### 2.5 Condition 缺少对 Status 层数和持续时间的查询

**问题**：`Condition` 只有 `HasTags` 和 Payload 相关判断，无法查询：

- 目标身上某个 Status 的当前层数
- 目标某个 Stat 的当前值与阈值比较
- 上下文变量的比较

**建议**：

```cfg
interface Condition {
    // ...existing...
    struct CompareFloat {
        left: FloatValue;
        op: CompareOp;
        right: FloatValue;
    }
    struct HasStatus {
        source: TargetSelector;
        statusId: int ->status;
        minStacks: int;
    }
}

enum CompareOp {
    Gt; Gte; Lt; Lte; Eq; Neq;
}
```

`CompareFloat` 配合 `FloatValue.StatValue` 可以覆盖几乎所有数值判断场景，比分散的 `PayloadMagnitudeGte`、`PayloadVarGte` 更通用。后者可以考虑**废弃**，全部统一到 `CompareFloat`。

---

### 2.6 事件系统的监听粒度问题

**问题**：`Trigger` 的 `listenEventTag` 是精确匹配还是前缀匹配？

- 如果精确匹配：监听 `Event.Combat.Damage` 无法捕获 `Event.Combat.Damage.Take.Pre`
- 如果前缀匹配：需要与 TagContainer 的层级展开策略一致

文档没有明确说明。这个决策直接影响：
- 策划能否配置"监听所有伤害事件"这种宽泛触发器
- EventBus 的 dispatch 实现方式（精确查找 vs 遍历父级）

**建议**：明确选择一种策略并在文档中声明。推荐**层级匹配**，与 TagContainer 保持一致——dispatch 时向所有父级 Tag 的监听器也广播：

```java
void dispatch(Event event) {
    int tagId = event.eventTagId;
    // 向当前 Tag 及其所有祖先 Tag 的监听队列广播
    while (tagId != ROOT) {
        SafeList<TriggerInstance> list = listeners.get(tagId);
        if (list != null) { /* iterate and fire */ }
        tagId = getParent(tagId);
    }
}
```

---

### 2.7 死循环防护机制不够健壮

**问题**：Context 中有 `recursionDepth`，但没有说明：

- 阈值是多少？全局配置还是硬编码？
- 触发限制时的行为是静默丢弃还是报错？
- 是按 Effect 链深度计数，还是按事件重入次数计数？

**建议**：在 `global_combat_settings` 中增加：

```cfg
// 在 global_combat_settings 中
maxRecursionDepth: int;          // 如 16
recursionBreachPolicy: BreachPolicy;

enum BreachPolicy {
    SilentDrop;     // 静默丢弃后续效果
    LogAndDrop;     // 记录告警日志并丢弃
    ThrowException; // 开发阶段直接崩溃报错
}
```

---

## 三、数据建模层面的问题

### 3.1 GameplayTag 的 `value` 字段语义模糊

```cfg
table gameplaytag[name] {
    name: str;
    value: int;       // <-- 这个既当主键又当层级引用的 ID？
    ancestors: list<int> ->gameplaytag[value];
    [value];
}
```

**问题**：
- `name` 是表的主键（`[name]`），但 `[value]` 声明了唯一索引
- `ancestors` 引用的是 `value` 而非 `name`
- `value` 看起来是运行时 ID，但策划需不需要手动填？如果自动生成，生成规则是什么？

**建议**：明确 `value` 为自增或哈希 ID，并增加注释说明生成规则。或者改为只用 `name` 作为唯一标识，运行时由引擎内部分配 int ID：

```cfg
table gameplaytag[name] {
    name: str;           // 全局唯一标识，如 "State.Debuff.Stun"
    description: text;
    // value 和 ancestors 由引擎初始化时自动计算，配置表中不暴露
}
```

---

### 3.2 ArgCapture 的 Dynamic 模式语义含糊

```cfg
enum ArgCaptureMode {
    Snapshot; // 捕获挂载瞬间的值
    Dynamic;  // 每次求值时实时获取
}
```

**问题**：Dynamic 模式下，`FloatValue` 的 Context 是**挂载时的 Context** 还是**求值时的 Context**？

- 如果是挂载时的 Context：那 `StatValue { source: ContextTarget }` 的 target 是挂载时的目标，Dynamic 只是重新读其属性
- 如果是求值时的 Context：那 Context 中的 target 可能已经变化

**建议**：在文档中明确声明：

> Dynamic 模式下，FloatValue 始终在 StatusInstance 当前的 Context 环境中求值。由于 StatusInstance 的 Context.target 固定为宿主（Host），source 选择器仍会基于宿主上下文解析。如需引用挂载时的动态实体引用（如"对当时的攻击者持续生效"），应使用 Snapshot 捕获 Actor 引用，而非 Dynamic。

---

### 3.3 Effect.Damage 与 global_combat_settings 的管线对接不清晰

**问题**：

- `Damage` Effect 有 `damageTags`，但没有说明这些 Tag 如何与 `DamageLayer` 交互
- 伤害结算是走 `DamageLayer` 从上到下扣减，但 `Damage` 的 `baseDamage` 计算完毕后，是直接进入扣减管线，还是先经过抗性计算？
- `Heal` 没有类似的管线定义

**建议**：给出完整的伤害结算流程伪代码：

```java
void executeDamage(Damage cfg, Context ctx) {
    // 1. 计算基础伤害
    float raw = FloatValues.evaluate(cfg.baseDamage, ctx, null);
    
    // 2. 构造 Payload
    Payload payload = new Payload(ctx, raw);
    payload.extras.setTags(cfg.damageTags); // 伤害标签写入 extras
    
    // 3. 广播 Pre 事件 → 收集 Modifier
    dispatch(new Event("Event.Combat.Damage.Deal.Pre", payload)); // 攻击方
    dispatch(new Event("Event.Combat.Damage.Take.Pre", payload)); // 防御方
    
    // 4. 结算 Modifier → 得到最终值
    float finalDamage = payload.resolve();
    
    // 5. 走 DamageLayer 管线扣减
    float remaining = finalDamage;
    for (DamageLayer layer : settings.damageLayers) {
        remaining = applyLayer(layer, target, remaining);
        if (remaining <= 0) break;
    }
    
    // 6. 广播 Post 事件
    dispatch(new Event("Event.Combat.Damage.Take.Post", payload));
    
    // 7. 死亡检测
    checkDeath(target);
}
```

---

## 四、可用性与工程化建议

### 4.1 缺少版本化/迁移策略

当 Status 或 Effect 的配置结构发生变更时，已存盘的 StatusInstance（`isPersistent` 的 Stat、离线玩家身上的 Status）如何兼容？需要考虑：

- 配置版本号
- 序列化/反序列化的向前兼容机制

### 4.2 缺少调试/可观测性设计

建议增加：

```cfg
// 全局调试配置
struct DebugSettings {
    logEffectExecution: bool;      // 记录每次 Effect 执行链路
    logPayloadModifiers: bool;     // 记录 Payload 的每次修饰
    logTagChanges: bool;           // 记录 Tag 增删
    logStatChanges: bool;          // 记录属性变化
    breakOnRecursionLimit: bool;   // 递归超限时断点
}
```

### 4.3 缺少编辑器约束元数据

配置表中没有为策划工具提供足够的元信息，如：

- FloatValue 在特定语境下的预期范围（如概率应该 0~1）
- Effect 节点的嵌套深度限制
- 循环引用检测（EffectRef → shared_effect → EffectRef）

---

## 五、优先级总结

| 优先级 | 问题 | 影响 |
|--------|------|------|
| 🔴 **P0** | SpawnObj 定义缺失 | 无法配置任何投射物/法阵 |
| 🔴 **P0** | Effect 缺少时序控制 | 无法实现多段技能、蓄力释放 |
| 🔴 **P0** | 伤害管线流程不完整 | 暴击/闪避/抗性无处安放 |
| 🟡 **P1** | StackingPolicy 交互不完整 | "叠满引爆"等常见机制难以实现 |
| 🟡 **P1** | Condition 表达力不足 | 无法基于属性阈值做条件判断 |
| 🟡 **P1** | 事件监听粒度未明确 | 实现者会做出不一致的决策 |
| 🟢 **P2** | 死循环防护细节 | 可后续补充 |
| 🟢 **P2** | 调试可观测性 | 不影响功能但影响开发效率 |

---

## 六、总评

这份设计在**架构抽象层面**做到了 9/10 的水平——核心理念正确、分层清晰、扩展性强。主要短板在于**实战完整性**：几个关键的战斗机制路径（伤害公式全链路、投射物生命周期、技能时间轴驱动）还没有闭合。建议按 P0 优先级补全这些缺口后，再进入实现阶段。