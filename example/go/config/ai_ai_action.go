package config

type AiAi_action struct {
    iD int
    desc string //描述
    formulaID int //公式
    argIList []int //参数(int)1
    argSList []int //参数(string)1
}

//getters
func (t *AiAi_action) GetID() int {
    return t.iD
}

func (t *AiAi_action) GetDesc() string {
    return t.desc
}

func (t *AiAi_action) GetFormulaID() int {
    return t.formulaID
}

func (t *AiAi_action) GetArgIList() []int {
    return t.argIList
}

func (t *AiAi_action) GetArgSList() []int {
    return t.argSList
}

type AiAi_actionMgr struct {
    all []*AiAi_action
    allMap map[int]*AiAi_action
}

func(t *AiAi_actionMgr) GetAll() []*AiAi_action {
    return t.all
}

func(t *AiAi_actionMgr) Get(key int) (*AiAi_action,bool) {
    v, ok := t.allMap[key]
    return v, ok
}

