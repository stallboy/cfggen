package config

import "fmt"

type CombatOrigin struct {
    x int32
    y int32
}

func createCombatOrigin(stream *Stream) *CombatOrigin {
    v := &CombatOrigin{}
    v.x = stream.ReadInt32()
    v.y = stream.ReadInt32()
    return v
}

func (t *CombatOrigin) String() string {
    return fmt.Sprintf("CombatOrigin{x=%v, y=%v}", t.x, t.y)
}

//getters
func (t *CombatOrigin) X() int32 {
    return t.x
}

func (t *CombatOrigin) Y() int32 {
    return t.y
}

