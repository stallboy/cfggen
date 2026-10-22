package config

type CombatEffectNone struct {
    _identity byte
}

func createCombatEffectNone(_ *Stream) *CombatEffectNone {
    v := &CombatEffectNone{}
    return v
}

func (t *CombatEffectNone) String() string {
    return "CombatEffectNone{}"
}

