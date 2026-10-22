package main

import (
	"cfgtest/config"
	"cfgtest/test"
	"fmt"
	"os"
)

func require(ok bool, message string) {
	if !ok {
		panic(message)
	}
}

func main() {
	file, err := os.Open("base.bytes")
	if err != nil {
		panic(err)
	}
	defer file.Close()
	config.Init(file)
	test.DoTest()

	ability := config.GetCombatAbilityMgr().Get(1001)
	cast := config.GetCombatCastMgr().Get(1)
	require(ability != nil && cast != nil, "combat records")
	machine := ability.Machines()[0]
	require(ability.RefInitial() == machine.Phases()[0] && ability.RefSpawn() == ability.Origin(), "root-local and fixed object")
	require(machine.Phases()[0].NullableRefNext() == machine.Phases()[1] &&
		machine.Phases()[1].NullableRefNext() == machine.Phases()[0], "local cycle")
	require(machine.Phases()[2].NullableRefNext() == nil, "nullable local index miss")
	require(ability.Machines()[1].Phases()[0].NullableRefNext() == nil, "local index cannot leak across machines")
	require(machine.RefFrame().Tick() == 9007199254740993, "64-bit list index precision")
	require(machine.Anchor() != ability.Machines()[1].Anchor(), "empty objects retain identity")
	anchorUses := make(map[*config.CombatAnchorUse]bool)
	for _, owner := range ability.Machines() {
		require(owner.RefInitial() != nil && owner.RefImpact() != nil && owner.RefFrame() != nil &&
			owner.RefStance() != nil && owner.RefVariant() != nil && owner.RefCombo() != nil, "local list targets exist")
		require(owner.RefInitial() == owner.Phases()[0], "local string index")
		require(owner.RefImpact() == owner.Impacts()[0], "integer list index")
		require(owner.RefFrame() == owner.Frames()[0], "64-bit list index")
		require(owner.RefStance() == owner.Stances()[0], "boolean list index")
		require(owner.RefVariant() == owner.Variants()[0], "struct list index")
		require(owner.RefCombo() == owner.Combos()[0], "composite list index")
		for _, phase := range owner.Phases() {
			require(phase.RefExit() == owner.OnExit().Effect(), "fixed interface target")
		}
		for _, use := range owner.Anchors() {
			require(use != nil && !anchorUses[use], "empty source objects retain identity")
			anchorUses[use] = true
			require(use.RefAnchor() == owner.Anchor(), "zero-key fixed target")
		}
	}
	require(len(anchorUses) == 3, "all empty source objects loaded")

	use := cast.Uses()[0]
	require(use.RefAbility() == ability, "source member path")
	require(len(use.Machines()) == 2, "all machine source scopes loaded")
	for i, machineUse := range use.Machines() {
		require(len(machineUse.Targets()) == len(ability.Machines()[i].Phases()), "all phase source objects loaded")
		for j, target := range machineUse.Targets() {
			require(target.RefTarget() == ability.Machines()[i].Phases()[j], "mixed ancestor source paths")
			require(target.RefByCode() == target.RefTarget(), "table unique key")
			require(target.RefOrigin() == cast.Origin(), "original source root")
		}
	}
	for _, aim := range cast.Optional()[:3] {
		require(aim.NullableRefTarget() == nil, "nullable full target chain")
	}
	require(cast.Optional()[3].NullableRefTarget() == machine.Phases()[2], "empty string key remains valid")
	book := config.GetCombatPhasebookMgr().Get("Fire", 0, false)
	require(book != nil && cast.RefBookRecord() == book, "composite table primary key")
	require(cast.RefBook() == book.Entry() && cast.RefBookByCode() == book.Entry(), "table primary and unique key targets")
	require(cast.RefReward() == config.GetCombatBranchrewardMgr().Get(cast.RewardKey()).Reward(), "struct table primary key")

	timeline := config.GetCombatTimelineMgr().Get(1)
	var event config.CombatEffect
	for key, effect := range timeline.Events() {
		if *key == *timeline.EventKey() {
			event = effect
		}
	}
	require(event != nil && timeline.RefEventKey() == event, "struct map key")
	require(timeline.RefChannel() == timeline.Channels()["impact"], "string map key")
	require(timeline.RefSlot() == timeline.Slots()[0], "integer map key")
	require(timeline.RefFrame() == timeline.Clock()[9007199254740993], "64-bit map key")
	require(timeline.RefAirborne() == timeline.Modes()[false], "boolean map key")
	require(timeline.RefDefault() == timeline.Fallback(), "interface fixed endpoint")
	emptyEffects := make(map[*config.CombatEffectNone]bool)
	for _, effect := range []config.CombatEffect{machine.OnExit().Effect(), ability.Machines()[1].OnExit().Effect(), timeline.Fallback(), timeline.Modes()[true]} {
		none, ok := effect.(*config.CombatEffectNone)
		require(ok && none != nil && !emptyEffects[none], "empty interface objects retain identity")
		emptyEffects[none] = true
	}
	outer, ok := config.GetCombatEncounterMgr().Get(1).Command().(*config.CombatCommandStateMachine)
	require(ok, "outer state machine")
	inner, ok := outer.Phases()[1].Action().(*config.CombatCommandStateMachine)
	require(ok, "nested state machine")
	missing, ok := inner.Phases()[0].Action().(*config.CombatCommandJump)
	require(ok && missing.NullableRefKey() == nil, "nearest scope cannot fall back")
	local, ok := inner.Phases()[1].Action().(*config.CombatCommandJump)
	require(ok && local.NullableRefKey() == inner.Phases()[0], "nearest scope hit")
	fmt.Println("go: old and internal-reference configuration assertions passed")
}
