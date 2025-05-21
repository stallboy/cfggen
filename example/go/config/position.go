package config

type Position struct {
    x int
    y int
    z int
}

//getters
func (t *Position) GetX() int {
    return t.x
}

func (t *Position) GetY() int {
    return t.y
}

func (t *Position) GetZ() int {
    return t.z
}

