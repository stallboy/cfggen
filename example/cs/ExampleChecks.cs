using System;
using System.Collections.Generic;
using Config;
using Config.Combat;
using Config.Combat.Command;
using Config.Combat.Effect;
using Config.Equip;
using Config.Other;
using Config.Task;

internal static class ExampleChecks
{
    internal static void Check(bool condition, string message)
    {
        if (!condition) throw new InvalidOperationException(message);
    }

    internal static void Verify(ConfigMgr config)
    {
        Check(DMonster.Get(config, 2)!.PosList[0].X == 33, "legacy monster");
        Check(DTask.Get(config, 1)!.NullableRefNexttask == DTask.Get(config, 2), "legacy record reference");
        Check(DRank.Green.Info(config).RankID == 1, "legacy enum details");

        var ability = Config.Combat.DAbility.Get(config, 1001)!;
        Check(ability.Machines.Count == 2, "machine fixture");
        var combat = ability.Machines[0];
        var movement = ability.Machines[1];
        Check(combat.Phases.Count == 3, "Combat phase fixture");
        Check(movement.Phases.Count == 1, "Movement phase fixture");
        Check(ReferenceEquals(ability.RefSpawn, ability.Origin), "fixed object");
        Check(ReferenceEquals(ability.RefInitial, combat.Phases[0]), "record local path");
        var anchorUses = new HashSet<object>(ReferenceEqualityComparer.Instance);
        var emptyEffects = new HashSet<object>(ReferenceEqualityComparer.Instance);
        foreach (var machine in ability.Machines)
        {
            Check(machine.OnExit.Effect is DNone, "empty effect type");
            Check(emptyEffects.Add(machine.OnExit.Effect), "empty effect occurrence identity");
            Check(ReferenceEquals(machine.RefInitial, machine.Phases[0]), "string list index: " + machine.Code);
            Check(ReferenceEquals(machine.RefImpact, machine.Impacts[0]), "int list index: " + machine.Code);
            Check(ReferenceEquals(machine.RefFrame, machine.Frames[0]), "long list index: " + machine.Code);
            Check(ReferenceEquals(machine.RefStance, machine.Stances[0]), "bool list index: " + machine.Code);
            Check(ReferenceEquals(machine.RefVariant, machine.Variants[0]), "struct list index: " + machine.Code);
            Check(ReferenceEquals(machine.RefCombo, machine.Combos[0]), "composite list index: " + machine.Code);
            foreach (var phase in machine.Phases)
                Check(ReferenceEquals(phase.RefExit, machine.OnExit.Effect), "fixed member path: " + machine.Code);
            foreach (var anchor in machine.Anchors)
            {
                Check(anchorUses.Add(anchor), "empty source occurrence identity");
                Check(ReferenceEquals(anchor.RefAnchor, machine.Anchor), "empty object target: " + machine.Code);
            }
        }
        Check(combat.RefFrame.Tick == 9007199254740993L, "long list index precision");
        Check(ReferenceEquals(combat.Phases[0].NullableRefNext, combat.Phases[1]), "next phase");
        Check(ReferenceEquals(combat.Phases[1].NullableRefNext, combat.Phases[0]), "cyclic reference");
        Check(combat.Phases[2].NullableRefNext == null, "missing phase is nullable");
        Check(movement.Phases[0].NullableRefNext == null, "nearest scope must not fall back");
        Check(!ReferenceEquals(combat.Anchor, movement.Anchor), "empty target occurrence identity");
        Check(anchorUses.Count == 3, "empty source fixture");

        var cast = DCast.Get(config, 1)!;
        Check(cast.Uses.Count == 1, "ability source fixture");
        var use = cast.Uses[0];
        Check(ReferenceEquals(use.RefAbility, ability), "fixed member source");
        Check(use.Machines.Count == 2, "machine source fixture");
        for (int i = 0; i < use.Machines.Count; i++)
        {
            var machineUse = use.Machines[i];
            var targetMachine = ability.Machines[i];
            Check(machineUse.MachineKey == targetMachine.Code, "machine source key");
            Check(machineUse.Targets.Count == targetMachine.Phases.Count, "phase source fixture");
            for (int j = 0; j < machineUse.Targets.Count; j++)
            {
                var target = machineUse.Targets[j];
                var targetPhase = targetMachine.Phases[j];
                Check(target.PhaseKey == targetPhase.Key, "phase source key");
                Check(ReferenceEquals(target.RefTarget, targetPhase), "mixed ancestor sources");
                Check(ReferenceEquals(target.RefByCode, targetPhase), "unique key and source root key");
                Check(ReferenceEquals(target.RefOrigin, cast.Origin), "source record root");
            }
        }
        for (int i = 0; i < 3; i++) Check(cast.Optional[i].NullableRefTarget == null, "nullable path step " + i);
        Check(ReferenceEquals(cast.Optional[3].NullableRefTarget, combat.Phases[2]), "valid empty key");
        var book = DPhasebook.Get(config, "Fire", 0, false)!;
        Check(ReferenceEquals(cast.RefBookRecord, book), "composite record target");
        Check(ReferenceEquals(cast.RefBook, book.Entry), "composite primary key");
        Check(ReferenceEquals(cast.RefBookByCode, book.Entry), "composite unique key");
        Check(ReferenceEquals(cast.RefReward, DBranchreward.Get(config, cast.RewardKey)!.Reward), "struct record key");

        var timeline = DTimeline.Get(config, 1)!;
        Check(ReferenceEquals(timeline.RefEventKey, timeline.Events[timeline.EventKey]), "struct map key");
        Check(ReferenceEquals(timeline.RefChannel, timeline.Channels[timeline.Channel]), "string map key");
        Check(ReferenceEquals(timeline.RefSlot, timeline.Slots[0]), "int map key");
        Check(ReferenceEquals(timeline.RefFrame, timeline.Clock[9007199254740993L]), "long map key precision");
        Check(ReferenceEquals(timeline.RefAirborne, timeline.Modes[false]), "bool map key");
        Check(ReferenceEquals(timeline.RefDefault, timeline.Fallback), "interface target");
        Check(timeline.Fallback is DNone, "empty fixed effect type");
        Check(emptyEffects.Add(timeline.Fallback), "empty fixed effect identity");
        Check(timeline.Modes[true] is DNone, "empty mapped effect type");
        Check(emptyEffects.Add(timeline.Modes[true]), "empty mapped effect identity");
        var outer = (DStateMachine)DEncounter.Get(config, 1)!.Command;
        var inner = (DStateMachine)outer.Phases[1].Action;
        Check(((DJump)inner.Phases[0].Action).NullableRefKey == null, "nearest interface scope");
        Check(ReferenceEquals(((DJump)inner.Phases[1].Action).NullableRefKey, inner.Phases[0]), "recursive interface scope");
    }
}
