package config

type CombatCommandNoop struct {
    _identity byte
}

func createCombatCommandNoop(_ *Stream) *CombatCommandNoop {
    v := &CombatCommandNoop{}
    return v
}

func (t *CombatCommandNoop) String() string {
    return "CombatCommandNoop{}"
}

