package config

import (
    "fmt"
)

type ConfigMgr struct {

}

func (t *ConfigMgr) Init() {
    cfgs := []string{
        "ai_ai",
        "ai_ai_action",
    }

    for _, cfg := range cfgs {
        fmt.Printf("Loading config: %s\n", cfg)
    }
}