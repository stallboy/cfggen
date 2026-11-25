# cfggen-tools

用于检查 CSV 编码、比对 Excel 读取结果和启动 MCP 服务。需要 JDK 25。

## 构建

在仓库根目录运行 `gentoolsjar.bat`。脚本从当前源码构建工具包，随后把根目录现有的 `cfggen.jar` 加入类路径，运行 `-h` 做启动检查。检查成功后才替换根目录的 `cfggen-tools.jar`。主包缺失或仍包含 MCP 服务时，先用 `genjar.bat` 更新主包。

工具项目始终依赖当前 `app` 源码。CI 用示例数据实跑两个工具。在 `app/` 运行 `./gradlew :cfggen-tools:test :cfggen-tools:stageJars` 可测试并构建配套的两个 JAR，产物位于 `cfgdev/cfggen-tools/build/jars/`。Windows 下使用 `gradlew.bat`。

## 使用

请把一起验证过的 `cfggen.jar` 和 `cfggen-tools.jar` 放在同一个目录。Java 通过工具包清单里的 `Class-Path` 加载 `cfggen.jar`，路径相对于工具包的位置。以下命令从仓库根目录运行。

```text
java -jar cfggen-tools.jar -h
java -jar cfggen-tools.jar -tool "csvencoding,datadir=example/config"
java -jar cfggen-tools.jar -tool "csvencoding,datadir=example/config,to=UTF-8,fix"
java -jar cfggen-tools.jar -tool "fastexcelcheck,datadir=example/config"
java -jar cfggen-tools.jar -datadir example/config -gen mcpserver
```

`csvencoding` 默认只检查。加上 `fix` 才会修改文件，需要明确指定原编码时使用 `source`。`fastexcelcheck` 比对 FastExcel 与 POI 的读表结果，可用 `csvencoding` 和 `headrow` 工具参数覆盖默认值 `GBK` 和 `2`。

`mcpserver` 默认监听 `http://localhost:3457/mcp`，可以通过 `port` 参数更改端口。已有脚本将 MCP 的入口 JAR 改成 `cfggen-tools.jar` 即可，原命令参数可继续使用。
