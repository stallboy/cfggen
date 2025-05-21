package config

import (
	"cfgtest/stream"
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

	for {
		cfgName := stream.ReadString(file)
		fmt.Println("load:", cfgName)
		switch cfgName {
		case "ai.ai":
			t.aiAiMgr = &AiAiMgr{}
			t.aiAiMgr.Init(file)
			break
		}
		return
	}
}
