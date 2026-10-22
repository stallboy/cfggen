package config

import "fmt"

type CombatFrame struct {
    tick int64
    effect CombatEffect
}

func createCombatFrame(stream *Stream) *CombatFrame {
    v := &CombatFrame{}
    v.tick = stream.ReadInt64()
    v.effect = createCombatEffect(stream)
    return v
}

func (t *CombatFrame) String() string {
    return fmt.Sprintf("CombatFrame{tick=%v, effect=%v}", t.tick, t.effect)
}

//getters
func (t *CombatFrame) Tick() int64 {
    return t.tick
}

func (t *CombatFrame) Effect() CombatEffect {
    return t.effect
}

