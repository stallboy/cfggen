package config

type Text struct {
    Zh_cn string
    En string
    Tw string
}

func createText(stream *Stream) *Text {
    self := &Text{}
    texts := stream.ReadTextsInPool()
    if 0 < len(texts) {
        self.Zh_cn = texts[0]
    }
    if 1 < len(texts) {
        self.En = texts[1]
    }
    if 2 < len(texts) {
        self.Tw = texts[2]
    }
    return self
}

func (t *Text) String() string {
    result := ""
    result += t.Zh_cn
    result += ","
    result += t.En
    result += ","
    result += t.Tw
    return result
}
