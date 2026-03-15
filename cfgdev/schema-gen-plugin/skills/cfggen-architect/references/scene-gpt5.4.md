从“**这是一个供具体项目裁剪演化的架构基线**”这个角度看，这份 `Scene Logic System Design` 的质量是**很高的**，而且比很多“过于像任务脚本编辑器说明书”的文档更成熟。它不是在重新发明战斗、AI、过场三套系统，而是在认真回答一个真正的工程问题：

> 当一个场景需要跨多个 Actor、跨一段时间、跨多个系统地协调动作时，应该由谁来编排，如何保证生命周期安全，如何避免和 GAS / AI 打架？

这份文档的答案总体是对的，而且很有工程意识。

下面我不纠结字段细节，而是从**架构思想是否稳、是否容易被具体项目吸收、哪些地方需要在“原则层”再收一收**来评价。

---

# 一、总体评价

## 可以给出的总体结论

这份场景系统设计的核心方向是：

- **把 Scene 定位成编排层，而不是逻辑替代层**
- **把执行模型定位成“可并发的运行时树”**
- **把等待与推进建立在事件订阅上，而不是每帧轮询**
- **把副作用纳入作用域清理**
- **把和 AI / GAS 的边界明确化**

这几个判断都很成熟。

如果前两份文档（Ability / AI）解决的是：
- 单个能力如何执行
- 单个 AI 如何决策

那么这份文档解决的是：
- **一段“时空跨度更长”的多方协同过程如何被可靠地组织**

这个定位是准确的。

---

# 二、这份设计最值得肯定的地方

---

## 1. “复用，不重造”是最重要的正确决策

这是整份文档里最核心、也最值钱的一条原则。

场景系统最容易走偏的方向，就是逐渐长成第四套逻辑系统：

- 自己有一套伤害节点
- 自己有一套 Buff 节点
- 自己有一套 AI 驱动节点
- 自己有一套事件机制
- 最后和 Ability / AI / Quest / Cutscene 系统相互重叠

你的文档明确说：

- 战斗逻辑复用 GAS 的 `Effect` / `Ability`
- AI 改行为池通过 `AIBehaviorModifier`
- Scene 只负责“编排”

这个定位非常健康。  
它保证 Scene 不会逐渐腐化成“万能导演系统”。

这是很成熟的系统边界意识。

---

## 2. “执行树而不是执行栈”是合理且必要的

这个判断非常对，而且和 AI 文档形成了很好的对照。

AI 用执行栈是对的，因为它本质是：
- 单线程意图推进
- 当前只需要推进一条活跃路径
- Parallel 是例外，需要专门处理

而 Scene 脚本天然存在大量：
- 并行等待
- 多路监听
- 一个子线成功后终止其他子线
- 多个长生命周期节点并存

所以 Scene 用树模型更自然。  
尤其你明确提出：

> 复合节点与子节点共存于树中，每帧只 tick Running 节点

这说明你不是拿行为树那套“递归遍历整棵树”过来硬套，而是想做一个**显式生命周期管理的执行树**。这点很好。

---

## 3. “订阅驱动，非轮询”抓住了 Scene 系统最容易烂掉的根因

这一条我认为是这份文档最强的工程意识体现。

传统场景脚本 / 任务脚本系统最常见的问题就是：

- 大量 `WaitUntil`
- 大量全局状态判断
- 大量每帧条件重算
- 条件值从各种系统取，调试时完全不知道是谁改了什么

你这里明确要求：

- `WaitForEvent` 通过订阅推进
- `WaitUntil` 也要求声明 `reactToEvents`
- `onEnter` 订阅，`onExit` 取消订阅
- 条件不是每帧瞎轮询，而是由事件驱动重评估

这是非常正确的。

从参考文档角度，我甚至认为这一条还可以再强调成一句硬原则：

> **Scene 系统中的等待，本质上应建模为“等待某个事实变化事件”，而不是“等待表达式下一次碰巧变真”。**

这是这个系统和传统任务机最大的分水岭。

---

## 4. “作用域即生命周期”贯彻得很好

这是整个系列文档中最统一、最强的一条哲学。

在 Scene 文档里，它比 Ability / AI 里体现得更直接，因为 Scene 更容易产生“编排型副作用”：

- 临时夺权
- 临时状态
- 临时局部变量
- 临时镜头
- 临时监听
- 临时生成物 / 临时 Actor 引用

你明确要求：

- 有副作用的节点必须包在作用域里
- `onExit` 必须 finally 语义
- 不管成功、失败、外部 abort，都清理

这会极大降低项目后期最难查的那类 bug：
- 镜头没还原
- AI 没恢复
- 状态没移除
- 监听没退订
- 变量脏留

这是一个很成熟的基础原则。

---

## 5. `await` 显式声明是一个特别好的设计

这个点非常值得肯定。

很多项目的剧情 / 场景系统最大的问题，就是“节点到底阻不阻塞”靠经验猜：

- 播动画默认等不等？
- 播对白默认等不等？
- 放技能默认等不等？
- 切镜头默认等不等？
- 移动默认等不等？

这种“隐式 await”是灾难源头。

你这里用：

- `Immediate`
- `UntilComplete`

把它显式化，这非常对。

更重要的是你强调：
- 完成依赖事件，不依赖猜时长

这比单纯加个 `waitForCompletion: bool` 更成熟，因为它把“等待”的根基绑定到了统一事件系统上。

---

## 6. 统一 outcome 终止语义是个好方向

你把：
- 失败
- 胜利
- 超时
- 多目标评分中间结果

都纳入 `outcomes`，而不是再搞一个平行的 `abortConditions + timeLimit + resultTable` 体系，这个方向是正确的。

这件事的价值不在于“少几个字段”，而在于：

> **Scene 的终止从此成为一个统一建模的问题，而不是几个分散机制拼起来的结果。**

这会让后续项目团队更容易建立一致认知。

---

# 三、从“架构基线”的角度，真正需要改进的地方

下面的建议不是补字段，而是调整**原则表达和系统边界**。

---

## 1. 需要更明确地区分：Scene 是“编排系统”，不是“通用任务逻辑语言”

这是我认为这份文档最需要再收束的一点。

当前文档已经强调“复用，不重造”，但从节点集合和例子规模来看，Scene 系统仍然有长成“全能脚本语言”的风险。

风险体现为：

- 有控制流
- 有局部变量
- 有循环
- 有条件
- 有并发
- 有副作用作用域
- 有 Actor 生成
- 有 UI
- 有 Camera
- 有 Dialogue
- 有战斗触发
- 有 AI 控制

如果再继续扩展，很容易变成：
> “凡是游戏中跨帧的事都往 Scene 里写”

这会让 Scene 变成一个过大的中心系统。

### 建议

在 Philosophy 里增加一条更明确的边界声明：

> Scene 系统只负责**有限时长、明确边界、跨系统协同**的流程编排；不承担长期常驻规则、不承担通用任务状态机、不替代任务系统或关卡脚本系统。

换句话说，Scene 更适合：
- Boss 战流程
- 剧情段落
- 护送流程
- 机关演出
- PVE 房间战斗阶段

而不适合：
- 整个主线任务状态树
- 城市场景常驻民众行为
- 全地图规则管理
- 长生命周期世界模拟

这条边界如果不提前讲清，后面具体项目很容易滥用。

---

## 2. 需要补一层“Scene 适用范围”的分级思想

这份文档现在默认所有“场景逻辑”都可以用一套 SceneDefinition 表示。  
但从参考基线角度，更推荐你引导项目做分层使用。

### 建议增加一个架构建议

可以在文档里明确：

**并不是所有场景流程都应该用同一重量级 Scene 系统。**

建议项目区分三类：

### A. 轻量触发型
- 开门
- 小机关
- 一句对白
- 一次提示

这类可以由更轻量的关卡触发器系统处理，不一定进入完整 SceneInstance。

### B. 中等流程型
- 小型战斗遭遇
- 护送
- 宝箱开启流程
- 小剧情片段

适合用 Scene 系统。

### C. 重型导演型
- Boss 战阶段
- 长剧情演出
- 多 Actor 配合
- 复杂镜头调度

适合用 Scene 系统 + 外部导演工具 / Cutscene 系统协同。

这样你就能避免 Scene 被理解成“所有流程都往这里装”。

---

## 3. “订阅驱动，非轮询”这个原则应再增加一条现实约束

你这个原则很正确，但如果作为项目基线文档，我建议你再加一句“**订阅优先，不绝对排斥局部轮询**”。

为什么？

因为现实项目里有一些条件天然不是事件友好的：

- 某个 Actor 与区域中心的距离持续变化
- 某个镜头 blend 状态
- 导航剩余距离
- 物理接触是否成立
- 某些第三方系统根本不发细粒度事件

如果文档把“非轮询”写得过死，落地团队可能会误以为：
- 一切都必须事件化
- 事件化不了就硬造事件
- 最后反而把系统搞复杂

### 更平衡的表达建议

可以改成：

> 等待节点默认采用订阅驱动；只有当条件无法被稳定映射为有限事件集合时，才允许使用局部受控轮询，并要求明确标识其成本与原因。

这样能保住你“不要全局瞎轮询”的初心，同时给具体项目留口子。

---

## 4. Outcome 统一终止语义是好事，但要防止“结果系统吞掉流程控制”

这是个架构层提醒。

你把所有终止路径都塞到 `outcomes`，这很好；但也有一个潜在副作用：

> 团队可能会把“流程推进条件”和“最终结局条件”都混写进 outcome，导致 scene script 本身失去叙事主线。

比如：
- 某阶段结束条件
- 某支线成功条件
- 某计分节点
- 某阶段失败条件
- 最终结局

如果都统一叫 outcome，概念上会略混。

### 建议

在文档层把“结局”分成两种语义：

- **Terminal Outcome**：真正导致 Scene 完结
- **Milestone Outcome**：中途记录、计分、埋点，不终止

你现在已经通过 `terminateScene` 表达这个区别了，这在结构上够了。  
但建议在概念层明确强调：

> outcomes 是统一的“结果判定机制”，不等于所有流程控制都应该放进 outcomes。  
> 主线推进仍应优先由 script 自身表达，outcome 更适合描述场景层级的结果裁决。

这句话能避免脚本逻辑和结局判定逻辑混成一团。

---

## 5. Scene 与 Quest / LevelScript / Cutscene 的边界还可以再明确

这份文档很强调和 GAS / AI 的边界，但对其它外围系统的边界还不够明确。  
而实际上 Scene 系统最容易产生重叠的往往不是 GAS，而是：

- **任务系统**
- **关卡脚本系统**
- **镜头 / 过场导演系统**
- **UI 引导系统**

### 建议在文档中补一段“系统边界建议”

例如：

- **Quest System**：负责长期任务状态、存档、任务链推进、奖励归属
- **Scene Logic**：负责一次有限场景流程的运行时编排
- **Level Script**：负责关卡内低层触发、区域事件、一次性机关响应
- **Cutscene System**：负责高保真镜头轨道、表演资源、Timeline 驱动
- **Scene Logic** 通过动作节点去调用这些系统，而不吞并它们

这会极大帮助具体项目避免系统重复建设。

---

## 6. “TakeActorControl” 是很强的设计，但应该强调它是“昂贵能力”

这个节点设计得很好，尤其你分了：

- Immediate
- Polite

而且明确了如何与 AI、GAS 联动。

但从架构原则上，我建议再补一条使用建议：

> **夺权应被视为高成本操作，只在确有必要时使用。**

因为一旦项目团队觉得“反正 Scene 可以接管 Actor”，就会出现：
- 所有剧情段都先 TakeActorControl
- 所有战斗过场都先让 AI 宕机
- 所有复杂行为都不用 AI 了，直接 Scene 驱动

这会伤害你前面 AI 系统的价值。

### 建议在文档中强调

优先级建议：

1. **优先自治**：能靠 AI/Ability 自己完成的，不要夺权
2. **优先软夺权**：需要引导演出但允许自然过渡时，优先 Polite
3. **最后硬夺权**：只有对白、硬镜头、严格姿态锁定等场景才用 Immediate

这样更符合你整套架构的“协同而非替代”哲学。

---

## 7. 执行树设计很好，但还需要一句“Scene 不是行为树”

虽然你说了“执行树，非执行栈”，但对于阅读者来说，还是很容易把它脑补成行为树 / 任务树。

建议在文档里更明确地点出：

> SceneAction 树的职责是**运行时编排与生命周期管理**，而不是 AI 式的选择决策树。  
> 它不承担择优、算分、黑板共享控制等职责，其树结构只用于表达顺序、并行、作用域与等待关系。

这句话能帮项目团队正确理解：
- AI 树是“决策结构”
- Scene 树是“编排结构”

两者虽然都有树，但不是一类东西。

---

## 8. 场景变量体系建议再强调“少而克制”

你这里用了 `sceneVars`，而且复用 GAS 的 `Store`。这个方向本身是好的。  
但这类变量容器往往会成为脚本系统退化成“全局状态机”的入口。

### 建议在 Philosophy 中补一句使用原则

> `sceneVars` 仅用于承载场景实例内部、无法自然归属到 Actor / GAS / AI 的少量编排态数据；凡可归属到 Actor 状态、GAS Status、AI Goal/Behavior 的信息，不应回流到场景变量。

例如：
- Boss 当前阶段 → 尽量用 GAS tag/status 表达
- 某角色是否被控制 → 用 Tag / Scope 表达
- AI 当前目标 → 属于 AI，不属于 sceneVars
- 仅用于场景计数/脚本桥接的小量数据 → 才放 sceneVars

这条原则很重要，否则 sceneVars 很容易变成 ECA 时代“全局变量池”的翻版。

---

# 四、从三份文档整体一致性的角度看，这份 Scene 文档的优点

这个文档最大的附加价值，在于它和 Ability / AI 文档形成了**一致的系统观**：

| 维度 | Ability | AI | Scene |
|------|--------|----|-------|
| 核心定位 | 原子战斗逻辑 | 自主决策 | 跨系统编排 |
| 驱动方式 | Context + Event | Goal + Score | Action Tree + Event |
| 生命周期管理 | Status / Trigger | BehaviorInstance | Scope ActionContext |
| 表现解耦 | Cue | 通过 Task 调表现 | 调用外部表现/对白/镜头 |
| 状态清理 | Status 生命周期 | 行为切换 / Abort | finally 语义作用域 |

这说明三套系统不是各自独立拍脑袋设计的，而是共享同一种哲学：
- 事件驱动
- 生命周期显式
- 状态作用域化
- 数据驱动
- 系统边界清晰

这是很难得的。

---

# 五、如果把它作为“项目模板文档”，我建议增加的三段内容

不是补字段，而是补三段**指导性文字**：

---

## 建议新增 1：系统边界章节
明确 Scene 与：
- Quest
- LevelScript
- Cutscene
- UI Flow
- GAS / AI

各自负责什么，不负责什么。

---

## 建议新增 2：使用分级建议
告诉团队：
- 什么适合 Scene
- 什么不适合 Scene
- 什么应该交给更轻量或更重型的系统

---

## 建议新增 3：成本意识章节
明确以下能力是高成本手段，应克制使用：
- `TakeActorControl`
- 大量 `Parallel`
- 大量 `Loop + WaitForEvent`
- 广泛使用 `sceneVars`
- 把流程推进过度写入 `outcomes`

这会让这份文档更像“工业设计基线”，而不只是“一个很强大的脚本 DSL”。

---

# 六、总评

如果从“**这是不是一份值得作为项目母版继续裁剪**”这个角度评价：

## 结论
**是，而且质量很高。**

它最强的地方不是节点多，而是以下几个架构判断很稳：

- Scene 只做编排，不重造战斗/AI
- 执行模型适合并发协同
- 等待由订阅驱动
- 副作用必须作用域化
- await 显式声明
- 终止路径统一裁决
- 与 GAS / AI 的边界清晰

## 最需要修改的，不是结构细节，而是再补强三类“原则层说明”：

1. **边界说明**：Scene 不是什么、不要拿来做什么
2. **分级使用建议**：不是所有流程都该进入 Scene
3. **成本意识**：夺权、变量、并行、循环这些能力都要克制使用

---

如果你愿意，我下一步可以继续做两种事情之一：

1. **把 Ability / AI / Scene 三份文档的共同哲学抽出来，整理成一份统一的《Runtime Design Principles》**
2. **专门帮你写一版“Scene 文档修订建议稿”，不改 schema，只改 Philosophy 和概念说明，让它更像工业标准文档**

我更推荐第 1 个，因为你这三份文档已经快形成一套完整的方法论了。



# 场景逻辑系统设计 (Scene Logic System Design)

本文档定义一套与 `ability-design` 和 `ai-design` 无缝集成的现代场景逻辑系统设计。它解决的核心问题是：**如何以数据驱动的方式，编排多实体在一段时间内的协同行为序列，并与 GAS 和 AI 系统深度联动，同时保证生命周期安全、终止语义统一、失败可控。**

本系统的定位是：**单场景内部的编排层，而非平行于战斗或 AI 的新逻辑层**。

---

## Philosophy

1. **复用，不重造**  
   `SceneAction` 的执行单元直接调用 GAS 的 `Effect`、`Ability`，AI 的行为修饰通过 `AIBehaviorModifier` 完成。场景系统不引入平行的战斗逻辑体系，只做编排。

2. **执行树，非执行栈**  
   所有动作节点构成一棵 **ActionContext 树**。复合节点（`Sequence`、`Parallel`、`Loop`）与子节点共存于树中，每帧只 tick 处于 `Running` 状态的节点。  
   这使 `Parallel` 的并发语义、作用域嵌套清理与分支中止都可被自然表达。

3. **订阅驱动优先，轮询作为保底退化**  
   所有等待类节点（`WaitForEvent`、`WaitUntil`）优先采用 EventBus 订阅机制驱动重新评估，而不是每帧盲目轮询。  
   但事件声明的完备性在复杂项目中未必总能被人工保证，因此：
   - `reactToEvents` 的主要作用是**性能优化与依赖提示**
   - 当条件依赖源无法穷举时，系统允许退化为**定频检查**
   - 编辑器应对“未声明 reactToEvents 的复杂条件”给出性能警告，而不是牺牲正确性

4. **作用域即生命周期**  
   所有有副作用的操作（占用 Actor 控制权、挂载临时 Status、声明局部变量、注入临时 AI 行为）都必须包装在对应的作用域节点中。作用域节点的 `onExit` 在 `finally` 语义下执行——无论正常完成还是被外部中止，清理动作一定发生，从根本上杜绝状态残留（Orphaned State）。

5. **await 语义显式声明**  
   `PlayAnimation`、`Dialogue`、`MoveTo`、`CastAbility` 等动作节点默认是 fire-and-forget（发出指令立刻返回）。如需等待完成，必须在配表中显式声明 `await: UntilComplete`，引擎通过 EventBus 订阅完成事件实现，而非硬编码时长。  
   这解决的是“用固定时长错误地代理完成事件”的问题。  
   **若等待的语义本来就是纯时间停顿，则 `WaitSeconds` 是合法且推荐的表达。**

6. **结局统一终止语义**  
   场景的所有终止路径（Boss 死亡、玩家死亡、超时、脚本自然结束）统一通过 `outcomes` 体系收口，不再设立 `abortConditions` 和 `timeLimit` 等平行字段，避免语义重叠。

7. **单场景原子性**  
   本系统定义的是**单个场景内部**的编排逻辑。场景对外表现为一个具有明确开始、运行、结束和结局的原子单元。  
   场景之间的链式触发、嵌套、跨场景中断恢复，属于更上层的任务系统、关卡导演系统或副本管理系统职责，不在本文档范围内。  
   场景通过 `broadcastEvent` 向外输出结果，外部系统通过 `trigger` 或显式 API 激活/关闭场景。

8. **失败必须可设计，不默认等价于全局崩溃**  
   场景编排中的局部动作失败（动画缺失、Ability 被拦截、目标提前销毁）并不总应导致整段脚本失败。  
   因此系统提供显式的失败控制与恢复手段（如 `Try` / `Fallback` / `onTimeout`），让“失败是否冒泡”为可配置语义，而非硬编码唯一答案。

---

## Data Foundation

本章节定义场景系统使用的原子数据类型。所有 `gameplaytag`、`stat_definition`、`event_definition` 均与 `ability-design` 共享同一张注册表，不重复定义。

---

### SceneCondition

`SceneCondition` 的运行时上下文是 `SceneInstance`，用于 `outcome.condition`、`trigger.condition`、`WaitUntil.condition` 等场合。

```cfg
interface SceneCondition {
    struct Const { value: bool; }
    struct And { conditions: list<SceneCondition>; }
    struct Or  { conditions: list<SceneCondition>; }
    struct Not { condition: SceneCondition; }

    // 槽位实体的属性值比较
    struct ActorStatCompare {
        actor: SceneActorSelector;
        statTag: str ->stat_definition;
        op: CompareOp;
        value: float;
    }

    // 槽位实体的 Tag 状态（复用 GAS 标签体系）
    struct ActorHasTags {
        actor: SceneActorSelector;
        tagQuery: GameplayTagQuery;
    }

    struct ActorIsAlive { actor: SceneActorSelector; }
    struct ActorIsDead  { actor: SceneActorSelector; }

    // 槽位是否已激活（处理可选参与者）
    struct SlotIsActive { slotTag: str ->gameplaytag; }

    // 场景变量比较
    struct SceneVarCompare {
        varTag: str ->gameplaytag;
        op: CompareOp;
        value: float;
    }

    // 场景已运行时长（由 SceneInstance 内部计时器驱动）
    struct TimeSinceSceneStart {
        op: CompareOp;
        seconds: float;
    }
}
```

---

### SceneActorSelector

`SceneActorSelector` 的运行时上下文是 `SceneInstance`，用于从场景中动态解析目标实体。

```cfg
interface SceneActorSelector {
    // 通过槽位 Tag 取单个实体（最常用）
    struct SlotActor  { slotTag: str ->gameplaytag; }

    // 取槽位内所有实体（支持多人槽）
    struct AllInSlot  { slotTag: str ->gameplaytag; }

    // 从场景变量中取（如 SpawnActor 输出后注册的动态实体）
    struct SceneVar   { actorVarTag: str ->gameplaytag; }

    // 按距离筛选（复用 ability-design 的 TargetFilter）
    struct NearestToSlot {
        referenceSlot: str ->gameplaytag;
        filter: TargetFilter;
        maxCount: int;
    }
}
```

---

### SceneValue / SceneScoreFormula

用于场景变量计算、评分计算、参数传递等。

```cfg
interface SceneValue {
    struct Const { value: float; }
    struct SceneVarValue { varTag: str ->gameplaytag; }
    struct Math { op: MathOp; a: SceneValue; b: SceneValue; }
}

interface SceneScoreFormula {
    struct Const { value: float; }
    struct SceneVarValue { varTag: str ->gameplaytag; }
    struct Math { op: MathOp; a: SceneScoreFormula; b: SceneScoreFormula; }
    // 线性时间衰减：score = base - decayPerSecond * elapsedTime
    struct TimeBonusDecay {
        baseScore: float;
        decayPerSecond: float;
    }
}
```

---

### AwaitMode

所有有“持续时间/完成事件”语义的动作节点都必须声明此枚举，这是解决 fire-and-forget 歧义的核心设计。

```cfg
enum AwaitMode {
    // 发出指令，立刻返回 Succeeded，不等待完成
    Immediate;

    // 等待引擎广播该动作的完成事件后才返回 Succeeded
    // 通过 EventBus 订阅完成事件实现，不依赖硬编码时长
    UntilComplete;
}
```

---

### ReevalPolicy

用于 `WaitUntil`、`SceneTrigger`、`SceneOutcome` 等“条件驱动节点”的重评估策略。

```cfg
enum ReevalPolicy {
    // 仅在 reactToEvents 声明的事件到来时重新 evaluate
    EventDriven;

    // 以固定频率重新 evaluate（用于依赖源难以穷举的条件）
    FixedInterval;

    // 每帧 evaluate（仅用于极少数必须实时判定的条件）
    EveryTick;
}
```

---

### FailurePolicy

用于声明单个动作节点失败时的语义。

```cfg
enum FailurePolicy {
    // 返回 Failed，交由父节点处理
    Propagate;

    // 记录日志/调试信息，但返回 Succeeded，继续后续流程
    IgnoreAsSuccess;
}
```

---

## Runtime Core

本章节定义场景系统的运行时核心数据结构，是连接静态配置与动态执行的桥梁。

---

### SceneInstance

场景运行时主体，由场景管理器在触发条件满足时实例化。

```java
class SceneInstance {
    scene_definition config;

    // 槽位到实体的实时映射
    Int2ObjectMap<Actor> slotMap;
    Int2ObjectMap<Actor> anchorMap;

    // 场景局部变量（复用 GAS 的 Store 接口）
    Store sceneVars;

    // 执行树根节点
    ActionContext rootContext;

    // 当前已运行时长（供 TimeSinceSceneStart 条件使用）
    float elapsedTime = 0;

    // 所有已匹配的 outcome（包括 terminateScene=false 的记录型结果）
    List<MatchedOutcomeRecord> matchedOutcomes = new ArrayList<>();

    SceneState state = Pending;

    void tick(float dt) {
        if (state != Running) return;
        elapsedTime += dt;

        // 1. 检查 terminateScene=true 的结果
        checkTerminatingOutcomes();
        if (state != Running) return;

        // 2. 驱动脚本
        if (rootContext.status == Pending) {
            rootContext.onEnter();
            rootContext.status = Running;
        }

        ActionContext.Status s = rootContext.tick(dt);
        if (s == Succeeded || s == Failed) {
            finalizeByScriptTermination(s);
        }
    }

    Actor resolveActor(SceneActorSelector selector) { ... }

    Context makeContext(Actor target) {
        return new Context(
            instigator: resolveActor(SlotActor("Slot.Player")),
            causer:     null,
            target:     target,
            instanceState: sceneVars,
            ...
        );
    }

    void abortScene(SceneAbortReason reason) {
        if (rootContext != null) rootContext.abort();
        finalizeByAbort(reason);
    }

    void finalizeByScriptTermination(ActionContext.Status terminalStatus) {
        SceneOutcome outcome = pickOutcomeForScriptTermination(terminalStatus);
        if (outcome != null) applyOutcome(outcome);
        state = (terminalStatus == Succeeded) ? Completed : Aborted;
    }

    void finalizeByAbort(SceneAbortReason reason) {
        SceneOutcome outcome = pickOutcomeForAbort(reason);
        if (outcome != null) applyOutcome(outcome);
        state = Aborted;
    }
}

record MatchedOutcomeRecord(
    SceneOutcome outcome,
    float matchedTime
) {}

enum SceneState { Pending, Running, Completed, Aborted; }

enum SceneAbortReason {
    OutcomeTriggered,
    ExternalAbort,
    ScriptFailed
}
```

---

### Outcome 语义说明

场景的结局采用统一模型：

1. **outcome 持续被评估**
   - `terminateScene=false` 的 outcome：匹配后记录到 `matchedOutcomes`
   - `terminateScene=true` 的 outcome：匹配后立刻触发场景终止

2. **脚本自然完成时**
   - 系统从所有已匹配 outcome 中选取优先级最高者作为最终结局
   - 如果没有任何匹配 outcome，则场景允许“无结局完成”
   - 具体项目若不希望出现该情况，应自行配置兜底 outcome

3. **同一帧多个终止型 outcome 同时命中时**
   - 按 `priority` 最高者生效
   - 若优先级相同，由配置顺序稳定决胜（建议编辑器告警）

---

### ActionContext

执行树的节点基类，是场景系统运行时的核心抽象。

```java
abstract class ActionContext {
    SceneAction   config;
    SceneInstance scene;
    ActionContext parent;
    List<ActionContext> children = new ArrayList<>();

    enum Status { Pending, Running, Succeeded, Failed, Aborted }
    Status status = Pending;

    abstract void onEnter();
    abstract Status tick(float dt);
    abstract void onExit(boolean aborted);

    final void abort() {
        if (status != Running && status != Pending) return;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).status == Running) {
                children.get(i).abort();
            }
        }
        onExit(true);
        status = Aborted;
    }

    static ActionContext create(SceneAction cfg, SceneInstance scene, ActionContext parent) { ... }
}
```

---

## 配置定义

### scene_definition

场景定义表，是整个场景系统的最上层配置入口。

```cfg
table scene_definition[id] (json) {
    id: int;
    name: text;
    description: text;
    sceneTags: list<str> ->gameplaytag;

    // 参与者：有 AI、有战斗逻辑的实体
    actorSlots: list<SceneActorSlot>;

    // 环境锚点：无 AI，仅用于承载环境 Status 或接收 SendEvent
    environmentAnchors: list<SceneEnvironmentAnchor>;

    // 静态前置条件：加载时检查一次，不满足则该场景对当前玩家不可用
    prerequisites: list<SceneCondition>;

    // 动态激活触发器：条件满足时激活场景
    trigger: SceneTrigger (nullable);

    // 核心脚本（主线序列）
    script: SceneAction;

    // 结局定义：统一承载所有终止条件、奖励与超时
    outcomes: list<SceneOutcome>;

    // 场景变量初始化
    initVars: list<SceneVarInit>;
}
```

```cfg
struct SceneTrigger {
    condition: SceneCondition;

    // 性能优化提示：声明哪些事件可能导致条件变化
    reactToEvents: list<str> ->event_definition;

    // 条件重评估策略
    reevalPolicy: ReevalPolicy;

    // 当 reevalPolicy = FixedInterval 时生效
    intervalSec: float;
}
```

```cfg
struct SceneActorSlot {
    slotTag:         str ->gameplaytag;
    aiArchetype:     str ->ai_archetype;
    spawnPoint:      str;
    initialStatuses: list<int> ->status;
    statOverrides:   list<SceneStatOverride>;
    removeOnDeath:   bool;
}
```

```cfg
struct SceneEnvironmentAnchor {
    slotTag:         str ->gameplaytag;
    spawnPoint:      str;
    initialStatuses: list<int> ->status;
}
```

```cfg
struct SceneStatOverride {
    statTag: str ->stat_definition;
    op:      ModifierOp;
    value:   float;
}
```

```cfg
struct SceneVarInit {
    varTag:    str ->gameplaytag;
    initValue: float;
}
```

```cfg
struct SceneOutcome {
    outcomeTag: str ->gameplaytag;

    // 触发条件
    condition: SceneCondition;

    // 性能优化提示：声明哪些事件可能导致条件变化
    reactToEvents: list<str> ->event_definition;

    // 条件重评估策略
    reevalPolicy: ReevalPolicy;
    intervalSec: float;

    // true = 命中后立即终止场景
    // false = 仅记录，不终止
    terminateScene: bool;

    // 并发命中时的优先级
    priority: int;

    rewardEffects: list<SceneRewardEntry>;
    scoreFormula:  SceneScoreFormula (nullable);
    broadcastEvent: str ->event_definition (nullable);
}
```

```cfg
struct SceneRewardEntry {
    target: SceneActorSelector;
    effect: Effect;
}
```

---

## SceneAction 节点

`SceneAction` 是场景脚本的执行单元，构成可任意组合的节点树。每帧只 tick 处于 Running 状态的节点。

```cfg
interface SceneAction {

    // ═══════════════════════════════
    // 基础动作节点
    // ═══════════════════════════════

    struct PlayAnimation {
        target:        SceneActorSelector;
        animName:      str;
        blendInTime:   float;
        await:         AwaitMode;
        stopOnAbort:   bool;
        failurePolicy: FailurePolicy;
    }

    struct Dialogue {
        speaker:       SceneActorSelector;
        dialogueId:    int ->dialogue;
        await:         AwaitMode;
        failurePolicy: FailurePolicy;
    }

    struct Camera {
        action:        CameraAction;
        await:         AwaitMode;
        failurePolicy: FailurePolicy;
    }

    struct DoEffect {
        target:        SceneActorSelector;
        effect:        Effect;
        await:         AwaitMode;
        failurePolicy: FailurePolicy;
    }

    struct CastAbility {
        caster:        SceneActorSelector;
        abilityId:     int ->ability;
        target:        SceneActorSelector (nullable);
        await:         AwaitMode;
        failurePolicy: FailurePolicy;
    }

    struct SendEvent {
        target:         SceneActorSelector (nullable);
        eventTag:       str ->event_definition;
        magnitude:      float;
        extras:         list<struct { tag: str ->gameplaytag; value: float; }>;
        failurePolicy:  FailurePolicy;
    }

    struct SpawnActor {
        outputVarTag:    str ->gameplaytag;
        aiArchetype:     str ->ai_archetype (nullable);
        spawnPoint:      str;
        initialStatuses: list<int> ->status;
        failurePolicy:   FailurePolicy;
    }

    struct SetSceneVar {
        varTag: str ->gameplaytag;
        op:     ModifierOp;
        value:  SceneValue;
    }

    struct ShowHint {
        hintKey:   str;
        duration:  float;
        hintStyle: HintStyle;
        failurePolicy: FailurePolicy;
    }

    // ═══════════════════════════════
    // 等待节点
    // ═══════════════════════════════

    struct WaitForEvent {
        eventTag:   str ->event_definition;
        source:     SceneActorSelector (nullable);
        conditions: list<SceneCondition>;
        timeoutSec: float;
        onTimeout:  SceneAction (nullable);
    }

    struct WaitUntil {
        condition:      SceneCondition;
        reactToEvents:  list<str> ->event_definition;
        reevalPolicy:   ReevalPolicy;
        intervalSec:    float;
        timeoutSec:     float;
    }

    struct WaitSeconds {
        duration: float;
    }

    // ═══════════════════════════════
    // 控制流节点
    // ═══════════════════════════════

    struct Sequence { actions: list<SceneAction>; }

    struct Parallel {
        actions: list<SceneAction>;
        policy:  ParallelPolicy; // WaitAll | WaitAny
    }

    struct Conditional {
        condition:  SceneCondition;
        then:       SceneAction;
        else:       SceneAction (nullable);
    }

    struct Loop {
        count: int; // -1 = 无限
        body:  SceneAction;
    }

    // 失败恢复：body 失败时执行 fallback
    struct Try {
        body: SceneAction;
        fallback: SceneAction;
    }

    struct ReturnFailed {}
    struct ReturnSucceeded {}

    // ═══════════════════════════════
    // 作用域节点
    // ═══════════════════════════════

    struct TakeActorControl {
        targets:        list<SceneActorSelector>;
        mode:           ActorControlMode;
        softTimeoutSec: float;
        body:           SceneAction;
        onAbort:        SceneAction (nullable);
    }

    struct WithStatus {
        target:   SceneActorSelector;
        statusId: int ->status;
        body:     SceneAction;
    }

    struct WithVar {
        varTag:    str ->gameplaytag;
        initValue: SceneValue;
        body:      SceneAction;
    }

    // ═══════════════════════════════
    // 复用
    // ═══════════════════════════════

    struct ScriptRef {
        sharedScriptId: int ->shared_scene_script;
        args: list<SceneScriptArg>;
    }
}
```

---

### 共享脚本参数化

为避免 `ScriptRef` 只能机械复用，场景系统支持参数化脚本调用。

```cfg
table shared_scene_script[id] (json) {
    id: int;
    name: text;
    description: text;
    action: SceneAction;
}
```

```cfg
struct SceneScriptArg {
    varTag: str ->gameplaytag;
    value: SceneScriptArgValue;
}

interface SceneScriptArgValue {
    struct FloatConst { value: float; }
    struct SlotActorRef { slotTag: str ->gameplaytag; }
    struct SceneVarRef { varTag: str ->gameplaytag; }
}
```

共享脚本内部通过 `WithVar` / `SceneVar` 读取这些参数。  
这使“通用 Boss 入场”“通用开门流程”“通用撤离倒计时”等脚本片段可以在不同场景中复用。

---

### 枚举与附属类型

```cfg
enum ActorControlMode {
    Immediate;
    Polite;
}

enum HintStyle {
    Normal;
    Warning;
    BossSkillAlert;
}

enum ParallelPolicy {
    WaitAll;
    WaitAny;
}
```

```cfg
interface CameraAction {
    struct FocusOn { target: SceneActorSelector; blendTime: float; }
    struct Cutscene { cutsceneId: str; }
    struct Shake    { preset: str; }
    struct Restore  { blendTime: float; }
}
```

---

## 与 GAS / AI 系统的集成关系

场景系统不独立实现战斗或 AI 逻辑，而是作为**编排层**，通过以下接口与两个系统交互。

| 场景节点 | 对接目标 | 机制 |
| :--- | :--- | :--- |
| `DoEffect` | GAS Effect 系统 | 直接调用 `Effects.execute()`，`scene.makeContext()` 注入场景 instigator/target |
| `CastAbility` | GAS Ability 系统 | 调用槽位实体的 AbilityComponent |
| `WithStatus` | GAS Status 系统 | 挂载/精确移除 `StatusInstance`，生命周期与作用域节点绑定 |
| `SendEvent` | GAS EventBus | 场景→战斗的单向广播，GAS Trigger 可监听 |
| `WaitForEvent` | GAS EventBus | 战斗→场景的反向通知 |
| `TakeActorControl` (Immediate) | GAS Tag 系统 | 授予 `State.Scene.Controlled`，被 `global_ai_settings` 拦截，AI 物理宕机 |
| `TakeActorControl` (Polite) | AI 行为系统 | 注入高优先级 `SceneDirected` 行为，AI 主动让权 |
| `SpawnActor` | AI Archetype | 生成实体并初始化 `AIBrainComponent` |
| 场景生命周期 | GAS EventBus | 场景开始/结束自动广播，任务系统、成就系统可监听 |

---

### Actor 控制权冲突：两种夺权模式

当场景需要控制一个正在自治运行 AI 的 Actor 时，存在控制权冲突。根据视觉连续性的要求选择模式：

#### Immediate（硬夺权）
适用于 `Dialogue`、`Cutscene`、需要完全锁定姿态的演出。

```text
1. TakeActorControl.onEnter → 通过 GAS 授予 State.Scene.Controlled
2. AI 在下一帧 global_ai_settings 校验命中 → 清空任务栈，物理宕机
3. SceneInstance 独占驱动 Actor
4. TakeActorControl.onExit (finally) → 移除 State.Scene.Controlled
5. AI 大脑恢复，下一帧重新进入算分流程
```

#### Polite（软夺权）
适用于 `MoveTo`、`CastAbility`、需要 AI 自然过渡的场景。

```text
1. TakeActorControl.onEnter → 注入极高优先级 SceneDirected 行为到 AI 决策池
2. AI 在完成当前 minCommitmentTime 后重选 → 选中 SceneDirected
3. SceneInstance 检测到 State.Scene.Directed → 开始执行 body
4. softTimeoutSec 内未让权 → 自动升级为 Immediate
5. TakeActorControl.onExit (finally) → 撤回注入，移除 State.Scene.Directed
6. AI 恢复原有决策池
```

#### Tag 引用计数安全性
当场景的 `State.Scene.Controlled` 与其他系统状态（如 `State.Debuff.Stun`）同时作用于同一个 Actor 时，`TagContainer` 的引用计数机制保证多来源状态互不覆盖，任一来源移除时都不会误解除另一来源的控制效果。

---

## 实现语义参考

---

### 复合节点语义

#### Sequence
子节点按顺序依次执行：
- 任一子节点 `Failed` → 整体 `Failed`
- 全部 `Succeeded` → 整体 `Succeeded`

#### Parallel
并发驱动所有子节点：
- `WaitAll`：全部完成后返回；若任一失败，则最终结果为 `Failed`
- `WaitAny`：任一子节点 `Succeeded` 即整体 `Succeeded`，其余 Running 子节点被中止清理

#### Try
用于局部失败恢复：
- `body` 成功 → 整体 `Succeeded`
- `body` 失败 → 执行 `fallback`
- `fallback` 的结果作为 Try 节点最终结果

---

### 条件重评估语义

适用于 `SceneTrigger`、`SceneOutcome`、`WaitUntil`：

- `EventDriven`：仅在 `reactToEvents` 触发时重新 evaluate
- `FixedInterval`：按 `intervalSec` 周期检查
- `EveryTick`：每帧检查，仅限少量高实时性条件

推荐顺序：
1. 事件驱动
2. 固定频率检查
3. 每帧检查（最后手段）

---

### 节点失败语义

场景系统不假设“任何失败都必须终止整个场景”。  
失败传播遵循以下原则：

- 基础动作节点按 `failurePolicy` 决定：
  - `Propagate`：返回 `Failed`
  - `IgnoreAsSuccess`：记录失败并返回 `Succeeded`
- 复合节点再根据控制流语义处理子节点结果
- 若设计者需要对失败执行替代逻辑，应使用 `Try`

---

## Examples

### 例 A：炎魔领主 Boss 战（双阶段）

```cfg
table scene_definition {
    id: 7001; name: "炎魔领主试炼";
    sceneTags: ["Scene.Type.Boss"];

    actorSlots: [
        { slotTag: "Slot.MagmaLord"; aiArchetype: "Boss_MagmaLord";
          spawnPoint: "Boss_Arena_Center"; removeOnDeath: true; },
        { slotTag: "Slot.Player"; spawnPoint: "Arena_Entrance"; removeOnDeath: false; }
    ];
    environmentAnchors: [
        { slotTag: "Slot.ArenaCenter"; spawnPoint: "Arena_Center_Marker"; initialStatuses: []; }
    ];

    prerequisites: [];
    trigger: null;

    initVars: [
        { varTag: "Var.Scene.Phase"; initValue: 1.0 }
    ];

    script: struct Sequence { actions: [

        struct TakeActorControl {
            targets: [struct SlotActor { slotTag: "Slot.MagmaLord"; }];
            mode: Immediate; softTimeoutSec: 0.0;
            body: struct Sequence { actions: [
                struct Camera {
                    action: struct FocusOn {
                        target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        blendTime: 1.0;
                    };
                    await: Immediate; failurePolicy: IgnoreAsSuccess;
                };
                struct PlayAnimation {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    animName: "entrance_roar"; blendInTime: 0.3;
                    await: UntilComplete;
                    stopOnAbort: true;
                    failurePolicy: Propagate;
                };
                struct Dialogue {
                    speaker: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    dialogueId: 5001;
                    await: UntilComplete;
                    failurePolicy: IgnoreAsSuccess;
                };
                struct Camera {
                    action: struct Restore { blendTime: 0.8; };
                    await: Immediate; failurePolicy: IgnoreAsSuccess;
                };
            ]};
            onAbort: struct PlayAnimation {
                target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                animName: "idle"; blendInTime: 0.5;
                await: Immediate; stopOnAbort: false;
                failurePolicy: IgnoreAsSuccess;
            };
        };

        struct DoEffect {
            target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
            effect: struct GrantTemporaryTags {
                duration: struct Const { value: -1.0; };
                grantedTags: ["Scene.MagmaLord.Phase1"];
            };
            await: Immediate;
            failurePolicy: Propagate;
        };

        struct Parallel {
            policy: WaitAny;
            actions: [
                struct WaitUntil {
                    condition: struct ActorStatCompare {
                        actor: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        statTag: "Stat.HP.Percent"; op: LessEqual; value: 0.3;
                    };
                    reactToEvents: ["Event.Combat.Damage.Post"];
                    reevalPolicy: EventDriven;
                    intervalSec: 0.0;
                    timeoutSec: -1.0;
                };
                struct Loop {
                    count: -1;
                    body: struct Sequence { actions: [
                        struct WaitForEvent {
                            eventTag: "Event.Scene.MinionDied";
                            source: null; conditions: []; timeoutSec: -1.0;
                            onTimeout: null;
                        };
                        struct SetSceneVar {
                            varTag: "Var.Scene.MinionKillCount";
                            op: Add;
                            value: struct Const { value: 1.0; };
                        };
                    ]};
                };
            ];
        };

        struct TakeActorControl {
            targets: [struct SlotActor { slotTag: "Slot.MagmaLord"; }];
            mode: Immediate; softTimeoutSec: 0.0;
            body: struct Sequence { actions: [
                struct SetSceneVar {
                    varTag: "Var.Scene.Phase";
                    op: Override;
                    value: struct Const { value: 2.0; };
                };
                struct Parallel {
                    policy: WaitAll;
                    actions: [
                        struct Camera {
                            action: struct FocusOn {
                                target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                                blendTime: 0.5;
                            };
                            await: Immediate; failurePolicy: IgnoreAsSuccess;
                        },
                        struct ShowHint {
                            hintKey: "Boss.MagmaLord.PhaseChange";
                            duration: 5.0;
                            hintStyle: BossSkillAlert;
                            failurePolicy: IgnoreAsSuccess;
                        }
                    ];
                };
                struct WithStatus {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    statusId: 4010;
                    body: struct PlayAnimation {
                        target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        animName: "phase2_roar"; blendInTime: 0.2;
                        await: UntilComplete; stopOnAbort: false;
                        failurePolicy: Propagate;
                    };
                };
                struct Dialogue {
                    speaker: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    dialogueId: 5002; await: UntilComplete;
                    failurePolicy: IgnoreAsSuccess;
                };
                struct Parallel {
                    policy: WaitAll;
                    actions: [
                        struct SendEvent {
                            target: struct SlotActor { slotTag: "Slot.ArenaCenter"; };
                            eventTag: "Event.Environment.StoneDoor.Close";
                            magnitude: 0.0; extras: [];
                            failurePolicy: IgnoreAsSuccess;
                        },
                        struct DoEffect {
                            target: struct SlotActor { slotTag: "Slot.ArenaCenter"; };
                            effect: struct ApplyStatus { statusId: 4003; captures: []; };
                            await: Immediate;
                            failurePolicy: Propagate;
                        }
                    ];
                };
                struct WaitSeconds { duration: 1.0; };
                struct Camera {
                    action: struct Restore { blendTime: 0.8; };
                    await: Immediate; failurePolicy: IgnoreAsSuccess;
                };
                struct DoEffect {
                    target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                    effect: struct ApplyStatus { statusId: 4002; captures: []; };
                    await: Immediate;
                    failurePolicy: Propagate;
                };
            ]};
        };

        struct WaitForEvent {
            eventTag: "Event.Character.Death";
            source: struct SlotActor { slotTag: "Slot.MagmaLord"; };
            conditions: []; timeoutSec: -1.0; onTimeout: null;
        };

        struct Parallel {
            policy: WaitAll;
            actions: [
                struct Camera {
                    action: struct FocusOn {
                        target: struct SlotActor { slotTag: "Slot.MagmaLord"; };
                        blendTime: 0.5;
                    };
                    await: Immediate; failurePolicy: IgnoreAsSuccess;
                },
                struct DoEffect {
                    target: struct SlotActor { slotTag: "Slot.Player"; };
                    effect: struct SendEvent {
                        eventTag: "Event.UI.ShowVictory"; magnitude: 0.0; extras: [];
                    };
                    await: Immediate;
                    failurePolicy: IgnoreAsSuccess;
                }
            ];
        };
        struct WaitSeconds { duration: 3.0; };
        struct Camera {
            action: struct Restore { blendTime: 1.5; };
            await: Immediate; failurePolicy: IgnoreAsSuccess;
        };
    ]};

    outcomes: [
        {
            outcomeTag: "Scene.Result.Victory";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.MagmaLord"; }; };
            reactToEvents: ["Event.Character.Death"];
            reevalPolicy: EventDriven; intervalSec: 0.0;
            terminateScene: true; priority: 10;
            rewardEffects: [
                { target: struct SlotActor { slotTag: "Slot.Player"; };
                  effect: struct ModifyStat {
                      statTag: "Stat.Currency.Gold"; op: Add;
                      value: struct Const { value: 1500.0; };
                  }; }
            ];
            scoreFormula: struct TimeBonusDecay { baseScore: 10000.0; decayPerSecond: 8.0; };
            broadcastEvent: "Event.Scene.MagmaLordDefeated";
        },
        {
            outcomeTag: "Scene.Result.Defeat";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.Player"; }; };
            reactToEvents: ["Event.Character.Death"];
            reevalPolicy: EventDriven; intervalSec: 0.0;
            terminateScene: true; priority: 100;
            rewardEffects: [];
            broadcastEvent: "Event.Scene.PlayerDefeated";
        },
        {
            outcomeTag: "Scene.Result.Timeout";
            condition: struct TimeSinceSceneStart { op: GreaterEqual; seconds: 600.0; };
            reactToEvents: [];
            reevalPolicy: FixedInterval; intervalSec: 1.0;
            terminateScene: true; priority: 50;
            rewardEffects: [];
            broadcastEvent: null;
        }
    ];
}
```

---

### 例 B：护送任务（多目标评分 + 可选结局）

```cfg
table scene_definition {
    id: 7002; name: "护送商人";
    sceneTags: ["Scene.Type.Escort"];

    actorSlots: [
        { slotTag: "Slot.Merchant"; aiArchetype: "NPC_Merchant";
          spawnPoint: "Escort_Start"; removeOnDeath: true; },
        { slotTag: "Slot.Player"; spawnPoint: "Escort_Start"; removeOnDeath: false; }
    ];
    environmentAnchors: [
        { slotTag: "Slot.EscortGoal"; spawnPoint: "Escort_End_Marker"; initialStatuses: []; }
    ];

    prerequisites: [
        struct ActorHasTags {
            actor: struct SlotActor { slotTag: "Slot.Player"; };
            tagQuery: { requireTagsAll: ["Quest.EscortPrerequisite.Done"]; };
        }
    ];

    trigger: struct SceneTrigger {
        condition: struct ActorStatCompare {
            actor: struct SlotActor { slotTag: "Slot.Player"; };
            statTag: "Stat.DistanceTo.EscortStart"; op: LessEqual; value: 5.0;
        };
        reactToEvents: ["Event.Actor.PositionUpdated"];
        reevalPolicy: EventDriven;
        intervalSec: 0.0;
    };

    initVars: [
        { varTag: "Var.Scene.MerchantsAlive"; initValue: 1.0 },
        { varTag: "Var.Scene.EnemiesKilled";  initValue: 0.0 }
    ];

    script: struct Sequence { actions: [

        struct TakeActorControl {
            targets: [struct SlotActor { slotTag: "Slot.Merchant"; }];
            mode: Polite; softTimeoutSec: 3.0;
            body: struct Dialogue {
                speaker: struct SlotActor { slotTag: "Slot.Merchant"; };
                dialogueId: 6001; await: UntilComplete;
                failurePolicy: IgnoreAsSuccess;
            };
            onAbort: null;
        };

        struct Parallel {
            policy: WaitAny;
            actions: [
                struct WaitUntil {
                    condition: struct ActorStatCompare {
                        actor: struct SlotActor { slotTag: "Slot.Merchant"; };
                        statTag: "Stat.DistanceTo.EscortGoal"; op: LessEqual; value: 3.0;
                    };
                    reactToEvents: ["Event.Actor.PositionUpdated"];
                    reevalPolicy: EventDriven;
                    intervalSec: 0.0;
                    timeoutSec: -1.0;
                },
                struct WaitForEvent {
                    eventTag: "Event.Character.Death";
                    source: struct SlotActor { slotTag: "Slot.Merchant"; };
                    conditions: []; timeoutSec: -1.0; onTimeout: null;
                },
                struct Loop {
                    count: -1;
                    body: struct Sequence { actions: [
                        struct WaitForEvent {
                            eventTag: "Event.Character.Death";
                            source: null;
                            conditions: [
                                struct ActorHasTags {
                                    actor: struct SceneVar { actorVarTag: "Data.KilledActor"; };
                                    tagQuery: { requireTagsAny: ["Faction.Bandit"]; };
                                }
                            ];
                            timeoutSec: -1.0;
                            onTimeout: null;
                        },
                        struct SetSceneVar {
                            varTag: "Var.Scene.EnemiesKilled";
                            op: Add;
                            value: struct Const { value: 1.0; };
                        }
                    ]};
                }
            ];
        };
    ]};

    outcomes: [
        {
            outcomeTag: "Scene.Result.EscortSuccess";
            condition: struct ActorStatCompare {
                actor: struct SlotActor { slotTag: "Slot.Merchant"; };
                statTag: "Stat.DistanceTo.EscortGoal"; op: LessEqual; value: 3.0;
            };
            reactToEvents: ["Event.Actor.PositionUpdated"];
            reevalPolicy: EventDriven; intervalSec: 0.0;
            terminateScene: true; priority: 10;
            rewardEffects: [
                { target: struct SlotActor { slotTag: "Slot.Player"; };
                  effect: struct ModifyStat {
                      statTag: "Stat.Currency.Gold"; op: Add;
                      value: struct Const { value: 300.0; };
                  }; }
            ];
            scoreFormula: struct Math {
                op: Add;
                a: struct TimeBonusDecay { baseScore: 5000.0; decayPerSecond: 5.0; };
                b: struct Math {
                    op: Multiply;
                    a: struct SceneVarValue { varTag: "Var.Scene.EnemiesKilled"; };
                    b: struct Const { value: 100.0; };
                };
            };
            broadcastEvent: "Event.Quest.EscortComplete";
        },
        {
            outcomeTag: "Scene.Result.MerchantDied";
            condition: struct ActorIsDead { actor: struct SlotActor { slotTag: "Slot.Merchant"; }; };
            reactToEvents: ["Event.Character.Death"];
            reevalPolicy: EventDriven; intervalSec: 0.0;
            terminateScene: true; priority: 100;
            rewardEffects: [];
            broadcastEvent: "Event.Quest.EscortFailed";
        }
    ];
}
```

---

## 结语

本设计的核心不是增加一个新的“场景逻辑玩法系统”，而是提供一个**安全、可组合、可中止、可复用的单场景编排层**：

- 战斗归 GAS
- 自治归 AI
- 场景负责组织它们在时间轴上的协作

其最重要的工程原则是：

1. **作用域绑定生命周期**
2. **等待完成依赖事件，而非猜测时长**
3. **失败可设计，不一刀切**
4. **场景对外保持原子性，场景间协调交给上层系统**

如果你愿意，我下一步可以继续帮你把这份新版文档再往前推进一层，整理成一个更统一的三件套风格版本，即：

- `ability-design`
- `ai-design`
- `scene-design`

三份文档在：
- Context 命名
- Condition / Value 抽象
- 生命周期术语
- Event / Outcome 语义

上完全对齐。