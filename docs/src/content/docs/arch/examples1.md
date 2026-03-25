---
title: 例子
sidebar:
  order: 30
---


### 案例一：史诗级 Boss 狂暴转阶段（强演出 + 机制重置）

**场景描述**：
Boss 血量降至 50% 时，触发无敌锁血。镜头拉近给特写，Boss 播放怒吼动画震退玩家，随后进入二阶段：攻击力翻倍，且 AI 放弃普通近战，开始疯狂使用全屏 AOE 技能。

**数据流向**：
`Scene (监听血量 -> 劫持演出 -> 挂载状态)` ➡️ `GAS (获得霸体与属性加成 -> 激活行为修饰器)` ➡️ `AI (旧行为被屏蔽 -> 新技能库注入 -> 重新决策)`

#### 1. Scene 配置（导演编排）
```text
scene_definition {
    sceneId: 1001; name: "BossFight_Dragon";
    rootAct: Sequence { acts: [
        // 后台持续监听血量阈值
        WaitUntil {
            condition: ActorStatCompare { 
                actor: SceneVar { actorVar: "BossEntity" }; 
                quantifier: All; // 必须显式量化
                stat: "Stat.HPPercent"; op: Lte; value: Const{value: 0.5}; 
            };
        },
        // 阈值触发后执行的演出序列       
        // 1. 劫持控制权（立刻打断 AI 的 tick）
        WithActorControl {
            targets: SceneVar { actorVar: "BossEntity" }; mode: Immediate;
            body: Sequence { acts: [
                // 2. 挂载临时无敌保护（防止演出期间被打死）
                ApplyEffect { effect: GrantTags { grantedTags: ["State.Invincible"]; duration: Const{value: 3.0} } };
                // 3. 镜头聚焦特写
                Camera { action: FocusOn { target: ContextTargets{}; blendTime: 0.5 }; await: FireAndForget };
                // 4. 强制播放怒吼动画，并挂起等待播完
                PlayAnimation { animName: "Roar_Phase2"; blendInTime: 0.1; await: UntilComplete };
                // 5. 演出结束，赋予二阶段永久 Status (bindings留空)
                ApplyEffect { effect: ApplyStatus { statusId: 8002; bindings: [] } } 
            ]}
        }
        
    ]}
}
```

#### 2. GAS 配置（生理突变）
```text
status {
    id: 8002; name: "Status_Phase2_Enraged";
    stackingPolicy: Single { refreshMode: KeepDuration }; // 不可堆叠
    core: StatusCore {
        grantedTags: ["State.Phase.Enraged"];
        duration: Const { value: -1.0 }; // 永久
        
        // 视觉表现：全身冒火
        cuesWhileActive: ["Cue.Vfx.BossFireAura"];
        
        behaviors: [
            // 属性突变：攻击力提升 50%
            StatModifier { stat: "Stat.Attack"; op: Mul; value: Const { value: 1.5 }; overridePriority: 0; requiresAll: [] },
            // 动态修改 AI 大脑
            AIModifier { modifier: "Modifier_Boss_Phase2" } // 👈 核心握手点
        ];
    };
}
```

#### 3. AI 配置（战术重组）
```text
ai_behavior_modifier {
    name: "Modifier_Boss_Phase2";
    // 禁用一阶段的平庸技能
    disableBehaviorsQuery: { requireAny: ["Tag.Behavior.Phase1_Melee"];};
    // 注入二阶段的毁灭技能
    injectedBehaviors: [
        { targetGroup: "Group_Ultimate"; behaviors: ["Behavior_Cast_Doomsday"] }
    ]
}

ai_behavior {
    behaviorId: 2005;
    name: "Behavior_Cast_Doomsday";
    interruptPriority: 0; 
    isInterruptible: false;
    minCommitmentTime: 0.0;
    
    // 只要玩家存活就放
    requiredGoal: { goalDef: "Goal.Target.Player"; selector: Nearest };
    preConditions: [ CanActivateAbility { abilityId: 9001 } ];
    cooldown: 15.0; // 每15秒放一次
    
    score: Const { value: 100.0 }; // 极高权重
    scoreInertia: 0.0;
    
    // 调用全屏大招 Ability
    task: CastAbility { abilityId: 9001; target: BoundGoalActor{} };
    abortConditions: [];
}
```

**💡 架构之美**：真正的机制解耦。Scene（导演）只管演出和下发状态，完全不碰具体数值与 AI 逻辑；Boss 属性的膨胀与大脑技能池的替换被完美封装在 GAS 的 Status 与 AIModifier 中。这意味着即使脱离了当前专属关卡，只要被挂上该状态，Boss 依然能自洽地展现出完整的二阶段战斗力。

---

### 案例二：环境交互与涌现式战斗（打爆火药桶）

**场景描述**：
AI 感知器（Perceive）发现火药桶附近有玩家，从而生成了高价值的 `Goal.Item.TacticalBarrel`。哥布林弓箭手（Think）决策射击火药桶。火药桶血量归零爆炸（GAS），对范围内的玩家造成巨额伤害，同时广播全局声响事件触发场景（Scene）的警报逻辑。

**数据流向**：
`AI (选定战术桶 -> 走位 -> 释放基础射击 Ability)` ➡️ `GAS (桶扣血至 0 -> 触发爆炸 Effect -> 无差别伤害 + 广播 Event)` ➡️ `Scene (监听到全局爆炸声 -> 刷出援军)`

#### 1. AI 配置（寻找聪明的战术）
```text
ai_behavior {
    behaviorId: 1050;
    name: "Behavior_Shoot_Tactical_Barrel";
    // 仅当感知器识别到"附近有敌人的桶"时才被驱动
    requiredGoal: { goalDef: "Goal.Item.TacticalBarrel"; selector: Nearest };
    preConditions: [
        { CanActivateAbility { abilityId: 105 } } // 确保射击技能不在 CD
    ];
    score: Const { value: 80.0 }; // 算分压过普通射击
    scoreInertia: 5.0;
    
    task: Sequence { tasks: [
        MoveTo { target: BoundGoalActor{}; speedStat: "Stat.MoveSpeed"; tolerance: 15.0; stopOnFinish: true }, 
        CastAbility { abilityId: 105; target: BoundGoalActor{} } // 对火药桶使用“射击”
    ]};

    // 【闭环防护】：在执行上述动作的任何一帧，如果绑定的目标桶死了，强制中止整棵行为树！
    abortConditions: [
        { TargetIsDead { target: BoundGoalActor{} } }
    ];
}
```

#### 2. GAS 配置（火药桶的物理法则）
```text
// 火药桶本质是一个拥有 HP Stat 和被动 Status 的 Actor
status {
    id: 4050; name: "Status_Barrel_Logic";
    stackingPolicy: Single { refreshMode: KeepDuration };
    core: StatusCore {
        duration: Const { value: -1.0 };
        behaviors: [
            Trigger {
                listenEvent: "Combat_Damage_Take_Post";
                // 校验：受击后自身是否被打上了死亡标签（由 Stat 归零时写入）
                requiresAll: [ { Actor { source: ContextTarget{}; cond: HasTags { query: { requireAll: ["State.Dead"] } } } } ];
                maxTriggers: 1;
                cooldown: Const { value: 0.0 };
                
                effect: Sequence { effects: [
                    // 1. 播放爆炸特效
                    FireCue { cue: "Cue.Explosion.Huge"; magnitude: Const{value: 1.0} },
                    // 2. 对周围 5 米内所有人造成无差别伤害
                    WithTargets {
                        targets: TargetScan { 
                            shape: Sphere { center: ContextTarget{}; radius: Const{value: 5.0} };
                            relationTo: ContextTarget{}; allowedRelations: [Hostile, Friendly, Neutral];
                            tagQuery: {requireAny:[]}; exclude: []; maxCount: -1; sort: None 
                        };
                        effect: ResolveCombat { pipeline: "FireDamage"; magnitude: Const{value: 300.0}; tags: ["Damage.Element.Fire"]; cuesOnExecute: [] }
                    },
                    // 3. 向【自身】的 EventBus 广播爆炸巨响事件
                    SendEvent { event: "Env_Explosion"; magnitude: Const{value: 1.0}; extras: [] },
                ]};
            }
        ];
    };
}
```

#### 3. Scene 配置（全局后果监听）
```text
scene_definition {
    sceneId: 2001; name: "Stealth_Mission_Zone";
    rootAct: Parallel { acts: [
        // 正常潜行流程...
        Sequence { acts: [ /* ... */ ] },
        
        // 修改后的 Scene 监听片段
        WaitForEvent {
            event: "Env_Explosion";
            source: GroupMembers { groupVar: "Group.LevelBarrels" };
            
            // 从底层事件载荷中，提取出“肇事者(instigator)”，存入场景的局部变量！
            extractPayloads: [
                { writeToVar: "Var.TheExplodedBarrel"; payloadKey: "instigator" } 
            ];
            
            body: Sequence { acts: [
                // 警报声精准地在爆炸的那个桶的位置响起，而不是在玩家头顶或者原点
                PlayCue { cueKey: "Cue.Siren_Alarm"; 
                    playAt: SceneVar{actorVar: "Var.TheExplodedBarrel"} },
                SpawnActor { outputVarKey: "Var.Guard"; archetype: "Archetype_HeavyGuard"; 
                    spawnAt: "SpawnPoint_Entrance" }
            ]}
        }]}
}
```


#### 4. 补全“涌现式感知”的逻辑闭环（GAS 光环 ➡️ AI 目标）

之前的描述中提到：“AI 感知器发现火药桶附近有玩家，从而生成了高价值的 `Goal.Item.TacticalBarrel`”。
在单 Goal 驱动的 AI 架构下，AI 的 `Think` 阶段不能同时计算“桶的位置”和“玩家的位置”。那么这极其聪明的“战术感知”是如何做到的？

**完美解法：让 GAS 替 AI 做空间计算！**
我们只需要在火药桶的 `Status` 中加入一个光环（Aura）或者周期雷达（Periodic）。当玩家靠近火药桶时，桶**自己给自己**挂上一个 `Tag.TacticalOpportunity`。哥布林的感知器只需简单地扫描带有这个 Tag 的桶即可！

**火药桶的补全配置（GAS）：**
```text
status {
    id: 4050; name: "Status_Barrel_Logic";
    core: StatusCore {
        duration: Const { value: -1.0 };
        behaviors: [
            // 1. 原有的受击爆炸 Trigger 略...
            
            // 2. 【新增】：战术雷达！
            Periodic {
                period: Const{value: 1.0}; // 每秒扫描一次
                executeOnApply: true;
                effect: Conditional {
                    // 如果 4 米内有敌对目标（玩家） (伪代码:配合TargetScan判断附近是否有人)
                    condition: Actor { source: ContextTarget{}; cond: HasTags { query: ... } }; 
                    // 给自己贴上“战术机会”标签，持续 1.1 秒（没人的时候自然消失）
                    thenEffect: ApplyEffect { 
                        effect: GrantTags { grantedTags: ["State.TacticalOpportunity"]; 
                        duration: Const{value: 1.1} }};
                    otherwise: [];
                }
            }
        ];
    };
}
```

**哥布林的感知器配置（AI Perceive）：**
```text
ai_goal_generator {
    name: "Gen_Scan_Tactical_Barrel";
    generator: SpatialScan {
        query: SphereScan {
            center: Self{};
            shape: SphereQuery {
                radius: 20.0;
                // 只看那些“身边有玩家、贴了战术标签”的桶！
                requiredTags: { requireAll: ["State.TacticalOpportunity"] };
                sortBy: Nearest; maxResults: 1;
            }
        };
        interval: 1.0;
        generatedGoal: "Goal.Item.TacticalBarrel"; // 产出高价值 Goal 喂给 Think 阶段
    }
}
```

**💡 架构之美**：极致的系统涌现与空间计算下放。AI 无需进行复杂的环境距离计算，火药桶会通过 GAS 雷达自动给自己贴上“战术标签”吸引 AI 开火。火药桶只管遵循物理法则爆炸发声，Scene 只管通过群组监听拉响警报。三者互不相识，却依靠 Tag 与 EventBus 完美咬合出了精妙的环境交互玩法。

---


### 案例三：护送防守玩法（持续引导 + 硬控打断与反噬）

**场景描述**：
经典的“保护圣女”任务。圣女（NPC）需要原地引导（Channel）一个长达 30 秒的神圣仪式。玩家必须保护她。怪物会不断涌出，如果怪物靠近圣女并用“重击”打晕了她，仪式会被**硬打断（Interrupt）**，圣女遭到法术反噬扣血，场景宣告任务失败。如果 30 秒内圣女安然无恙，引导自然完成，场景宣告胜利。

**数据流向**：
`Scene (下令圣女施法，刷怪，挂起等待结局)` ➡️ `GAS (圣女进入 Processing 阶段，处理打断与反噬)` ➡️ `TagRules (裁定怪物的眩晕是否能打断仪式)`

#### 1. GAS 配置（读条机制与反噬）
```text
ability {
    id: 5001; name: "Ability_Holy_Ritual";
    abilityTags: ["Ability.Type.Spell", "Ability.Type.Ritual"];
    
    // 无需目标，原地施法
    targeting: None {};
    
    castMode: CastBar {
        castTime: Const { value: 30.0 }; // 漫长的 30 秒读条
        castingTags: ["State.Casting.Spell", "State.Immobile"]; // 施法期间不可移动
        commitPolicy: OnActivate;
    };

    // 读条期间被 TagRules 的 interruptsAbilities 命中时执行（硬打断惩罚）
    onInterrupt: [
        // 1. 受到反噬伤害
        ResolveCombat { pipeline: "PureDamage"; magnitude: Const{value: 9999.0} };
    ];
}
```

#### 2. 全局物理法则（TagRules 裁定）
```text
tag_rules {
    name: "Global_Combat_Rules";
    rules: [
        {
            whenPresent: "State.Debuff.Control.Stun";
            // 怪物施加的 Stun 会触发硬打断，精准截断圣女的 CastBar 并触发 onInterrupt
            interruptsAbilities: ["Ability.Type.Spell"];
            blocksAbilities: ["Ability.Type.Spell"];
        }
    ]
}
```

#### 3. Scene 配置（宏观导演）
```text
scene_definition {
    sceneId: 3001; name: "Quest_Defend_Maiden";
    
    rootAct: WithActorControl {
        targets: SceneVar { actorVar: "NPC_Maiden" }; mode: Polite;
        body: CastAbility { abilityId: 5001; await: UntilComplete } // 阻塞30秒
    };
    
    outcomes: [
        // 1. 异步中断：如果在 30 秒内，玩家或圣女被打死了，不论剧本演到哪，立刻判负
        { condition: ActorIsDead { actor: "NPC_Maiden" }; resultCode: 0 },
        { condition: ActorIsDead { actor: "Player" }; resultCode: 0 },

        // 2. 主线顺畅：剧本毫无阻碍地演完了（圣女顺利读完条），判胜
        { condition: RootActFinished { expectedStatus: Success }; resultCode: 1 },
    ]
}
```

**💡 架构之美**：Scene 脚本极其清爽，完全没有写复杂的计时器或血量检测。30 秒的时长管理、被打断时的判定、甚至反噬扣血，全部交由 GAS 的 `CastBar` 生命周期原生接管。Scene 只需坐等结局事件。

---

### 案例四：智能连招与“后摇取消”博弈（动作游戏的精髓）

**场景描述**：
精英刺客 AI 拥有一套“三连斩”攻击。
当它挥出第一刀时，会产生 0.6 秒的**后摇（Recovery）**。在这 0.6 秒内它无法走位。但如果此时玩家企图趁机攻击它，刺客 AI 的决策树会瞬间做出反应，使用“翻滚（Dodge）”技能。翻滚的 `State.Dodging` 标签会**软取消（Cancel）**掉第一刀的后摇，让刺客飘逸地躲开玩家的攻击。

**数据流向**：
`GAS (连招状态分发与后摇占用)` ➡️ `AI (检测到高危攻击，高优抢占翻滚)` ➡️ `TagRules (翻滚标签触发 cancelsAbilities，无缝清空普攻后摇)`

#### 1. GAS 配置（连招与后摇）
```text
// 第一斩
ability {
    id: 101; name: "Ability_Slash_1";
    abilityTags: ["Ability.Type.Melee"];
    castMode: Instant {}; // 瞬发出伤
    
    effect: Sequence { effects: [
        WithTargets {
            targets: TargetScan {
                shape: Sector {
                    center: ContextInstigator {};
                    facingOf: ContextInstigator {};
                    radius: Const { value: 3.0 }; // 3米刀围
                    angle: Const { value: 120.0 }; // 120度横扫
                };
                relationTo: ContextInstigator {};
                allowedRelations: [Hostile]; // 只砍敌人
            };
            // 扫描到的每一个敌人，都会分别执行一次 ResolveCombat
            effect: ResolveCombat { 
                pipeline: "PhysicsDamage"; 
                magnitude: Const{value: 50.0} 
            };
        },
        // 开放 1.5 秒的二段连招窗口
        GrantTags { grantedTags: ["Combo.Slash.S2"]; duration: Const{value: 1.5} }
    ]};
    
    // 动作做完后，进入 0.6 秒后摇
    recovery: { duration: Const{value: 0.6}; recoveryTags: ["State.Recovery"] };
}

// 翻滚技能
ability {
    id: 900; name: "Ability_Dodge";
    abilityTags: ["Ability.Type.Movement"];
    effect: ApplyStatus { statusId: 901 }; // 挂载含 "State.Dodging" 的状态，持续 0.4 秒
}
```

#### 2. 全局法则（后摇封锁与翻滚取消）
```text
tag_rules {
    rules: [
        {   // 规则A：后摇期间，什么都干不了（封锁其他攻击和走位）
            whenPresent: "State.Recovery";
            blocksAbilities: ["Ability.Type.Melee", "Ability.Type.Spell"];
        },
        {   // 规则B：但是！如果是翻滚状态，不仅能放，还能软取消掉一切现存的动作（截断 Recovery）
            whenPresent: "State.Dodging";
            cancelsAbilities: ["Ability.Type"]; // 软取消，不触发 onInterrupt
        }
    ]
}
```

#### 3. AI 配置（微操博弈）
```text
// 刺客的紧急闪避行为（特权级）
ai_behavior {
    name: "Behavior_Emergency_Dodge";
    behaviorId: 9000;
    interruptPriority: 100; // 极高优先级，无视当前在干嘛，强行抢占大脑
    
    preConditions: [
        { CanActivateAbility { abilityId: 900 } }, // 翻滚 CD 得转好
        // 且玩家正在对刺客释放重攻击（感知玩家身上的施法标签）
        { TargetHasTags { target: BoundGoalActor{};
             tagQuery: { requireAll: ["State.Casting.Heavy"] } } }
    ];
    
    task: CastAbility { abilityId: 900; target: Self{} }
}
```

**💡 架构之美**：完美实现了 ACT 游戏中的 **"Cancel 机制"**。因为 `Recovery` 阶段被打断是不触发 `onInterrupt` 惩罚的，所以刺客用翻滚顶掉自己的后摇，表现得如同行云流水。而如果刺客没有翻滚 CD，它就会被死死卡在 `State.Recovery` 中挨打。

---

### 案例五：动态目标追踪与“断线”机制（Targeting 追踪）

**场景描述**：
治疗法师正在对 Boss 引导（Channel）一根持续 5 秒的“治疗射线”。
这是一个允许移动施法的技能（无 `State.Immobile` 标签）。但因为它的 `Targeting` 设为了单体目标且最大距离 15 米，当玩家用“击退”技能把 Boss 踹飞出 15 米开外时，系统底层会自动触发 `TargetLost`，柔性取消（Cancel）该治疗法术。

**数据流向**：
`AI (选定 Boss 释放治疗)` ➡️ `GAS (进入 Channel 生命周期，每帧校验 Target 距离)` ➡️ `Player (改变物理空间)` ➡️ `GAS (越界触发 cancel，终止法术)`

#### 1. GAS 配置（动态距离追踪）
```text
ability {
    id: 602; name: "Ability_Tether_Heal";
    abilityTags: ["Ability.Type.Spell", "Ability.Casting.MoveCancel"];
    
    // 【核心机制】：瞄准追踪策略
    targeting: SingleTarget {
        allowedRelations: [Friendly];
        tagQuery: { exclude: ["State.Dead"]; };
        maxRange: Const { value: 15.0 }; // 施法时和持续期间，目标不能超过 15 米
        onTargetLost: Cancel;            // 一旦超出，立刻触发 Cancel 流程
    };

    castMode: Channel {
        duration: Const { value: 5.0 };
        tickInterval: Const { value: 1.0 }; // 每秒奶一口
        tickOnStart: true;
        channelingTags: ["State.Casting.Spell"]; // 注意：没有 Immobile，法师可以边走边连线
        
        // 每次 Tick 触发的效果
        tickEffect: WithTarget {
            target: ContextVar { varKey: "Sys.Targeting.Actor" }; // 读取引擎每帧更新的准星目标
            effect: ResolveCombat { pipeline: "StandardHeal"; magnitude: Const{value: 200.0} }
        }
    };
    
    // 如果是被玩家的沉默技能“硬打断”，会反噬。
    // 但如果是目标跑远了导致的 Cancel，不走这里，直接平滑结束。
    onInterrupt: [ FireCue { cue: "Cue.Voice.Ouch" } ]; 
}
```

#### 2. AI 配置（治疗者逻辑）
```text
ai_behavior {
    name: "Behavior_Heal_Boss";
    // 感知到血量不满的友军（通常是 Boss）
    // 假设 GoalGenerator 已将 "缺血量" 映射为 Goal 的 magnitude
    requiredGoal: { goalDef: "Goal.Friendly.NeedsHeal"; selector: Strongest };
    
    task: Sequence { tasks: [
        // 1. 走到 10 米内（留出 5 米的安全冗余防断线）
        MoveTo { target: BoundGoalActor{}; tolerance: 10.0 };
        
        // 2. 平行执行：一边施法，一边试图保持跟随
        Parallel {
            policy: WaitAny; // 施法结束（或断线）则整个节点退出
            tasks: [
                CastAbility { abilityId: 602; target: BoundGoalActor{} };
                Loop { count: -1.0; task: 
                    MoveTo { target: BoundGoalActor{}; tolerance: 10.0 } 
                }
            ]
        }
    ]}
}
```

**💡 架构之美**：**逻辑与物理状态的高度解耦**。
如果不用底层的 `Targeting` 追踪机制，策划就必须在各种 Effect 节点或者 AI 节点里手动写 `If Distance > 15 Then Abort`，这会极其容易产生 Bug 并污染决策树。
现在，AI 只管“我要奶他并跟着他走”，玩家只管“把 Boss 踢飞”。空间计算与打断逻辑由底层的 Ability `Processing` 阶段每帧自动校验并调用 `cancel()`，这就是原语抽象的威力。