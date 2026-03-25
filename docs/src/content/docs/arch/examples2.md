---
title: 综合示例
sidebar:
  order: 31
---

# 综合示例集

以下每个示例横跨 **GAS（能力系统 + 施法过程）**、**AI（行为系统）**、**Scene（场景编排系统）** 三层，重点展示施法生命周期（CastBar/Charged/Channel）如何与 AI 决策和场景编排深度联动。

---

## 示例一：火法师精英 — 蓄力火球 + 火雨引导 + 连招

**玩法概述**：一个火法师精英怪，拥有三种核心技能：蓄力火球（Charged，蓄力越久伤害越高）、火雨引导（Channel，持续降下火雨）、火焰冲击连招（Instant 二连击）。AI 根据距离和战术环境选择不同技能。玩家可以通过控制技能打断其蓄力和引导。

### 第一层：GAS — 技能与施法过程

```
// ═══ 属性定义 ═══
stat_definition { statKey: "HP_Current"; defaultValue: 800;
    minLimit: Const { value: 0; };
    maxLimit: StatLink { stat: "HP_Max"; };
    clampMode: Absolute;
    onDepletedGrantTag: ["State.Dead"]; }
stat_definition { statKey: "HP_Max"; defaultValue: 800; }
stat_definition { statKey: "Mana_Current"; defaultValue: 300;
    minLimit: Const { value: 0; };
    maxLimit: StatLink { stat: "Mana_Max"; };
    clampMode: Absolute; }
stat_definition { statKey: "Mana_Max"; defaultValue: 300; }
stat_definition { statKey: "Attack_Power"; defaultValue: 50; }
stat_definition { statKey: "Combat_CritRate"; defaultValue: 0.15; }
stat_definition { statKey: "Combat_CritDamage"; defaultValue: 1.5; }

// ═══════════════════════════════════════════
// 技能 1：蓄力火球 (Charged)
// ═══════════════════════════════════════════
ability {
    abilityId: 1001;
    name: "蓄力火球";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire"];

    activationConditions: [
        HasTags { source: ContextTarget {};
            query: { exclude: ["State.Dead"]; }; }
    ];
    costs: [{ stat: "Mana_Current"; value: Const { value: 40; }; }];
    cooldown: Const { value: 5.0; };

    targeting: SingleTarget {
        allowedRelations: [Hostile];
        tagQuery: { exclude: ["State.Dead"]; };
        maxRange: Const { value: 20.0; };
        onTargetLost: Cancel;
    };

    castMode: Charged {
        minChargeTime: Const { value: 0.5; };
        maxChargeTime: Const { value: 2.5; };
        releaseOnMax: true;    // 蓄满自动释放
        chargeProgressVar: "Var.ChargeProgress";
        chargingTags: ["State.Casting.Spell"];
        // 不含 State.Immobile → 可以被移动取消
        cuesDuringCharge: ["Cue.Charge.Fireball"];
        commitPolicy: OnRelease;
        // 蓄力不足 minChargeTime 松手 → cancel → 不扣费不触发CD
    };

    // 蓄力进度影响伤害：基础 1.0 倍 + 蓄力进度 * 2.0 倍
    effect: WithTarget {
        target: ContextVar { varKey: "Var.AbilityTarget"; };
        effect: Sequence { effects: [
            FireCue { cue: "Cue.Release.Fireball"; },
            SpawnObj {
                duration: Const { value: 5.0; };
                objTags: ["Obj.Projectile.Fire"];
                moveInfo: ...; // 追踪弹道
                cuesWhileActive: ["Cue.Projectile.FireTrail"];
                dieInfo: [{
                    onHitTarget: true;
                    effects: [
                        ResolveCombat {
                            pipeline: "StandardMagicDamage";
                            magnitude: Math {
                                op: Mul;
                                a: StatValue { source: ContextInstigator {};
                                    stat: "Attack_Power"; };
                                // 基础 1.0 + 蓄力进度(0~1) * 2.0 = 1.0~3.0 倍
                                b: Math { op: Add;
                                    a: Const { value: 1.0; };
                                    b: Math { op: Mul;
                                        a: ContextVar { varKey: "Var.ChargeProgress"; };
                                        b: Const { value: 2.0; };
                                    };
                                };
                            };
                            tags: ["Damage.Element.Fire"];
                            cuesOnExecute: ["Cue.Hit.Fireball"];
                        }
                    ];
                }];
            }
        ]};
    };

    onInterrupt: [
        // 蓄力被打断：自身受到反噬伤害 + 短暂锁定
        ResolveCombat {
            pipeline: "PureDamage";
            magnitude: Const { value: 20; };
            tags: ["Damage.Type.SelfHarm"];
        },
        GrantTags {
            grantedTags: ["State.AbilityLockout"];
            duration: Const { value: 0.8; };
        },
        FireCue { cue: "Cue.Cast.Interrupted.Fire"; }
    ];

    recovery: {
        duration: Const { value: 0.4; };
        recoveryTags: ["State.Recovery"];
        cuesDuringRecovery: [];
    };
}

// ═══════════════════════════════════════════
// 技能 2：火雨引导 (Channel)
// ═══════════════════════════════════════════
ability {
    abilityId: 1002;
    name: "火雨";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire",
        "Ability.AOE"];

    activationConditions: [];
    costs: [{ stat: "Mana_Current"; value: Const { value: 80; }; }];
    cooldown: Const { value: 12.0; };

    targeting: PointTarget {
        maxRange: Const { value: 15.0; };
    };

    castMode: Channel {
        duration: Const { value: 4.0; };
        tickInterval: Const { value: 0.5; };
        maxTicks: 8;
        tickOnStart: true;

        // 每次心跳：在指定地点造成 AOE 伤害
        tickEffect: WithTargets {
            targets: TargetScan {
                shape: Sphere {
                    center: ContextVar { varKey: "Var.AbilityPoint"; };
                    // 注意：ContextVar 在此处读取 PointTarget 写入的 pointVar
                    // 因为 TargetSelector 不支持直接读 pointVar，
                    // 需要 combat_settings.targetingVars.pointVar 定义映射
                    radius: Const { value: 5.0; };
                };
                relationTo: ContextInstigator {};
                allowedRelations: [Hostile];
                maxCount: -1;
            };
            effect: ResolveCombat {
                pipeline: "StandardMagicDamage";
                magnitude: Math {
                    op: Mul;
                    a: StatValue { source: ContextInstigator {};
                        stat: "Attack_Power"; };
                    b: Const { value: 0.6; };
                };
                tags: ["Damage.Element.Fire", "Damage.AOE"];
                cuesOnExecute: ["Cue.Hit.FireRain"];
            };
        };

        channelingTags: ["State.Casting.Spell", "State.Immobile"];
        // State.Immobile → TagRules blocks Ability.Type.Movement → 站桩引导
        cuesDuringChannel: ["Cue.Channel.FireRain"];
        commitPolicy: OnFirstTick;
    };

    // 引导结束的收尾技：地面留下燃烧区域
    effect: WithLocalVar {
        bindings: [{
            varKey: "Var.BurnLocation";
            value: Location { selector: ContextVar { varKey: "Var.AbilityPoint"; }; };
            bindMode: Snapshot;
        }];
        effect: SpawnObj {
            duration: Const { value: 6.0; };
            objTags: ["Obj.Zone.Fire"];
            moveInfo: ...; // 静止
            cuesWhileActive: ["Cue.Zone.BurningGround"];
            effectsOnCreate: [
                ApplyStatusInline {
                    core: {
                        statusTags: ["Status.Type.Zone"];
                        grantedTags: [];
                        duration: Const { value: 6.0; };
                        behaviors: [
                            Aura {
                                scan: TargetScan {
                                    shape: Sphere {
                                        center: ContextCauser {};
                                        radius: Const { value: 5.0; };
                                    };
                                    relationTo: ContextInstigator {};
                                    allowedRelations: [Hostile];
                                    maxCount: -1;
                                };
                                grantedStatusId: 6010; // 燃烧DOT
                                updateInterval: Const { value: 1.0; };
                            }
                        ];
                    };
                }
            ];
            dieInfo: [];
        };
    };

    onInterrupt: [
        GrantTags {
            grantedTags: ["State.AbilityLockout"];
            duration: Const { value: 1.0; };
        },
        FireCue { cue: "Cue.Cast.Interrupted.Fire"; }
    ];

    recovery: {
        duration: Const { value: 0.6; };
        recoveryTags: ["State.Recovery.Heavy"];
        // 重型后摇：TagRules blocks Ability.Type.Movement → 不可翻滚
        cuesDuringRecovery: [];
    };
}

// ═══ 燃烧DOT（被火雨光环挂载） ═══
status {
    id: 6010;
    name: "燃烧";
    statusTags: ["Status.Type.DOT"];
    grantedTags: ["State.Debuff.Burning"];
    duration: Const { value: 3.0; };
    stackingPolicy: Single { refreshMode: ResetDuration; };
    cuesWhileActive: ["Cue.Debuff.Burning"];
    core: { behaviors: [
        Periodic {
            period: Const { value: 1.0; };
            executeOnApply: false;
            effect: ResolveCombat {
                pipeline: "PureDamage";
                magnitude: Const { value: 12; };
                tags: ["Damage.Element.Fire"];
            };
        }
    ]; };
}

// ═══════════════════════════════════════════
// 技能 3a/3b：火焰冲击连招 (Instant × 2)
// ═══════════════════════════════════════════
ability {
    abilityId: 1003;
    name: "火焰冲击·一段";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire",
        "Ability.Type.Melee"];

    activationConditions: [];
    costs: [{ stat: "Mana_Current"; value: Const { value: 15; }; }];
    cooldown: Const { value: 0; };

    targeting: SingleTarget {
        allowedRelations: [Hostile];
        tagQuery: { exclude: ["State.Dead"]; };
        maxRange: Const { value: 4.0; };
        onTargetLost: Continue;
    };

    castMode: Instant {};

    effect: WithTarget {
        target: ContextVar { varKey: "Var.AbilityTarget"; };
        effect: Sequence { effects: [
            ResolveCombat {
                pipeline: "StandardMagicDamage";
                magnitude: Math { op: Mul;
                    a: StatValue { source: ContextInstigator {};
                        stat: "Attack_Power"; };
                    b: Const { value: 0.8; }; };
                tags: ["Damage.Element.Fire"];
                cuesOnExecute: ["Cue.Hit.FlameStrike1"];
            },
            // 开启二段窗口
            WithTarget {
                target: ContextInstigator {};
                effect: GrantTags {
                    grantedTags: ["Combo.FlameStrike.S2"];
                    duration: Const { value: 1.2; };
                };
            }
        ]};
    };

    onInterrupt: [];
    recovery: {
        duration: Const { value: 0.5; };
        recoveryTags: ["State.Recovery"];
        // 轻型后摇：可被翻滚/移动取消，也可被二段连招取消
    };
}

ability {
    abilityId: 1004;
    name: "火焰冲击·二段";
    abilityTags: ["Ability.Type.Spell", "Ability.Element.Fire",
        "Ability.Type.Melee"];

    activationConditions: [
        // 必须在一段的窗口期内
        HasTags { source: ContextInstigator {};
            query: { requireAll: ["Combo.FlameStrike.S2"]; }; }
    ];
    costs: [{ stat: "Mana_Current"; value: Const { value: 20; }; }];
    cooldown: Const { value: 3.0; }; // 整套连招的共享CD

    targeting: SingleTarget {
        allowedRelations: [Hostile];
        tagQuery: { exclude: ["State.Dead"]; };
        maxRange: Const { value: 5.0; };
        onTargetLost: Continue;
    };

    castMode: Instant {};

    effect: WithTarget {
        target: ContextVar { varKey: "Var.AbilityTarget"; };
        effect: Sequence { effects: [
            // 清除连招标记
            WithTarget {
                target: ContextInstigator {};
                effect: RemoveStatusByTag {
                    query: { requireAny: ["Combo.FlameStrike"]; };
                    matchGrantedTags: true;
                };
            },
            // 二段伤害更高 + 击退
            ResolveCombat {
                pipeline: "StandardMagicDamage";
                magnitude: Math { op: Mul;
                    a: StatValue { source: ContextInstigator {};
                        stat: "Attack_Power"; };
                    b: Const { value: 1.5; }; };
                tags: ["Damage.Element.Fire", "Damage.Type.Knockback"];
                cuesOnExecute: ["Cue.Hit.FlameStrike2"];
            }
        ]};
    };

    onInterrupt: [];
    recovery: {
        duration: Const { value: 0.8; };
        recoveryTags: ["State.Recovery.Heavy"];
        // 重型后摇：大招收尾
    };
}

// ═══ 被动：施法增幅（利用生命周期事件） ═══
status {
    id: 6020;
    name: "火焰掌控（被动）";
    grantedTags: ["State.Passive.FireMastery"];
    duration: Const { value: -1; };
    stackingPolicy: Single { refreshMode: KeepDuration; };
    core: { behaviors: [
        // 每次成功释放法术后，下一次法术伤害 +25%
        Trigger {
            listenEvent: "Ability_Executed";
            requiresAll: [
                PayloadHasTag { query: { requireAny: ["Ability.Type.Spell"]; }; }
            ];
            effect: WithTarget {
                target: ContextTarget {}; // 施法者自身
                effect: ApplyStatus {
                    statusId: 6021; // 火焰增幅层
                };
            };
        },
        // 被打断时失去所有增幅层
        Trigger {
            listenEvent: "Ability_Interrupted";
            effect: WithTarget {
                target: ContextTarget {};
                effect: RemoveStatus { anyIds: [6021]; };
            };
        }
    ]; };
}

status {
    id: 6021;
    name: "火焰增幅";
    grantedTags: ["State.Buff.FireAmplify"];
    duration: Const { value: 8.0; };
    stackingPolicy: Shared { maxStacks: 3;
        refreshMode: ResetDuration;
        overflowPolicy: Reject; };
    core: { behaviors: [
        // 每层增加 8% 法术伤害
        Trigger {
            listenEvent: "Combat_Damage_Deal_Pre";
            requiresAll: [
                PayloadHasTag { query: { requireAny: ["Damage.Element.Fire"]; }; }
            ];
            effect: ModifyPayloadMagnitude {
                op: Mul;
                value: StackScaling {
                    baseValue: 1.0;
                    perStackAdd: 0.08;
                    perStackMul: 1.0;
                };
            };
        },
        // 发动伤害后消耗所有层数
        Trigger {
            listenEvent: "Combat_Damage_Deal_Post";
            requiresAll: [
                PayloadHasTag { query: { requireAny: ["Damage.Element.Fire"]; }; }
            ];
            effect: WithTarget {
                target: ContextTarget {};
                effect: RemoveStatus { anyIds: [6021]; };
            };
        }
    ]; };
}
```

### 第二层：AI — 战术决策

```
// ═══ Archetype ═══
ai_archetype {
    name: "FireMage_Elite";
    goalGenerators: ["Gen_Scan_Enemy"];
    behaviorGroups: ["Group_FireMage_Combat"];
    defaultBehavior: "Behavior_Mage_Idle";
    perceiveInterval: 0.5;
    thinkInterval: 0.3;
}

ai_behavior_group {
    name: "Group_FireMage_Combat";
    sharedConditions: [
        { GoalTypeExists { goalDef: "Goal.Combat.Enemy"; } }
    ];
    behaviors: [
        "Behavior_FM_ChargedFireball",
        "Behavior_FM_FireRain",
        "Behavior_FM_FlameStrike",
        "Behavior_FM_Retreat",
        "Behavior_FM_Reposition"
    ];
}

// ═══════════════════════════════════════════
// 行为：蓄力火球 — 远距离高伤害
// ═══════════════════════════════════════════
// AI 视角：距离远时优先选择。CastAbility 调用后，
// 引擎自动处理 Charged 的蓄力过程。
// AI 不需要知道"什么时候松手"——releaseOnMax=true 蓄满自动释放。
// 如果蓄力期间被眩晕 → interruptsAbilities 触发 → onInterrupt 执行
// → AI 的 CastAbility 任务节点返回 Failed → 行为 Failed → 重新决策
ai_behavior {
    behaviorId: 7001;
    name: "Behavior_FM_ChargedFireball";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.Ranged"];
    isInterruptible: false; // 蓄力中不允许被 AI 重选打断
    interruptPriority: 0;
    minCommitmentTime: 0.5;

    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { CanActivateAbility { abilityId: 1001; } },
        // 距离 > 8m 时才选择远程蓄力
        { Compare {
            a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Gt;
            b: Const { value: 8.0; };
        }; }
    ];
    cooldown: 0;

    // 距离越远分数越高（鼓励保持距离）
    score: Math { op: Add;
        a: Const { value: 40.0; };
        b: Math { op: Mul;
            a: Distance { from: Self {}; to: BoundGoalActor {}; };
            b: Const { value: 2.0; };
        };
    };
    scoreInertia: 15.0; // 蓄力中给予高惯性防抖

    task: Sequence { tasks: [
        MoveTo {
            target: BoundGoalActor {};
            speedStat: "Stat.MoveSpeed";
            tolerance: 18.0; // 走到射程内
            stopOnFinish: true;
        },
        // CastAbility 封装了整个 Charged 生命周期：
        // Activating → Processing(蓄力) → Executing(火球飞出) → Recovery
        // AI 只需发出指令，引擎处理一切
        CastAbility {
            abilityId: 1001;
            target: BoundGoalActor {};
        }
    ]};

    abortConditions: [
        { TargetIsDead { target: BoundGoalActor {}; } },
        // 目标冲到面前 → 放弃蓄力
        { DistanceGreaterThan {
            from: Self {};
            to: BoundGoalActor {};
            maxDistance: 20.0; // 实际是 "距离小于 5" 的反向表达
            // 注：这里应该用 Custom + AICondition
        }; }
    ];
}

// ═══════════════════════════════════════════
// 行为：火雨引导 — 区域控场
// ═══════════════════════════════════════════
// AI 视角：当多个敌人聚集时高分。Channel 期间站桩不动
// （channelingTags 含 State.Immobile），若被硬控打断，
// onInterrupt 生效，AI 行为返回 Failed。
ai_behavior {
    behaviorId: 7002;
    name: "Behavior_FM_FireRain";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.AOE"];
    isInterruptible: false;
    interruptPriority: 0;
    minCommitmentTime: 1.0;

    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { CanActivateAbility { abilityId: 1002; } }
    ];
    cooldown: 0;

    // 基础分 + 范围内敌人数量加成（AI 通过 GoalTypeExists 间接感知）
    score: Const { value: 55.0; };
    scoreInertia: 20.0; // 引导中高惯性

    task: Sequence { tasks: [
        MoveTo {
            target: BoundGoalActor {};
            speedStat: "Stat.MoveSpeed";
            tolerance: 13.0;
            stopOnFinish: true;
        },
        // CastAbility 封装了整个 Channel 生命周期：
        // Activating → Processing(引导心跳×8) → Executing(燃烧区域) → Recovery
        CastAbility {
            abilityId: 1002;
            target: BoundGoalActor {};
        }
    ]};

    abortConditions: [
        { TargetIsDead { target: BoundGoalActor {}; } }
    ];
}

// ═══════════════════════════════════════════
// 行为：火焰冲击连招 — 近战爆发
// ═══════════════════════════════════════════
// AI 视角：距离近时高分。通过两次 CastAbility 串联连招。
// 一段的 effect 自动 GrantTags("Combo.FlameStrike.S2")，
// 二段的 activationConditions 自动检查该 Tag。
// 若一段后摇期间 AI 调用二段 → TagRules 中 cancelsAbilities 
// 生效（翻滚/新技能取消后摇）→ 无缝衔接。
ai_behavior {
    behaviorId: 7003;
    name: "Behavior_FM_FlameStrike";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.Melee"];
    isInterruptible: false;
    interruptPriority: 0;
    minCommitmentTime: 0;

    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { CanActivateAbility { abilityId: 1003; } },
        // 距离 ≤ 5m 时才选择近战
        { Compare {
            a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Lte;
            b: Const { value: 5.0; };
        }; }
    ];
    cooldown: 0;

    score: Const { value: 65.0; }; // 近距离高优先

    task: Sequence { tasks: [
        MoveTo {
            target: BoundGoalActor {};
            speedStat: "Stat.MoveSpeed";
            tolerance: 3.5;
            stopOnFinish: true;
        },
        // 一段
        CastAbility {
            abilityId: 1003;
            target: BoundGoalActor {};
        },
        // 短暂等待（让后摇开始、连招窗口打开）
        Wait { duration: Const { value: 0.15; }; },
        // 二段 — 如果 Tag 窗口已关闭，CanActivate 失败
        // → CastAbility 返回 Failed → Sequence 终止 → 行为正常结束
        CastAbility {
            abilityId: 1004;
            target: BoundGoalActor {};
        }
    ]};

    abortConditions: [
        { TargetIsDead { target: BoundGoalActor {}; } }
    ];
}

// ═══════════════════════════════════════════
// 行为：后撤走位
// ═══════════════════════════════════════════
ai_behavior {
    behaviorId: 7004;
    name: "Behavior_FM_Retreat";
    behaviorTags: ["Behavior.Tag.Tactical"];
    isInterruptible: true;
    interruptPriority: 0;

    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { Compare {
            a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Lt;
            b: Const { value: 6.0; };
        }; },
        // 不在施法/引导中才后撤
        { Not { condition: TargetHasTags {
            target: Self {};
            tagQuery: { requireAny: ["State.Casting.Spell"]; };
        }; }; }
    ];
    cooldown: 4.0;

    score: Const { value: 70.0; };

    task: MoveTo {
        target: RuntimeQuery {
            query: SphereScan {
                center: Self {};
                shape: { radius: 10.0;
                    requiredTags: { requireAny: ["Zone.Walkable"]; };
                    sortBy: Nearest; maxResults: 1; };
            };
        };
        speedStat: "Stat.MoveSpeed";
        tolerance: 1.0;
        stopOnFinish: false;
    };
    abortConditions: [];
}

// ═══════════════════════════════════════════
// 行为：重新定位（保持最优施法距离）
// ═══════════════════════════════════════════
ai_behavior {
    behaviorId: 7005;
    name: "Behavior_FM_Reposition";
    behaviorTags: ["Behavior.Tag.Tactical"];
    isInterruptible: true;
    interruptPriority: 0;

    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        // 距离在 6~8m 的尴尬区间 — 太近蓄力不安全，太远需要走位
        { Compare {
            a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Gte;
            b: Const { value: 6.0; };
        }; },
        { Compare {
            a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Lte;
            b: Const { value: 10.0; };
        }; }
    ];
    cooldown: 2.0;

    score: Const { value: 25.0; }; // 低优先级填充行为

    task: MoveTo {
        target: BoundGoalActor {};
        speedStat: "Stat.MoveSpeed";
        tolerance: 12.0; // 走到 12m 最优距离
        stopOnFinish: true;
    };
    abortConditions: [];
}

// ═══ 兜底 ═══
ai_behavior {
    behaviorId: 7099;
    name: "Behavior_Mage_Idle";
    isInterruptible: true;
    requiredGoal: None {};
    score: Const { value: 1.0; };
    task: Wait { duration: Const { value: 1.5; }; };
    abortConditions: [];
}
```

### 第三层：Scene — Boss 遭遇战编排

```
scene_definition {
    sceneId: 8001;
    name: "火法师精英遭遇战";

    signature: [
        { varKey: "Cast.Player"; type: Actor; },
        { varKey: "Cast.FireMage"; type: Actor; },
        { varKey: "Zone.Arena"; type: Actor; },
        { varKey: "Zone.Center"; type: Actor; },
        { varKey: "SpawnPoint.Add1"; type: Location; },
        { varKey: "SpawnPoint.Add2"; type: Location; }
    ];

    outcomes: [
        { condition: ActorIsDead {
            actor: SceneVar { actorVar: "Cast.FireMage"; }; };
          resultCode: 1; },
        { condition: ActorIsDead {
            actor: SceneVar { actorVar: "Cast.Player"; }; };
          resultCode: 0; }
    ];

    rootAct: Parallel { policy: WaitAny; acts: [

        // ══════════════════════════════════════════
        // 分支 1：主线状态机
        // ══════════════════════════════════════════
        StateMachine {
            phases: [
                // ─── 开场演出 ───
                Phase {
                    phaseKey: "Phase_Intro";
                    onEnter: WithActorControl {
                        targets: SceneVar { actorVar: "Cast.FireMage"; };
                        mode: Immediate;
                        body: Cinematic {
                            skippable: true;
                            body: Sequence { acts: [
                                Camera { action: FocusOn {
                                    target: SceneVar { actorVar: "Cast.FireMage"; };
                                    blendTime: 0.5; }; await: UntilComplete; },
                                PlayAnimation { animName: "taunt_gesture";
                                    blendInTime: 0.2; await: UntilComplete; },
                                Dialogue { dialogueId: 8001; await: UntilComplete; },
                                Camera { action: Restore { blendTime: 0.3; };
                                    await: UntilComplete; }
                            ]};
                            onSkip: Camera { action: Restore { blendTime: 0.1; };
                                await: FireAndForget; };
                        };
                    };
                    onExit: None {};
                    transitions: [];
                    autoAdvance: ToPhase { phaseKey: "Phase_Combat"; };
                },

                // ─── 主战斗 ───
                Phase {
                    phaseKey: "Phase_Combat";
                    onEnter: None {};
                    // 法师 AI 全权自治：
                    // - 远距离 → 蓄力火球（Charged，蓄满自动释放）
                    // - 中距离 → 火雨引导（Channel，站桩 4 秒）
                    // - 近距离 → 火焰冲击连招（Instant ×2）
                    // - 被逼近 → 后撤走位
                    // 所有施法生命周期由 GAS 托管，AI 只发指令
                    transitions: [];
                    autoAdvance: None {};
                },

                // ─── 狂暴阶段 ───
                Phase {
                    phaseKey: "Phase_Enraged";
                    onEnter: Sequence { acts: [
                        // 接管法师播放狂暴演出
                        WithActorControl {
                            targets: SceneVar { actorVar: "Cast.FireMage"; };
                            mode: Polite;
                            // Polite 模式：如果法师正在蓄力/引导，
                            // 等当前技能 Executing 完成后再接管。
                            // 不会粗暴打断正在引导的火雨。
                            body: Sequence { acts: [
                                // 临时霸体
                                ApplyEffect {
                                    target: ContextTargets {};
                                    effect: GrantTags {
                                        grantedTags: ["State.Buff.Hyperarmor"];
                                        duration: Const { value: 3.0; };
                                    };
                                },
                                PlayAnimation { animName: "enrage_channel";
                                    blendInTime: 0.1; await: UntilComplete; },
                                Camera { action: Shake { preset: "heavy"; };
                                    await: FireAndForget; },
                                PlayCue { cueKey: "Cue.FireMage.EnrageBurst";
                                    playAt: SceneVar { actorVar: "Cast.FireMage"; }; }
                            ]};
                        },
                        // 永久增益
                        ApplyEffect {
                            target: SceneVar { actorVar: "Cast.FireMage"; };
                            effect: ApplyStatus { statusId: 6030; };
                            // Status 6030 内含：
                            // - StatModifier: CastSpeed × 1.5
                            // - StatModifier: Attack_Power × 1.3
                            // - AIModifier: 注入"急速火球"行为（cooldown 缩短）
                        },
                        // 刷两只火元素小怪
                        SpawnActor { outputVarKey: "Var.Add1";
                            archetype: "FireElemental"; spawnAt: "SpawnPoint.Add1"; },
                        SpawnActor { outputVarKey: "Var.Add2";
                            archetype: "FireElemental"; spawnAt: "SpawnPoint.Add2"; }
                    ]};
                    transitions: [];
                    autoAdvance: None {};
                }
            ];

            globalTransitions: [
                { condition: And { conditions: [
                    ActorStatCompare {
                        actor: SceneVar { actorVar: "Cast.FireMage"; };
                        statTag: "HP_Current"; op: LessEqual;
                        value: Math { op: Mul;
                            a: ActorStat {
                                actor: SceneVar { actorVar: "Cast.FireMage"; };
                                statTag: "HP_Max"; };
                            b: Const { value: 0.35; }; };
                    },
                    Not { condition: CurrentPhaseIs {
                        phaseKey: "Phase_Enraged"; }; }
                ]}; target: ToPhase { phaseKey: "Phase_Enraged"; }; }
            ];
        },

        // ══════════════════════════════════════════
        // 分支 2：施法被打断时的场景反应
        // ══════════════════════════════════════════
        Loop { count: -1; body:
            WaitForEvent {
                event: "Ability_Interrupted";
                source: SceneVar { actorVar: "Cast.FireMage"; };
                conditions: [];
                timeoutSec: 0; onTimeout: None {};
                body: Sequence { acts: [
                    // 法师被打断 → 场景播放嘲讽提示
                    PlayCue { cueKey: "Cue.UI.BossStaggered";
                        playAt: SceneVar { actorVar: "Cast.Player"; }; },
                    // 短暂易伤窗口（场景级奖励机制）
                    WithStatus {
                        target: SceneVar { actorVar: "Cast.FireMage"; };
                        statusId: 6040; // "破绽暴露"：受伤 +30%
                        body: WaitSeconds { duration: Const { value: 3.0; }; };
                    }
                ]};
                extractPayloads: [];
            };
        },

        // ══════════════════════════════════════════
        // 分支 3：竞技场边界监控
        // ══════════════════════════════════════════
        Loop { count: -1; body: Sequence { acts: [
            WaitUntil {
                condition: Not { condition: ActorInZone {
                    actor: SceneVar { actorVar: "Cast.Player"; };
                    zone: SceneVar { actorVar: "Zone.Arena"; };
                }; };
                timeoutSec: 0; onTimeout: None {};
            },
            // 玩家离场 → 法师瞬移回中心 + 回满血
            ApplyEffect {
                target: SceneVar { actorVar: "Cast.FireMage"; };
                effect: Sequence { effects: [
                    Teleport { destination: ActorLocation {
                        target: ContextVar { varKey: "Zone.Center"; }; }; },
                    ModifyStat { stat: "HP_Current"; op: Override;
                        value: StatValue { source: ContextTarget {};
                            stat: "HP_Max"; }; }
                ]};
            },
            // 等玩家回来
            WaitUntil {
                condition: ActorInZone {
                    actor: SceneVar { actorVar: "Cast.Player"; };
                    zone: SceneVar { actorVar: "Zone.Arena"; };
                };
                timeoutSec: 0; onTimeout: None {};
            }
        ]}; }

    ]}; // End Parallel
}

// ═══ 破绽暴露 Status ═══
status {
    id: 6040;
    name: "破绽暴露";
    statusTags: ["Status.Type.Debuff"];
    grantedTags: ["State.Debuff.Vulnerable"];
    duration: Const { value: -1; }; // 由 WithStatus RAII 控制
    stackingPolicy: Single { refreshMode: KeepDuration; };
    cuesWhileActive: ["Cue.Debuff.Vulnerable"];
    core: { behaviors: [
        Trigger {
            listenEvent: "Combat_Damage_Take_Pre";
            effect: ModifyPayloadMagnitude {
                op: Mul;
                value: Const { value: 1.3; }; // 受伤 +30%
            };
        }
    ]; };
}
```

### 三层联动全景

```
┌───────────────────────────────────────────────────────────────┐
│  Scene Layer                                                  │
│                                                               │
│  StateMachine: Intro → Combat → Enraged(HP≤35%)              │
│  ├─ Polite 接管：等法师当前技能完成再演出                        │
│  ├─ 监听 Ability_Interrupted → WithStatus(破绽暴露, 3s)       │
│  └─ 边界监控：离场 → Teleport + 满血                           │
│                                                               │
│  Scene 关心的是"宏观节奏"，不关心法师每秒在放什么技能              │
└────────────────────────────┬──────────────────────────────────┘
                             │ SpawnActor / ApplyEffect / WithActorControl
                             ▼
┌───────────────────────────────────────────────────────────────┐
│  AI Layer                                                     │
│                                                               │
│  Think 评分：                                                  │
│  ┌─────────────────────────────────────────┐                  │
│  │ 距离 > 8m:  蓄力火球(40+距离×2) ▓▓▓▓▓▓ │ ← Charged        │
│  │ 任意距离:   火雨引导(55)        ▓▓▓▓   │ ← Channel        │
│  │ 距离 ≤ 5m:  火焰冲击(65)       ▓▓▓▓▓  │ ← Instant×2      │
│  │ 距离 < 6m:  后撤(70)          ▓▓▓▓▓▓  │ ← 保命优先       │
│  │ 6~10m:      重新定位(25)       ▓▓      │ ← 填充          │
│  └─────────────────────────────────────────┘                  │
│                                                               │
│  Act 执行：CastAbility → 引擎接管 Ability 生命周期              │
│  AI 不知道"蓄力进度""引导心跳"的细节，只等返回 Success/Failed    │
└────────────────────────────┬──────────────────────────────────┘
                             │ CastAbility / CanActivateAbility
                             ▼
┌───────────────────────────────────────────────────────────────┐
│  GAS Layer — Ability 生命周期                                  │
│                                                               │
│  蓄力火球 (Charged):                                           │
│  Activate → Processing[蓄力0~2.5s] → Execute[火球飞出] → Rec  │
│     │            │                                            │
│     │     被眩晕 interruptsAbilities                           │
│     │            ↓                                            │
│     │     onInterrupt: 反噬20伤 + 锁定0.8s                     │
│     │     → AI CastAbility 返回 Failed → 重新决策              │
│     │                                                         │
│  火雨 (Channel):                                               │
│  Activate → Processing[心跳×8, 每0.5s AOE] → Execute[燃烧区]   │
│     │            │                              → Recovery    │
│     │     commitPolicy: OnFirstTick                           │
│     │     channelingTags: [Immobile] → 站桩                    │
│     │                                                         │
│  连招 (Instant×2):                                             │
│  一段 → GrantTags(Combo.S2, 1.2s) → Recovery[0.5s]            │
│     二段 activationConditions: HasTag(Combo.S2)               │
│     → cancelsAbilities cancel 一段后摇 → 无缝衔接              │
│                                                               │
│  Pipeline: Pre(增幅层+30%) → CheckStage(暴击) → 扣血 → Post   │
│  TagRules: 眩晕=interrupt+block, 翻滚=cancel                  │
│  被动: Ability_Executed → 叠火焰增幅, Interrupted → 清增幅层    │
└───────────────────────────────────────────────────────────────┘
```

---

## 示例二：双Boss 协同战 — 骑士 + 牧师

**玩法概述**：玩家同时面对一个骑士 Boss（近战冲锋 + 读条大招）和一个牧师 Boss（引导治疗骑士 + 读条群体治疗）。核心机制：打断牧师的引导治疗是取胜关键。骑士在牧师存活时拥有光环增益。骑士死亡后牧师进入狂暴。

### 第一层：GAS — 核心技能与状态

```
// ═══════════════════════════════════════════
// 骑士技能
// ═══════════════════════════════════════════

// 技能：盾牌冲锋 (CastBar — 短读条后冲锋撞击)
ability {
    abilityId: 2001;
    name: "盾牌冲锋";
    abilityTags: ["Ability.Type.Melee", "Ability.Type.Movement"];

    costs: [{ stat: "Mana_Current"; value: Const { value: 20; }; }];
    cooldown: Const { value: 8.0; };

    targeting: SingleTarget {
        allowedRelations: [Hostile];
        tagQuery: { exclude: ["State.Dead"]; };
        maxRange: Const { value: 15.0; };
        onTargetLost: Cancel;
    };

    castMode: CastBar {
        castTime: Const { value: 0.8; };
        castingTags: ["State.Casting.Melee", "State.Buff.Hyperarmor"];
        // 读条期间自带霸体 → immuneToTags: State.Debuff.Control
        cuesDuringCast: ["Cue.Charge.Shield"];
        commitPolicy: OnActivate; // 激活即扣费
    };

    effect: WithTarget {
        target: ContextVar { varKey: "Var.AbilityTarget"; };
        effect: Sequence { effects: [
            // 冲锋位移（通过 RootMotion Status 实现）
            WithTarget {
                target: ContextInstigator {};
                effect: ApplyStatusInline {
                    core: {
                        grantedTags: ["State.Charging"];
                        duration: Const { value: 0.6; };
                        behaviors: [
                            RootMotion {
                                motionType: ...; // 向目标方向冲锋
                                onSweepHit: {
                                    effect: ResolveCombat {
                                        pipeline: "StandardPhysicalDamage";
                                        magnitude: Math { op: Mul;
                                            a: StatValue { source: ContextInstigator {};
                                                stat: "Attack_Power"; };
                                            b: Const { value: 2.0; }; };
                                        tags: ["Damage.Type.Charge"];
                                        cuesOnExecute: ["Cue.Hit.ShieldBash"];
                                    };
                                };
                            }
                        ];
                    };
                };
            },
            // 撞击后给目标挂眩晕
            ApplyStatus { statusId: 7010; } // 1.5 秒眩晕
        ]};
    };

    onInterrupt: [
        FireCue { cue: "Cue.Charge.Failed"; }
    ];
    recovery: { duration: Const { value: 1.0; };
        recoveryTags: ["State.Recovery.Heavy"]; };
}

// 技能：天堂之锤 (CastBar — 长读条大招)
ability {
    abilityId: 2002;
    name: "天堂之锤";
    abilityTags: ["Ability.Type.Melee", "Ability.AOE"];

    costs: [{ stat: "Mana_Current"; value: Const { value: 60; }; }];
    cooldown: Const { value: 20.0; };

    targeting: None {};

    castMode: CastBar {
        castTime: Const { value: 3.0; }; // 长达 3 秒的读条
        castingTags: ["State.Casting.Melee", "State.Immobile"];
        // 站桩读条，不带霸体 → 可以被打断！
        cuesDuringCast: ["Cue.Cast.HeavenHammer"];
        commitPolicy: OnComplete;
    };

    effect: Sequence { effects: [
        FireCue { cue: "Cue.Slam.HeavenHammer"; },
        Camera { action: Shake { preset: "massive"; }; },
        WithTargets {
            targets: TargetScan {
                shape: Sphere {
                    center: ContextInstigator {};
                    radius: Const { value: 8.0; };
                };
                relationTo: ContextInstigator {};
                allowedRelations: [Hostile];
                maxCount: -1;
            };
            effect: ResolveCombat {
                pipeline: "StandardPhysicalDamage";
                magnitude: Math { op: Mul;
                    a: StatValue { source: ContextInstigator {};
                        stat: "Attack_Power"; };
                    b: Const { value: 4.0; }; };
                tags: ["Damage.Type.Slam", "Damage.AOE"];
                cuesOnExecute: ["Cue.Hit.HeavenHammer"];
            };
        }
    ]};

    onInterrupt: [
        // 大招被打断 → 骑士短暂虚弱
        WithTarget {
            target: ContextInstigator {};
            effect: ApplyStatus { statusId: 7020; }; // "破势" 3 秒
        },
        FireCue { cue: "Cue.Cast.Interrupted.Heavy"; }
    ];
    recovery: { duration: Const { value: 1.5; };
        recoveryTags: ["State.Recovery.Heavy"]; };
}

// ═══════════════════════════════════════════
// 牧师技能
// ═══════════════════════════════════════════

// 技能：神圣引导 (Channel — 持续治疗骑士)
ability {
    abilityId: 3001;
    name: "神圣引导";
    abilityTags: ["Ability.Type.Spell", "Ability.Type.Heal"];

    costs: [{ stat: "Mana_Current"; value: Const { value: 50; }; }];
    cooldown: Const { value: 10.0; };

    targeting: SingleTarget {
        allowedRelations: [Friendly];
        tagQuery: { exclude: ["State.Dead"]; };
        maxRange: Const { value: 20.0; };
        onTargetLost: Cancel;
        // 骑士死亡 → 目标丢失 → Cancel → 引导中止（无惩罚）
    };

    castMode: Channel {
        duration: Const { value: 5.0; };
        tickInterval: Const { value: 1.0; };
        maxTicks: 5;
        tickOnStart: true;
        tickEffect: WithTarget {
            target: ContextVar { varKey: "Var.AbilityTarget"; };
            effect: ResolveCombat {
                pipeline: "StandardHeal";
                magnitude: Math { op: Mul;
                    a: StatValue { source: ContextInstigator {};
                        stat: "Attack_Power"; };
                    b: Const { value: 1.2; }; };
                cuesOnExecute: ["Cue.Heal.HolyPulse"];
            };
        };
        channelingTags: ["State.Casting.Spell", "State.Immobile"];
        cuesDuringChannel: ["Cue.Channel.HolyBeam"];
        commitPolicy: OnFirstTick;
    };

    // 引导完成后：给骑士挂一个持续回血 HOT
    effect: WithTarget {
        target: ContextVar { varKey: "Var.AbilityTarget"; };
        effect: ApplyStatus { statusId: 7030; }; // 神圣余韵 HOT
    };

    onInterrupt: [
        // 引导被打断 → 牧师短暂沉默自己
        WithTarget {
            target: ContextInstigator {};
            effect: GrantTags {
                grantedTags: ["State.Debuff.Silence"];
                duration: Const { value: 2.0; };
            };
        },
        FireCue { cue: "Cue.Cast.Interrupted.Holy"; }
    ];
    recovery: { duration: Const { value: 0.5; };
        recoveryTags: ["State.Recovery"]; };
}

// 技能：群体祝福 (CastBar — 读条群体治疗)
ability {
    abilityId: 3002;
    name: "群体祝福";
    abilityTags: ["Ability.Type.Spell", "Ability.Type.Heal", "Ability.AOE"];

    costs: [{ stat: "Mana_Current"; value: Const { value: 80; }; }];
    cooldown: Const { value: 15.0; };

    targeting: None {};

    castMode: CastBar {
        castTime: Const { value: 2.5; };
        castingTags: ["State.Casting.Spell", "State.Immobile"];
        cuesDuringCast: ["Cue.Cast.MassBlessing"];
        commitPolicy: OnComplete;
    };

    effect: WithTargets {
        targets: TargetScan {
            shape: Sphere {
                center: ContextInstigator {};
                radius: Const { value: 12.0; };
            };
            relationTo: ContextInstigator {};
            allowedRelations: [Friendly, Self];
            maxCount: -1;
        };
        effect: Sequence { effects: [
            ResolveCombat {
                pipeline: "StandardHeal";
                magnitude: Math { op: Mul;
                    a: StatValue { source: ContextInstigator {};
                        stat: "Attack_Power"; };
                    b: Const { value: 2.0; }; };
                cuesOnExecute: ["Cue.Heal.MassBlessing"];
            },
            ApplyStatus { statusId: 7031; } // 防御祝福 3 秒
        ]};
    };

    onInterrupt: [
        GrantTags {
            grantedTags: ["State.Debuff.Silence"];
            duration: Const { value: 3.0; };
        },
        FireCue { cue: "Cue.Cast.Interrupted.Holy"; }
    ];
    recovery: { duration: Const { value: 0.6; };
        recoveryTags: ["State.Recovery"]; };
}

// ═══ 配套 Status ═══

// 眩晕
status { id: 7010; name: "冲锋眩晕";
    grantedTags: ["State.Debuff.Control.Stun"];
    duration: Const { value: 1.5; };
    stackingPolicy: Single { refreshMode: ResetDuration; };
    cuesWhileActive: ["Cue.Debuff.Stun"]; }

// 骑士破势（被打断后虚弱）
status { id: 7020; name: "破势";
    grantedTags: ["State.Debuff.Weakened"];
    duration: Const { value: 3.0; };
    stackingPolicy: Single { refreshMode: ResetDuration; };
    core: { behaviors: [
        Trigger {
            listenEvent: "Combat_Damage_Take_Pre";
            effect: ModifyPayloadMagnitude { op: Mul;
                value: Const { value: 1.5; }; }; // 受伤 +50%
        }
    ]; }; }

// 神圣余韵 HOT
status { id: 7030; name: "神圣余韵";
    grantedTags: ["State.Buff.HolyRegen"];
    duration: Const { value: 10.0; };
    stackingPolicy: Single { refreshMode: ResetDuration; };
    core: { behaviors: [
        Periodic {
            period: Const { value: 2.0; };
            executeOnApply: false;
            effect: ResolveCombat {
                pipeline: "StandardHeal";
                magnitude: Const { value: 30; };
            };
        }
    ]; }; }

// 防御祝福
status { id: 7031; name: "防御祝福";
    grantedTags: ["State.Buff.DefenseBlessing"];
    duration: Const { value: 5.0; };
    stackingPolicy: Single { refreshMode: ResetDuration; };
    core: { behaviors: [
        Trigger {
            listenEvent: "Combat_Damage_Take_Pre";
            effect: ModifyPayloadMagnitude { op: Mul;
                value: Const { value: 0.7; }; };
        }
    ]; }; }

// 骑士光环（牧师存活时给骑士的增益 — 由 Scene WithStatus 管理）
status { id: 7040; name: "圣光共鸣";
    grantedTags: ["State.Buff.HolyResonance"];
    duration: Const { value: -1; };
    stackingPolicy: Single { refreshMode: KeepDuration; };
    cuesWhileActive: ["Cue.Aura.HolyResonance"];
    core: { behaviors: [
        StatModifier { stat: "Attack_Power"; op: Mul;
            value: Const { value: 1.3; }; },
        // 受击时概率触发护盾
        Trigger {
            listenEvent: "Combat_Damage_Take_Post";
            requiresAll: [
                Chance { probability: Const { value: 0.2; }; }
            ];
            cooldown: Const { value: 5.0; };
            effect: WithTarget {
                target: ContextTarget {};
                effect: ModifyStat { stat: "Shield_Current"; op: Add;
                    value: Const { value: 50; }; };
            };
        }
    ]; }; }

// 牧师狂暴（骑士死后激活 — 由 Scene ApplyStatus 管理）
status { id: 7050; name: "圣光之怒";
    statusTags: ["Status.Type.Enrage"];
    grantedTags: ["State.Buff.Enrage", "State.Phase.Priest.Enraged"];
    duration: Const { value: -1; };
    stackingPolicy: Single { refreshMode: KeepDuration; };
    cuesWhileActive: ["Cue.Aura.HolyWrath"];
    core: { behaviors: [
        StatModifier { stat: "Attack_Power"; op: Mul;
            value: Const { value: 2.0; }; },
        // 被动：每次施法后回蓝
        Trigger {
            listenEvent: "Ability_Executed";
            effect: WithTarget { target: ContextTarget {};
                effect: ModifyStat { stat: "Mana_Current"; op: Add;
                    value: Const { value: 25; }; }; };
        }
    ]; }; }
```

### 第二层：AI — 双Boss 各自的战术大脑

```
// ═══════════════════════════════════════════
// 骑士 AI
// ═══════════════════════════════════════════
ai_archetype {
    name: "Boss_Knight";
    goalGenerators: ["Gen_Scan_Enemy"];
    behaviorGroups: ["Group_Knight"];
    defaultBehavior: "Behavior_Knight_Idle";
    perceiveInterval: 0.5;
    thinkInterval: 0.3;
}

// 行为：盾牌冲锋（CastBar 读条 → 冲撞）
ai_behavior {
    behaviorId: 8001;
    name: "Behavior_Knight_Charge";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.Gap"];
    isInterruptible: false;
    minCommitmentTime: 1.0;

    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { CanActivateAbility { abilityId: 2001; } },
        // 距离 7~15m 才冲锋
        { Compare { a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Gte; b: Const { value: 7.0; }; }; }
    ];
    cooldown: 0;
    score: Const { value: 70.0; };
    scoreInertia: 10.0;

    task: CastAbility {
        abilityId: 2001;
        target: BoundGoalActor {};
        // CastAbility 封装整个 CastBar 过程：
        // 0.8s 读条（自带霸体）→ 冲锋位移 → 眩晕 → 1.0s 重型后摇
    };
    abortConditions: [
        { TargetIsDead { target: BoundGoalActor {}; } }
    ];
}

// 行为：天堂之锤（长读条大招）
// 关键：不带霸体，3 秒读条可被打断
// AI 选择此行为说明它判断"现在安全"
ai_behavior {
    behaviorId: 8002;
    name: "Behavior_Knight_HeavenHammer";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.Ultimate"];
    isInterruptible: false;
    minCommitmentTime: 0;

    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { CanActivateAbility { abilityId: 2002; } },
        // 近距离才使用
        { Compare { a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Lte; b: Const { value: 8.0; }; }; },
        // 目标被眩晕时更安全地放大招
        { TargetHasTags { target: BoundGoalActor {};
            tagQuery: { requireAny: ["State.Debuff.Control"]; }; }; }
    ];
    cooldown: 0;
    score: Const { value: 85.0; }; // 高分但条件苛刻

    task: Sequence { tasks: [
        MoveTo { target: BoundGoalActor {};
            speedStat: "Stat.MoveSpeed"; tolerance: 5.0; stopOnFinish: true; },
        CastAbility { abilityId: 2002; target: BoundGoalActor {}; }
        // 如果读条被打断 → onInterrupt 挂"破势" → CastAbility 返回 Failed
        // → AI 下帧重新决策
    ]};
    abortConditions: [];
}

// 行为：普通劈砍
ai_behavior {
    behaviorId: 8003;
    name: "Behavior_Knight_Slash";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.Melee"];
    isInterruptible: true;
    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { Compare { a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Lte; b: Const { value: 4.0; }; }; }
    ];
    cooldown: 0;
    score: Const { value: 40.0; };

    task: Sequence { tasks: [
        MoveTo { target: BoundGoalActor {};
            speedStat: "Stat.MoveSpeed"; tolerance: 2.5; stopOnFinish: true; },
        CastAbility { abilityId: 2010; target: BoundGoalActor {}; }
        // 普通劈砍（Instant）
    ]};
    abortConditions: [
        { TargetIsDead { target: BoundGoalActor {}; } }
    ];
}

// ═══════════════════════════════════════════
// 牧师 AI
// ═══════════════════════════════════════════
ai_archetype {
    name: "Boss_Priest";
    goalGenerators: ["Gen_Scan_Enemy", "Gen_Scan_FriendlyWounded"];
    behaviorGroups: ["Group_Priest"];
    defaultBehavior: "Behavior_Priest_Idle";
    perceiveInterval: 0.5;
    thinkInterval: 0.3;
}

ai_goal_definition {
    name: "Goal.Heal.Ally";
    goalId: 3001;
    validConditions: [
        TargetIsValid {},
        TargetHasTags { requirements: { exclude: ["State.Dead"]; }; }
    ];
    validationInterval: 1.0;
}

ai_goal_generator {
    name: "Gen_Scan_FriendlyWounded";
    generator: SpatialScan {
        query: SphereScan {
            center: Self {};
            shape: { radius: 20.0;
                requiredTags: { requireAny: ["Faction.Friendly"]; };
                sortBy: Nearest; maxResults: 3; };
        };
        interval: 1.0;
        generatedGoal: "Goal.Heal.Ally";
    };
}

// 行为：神圣引导（Channel 治疗骑士）
// AI 发出 CastAbility 后，引擎处理整个 Channel：
// - 每秒一次 tickEffect 治疗
// - 站桩不动（channelingTags: Immobile）
// - 被玩家打断 → onInterrupt 自我沉默 2 秒
// - 骑士死亡 → onTargetLost: Cancel → 引导中止（无惩罚）
ai_behavior {
    behaviorId: 9001;
    name: "Behavior_Priest_HealChannel";
    behaviorTags: ["Behavior.Tag.Support", "Behavior.Tag.Heal"];
    isInterruptible: false;
    minCommitmentTime: 1.0;

    requiredGoal: Require { goalDef: "Goal.Heal.Ally"; selector: Strongest; };
    // Strongest → 选 magnitude 最大的（即血量损失最多的友方）
    preConditions: [
        { CanActivateAbility { abilityId: 3001; } },
        // 只在骑士血量 < 70% 时治疗
        { Compare {
            a: StatValue { target: BoundGoalActor {}; statTag: "HP_Current"; };
            op: Lt;
            b: Math { op: Mul;
                a: StatValue { target: BoundGoalActor {}; statTag: "HP_Max"; };
                b: Const { value: 0.7; }; };
        }; }
    ];
    cooldown: 0;
    // 骑士血量越低，治疗优先级越高
    score: Math { op: Sub;
        a: Const { value: 100.0; };
        b: Math { op: Mul;
            a: StatValue { target: BoundGoalActor {}; statTag: "HP_Current"; };
            b: Const { value: 0.05; }; };
    };
    scoreInertia: 25.0; // 引导中高惯性，防止被打断后立即重选

    task: CastAbility {
        abilityId: 3001;
        target: BoundGoalActor {};
    };
    abortConditions: [
        { TargetIsDead { target: BoundGoalActor {}; } }
    ];
}

// 行为：群体祝福（CastBar 读条群体治疗）
ai_behavior {
    behaviorId: 9002;
    name: "Behavior_Priest_MassBlessing";
    behaviorTags: ["Behavior.Tag.Support", "Behavior.Tag.AOE"];
    isInterruptible: false;
    minCommitmentTime: 0;

    requiredGoal: None {};
    preConditions: [
        { CanActivateAbility { abilityId: 3002; } }
    ];
    cooldown: 0;
    score: Const { value: 50.0; };

    task: Sequence { tasks: [
        // 先走到骑士附近（确保群体治疗覆盖骑士）
        WithLocalVar {
            bindings: [{
                varKey: "Var.KnightPos";
                value: ActorOrLocation {
                    selector: RuntimeQuery {
                        query: SphereScan {
                            center: Self {};
                            shape: { radius: 25.0;
                                requiredTags: { requireAny: ["Faction.Friendly"]; };
                                sortBy: Nearest; maxResults: 1; };
                        };
                    };
                };
            }];
            body: MoveTo { target: LocalVarActor { varKey: "Var.KnightPos"; };
                speedStat: "Stat.MoveSpeed"; tolerance: 8.0; stopOnFinish: true; };
        },
        CastAbility { abilityId: 3002; target: Self {}; }
        // 2.5 秒读条。被打断 → 自我沉默 3 秒。
    ]};
    abortConditions: [];
}

// 行为：远离敌人（保持安全距离）
ai_behavior {
    behaviorId: 9003;
    name: "Behavior_Priest_Kite";
    behaviorTags: ["Behavior.Tag.Tactical"];
    isInterruptible: true;
    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { Compare { a: Distance { from: Self {}; to: BoundGoalActor {}; };
            op: Lt; b: Const { value: 8.0; }; }; }
    ];
    cooldown: 3.0;
    score: Const { value: 65.0; };

    task: MoveTo {
        target: RuntimeQuery { ... }; // 远离敌人方向
        speedStat: "Stat.MoveSpeed"; tolerance: 1.0; stopOnFinish: false;
    };
    abortConditions: [];
}

// 行为：牧师的轻攻击（填充）
ai_behavior {
    behaviorId: 9004;
    name: "Behavior_Priest_Smite";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.Ranged"];
    isInterruptible: true;
    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { CanActivateAbility { abilityId: 3010; } }
    ];
    cooldown: 0;
    score: Const { value: 30.0; };
    task: Sequence { tasks: [
        MoveTo { target: BoundGoalActor {};
            speedStat: "Stat.MoveSpeed"; tolerance: 12.0; stopOnFinish: true; },
        CastAbility { abilityId: 3010; target: BoundGoalActor {}; }
    ]};
    abortConditions: [
        { TargetIsDead { target: BoundGoalActor {}; } }
    ];
}

// 牧师狂暴后注入的行为（通过 AIModifier）
ai_behavior_modifier {
    name: "Mod_Priest_Enraged";
    disableBehaviorsQuery: { requireAny: ["Behavior.Tag.Heal"]; };
    // 狂暴后不再治疗
    injectedBehaviors: [{
        targetGroup: "Group_Priest";
        behaviors: ["Behavior_Priest_HolyWrath"];
    }];
}

// 牧师狂暴专属行为：神圣之怒（引导型 AOE 攻击）
ai_behavior {
    behaviorId: 9010;
    name: "Behavior_Priest_HolyWrath";
    behaviorTags: ["Behavior.Tag.Combat", "Behavior.Tag.AOE"];
    isInterruptible: false;
    requiredGoal: Require { goalDef: "Goal.Combat.Enemy"; selector: Nearest; };
    preConditions: [
        { CanActivateAbility { abilityId: 3020; } }
    ];
    cooldown: 0;
    score: Const { value: 75.0; };

    task: Sequence { tasks: [
        MoveTo { target: BoundGoalActor {};
            speedStat: "Stat.MoveSpeed"; tolerance: 10.0; stopOnFinish: true; },
        CastAbility { abilityId: 3020; target: BoundGoalActor {}; }
    ]};
    abortConditions: [];
}
```

### 第三层：Scene — 双Boss 遭遇战编排

```
scene_definition {
    sceneId: 8002;
    name: "骑士与牧师双Boss战";

    signature: [
        { varKey: "Cast.Player"; type: Actor; },
        { varKey: "Cast.Knight"; type: Actor; },
        { varKey: "Cast.Priest"; type: Actor; },
        { varKey: "Zone.Arena"; type: Actor; }
    ];

    outcomes: [
        // 胜利：双 Boss 全灭
        { condition: And { conditions: [
            ActorIsDead { actor: SceneVar { actorVar: "Cast.Knight"; }; },
            ActorIsDead { actor: SceneVar { actorVar: "Cast.Priest"; }; }
        ]}; resultCode: 1; },
        // 失败
        { condition: ActorIsDead {
            actor: SceneVar { actorVar: "Cast.Player"; }; };
          resultCode: 0; }
    ];

    rootAct: Parallel { policy: WaitAny; acts: [

        // ══════════════════════════════════════════
        // 分支 1：主线状态机
        // ══════════════════════════════════════════
        StateMachine {
            phases: [
                Phase {
                    phaseKey: "Phase_Intro";
                    onEnter: Sequence { acts: [
                        // 双 Boss 同时入场演出
                        Parallel { policy: WaitAll; acts: [
                            WithActorControl {
                                targets: SceneVar { actorVar: "Cast.Knight"; };
                                mode: Immediate;
                                body: Sequence { acts: [
                                    PlayAnimation { animName: "draw_sword";
                                        blendInTime: 0.2; await: UntilComplete; }
                                ]};
                            },
                            WithActorControl {
                                targets: SceneVar { actorVar: "Cast.Priest"; };
                                mode: Immediate;
                                body: Sequence { acts: [
                                    PlayAnimation { animName: "prayer_gesture";
                                        blendInTime: 0.2; await: UntilComplete; }
                                ]};
                            }
                        ]},
                        // 牧师台词
                        WithActorControl {
                            targets: SceneVar { actorVar: "Cast.Priest"; };
                            mode: Immediate;
                            body: Dialogue { dialogueId: 8201; await: UntilComplete; };
                        }
                    ]};
                    transitions: [];
                    autoAdvance: ToPhase { phaseKey: "Phase_DualBoss"; };
                },

                // ─── 双 Boss 共存阶段 ───
                Phase {
                    phaseKey: "Phase_DualBoss";
                    onEnter: Parallel { policy: WaitAll; acts: [
                        // 骑士在牧师存活期间获得光环增益（RAII）
                        WithStatus {
                            target: SceneVar { actorVar: "Cast.Knight"; };
                            statusId: 7040; // 圣光共鸣
                            body: WaitUntil {
                                condition: ActorIsDead {
                                    actor: SceneVar { actorVar: "Cast.Priest"; }; };
                                timeoutSec: 0; onTimeout: None {};
                            };
                            // 牧师死亡 → WithStatus 退出 → 光环 RAII 清除
                        }
                    ]};
                    // 两个 Boss AI 各自独立战斗：
                    // - 骑士：冲锋 → 劈砍 → 眩晕目标后放天堂之锤
                    // - 牧师：引导治疗骑士 → 群体祝福 → 远离敌人 → 轻攻击
                    // 所有施法生命周期由 GAS 托管
                    transitions: [];
                    autoAdvance: None {};
                },

                // ─── 牧师独战阶段（骑士死后） ───
                Phase {
                    phaseKey: "Phase_PriestSolo";
                    onEnter: Sequence { acts: [
                        // 牧师哀嚎演出
                        WithActorControl {
                            targets: SceneVar { actorVar: "Cast.Priest"; };
                            mode: Polite;
                            // Polite: 如果牧师正在引导治疗（Channel），
                            // 骑士死亡 → onTargetLost: Cancel → Channel 取消
                            // → 牧师空闲后被 Polite 接管
                            body: Sequence { acts: [
                                ApplyEffect {
                                    target: ContextTargets {};
                                    effect: GrantTags {
                                        grantedTags: ["State.Buff.Hyperarmor"];
                                        duration: Const { value: 4.0; };
                                    };
                                },
                                PlayAnimation { animName: "grief_scream";
                                    blendInTime: 0.1; await: UntilComplete; },
                                Dialogue { dialogueId: 8202; await: UntilComplete; },
                                Camera { action: Shake { preset: "medium"; };
                                    await: FireAndForget; }
                            ]};
                        },
                        // 挂载狂暴 Status（内含 AIModifier）
                        ApplyEffect {
                            target: SceneVar { actorVar: "Cast.Priest"; };
                            effect: ApplyStatus { statusId: 7050; };
                            // Status 7050 "圣光之怒"：
                            // - ATK ×2
                            // - 每次施法后回蓝
                            // - AIModifier "Mod_Priest_Enraged"：
                            //   禁用治疗行为，注入"神圣之怒"攻击引导
                        }
                    ]};
                    transitions: [];
                    autoAdvance: None {};
                }
            ];

            globalTransitions: [
                // 骑士死亡 → 进入牧师独战
                { condition: And { conditions: [
                    ActorIsDead { actor: SceneVar { actorVar: "Cast.Knight"; }; },
                    ActorIsAlive { actor: SceneVar { actorVar: "Cast.Priest"; }; },
                    Not { condition: CurrentPhaseIs {
                        phaseKey: "Phase_PriestSolo"; }; }
                ]}; target: ToPhase { phaseKey: "Phase_PriestSolo"; }; }
            ];
        },

        // ══════════════════════════════════════════
        // 分支 2：打断奖励监听
        // ══════════════════════════════════════════
        // 当玩家成功打断牧师的引导/读条时，场景给予战术提示
        Loop { count: -1; body:
            WaitForEvent {
                event: "Ability_Interrupted";
                source: SceneVar { actorVar: "Cast.Priest"; };
                conditions: [];
                timeoutSec: 0; onTimeout: None {};
                body: Sequence { acts: [
                    PlayCue { cueKey: "Cue.UI.InterruptSuccess";
                        playAt: SceneVar { actorVar: "Cast.Player"; }; },
                    // 奖励：短暂增加玩家暴击率
                    ApplyEffect {
                        target: SceneVar { actorVar: "Cast.Player"; };
                        effect: ApplyStatusInline {
                            core: {
                                grantedTags: ["State.Buff.InterruptBonus"];
                                duration: Const { value: 5.0; };
                                behaviors: [
                                    StatModifier { stat: "Combat_CritRate"; op: Add;
                                        value: Const { value: 0.15; }; }
                                ];
                            };
                        };
                    }
                ]};
                extractPayloads: [];
            };
        },

        // ══════════════════════════════════════════
        // 分支 3：骑士大招读条提示
        // ══════════════════════════════════════════
        // 当骑士开始读条天堂之锤时，给玩家 UI 警告
        Loop { count: -1; body:
            WaitForEvent {
                event: "Ability_Activated";
                source: SceneVar { actorVar: "Cast.Knight"; };
                conditions: [
                    // 检查是否是天堂之锤
                    // （通过事件 payload 中的 abilityTags 判断）
                    ActorHasTags {
                        actor: SceneVar { actorVar: "Cast.Knight"; };
                        quantifier: All;
                        tagQuery: { requireAny: ["State.Casting.Melee"]; };
                    }
                ];
                timeoutSec: 0; onTimeout: None {};
                body: Sequence { acts: [
                    PlayCue { cueKey: "Cue.UI.DangerWarning.Slam";
                        playAt: SceneVar { actorVar: "Cast.Player"; }; },
                    // 3 秒后检查是否被打断（如果没有，播放另一个提示）
                    WaitSeconds { duration: Const { value: 3.0; }; },
                    Conditional {
                        condition: ActorHasTags {
                            actor: SceneVar { actorVar: "Cast.Knight"; };
                            tagQuery: { requireAny: ["State.Casting.Melee"]; };
                        };
                        thenAct: PlayCue { cueKey: "Cue.UI.DangerImminent";
                            playAt: SceneVar { actorVar: "Cast.Player"; }; };
                        elseAct: None {};
                    }
                ]};
                extractPayloads: [];
            };
        }

    ]}; // End Parallel
}
```

### 三层联动全景

```
┌────────────────────────────────────────────────────────────────┐
│  Scene Layer                                                   │
│                                                                │
│  StateMachine: Intro → DualBoss → PriestSolo(骑士死)            │
│                                                                │
│  ├─ WithStatus(圣光共鸣) on Knight                              │
│  │   └─ WaitUntil(Priest死) → RAII 自动剥离光环                 │
│  │                                                             │
│  ├─ 监听 Ability_Interrupted(Priest)                            │
│  │   └─ 奖励玩家暴击 +15% (5s) — 鼓励打断引导                    │
│  │                                                             │
│  ├─ 监听 Ability_Activated(Knight, 天堂之锤)                    │
│  │   └─ UI 危险警告 — 提示玩家打断读条                            │
│  │                                                             │
│  └─ 骑士死亡 → Polite 接管牧师(等Channel结束)                    │
│       → 哀嚎演出 → ApplyStatus(圣光之怒 + AIModifier)    │
│                                                                │
│  Scene 不关心"牧师怎么治疗"或"骑士怎么连招"                        │
│  只关心宏观节奏转换和玩家反馈                                      │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────────┐
│  AI Layer                                                      │
│                                                                │
│  Knight Brain:                                                 │
│  ┌────────────────────────────────────┐                        │
│  │ 冲锋(70)  ← 7~15m, CastBar 0.8s  │                        │
│  │ 天堂之锤(85) ← ≤8m + 敌眩晕,      │                        │
│  │              CastBar 3s 无霸体     │ ← 被打断→破势3s        │
│  │ 劈砍(40)  ← ≤4m, Instant          │                        │
│  └────────────────────────────────────┘                        │
│                                                                │
│  Priest Brain (正常):                                           │
│  ┌────────────────────────────────────┐                        │
│  │ 引导治疗(~90) ← 骑士HP<70%,        │                        │
│  │              Channel 5s 站桩       │ ← 被打断→自我沉默2s    │
│  │ 群体祝福(50) ← CastBar 2.5s       │ ← 被打断→自我沉默3s    │
│  │ 远离(65)  ← 距离<8m               │                        │
│  │ 轻攻击(30)                         │                        │
│  └────────────────────────────────────┘                        │
│                                                                │
│  Priest Brain (狂暴, AIModifier 生效后):                 │
│  ┌────────────────────────────────────┐                        │
│  │ ~~~治疗行为被禁用~~~                │                        │
│  │ 神圣之怒(75) ← Channel AOE 攻击   │ ← 新注入               │
│  │ 轻攻击(30)                         │                        │
│  │ 远离(65)                           │                        │
│  └────────────────────────────────────┘                        │
│                                                                │
│  AI 通过 CastAbility 发出指令，不管理施法生命周期                  │
│  被打断→CastAbility返回Failed→AI重新决策                         │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────────┐
│  GAS Layer — Ability Lifecycle                                 │
│                                                                │
│  骑士·盾牌冲锋 (CastBar 0.8s):                                  │
│    Activate(扣费) → Processing[读条+霸体] → Execute[冲撞+眩晕]   │
│    → Recovery[1.0s Heavy]                                      │
│    霸体期间 immuneToTags: Control → 读条不可被打断                │
│                                                                │
│  骑士·天堂之锤 (CastBar 3.0s):                                   │
│    Activate → Processing[读条, 无霸体, 站桩]                     │
│    │  被眩晕 → interruptsAbilities                              │
│    │  → onInterrupt: 挂"破势"+Lockout                          │
│    │  → 广播 Ability_Interrupted (Scene 监听 → UI 警告)         │
│    └─正常完成→ Execute[AOE 4倍伤害] → Recovery[1.5s Heavy]       │
│                                                                │
│  牧师·神圣引导 (Channel 5s):                                     │
│    Activate → Processing[心跳×5, 每秒治疗骑士]                   │
│    │  commitPolicy: OnFirstTick → 首次心跳后才扣费               │
│    │  骑士死亡 → onTargetLost: Cancel → 无惩罚中止               │
│    │  被玩家打断 → interruptsAbilities                          │
│    │    → onInterrupt: 自我沉默 2s                              │
│    │    → 广播 Ability_Interrupted (Scene 监听 → 奖励暴击)      │
│    └─正常完成→ Execute[挂 HOT] → Recovery[0.5s]                 │
│                                                                │
│  TagRules 协同：                                                │
│    眩晕 → interrupts + blocks ALL                              │
│    沉默 → interrupts + blocks SPELL                            │
│    霸体 → immune to CONTROL                                    │
│    后摇 → blocks SPELL + MELEE                                 │
│    重型后摇 → 追加 blocks MOVEMENT                              │
│                                                                │
│  Status 协同：                                                  │
│    圣光共鸣(7040): ATK+30% + 受击概率护盾 ← Scene WithStatus    │
│    破势(7020): 受伤+50% ← onInterrupt 挂载                     │
│    圣光之怒(7050): ATK×2 + 施法回蓝 + AIModifier        │
└────────────────────────────────────────────────────────────────┘
```

### 关键联动时序（玩家打断牧师引导）

```
时间线 ──────────────────────────────────────────────────────▶

t=0   牧师 AI Think: 骑士HP<70%, 引导治疗分数最高
      → CastAbility(3001, target=Knight)
      → GAS: CanActivate ✓ → Activating

t=0   GAS: Channel Processing 开始
      → channelingTags: [Casting.Spell, Immobile] 写入 TagContainer
      → cuesDuringChannel: "Cue.Channel.HolyBeam" → 客户端播放光束

t=0   GAS: tickOnStart=true → 首次 tickEffect 执行
      → StandardHeal(骑士, ATK×1.2)
      → commitPolicy: OnFirstTick → 此刻扣费+启动CD

t=1   GAS: 第2次 tickEffect → 治疗骑士
t=2   GAS: 第3次 tickEffect → 治疗骑士

t=2.5 玩家对牧师释放"踢击"(grantTags: State.Debuff.Control.Stun)

t=2.5 GAS TagRules 触发:
      whenPresent: State.Debuff.Control.Stun
        → interruptsAbilities: [Ability.Type] → 命中牧师的 Channel
        → Processing 阶段 interrupt!

t=2.5 GAS: 执行 onInterrupt:
      → GrantTags(State.Debuff.Silence, 2s) → 牧师自我沉默
      → FireCue(Cue.Cast.Interrupted.Holy) → 光束断裂特效
      → 广播 Ability_Interrupted 到牧师 EventBus

t=2.5 GAS: ability.effect（挂 HOT）不执行（被打断）
      → AbilityInstance 清理 → CastAbility 返回 Failed

t=2.5 Scene 分支2: 监听到 Ability_Interrupted(Priest)
      → PlayCue("Cue.UI.InterruptSuccess") → 玩家屏幕闪烁提示
      → ApplyStatusInline(暴击+15%, 5s) → 奖励玩家

t=2.5 AI: CastAbility 返回 Failed → Behavior Failed
      → 下帧重新 Think
      → 牧师有 State.Debuff.Silence → CanActivateAbility(Spell) 全部 ✗
      → 只剩 Behavior_Priest_Kite(65分) 可用 → 后撤远离

t=4.5 GAS: 沉默到期 → Tag 移除
      → AI Think: 治疗 CD 可能还没好 → 选择轻攻击或群体祝福
```