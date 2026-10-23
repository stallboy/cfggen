using System;
using System.Collections.Generic;
namespace Config
{

public static class Processor
{
    private static readonly string[] Languages = { "zh_cn", "en", "tw" };
    public static ConfigMgr LoadPackagesAndSetMgr(params ConfigPackage[] packages)
    {
        var config = LoadPackages(packages);
        ConfigMgr.SetMgr(config);
        return config;
    }

    public static ConfigMgr LoadPackages(params ConfigPackage[] packages)
    {
        ConfigPackage.ValidateOrder(packages);
        var errors = new LoadErrors();
        var config = new ConfigMgr();
        var loaders = NewLoaders(packages, config, errors);
        foreach (var pack in packages) {
            foreach (string block in pack.BlockNames) {
                if (block.StartsWith(".", StringComparison.Ordinal) && !loaders.ContainsKey(block.Substring(1))) {
                    errors.UnknownTableBlock(pack.Name, block);
                }
            }
        }
        errors.CheckErrors();
        foreach (var load in loaders.Values) {
            load();
        }
        errors.CheckErrors();
        foreach (var row in global::Config.Combat.DAbility.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Combat.DCast.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Combat.DEncounter.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Combat.DTimeline.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Equip.DJewelry.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Equip.DJewelryRandom.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DKeyTest.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DLoot.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DMonster.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DSignIn.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Task.DTask.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Task.DTask2.All(config)) {
            row._resolve(errors, config);
        }
        errors.CheckErrors();
        ResolveRelations(config, errors);
        errors.CheckErrors();
        foreach (var row in global::Config.Ai.DAi.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Ai.DAiAction.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Ai.DAiCondition.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DAbility.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DBranchreward.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DCast.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DEncounter.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DPhasebook.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DTimeline.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DAbilityInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DEquipConfig.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelry.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelryRandom.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelrySuit.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelryTypeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DRankInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DArgCaptureModeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DDrop.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DKeyTest.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DLoot.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DLootItem.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DMonster.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DSignIn.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DCompleteConditionTypeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTask.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTask2.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTaskExtraExp.All(config)) {
            Ready(row, config);
        }
        config.MarkReady();
        return config;
    }

    private static void Ready<T>(T value, ConfigMgr config)
    {
        if (value is IConfigExtension extension) {
            extension.OnConfigReady(config);
        }
    }

    private static Dictionary<string, Action> NewLoaders(ConfigPackage[] packages, ConfigMgr config, LoadErrors errors)
    {
        return new Dictionary<string, Action>(StringComparer.Ordinal) {
            ["ai.ai"] = () => global::Config.Ai.DAi.InitializeAll(
                ReadTable(packages, "ai.ai", global::Config.Ai.DAi._create,
                    global::Config.Ai.DAi.PrimaryKey, config._ai_dai_Origins, errors), errors, config),
            ["ai.ai_action"] = () => global::Config.Ai.DAiAction.InitializeAll(
                ReadTable(packages, "ai.ai_action", global::Config.Ai.DAiAction._create,
                    global::Config.Ai.DAiAction.PrimaryKey, config._ai_dai_uaction_Origins, errors), errors, config),
            ["ai.ai_condition"] = () => global::Config.Ai.DAiCondition.InitializeAll(
                ReadTable(packages, "ai.ai_condition", global::Config.Ai.DAiCondition._create,
                    global::Config.Ai.DAiCondition.PrimaryKey, config._ai_dai_ucondition_Origins, errors), errors, config),
            ["combat.ability"] = () => global::Config.Combat.DAbility.InitializeAll(
                ReadTable(packages, "combat.ability", global::Config.Combat.DAbility._create,
                    global::Config.Combat.DAbility.PrimaryKey, config._combat_dability_Origins, errors), errors, config),
            ["combat.branchreward"] = () => global::Config.Combat.DBranchreward.InitializeAll(
                ReadTable(packages, "combat.branchreward", global::Config.Combat.DBranchreward._create,
                    global::Config.Combat.DBranchreward.PrimaryKey, config._combat_dbranchreward_Origins, errors), errors, config),
            ["combat.cast"] = () => global::Config.Combat.DCast.InitializeAll(
                ReadTable(packages, "combat.cast", global::Config.Combat.DCast._create,
                    global::Config.Combat.DCast.PrimaryKey, config._combat_dcast_Origins, errors), errors, config),
            ["combat.encounter"] = () => global::Config.Combat.DEncounter.InitializeAll(
                ReadTable(packages, "combat.encounter", global::Config.Combat.DEncounter._create,
                    global::Config.Combat.DEncounter.PrimaryKey, config._combat_dencounter_Origins, errors), errors, config),
            ["combat.phasebook"] = () => global::Config.Combat.DPhasebook.InitializeAll(
                ReadTable(packages, "combat.phasebook", global::Config.Combat.DPhasebook._create,
                    global::Config.Combat.DPhasebook.PrimaryKey, config._combat_dphasebook_Origins, errors), errors, config),
            ["combat.timeline"] = () => global::Config.Combat.DTimeline.InitializeAll(
                ReadTable(packages, "combat.timeline", global::Config.Combat.DTimeline._create,
                    global::Config.Combat.DTimeline.PrimaryKey, config._combat_dtimeline_Origins, errors), errors, config),
            ["equip.ability"] = () => global::Config.Equip.DAbilityInfo.InitializeAll(
                ReadTable(packages, "equip.ability", global::Config.Equip.DAbilityInfo._create,
                    global::Config.Equip.DAbilityInfo.PrimaryKey, config._equip_dability_Origins, errors), errors, config),
            ["equip.equip_config"] = () => global::Config.Equip.DEquipConfig.InitializeAll(
                ReadTable(packages, "equip.equip_config", global::Config.Equip.DEquipConfig._create,
                    global::Config.Equip.DEquipConfig.PrimaryKey, config._equip_dequip_uconfig_Origins, errors), errors, config),
            ["equip.jewelry"] = () => global::Config.Equip.DJewelry.InitializeAll(
                ReadTable(packages, "equip.jewelry", global::Config.Equip.DJewelry._create,
                    global::Config.Equip.DJewelry.PrimaryKey, config._equip_djewelry_Origins, errors), errors, config),
            ["equip.jewelry_random"] = () => global::Config.Equip.DJewelryRandom.InitializeAll(
                ReadTable(packages, "equip.jewelry_random", global::Config.Equip.DJewelryRandom._create,
                    global::Config.Equip.DJewelryRandom.PrimaryKey, config._equip_djewelry_urandom_Origins, errors), errors, config),
            ["equip.jewelry_suit"] = () => global::Config.Equip.DJewelrySuit.InitializeAll(
                ReadTable(packages, "equip.jewelry_suit", global::Config.Equip.DJewelrySuit._create,
                    global::Config.Equip.DJewelrySuit.PrimaryKey, config._equip_djewelry_usuit_Origins, errors), errors, config),
            ["equip.jewelry_type"] = () => global::Config.Equip.DJewelryTypeInfo.InitializeAll(
                ReadTable(packages, "equip.jewelry_type", global::Config.Equip.DJewelryTypeInfo._create,
                    global::Config.Equip.DJewelryTypeInfo.PrimaryKey, config._equip_djewelry_utype_Origins, errors), errors, config),
            ["equip.rank"] = () => global::Config.Equip.DRankInfo.InitializeAll(
                ReadTable(packages, "equip.rank", global::Config.Equip.DRankInfo._create,
                    global::Config.Equip.DRankInfo.PrimaryKey, config._equip_drank_Origins, errors), errors, config),
            ["other.ArgCaptureMode"] = () => global::Config.Other.DArgCaptureModeInfo.InitializeAll(
                ReadTable(packages, "other.ArgCaptureMode", global::Config.Other.DArgCaptureModeInfo._create,
                    global::Config.Other.DArgCaptureModeInfo.PrimaryKey, config._other_dArgCaptureMode_Origins, errors), errors, config),
            ["other.drop"] = () => global::Config.Other.DDrop.InitializeAll(
                ReadTable(packages, "other.drop", global::Config.Other.DDrop._create,
                    global::Config.Other.DDrop.PrimaryKey, config._other_ddrop_Origins, errors), errors, config),
            ["other.key_test"] = () => global::Config.Other.DKeyTest.InitializeAll(
                ReadTable(packages, "other.key_test", global::Config.Other.DKeyTest._create,
                    global::Config.Other.DKeyTest.PrimaryKey, config._other_dkey_utest_Origins, errors), errors, config),
            ["other.loot"] = () => global::Config.Other.DLoot.InitializeAll(
                ReadTable(packages, "other.loot", global::Config.Other.DLoot._create,
                    global::Config.Other.DLoot.PrimaryKey, config._other_dloot_Origins, errors), errors, config),
            ["other.loot_item"] = () => global::Config.Other.DLootItem.InitializeAll(
                ReadTable(packages, "other.loot_item", global::Config.Other.DLootItem._create,
                    global::Config.Other.DLootItem.PrimaryKey, config._other_dloot_uitem_Origins, errors), errors, config),
            ["other.monster"] = () => global::Config.Other.DMonster.InitializeAll(
                ReadTable(packages, "other.monster", global::Config.Other.DMonster._create,
                    global::Config.Other.DMonster.PrimaryKey, config._other_dmonster_Origins, errors), errors, config),
            ["other.sign_in"] = () => global::Config.Other.DSignIn.InitializeAll(
                ReadTable(packages, "other.sign_in", global::Config.Other.DSignIn._create,
                    global::Config.Other.DSignIn.PrimaryKey, config._other_dsign_uin_Origins, errors), errors, config),
            ["task.complete_condition_type"] = () => global::Config.Task.DCompleteConditionTypeInfo.InitializeAll(
                ReadTable(packages, "task.complete_condition_type", global::Config.Task.DCompleteConditionTypeInfo._create,
                    global::Config.Task.DCompleteConditionTypeInfo.PrimaryKey, config._task_dcomplete_ucondition_utype_Origins, errors), errors, config),
            ["task.task"] = () => global::Config.Task.DTask.InitializeAll(
                ReadTable(packages, "task.task", global::Config.Task.DTask._create,
                    global::Config.Task.DTask.PrimaryKey, config._task_dtask_Origins, errors), errors, config),
            ["task.task2"] = () => global::Config.Task.DTask2.InitializeAll(
                ReadTable(packages, "task.task2", global::Config.Task.DTask2._create,
                    global::Config.Task.DTask2.PrimaryKey, config._task_dtask2_Origins, errors), errors, config),
            ["task.task_extra_exp"] = () => global::Config.Task.DTaskExtraExp.InitializeAll(
                ReadTable(packages, "task.task_extra_exp", global::Config.Task.DTaskExtraExp._create,
                    global::Config.Task.DTaskExtraExp.PrimaryKey, config._task_dtask_uextra_uexp_Origins, errors), errors, config),
        };
    }

    private static void ResolveRelations(ConfigMgr config, LoadErrors errors)
    {
        var resolver = new RelationResolver(errors);
        resolver.Type<global::Config.Combat.DAbilityUse>("combat.AbilityUse",
                row => new object[] { row.Binding, row.Machines })
            .Bind(new RelationResolver.Plan("combat.cast", "ability", false, "table:combat.ability").Source("", 0, 0).Key(0),
                (row, reference) => row.RefAbility = (global::Config.Combat.DAbility)reference!);
        resolver.Type<global::Config.Combat.DAnchor>("combat.Anchor",
                row => new object[] {  });
        resolver.Type<global::Config.Combat.DAnchorUse>("combat.AnchorUse",
                row => new object[] {  })
            .Bind(new RelationResolver.Plan("combat.ability", "anchor", false, "ancestor:combat.Machine").Field(8),
                (row, reference) => row.RefAnchor = (global::Config.Combat.DAnchor)reference!);
        resolver.Type<global::Config.Combat.DBinding>("combat.Binding",
                row => new object[] { row.AbilityId });
        resolver.Type<global::Config.Combat.DBookEntry>("combat.BookEntry",
                row => new object[] { row.Name });
        resolver.Type<global::Config.Combat.DCombo>("combat.Combo",
                row => new object[] { row.Weapon, row.Tier, row.Airborne, row.Effect });
        resolver.Type<global::Config.Combat.Command.DJump>("combat.Command.Jump",
                row => new object[] { row.Key })
            .Bind(new RelationResolver.Plan("combat.encounter", "key", true, "ancestor:combat.Command.StateMachine").Source("", 0).List(0, 0),
                (row, reference) => row.NullableRefKey = (global::Config.Combat.DNestedPhase)reference!);
        resolver.Type<global::Config.Combat.Command.DNoop>("combat.Command.Noop",
                row => new object[] {  });
        resolver.Type<global::Config.Combat.Command.DStateMachine>("combat.Command.StateMachine",
                row => new object[] { row.Phases })
            .Index(0, 0);
        resolver.Type<global::Config.Combat.Effect.DDamage>("combat.Effect.Damage",
                row => new object[] { row.Amount });
        resolver.Type<global::Config.Combat.Effect.DHeal>("combat.Effect.Heal",
                row => new object[] { row.Amount });
        resolver.Type<global::Config.Combat.Effect.DNone>("combat.Effect.None",
                row => new object[] {  });
        resolver.Type<global::Config.Combat.DExit>("combat.Exit",
                row => new object[] { row.Effect });
        resolver.Type<global::Config.Combat.DFrame>("combat.Frame",
                row => new object[] { row.Tick, row.Effect });
        resolver.Type<global::Config.Combat.DImpact>("combat.Impact",
                row => new object[] { row.Ordinal, row.Damage });
        resolver.Type<global::Config.Combat.DMachine>("combat.Machine",
                row => new object[] { row.Code, row.Phases, row.Impacts, row.Frames, row.Stances, row.Variants, row.Combos, row.Markers, row.Anchor, row.Anchors, row.OnExit, row.Initial, row.Impact, row.Frame, row.Stance, row.Variant, row.Weapon, row.Tier, row.Airborne })
            .Index(1, 0)
            .Index(2, 0)
            .Index(3, 0)
            .Index(4, 0)
            .Index(5, 0)
            .Index(6, 0, 1, 2)
            .Index(7, 0)
            .Bind(new RelationResolver.Plan("combat.ability", "initial", false, "ancestor:combat.Machine").Source("", 11).List(1, 0),
                (row, reference) => row.RefInitial = (global::Config.Combat.DPhase)reference!)
            .Bind(new RelationResolver.Plan("combat.ability", "impact", false, "ancestor:combat.Machine").Source("", 12).List(2, 0),
                (row, reference) => row.RefImpact = (global::Config.Combat.DImpact)reference!)
            .Bind(new RelationResolver.Plan("combat.ability", "frame", false, "ancestor:combat.Machine").Source("", 13).List(3, 0),
                (row, reference) => row.RefFrame = (global::Config.Combat.DFrame)reference!)
            .Bind(new RelationResolver.Plan("combat.ability", "stance", false, "ancestor:combat.Machine").Source("", 14).List(4, 0),
                (row, reference) => row.RefStance = (global::Config.Combat.DStance)reference!)
            .Bind(new RelationResolver.Plan("combat.ability", "variant", false, "ancestor:combat.Machine").Source("", 15).List(5, 0),
                (row, reference) => row.RefVariant = (global::Config.Combat.DPhaseVariant)reference!)
            .Bind(new RelationResolver.Plan("combat.ability", "combo", false, "ancestor:combat.Machine").Source("", 16).Source("", 17).Source("", 18).List(6, 0, 1, 2),
                (row, reference) => row.RefCombo = (global::Config.Combat.DCombo)reference!);
        resolver.Type<global::Config.Combat.DMachineUse>("combat.MachineUse",
                row => new object[] { row.MachineKey, row.Targets });
        resolver.Type<global::Config.Combat.DMarker>("combat.Marker",
                row => new object[] { row.Key });
        resolver.Type<global::Config.Combat.DNestedPhase>("combat.NestedPhase",
                row => new object[] { row.Key, row.Action });
        resolver.Type<global::Config.Combat.DOptionalAim>("combat.OptionalAim",
                row => new object[] { row.AbilityId, row.MachineKey, row.PhaseKey })
            .Bind(new RelationResolver.Plan("combat.cast", "target", true, "table:combat.ability").Source("", 0).Source("", 1).Source("", 2).Key(0).List(3, 0).List(1, 0),
                (row, reference) => row.NullableRefTarget = (global::Config.Combat.DPhase)reference!);
        resolver.Type<global::Config.Combat.DOrigin>("combat.Origin",
                row => new object[] { row.X, row.Y });
        resolver.Type<global::Config.Combat.DPhase>("combat.Phase",
                row => new object[] { row.Key, row.Next })
            .Bind(new RelationResolver.Plan("combat.ability", "next", true, "ancestor:combat.Machine").Source("", 1).List(1, 0),
                (row, reference) => row.NullableRefNext = (global::Config.Combat.DPhase)reference!)
            .Bind(new RelationResolver.Plan("combat.ability", "exit", false, "ancestor:combat.Machine").Field(10).Field(0),
                (row, reference) => row.RefExit = (global::Config.Combat.DEffect)reference!);
        resolver.Type<global::Config.Combat.DPhaseKey>("combat.PhaseKey",
                row => new object[] { row.Name, row.Variant });
        resolver.Type<global::Config.Combat.DPhaseUse>("combat.PhaseUse",
                row => new object[] { row.PhaseKey })
            .Bind(new RelationResolver.Plan("combat.cast", "target", false, "table:combat.ability").Source("ancestor:combat.AbilityUse", 0, 0).Source("ancestor:combat.MachineUse", 0).Source("", 0).Key(0).List(3, 0).List(1, 0),
                (row, reference) => row.RefTarget = (global::Config.Combat.DPhase)reference!)
            .Bind(new RelationResolver.Plan("combat.cast", "byCode", false, "table:combat.ability").Source("root", 1).Source("ancestor:combat.MachineUse", 0).Source("", 0).Key(1).List(3, 0).List(1, 0),
                (row, reference) => row.RefByCode = (global::Config.Combat.DPhase)reference!)
            .Bind(new RelationResolver.Plan("combat.cast", "origin", false, "root").Field(2),
                (row, reference) => row.RefOrigin = (global::Config.Combat.DOrigin)reference!);
        resolver.Type<global::Config.Combat.DPhaseVariant>("combat.PhaseVariant",
                row => new object[] { row.Key, row.Effect });
        resolver.Type<global::Config.Combat.DReward>("combat.Reward",
                row => new object[] { row.Count });
        resolver.Type<global::Config.Combat.DStance>("combat.Stance",
                row => new object[] { row.Airborne, row.Effect });
        resolver.Type<global::Config.Combat.DAbility>("combat.ability",
                row => new object[] { row.Id, row.Code, row.Origin, row.Machines, row.StartMachine, row.StartPhase })
            .Index(3, 0)
            .Bind(new RelationResolver.Plan("combat.ability", "initial", false, "root").Source("", 4).Source("", 5).List(3, 0).List(1, 0),
                (row, reference) => row.RefInitial = (global::Config.Combat.DPhase)reference!)
            .Bind(new RelationResolver.Plan("combat.ability", "spawn", false, "root").Field(2),
                (row, reference) => row.RefSpawn = (global::Config.Combat.DOrigin)reference!);
        resolver.Type<global::Config.Combat.DBranchreward>("combat.branchreward",
                row => new object[] { row.Key, row.Reward });
        resolver.Type<global::Config.Combat.DCast>("combat.cast",
                row => new object[] { row.Id, row.AbilityCode, row.Origin, row.Uses, row.Optional, row.School, row.Tier, row.Airborne, row.BookCode, row.Edition, row.RewardKey })
            .Bind(new RelationResolver.Plan("combat.cast", "book", false, "table:combat.phasebook").Source("", 5).Source("", 6).Source("", 7).Key(0, 1, 2).Field(5),
                (row, reference) => row.RefBook = (global::Config.Combat.DBookEntry)reference!)
            .Bind(new RelationResolver.Plan("combat.cast", "bookByCode", false, "table:combat.phasebook").Source("", 8).Source("", 9).Key(3, 4).Field(5),
                (row, reference) => row.RefBookByCode = (global::Config.Combat.DBookEntry)reference!)
            .Bind(new RelationResolver.Plan("combat.cast", "bookRecord", false, "table:combat.phasebook").Source("root", 5).Source("root", 6).Source("root", 7).Key(0, 1, 2),
                (row, reference) => row.RefBookRecord = (global::Config.Combat.DPhasebook)reference!)
            .Bind(new RelationResolver.Plan("combat.cast", "reward", false, "table:combat.branchreward").Source("", 10).Key(0).Field(1),
                (row, reference) => row.RefReward = (global::Config.Combat.DReward)reference!);
        resolver.Type<global::Config.Combat.DEncounter>("combat.encounter",
                row => new object[] { row.Id, row.Command });
        resolver.Type<global::Config.Combat.DPhasebook>("combat.phasebook",
                row => new object[] { row.School, row.Tier, row.Airborne, row.Code, row.Edition, row.Entry });
        resolver.Type<global::Config.Combat.DTimeline>("combat.timeline",
                row => new object[] { row.Id, RelationResolver.WrapMap(row.Events), RelationResolver.WrapMap(row.Channels), RelationResolver.WrapMap(row.Slots), RelationResolver.WrapMap(row.Clock), RelationResolver.WrapMap(row.Modes), row.EventKey, row.Channel, row.Slot, row.Frame, row.Airborne, row.Fallback })
            .Bind(new RelationResolver.Plan("combat.timeline", "eventKey", false, "root").Source("", 6).Map(1),
                (row, reference) => row.RefEventKey = (global::Config.Combat.DEffect)reference!)
            .Bind(new RelationResolver.Plan("combat.timeline", "channel", false, "root").Source("", 7).Map(2),
                (row, reference) => row.RefChannel = (global::Config.Combat.DEffect)reference!)
            .Bind(new RelationResolver.Plan("combat.timeline", "slot", false, "root").Source("", 8).Map(3),
                (row, reference) => row.RefSlot = (global::Config.Combat.DEffect)reference!)
            .Bind(new RelationResolver.Plan("combat.timeline", "frame", false, "root").Source("", 9).Map(4),
                (row, reference) => row.RefFrame = (global::Config.Combat.DEffect)reference!)
            .Bind(new RelationResolver.Plan("combat.timeline", "airborne", false, "root").Source("", 10).Map(5),
                (row, reference) => row.RefAirborne = (global::Config.Combat.DEffect)reference!)
            .Bind(new RelationResolver.Plan("combat.timeline", "default", false, "root").Field(11),
                (row, reference) => row.RefDefault = (global::Config.Combat.DEffect)reference!);
        resolver.Table("combat.phasebook", global::Config.Combat.DPhasebook.All(config));
        resolver.Table("combat.encounter", global::Config.Combat.DEncounter.All(config));
        resolver.Table("combat.ability", global::Config.Combat.DAbility.All(config));
        resolver.Table("combat.branchreward", global::Config.Combat.DBranchreward.All(config));
        resolver.Table("combat.timeline", global::Config.Combat.DTimeline.All(config));
        resolver.Table("combat.cast", global::Config.Combat.DCast.All(config));
        resolver.Resolve();
    }

    private sealed class RelationResolver
    {
        internal sealed record Source(string Scope, int[] Fields);
        internal sealed record Step(int Field, int Kind, int[] Keys);
        internal sealed record Index(int Field, int[] Keys);
        private sealed record Binding(Plan Plan, Action<object, object?> Bind);
        internal sealed class Plan
        {
            internal readonly string Root, Name, Target;
            internal readonly bool Nullable;
            internal readonly List<Source> Sources = new List<Source>();
            internal readonly List<Step> Steps = new List<Step>();
            internal int[] RecordKey = Array.Empty<int>();

            internal Plan(string root, string name, bool nullable, string target)
            { Root = root; Name = name; Nullable = nullable; Target = target; }

            internal Plan Source(string scope, params int[] fields) { Sources.Add(new Source(scope, fields)); return this; }
            internal Plan Key(params int[] fields) { RecordKey = fields; return this; }
            internal Plan Field(int field) { Steps.Add(new Step(field, 0, Array.Empty<int>())); return this; }
            internal Plan List(int field, params int[] keys) { Steps.Add(new Step(field, 1, keys)); return this; }
            internal Plan Map(int field) { Steps.Add(new Step(field, 2, Array.Empty<int>())); return this; }
        }
        internal class Shape
        {
            internal readonly string Name;
            internal readonly Func<object, object[]> Fields;
            internal readonly List<Index> Indexes = new List<Index>();
            private readonly List<Binding> _bindings = new List<Binding>();

            internal Shape(string name, Func<object, object[]> fields) { Name = name; Fields = fields; }
            internal bool HasBindings => _bindings.Count != 0;
            internal void AddBinding(Plan plan, Action<object, object?> bind) { _bindings.Add(new Binding(plan, bind)); }
            internal void Resolve(RelationResolver resolver, Occurrence at)
            {
                foreach (var binding in _bindings) {
                    if (binding.Plan.Root == at.Table) binding.Bind(at.Value, resolver.Bind(at, this, binding.Plan));
                }
            }
        }
        internal sealed class Shape<T> : Shape
        {
            internal Shape(string name, Func<T, object[]> fields) : base(name, value => fields((T)value)) { }
            internal Shape<T> Index(int field, params int[] keys) { Indexes.Add(new Index(field, keys)); return this; }
            internal Shape<T> Bind(Plan plan, Action<T, object?> bind)
            { AddBinding(plan, (value, reference) => bind((T)value, reference)); return this; }
        }
        internal sealed record Occurrence(object Value, object Root, string Table, Occurrence? Parent);
        internal static object WrapMap<K, V>(IReadOnlyDictionary<K, V> map) where K : notnull
        {
            var view = new Dictionary<object, object>();
            foreach (var pair in map) view.Add(pair.Key, pair.Value!);
            return view;
        }
        private sealed class Identity : IEqualityComparer<object>
        {
            public new bool Equals(object? a, object? b) => ReferenceEquals(a, b);
            public int GetHashCode(object value) => System.Runtime.CompilerServices.RuntimeHelpers.GetHashCode(value);
        }
        private sealed class Key : IEquatable<Key>
        {
            private readonly object[] _parts;
            internal Key(object[] parts) { _parts = parts; }
            public bool Equals(Key? other) => other != null && System.Linq.Enumerable.SequenceEqual(_parts, other._parts);
            public override bool Equals(object? other) => other is Key key && Equals(key);
            public override int GetHashCode() { int hash = 17; foreach (var part in _parts) hash = unchecked(hash * 31 + part.GetHashCode()); return hash; }
        }
        private readonly Dictionary<Type, Shape> _types = new Dictionary<Type, Shape>();
        private readonly Dictionary<string, System.Collections.IEnumerable> _tables = new Dictionary<string, System.Collections.IEnumerable>();
        private readonly Dictionary<object, object[]> _fields = new Dictionary<object, object[]>(new Identity());
        private readonly Dictionary<object, Dictionary<string, Dictionary<Key, object>>> _indexes = new Dictionary<object, Dictionary<string, Dictionary<Key, object>>>(new Identity());
        private readonly List<Occurrence> _occurrences = new List<Occurrence>();
        private readonly LoadErrors _errors;

        internal RelationResolver(LoadErrors errors)
        {
            _errors = errors;
        }
        internal Shape<T> Type<T>(string name, Func<T, object[]> fields)
        {
            var shape = new Shape<T>(name, fields);
            _types.Add(typeof(T), shape);
            return shape;
        }
        internal void Table(string name, System.Collections.IEnumerable rows) { _tables.Add(name, rows); }
        private object[] Read(object value)
        {
            if (!_fields.TryGetValue(value, out var fields)) {
                fields = _types[value.GetType()].Fields(value);
                _fields.Add(value, fields);
            }
            return fields;
        }

        internal void Resolve()
        {
            foreach (var table in _tables) foreach (object row in table.Value) Walk(row, row, table.Key, null);
            _errors.CheckErrors();
            foreach (var at in _occurrences) {
                _types[at.Value.GetType()].Resolve(this, at);
            }
        }

        private object? Bind(Occurrence at, Shape shape, Plan plan)
        {
            var keys = new List<object>(plan.Sources.Count);
            bool valid = true;
            foreach (var source in plan.Sources) {
                object? value = Scope(source.Scope, at);
                if (value == null) { valid = false; continue; }
                foreach (int field in source.Fields) value = Read(value)[field];
                keys.Add(value);
            }
            bool external = plan.Target.StartsWith("table:", StringComparison.Ordinal);
            object? target = external ? _tables[plan.Target.Substring(6)] : Scope(plan.Target, at);
            if (!valid || target == null) {
                _errors.MissingReference(shape.Name, plan.Name, "scope", at.Table);
                return null;
            }
            int used = 0;
            if (external) {
                Lookup((System.Collections.IEnumerable)target, plan.RecordKey, plan.Target)
                    .TryGetValue(new Key(keys.GetRange(0, plan.RecordKey.Length).ToArray()), out target);
                used = plan.RecordKey.Length;
            }
            foreach (var step in plan.Steps) {
                if (target == null) break;
                object member = Read(target)[step.Field];
                if (step.Kind == 0) target = member;
                else if (step.Kind == 1) Lookup((System.Collections.IEnumerable)member, step.Keys, shape.Name)
                    .TryGetValue(new Key(keys.GetRange(used, step.Keys.Length).ToArray()), out target);
                else ((Dictionary<object, object>)member).TryGetValue(keys[used], out target);
                used += step.Kind == 2 ? 1 : step.Keys.Length;
            }
            if (target == null && !plan.Nullable) _errors.MissingReference(shape.Name, plan.Name, plan.Target, keys);
            return target;
        }

        private object? Scope(string scope, Occurrence at)
        {
            if (scope.Length == 0) return at.Value;
            if (scope == "root") return at.Root;
            for (Occurrence? current = at; current != null; current = current.Parent) {
                object value = current.Value;
                if ("ancestor:" + _types[value.GetType()].Name == scope) return value;
            }
            return null;
        }

        private void Walk(object value, object root, string table, Occurrence? parent)
        {
            if (_types.TryGetValue(value.GetType(), out var shape)) {
                var at = new Occurrence(value, root, table, parent);
                if (shape.HasBindings) _occurrences.Add(at);
                var fields = Read(value);
                foreach (var index in shape.Indexes) Lookup((System.Collections.IEnumerable)fields[index.Field], index.Keys, shape.Name);
                foreach (var field in fields) Walk(field, root, table, at);
            }
            else if (value is Dictionary<object, object> map) { foreach (var pair in map) { Walk(pair.Key, root, table, parent); Walk(pair.Value, root, table, parent); } }
            else if (value is System.Collections.IEnumerable list && value is not string) { foreach (object item in list) Walk(item, root, table, parent); }
        }

        private Dictionary<Key, object> Lookup(System.Collections.IEnumerable rows, int[] keys, string name)
        {
            if (!_indexes.TryGetValue(rows, out var definitions)) { definitions = new Dictionary<string, Dictionary<Key, object>>(); _indexes.Add(rows, definitions); }
            string definition = string.Join(",", keys);
            if (definitions.TryGetValue(definition, out var index)) return index;
            index = new Dictionary<Key, object>(); definitions.Add(definition, index);
            foreach (object row in rows) {
                var fields = Read(row); var parts = new object[keys.Length];
                for (int i = 0; i < keys.Length; i++) parts[i] = fields[keys[i]];
                if (!index.TryAdd(new Key(parts), row)) _errors.DuplicateKey(name, parts);
            }
            return index;
        }
    }

    private static void Ready(global::Config.Ai.DAi value, ConfigMgr config)
    {
        Ready(value.TrigTick, config);
        Ready<global::Config.Ai.DAi>(value, config);
    }

    private static void Ready(global::Config.Combat.DExit value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DExit>(value, config);
    }

    private static void Ready(global::Config.Combat.DFrame value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DFrame>(value, config);
    }

    private static void Ready(global::Config.Combat.DStance value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DStance>(value, config);
    }

    private static void Ready(global::Config.Combat.DPhaseVariant value, ConfigMgr config)
    {
        Ready(value.Key, config);
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DPhaseVariant>(value, config);
    }

    private static void Ready(global::Config.Combat.DCombo value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DCombo>(value, config);
    }

    private static void Ready(global::Config.Combat.DMachine value, ConfigMgr config)
    {
        foreach (var item in value.Phases) {
            Ready(item, config);
        }
        foreach (var item in value.Impacts) {
            Ready(item, config);
        }
        foreach (var item in value.Frames) {
            Ready(item, config);
        }
        foreach (var item in value.Stances) {
            Ready(item, config);
        }
        foreach (var item in value.Variants) {
            Ready(item, config);
        }
        foreach (var item in value.Combos) {
            Ready(item, config);
        }
        foreach (var item in value.Markers) {
            Ready(item, config);
        }
        Ready(value.Anchor, config);
        foreach (var item in value.Anchors) {
            Ready(item, config);
        }
        Ready(value.OnExit, config);
        Ready(value.Variant, config);
        Ready<global::Config.Combat.DMachine>(value, config);
    }

    private static void Ready(global::Config.Combat.DAbility value, ConfigMgr config)
    {
        Ready(value.Origin, config);
        foreach (var item in value.Machines) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.DAbility>(value, config);
    }

    private static void Ready(global::Config.Combat.DMachineUse value, ConfigMgr config)
    {
        foreach (var item in value.Targets) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.DMachineUse>(value, config);
    }

    private static void Ready(global::Config.Combat.DAbilityUse value, ConfigMgr config)
    {
        Ready(value.Binding, config);
        foreach (var item in value.Machines) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.DAbilityUse>(value, config);
    }

    private static void Ready(global::Config.Combat.DPhasebook value, ConfigMgr config)
    {
        Ready(value.Entry, config);
        Ready<global::Config.Combat.DPhasebook>(value, config);
    }

    private static void Ready(global::Config.Combat.DBranchreward value, ConfigMgr config)
    {
        Ready(value.Key, config);
        Ready(value.Reward, config);
        Ready<global::Config.Combat.DBranchreward>(value, config);
    }

    private static void Ready(global::Config.Combat.DCast value, ConfigMgr config)
    {
        Ready(value.Origin, config);
        foreach (var item in value.Uses) {
            Ready(item, config);
        }
        foreach (var item in value.Optional) {
            Ready(item, config);
        }
        Ready(value.RewardKey, config);
        Ready<global::Config.Combat.DCast>(value, config);
    }

    private static void Ready(global::Config.Combat.DTimeline value, ConfigMgr config)
    {
        foreach (var pair in value.Events) {
            Ready(pair.Key, config);
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Channels) {
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Slots) {
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Clock) {
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Modes) {
            Ready(pair.Value, config);
        }
        Ready(value.EventKey, config);
        Ready(value.Fallback, config);
        Ready<global::Config.Combat.DTimeline>(value, config);
    }

    private static void Ready(global::Config.Combat.Command.DStateMachine value, ConfigMgr config)
    {
        foreach (var item in value.Phases) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.Command.DStateMachine>(value, config);
    }

    private static void Ready(global::Config.Combat.DNestedPhase value, ConfigMgr config)
    {
        Ready(value.Action, config);
        Ready<global::Config.Combat.DNestedPhase>(value, config);
    }

    private static void Ready(global::Config.Combat.DEncounter value, ConfigMgr config)
    {
        Ready(value.Command, config);
        Ready<global::Config.Combat.DEncounter>(value, config);
    }

    private static void Ready(global::Config.Equip.DTestPackBean value, ConfigMgr config)
    {
        Ready(value.IRange, config);
        Ready<global::Config.Equip.DTestPackBean>(value, config);
    }

    private static void Ready(global::Config.Equip.DJewelry value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready<global::Config.Equip.DJewelry>(value, config);
    }

    private static void Ready(global::Config.Equip.DJewelryRandom value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready(value.AttackRange, config);
        foreach (var item in value.OtherRange) {
            Ready(item, config);
        }
        foreach (var item in value.TestPack) {
            Ready(item, config);
        }
        Ready<global::Config.Equip.DJewelryRandom>(value, config);
    }

    private static void Ready(global::Config.Other.DDrop value, ConfigMgr config)
    {
        foreach (var item in value.Items) {
            Ready(item, config);
        }
        Ready<global::Config.Other.DDrop>(value, config);
    }

    private static void Ready(global::Config.Other.DMonster value, ConfigMgr config)
    {
        foreach (var item in value.PosList) {
            Ready(item, config);
        }
        Ready<global::Config.Other.DMonster>(value, config);
    }

    private static void Ready(global::Config.Task.DTestDefaultBean value, ConfigMgr config)
    {
        Ready(value.TestSubBean, config);
        Ready<global::Config.Task.DTestDefaultBean>(value, config);
    }

    private static void Ready(global::Config.Task.Completecondition.DConditionAnd value, ConfigMgr config)
    {
        Ready(value.Cond1, config);
        Ready(value.Cond2, config);
        Ready<global::Config.Task.Completecondition.DConditionAnd>(value, config);
    }

    private static void Ready(global::Config.Task.DTask value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestDefaultBean, config);
        Ready<global::Config.Task.DTask>(value, config);
    }

    private static void Ready(global::Config.Task.DTask2 value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestStruct, config);
        foreach (var item in value.TestListStruct) {
            Ready(item, config);
        }
        foreach (var item in value.TestListInterface) {
            Ready(item, config);
        }
        Ready<global::Config.Task.DTask2>(value, config);
    }

    private static void Ready(global::Config.Combat.DCommand value, ConfigMgr config)
    {
        if (value is global::Config.Combat.Command.DStateMachine item) {
            Ready(item, config);
        }
        else {
            Ready<global::Config.Combat.DCommand>(value, config);
        }
    }

    private static void Ready(global::Config.Task.DCompletecondition value, ConfigMgr config)
    {
        if (value is global::Config.Task.Completecondition.DConditionAnd item) {
            Ready(item, config);
        }
        else {
            Ready<global::Config.Task.DCompletecondition>(value, config);
        }
    }

    private static List<T> ReadTable<K, T>(ConfigPackage[] packages, string table,
        Func<ConfigReader, T> readRow, Func<T, K> primaryKey, Dictionary<K, List<string>> origins,
        LoadErrors errors) where K : notnull
    {
        var rows = new List<T>();
        var slots = new Dictionary<K, int>();
        string block = "." + table;
        foreach (var pack in packages) {
            if (!pack.HasBlock(block)) {
                continue;
            }
            bool isBase = ReferenceEquals(pack, packages[0]);
            string packName = pack.Name;
            var input = pack.Block(block, Languages); input.SetErrors(errors);
            int count = input.ReadCount();
            for (int j = 0; j < count; j++) {
                var row = readRow(input);
                K key = primaryKey(row);
                bool exists = slots.TryGetValue(key, out var slot);
                bool duplicate = exists;
                if (!isBase) {
                    if (!origins.TryGetValue(key, out var chain)) {
                        chain = new List<string>();
                        origins.Add(key, chain);
                    }
                    duplicate = chain.Count != 0 && chain[chain.Count - 1] == packName;
                    if (!duplicate) {
                        chain.Add(packName);
                    }
                }
                if (duplicate) {
                    errors.DuplicateRecord(table, packName, key);
                    continue;
                }
                if (exists) {
                    rows[slot] = row;
                } else {
                    slots.Add(key, rows.Count);
                    rows.Add(row);
                }
            }
            input.RequireEnd();
        }
        return rows;
    }
}
}
