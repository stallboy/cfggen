package main

import (
    "os"
    "fmt"
    "GO/stream"
    "config/Task/Completeconditiontype" // 导入 config 包
//     "encoding/binary"
//     "GO/config" // 导入 LevelRank 所在的包
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

    println("age:", Age()) // 调用 main 包中的 age 变量

    var xx = Completeconditiontype{}
    fmt.Printf("id: %d, name: %d\n", xx.GetId(), xx.GetName())

}