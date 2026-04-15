# 表现层系统设计 —— 标签驱动集中注册版

基于 2D 暗黑/割草类 Roguelike 的极高频触发特征，本系统的核心哲学是：**意图与资源彻底解耦、严格的性能剔除、基于集中式注册表的智能资产寻址，以及锚点归一的生命周期管理**。

表现层仅作为逻辑层 `CueEvent` 的下游消费者，不进行任何业务数值判定。逻辑层只发号施令（触发、挂载、卸载、刷新），表现层只管执行。

## 1. Runtime Context (运行时上下文)

当底层管线触发效果时，系统向目标 Actor 的表现组件广播 `CueEvent`。

```java
record CueEvent(
    CueEventType type,       // Trigger (瞬发) / Attach (挂载) / Detach (卸载) / Refresh (刷新)
    int cueId,               // 对应能力系统中的 cue_key (如 "Hit.Physical")
    long bindingId,          // 逻辑层实例的不透明句柄 (StatusInstance.uid / SpawnObj.uid)，Trigger 时为 0
    
    // --- 实体上下文 ---
    Actor target,            // 承受者 / 表现锚点宿主
    Actor instigator,        // 发起者
    Actor causer,            // 媒介 (如飞行中的子弹实例)
    
    // --- 动态修饰上下文 ---
    float magnitude,         // 强度数值 (用于决定爆炸缩放、叠层表现等)
    IntList contextTags      // 逻辑管线的瞬态标签快照 (如 ["Damage.Element.Fire", "Combat.Result.Critical"])
){}

enum CueEventType { 
    Trigger;  // 瞬发指令：一波流释放，自动回收。
    Attach;   // 挂载指令：创建并绑定到 bindingId，持久存在。
    Detach;   // 卸载指令：精确销毁 bindingId 对应的资源。
    Refresh;  // 刷新指令：更新 bindingId 对应的资源参数 (如叠层变强)。
}
```

## 2. 集中式资产库

这是标签驱动的基石。我们将所有同类表现资源打包成一个个“资产池”。引擎启动时读取此表，作为资源寻址的数据库。

```cfg
// VFX 专属资产池
table vfx_metadata[asset] {
    asset: str; 
    description: text;
    assets: list<VfxAsset> (block=1);
}

struct VfxAsset {
    assetPath: str; 
    supportedTags: list<str> -> gameplaytag (pack); 
    spatialRadius: float;  //空间剔除半径
}

// SFX 专属资产池
table sfx_metadata[asset] {
    asset: str; 
    description: text;
    assets: list<SfxAsset> (block=1);
}

struct SfxAsset {
    assetPath: str; 
    supportedTags: list<str> -> gameplaytag (pack); 
    cooldownPeriod: float; // 时间防爆音冷却
}

// 材质专属资产池
table mat_metadata[asset] {
    asset: str; 
    description: text;
    assets: list<MatAsset> (block=1);
}

struct MatAsset {
    assetPath: str; // 指向 Godot 的 .tres 文件
    supportedTags: list<str> -> gameplaytag (pack); 
    priority: int; // 该材质在材质栈中的优先级（如：霸体 > 冰冻 > 中毒）
}

// 飘字风格池
table floating_text_style[styleId] {
    styleId: str;
    assetPath: str; // 资源实际路径
    
    // 聚合逻辑
    hideIfBelowMagnitude: float; 
    mergeMode: TextMergeMode;
    mergeWindow: float; // 聚合时间窗口 (如 0.2s)
}

enum TextMergeMode {
    None;    // 独立弹出：绝不合并 (适用于单发慢速武器、暴击)
    Batch;   // 节奏批处理：在内存中偷偷累加，窗口期满后【生成 1 个】新飘字。
             // -> 适用于：DOT、HOT、或者存在微小时间差的多发霰弹枪。
    Rolling; // 滚动刷新：【立刻生成】飘字，后续伤害持续叠加并【刷新原有UI节点】的数值与存活时间。
             // -> 适用于：激光束、喷火器、高频持续切割。
    Highest; // 取最高值：窗口期内只显示最大的那个数字。
}
```

## 3. Cue Schema (语义注册表)

逻辑层派发的 `cue_key` 在此处被翻译为具体的“资源请求”。

```cfg
table cue_registry[cueKey] (json) {
    cueKey: str -> cue_key;
    handler: CueHandler; 
}
```

## 4. Cue Handlers (意图执行器)

执行器明确划分瞬发与持续语义。**核心契约：瞬发型允许跨实体调度，持续型强制锚点归一（无 role 选择，仅作用于宿主自身）。**

```cfg
interface CueHandler {
    // 瞬发型：一波流释放，自动回收。
    // 允许通过 role 跨实体表现 (如：在 Instigator 身上播放吸血流轨迹)。
    struct Instant {
        vfx: list<InstantVfx>;             
        sfx: list<InstantSfx>;
        floatingTexts: list<FloatingText>; // 飘字
    }

    // 持续型：跟随 Status/SpawnObj 同生共死。
    // 强制锚点归一：无 role 字段，所有表现仅挂载在接收该 CueEvent 的宿主 Actor 身上。
    struct Loop {
        vfx: list<LoopVfx>;
        sfx: list<LoopSfx>;
        materials: list<MaterialOverride>; // 材质状态覆写
    }
}

// 瞬发型 (含 role，可跨实体)

struct InstantVfx {
    role: CueRole;
    attach: VfxAttach;
    socket: str;
    asset: str -> vfx_metadata;   
}

struct FloatingText {
    role: CueRole;
    style: str ->floating_text_style;
}

// 持续型 Entry (无 role，必定挂载在宿主 Actor 身上)

struct LoopVfx { 
    attach: VfxAttach; 
    socket: str;
    asset: str -> vfx_metadata; 
}

struct LoopSfx { 
    asset: str -> sfx_metadata; 
}

struct MaterialOverride {
    slotIndex: int;
    asset: str ->mat_metadata;
}

enum VfxAttach { WorldStatic; FollowTarget; }
enum CueRole { Target; Instigator; Causer; }
```
针对 `cue-design.md` 的后续章节，我进行了重构与优化。本次优化重点在于**强化“资产解析算法”的权重逻辑**，并确保**生命周期管理**与逻辑层推导规则严丝合缝。

---

## 5. 智能资产解析算法 (Intelligent Asset Matching)

表现层不通过 `if-else` 判断分支，而是利用标签空间进行**加权语义匹配**。当 `CueHandler` 请求一个资产池（如 `asset: "Vfx.Hit"`）时，底层执行以下逻辑：

### 匹配权重公式
对于候选池中的每一个资产 $A$，其匹配得分 $S$ 计算如下：

$$S = \text{Count}(\text{A.Tags} \cap \text{Event.Tags}) \times 100 + \text{Count}(\text{A.Tags} \cap \text{Target.Tags})$$

* **优先匹配事件标签**：如 `Damage.Element.Fire`，这代表了本次动作的“本质”。
* **其次匹配宿主状态**：如 `State.Debuff.Frozen`，这代表了表现的“环境”。
* **排除法（硬拦截）**：若资产要求的某个标签在 `Event` 或 `Target` 中被显式标记为“排除”（取决于具体的 TagQuery 定义），该资产直接出局。

### 寻址示例
* **逻辑输入**：触发 `Hit.Physical` 效果，`ContextTags` 携带 `[Damage.Element.Fire]`。
* **候选资产池 (`Vfx.Hit`)**：
    1.  `vfx_default_spark` (无标签) -> **Score: 0** (兜底)
    2.  `vfx_ice_shatter` (`[State.Debuff.Frozen]`) -> **Score: 0**
    3.  `vfx_fire_explosion` (`[Damage.Element.Fire]`) -> **Score: 100** 🏆 **选中**

---

## 6. 生命周期管理：锚点归一原则

表现层严格遵循**锚点归一（Anchor Normalization）**：**持续型表现的生命周期必须且仅能与其挂载的 Actor 绑定。** ### 引擎自动推导规则
逻辑层无需关心表现指令。表现系统拦截底层管线动作，自动将其翻译为下表中的 `CueEvent`：

| 逻辑动作 (Logic Source) | 推导事件 (`Type`) | 广播目标 (Anchor) | 唯一标识 (`BindingId`) |
| :--- | :--- | :--- | :--- |
| `Effect.FireCue` / `ResolveCombat.cuesOnExecute` | **`Trigger`** | `target` | `0` (即放即走) |
| `Status` 首次挂载 | **`Attach`** | 宿主 Actor | `StatusInstance.uid` |
| `Status` 层数变化 / 时长刷新 | **`Refresh`** | 宿主 Actor | `StatusInstance.uid` |
| `Status` 移除 (过期/驱散/死亡) | **`Detach`** | 宿主 Actor | `StatusInstance.uid` |
| `SpawnObj` (子弹/法阵) 诞生 | **`Attach`** | 实体自身 | `SpawnObj.uid` |
| `SpawnObj` (子弹/法阵) 销毁 | **`Detach`** | 实体自身 | `SpawnObj.uid` |

### 核心机制：CueComponent
每个 Actor 挂载一个无状态的 `CueComponent`，其职责如下：
1.  **字典寻址**：维护 `Map<BindingId, ActiveCueResources>`。
2.  **刷新响应**：收到 `Refresh` 时，根据新的 `magnitude` 动态调整 VFX 缩放或材质参数。
3.  **确定性清理**：当 `Detach` 信号到达或 Actor 销毁时，根据 `BindingId` 精准回收资源，**杜绝幽灵特效**。

---

## 7. 高级表现特性

### 材质栈管理 (Material Stack)
对于 `MaterialOverride`，系统内部维护一个基于优先级的**材质栈**：
* **入栈**：新状态（如“石化”）覆盖旧状态（如“中毒”）。
* **优先级比较**：仅 `priority` 最高的材质渲染到模型。
* **出栈**：当“石化”结束触发 `Detach`，材质栈弹出顶部元素，模型自动恢复为次高优先级的“中毒”效果或原始材质。

### 飘字聚合策略 (Floating Text Batching)
针对割草类游戏的高频伤害，表现层提供内置聚合：
* **Batch (批处理)**：在 `mergeWindow` (如 0.2s) 内收到的所有伤害，合并为一个数字弹出。
* **Rolling (滚动刷新)**：第一个数字弹出后，后续数字在原 UI 位置累加，并重置 UI 的消失动画时间。

---

## 8. LLM 配置指南 (Agent Instructions)

> **核心原则**：你作为逻辑配置者，**严禁**硬编码资源路径，**严禁**直接操作表现生命周期。

1.  **标签即表现**：在 `ResolveCombat` 中务必带上准确的 `tags`（如 `Damage.Element.Lightning`）。
2.  **意图对齐**：
    * 若是瞬发动作（如受击、爆炸），在 `Effect` 中调用 `FireCue`。
    * 若是持续状态（如 Buff、光环、材质变化），将 `cue_key` 填入 `Status` 的 `cuesWhileActive` 字段。
3.  **强度映射**：利用 `magnitude` 传递关键数值（如暴击倍率、叠层数），表现层会自动根据此值调整视听强度。

