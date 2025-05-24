package main

import (
	"cfgtest/config" // 导入 config 包
	"os"
)

func main() {
	// 初始化cfgMgr
	file, err := os.Open("config.bytes")
	if err != nil {
		return
	}
	defer file.Close()
	cfgMgr := &config.ConfigMgr{}
	cfgMgr.Init(file)

	// 使用cfgMgr
	for _, v := range cfgMgr.AiAiMgr.GetAll() {
		print(v.GetCondID(), v.GetDesc())
	}

}
