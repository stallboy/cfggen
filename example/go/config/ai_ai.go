package config

import (
	"cfgtest/stream"
	"fmt"
	"os"
)

type AiAi struct {
	iD          int
	desc        string        //描述----这里测试下多行效果--再来一行
	condID      string        //触发公式
	trigTick    AiTriggerTick //触发间隔(帧)
	trigOdds    int           //触发几率
	actionID    []int         //触发行为
	deathRemove bool          //死亡移除
}

// getters
func (t *AiAi) GetID() int {
	return t.iD
}

func (t *AiAi) GetDesc() string {
	return t.desc
}

func (t *AiAi) GetCondID() string {
	return t.condID
}

func (t *AiAi) GetTrigTick() AiTriggerTick {
	return t.trigTick
}

func (t *AiAi) GetTrigOdds() int {
	return t.trigOdds
}

func (t *AiAi) GetActionID() []int {
	return t.actionID
}

func (t *AiAi) GetDeathRemove() bool {
	return t.deathRemove
}

type AiAiMgr struct {
	all   []*AiAi
	iDMap map[int]*AiAi
}

func (t *AiAiMgr) GetAll() []*AiAi {
	return t.all
}

func (t *AiAiMgr) GetByID(ID int) (*AiAi, bool) {
	v, ok := t.iDMap[ID]
	return v, ok
}

func (t *AiAiMgr) Init(file *os.File) {
	cnt := stream.ReadInt32(file)
	for i := 0; i < int(cnt); i++ {
		aiAi := &AiAi{}
		aiAi.iD = int(stream.ReadInt32(file))
		aiAi.desc = stream.ReadString(file)
		aiAi.condID = stream.ReadString(file)
		fmt.Println("aiAi.iD:", aiAi.iD)
		fmt.Println("aiAi.desc:", aiAi.desc)
		fmt.Println("aiAi.condID:", aiAi.condID)

		break
		// aiAi.trigTick = AiTriggerTick(stream.ReadInt32(file))
		// aiAi.trigOdds = int(stream.ReadInt32(file))
		// aiAi.deathRemove = stream.ReadInt32(file) != 0
		// actionIDCnt := stream.ReadInt32(file)
		// for j := 0; j < int(actionIDCnt); j++ {
		// 	aiAi.actionID = append(aiAi.actionID, int(stream.ReadInt32(file)))
		// }
		// t.all = append(t.all, aiAi)
	}
}
