package main

import (
	"cfgtest_ls_client/config"
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

	stream := config.Init(file)

	// 获取语言列表
	langNames := stream.GetLangNames()
	fmt.Println("language Count:", len(langNames))

	for i, lang := range langNames {
		fmt.Println("\n=== Language:", lang, "===")
		// 设置当前语言的文本池
		config.TextPoolManagerInstance().SetGlobalTexts(stream.GetLangTextPools()[i])

		task := config.GetTaskTaskMgr().Get(1)
		if task != nil {
			fmt.Println(task)
		}
	}
}
