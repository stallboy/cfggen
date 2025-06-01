package config

type Position struct {
    x int32
    y int32
    z int32
}

func createPosition(stream *Stream) *Position {
    v := &Position{}
    v.x = stream.ReadInt32()
    v.y = stream.ReadInt32()
    v.z = stream.ReadInt32()
    return v
}

//getters
func (t *Position) X() int32 {
    return t.x
}

func (t *Position) Y() int32 {
    return t.y
}

func (t *Position) Z() int32 {
    return t.z
}

