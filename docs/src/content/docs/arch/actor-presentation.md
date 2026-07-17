---
title: Actor 表现层
sidebar:
  order: 23
---

`ActorPresentation`：Actor 的持续视觉状态机。它掌管 Actor 的内禀视觉——待机、移动、朝向、持续动画混合、Socket 装配——并对**瞬时动作意图**做自主裁决（打断、混合）。

它和 [Cue](./cue-design.md)、[场景演出](./scene-design.md) 分工：Cue 是双轨表现系统（Pulse 轨管瞬时视听、State 轨管宿主绑定的持续表现如 Buff 光环与循环音），scene 是导演级调度，ActorPresentation 是 Actor 自己的持续动画状态机。三者各司其职，仅 `Pulse.anim` 的抽象意图是 Cue 与 ActorPresentation 的握手面。

> 本文是设计基准（should-be）。ActorPresentation 目前**尚未实现**（代码库中无类定义，`anim_intent` 在 Godot 层的接收点 `GodotCueAssetProvider.DoPulseAnim` 为占位 stub）。本文锚定**已确定的边界与契约**（源自 cue-design.md 的职责划分），对内部未决项以「开放问题」如实标注，不臆造实现细节。

---

## 一、职责边界

ActorPresentation 拥有 Actor 的**持续动画状态**——待机、移动、朝向、持续动画混合。下列职责与 Cue / scene 按生命周期与耦合方向划归他处：

### 1.1 持续动画状态机 → ActorPresentation

待机、移动、朝向、持续动画混合是 Actor 的内禀视觉状态，由 ActorPresentation 自治。**注意区分**：Buff 光环、循环音、材质覆写这类"宿主绑定的持续表现"仍是 Cue 的 State 轨职责（由 `CueComponent` 管理，含 Slot 优先级竞争）；ActorPresentation 只管**骨骼驱动的动画状态**，不接管这些特效/音效。

**理由**：动画状态机强耦合骨骼与上层逻辑（移动、寻路、能力阶段）。若由 cue 反向驱动，会制造"表现层→逻辑层"的双向依赖，违背瘦配置原则。

**唯一握手**：`cue_registry_pulse.anim`。Cue 在瞬时事件（受击、处决）发生时广播一个抽象动作意图（`anim_intent`，如 `"Hit.Recoil"`），ActorPresentation 监听后自主裁决打断与混合。Cue 不感知骨骼与动画片段。

### 1.2 相机反馈 → VFX 轨（瞬时） / scene（宏观）

| 路径 | 归属 | 理由 |
| :--- | :--- | :--- |
| 瞬时震屏（暴击、爆炸） | cue VFX 轨 | 与主特效同源选择、同源限频。震屏实现为一个 `vfx_metadata` 资产，prefab 内部脚本驱动相机 |
| 宏观相机控制（FocusOn / Cutscene / Restore） | [scene](./scene-design.md) 的 `CameraAction` | 演出级调度，生命周期与瞬时事件不同量级，归导演层 |

**不为相机单设轨**：震屏复用 VFX 轨即可，无需新增表；宏观控制属演出意图，让 Cue 穿透到导演层会破坏职责单一。

### 1.3 大招演出（Ultimate / Cinematic）→ scene

招式过场、镜头编排、输入锁定、时间轴推进由 scene 层的 `Act` / `CameraAction` / `WithActorControl` 承担，**不通过 Cue 触发**。可以考虑为 Effect 引入 `RunSceneEffect`。

**ActorPresentation 的本份**：仅负责演出过程中的瞬时视听碎片（剑光、法阵、震屏），走 Pulse 的 VFX/SFX 轨。演出编排本身是 Ability 的逻辑阶段，归 scene 层。

---

## 二、动作归属：瞬时动作 vs 持续动画

"角色动画"按**是否一次性触发 / 是否循环**分两条路，二者在系统里的归属相反，是最容易配错的边界：

| 动作性质 | 归属 | 触发方式 |
| :--- | :--- | :--- |
| **持续 / 循环**（待机、移动、施法姿势、引导循环、收招） | ActorPresentation，**不进 cue** | Ability 通过 `Phase.statuses[].grantedTags` 打标签（如 `Actor.Cast.Casting`），ActorPresentation 监听标签自治切换 |
| **瞬时 / 一次性**（挥下一击、受击后仰、处决、格挡弹反） | cue 的 **Pulse `anim` 轨** | 逻辑层 `FireCue` 触发，`cue_registry_pulse.anim` 广播抽象 `anim_intent`，ActorPresentation 监听后播放 |

> **"用 cue"的精确含义**：瞬时动作走 cue，指走 **Pulse.anim**——cue 仍只广播一个**抽象动作意图**（`anim_intent`，如 `Cast.Fireball.Swing`），**不感知骨骼、不持有动画片段**。真正播什么、如何与当前状态混合、能否打断，仍由 ActorPresentation 自治裁决。cue 的"瘦"边界在瞬时动作上同样成立。

**第一性原理**：动画时序的真值只应有一个来源。持续动画的真值是 Actor 的内禀状态（"我在不在施法"），由标签表达 → 状态机自治维持；瞬时动画的真值是逻辑事件（"这一击发生了"），由 cue 广播 → 状态机一次性响应。两者**都把状态机当唯一的动画执行者**，区别只在"谁发起切换"（标签被动 vs 事件主动）。让 cue 反向驱动持续动画会制造"表现层→逻辑层"的双向依赖，违背瘦配置原则。

### 例子："挥法杖"施法

一次火球术的"动作"实际是两段，分别走两条路：

1. **持续姿势**（进入前摇 → 举起法杖 + 循环吟唱 → 出伤 → 收招）：循环、状态化 → **不进 cue**。
   `Phase.Startup.statuses[].grantedTags: ["Actor.Cast.Casting"]` → ActorPresentation 监听到标签，切到"持杖施法"循环分支；前摇结束标签移除，自动切回收招/待机。姿势维持多久完全由前摇 `duration` 决定，与动画长度解耦。

2. **瞬时挥击**（出伤瞬间法杖划出弧线的那个动作）：一次性、不循环 → **走 cue**。
   在结算的 `effect` 里 `FireCue("Cast.Fireball")`，其 `cue_registry_pulse` 的 `anim` 段广播意图：

```cfg
// cue_registry_pulse / Cast.Fireball 的 anim 段
anim: [
    { role: Instigator; intent: "Cast.Fireball.Swing"; requireTags: []; }
];
```

   cue 广播 `Cast.Fireball.Swing` → ActorPresentation 监听，瞬时叠加一次"挥杖"动作（可打断/混合当前循环姿势）；同一 cueKey 的 vfx/sfx 轨可附带法杖光效与施法音，与挥击意图同源触发、同源限频。

> 拆开两段后，"姿势"由标签稳态维持（前摇多长就举多久），"挥击"由事件瞬时触发（精确对齐出伤那一刻的视觉），各取所长且互不耦合。判定口诀：**循环看标签，一击走 Pulse.anim**。

---

## 三、开放问题

ActorPresentation 的**边界与握手已定**（见上），但**内部设计尚未确定**。下列各项在实现前需要决策，本文不臆造：

1. **状态机拓扑**：有哪些状态、状态间如何转移（待机↔移动↔施法 等），尚未设计。现有设计文档仅以"自治状态机"概括，未给出拓扑。
2. **tag → 动画分支的映射**：持续动画靠 `grantedTags` 驱动，但驱动动画的 tag 命名空间（如 `Actor.Cast.*`、`Actor.Move.*`）在 config 数据（`gameplaytag.csv`）中**尚不存在**——`Actor.Cast.Casting` 等仅是文档假设。需要先定义这套 tag 命名空间，再定 ActorPresentation 如何映射 tag → 动画分支。
3. **命名 Socket 的挂载机制（引擎侧空白）**：需区分两个概念——
   - **Slot（`visual_slot`/`audio_slot`，如 `BodyAura`/`Overhead_Status`）**：已由 `CueComponent`（Cue State 轨）消费，做 Compete 优先级竞争与驱逐，是已实现逻辑，不归 ActorPresentation。
   - **`socket` 字段（如 `"center"`）**：存在于 `DStateVfx`/`DPulseVfx`，目前**仅用于 Shared 去重 key**；Godot provider 直接 `AddChild` 到 Actor 根节点，**没有任何代码按命名 socket/bone 定位挂载**。这个"特效挂到骨骼/socket 点"的引擎侧机制是真空白——可能归 ActorPresentation（它掌骨骼），也可能独立，待定。
4. **混合 / 打断规则**：瞬时意图（`anim_intent`）如何与当前持续动画混合、打断优先级、混合树参数，目前全以"自主裁决"含糊带过，无具体规则。
5. **与 scene 层 `PlayAnimation` 的冲突（待调和）**：[scene-design](./scene-design.md) 有 `PlayAnimation { animName }` Act，直接按动画名播放片段。这与"ActorPresentation 是唯一动画执行者、不感知动画片段"的主张存在张力——scene 层绕过 ActorPresentation 直接播片段，还是经它转发？两者如何共存，设计文档未处理。
6. **承载形态**：ActorPresentation 是 Godot Node、C# 组件，还是两者（core 层无骨骼依赖的抽象 + Godot 层实现）？未定。

---

> **一句话**：Cue = 双轨表现（Pulse 瞬时 + State 持续）；ActorPresentation = Actor 自治的持续动画状态机；scene = 导演调度。三者各司其职，仅 `Pulse.anim` 的抽象意图是 Cue 与 ActorPresentation 的握手面。
