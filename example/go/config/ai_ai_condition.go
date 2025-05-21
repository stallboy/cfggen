package config

type AiAi_condition struct {
    iD int
    desc string //描述
    formulaID int //公式
    argIList []int //参数(int)1
    argSList []int //参数(string)1
}

//getters
func (t *AiAi_condition) GetID() int {
    return t.iD
}

func (t *AiAi_condition) GetDesc() string {
    return t.desc
}

func (t *AiAi_condition) GetFormulaID() int {
    return t.formulaID
}

func (t *AiAi_condition) GetArgIList() []int {
    return t.argIList
}

func (t *AiAi_condition) GetArgSList() []int {
    return t.argSList
}

type AiAi_conditionMgr struct {
    all []*AiAi_condition
    iDMap map[int]*AiAi_condition
}

func(t *AiAi_conditionMgr) GetAll() []*AiAi_condition {
    return t.all
}

func(t *AiAi_conditionMgr) GetByID(ID int) (*AiAi_condition,bool) {
    v, ok := t.iDMap[ID]
    return v, ok
}



