package config

type OtherKeytest struct {
    mainKey int
    subKey int
    thirdKey int
}

//getters
func (t *OtherKeytest) GetMainKey() int {
    return t.mainKey
}

func (t *OtherKeytest) GetSubKey() int {
    return t.subKey
}

func (t *OtherKeytest) GetThirdKey() int {
    return t.thirdKey
}

type KeyMainKeyThirdKey struct {
    mainKey int
    thirdKey int
}

type KeyMainKeySubKey struct {
    mainKey int
    subKey int
}

type KeySubKeyThirdKey struct {
    subKey int
    thirdKey int
}

type OtherKeytestMgr struct {
    all []*OtherKeytest
    allMap map[KeyMainKeyThirdKey]*OtherKeytest
}

func(t *OtherKeytestMgr) GetAll() []OtherKeytest {
    return t.all
}

func(t *OtherKeytestMgr) Get(key KeyMainKeyThirdKey) (*OtherKeytest,bool) {
    v, ok := t.allMap[key]
    return v, ok
}

