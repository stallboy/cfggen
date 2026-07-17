---
title: 运行时生命周期与编排
sidebar:
  order: 21
---

SceneInstance 与 Actor 这两个运行时核心对象的生命周期与编排设计基准。它与各业务域设计（[能力](./ability-design.md)、[AI](./ai-design.md)、[Cue](./cue-design.md)、[场景演出](./scene-design.md)）正交——业务域定义"做什么"，本文定义"对象如何存在、如何被驱动、如何被回收"。所有业务系统都跑在这套骨架之上。

---

## 一、设计哲学

三个贯穿全部对象的主张，是后续所有具体决策的根因：

1. **存在性和业务态正交** —— 运行时只回答"对象是否存在"，业务生死由 tag 表达。两者解耦，互不污染。
2. **被动数据 + 中央编排** —— 业务对象不自驱动，由唯一主循环按固定阶段驱动。散落的独立 update 循环是被禁止的。
3. **构造纪律即硬约束** —— 对象的构造/初始化/注入有严格顺序，任何代码只能读它**之前**就绪的状态。这不是风格偏好，而是消除一整类"时序潜伏 bug"的硬约束。


---

## 二、SceneInstance：组合根与编排者

**定位**：上帝对象拆解后的"收口点"。自身不含业务逻辑，只做两件事——**组装**三件套、按阶段**编排**一帧。

| 职责 | 承载者 | SceneInstance |
|---|---|---|
| 身份 / 查询 / Sweep 回收 | `ActorRegistry` | 持有，不实现 |
| 配置 / 时钟 / 服务接口 | `SceneServices` | 持有，不实现 |
| 子系统注册 + 分阶段调度 | `SystemHub` | 持有，不实现 |
| 组装 + Tick 编排 + 关闭编排 | SceneInstance 本身 | 实现 |

**为什么"持有而不实现"**：把身份、服务、调度各自收口到专门对象，SceneInstance 只保留"编排"这一最薄职责，避免它重新膨胀成上帝对象。公开 API 因此能收敛到极少几个动作。

```java
class SceneInstance {
    // 三件套：持有而不实现
    ActorRegistry actors;       // 身份 / 查询 / Sweep 回收
    SceneServices services;     // 配置 / 时钟 / 随机 / DevLog / CueOps
    SystemHub    systems;       // 子系统注册 + 分阶段调度

    // ── 启动期注入（唯一允许写入 services / 注册 system 的窗口）──
    void initSystem(MovementSystem m, ProjectileSystem p, CueOps cue);
    void bindDirector(SceneDirector director);   // 可选

    // ── 运行期 ──
    Actor createActor(Vector2 pos, DActorDefinition def, Action<Actor> configure);
    void  killActor(Actor actor);
    ReadOnlySpan<Actor> getAliveActors();
    void  tick(float dt);
    void  shutdown();
}
```

---

## 三、Actor：被动数据容器

**定位**：战斗实体的被动数据容器。自身不驱动时间，由主循环调用其 `tick`。一个 Actor 可以"挂着死亡 tag 但对象仍存活"——**业务死亡 ≠ 对象回收**。

**存在性状态机（三态）**：只表达"对象存在性"，不表达业务态。

```java
enum ActorLifecycle {
    Active,          // 存活，参与一切
    PendingDestroy,  // 已标记，本帧仍可查，不参与 tick
    Destroyed        // 已回收，引用失效
}
//   Active ──killActor──► PendingDestroy ──下帧 Sweep──► Destroyed
```

killActor 只做两件事——**标记 + 派发临终钩子**，不立即 cleanup，留待下帧 Sweep。

---

## 四、Actor 存在性 vs 业务生死

上一节的状态机只回答"对象存在性"，那业务生死归谁管？它是另一个正交维度，由 gameplaytag 表达，与存在性各自独立演进：

| 维度 | 表达方式 | 归属 |
|---|---|---|
| 对象存在性 | 三态状态机 | `ActorRegistry` |
| 业务生死 | gameplaytag（如 `Actor.Life.Dead`） | `TagComponent` + 业务 |

**推论**：HP 归零 ≠ 对象回收。

- HP 归零 → 资源系统授予 Dead tag，对象仍 `Active`（可复活、可继续被结算、可播死亡动画）。
- 只有业务判定"该 Actor 要真正消失"时，才走 `killActor` 把对象推入回收流程。

这一分离让"倒地 / 复活 / 尸体留存"等业务语义完全不进入对象生命周期管理。Registry 永远只回答一个问题——"这对象还在吗"，与此无关的一切交给 tag。`IsAlive` 因此有个语义陷阱：挂着 Dead tag 的 Actor `IsAlive == true`；是否参与战斗由调用点按 tag 判断，注册表只管存在性。

---

## 五、扩展点：组件层与系统层

业务系统如何接入这套骨架？有两条对称的扩展轴——**挂到单个 Actor 上**（组件层），或**全局订阅所有 Actor / 场景事件**（系统层）。两者是本框架对使用者暴露的全部扩展入口，集中在这一节声明。

### 5.1 两个扩展基类

```java
// 组件层：每个 Actor 自己的组件（owner 即所属 Actor）
abstract class ActorComponent {
    protected final Actor owner;

    void onCreated() {}                              // 出生：进注册表后
    void onPendingDestroy() {}                        // 临终：Active→PendingDestroy，旧态完整
    void onDestroyed() {}                            // 销毁：PendingDestroy→Destroyed 后
}

// 系统层：全局订阅所有 Actor 的生命周期（钩子名 = 组件层 + Actor 前缀）
abstract class SceneSystem {
    void onActorCreated(Actor a) {}                              // ← 对应 onCreated
    void onActorPendingDestroy(Actor a) {}                       // ← 对应 onPendingDestroy
    void onActorDestroyed(Actor a) {}                            // ← 对应 onDestroyed
    void onSceneShutdown() {}                        // 场景级关闭，非 per-actor
}
```

| 轴 | 基类 | 作用域 | 注册方式 | 典型例子 |
|---|---|---|---|---|
| 组件层 | `ActorComponent` | 单个 Actor | `addComponent` | 移动组件、AI 脑、自定义战斗组件 |
| 系统层 | `SceneSystem` | 全局 | `SystemHub.register` | 移动系统、投射物系统、导演 |

### 5.2 生命周期钩子

钩子挂在 Actor 状态机的**三个边界**上：出生、临终、销毁。

```
[Init] ──onCreated──► Active ──onPendingDestroy──► PendingDestroy ──onDestroyed──► Destroyed
        （出生）            （临终）                  （销毁）
```

**契约表**（统一参照系 = 状态机边界）：

| 边界 | 组件层 | 系统层 | 时机 | 此刻能读 | 该做 |
|---|---|---|---|---|---|
| 出生 | `onCreated` | `onActorCreated` | 进注册表后（OnCreated 派发链） | 兄弟组件、场景服务、兄弟 Actor | 订阅事件、获取资源、注册后端 |
| 临终 | `onPendingDestroy` | `onActorPendingDestroy` | Kill 时（本帧） | 完整旧态 | 掉落、计分、触发死亡 Cue |
| 销毁 | `onDestroyed` | `onActorDestroyed` | 下帧 Sweep 后 | —— | 释放资源、解绑监听、反注册后端 |

**为什么临终与销毁必须拆成两个钩子（核心硬约束）**：

根因不在"两个时间点方便读旧态"，而在于**两个钩子的调用上下文不同，因此安全契约不同**：

| | `onPendingDestroy`（临终） | `onDestroyed`（销毁） |
|---|---|---|
| 触发路径 | **仅** `kill` | 所有回收路径（含 shutdown 强清） |
| 此刻世界状态 | 正常运转 | **可能正在拆解** |
| 安全动作 | 有副作用（创建掉落物、写计分、播 Cue） | 纯资源释放 |

**shutdown 强清路径故意跳过临终钩子**。这不是疏忽，是用"钩子有没有被调用"本身编码了一条信息——

> 临终钩子被调用 ⇔ 这是一次正常死亡 ⇔ 世界安全，可以放手做有副作用的收尾。

合并任何一个钩子，这条信息就丢：
- 只留销毁钩子：掉落/计分挤进去，但销毁钩子在 shutdown 拆解世界时也会触发——在正在拆除的世界里 new 掉落物 Actor，灾难。
- 只留临终钩子：资源释放塞进去，违反"本帧仍可见"承诺——Actor 本帧仍在 `aliveSpan` 能被别的系统查到，但它组件的资源已释放，别人查到再用即崩溃。

所以两个钩子职责正交——**业务收尾 vs 资源释放**，不是同一件事切两半。

### 5.3 Tick opt-in

生命周期钩子是"响应事件"，tick 是"被每帧驱动"——两个能力正交。需被驱动的扩展点**另外**实现 tick 接口（opt-in），不实现则不被每帧调用。

```java
// 组件层：实现即自动进 Actor 的扩展 tick 列表
interface ITickable { void tick(float dt); }

// 系统层：需要 tick 的子系统 opt-in（与组件层 ITickable 对称）
interface ISceneTickable {
    SceneTickPhase phase();
    void tick(SceneInstance scene, float dt);
}
```

- 组件实现 `ITickable` → 自动进 Actor 的扩展 tick 列表（按注册顺序，无排序）。
- 系统实现 `ISceneTickable` → 按 `phase()` 注册到主循环对应阶段。
- 一个只响应生命周期的系统无需声明 `phase`——钩子与 tick 两个能力各自 opt-in。

阶段语义在 tick 分阶段编排一节。**例外**：`AIBrainComponent` 是组件却**不**实现 `ITickable`，其 tick 由 Actor 字段直访保 AI-first 顺序，避免进扩展 tick 列表被乱序——热路径顺序特例，理由在组件模型分层一节。

### 5.4 注入方式

- **组件**：在 `createActor` 的 configure 回调里 `addComponent`。这是唯一正确窗口，理由是注入时机的契约——子系统在出生钩子里读组件状态做自动注册，若组件在工厂返回后才赋值，钩子拿到空值，后端永不注册。
- **系统**：在启动期通过 `SystemHub.register` 注册，服务冻结前完成。

---

## 六、Actor 初始化

### 四原则

1. **ctor 只赋字段** —— 不调业务方法、不读兄弟组件、不注册副作用。
2. **副作用进显式 Init / 钩子** —— 配置初始化走 `initFromDefinition`，事件订阅/资源获取走进出生钩子 `onCreated`，资源释放走销毁钩子 `onDestroyed`。
3. **注入走单一编排点** —— Actor 扩展组件进创建工厂的 configure 回调；子系统进 `SystemHub.register`。
4. **严格顺序**：ctor → Init → configure → OnCreated → Tick。

> 第 4 条是其余三条的验收标准。一段代码读到的状态，必须是它执行之前就已就绪的；违反即潜伏 bug。

### 就绪时间线

四原则投影成下面两条时间线，把"何时可读何物"写成可检查的契约：

```
[SceneInstance]
  组合根就绪 ──► 注入子系统 ──► [挂导演] ──► 首次 Tick

[Actor]
  ctor（空壳）──► 配置初始化 ──► 注入扩展组件 ──► OnCreated ──► 首次 Tick
                                                                  │
  killActor ──► PendingDestroy（本帧临终钩子，仍可查、不 Tick）
         ──下帧 Sweep──► Destroyed（销毁钩子，引用失效）
```

**何时能读何物**（贯穿所有对象，所有反模式都归结为"在状态就绪之前读了它"）：

- OnCreated 之前，只读 ctor 设的字段 + configure 注入的组件。
- OnCreated 之后，才能读兄弟对象、查场景服务、跑业务。
- 临终钩子之后，只读旧态做收尾（掉落/计分），不改未来态。
- 销毁钩子之后，引用不应再被持有。

---

## 七、SceneInstance.Tick 分阶段编排

主循环是驱动世界的唯一入口，按固定阶段顺序执行。**任何一段的产物只能被其后的段消费**——这是单向数据流约束，颠倒即出错。

```java
void tick(float dt) {
    if (isShutdown) return;                       // 0. 关闭早退

    actors.sweepPendingDestroy();                 // 1. Sweep（必须在暂停判断之前）
    if (services.time.paused) { advanceFrame(); return; }  // 暂停：只推帧计数，业务不跑

    float scaled = dt * services.time.timeScale;
    services.time.advance(scaled);

    systems.tick(this, SceneTickPhase.Pre,  scaled);   // 2. 全局前置：输入采样、规则广播
    for (Actor a : actors.aliveSpan) a.tick(scaled);   // 3. Actor 主体：AI/Status/Ability/Stat/Tag
    systems.tick(this, SceneTickPhase.Main, scaled);   // 4. 主推进：Movement 积分、导演 Act 树
    systems.tick(this, SceneTickPhase.Post, scaled);   // 5. 解算：碰撞、命中、投射物回收
    systems.tick(this, SceneTickPhase.Presentation, scaled);  // 6. 表现：Cue flush
}
```

阶段划分的本质是**单向数据流契约**：每个 Phase 只能读前序 Phase 已落定的状态，不能颠倒。否则后写的数据被前序 Phase 当成输入，逻辑错乱。

```java
enum SceneTickPhase {
    Pre,           // 全局前置：必须在任何 Actor 决策前（输入采样、规则广播）
    Main,          // 主推进：Actor.tick 已跑完（Movement 积分、SceneDirector）
    Post,          // 解算：读 Main 写完的位置（碰撞、命中、投射物回收）
    Presentation   // 表现：逻辑全结算完（Cue flush）
}
```

---

## 八、SceneInstance.Shutdown 两阶段

场景级关闭是资产释放的统一入口，分两阶段：

```java
void shutdown() {
    if (isShutdown) return; // 幂等
    isShutdown = true;

    systems.notifySceneShutdown();  // 阶段 1：子系统清【非 Actor 资产】
    // 导演中止 Act 树 / Cue 资产池强清 / 移动后端清全局态

    actors.clear();  // 阶段 2：所有残留 Actor 一次性不可逆回收
    // per-actor cleanup 链（含 onActorDestroyed）
}
```

在 Tick 内调用 `shutdown` 时，会自动延迟到本帧末尾兑现——避免重入正在遍历的系统/Actor 列表。调用方无需区分自己处于 Tick 内外，一律调 `shutdown` 即可。

阶段 1 派发的 `onSceneShutdown` 是系统层独有的场景级钩子（已在两个扩展基类里声明），不挂在 per-actor 状态机上。

---

## 九、Actor 组件分层

热路径性能要求决定了组件必须分两层，而非统一抽象：

```java
class Actor {
    final SceneInstance scene;
    final int actorId;
    final Vector2 position;

    // ── 核心组件：ctor 内创建，永远存在，字段直访（热路径无字典查找，可内联）──
    final EventBus      eventBus;
    final TagComponent   tagComponent;
    final StatComponent  statComponent;
    final ResourceComponent resourceComponent;
    final StatusComponent statusComponent;
    final AbilityComponent  abilityComponent;

    // ── 字段直访的可选组件（外部注入，可空）──
    MovementComponent movementComponent;
    AIBrainComponent  aiBrainComponent;
    CueComponent      cueComponent;

    // ── 扩展组件注册表（按类型索引）──
    <T extends ActorComponent> T addComponent(T c);   // 同类型覆盖
    <T extends ActorComponent> T getComponent(Class<T> type);
    boolean hasComponent(Class<T> type);
}
```

| 层 | 创建 | 访问 | 回收 |
|---|---|---|---|
| 核心组件 | ctor 内创建，永远存在 | 字段直访（无字典查找，可内联） | 手工 cleanup |
| 扩展组件 | configure 回调注册 | 注册表按类型查 | `onDestroyed` 钩子 |

**设计取舍**：核心组件为保热路径字段直访，故意不进统一组件基类，因此回收走手工调用而非钩子——两种入口，同一件事（释放资源）。扩展组件则享受统一基类带来的 `onCreated` / `onDestroyed` 钩子便利，代价是字典查找。这是"热路径零开销 vs 扩展便利"的显式权衡，而非遗漏。
