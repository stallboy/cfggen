package config

type CombatAnchor struct {
    _identity byte
}

func createCombatAnchor(_ *Stream) *CombatAnchor {
    v := &CombatAnchor{}
    return v
}

func (t *CombatAnchor) String() string {
    return "CombatAnchor{}"
}

