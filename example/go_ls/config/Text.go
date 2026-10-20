package config

type Text struct {
    Zh_cn string
    En string
    Tw string
}

func createText(stream *Stream) *Text {
    self := &Text{}
    index := stream.ReadTextIndex()
    pools := stream.GetLangTextPools()
    self.Zh_cn = pools[0][index]
    self.En = pools[1][index]
    self.Tw = pools[2][index]
    return self
}

func (t *Text) String() string {
        return t.Zh_cn + "," + t.En + "," + t.Tw
}
