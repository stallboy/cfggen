package config

type CombatAnchorUse struct {
    _identity byte
    refAnchor *CombatAnchor
}

func createCombatAnchorUse(_ *Stream) *CombatAnchorUse {
    v := &CombatAnchorUse{}
    return v
}

func (t *CombatAnchorUse) String() string {
    return "CombatAnchorUse{}"
}

func (t *CombatAnchorUse) RefAnchor() *CombatAnchor {
    return t.refAnchor
}
