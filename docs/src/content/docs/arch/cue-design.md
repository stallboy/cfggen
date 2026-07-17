---
title: Cue 表现系统
sidebar:
  order: 22
---

本系统的核心哲学是：以“瘦配置、胖资产”实现意图与资源的彻底解耦；借集中注册表与动态标签完成智能寻址；靠双轨防线（脉冲/状态）实现严格性能剔除；并通过 Ticket 凭证闭环，使逻辑驱动与资产自决的生命周期完全分离。


## 核心哲学与职责边界

本系统基于 **“瘦配置层，胖资产层 (Thin Config, Fat Asset)”** 的核心架构哲学，严格界定逻辑意图与物理视听资源的职责边界，实现两者的彻底解耦。

### 瘦配置层 (Cue Layer)：专注意图表达
逻辑系统与配置表**绝对不关心**表现的实现细节。
- **只提供上下文**：仅输出事件发生的时机、空间锚点 (Anchor)、瞬时强度 (Magnitude) 以及环境标签 (Tags)。
- **只做调度与节流**：负责意图寻址、同源合并、槽位挤占与 UI 数据聚合，确保下发给引擎的指令是安全且收敛的。
- **零物理耦合**：严禁在配置层硬编码资源路径，严禁向下穿透干涉引擎的粒子浓度、动画帧或材质属性。
 
### 胖资产层 (Engine Asset Layer)：绝对黑盒自治
底层游戏引擎的预制体（如 Godot 的 `.tscn` 或 Unity 的 `Prefab`）被视为**绝对黑盒**。资产接收高度浓缩的抽象数据后，拥有完整的**表现自决权与生命周期自治权**：
- **视觉自决**：UI 飘字资产收到 `AggregateStats(Count=5, Sum=500)` 后，由其内部脚本自主决定是仅仅显示文字，还是触发“字体爆裂”动画。
- **生命周期自决**：短促特效发现下发凭证中包含 `Delay > 200ms` 的异步延迟时，可自主决定快进或直接销毁。
- **优化自治**：视锥剔除、音频通道抢占 (Voice Stealing) 以及基于距离的 LOD 降级休眠，均由引擎层自闭环实现，Cue 系统绝不介入干预。


### 架构管线流程图

系统执行一条单向数据流。在逻辑层派发指令后，数据首先经过静态的解析与提取，随后根据指令类型的不同，被精准分流至“双轨制”防线，最终移交底层引擎。

```
【逻辑层】 发起表现请求
 ├── 快路径调用：FireCue ("Hit.Physical", Tags: [Flesh, Crit])
 └── 慢路径调用：AttachCue ("Buff.Poison", Tags: [DOT])
        │
        ▼
【资产解析管线】(纯数据层只读过滤)
 ├─ 阶段一：意图回退寻址 (沿继承链向上，锁定对应 cue_key 注册表)
 └─ 阶段二：Tag 上下文过滤 (抽出满足当前环境的意图，如 vfx_blood, sfx_hit)
        │
        ▼
【双轨决断防线】(意图过滤完成)
 ├─▶ [轨一：Pulse 快路径] (同帧去重 → UI数据节流)
 │      │
 │      ▼
 │   【全局防线 (limit_group)】(仅拦截 Pulse 与 UI，执行 Reject/Stop)
 │      │
 │      └──────────────────────────┐
 │                                 │
 ├─▶ [轨二：State 慢路径]          │
        │                          │
        ▼                          │
     (宿主级：同源融合 → 槽位挤占) │
        │                          │
        └──────────────────────────┤
                                   ▼
【引擎资产层 - Fat Asset】 
 ├─ 自治行为：执行音频通道抢占 与 视锥剔除休眠。
 └─ 参数驱动：自主解析传入参数，驱动内部的粒子浓度、材质覆盖或 RTPC 混音。
 └─ 生命周期终结 (表现结束 / 被 ForceKill / 宿主销毁)
        │
        ▼
【票据归还 (Ticket Return)】
 └─ 胖资产调用 Ticket.Recycler.Return(Handle) 
```

整个流程清晰定义了“逻辑只负责扣扳机，胖资产完全自治”的单向依赖关系。

---

## 对外接口：逻辑层如何触发

### 通信契约：脉冲与状态的核心语义
表现层基于**单向广播**机制运作。逻辑层仅负责在生命周期的关键节点调用 `ICueOps` 接口，严格恪守这个“黑盒”边界，绝不向下干涉具体的视听实现。

#### ICueOps 接口定义
```csharp
public interface ICueOps
{
    // 触发
    void FireCue(DCueKeyPulse cue, in CueContext ctx);
    // 持续挂载，返回token供之后update，detach使用
    int AttachCue(Actor target, IReadOnlyList<DCueKeyState> cues, in CueContext ctx);
    // 刷新
    void UpdateCue(Actor target, int token, in CueParams parameters);
    // 卸载
    void DetachCue(Actor target, int token);
}

public readonly record struct CueContext(
    CueAnchor Target,   // 承受者 / 表现锚点坐标
    Actor Instigator,   // 发起者
    Actor Causer,       // 媒介 (如飞行中的子弹实例)
    
    TagSet ContextTags,// Event的瞬态标签快照 (如 ["Combat.DamageType.Fire"])
    CueParams Parameters
);

public readonly record struct CueAnchor(
    Actor? Actor,
    Vector2 Position,
    Vector2 Direction
);

public readonly struct CueParams {
    public float Magnitude; // 强度数值
    public readonly float Param1;
    public readonly float Param2;
    public readonly int ColorHex;
}
```

所有上下文结构体（`CueContext`, `CueAnchor`, `CueParams`）均设计为值类型，避免装箱与堆分配，确保高频事件分发时的 0‑GC。

通信契约按底层生命周期特征，严格划分为**脉冲（Pulse）**与**状态（State）**两大类：

#### 脉冲型指令 (Pulse)
**核心语义：点火即走，底层自治。**

- **Fire (触发)**：逻辑层派发指令后立刻脱手。系统随即下发操作凭证 `Ticket`（含唯一句柄 `Handle`、资源加载耗时 `Delay` 与回收器 `Recycler`）。此后该实例的物理生命周期完全交由底层胖资产黑盒接管——表现自然结束或被全局防线强杀时，胖资产须通过 `Recycler.Return(Handle)` 主动归还句柄，**严禁私自执行销毁操作**。  
- **异步容忍 (Async Tolerance)**：若请求时目标资产尚未加载完毕，`Provider` 可返回一个轻量的 `IPulseCue` 代理对象。该代理立即截留 `Ticket`，并在资源就绪后实例化真实的底部资产。此时系统会基于加载耗时重新计算 `Delay`，并以新的 `Fire` 调用将 `Ticket` 移交给真实资产；真实资产依据 `Delay` 自主裁决——短促特效可直接丢弃，长特效则播放

> 注：逻辑层从不感知代理与真实资产的存在，且整个过程复用同一套 `IPulseCue` 接口，保证了状态机归零和接口极简。

#### 状态型指令 (State)
**核心语义：与宿主绑定，受控生死。**
* **Attach (装载)**：状态表现的起点。返回token
* **Update (刷新)**：状态演进。当逻辑状态变更（如 Buff 叠层、数值动态衰减）时，系统驱动底层资产进行动态视觉演进。
* **Detach (卸载)**：状态终结。严格遵循**“逻辑终止 ≠ 物理销毁”**的核心契约。系统收到指令解除映射后，通知底层资产执行退出过渡（如音效淡出、粒子停止发射），待表现优雅收尾后再由池回收。

### 外部调用规范

**持续型表现的生命周期必须且仅能与其挂载的 Actor 绑定。** 以下为各逻辑源的标准调用映射：

| Logic Source | 调用方法  | 目标 |
| :--- | :--- | :--- |
| `Effect.FireCue` / `RunPipeline.impactCues`... | FireCue | `target` |
| `Status` 首次挂载 | AttachCue | host | 
| `Status` 层数变化 / 时长刷新 | UpdateCue | host | 
| `Status` 移除 (过期/驱散/死亡) | DetachCue | host | 
| `Ability` 进入前摇/蓄力/引导 | AttachCue | owner | 
| `Ability` 阶段切换/被打断/结束 | DetachCue | owner |


---

## 意图注册与资产寻址

表现系统将“做什么”与“物理资源是什么”彻底分离：**意图层**负责描述行为（权重、合并策略），**物理层**负责描述资产的极限。

### 意图层：Cue Registry

**意图层**：定义“做什么、在哪里做、有多重要”。  
注册表是逻辑意图的集合。逻辑层派发的 `cue_key` 在此处被翻译为具体的“资源请求”。诸如 `visualSlot`（占用哪个频道）、`priority`（竞争权重）以及 `mergePolicy`（数据处理规则）等字段属于注册表，因为它们描述的是**资产在特定逻辑上下文中的应用行为**，而非资产本身的物理属性。这确保了同一物理资产在不同技能或状态下可以拥有完全不同的挤占逻辑和聚合策略。

#### 脉冲型 (Pulse)
**适用于：一次性事件反馈（如开火、受击、爆炸、飘字）。**
脉冲型表现由系统倒计时自动回收。允许通过 `role` 字段进行跨实体空间路由（例如：Instigator 吸血，但特效飞向 Target）。

```cfg
// 脉冲型：适用于：一次性事件反馈（如开火、受击、爆炸、飘字）。
// 允许通过 role 跨实体表现 (如：在 Instigator 身上播放吸血流轨迹)。
table cue_registry_pulse[cueKey] (json) {
    cueKey: str;
    vfxSelectors: list<PulseVfx>; // 视觉选择：互斥执行，用于物理材质匹配
    vfxAdditives: list<PulseVfx>; // 叠加：条件满足即播放，用于暴击、处决等额外特效
    sfxSelectors: list<PulseSfx>; // 听觉选择：互斥执行，防止物理碰撞音效重叠
    sfxAdditives: list<PulseSfx>; // 叠加：用于 UI 反馈、额外机制音效
    floatingTexts: list<FloatingText>; // 飘字选择：互斥执行
    anim: list<PulseAnim>; // 动作选择：互斥执行，向 ActorView 广播抽象意图
}

struct PulseVfx {
    role: CueRole;
    socket: str;
    tracking: TrackingMode;
    asset: str -> vfx_metadata;
    requireTags: list<str> -> gameplaytag; // AND
    allowOverlap: bool;
}

struct PulseSfx { 
    role: CueRole;
    asset: str ->sfx_metadata;
    requireTags: list<str> ->gameplaytag;
    allowOverlap: bool;
}

struct FloatingText {
    role: CueRole;
    asset: str ->floating_text_metadata;
    requireTags: list<str> -> gameplaytag;

    hideIfBelowMagnitude: float; 
    mergePolicy: TextMergePolicy;
}

struct PulseAnim {
    role: CueRole;
    intent: str ->anim_intent;
    requireTags: list<str> -> gameplaytag; // AND
}

enum TrackingMode {
    Follow; // 随宿主移动
    Stationary; // 宿主后续的任何移动，都与它无关。
}

enum CueRole { Target; Instigator; Causer; }

interface TextMergePolicy {
    
    // 模式 1：独立弹出
    // 适用于单发慢速武器、暴击
    struct None {
    }

    // 模式 2：节奏批处理
    // 在内存中偷偷累加，窗口期满后【生成 1 个】新飘字。
    // 适用于DOT、HOT、或者存在微小时间差的多发霰弹枪。
    struct Batch {
        mergeScope: MergeScope;
        mergeWindow: float; 
    }

    // 模式 3：滚动刷新
    // 【立刻生成】飘字，后续伤害持续叠加并【刷新原有UI节点】的数值与存活时间。
    // 适用于激光束、喷火器、高频持续切割，或多段判定中仅展示最强一击
    struct Rolling {
        mergeScope: MergeScope;
        mergeWindow: float; 
    }
}

enum MergeScope {
    PerInstigator; // 区分施法者（常规战斗）
    Global;        // 全局合并（环境伤害/大世界机制）
}
```

- TrackingMode 是**意图声明**，非实现规定：配置层只声明"要不要跟随宿主"，具体跟随方式（同步位置 / 是否继承旋转）由胖资产自决。要实现"随宿主位置移动但不继承旋转"，配置成 Follow，"不继承旋转"由胖资产实现。
- **Pulse 的 Follow 不得通过"挂为宿主子节点"实现**：宿主销毁会级联带走子节点，从而绕过 Ticket 归还闭环（资产须主动 Return、严禁私自销毁），留下悬挂引用。Pulse 无主、fire-and-forget，其跟随只能以"资产独立存活、按帧同步锚点位置"实现，宿主死亡后停止跟随、原地播完自行归还。State 因有主、随宿主同生共死，挂载于宿主安全，不受此限。
- **与 Unreal/Unity 的 attach + 死前 detach 有别**：那条路径隐含"资产归属某宿主"的反向索引，与无主模型冲突；且逻辑层宿主与引擎节点销毁不同步，detach 可能错过时机。按帧同步是无主模型的代价，非实现偏差。

#### 状态型 (State)
**适用于：持续性表现（如 Buff光环、引导施法、材质覆写）。**
状态型表现跟随逻辑层的 Status / Ability / SpawnObj 同生共死。因其必定挂载在宿主自身，无需 role 路由，直接平铺 Intent 意图。

```cfg
// 状态型：适用于：持续性表现（如 Buff光环、引导施法、材质覆写）。
// 跟随 Status/Ability/SpawnObj 同生共死。必定挂载在宿主自身，无需 role 路由。
table cue_registry_state[cueKey] (json) {
    cueKey: str;
    vfxSelectors: list<StateVfx>;
    vfxAdditives: list<StateVfx>;
    sfxSelectors: list<StateSfx>;
    sfxAdditives: list<StateSfx>;
    renderModifiers: list<RenderStateModifier>;
}

// 状态视觉意图：需要进行同源合并和槽位挤占
struct StateVfx {
    socket: str;
    tracking: TrackingMode;
    asset: str -> vfx_metadata;
    requireTags: list<str> -> gameplaytag;
    
    instancePolicy: InstancePolicy; // Shared / Unique (各自独立)
    slotPolicy: VisualSlotPolicy;   // None (无干扰共存) / Compete (挤占)
}

enum InstancePolicy {
    Shared; // 默认值：同源融合共享实例
    Unique; // 独立实例
}

interface VisualSlotPolicy {
    // 不参与挤占（完美共存，如流血、左手火）
    struct None {}

    // 参与挤占（如头顶眩晕标识）
    struct Compete {
        slotId: str ->visual_slot;  // 比如 "Overhead_Status"
        priority: int;              // 优先级
    }
}


// 状态听觉意图：如持续的技能引导音效，需要参与宿主的听觉槽位抢占
struct StateSfx {
    asset: str -> sfx_metadata;
    requireTags: list<str> -> gameplaytag;
    
    slotPolicy: AudioSlotPolicy;    // None / Compete (如打断施法时淡出并抢占)
}

interface AudioSlotPolicy {
    // 不参与挤占（如多发子弹同时命中的噗噗声）
    struct None {}

    // 参与挤占（如同一个 NPC 的语音只能播一句）
    struct Compete {
        slotId: str ->audio_slot; // 比如 "Voice"
        priority: int;            // 优先级
    }
}

struct RenderStateModifier {
    requireTags: list<str> ->gameplaytag; // 触发条件
    priority: int;                        // 挤占权重
    target: str ->render_target_key;      // 比如 "Body"、"Weapon"
    overrideDetail: RenderOverride;       // 行为载荷
}

interface RenderOverride {
    
    // 模式 1：整体材质替换
    struct SwapMaterial {
        asset: str ->mat_metadata;   
    }

    // 模式 2：修改 Shader 浮点数
    struct ShaderFloat {
        shaderParam: str ->shader_param_key;           
        floatValue: float;
    }

    // 模式 3：修改 Shader 颜色
    struct ShaderColor {
        shaderParam: str ->shader_param_key;
        colorValue: str;
    }
}
```

### 物理层：Asset Library

**物理层**：定义“我是谁、我的物理极限是什么”。  
资产库记录引擎资源的客观物理元数据（如 `limit_group` 全局配额、`cullDistance` 裁剪距离）。这些物理约束是恒定的，不随逻辑意图的改变而转移。

#### 资产元数据 (Metadata)

配置库强制使用抽象的 **`assetId`** 作为查询主键，**绝对严禁直接使用底层引擎的物理资源路径（如 Prefab 或 AudioClip 路径）**。
* **解耦优势**：这种“基于主键的间接寻址”彻底切断了逻辑配置与工程目录的强关联。它允许底层引擎在不触碰任何业务配置的前提下，自由进行资源重构、打包策略调整、AB 测试，甚至是针对低端机型的同键异体资产（Asset Downgrade）替换。

```cfg
// 特效元数据
table vfx_metadata[assetId] {
    assetId: str;
    description: text;
    prefabPath: str;   // 引擎资源路径
    
    group: str ->limit_group; // [仅 Pulse 生效] 全局并发限额
    cullDistance: float;      // [仅 Pulse 生效] 距离剔除
}

// 音效元数据
table sfx_metadata[assetId] {
    assetId: str;
    description: text;
    audioPath: str;
    
    group: str ->limit_group; // [仅 Pulse 生效]
    cooldown: float;          // [仅 Pulse 生效]  时间限频（防爆音）
}

// 材质元数据
table mat_metadata[assetId] {
    assetId: str; 
    description: text;
    assetPath: str;
}

// 飘字元数据
table floating_text_metadata[assetId] {
    assetId: str;
    description: text;
    assetPath: str;

    group: str ->limit_group; // [仅 Pulse 生效]
}

// 动作意图元数据
// 注意：这里只登记"抽象动作意图"（如 "Hit.Recoil"），不持有动画状态机。
// Actor 表现层作为 Actor 的组件监听该意图，自行驱动状态机。
table anim_intent[intentId] {
    intentId: str;
    description: text;
}
```

**💡 设计决策：物理防线参数的作用域边界**

元数据中的 `group`、`cullDistance` 与 `cooldown` 共同构筑了**“实例化前防线（Pre-instantiation Defense）”**。系统调度时，上述参数**仅对脉冲（Pulse）与飘字（UI）型意图生效**；当资产被状态（State）意图调用时，防线参数将被直接忽略并全量放行。

此决策基于三大架构考量：
1. **防爆流 vs 自收敛**：Pulse 具瞬时爆发性，易引发并发风暴，必须在跨引擎边界前无情拦截以阻断 CPU 开销；State 生命随宿主存亡，其并发量已受制于同屏实体上限与单体槽位挤占，天然收敛，无需全局设限。
2. **资产无意图**：不将 `vfx_metadata` 依意图拆表。物理资产客观存在，不具“意图”属性——同一团火焰 Prefab，既可作落地瞬发（走 Pulse 限流），亦可作点燃 Buff 附着（走 State 挤占）。单表设计最大化捍卫了底层资源的复用率。

#### 辅助表

```cfg
// 视觉槽位定义表，例如："BodyAura", "Overhead_Status"
table visual_slot[slotId] {
    slotId: str; 
    description: text; // 策划备注
}

// 听觉槽位定义表，例如："MovementLoop", "EnvironmentalState", "Voice"
table audio_slot[slotId] {
    slotId: str;
    description: text; 
}

// 渲染目标注册表，如 "Body", "Weapon", "Head"
table render_target_key[targetKey] (entry='targetKey') {
    targetKey: str;
    description: text;
}

// Shader 参数名注册表，如 "_GlowIntensity", "_FresnelColor", "_Dissolve"
table shader_param_key[paramKey] (entry='paramKey') {
    paramKey: str;
    description: text;
    paramType: ShaderParamType; // 参数类型，便于编辑器展示
}

enum ShaderParamType { Float; Color; }
```

#### 限制规则表

```cfg
// 全局并发限制规则，例如："VFX_Hit_Spark", "SFX_Voice_Bark"，"UI_Text_CritDmg"
table limit_group[groupId] {
    groupId: str;
    maxCount: int; // 该通道的全局最大存活数量
    resolveRule: ResolveRule; // 超出限制时的解决策略
}

enum ResolveRule {
    StopOldest;  // 顶替最老的 (适用于连续受击)
    RejectNew;   // 拒绝新的 (适用于持续性范围光环)
    StopLowest;  // 优先顶替 Magnitude 最低的实例 (适用于海量伤害飘字，受击声音)
}
```

**全局限制与剔除策略示例**：

* **视觉防线**
    `VFX_Hit_Spark` (小火花)：`maxCount: 15`, `resolveRule: StopOldest`。适用于高频冲锋枪扫射，老的火花被瞬间顶替不会引起视觉突兀。
* **听觉防线**
    `SFX_Voice_Bark` (环境喊话)：`maxCount: 3`, `resolveRule: RejectNew`。防止同屏 20 个怪物同时喊话导致严重出戏，确保已发声的实体把台词念完。
* **界面防线**
    `UI_Text_CritDmg` (暴击黄字)：`maxCount: 5`, `resolveRule: StopLowest`。在 `TextAggregatePolicy` 完成基础清洗后，进一步限制同屏实体绝对上限。基于伤害值（Magnitude）裁决，优先剔除伤害最低的飘字，确保最爆炸的输出反馈占据视觉中心。

---

## 资产解析管线

当逻辑层调用 `ICueOps` 后，请求将经过严格的**两阶段过滤**：首先在树状拓扑中完成静态意图的寻址，随后基于运行时上下文（Tags）进行选择。

### 阶段一：意图层级回退
当逻辑层派发 `cueKey` 时，系统依据继承关系寻找最优配置：
1. **本级探查**：优先匹配当前 `cueKey` 绑定的配置实体。
2. **祖先回退**：若自身无独立配置，则沿 `ancestors` 继承链逐层上溯，直至命中首个有效注册表。

以上树状回退寻址的最终结果，可以在第一次访问时保存下来。

### 阶段二：基于 Tags 的上下文过滤与意图提取
获取静态意图（Intent）后，系统会基于当前环境与实体的 Tags 状态，执行纯粹的**只读过滤**，最终输出一份“待执行的有效意图清单”交由下游的【防过载机制】处理：

#### VFX / SFX (视觉与听觉)
* **Selector (互斥选择器)**：自上而下遍历，一旦目标的上下文标签满足 `requireTags`，即**命中并阻断**后续遍历，提取出唯一的表现意图（专用于物理特征绝对互斥，如：砍肉 vs 砍铁）。
* **Additive (并行叠加器)**：全量遍历，满足条件的条目**全部放行**并提取（专用于复合机制，如：暴击时附带震屏）。

#### RenderState (渲染状态)
* **全量提取**：遍历配置，全量提取所有满足 `requireTags` 阈值的材质替换或 Shader 参数修改指令。不再此阶段做优先级判断。

#### FloatingText (飘字)
* **静默剔除**：直接丢弃 `Magnitude` 低于 `hideIfBelowMagnitude` 阈值的无效或微小浮点请求，其余全量提取。

> **管线交接点**：至此，配置解析完成。所有被成功提取的意图，将统一被送入【防过载与冲突决断】的三级漏斗，进行物理生命周期裁决。

### 寻址示例

* **逻辑输入**：逻辑管线触发了 `Hit.Physical` 事件，此时受击目标（Actor）身上带有 `[Target.Flesh]` 标签，且本次攻击附带 `[Event.Critical]` 标签。
* **执行步骤**：
    1. **层级获取**：定位到 `Hit.Physical` 的 `cue_registry_pulse` 配置。
    2. **选择器决断 (`vfx_selector`)**：
        * 匹配项 1: `requireTags: [Target.Metal]` -> **失败**，跳过。
        * 匹配项 2: `requireTags: [Target.Flesh]` -> **命中**，选中 `vfx_blood_splash`，**立刻跳出选择器**。
        * 匹配项 3: `requireTags: []` (通用白光兜底) -> 未执行。
    3. **叠加器决断 (`vfx_additive`)**：
        * 匹配项 1: `requireTags: [Event.Critical]` -> **命中**，额外选中 `vfx_camera_shake`。
* **结果**：系统极速判定并播放了“喷血特效”与“屏幕震动”，全程逻辑线性，无任何歧义与性能浪费。

---

## 双轨防线

当【资产解析管线】输出了有效的“意图清单”后，系统根据表现的生命周期特征实施**双轨制（Dual-Track）防线**。所有冲突裁决与性能过滤均在调用底层引擎 `Instantiate` 之前完成，确保 0‑GC 开销与绝对确定的视听反馈。

| 对比维度 | 轨一：Pulse (脉冲快路径) | 轨二：State (状态慢路径) |
| :--- | :--- | :--- |
| **适用场景** | 受击、开火、爆炸、飘字 | Buff 光环、引导施法、材质覆盖 |
| **生命周期** | 点火即走，底层时间轴自治 | 与逻辑宿主同生共死 |
| **去重方法** | 同帧去重，聚合 | 同源融合 (Shared)，计算峰值 |
| **冲突裁决** | 触发 `limit_group` 全局限额剔除 | 触发内部槽位抢占 (Compete/None) |
| **底层接口** | `IPulseCue`, `IFloatingTextCue` | `IStateCue` |

### 轨一：脉冲型 (Pulse)

**作用域**：瞬发且无生命周期的事件反馈（如受击火花、开火、爆炸、飘字）。  
**核心策略**：无状态、不建对象、超限即丢。绝对不进入状态机，通过纯粹的数据拦截捍卫性能底线。

#### Vfx/Sfx 同帧去重  
针对同一帧内爆发的海量同质 Vfx/Sfx 请求（例如霰弹枪多发弹丸同时命中），本层在纯内存中执行极速物理去重，消除无意义的并发光污染与 CPU 开销。

- **挂载实体标识 (HostId)**  
  `HostId` 为表现实际挂载的实体 ID。

- **判断顺序**  
  对每一个 Vfx/Sfx 请求，系统按以下逻辑执行：
  1. **`allowOverlap`**：若对应意图配置显式声明 `allowOverlap: true`，则**跳过本层去重，直接全量放行**。此机制专为需要强制共存的矢量特例设计（例如法师同帧对多个目标释放瞬发闪电链）。  
  2. **去重（默认）**：若 `allowOverlap` 为 `false`，系统以 `(AssetId, HostId)` 作为去重键值进行本帧内查重。若该键值已存在，则拦截后续重复请求，仅保留首个实例。  
     > *示例*：霰弹枪 10 发弹丸同帧命中同一目标（`role: Target`→`HostId` 为目标 ID），防线瞬间拦截后 9 个，仅爆 1 滩血。

#### Vfx/Sfx 实例化前拦截

- **距离剔除 (`cullDistance`)**：VFX 专属。若表现锚点至相机的距离超越阈值，直接丢弃请求。彻底抹除视距外事件（如远方团战）的无效 CPU 消耗。
- **时间限频 (`cooldown`)**：SFX 专属。基于资产主键校验最后派发时间戳，冷却期内请求直接丢弃。在纯内存层截断高频连发或 DoT 造成的物理“爆音”。注意：对于需要互相打断的同类短促语音（如 NPC 连发台词），Cue 表现层**不提供互斥逻辑**，由引擎层提供。


#### UI 聚合  
针对高频伤害飘字（如霰弹枪、持续喷火、DoT），系统在内存层构建一道绝对静默的数据节流阀，将瞬时海量数值清洗为极低频的引擎 UI 指令，彻底免除 UI 线程卡顿的同时，赋予底层胖资产极大的表现自由度。

**路由分发**  
所有飘字请求首先根据配置的 `mergePolicy` 类型进行分流：

- **无聚合 (None)**：直接放行。只要再通过并发防线，则即时生成 `Count=1` 的 `AggregateStats` 调用 `Fire`，下游 UI 资产直接呈现单次伤害。  
- **聚合模式 (Batch / Rolling)**：请求进入聚合缓冲区，由系统执行统一的数据合并与节流调度。

**聚合流程（Batch / Rolling）**  
当请求落入聚合模式后，按以下步骤处理：

1. **隐式聚合主键 (Merge Key)**  
   系统基于严格的物理隔离生成不可变聚合组键：`(TargetId, AssetId, MergeScope == PerInstigator ? InstigatorId : 0)`。  
   这确保不同视觉资产（如暴击 vs 普攻）或不同玩家造成的伤害被天然分流至独立聚合池，永不出现数据污染。

2. **纯数据统计 (AggregateStats)**  
   请求命中同一聚合池时，系统**拦截且扣留**向引擎的派发指令，转而在纯内存中执行零 GC 的数学统计，生成多维数据包 `AggregateStats`（内含命中次数 `Count`、累计总和 `Sum`、单次最高峰值 `Max`、最小值 `Min`、最新值 `Latest`）。

3. **生命周期策略流转**  
   依据配置启动对应的时空调度策略：
   - **批处理 (Batch)**：在 `mergeWindow` 窗口期内仅在内存中累加，窗口期满后向下游**发送 1 次** `OnFire` 指令，携带聚合后的 `AggregateStats`。适用于 DoT 或微小时间差的多发弹丸命中。  
   - **流式刷新 (Rolling)**：首次请求立刻触发 `OnFire` 生成 UI 实例；窗口期内的后续伤害不再生成新节点，而是通过高频 `OnUpdateRolling` 指令注入该实例，实时更新其聚合数据。适用于激光束或极高频持续切割。

**数据下发**：系统完成数学聚合后，直接向下游抛出 `AggregateStats` 结构体。下游 UI 资产根据该载荷内的数据（如 `Count`, `Max`, `Sum`）自主驱动视觉表现（如暴击震动、字体跳动）。

#### 音频通道自治  
对于需要互相打断的同类短促语音（如 NPC 连发台词），Cue 表现层**拒绝提供互斥逻辑**。指令直传底层，由音频引擎（如 FMOD/Wwise）依赖自身的 Voice Channel 与抢占规则（Voice Stealing）在物理层完成无缝切断，坚守“瘦配置”原则。

#### 并发防线  
这是脉冲快路径与 UI飘字在实例化前的**最后一道硬性关卡**。当请求穿透上述防线抵达此处时，系统查询资产元数据中的 `limit_group`，并执行严格的并发裁决：

- 若当前场景同类实例未达 `maxCount`，正常放行。  
- 若已达上限，严格遵循资产侧声明的 `ResolveRule`：  
  - `RejectNew`：拒绝新请求，直接丢弃。  
  - `StopOldest`：选定存活最久的实例作为牺牲者，强制终止并回收。  
  - `StopLowest`：选定瞬时强度 (`magnitude`) 最低的实例作为牺牲者，强制终止并回收。  

**绝不补发**：被选为牺牲者的脉冲实例直接终止，不会因后续名额空出而自动复活。全局防线仅做瞬时裁决，不持有任何队列状态。

### 轨二：状态型 (State)

**作用域**：与宿主生命周期深度绑定的持续状态（Buff 光环、引导音效、材质覆写）。  
**核心策略**：通过严密的单体防线实施引用追踪与频道焦点管理。本轨道永不进入全局并发防线，以此保证逻辑状态与表现的一致性。

#### StateVfx / StateSfx 防线

##### 同源融合  
解决单体宿主上跨逻辑源的“同质意图重叠”与冗余实例化。  

- **Shared（默认）**：`StateVfx` 默认采用，`StateSfx` 强制采用。  
  鉴权主键：**视觉 `[AssetId + Socket]`，听觉 `[AssetId]`**。  
  若主键已存在，拦截新生成请求，仅登记引用（引用计数 +1）；随后计算最高 `magnitude`，通过 `OnUpdateCue` 驱动唯一实例平滑演进。  
- **Unique（仅视觉）**：允许矢量特征表现绕过合并（如多条来自不同方向的吸血光束独立生成）。

##### 槽位挤占  
解决单体宿主内部 VFX / SFX 的“异质意图冲突”，维护视听频道焦点。  

- **Compete**：参与频道焦点抢占。高优状态切入时，当前持有焦点的实例触发 `Suspend`（剥夺视听表现并移入挂起队列，保持物理存活与参数上下文）；高优状态结束并卸载后，系统从挂起队列中选取最高优先级的候补触发 `Resume`（夺回焦点，无缝恢复）。**仅当意图的逻辑生命周期真正宣告终结时，才触发 `Detach` 执行平滑卸载与回收。**
- **None**：跳过本层，与当前槽位的焦点实例完美共存（如左右手各自独立的武器附魔，互不干扰）。

#### RenderState 覆写裁决

`RenderStateModifier` 的意图在管线阶段全量收集，**真正裁决发生在本层**。  

**核心规则** - **隐式槽位**：每个 `RenderStateModifier` 通过 `target`（如 `"Body"`）占据一个渲染槽位；同一 `target` 同时只允许一个意图生效。  
- **优先级裁决**：同一 `target` 上，所有挂起意图**仅按 `priority` 降序排列**；若优先级相同，以**最先 Attach 者**胜出（先到先得）。
- **众生平等**：裁决**不区分覆写类型**（`SwapMaterial`、`ShaderFloat`、`ShaderColor` 平等竞争）。高优先级的 Shader 参数修改完全可以挂起低优先级的材质替换。
- **回落与恢复**：当占据焦点的覆写被移除时，系统恢复引擎原始材质，随后从挂起队列中选取最高优先级候补重新生效。
- **不参与同源融合**：`RenderStateModifier` 无 `Shared`/`Unique` 合并策略，每个意图独立参与优先级竞争。


#### 无全局并发防线

状态型表现不做基于计数的强制剔除。性能安全由两层保证：同屏 Actor 上限受对象池与裁剪严格控制；单 Actor 状态槽位由设计与逻辑约束自然封顶。运行时优化交由胖资产内部，基于 LOD、Visibility 或 Virtual Voice 自动挂起/降级，Cue 系统不再介入。

### 防线示例

1. **同帧标量去重（Pulse）**：霰弹枪 10 发弹丸同帧击中玩家。系统去重，仅爆 1 滩血；伤害数字经 **UI 数据节流**合并为 1 个飘字；随后请求进入 **全局并发防线**，若该火花组已满，触发 `StopOldest` 顶替最老实例。
2. **同帧矢量共存（Pulse）**：法师开启“连环闪电”同帧命中 3 个敌人。因意图配置了 `allowOverlap: true`，防线放行去重，法师手中同时劈出 3 条瞬发闪电；这 3 条闪电各自向下，接受 **全局并发防线** 的限额检测。
3. **同质参数演进（State）**：玩家被施加 3 个 `vfx_poison` 中毒 Buff。系统在 **同源融合** 层拦截后 2 个请求，仅生成 1 团毒气，并通过 `OnUpdateCue` 推高浓度参数。该毒气实例直接移交底层，若全局超量则触发 **弹性扩容**。
4. **视觉焦点抢占（State）**：玩家开启“火环”（优先级 5），随后被 BOSS 的“极寒深冻”（优先级 100）击中。系统在 **槽位挤占** 层强制隐藏火环并播放冰冻。冰冻解除后火环无缝回落，全过程闭环在宿主内部。

---

## 底层资产接口与生命周期

**核心契约**：表现系统绝不轮询资产状态，资产也绝不能私自执行 `Destroy` 销毁自身。两者的交接必须通过操作凭证 `Ticket` 与回收器 `ICueRecycler` 完成绝对闭环。

### 生命周期凭证 (Ticket)

```csharp
// AssetHandle句柄：防 ABA 问题的 64 位代型索引 (Generational Index)。纯值类型。
// 强制回收器：资产生命周期结束时的唯一合法出口
public interface ICueRecycler {
    // 资产表现结束后，必须主动调用此方法将自身归还给对象池
    void Return(AssetHandle handle);
}

// 操作凭证：系统下发给引擎资产的只读契约
public readonly record struct Ticket(
    AssetHandle Handle,
    float Delay,
    ICueRecycler Recycler
);
```


* 当底层特效播放完毕（如粒子停发且存活时间结束、音效播放完成），资产内部组件**严禁调用 `Destroy(gameObject)`**，必须调用 `Ticket.Recycler.Return(Ticket.Handle)`。
* `Delay` 字段赋予了底层资产“时间轴自治权”：如果资产是异步加载出来的，`Delay` 会记录加载耗时。短促特效发现 `Delay > 200ms` 可以直接自我回收；长特效则可将自身的粒子系统快进 `Delay` 毫秒，实现无缝对齐。

* **防泄漏契约 (Watchdog Failsafe)**:

    信任但需验证。系统对所有凭证实行“看门狗”兜底管理。

    * 对于 Pulse/UI：自 OnFire 起设定绝对最大存活时间阈值。
    * 对于 State：自下发 OnDetach（平滑卸载）指令起，设定最大过渡宽限期（如 5 秒）。

    任何逾期未主动调用 Return() 归还凭证的资产，系统将抛出运行时警告，并越权下发 OnForcedKill() 指令执行暴力回收，以此确保复杂生产环境下的绝对内存安全。

### 飘字载荷 (AggregateStats)
专为飘字或需要积攒爆发的表现设计的数据结构。系统层负责拦截、聚合与节流，将最终的数学统计结果交给胖资产进行视觉呈现。

```csharp
// 记录时间窗口内的统计信息
public readonly record struct AggregateStats(
    int Count,   // 命中次数 (如：5连击)
    float Sum,   // 累计总和 (如：总伤 500)
    float Max,
    float Min,
    float Latest // 最后一次的值
);
```

### 核心资产接口
所有的引擎资产必须根据其生命周期特征，实现以下三大接口之一：

```csharp
// 轨一：脉冲型 (点火即走，底层自治)
public interface IPulseCue 
{
    // 触发：接收上下文与凭证。此后 Cue 系统完全脱手。
    void OnFire(in CueContext ctx, in Ticket ticket);
    
    // 强制掐断
    void OnForcedKill(); 
}

// 轨一特化：飘字型 (UI 专用，支持流式聚合)
public interface IFloatingTextCue 
{
    // 首次触发或 Batch 批处理结束时下发。
    // 注意：哪怕是单次不聚合的伤害，也会统一包装为 Count=1 的 stats 传入，消灭 if-else。
    void OnFire(in CueContext ctx, in Ticket ticket, in AggregateStats stats);
    
    // 专为 Rolling 模式提供的增量刷新接口。
    // 引擎层可在此处利用 stats 读取最新总伤，并播放“字体跳动”、“变色”等动画。
    void OnUpdateRolling(in CueContext ctx, in AggregateStats stats);
    
    void OnForcedKill();
}

// 轨二：状态型 (与宿主绑定，受控生死，支持虚拟化代理)
public interface IStateCue 
{
    // 首次装载：建立物理映射
    void OnAttach(in CueContext ctx, in Ticket ticket);
    
    // 状态刷新：接收逻辑层聚合后的峰值强度参数 (如：毒 Buff 层数加深)
    void OnUpdate(in CueParams parameters);
    
    // 被动挂起 (虚拟化)：被槽位高优实例挤占时调用。
    // 资产必须：停止粒子发射、隐藏 Mesh、暂停混音。保留内部参数，绝不能自我回收！
    void OnSuspend();

    // 重新唤醒：夺回焦点时调用。
    // 资产必须：立刻依据当前缓存的参数，无缝恢复播放。
    void OnResume();
    
    // 平滑卸载：逻辑宣告终结。
    // 资产必须：执行淡出过渡 (如音效 FadeOut)，过渡彻底结束后，主动调用 Recycler.Return()。
    void OnDetach();

     // 强杀兜底：当 OnDetach 超时，或逻辑实体被异常摧毁（如强制切场景）时系统越权调用。
    void OnForcedKill();

}
```

### 同步边界：资产提供者
Cue 模块的所有边界接口必须是同步的。引擎层的异步加载（如 Godot 的线程化资源加载）是实现层的内部细节，不得泄露到接口签名中。Cue 模块不关心资产是如何加载的。

```csharp
public readonly record struct VfxArgs(
    DVfxMetadata vfx,
    DTrackingMode Tracking,
    string Socket
);

public interface ICueAssetProvider
{
    IPulseCue? CreatePulseVfx(in VfxArgs vfx, CueAnchor anchor);
    IPulseCue? CreatePulseSfx(DSfxMetadata sfx, CueAnchor anchor);
    IFloatingTextCue? CreateFloatingText(DFloatingTextMetadata text, Actor anchor);
    // 注意：anim intent 是纯广播，无物理资产/无 Ticket 闭环/无 Watchdog，
    // 故不返回 IPulseCue，也不参与生命周期防线。直接广播给 ActorView。
    void DoPulseAnim(DAnimIntent intent, Actor anchor);

    IStateCue? CreateStateVfx(in VfxArgs vfx, Actor anchor);
    IStateCue? CreateStateSfx(DSfxMetadata sfx, Actor anchor);
    IStateCue? CreateRenderState(DRenderOverride render, Actor anchor);
}
```

---

## 配置 Checklist
1. **标签即表现**：逻辑源必须提供准确的 Tags（如 `Combat.DamageType.Lightning`），而非直接请求具体特效。
2. **意图对齐**：瞬发事件使用 `FireCue`；持续状态使用 `Status.cuesWhileActive` 挂载。
3. **参数浓缩**：仅通过 `magnitude` 或 `CueParams` 传递数值，严禁在协议中扩展具体的颜色、缩放等物理字段。

---

> **表现层职责边界**：Cue 是双轨——Pulse 轨管瞬时视听（命中/爆炸/飘字），State 轨管宿主绑定的持续表现（Buff 光环、循环音、材质覆写、Slot 优先级竞争）；持续动画状态机与瞬时动作裁决归 [ActorPresentation](./actor-presentation.md)；宏观相机与大招演出归 [场景演出](./scene-design.md)。Cue 与 ActorPresentation 的唯一握手是 `Pulse.anim` 的抽象意图。详见 [Actor 表现层](./actor-presentation.md)。
