---
title: 设计规范
sidebar:
  order: 0
---

# Design 设计规范

> Ripple 战斗框架的 **should-be** 设计基准。这里定义"系统应该是什么样"，而非"代码现在怎么写"。
>
> ⚠️ 只有下表的 **设计规范**（8 篇）是评审基准，改动需评审；**设计评审**与**示例**是辅助材料，不构成基准。

---

## 三种体裁

arch/ 按体裁分三类，回答不同的问题：

| 体裁 | 文件 | 回答的问题 | 性质 |
|:---|:---|:---|:---|
| **设计规范** | `data-foundation` `runtime-lifecycle` `ability-design` `ability-cast` `ai-design` `cue-design` `actor-presentation` `scene-design` | 字段的完整规范是什么？ | 评审基准（should-be） |
| **设计评审** | `ability-review` `ai-review` `scene-review` | 为什么这样设计、不那样设计？业界怎么对照？ | 决策备忘 + 行业对照 |
| **示例** | `examples1` `examples2` | 怎么写？三层如何联动？ | 说明性（不可直接 gen） |

**查询路径**：查字段权威定义 → 设计规范；想知道某个决策的取舍理由 → 设计评审；想看完整联动写法 → 示例。

> 示例为说明性配置，含 `...` 占位与省略定义，旨在展示 GAS / AI / Scene 三层联动，不可直接 gen。设计评审中的业界信息以公开资料为准，不逐条核实。

---

## 设计规范（评审基准）

按架构层次组织，共 8 篇，每篇是各自域的权威基准。

### 基础层（横切，承载一切）

| 文档 | 定位 |
|:---|:---|
| [`data-foundation.md`](./data-foundation.md) | **词汇表** — 标签、属性、资源、事件、表现键、变量的原子定义。所有域共享同一注册表。 |
| [`runtime-lifecycle.md`](./runtime-lifecycle.md) | **运行时骨架** — SceneInstance（组合根 + Tick 编排者）与 Actor（被动数据容器）的生命周期、对外扩展点、初始化纪律、两段式回收。 |

### GAS 能力系统（地基）

| 文档 | 定位 |
|:---|:---|
| [`ability-design.md`](./ability-design.md) | **架构基准** — 全文档集的根。Ability / Effect / Status 三正交 + Tag / EventBus / Pipeline / TagRules。 |
| [`ability-cast.md`](./ability-cast.md) | **施法生命周期专题** — 前摇 / 蓄力 / 引导 / 后摇的 Phase 序列，打断与取消双轨语义。 |

### 上层系统（建在 GAS 之上）

| 文档 | 定位 |
|:---|:---|
| [`ai-design.md`](./ai-design.md) | **AI 行为系统** — Perceive → Think → Act 三段式，扁平评分替代行为树。 |
| [`cue-design.md`](./cue-design.md) | **表现层 Cue** — 瘦配置 / 胖资产，意图与资源解耦；脉冲/状态双轨 + Ticket 凭证闭环。 |
| [`actor-presentation.md`](./actor-presentation.md) | **Actor 表现层** — Actor 自治的持续动画状态机；Socket 装配、瞬时动作裁决。与 Cue 仅 `Pulse.anim` 意图握手。 |
| [`scene-design.md`](./scene-design.md) | **场景演出编排** — 遭遇战/Boss 战级逻辑编排。一切皆 Act 树，FSM 作为树中节点。 |

---

## 架构全景

```
┌─────────────────────────────────────────────────────────┐
│  关卡 / 任务系统    ← 顶层（房间流转、存档、全局进度）     │
├─────────────────────────────────────────────────────────┤
│  Scene 场景系统     ← 导演（编排一场遭遇战 / Boss 战）     │
│  AI 系统           ← 大脑（感知 → 思考 → 行动）             │
│  GAS 能力系统       ← 躯体（技能、Buff、伤害、属性）⭐ 地基  │
│      Ability / Effect / Status 三正交                     │
│      + Tag / EventBus / Pipeline / Stat                  │
│  ActorPresentation ← 持续状态机（待机/移动/朝向、Socket）  │
│  Cue 表现层        ← 瞬时广播（特效、音效、飘字、一击动作）│
└─────────────────────────────────────────────────────────┘

  横切基础：data-foundation（词汇表）· runtime-lifecycle（运行时骨架）
```

GAS 是地基，AI / Scene / Cue 都建立在它之上；data-foundation 与 runtime-lifecycle 是横切基础设施，被所有层共享。
