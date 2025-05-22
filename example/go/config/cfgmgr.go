package config

import (
	"fmt"
	"os"
)

type ConfigMgr struct {
	aiAiMgr *AiAiMgr
}

func (t *ConfigMgr) Init() {
	file, err := os.Open("config.bytes") // Go 1.16+
	if err != nil {
		return
	}
	defer file.Close()

	myStream := &Stream{file: file}
	for {
		cfgName := myStream.ReadString()
		fmt.Println("load:", cfgName)
		switch cfgName {
		case "ai.ai":
			t.aiAiMgr = &AiAiMgr{}
			t.aiAiMgr.Init(myStream)
			break
		}
		return
	}
}
