package main

import (
	"cfgtest_ls/config"
	"fmt"
	"os"
)

func main() {
	file, err := os.Open("config.bytes")
	if err != nil {
		fmt.Println("Error opening file:", err)
		return
	}
	defer file.Close()

	config.Init(file)

	fmt.Println("=== Test Data ===")
	task := config.GetTaskTaskMgr().Get(1)
	if task != nil {
		fmt.Println(task)
	}
}
