package config

type AiAi_action struct {
    iD int32
    desc string //描述
    formulaID int32 //公式
    argIList []int32 //参数(int)1
    argSList []int32 //参数(string)1
}

func createAiAi_action(stream *Stream) *AiAi_action {
    v := &AiAi_action{}
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
func (t *AiAi_action) ID() int32 {
    return t.iD
}

func (t *AiAi_action) Desc() string {
    return t.desc
}

func (t *AiAi_action) FormulaID() int32 {
    return t.formulaID
}

func (t *AiAi_action) ArgIList() []int32 {
    return t.argIList
}

func (t *AiAi_action) ArgSList() []int32 {
    return t.argSList
}

type AiAi_actionMgr struct {
    all []*AiAi_action
    iDMap map[int32]*AiAi_action
}

func(t *AiAi_actionMgr) GetAll() []*AiAi_action {
    return t.all
}

func(t *AiAi_actionMgr) Get(iD int32) *AiAi_action {
    return t.iDMap[iD]
}

func (t *AiAi_actionMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi_action, 0, cnt)
    t.iDMap = make(map[int32]*AiAi_action, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createAiAi_action(stream)
        t.all = append(t.all, v)
        t.iDMap[v.iD] = v
    }
}
