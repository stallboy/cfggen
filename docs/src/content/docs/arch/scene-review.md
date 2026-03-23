---
title: 场景系统设计 review
sidebar:
  order: 21
---


## 5 个核心共识思维模式

### 模式一：编排层与行为层的严格分离（Orchestration vs. Behavior Separation）

**核心理念**：场景逻辑系统是"导演"，不是"演员"。它不实现战斗计算、不做寻路、不管状态效果的底层结算——它只负责告诉谁在什么时候做什么，以及什么条件下流转到下一步。

这是所有从业者最无争议的共识：**混淆编排层与行为层是场景系统腐化的根源**。

**各工作室做法**：

| 工作室 | 实现方式 |
|:---|:---|
| **Bungie（Destiny 2）** | Encounter Scripting Layer 只发出 "指令"（spawn wave、activate mechanic），具体战斗行为由 AI Subsystem 和 Sandbox Layer 处理。场景脚本永远不直接修改 HP 或碰撞体。 |
| **FromSoftware（Elden Ring / Dark Souls）** | Boss 的 Phase 切换由 "Event Script"（Lua/ESD）驱动，但攻击模式的选择、伤害计算完全在 AI 和战斗系统内部。ESD 只做条件判断和状态迁移。 |
| **miHoYo（Genshin Impact）** | 关卡编排使用 Graph-based 可视化编辑器，节点只调用引擎已注册的 Ability、Effect、AI Profile，不直接操作数值层。 |
| **Naughty Dog（The Last of Us Part II）** | Encounter 脚本使用自研 Scheme 脚本语言，只负责 spawn/despawn、trigger dialogue、set AI state，底层 combat 由 C++ 引擎处理。 |
| **Epic Games（Fortnite / UE Verse）** | Verse 语言中 Device 层只做事件编排，GameplayAbilitySystem 管状态效果，物理和碰撞由引擎底层处理。 |

**与本文档的对应**：`ApplyEffect` 调用 GAS 的 `Effects.execute()`，`WithActorControl` 通过 Tag 间接影响 AI——从不越权直接改数据。

---

### 模式二：数据驱动的声明式配置（Data-Driven Declarative Configuration）

**核心理念**：场景逻辑应尽可能以数据（表、JSON、Graph）而非硬编码表达。策划能在不改代码的前提下创建、修改、调试遭遇战。

这是工业界过去 15 年最坚定的趋势，从"程序员写脚本"走向"策划填表/拖图"。

**各工作室做法**：

| 工作室 | 实现方式 |
|:---|:---|
| **Blizzard（WoW / Overwatch 2）** | WoW 的 Encounter Journal + Server-side Lua 脚本高度数据化。OW2 的 Workshop 系统将场景规则暴露为声明式条件-动作对。 |
| **CD Projekt Red（Cyberpunk 2077）** | Quest/Scene 系统基于 **questDB**（数据库驱动），所有任务节点、条件、分支以结构化数据存储，编辑器 GUI 操作。 |
| **Supergiant Games（Hades）** | 遭遇战房间配置为纯数据文件（Lua 表），包含 wave 定义、spawn 规则、reward 条件。房间逻辑不写在引擎中。 |
| **Riot Games（League of Legends）** | 英雄技能和场景机制通过 Block-based 数据配置系统定义，运行时解释执行。 |
| **Bungie（Destiny 2）** | Encounter 使用自研的 Tag-based 声明式系统，每个 encounter 是一组数据化的 "活动规则" 和 "触发器"。 |

**与本文档的对应**：`scene_definition` 以 JSON 表为载体，`Act` 树的每个节点都是声明式结构体，引擎解释执行。

---

### 模式三：确定性的生命周期与清理保证（Deterministic Lifecycle & RAII Cleanup）

**核心理念**：场景中的每一个副作用（控制权接管、状态施加、AI 修饰、镜头接管）都必须有**确定性的清理路径**。无论正常结束、玩家跳过、还是系统强制中止，副作用必须被撤销。

这是从无数生产事故中总结出的血泪共识。"Boss 死了但无敌 Buff 还在"、"过场结束但镜头锁死" 是最常见的 P0 Bug 类别。

**各工作室做法**：

| 工作室 | 实现方式 |
|:---|:---|
| **Naughty Dog** | 所有 encounter scripting 操作基于 "scope token" 模式——获取 token 时施加效果，释放 token 时自动回滚。脚本崩溃时 token 自动回收。 |
| **Santa Monica Studio（God of War）** | 场景使用 "Encounter Wrapper" 模式，每个 wrapper 维护一个 cleanup list，任何 exit path 都执行 reverse cleanup。 |
| **Square Enix（FFXIV）** | Raid 遭遇战脚本使用严格的 Phase 退出回调体系，onPhaseEnd 必须清除该 Phase 施加的所有 mechanic marker 和 debuff。 |
| **Valve（Half-Life: Alyx / Source 2）** | VScript 中的实体操作通过 "context scope" 管理，scope 销毁时自动 fire OnDestroy outputs，连接的所有效果链自动清理。 |
| **Larian Studios（BG3 / Divinity）** | 场景系统中每个 "Story Event" 注册时附带 undo closure，Timeline 中止时按注册逆序执行 undo。 |

**与本文档的对应**：`WithXXX` 系列的作用域节点 + `onCleanUp(boolean aborted)` 的 finally 语义 + `ActInstance.abort()` 递归清理子节点。

---

### 模式四：事件驱动与响应式更新（Event-Driven Reactive Evaluation）

**核心理念**：条件检测不应每帧轮询全部条件，而应由事件驱动——只在相关状态变化时重新求值。这既是性能考量（Raid 中可能有上百个同时活跃的条件），也是正确性保障（避免轮询时序的 off-by-one-frame 问题）。

**各工作室做法**：

| 工作室 | 实现方式 |
|:---|:---|
| **Bungie（Destiny 2）** | Encounter 条件系统基于 "事件通道" 订阅。每个条件声明它关心的 event channel，只在 channel 有消息时重新评估。 |
| **Epic Games（UE GAS）** | `WaitForGameplayEvent` 是 GAS 的核心异步节点——挂起 Ability Task，等待特定 GameplayEvent 到达后恢复执行。 |
| **miHoYo** | 条件系统使用 "脏标记 + 事件订阅" 双层架构：事件到达时标记脏，下一个 evaluate tick 只重算脏条件。 |
| **Riot Games** | 技能系统的条件评估完全基于事件流（damage event、death event、buff applied event），没有 per-frame polling。 |
| **CIG（Star Citizen）** | Subsumption AI/Scene 系统使用 "Stimulus-Response" 模型，条件节点注册 stimulus listener，收到 stimulus 时触发 response 链。 |

**与本文档的对应**：每种 `SceneCondition` 自带 `reactToEvents` 声明，`WaitForEvent` 在 `onEnter` 注册监听、`onExit` 取消。

---

### 模式五：逻辑与空间的正交分离（Logic-Space Orthogonality）

**核心理念**：场景的逻辑编排（谁做什么、什么顺序、什么条件）与空间布局（出生点在哪、区域多大、路径怎么走）必须正交。同一套 Boss 战逻辑应能放进不同的竞技场而无需改脚本。

**各工作室做法**：

| 工作室 | 实现方式 |
|:---|:---|
| **Bungie（Destiny 2）** | Encounter 脚本引用 "Squad Spawn Point" 和 "Volume" 的抽象 ID，关卡设计师在编辑器中绑定到具体空间。同一 encounter 逻辑可在不同 Strike 中复用。 |
| **FromSoftware** | Boss 的 AI/Event 脚本不含坐标，空间信息通过 map 的 "region" 和 "point" 标记注入。同一个 Boss（如 Margit）可在不同位置复用。 |
| **Naughty Dog** | Encounter 定义中使用 "mark" 系统——逻辑引用 mark 名称，LD 在场景中放置 mark geometry。 |
| **Respawn Entertainment（Apex Legends）** | 赛季更新中同一套环形区域收缩逻辑适用于不同地图，通过 "anchor point" 映射空间。 |
| **Creative Assembly（Total War）** | 战斗脚本引用阵型模板和相对位置，部署到具体战场时通过 terrain adapter 映射。 |

**与本文档的对应**：`scene_definition` 是纯逻辑，空间信息通过 `signature`（入参）注入。SpawnAt、Zone 等通过 `var_key` 间接引用。

---

## 三大根本分歧

### 分歧一：执行模型之争 — 树/图（Hierarchical Tree）vs. 状态机优先（FSM-First）vs. 协程/脚本（Coroutine/Script）

**这是最深层的架构分歧，决定了整个系统的形态。**

#### 立场 A：一切皆树（Tree-First）

**代表**：本文档的 Act Tree 模型、UE 的 Behavior Tree、部分 Visual Scripting 系统

**最有力论点**：
- **统一的生命周期模型**。树的结构天然给出了父子关系和清理顺序——abort 一个节点，其所有子节点自动被 abort。这种结构化清理在 FSM 中极难保证（状态 A 打断状态 B 时，B 的 cleanup 依赖哪些上下文？）。
- **并发语义自然表达**。`Parallel` 节点天然表达"同时做多件事"，而 FSM 的正交并发需要引入 Harel Statecharts 的 Region 概念，复杂度陡增。
- **策划认知负担低**。树是可视化最直观的结构——从上到下、从左到右，执行路径清晰。

#### 立场 B：状态机优先（FSM-First）

**代表**：FromSoftware 的 ESD、Unity 的 Playable Director + State Machine、传统 MMO Raid 脚本

**最有力论点**：
- **Phase 互斥是遭遇战的本质语义**。Boss 要么在 P1 要么在 P2，状态之间是互斥的。FSM 天然表达这种语义，而在树中你需要专门造一个 `StateMachine` 节点来模拟——这说明树不够自然。
- **热跳转（Reactive Transition）**。FSM 的 globalTransition "无论在做什么，只要条件满足就跳" 是极自然的。树需要用 `Parallel + Abort` 的组合来模拟，引入了更多 edge case。
- **状态可视化调试更简单**。当前在哪个 Phase、可能跳向哪个 Phase，一目了然。树的执行路径需要 stack trace 才能理解。

#### 立场 C：协程/脚本优先（Coroutine/Script-First）

**代表**：Naughty Dog 的 Scheme 脚本、Celeste/PICO-8 风格的协程、Larian 的 Osiris 脚本、GDScript 的 `await`

**最有力论点**：
- **时序逻辑的黄金表达**。"做 A，等 3 秒，做 B，等事件 C，做 D" 这种线性时序逻辑，协程写成 `doA(); await sleep(3); doB(); await waitEvent(C); doD();` 远比树结构的 `Sequence[A, Wait(3), B, WaitForEvent(C), D]` 直观。
- **无限灵活性**。脚本可以表达任意逻辑，树和 FSM 都是脚本的子集。复杂的条件分支、动态计算在脚本中是原生操作，在声明式数据结构中需要不断扩展节点类型。
- **调试效率高**。可以直接打断点、看 stack trace、step through。数据化的树需要专门构建可视化调试工具。

---

### 分歧二：副作用管理 — 作用域 RAII（Scoped RAII）vs. 手动标记清理（Manual Tagging）vs. 快照回滚（Snapshot Rollback）

**副作用何时、如何被清理，是场景系统最容易出 Bug 的地方。**

#### 立场 A：作用域 RAII

**代表**：本文档的 `WithXXX` 模式、Naughty Dog 的 scope token

**最有力论点**：
- **不可能忘记清理**。副作用的生命周期被结构化地绑定在树节点的作用域上——进入时施加，退出时撤销（finally 语义）。无论正常退出、异常中止、还是外部 abort，清理必然发生。
- **可组合性强**。多个 `WithXXX` 嵌套时，清理按 LIFO 顺序自动进行，与手动管理相比零心智负担。

#### 立场 B：手动标记清理

**代表**：Unity Timeline 的 TrackMixerBehaviour、传统 MMO 的 "每个 Phase onExit 手写 cleanup"

**最有力论点**：
- **更灵活、更透明**。策划明确知道每个 cleanup 做了什么——"移除无敌 buff"、"恢复 AI"、"清除标记"。RAII 模式下 cleanup 是隐式的，当行为出乎预期时更难调试。
- **部分清理**。有时需要 Phase A 施加的某个效果延续到 Phase B（比如"中毒"跨阶段），RAII 作用域绑定使这种需求变得别扭——你需要把效果提升到更高的作用域，违背了"最小作用域"原则。
- **现实世界证明**。WoW Raid 脚本大量使用手动清理，运行了 20 年证明了可行性。

#### 立场 C：快照回滚

**代表**：Larian（BG3）的 undo system、部分 roguelike 的 save-state 机制

**最有力论点**：
- **终极安全网**。在 transition 前拍快照，如果出错可以完整回滚到 known-good state。这解决了 RAII 和手动方式都难以应对的"多个副作用交织、清理顺序不确定"问题。
- **对策划零要求**。策划不需要思考 cleanup——系统自动保证"要么全部生效，要么全部回滚"的事务语义。

---

### 分歧三：场景终止权 — 外部裁定（External Outcome）vs. 树内自决（Tree-Internal Termination）vs. 双层混合

**场景的生死由谁决定？**

#### 立场 A：外部裁定（Outcome Monitor 独立于执行树）

**代表**：本文档的 `outcomes` 设计、Bungie 的 Encounter Completion Conditions

**最有力论点**：
- **终止条件与执行逻辑正交**。"Boss 死了" 这个终止条件不应该被埋在某个 Phase 的某个 Sequence 的某个分支里。它应该是全局监控、始终生效的。这样无论流程走到哪一步，终止条件都能触发——不会出现"Boss 在过场动画中被击杀但脚本没处理"这种 bug。
- **复用性高**。同一棵执行树可以搭配不同的 outcome（计时挑战版、正常版、教学版），只需换 outcome 列表。

#### 立场 B：树内自决

**代表**：UE Behavior Tree 的 Success/Failure 冒泡、部分 Visual Scripting 系统

**最有力论点**：
- **因果关系更明确**。"做完这件事就结束" 比 "做完这件事 → 设个变量 → 外部 monitor 检测到变量 → 终止" 更直接。过度分离导致因果链断裂，调试时需要跨两个系统追踪。
- **简单场景的杀鸡牛刀问题**。一个线性演出（开门 → 走过去 → 对话 → 结束），用 outcome monitor 来裁定"Sequence 执行完毕"是不必要的复杂度。树走完就是场景结束，天然且直觉。

#### 立场 C：双层混合

**代表**：FFXIV Raid 系统、Destiny 2 部分高复杂度 Encounter

**最有力论点**：
- **日常流程让树自决，异常情况让外部裁定**。正常流程中 Phase 1 → 2 → 3 → Boss 死 → 树走完 → 场景结束，这是树内自决的自然流程。但"全员团灭"、"Boss 在过渡动画中被秒杀"、"超时"等异常由外部 monitor 兜底。这是最实用的工程妥协。

---

## AI对三大分歧的判断

### 对分歧一（执行模型）的判断：树为骨架，FSM 为内嵌节点，关键路径保留脚本逃生舱

**我的立场：本文档的 "Act Tree + 内嵌 StateMachine" 是目前的最优解，但应预留协程逃生舱。**

**理由**：

树的结构化清理能力是不可替代的工程优势。在生产环境中，"忘记清理"导致的 bug 远多于"表达能力不够"。FSM 的 Phase 互斥语义是真实需求，但它作为树的一个节点类型（本文档的做法）比作为顶层机制更好——因为你经常需要"Phase 内部还有并行的后台任务"，这在纯 FSM 中需要引入 Harel Statecharts 的复杂性，而在树中只需 `Parallel`。

但必须承认：极度复杂的时序逻辑（如 FFXIV 绝本的机制编排，多个定时器交叉、条件判断层层嵌套）在纯声明式树中会变成"节点爆炸"。对此应预留一个 `ScriptAct { code: string }` 节点作为逃生舱，允许在受控环境中使用协程脚本——但严格限定其 scope，要求实现 `onCleanUp`。

**各工作室的解决方案**：

| 工作室 | 方案 |
|:---|:---|
| **Bungie** | 混合架构：声明式 encounter 规则 + Lua 脚本处理复杂 edge case，但 Lua 脚本被限制在 sandbox 中运行 |
| **FromSoftware** | FSM 优先（ESD），但 Boss AI 内部用行为树。两层各有明确边界 |
| **miHoYo** | 图式编辑器（类似树），但节点内部实现是 C++ 协程。对策划暴露声明式，对程序员暴露协程 |
| **Epic Games** | BT + GAS AbilityTask（本质是协程）+ Blueprint Visual Scripting 三层并存 |
| **Naughty Dog** | 协程脚本优先，但用 scope token 系统强制结构化清理 |
| **Larian** | Osiris 脚本（规则引擎） + Timeline（声明式） + Story Editor（图），三者分工不同场景 |

---

### 对分歧二（副作用管理）的判断：RAII 作为默认机制，手动清理作为补充，快照作为最后安全网

**我的立场：本文档的作用域 RAII 是正确的默认选择，但需要为"跨作用域持久效果"提供显式语义。**

**理由**：

RAII 解决了 90% 的副作用清理问题，且是唯一能保证"不会忘记"的机制。但现实中确实存在"我希望这个 Debuff 从 Phase 1 持续到 Phase 3"的需求。对此，不应放弃 RAII 而回退到全手动——而是**提升作用域**或**引入显式的 persist 语义**。

具体方案：
1. 如果效果需要跨 Phase，将其 `WithStatus` 放在 `StateMachine` 的外层（而非某个 Phase 内部）
2. 如果效果需要比场景更持久，使用 `ApplyEffect { persist: true }`，由 GAS 而非场景管理其生命周期——这是"谁管理谁清理"原则的体现

快照回滚作为生产环境的 debug 工具极有价值，但作为正式的清理机制成本过高（内存、性能、状态一致性）。它更适合作为最后的安全网而非默认策略。

**各工作室的解决方案**：

| 工作室 | 方案 |
|:---|:---|
| **Naughty Dog** | 严格 scope token RAII，跨 scope 效果通过 "promoted token" 显式提升 |
| **Santa Monica** | RAII 为主，但允许显式标记 "outlive scope" 的效果，由上层 encounter wrapper 管理 |
| **Blizzard（WoW）** | 手动清理为主，但每个 Raid encounter 有一个全局 reset() 作为兜底——wipe 时调用以保证干净状态 |
| **Square Enix（FFXIV）** | 每个 Phase 的 onExit 手动清理 + encounter 级 reset() 兜底。出过多次"debuff 跨 phase 残留"的著名 bug |
| **Larian** | undo closure 系统（轻量快照），对复杂分支任务的回滚非常有效 |

---

### 对分歧三（终止权归属）的判断：外部裁定为主权机制，树的自然完成为便利语法糖

**我的立场：本文档"outcome 独立于树"的设计是正确的，但应增加一个隐式规则来处理简单场景。**

**理由**：

终止条件与执行逻辑的正交性是一个深刻的正确设计。当你把"Boss 死了"这个判断埋在 Phase 3 的 onEnter 脚本里时，你隐式假设了"Boss 只可能在 Phase 3 死亡"——但玩家总能找到方法打破你的假设（过高伤害跳阶段、bug 利用等）。外部 monitor 没有这个假设。

但对于简单的线性演出（如一段过场动画），强制要求配 outcome 是不必要的仪式感。应有一个隐式规则：

> **如果 `outcomes` 列表为空且 `rootAct` 自然完成（Succeeded），场景自动以 `resultCode: 1`（成功）终止。**

这使得简单场景零配置即可工作，复杂场景通过显式 outcome 获得完整控制。

**各工作室的解决方案**：

| 工作室 | 方案 |
|:---|:---|
| **Bungie** | Encounter 有显式的 Completion Condition 列表（外部裁定），但简单的 "完成所有 objective" 是默认 condition |
| **FromSoftware** | Boss 死亡由引擎级事件触发 encounter 结束——本质是外部裁定，但实现在引擎层而非脚本层 |
| **Blizzard（WoW）** | Raid Boss encounter 由服务器端 "boss death event" 触发结束，但 trash/event encounter 由脚本走完自动结束——双层混合 |
| **miHoYo** | Domain/Spiral Abyss 使用显式的 clear condition（杀光所有怪/时间内完成），但世界任务中的小演出走完即结束 |
| **Square Enix（FFXIV）** | Duty 级别有严格的外部裁定（Boss HP → 0、全员死亡、超时），Fate/Quest 场景较简单的用树完成作为终止 |
| **Naughty Dog** | Encounter 有 "completion criteria" 字段（外部），但 cutscene 走完即结束（树内自决）。两种模式共存 |

---

## 总结

本文档的设计在五大共识上完全对齐行业最佳实践，在三大分歧中做出了有明确理由的选择。