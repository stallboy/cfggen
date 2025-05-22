package main

import (
	"cfgtest/config" // 导入 config 包
)

func main() {
	cfgMgr := &config.ConfigMgr{}
	cfgMgr.Init()
}
