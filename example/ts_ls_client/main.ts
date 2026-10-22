import assert from "node:assert/strict";
import {readFileSync} from "node:fs";
import {Config} from "./Config";
import {ConfigPackage, LoadErrors, TextPoolManager} from "./ConfigUtil";

const errors = new LoadErrors();
const stream = Config.Processor.ProcessPackage(
    ConfigPackage.Read(new Uint8Array(readFileSync("base.bytes")).buffer), errors);
assert.deepEqual(errors.Errors, [], "configuration load errors");

const task = Config.Task_Task.Get(1);
assert(task);
assert.equal(task.NullableRefNexttask, Config.Task_Task.Get(2));
const missingTask = Config.Task_Task.Get(3);
assert(missingTask);
assert.equal(missingTask.NullableRefNexttask, undefined);
assert.equal(Config.Equip_Jewelry.Get(1)?.RefLvlRank?.LvlRank.Level, 5);
const listSource = Config.Other_Key_test.Get(1, 2n);
const listTarget = Config.Other_Sign_in.Get(12);
const signIn = Config.Other_Sign_in.Get(4);
const mapTarget = Config.Other_Loot.Get(10);
const loot = Config.Other_Loot.Get(2);
const lootItem = Config.Other_Loot_item.Get(2, 40007);
assert(listSource && listTarget && signIn && mapTarget && loot && lootItem);
assert.equal(lootItem.Itemid, 40007);
assert.equal(signIn.Item2countMap.get(10001), 5);
assert.equal(listSource.RefIds.length, 2);
assert.equal(listSource.RefIds[1], listTarget);
assert.equal(signIn.RefVipitem2vipcountMap.get(10001), mapTarget);
assert.equal(loot.ListRefLootid.length, 7);
assert(loot.ListRefLootid.includes(lootItem));
for (const row of loot.ListRefLootid) {
    assert.equal(row.Lootid, 2);
    assert.equal(row, Config.Other_Loot_item.Get(row.Lootid, row.Itemid));
}
assert.deepEqual(stream.GetLangNames(), ["zh_cn", "en", "tw"]);
const translations = stream.GetLangTextPools().map(pool => {
    TextPoolManager.SetGlobalTexts(pool);
    return task.Name[0].T;
});
assert.deepEqual(translations, ["杀个怪", "kill a monster", ""]);
TextPoolManager.SetGlobalTexts(stream.GetLangTextPools()[0]);
assert.equal(task.Name[0].T, "杀个怪"); // The same loaded object follows the active language.

const ability = Config.Combat_Ability.Get(1001);
const cast = Config.Combat_Cast.Get(1);
assert(ability && cast);
const machine = ability.Machines[0];
assert.equal(ability.RefInitial, machine.Phases[0]);
assert.equal(ability.RefSpawn, ability.Origin);
assert.equal(machine.Phases[0].NullableRefNext, machine.Phases[1]);
assert.equal(machine.Phases[1].NullableRefNext, machine.Phases[0]);
assert.equal(machine.Phases[2].NullableRefNext, undefined);
assert.equal(ability.Machines[1].Phases[0].NullableRefNext, undefined);
assert.equal(machine.RefFrame.Tick, 9007199254740993n);
assert.notEqual(machine.Anchor, ability.Machines[1].Anchor);
const anchorUses = ability.Machines.flatMap(owner => owner.Anchors);
assert.equal(anchorUses.length, 3);
assert.equal(new Set(anchorUses).size, anchorUses.length);
for (const owner of ability.Machines) {
    assert(owner.RefInitial && owner.RefImpact && owner.RefFrame && owner.RefStance && owner.RefVariant && owner.RefCombo);
    assert.equal(owner.RefInitial, owner.Phases[0]);
    assert.equal(owner.RefImpact, owner.Impacts[0]);
    assert.equal(owner.RefFrame, owner.Frames[0]);
    assert.equal(owner.RefStance, owner.Stances[0]);
    assert.equal(owner.RefVariant, owner.Variants[0]);
    assert.equal(owner.RefCombo, owner.Combos[0]);
    for (const phase of owner.Phases) assert.equal(phase.RefExit, owner.OnExit.Effect);
    for (const use of owner.Anchors) assert.equal(use.RefAnchor, owner.Anchor);
}

const use = cast.Uses[0];
assert.equal(use.RefAbility, ability);
assert.deepEqual(use.Machines.map(owner => owner.Targets.length), [3, 1], "all mixed-source targets loaded");
for (let i = 0; i < use.Machines.length; i++) {
    for (let j = 0; j < use.Machines[i].Targets.length; j++) {
        const target = use.Machines[i].Targets[j];
        assert.equal(target.RefTarget, ability.Machines[i].Phases[j]);
        assert.equal(target.RefByCode, target.RefTarget);
        assert.equal(target.RefOrigin, cast.Origin);
    }
}
for (const aim of cast.Optional.slice(0, 3)) assert.equal(aim.NullableRefTarget, undefined);
assert.equal(cast.Optional[3].NullableRefTarget, machine.Phases[2]);
const book = Config.Combat_Phasebook.Get("Fire", 0, false);
assert(book);
assert.equal(cast.RefBookRecord, book);
assert.equal(cast.RefBook, book.Entry);
assert.equal(cast.RefBookByCode, book.Entry);
assert.equal(cast.RefReward, Config.Combat_Branchreward.Get(cast.RewardKey)?.Reward);

const timeline = Config.Combat_Timeline.Get(1);
assert(timeline);
const event = [...timeline.Events].find(([key]) =>
    key.Name === timeline.EventKey.Name && key.Variant === timeline.EventKey.Variant);
assert(event, "struct map key exists");
assert.equal(timeline.RefEventKey, event[1]);
assert.equal(timeline.RefChannel, timeline.Channels.get("impact"));
assert.equal(timeline.RefSlot, timeline.Slots.get(0));
assert.equal(timeline.RefFrame, timeline.Clock.get(9007199254740993n));
assert.equal(timeline.RefAirborne, timeline.Modes.get(false));
assert.equal(timeline.RefDefault, timeline.Fallback);
const emptyEffects = [machine.OnExit.Effect, ability.Machines[1].OnExit.Effect, timeline.Fallback, timeline.Modes.get(true)];
assert(emptyEffects.every(effect => effect instanceof Config.Combat_Effect_None));
assert.equal(new Set(emptyEffects).size, emptyEffects.length);
const outer = Config.Combat_Encounter.Get(1)?.Command;
assert(outer instanceof Config.Combat_Command_StateMachine);
const inner = outer.Phases[1].Action;
assert(inner instanceof Config.Combat_Command_StateMachine);
const missing = inner.Phases[0].Action;
const local = inner.Phases[1].Action;
assert(missing instanceof Config.Combat_Command_Jump && local instanceof Config.Combat_Command_Jump);
assert.equal(missing.NullableRefKey, undefined);
assert.equal(local.NullableRefKey, inner.Phases[0]);

console.log("ts_ls_client: old and internal-reference configuration assertions passed");
