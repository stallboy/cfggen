# cfggen

让策划可以在excel中做灵活的配置，之后再为程序生成访问配置表的代码


## 主要功能

* 通过配置外键，取值范围，使策划可以随时检测数据一致性

* 通过生成代码，使程序方便访问类型化数据，生成外键引用，生成entry（程序中没有魔数），支持java,c#,lua

* 支持多态结构，嵌套结构，可在一个单元格里写任意复杂的结构数据，相当于让csv有了xml的灵活性

* 生成java注重安全

* 生成lua注重内存大小

## build & test

* 生成configgen.jar
```bash
gradle fatjar  # 打出app/build/libs/configgen.jar
```

* 测试java：生成java代码和数据
```bash
cd example
genjavasealed.bat # genjava 也可以，sealed需要java 17或以上才支持
```

* 测试java：检验java生成
```bash
gradle build 
java -jar build/libs/example.jar # 进入命令行，输入q退出，输入其他比如ai会打印表名称以ai开头的结构定义和数据
```

* 测试lua
```bash
genlua.bat 
cd lua
lua.exe test.lua
```

* 测试csharp

```bash
gencshape.bat 
cd cs
dotnet run
```