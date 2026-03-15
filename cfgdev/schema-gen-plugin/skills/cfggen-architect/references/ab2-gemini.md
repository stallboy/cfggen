这是一份非常优秀的、达到工业级水准的技能系统底层架构草案。它不仅准确抓住了 Unreal GAS（Gameplay Ability System）的核心精髓，还结合了 `cfggen` 的静态强类型配置优势，彻底规避了传统 OOP 技能树的高耦合问题。

### 💡 架构评估与优化建议 (Architectural Critique)

站在资深游戏架构师的角度，这份设计在“正交性”和“数据驱动”上已经做得极好。但在实际落地到 AAA 或中大型商业项目时，还可以从以下几个维度进行优化与升华：

1. **数值聚合器 (Aggregator) 公式标准化**：
   目前的 `ChangeSet` 提到了 `override`, `add`, `multiply`。但在 RPG 商业项目中，百分比加成（`AddPct`）和独立乘区（`Multiply`）极易混淆。必须在架构层面把 **Stat 的结算公式写死并标准化**（例如：`最终值 = (Base + Add) * (1 + AddPct) * Multiply`），否则后期策划配表会产生巨大的数值黑洞。
2. **目标选取流水线 (Targeting Pipeline) 的解耦**：
   原文的 `TargetScan` 混合了“形状(Shape)”、“过滤(Filter)”和“排序(Sort)”。更优雅的设计是将其拆分为管线：**获取源 (Generator) -> 形状检测 (Shape) -> 过滤筛除 (Filter) -> 排序截取 (Sort/Limit)**。这样所有的零件都可以高度复用。
3. **能力(Ability)生命周期细化**：
   原文的 Ability 直接指向一个 Effect。在实际动作游戏中，Ability 往往有前摇、引导、后摇，甚至分段（段落连招）。引入 **执行阶段 (Phases)** 或 **持续性任务 (Ability Tasks)** 的概念，能更好地处理被打断时的状态回滚。
4. **随机性与延迟 (RNG & Time) 缺失**：
   Effect 体系中缺少了最常用的两个原子控制流：`Delay`（延迟执行，比如天降正义 2 秒后爆炸）和 `Chance`（概率执行，比如 30% 几率挂毒）。这部分应作为通用控制节点补充。

---

综合以上考量，我为您重新整理并升级了这份**《现代能力系统架构设计文档 (v2.0)》**。此版本在保留您原有核心思想的基础上，补齐了商用管线的缺失环节，使之更严密、更易于编辑器化。

---

# 现代能力系统架构设计 (Modern Ability System Design)

本文档定义了一套基于数据驱动、高内聚、低耦合的通用 Gameplay 技能框架。全面吸收 GAS 设计哲学，剥离逻辑与表现，采用**“标签驱动机制、事件流转载荷、节点组合逻辑”**的范式，适用于 ARPG、MMO、MOBA 等复杂多维度的战斗环境。

## 1. 核心设计哲学 (Core Philosophy)

1. **标签即法律 (Tag is Law)**：树状 `GameplayTag` 是系统内唯一的沟通语言。禁止使用硬编码的 `enum` 来判断状态（如 `isStunned`），一切通过查 Tag 解决。
2. **两阶段事件管线 (Two-Phase Event Pipeline)**：数值的交互必须通过 `EventBus` 发送 `Payload` 载荷。经历 **Pre（篡改期：加减伤）** -> **Execute（结算期：扣血）** -> **Post（副作用期：吸血/反伤）**，彻底解耦攻防双方。
3. **逻辑与表现断绝关系 (Logic-View Separation)**：服务器与核心战斗逻辑严禁知道“特效、音效、UI飘字”的存在。逻辑层仅负责向外抛出 `GameplayCue`，由客户端独立决定如何渲染视听反馈。
4. **属性公式绝对集权 (Aggregator Centralization)**：所有战斗属性（Stat）的 Buff 叠加结算遵循引擎底层的唯一公式，拒绝策划在技能里自定义复杂的乘除法嵌套。

---

## 2. 数据基盘 (Data Foundation)

### 2.1 游戏标签 (GameplayTag)
严格层级化的字符串标识，使用 `.` 分隔。底层采用运行时展开（写入时把父级一并写入容器）以实现 O(1) 的查询性能。

```cfg
table gameplaytag[name] {
    name: str; // 例: "State.Control.Stun", "Damage.Element.Fire"
    description: text;
}
```

### 2.2 核心属性 (Stat Definition)
定义实体的数值属性，明确初始值与极值约束。

```cfg
table stat_definition[statTag] {
    statTag: str ->gameplaytag;
    name: text;
    defaultValue: float;
    
    // 决定计算公式的行为
    // Snapshot: 挂载瞬间锁定施法者的属性 (常用于DOT伤害，吃快照)
    // Dynamic: 实时跟随施法者的属性变化 (常用于光环类Buff)
    calcType: StatCalcType; 
    
    minLimit: StatLimit;
    maxLimit: StatLimit;
    clampMode: StatClampMode; // 当MaxHp改变时，CurrentHp如何随之联动
}
```

---

## 3. 数值与修饰管线 (Modifier Pipeline)

这是 RPG 系统的核心，解决了“一堆 Buff 叠加在一起怎么算”的世界级难题。

### 3.1 属性计算公式 (Stat Aggregator)
任何一个 Stat 最终值的结算，都必须且只能经过以下管线：

$$ FinalValue = (BaseValue + \sum Add) \times (1 + \sum AddPct) \times (\prod Multiply) $$
*若存在 `Override` 修饰器，则直接取优先级最高的 Override 值，忽略上述公式。*

```cfg
enum ModifierOp {
    Add;        // 绝对值加减 (如：攻击力 +50)
    AddPct;     // 百分比加减法，通常用于同类Buff的加法稀释 (如：提升 15% 攻击力)
    Multiply;   // 独立乘区加减，永远产生实际复利 (如：造成伤害 * 1.5)
    Override;   // 强制覆写 (如：将目标移速强制变为 100)
}
```

### 3.2 运行时载荷 (Payload & ChangeSet)
事件通信的标准集装箱。

```java
record Payload(
    Context context,    // 环境上下文 (发起者、造成者、当前目标、局部变量)
    
    // 主干数值
    float baseMagnitude,           // 原始值 (如基础伤害 500)
    ChangeSet magnitudeModifiers,  // 收集流转过程中其他节点对主值的修饰 (加减伤)
    
    // 副属性透传 (例如附带的削韧值、仇恨值，也可在此修改)
    Store extras
){}
```

---

## 4. 表达式与AST节点 (Expression AST)

将硬编码的判断抽离为上下文感知的微型节点，是实现高复用的关键。运行时环境注入 `(Context, Payload)`。

### 4.1 目标选取管线 (Targeting Pipeline)
标准化目标选取的四个步骤：定源 -> 划圈 -> 过滤 -> 挑人。

```cfg
struct TargetSelector {
    // 1. 定源：以谁为中心？什么朝向？
    center: TargetSource;  
    direction: TargetSource;

    // 2. 划圈：扫描形状
    shape: TargetShape; // Point(单体), Sphere(球), Sector(扇形), Box(矩形)

    // 3. 过滤：敌我关系、状态标签、视野遮挡
    filter: TargetFilter;

    // 4. 挑人与限制：超出数量怎么办？
    limit: int;            // -1 为无限制
    sort: TargetSortType;  // 距中心最近、血量最低、随机等
}

enum TargetSource {
    ContextInstigator; // 施法者
    ContextCauser;     // 肇事者 (如飞行中的火球)
    ContextTarget;     // 当前技能锁定的主目标
    PayloadTarget;     // 事件载荷中的受击者
}
```

### 4.2 浮点动态演算 (FloatValue) & 条件判定 (Condition)

```cfg
interface FloatValue {
    struct Const { value: float; }
    struct Math { op: MathOp; a: FloatValue; b: FloatValue; }
    struct RandomFloat { min: float; max: float; }
    struct StatValue { source: TargetSource; statTag: str; }
    struct PayloadMagnitude {} // 取出事件中正在流转的值
}

interface Condition {
    struct Logic { op: LogicOp; conditions: list<Condition>; } // And/Or/Not
    struct Compare { left: FloatValue; op: CompareOp; right: FloatValue; }
    struct HasTags { source: TargetSource; tags: GameplayTagQuery; }
    struct Chance { probability: FloatValue; } // 0.0 ~ 1.0 的概率判定
}
```

---

## 5. 核心业务实体 (Core Entities)

Ability 负责管理资源与状态流转；Effect 负责瞬间的改变；Status 负责持续的逻辑。

### 5.1 能力技能 (Ability)
代表游戏中的一个“动作行为”，如普攻、闪避、施放火球。

```cfg
table ability[id] {
    id: int;
    abilityTags: list<str> ->gameplaytag;

    // --- 准入检查 (CanActivate) ---
    activationBlockedTags: list<str>;  // 被这些Tag拦住 (如 被控)
    activationRequiredTags: list<str>; // 必须拥有这些Tag (如 变身状态)
    costs: list<StatCost>;             // 需要扣除的蓝量/体力
    cooldownId: str ->gameplaytag;     // 共享CD组

    // --- 执行内容 (Execute) ---
    // 能力被激活时，按顺序执行的指令。动作游戏常需要用 Timeline/Sequence 来做
    effectTree: Effect;
}
```

### 5.2 瞬间指令流 (Effect)
Effect 是**无状态、瞬时发生**的动词。

```cfg
interface Effect {
    // 【基础修改】
    struct ModifyStat { statTag: str; op: ModifierOp; value: FloatValue; }
    
    // 【高级业务指令】
    struct Damage { 
        damageTags: list<str>; // 决定了打在敌人身上触发什么表现和护甲抗性结算
        baseDamage: FloatValue; 
    }
    struct Heal { baseHeal: FloatValue; }
    struct ApplyStatus { statusId: int; durationOverride: FloatValue; }
    struct SpawnObj { objId: int; offset: Vector3; } // 发射弹道/生成法阵
    
    // 【事件与修改器 (GAS的精髓)】
    struct SendEvent { eventTag: str; magnitude: FloatValue; }
    struct InjectPayloadModifier { 
        // 专门用在 Trigger 监听里，用于修改别人发出的 Payload (如：拦截受击事件并减伤)
        op: ModifierOp; 
        value: FloatValue; 
    }

    // 【控制流 (Control Flow)】
    struct Sequence { effects: list<Effect>; }
    struct Conditional { condition: Condition; trueEffect: Effect; falseEffect: Effect; }
    struct Delay { time: FloatValue; effect: Effect; } // 延迟执行
    struct Repeat { count: int; interval: FloatValue; effect: Effect; }
    struct WithTarget { target: TargetSelector; effect: Effect; } // 改变局部上下文目标
}
```

### 5.3 持续状态 (Status / Buff)
承载所有持久化状态（增益、减益、护盾、霸体、控制）。

```cfg
table status[id] {
    id: int;
    name: text;
    stackingPolicy: StackingPolicy; // 叠加(MaxStacks), 刷新(Refresh), 时长累加等

    core: StatusCore;
}

struct StatusCore {
    duration: FloatValue; 
    
    // --- Tag 交互 ---
    grantedTags: list<str>; // 赋予宿主的Tag (如霸体: State.Invincible)
    blockTags: list<str>;   // 免疫后续挂载的含此类Tag的Status (免疫毒)
    cancelTags: list<str>;  // 挂载瞬间驱散宿主身上含此类Tag的状态 (净化)

    behaviors: list<Behavior>;
}

// 状态在存续期间干什么？
interface Behavior {
    // 持续影响属性 (如下降50%护甲)
    struct StatModifier { statTag: str; op: ModifierOp; value: FloatValue; }
    
    // 周期性跳字 (如DOT中毒)
    struct Periodic { period: FloatValue; effect: Effect; }
    
    // 被动触发器 (全局事件总线的监听者，实现反甲、名刀、闪避特效的核心)
    struct Trigger {
        listenEventTag: str ->gameplaytag; // 监听的事件阶段，如 "Event.Damage.Take.Pre"
        conditions: list<Condition>; 
        effect: Effect; 
        
        limitCount: int;      // 触发次数限制 (如只挡3次攻击)
        removeStatusOnLimit: bool; // 次数用尽是否销毁本状态
    }
}
```

---

## 6. 物理法则 (Global Rules)

通过全局配置文件定义引擎底层的刚性运行规则，将硬编码转化为数据配置。

### 6.1 全局标签互斥律 (Tag Relationship)
一表统管全游戏所有的“异常状态打断与克制”关系，彻底消灭 `if(isStunned)` 代码。

```cfg
table global_tag_rules {
    // 定义 Tag 之间的宏观影响。
    // 例：当宿主拥有 "State.Debuff.Stun" (眩晕) 时
    // blocks: ["Ability"] -> 禁止激活任何普通技能
    // cancels: ["Ability.Cast"] -> 直接打断正在读条的技能
    // immunes: ["State.Debuff.Slow"] -> 被晕的时候免疫减速(优化表现)
    rules: list<TagRelationshipRule>; 
}
```

### 6.2 伤害结算管线 (Combat Pipeline)
标准的 `Damage` Effect 会在底层转化为一次执行流，按此表定义的顺序一层层扣减护盾和血量。

```cfg
table global_combat_settings {
    // 护甲与抗性类型矩阵 (如 火系打冰盾 乘以 1.5)
    damageMatrix: list<DamageMatrixRule>;
    
    // 伤害扣减顺序表 (例：先扣魔法盾，再扣物理白盾，最后扣真实生命值)
    damageLayers: list<DamageLayer>; 
}

struct DamageLayer {
    statTag: str;               // 被扣减的属性
    allowOverflow: bool;        // 这一层扣空了，剩下的伤害是否继续往下传？(名刀锁血设为false)
    hitCueTag: str;             // 扣这一层时，给客户端发什么受击特效？(如打在魔法盾上的波纹)
    breakCueTag: str;           // 这一层破裂时的特效 (破盾音效)
}
```

---

## 7. 表现层分离 (Gameplay Cue Layer)

**绝对原则：服务端和纯逻辑层没有模型、没有特效、没有声音。**
逻辑系统在产生动作时，只向客户端广播 `GameplayCueEvent (Tag, Type, Actor)`。客户端维持一个配置表来映射和播放这些反馈。

### 7.1 Cue 注册表
客户端独占表。

```cfg
table client_cue_registry[cueTag] {
    cueTag: str ->gameplaytag; // 比如 "Cue.Combat.Hit.Fire"
    
    // 瞬发表现 (阅后即焚，不保留引用)
    instantHandlers: list<InstantCue>;
    
    // 持续表现 (常驻光效，需受状态生命周期管理)
    sustainedHandlers: list<SustainedCue>;
}

struct InstantCue {
    targetRole: ActorRole; // 播放目标是 Instigator(发起者) 还是 Target(承受者)
    
    vfx: VfxConfig;
    sfx: SfxConfig;
    animTrigger: str;       // 触发动画机节点 (如 Hit_React_Flinch)
    cameraShake: str;
    damageText: FloatingTextConfig; // 飘字配置 (如 红色暴击字体)
}
```

### 7.2 Cue 生命周期推导
引擎底层自动完成逻辑与表现的对接：
- 当 `Effect.Damage` 命中时，系统发出 `(CueTag, Executed)`，客户端调用 `instantHandlers` 播放爆血。
- 当 `Status.燃火` 挂载时，系统发出 `(CueTag, Added)`，客户端调用 `sustainedHandlers` 在人物模型上挂一个燃烧粒子，并存入活跃字典。
- 当 `Status.燃火` 结束时，系统发出 `(CueTag, Removed)`，客户端查字典找到那个粒子，调用 Stop() 使其熄灭。

---

## 8. 实战范例 (Practical Example)

通过以上系统，如何配置一个复杂的**“吸血光环 + 残血狂暴”**机制，而不需要改动一行代码：

**机制描述：** 
只要这个 Status 存在，周围 5 米友军获得 10% 吸血；当自己血量低于 30% 时，自己伤害翻倍。

**配置实现 (Status)：**
```json
{
  "id": 9001,
  "name": "血祭光环",
  "core": {
    "duration": {"value": -1}, // 永久光环
    "behaviors": [
      // 行为 1：为周围友军挂吸血Buff (周期扫描)
      {
        "$type": "Periodic",
        "period": 0.5,
        "effect": {
          "$type": "WithTarget",
          "target": { 
             "center": "ContextTarget", "shape": {"$type": "Sphere", "radius": 5.0},
             "filter": {"relation": "Friendly"} 
          },
          "effect": {
             "$type": "ApplyStatus", "statusId": 9002 // 9002 是一个监听伤害事件并按比例回血的子Status
          }
        }
      },
      // 行为 2：监听自身血量变化，进行残血狂暴
      {
         "$type": "StatModifier",
         "targetTagQuery": {"require": []}, 
         "statTag": "Stat.Combat.DamageMultiplier",
         "op": "Multiply",
         "value": {"$type": "Const", "value": 2.0},
         // 这里假设引擎支持 StatModifier 带激活条件，或者通过 Trigger 动态赋予/移除子 Buff
         "activeCondition": {
             "$type": "Compare",
             "left": {"$type": "StatValue", "statTag": "Stat.HP.CurrentPct"},
             "op": "Lte",
             "right": {"$type": "Const", "value": 0.3}
         }
      }
    ]
  }
}
```

### 结语
这套基于抽象节点 (AST) 与配置分离的架构，初期程序搭建解析器（Context 传递、SafeList 容器、AST 递归求值、双端 Cue 同步）会有一定的开发成本。但一旦完备，策划可以通过可视化编辑器组装出 95% 以上的商业级战斗机制，实现工业级管线的高效量产。