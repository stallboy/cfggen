-- Shared assertions for the plain and language-switching Lua examples.
return function(cfg)
    local ability = cfg.combat.ability.get(1001)
    local cast = cfg.combat.cast.get(1)
    assert(ability and cast and #ability.machines == 2, "combat records and both machine scopes must load")
    local machine = ability.machines[1]
    assert(rawequal(ability.RefInitial, machine.phases[1]))
    assert(rawequal(ability.RefSpawn, ability.origin))
    local anchorUses = {}
    for i, current in ipairs(ability.machines) do
        assert(#current.phases == (i == 1 and 3 or 1), "phase instances in both scopes must be exercised")
        assert(#current.anchors == (i == 1 and 2 or 1), "empty sources must be exercised in both machines")
        assert(current.RefInitial and rawequal(current.RefInitial, current.phases[1]), "nearest string index")
        assert(current.RefImpact and rawequal(current.RefImpact, current.impacts[1]), "nearest int index")
        assert(current.RefFrame and rawequal(current.RefFrame, current.frames[1]), "nearest long index")
        assert(current.RefStance and rawequal(current.RefStance, current.stances[1]), "nearest bool index")
        assert(current.RefVariant and rawequal(current.RefVariant, current.variants[1]), "nearest struct index")
        assert(current.RefCombo and rawequal(current.RefCombo, current.combos[1]), "nearest composite index")
        for _, phase in ipairs(current.phases) do assert(rawequal(phase.RefExit, current.onExit.effect)) end
        for _, anchor in ipairs(current.anchors) do
            assert(not anchorUses[anchor], "empty source objects retain identity")
            anchorUses[anchor] = true
            assert(rawequal(anchor.RefAnchor, current.anchor))
        end
    end
    assert(rawequal(machine.phases[1].NullableRefNext, machine.phases[2])
        and rawequal(machine.phases[2].NullableRefNext, machine.phases[1]), "cycle must follow Idle -> Hit -> Idle")
    assert(machine.phases[3].NullableRefNext == nil)
    assert(ability.machines[2].phases[1].NullableRefNext == nil)
    assert(not rawequal(machine.anchor, ability.machines[2].anchor))
    assert(machine.RefFrame.tick == 9007199254740993 and ability.machines[2].RefFrame.tick == 0)
    assert(#cast.uses == 1 and #cast.uses[1].machines == 2, "mixed sources must exercise both machine scopes")
    local abilityUse = cast.uses[1]
    assert(rawequal(abilityUse.RefAbility, ability))
    for i, machineUse in ipairs(abilityUse.machines) do
        assert(#machineUse.targets == (i == 1 and 3 or 1), "phase source checks must not be empty loops")
        for j, use in ipairs(machineUse.targets) do
            local phase = ability.machines[i].phases[j]
            assert(rawequal(use.RefTarget, phase) and rawequal(use.RefByCode, phase))
            assert(rawequal(use.RefOrigin, cast.origin))
        end
    end
    local book = cfg.combat.phasebook.get("Fire", 0, false)
    assert(rawequal(cast.RefBook, book.entry) and rawequal(cast.RefBookByCode, book.entry))
    assert(rawequal(cast.RefBookRecord, book))
    assert(rawequal(cast.RefReward, cfg.combat.branchreward.get(cast.rewardKey).reward))
    assert(#cast.optional == 4, "nullable checks need all three misses and the empty-string hit")
    for i = 1, 3 do assert(cast.optional[i].NullableRefTarget == nil) end
    assert(rawequal(cast.optional[4].NullableRefTarget, machine.phases[3]))
    local timeline = cfg.combat.timeline.get(1)
    local event
    for key, effect in pairs(timeline.events) do
        if key.name == timeline.eventKey.name and key.variant == timeline.eventKey.variant then event = effect end
    end
    assert(event ~= nil and rawequal(timeline.RefEventKey, event) and event.type() == "Damage")
    assert(timeline.RefChannel ~= nil and rawequal(timeline.RefChannel, timeline.channels["impact"]))
    assert(timeline.RefSlot ~= nil and rawequal(timeline.RefSlot, timeline.slots[0]))
    assert(timeline.RefFrame ~= nil and rawequal(timeline.RefFrame, timeline.clock[9007199254740993]))
    assert(timeline.RefAirborne ~= nil and rawequal(timeline.RefAirborne, timeline.modes[false]))
    assert(rawequal(timeline.RefDefault, timeline.fallback))
    local emptyEffects = {machine.onExit.effect, ability.machines[2].onExit.effect, timeline.fallback, timeline.modes[true]}
    local seen = {}
    for i = 1, 4 do
        local effect = emptyEffects[i]
        assert(effect and effect.type() == "None" and not seen[effect], "empty interface objects retain identity")
        seen[effect] = true
    end
    local nested = cfg.combat.encounter.get(1).command.phases[2].action
    assert(nested.phases[1].action.NullableRefKey == nil)
    assert(rawequal(nested.phases[2].action.NullableRefKey, nested.phases[1]))
end
