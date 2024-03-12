# 技能系统

## 核心概念

跟星际争霸的abe技能架构相同，a： ability，b：behavior，e：effect。
这里为了跟mmorpg里的习惯用语对齐，a叫skill，b叫buff，e保留叫effect


- buff

  这是actor身上的状态效果，包括了buff和debuff，也叫status effect。会保留状态记录。这个状态大部分与战斗相关，
  但也可扩展到行为状态，比如BeHit表示受击动画状态中，比如ClientControlReverse让客户端上下左右控制逆转。

- effect

  这是直接效果，比如伤害，治疗，增加buff等表示要做一个动作。

- skill

  这是actor的技能列表，actor可以castSkill来释放技能。技能效果就是个effect
  
这三个概念构成了技能系统。


## 上下文 ctx
Effect执行时的上下文，Buff持有时的上下文，两者结构基本相同，我们称之为ctx，主要包含以下成员

```java
record Ctx(Actor self,
           Actor caster,
           Actor sender,
           LockedTarget lockedTarget){
}
```


- self
    * 执行主体
    * Effect是要对self起作用，比如Damage是对上下文中的self做血量扣除。
    * Buff是self持有这个buff

- caster  
    * 最初谁发起的，一般就是最初来源技能的施法者
  
- sender
    * 上一个发起者
  
- LockedTarget
    * 锁定Actor或Pos，一般最初是CastSkill传进来的

例子:

A使用技能对B头上放个3秒后爆炸的炸弹，对C造成了Damage。
Damage执行时的Context为
```
（self=C，sender=B，caster=A，lockedTarget=B）
```

## 正交组合

这个技能配置系统的灵活性来源于可以自由的组合```effect触发时刻```，```effect触发条件```，```effect作用目标```。

### effect触发时刻：时间或事件

#### 按时触发：TimelineBuff
```
interface BuffLogic {
    ...
    struct TimelineBuff { // 技能skill的逻辑一般是给自己add一个TimelineBuff
        durationSec:float;
        effectsOnTime:list<EffectOnTime>;
    }
}

struct EffectOnTime {
    time:float; //发生时间点
    effect:list<EffectLogic>;
    target:list<TargetSelector>; //target为空时，等价于Self
}
```

技能大多是```给自己add一个TimelineBuff```的effect。

#### 事件触发：TriggerBuff
```
interface BuffLogic {
    ...
    struct TriggerBuff { // 事件触发Buff，可用于实现一些事件触发的buff，或者给npc加上这个buff用于实现一个简单的ai
		times:int; // 触发次数,不限制填0
		cooldownSecond:float; //触发CD
		effectsOnTrigger:list<EffectOnTrigger>;
	}
}

struct EffectOnTrigger {
	triggers:list<str> ->buff.triggerevt;
	effect:list<EffectLogic>;
}
```

比如要实现```受到伤害时给自己加个盾```的buff，这个```受到伤害```就是个事件。


### effect触发条件：Condition
```
interface EffectLogic {
    ...
    struct EffectIf {
        condition:Condition;
        effect:list<EffectLogic>;
        elseEffect:list<EffectLogic>;
    }
}
```

### effect作用目标：TargetSelector

目标默认是self，如果要修改，请用EffectTarget，或在触发这个effect的地方会有TargetSelector，让你选择。
```
interface EffectLogic {
    ...
    struct EffectTarget {
        effect:EffectLogic; // 从target里选出actor后，设置为新ctx的self，然后在新ctx下执行effect
        target:list<TargetSelector>;
    }
}
```

这里TargetSelector既包含，self, sender, caster, lockedTarget, 也包含区域目标Cube，Cylinder，Ring，FullScene

## 创建子物体和运行轨迹

```
interface EffectLogic {
    ...
    struct CreateObj { // 创建子物体
        duration:float;
        asset:ParamStr;
        offset:Vec3;
        yAngle:float;
        lockDirection:bool; // 锁定朝向(当施法者攻击目标时，特效朝向为：施法者指向目标)
        lifeRelyParentBuff:bool; // 为true时含义： 上层buff销毁时，产生的此obj也销毁
        isUseLockedOffsetPos:bool; // 使用技能范围摇杆确定的位置

        objInfo:ObjCreateInfo;
        objInitBuffs:list<BuffLogic>;
    }
}
```

运行轨迹有很多，在ObjCreateInfo里去扩展，比如陷阱是Static，跟随自身旋转的法球是Bind，直线子弹是Line，追踪子弹是Chase。


### 同步方案

之前说了skill大多数是 ```给自己add一个TimelineBuff```的effect。


- 预播放
    * 暗黑3是客户端只预播放动作，其他都等服务器
    * 武林是预播放TimelineBuff下属于“表现”那一列的Effect；CreateObj如果是没有逻辑的特效或绑定特效则也预播放

- 预播放Effect和服务器Effect的同步
    * 服务器提前一个RTT（Round-Trip Time）来触发TimelineBuff下的各个逻辑Effect


