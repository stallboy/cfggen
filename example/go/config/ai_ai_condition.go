package config

type AiAi_condition struct {
    iD int32
    desc string //描述
    formulaID int32 //公式
    argIList []int32 //参数(int)1
    argSList []int32 //参数(string)1
}

func createAiAi_condition(stream *Stream) *AiAi_condition {
    v := &AiAi_condition{}
    v.iD = stream.ReadInt32()
    v.desc = stream.ReadString()
    v.formulaID = stream.ReadInt32()
    argIListSize := stream.ReadInt32()
    v.argIList = make([]int32, argIListSize)
    for i := 0; i < int(argIListSize); i++ {
        v.argIList[i] = stream.ReadInt32()
    }
    argSListSize := stream.ReadInt32()
    v.argSList = make([]int32, argSListSize)
    for i := 0; i < int(argSListSize); i++ {
        v.argSList[i] = stream.ReadInt32()
    }
    return v
}

//getters
func (t *AiAi_condition) ID() int32 {
    return t.iD
}

func (t *AiAi_condition) Desc() string {
    return t.desc
}

func (t *AiAi_condition) FormulaID() int32 {
    return t.formulaID
}

func (t *AiAi_condition) ArgIList() []int32 {
    return t.argIList
}

func (t *AiAi_condition) ArgSList() []int32 {
    return t.argSList
}

type AiAi_conditionMgr struct {
    all []*AiAi_condition
    iDMap map[int32]*AiAi_condition
}

func(t *AiAi_conditionMgr) GetAll() []*AiAi_condition {
    return t.all
}

func(t *AiAi_conditionMgr) Get(iD int32) *AiAi_condition {
    return t.iDMap[iD]
}

func (t *AiAi_conditionMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi_condition, 0, cnt)
    t.iDMap = make(map[int32]*AiAi_condition, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createAiAi_condition(stream)
        t.all = append(t.all, v)
        t.iDMap[v.iD] = v
    }
}
