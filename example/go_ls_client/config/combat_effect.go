package config

type CombatEffect interface{}

func createCombatEffect(stream *Stream) CombatEffect {
    var typeName = stream.ReadStringInPool()
    switch typeName {
    case "Damage":
        return createCombatEffectDamage(stream)
    case "Heal":
        return createCombatEffectHeal(stream)
    case "None":
        return createCombatEffectNone(stream)
    }
    panic("unexpected CombatEffect type: " + typeName)
}
