---
name: CFG Skill System Design Reference
description: This skill provides a comprehensive reference for designing complex game skill systems using CFG. Use when designing skill, buff, effect systems, or learning advanced CFG patterns for game mechanics.
version: 1.0.0
---

# 技能系统的 CFG 设计参考

本文档展示如何使用 cfggen 的 CFG 语法设计一个复杂的游戏技能系统。这是一个完整的实战案例，演示了 struct、interface、多态结构等高级特性的应用。

## 解决的问题

类似于暗黑破坏神的技能系统，有主动技能、天赋、被动技能、装备、宝石等各系统来对技能做各种修改，这些系统可能还有等级。复杂的能到 100 多个节点。

## 核心概念

跟星际争霸的 ABE 技能架构相同：
- **A (Ability/Skill)**: actor可以 castSkill 来释放技能
- **B (Behavior/Buff)**: actor 身上的状态效果，会保留状态记录
- **E (Effect)**: 直接效果，比如伤害、治疗、增加 buff、创建临时对象等

## CFG 结构设计要点

### 1. 上下文 Context

```cfg
struct Context {
    caster:Actor;              // 最初谁发起的
    sender:Actor;              // 上一个非 CreatedObj 的发起者
    anySender:Actor;           // 上一个发起者，可以是 CreatedObj
    binder:Actor;              // 执行主体
    skillData:SkillData (nullable);  // 技能实例信息
    buff:Buff (nullable);      // buff 配置
    blackboard:Blackboard;     // 黑板，整个节点图运行期间共享
    note:Note;                 // 便条，只读，只对动态产生的子节点图有效
}
```

### 2. Effect 多态接口

```cfg
interface EffectLogic {
    // 叶子节点
    struct Damage {
        attackPercent:float;
        attackPercentAddBys:list<FloatValue>;
        damageTypeCases:list<DamageTypeCase>;
        damageType:int;
        // ... 其他字段
    }

    struct Heal {
        // 回血、回能量
    }

    struct CreateObj {
        duration:float;
        asset:str;
        createinfo:ObjCreateInfo;
        objInitBuffs:list<Buff>;
        dieEffects:DieEffects;
    }

    // 组合节点
    struct EffectList {
        effects:list<EffectLogic>;
    }

    struct EffectOnTarget {
        target:TargetScan;
        effect:EffectLogic;
        bbKeyIndex:str;
        bbKeyCount:str;
    }

    struct EffectSwitch {
        cases:list<EffectCase>;
        def:EffectLogic (nullable);
    }

    struct AddBuff {
        buff:Buff;
    }

    // ... 更多实现
}
```

### 3. Buff 多态接口

```cfg
interface BuffLogic {
    // 叶子节点
    struct AttrModifier {
        attrs:list<AttrModItem>;
        attrTriggers:list<TriggerEvent>;
        priority:int;
    }

    struct God {
        // 不受伤害，不受 debuff
    }

    struct Stun {
        // 眩晕，不能移动和施法
    }

    // 组合节点
    struct BuffList {
        buffs:list<BuffLogic>;
    }

    struct EffectDelayed {
        effect:EffectLogic;
    }

    struct EffectPeriod {
        period:float;
        effect:EffectLogic;
        bbKeyIndex:str;
    }

    struct EffectByTrigger {
        maxCount:int;
        cooldown:float;
        trigger:TriggerEvent;
        effect:EffectLogic;
    }

    // ... 更多实现
}
```

### 4. 数值节点多态

```cfg
interface FloatValue {
    // 基础值
    struct Const {
        value:float;
    }

    // 根据上下文查询
    struct ByTalentLevelIndex {
        talentId:int;
        values:list<float>;
    }

    struct ByPassiveLevelIndex {
        passiveId:int;
        values:list<float>;
    }

    struct BinderToCasterDistance {
        minDist:float;
        maxDist:float;
        minValue:float;
        maxValue:float;
    }

    // 组合运算
    struct Add {
        a:FloatValue;
        b:FloatValue;
    }

    struct Multiply {
        a:FloatValue;
        b:FloatValue;
    }

    struct Switch {
        condition:Condition;
        ifTrue:FloatValue;
        ifFalse:FloatValue;
    }
}
```

### 5. 条件节点多态

```cfg
interface Condition {
    struct And {
        conditions:list<Condition>;
    }

    struct Or {
        conditions:list<Condition>;
    }

    struct Not {
        condition:Condition;
    }

    struct TalentLevelGte {
        talentId:int;
        level:int;
    }

    struct HasBuffTag {
        tag:str;
    }

    struct HpPercentGte {
        percent:float;
    }
}
```

## 关键设计模式

### 字段命名约定：xxxCases

用于条件性修改字段值：

```cfg
struct Damage {
    damageType:int;  // 默认值
    damageTypeCases:list<DamageTypeCase>;  // 条件覆盖
}

struct DamageTypeCase {
    condition:Condition;
    value:int;
}
```

运行时：返回最先满足条件的值，如果都 不满足则用默认值。

### 字段命名约定：addXxxs

用于累加修改字段值：

```cfg
struct SomeEffect {
    count:int;  // 基础值
    addCounts:list<FloatValue>;  // 额外加成
}
```

最终值 = count + addCounts 里每一节点计算出的值。

**为什么这么设计？**
- 默认为空时，JSON 不会有多余结构
- 策划逻辑上大多是 addXxxs 的需求，正好符合

### 黑板 Blackboard

解决"多重射击"这类问题的方案：

```
多重射击：默认一轮射击，射5 只箭。如果有天赋 A 则变成2 轮。如果有天赋 B 则变成7 只箭。
```

使用 `bbKeyIndex` 字段，子 effect 第一次执行时设置 a=1，第二次执行时 a=2。

```cfg
struct EffectMulti {
    count:FloatValue;
    effect:EffectLogic;
    bbKeyIndex:str;  // "PeriodMultiIndex"
}
```

### 便条 Note

解决动态节点树之间的信息传递问题：

```cfg
struct SetNoteVar {
    bbKey:str;
    value:FloatValue;
}
```

便条只对动态产生的子节点图有效，子节点能继承看到父节点的信息。

## 事件响应机制

```cfg
struct EffectByTrigger {
    maxCount:int;       // 最多触发几次，<=0则不限制
    cooldown:float;     // 最短触发时间间隔
    trigger:TriggerEvent;
    effect:EffectLogic;
}
```

### TriggerEvent 类型

| 事件 | 说明 |
|------|------|
| SendDamageCfg | 伤害计算前：可改变伤害配置 |
| SendDamageBefore | 伤害计算后，扣血前 |
| SendDamage | 扣血后 |
| ReceiveDamageBefore | 接受伤害计算后，扣血前 |
| ReceiveDamage | 扣血后 |
| CastSkill | 释放技能前 |
| CastSkillEnd | 释放技能结束 |
| HpChanged | 血量改变后 |
| EpChanged | 能量改变后 |

### 事件上下文扩展（以伤害为例）

> **设计要点**：不同事件可能需要扩展上下文。以下以伤害事件为例，其他事件（治疗、释放技能等）同理需要类似的上下文扩展机制。

监听伤害事件时，上下文变为 `ContextWithDamage`：

```cfg
struct ContextWithDamage {
    localCtx:Context;      // 本地上下文（buff 所在的上下文）
    damageData:DamageData; // 伤害发生时的信息
}

struct DamageData {
    ctx:Context;           // Damage 发生时的上下文
    isHit:bool;            // 是否命中
    isCritical:bool;       // 是否暴击
    hpLoss:int;            // 扣血量（可修改）
}
```

针对伤害上下文，需要配套的 `DamageCondition` 和 `DamageFloatValue` 接口。以下是代表性示例：

#### DamageCondition 接口（示例）

```cfg
interface DamageCondition {
    // 来源判断
    struct FromSkillId {
        skillId:int ->skill;
    }
    struct DamageTypeEqual {
        damageType:str ->damagetype;
    }

    // 命中结果判断
    struct IsHit {}
    struct IsCritical {}
    struct WillReceiverKilled {}  // 是否致死

    // 上下文切换查询
    struct BySender { cond:ActorCondition; }    // 伤害发送者条件
    struct ByReceiver { cond:ActorCondition; }  // 伤害承受者条件
    struct ByDamageCtx { cond:Condition; }      // 伤害发生时的技能上下文
    struct ByLocalCtx { cond:Condition; }       // 当前上下文

    // 逻辑运算
    struct And { conditions:list<DamageCondition>; }
    struct Or { conditions:list<DamageCondition>; }
    struct Not { condition:DamageCondition; }
}
```

#### DamageFloatValue 接口（示例）

```cfg
interface DamageFloatValue (defaultImpl='Const') {
    struct Const { value:float; }

    // 伤害相关值
    struct DamageHpLoss {}  // 本次伤害扣血量

    // 根据发送者属性查询
    struct ByTalentLevelIndex {
        talent:int ->talent;
        array:list<float>;
    }

    // 上下文切换查询
    struct BySender { value:ActorFloatValue; }
    struct ByReceiver { value:ActorFloatValue; }
    struct ByDamageCtx { value:FloatValue; }
    struct ByLocalCtx { value:FloatValue; }

    // 数学运算
    struct Add { values:list<DamageFloatValue>; }
    struct AddPercent { percentValues:list<DamageFloatValue>; }
}
```

## CreateObj 运动逻辑

```cfg
interface ObjCreateInfo {
    struct Static {
        offsetX:float;
        offsetY:float;
        offsetZ:float;
        yAngle:float;
    }

    struct Bind {
        // 跟随 binder 位置
        offsetX:float;
        offsetY:float;
        offsetZ:float;
        yAngle:float;
        followDir:bool;
        bind:str;  // 骨骼名称
    }

    struct Line {
        // 直线导弹
        offsetX:float;
        offsetY:float;
        offsetZ:float;
        yAngle:float;
        speed:float;
        acc:float;
        noWallCollide:bool;
        type:CollideType;
    }

    struct Chase {
        // 追踪导弹
        from:ActorRef;
        to:ActorRef;
        offsetX:float;
        offsetY:float;
        offsetZ:float;
        speed:float;
        acc:float;
        curveAngle:float;
        type:CollideType;
    }
}
```

## Buff 叠加规则

```cfg
struct Buff {
    id:int;
    rule:int;  // 引用 buffrule.csv
    stackCount:int;
    // ...
}
```

规则由 `buffrule.csv` 定义：
- `maxCount <= 0`: 不加限制
- `maxCount > 0`: 同一个 id 的 buff 最多有一个存在，且`stackCount <= maxCount`

### AddParamBuff 和 AddStackBuff

```cfg
struct AddParamBuff {
    buff:Buff;
    params:list<float>;  // 传递参数
}

struct AddStackBuff {
    buff:Buff;
    addStack:int;  // 一次性增加多层
}
```

## 设计原则总结

### 正交性

系统的灵活性来源于节点之间的可组合性，而简单易理解则来源于组合的**正交性**。

假设系统有 u1, u2, u3 三种特性，各有 c1, c2, c3 参数可调节。如果我要确定 c1 的时候，只用考虑 u1 就可以，不用去看 u2, u3，则此系统有正交性。

### 依赖关系设计

1. 主动技能是 effect，它知道天赋，但不知道被动技能和装备
2. 被动技能和装备是 buff，它知道被动技能，知道天赋
3. 天赋主要是属性，它可被查询等级

这样更新或增加装备和被动技能，并不影响主动技能。

## CFG 代码量参考

- 约 2500 行的 skill.cfg 结构配置
- 约 9000 行的 Java 服务器代码

## 与 UE GAS 的对比

| 特性 | 本方案 | UE GAS |
|------|--------|--------|
| GA/Skill | Skill | Gameplay Ability |
| GE/Effect | Effect & Buff 分离 | Gameplay Effect (统一) |
| Tag系统 | 可借鉴层级化 Tag | Gameplay Tags |
| 配置方式 | 单一节点树 | 多资产协同 |
| 复杂度 | 简单直接 | 较复杂 |

## 扩展阅读

- cfgeditor: https://github.com/stallboy/cfggen
- 文档: https://stallboy.github.io/cfggen/
