package main

import (
	"cfgtest/config" // 导入 config 包
	"cfgtest/test"
	"fmt"
	"os"
)

func main() {
	// 初始化cfgMgr
	file, err := os.Open("config.bytes")
	if err != nil {
		return
	}
	defer file.Close()
	config.Init(file)

	fmt.Println(config.GetTaskTaskMgr().Get(1))

	test.DoTest()

}
