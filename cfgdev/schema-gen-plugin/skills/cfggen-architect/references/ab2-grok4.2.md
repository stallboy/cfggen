**✅ 评估完成**

### 原设计整体评价

这份文档质量**极高**，是目前我见过最接近工业级、同时又充分吸收了 Unreal GAS 精髓的中文技能系统设计。它在以下几个维度做得非常出色：

- **哲学高度**：真正理解了“标签驱动 + 事件总线 + 逻辑/表现解耦 + 原子组合”这四个核心现代范式。
- **数据驱动彻底性**：几乎所有逻辑都被配置化，硬编码空间被压缩到极致。
- **前瞻性**：两阶段提交（Pre/Post）、Context 快照、写入时展开的 TagContainer、SafeList 等实现细节都体现了很强的工程意识。

---

### 需要改进的核心点

1. **文档结构过于平铺**，层级感不强，阅读体验可优化。
2. **概念边界模糊**：`Effect` 被过度使用，既是原子指令，又是复合容器，语义不够纯粹。
3. **缺少“执行阶段”概念**（Activation → Execution → End），导致复杂技能（如蓄力、引导、连招）表达困难。
4. **Ability 和 Status 的关系**没有完全理顺（被动技能、装备技能等边缘情况）。
5. **缺少“任务系统”（AbilityTask）**，这是 GAS 最精华的部分之一，用于处理长时间、异步、多阶段行为。
6. **Modifier 系统可以进一步抽象**，让 StatModifier 和 PayloadModifier 统一。
7. **配置表组织**可以更模块化，便于后续扩展。

---

## 【新版】能力系统设计文档 v2.0

---

### 1. 设计哲学（Philosophy）

本系统旨在构建一套**高内聚、低耦合、全数据驱动、可热更新**的现代游戏能力系统，核心原则如下：

1. **标签即真理**：所有交互（互斥、免疫、分类、过滤）仅通过严格树状 `GameplayTag` 进行。
2. **事件驱动 + 两阶段提交**：所有逻辑通过 EventBus 通信，Pre 阶段修改 Payload，Post 阶段处理副作用。
3. **逻辑与表现彻底解耦**：服务器只发送 Cue 事件，客户端独立映射表现。
4. **正交原子组合**：拒绝深继承，使用组合（Ability + Effect + Status + Task）构建复杂机制。
5. **执行阶段化**：任何复杂能力都应清晰分为 **Activation / Execution / End** 三个阶段。
6. **上下文驱动**：所有表达式和行为均依赖 `Context`，实现强隔离与可预测性。

---

### 2. 核心概念（Core Concepts）

#### 2.1 GameplayTag（核心语言）
- 采用严格树状结构（如 `State.Debuff.Control.Stun`）。
- 支持“父标签包含查询”和“精确标签匹配”。
- 所有规则、过滤、分类均依赖 Tag。

#### 2.2 Stat（属性系统）
统一管理所有数值，包括基础属性、资源、战斗数值等。

#### 2.3 Event 与 Payload（通信标准）
所有逻辑交互必须通过标准化事件进行。

#### 2.4 Context（执行上下文）
贯穿整个系统的“快照 + 作用域”容器，是表达式和行为求值的唯一依赖。

---

### 3. 配置表总览（Configuration Tables）

#### 3.1 基础字典表

```cfg
table gameplaytag[name] { ... }           // Tag 字典
table stat_definition[statTag] { ... }    // 属性定义
table event_definition[eventTag] { ... }  // 事件定义
```

#### 3.2 核心实体表

```cfg
table ability[id] (json)           // 能力定义（主动/被动/普攻等）
table status[id] (json)            // 标准状态定义
table shared_effect[id] (json)     // 可复用的 Effect 片段
table client_cue_registry[cueTag]  // 客户端表现映射（仅客户端）
```

#### 3.3 全局规则表

```cfg
table global_tag_rules[name]       // 标签交互最高法则
table global_combat_settings[name] // 伤害/治疗结算管线
```

---

### 4. 运行时核心架构（Runtime Architecture）

- **Actor** 拥有以下核心组件：
  - `TagContainer`
  - `StatComponent`
  - `StatusComponent`
  - `AbilityComponent`
  - `EventBus`

- **Context**（核心数据结构）
- **Payload** + **ChangeSet**（两阶段修改）
- **SafeList<T>**（所有动态集合的统一容器，防重入）

---

### 5. 表达式层（Expression Layer）

```cfg
interface TargetSelector { ... }   // 目标选取（Context / Payload / Scan）
interface FloatValue { ... }       // 数值表达式（支持运算、属性引用、上下文变量）
interface Condition { ... }        // 条件表达式
```

新增重要类型：
- `TargetScan`（AOE、扇形、链式等）
- `TargetFilter`（阵营 + Tag + 排除 + 排序）

---

### 6. 核心实体（Core Entities）

#### 6.1 Ability（能力）

```cfg
table ability[id] (json) {
    id: int;
    name: text;
    tags: list<str> ->gameplaytag;           // 自身标签
    
    activationPolicy: ActivationPolicy;      // 主动/被动/事件触发
    activationBlockedTags: list<str>;
    activationRequiredTags: list<str>;
    
    phases: AbilityPhases;                   // 【重要新增】
    
    costs: list<StatCost>;
    cooldowns: list<TagCooldown>;
}
```

**新增概念：AbilityPhases**
- `ActivationPhase`（前置检查、消耗、进入冷却）
- `ExecutionPhase`（可包含多个 Task）
- `EndPhase`（清理、后摇、触发结束事件）

#### 6.2 Effect（原子指令）

**重新定义**：Effect 是**无状态、瞬间执行**的原子操作指令。

主要类型：
- `ModifyStat`, `Damage`, `Heal`
- `ApplyStatus`, `ApplyStatusInline`, `GrantTemporaryTags`
- `SendEvent`
- `SpawnProjectile`, `SpawnArea`
- `WithTarget`, `WithTargets`, `WithLocalVar`
- `Sequence`, `Conditional`, `Repeat`
- `ModifyPayloadMagnitude/Extra`

#### 6.3 Status（持续状态）

```cfg
table status[id] (json) {
    id: int;
    stackingPolicy: StackingPolicy;
    core: StatusCore;
}

struct StatusCore {
    duration: FloatValue;
    grantedTags: list<str>;
    blockTags: list<str>;
    cancelTags: list<str>;
    cuesWhileActive: list<str> ->gameplaytag;
    behaviors: list<Behavior>;
}
```

**Behavior** 类型：
- `StatModifierBehavior`
- `PeriodicBehavior`
- `TriggerBehavior`（最重要）

#### 6.4 AbilityTask（新增核心概念）

用于处理**长时间、多阶段、异步**的行为（蓄力、引导、移动、动画同步等），极大提升表达力。

---

### 7. 全局规则（Global Rules）

#### 7.1 global_tag_rules
定义标签之间的**物理法则**（阻挡、打断、免疫）。

#### 7.2 global_combat_settings
定义伤害/治疗的**分层结算管线**（护盾 → 护甲 → 生命等），支持溢出、破盾特效等。

---

### 8. Gameplay Cue 系统（表现层）

客户端完全独立的表现系统，仅通过 Cue Tag 驱动。

- `InstantCue`：瞬间表现（受击、爆炸）
- `SustainedCue`：持续表现（跟随状态生命周期）

引擎自动根据上下文推导 `Added / Executed / Removed` 三种事件。

---

### 9. 关键改进总结（v2.0 相对于原版）

1. **引入 AbilityPhases**，解决复杂技能的阶段管理问题。
2. **引入 AbilityTask**，大幅提升对蓄力、引导、连招等机制的支持。
3. **明确 Effect 的原子性**，避免语义过载。
4. **结构重新组织**，可读性和扩展性大幅提升。
5. **增加 Projectile 和 Area** 作为一等公民（可独立配置）。
6. **Modifier 系统更统一**。
7. **配置表更模块化**，便于多人协作和后续扩展。

---

**后续可继续扩展的方向**（v2.1 预告）：
- Ability 实例化后的热更新支持
- 客户端预测与回滚框架
- 行为树风格的复合 Ability 编辑器
- 跨技能的“连招系统”抽象

---

需要我立即输出**完整重构后的详细设计文档**（包含所有配置表完整定义、伪代码、详细示例）吗？还是先针对某个具体模块（如 AbilityPhases + Task 系统）进行深化？