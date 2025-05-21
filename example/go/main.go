package main

import (
	"os"
	//     "fmt"
	"cfgtest/config" // 导入 config 包
	"cfgtest/stream"
	//     "encoding/binary"
)

func main() {
	file, err := os.Open("config.bytes") // Go 1.16+
	if err != nil {
		return
	}
	defer file.Close()
	// 调用 ReadString 并处理返回值

	result, err := stream.ReadString(file)
	if err != nil {
		// 打印错误信息
		println("Error reading string:", err.Error())
		return
	}

	// 打印读取的字符串
	println("Read string:", result)

    cfgMgr := &config.ConfigMgr{}
    cfgMgr.Init()
}
