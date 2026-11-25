# cfggen-tools

用于检查 CSV 编码、比对 Excel 读取结果和启动 MCP 服务。需要 JDK 25。

## 构建

在仓库根目录运行 `genjar.bat` 更新主包，再运行 `gentoolsjar.bat` 构建工具包和 Excel 写回包。脚本使用根目录的 `cfggen.jar` 运行 `-h` 做启动检查，检查通过后更新根目录的两个配套 JAR。

工具项目始终依赖当前 `app` 源码。CI 用示例数据实跑两个工具。在 `app/` 运行 `./gradlew :cfggen-tools:test :cfggen-tools:stageJars` 可测试并构建配套的三个 JAR，产物位于 `cfgdev/cfggen-tools/build/jars/`。Windows 下使用 `gradlew.bat`。

## 使用

请把同版本的 `cfggen.jar`、`cfggen-tools.jar` 和 `cfggen-excel-write.jar` 放在同一个目录。写回包为 `fastexcelcheck` 提供 POI，也支持 MCP 编辑 Excel。只运行 `csvencoding` 时，主包和工具包即可。以下命令从仓库根目录运行。

```text
java -jar cfggen-tools.jar -h
java -jar cfggen-tools.jar -tool "csvencoding,datadir=example/config"
java -jar cfggen-tools.jar -tool "csvencoding,datadir=example/config,to=UTF-8,fix"
java -jar cfggen-tools.jar -tool "fastexcelcheck,datadir=example/config"
java -jar cfggen-tools.jar -datadir example/config -gen mcpserver
```

`csvencoding` 默认只检查。加上 `fix` 才会修改文件，需要明确指定原编码时使用 `source`。`fastexcelcheck` 比对 FastExcel 与 POI 的读表结果，可用 `csvencoding` 和 `headrow` 工具参数覆盖默认值 `GBK` 和 `2`。

`mcpserver` 默认监听 `http://localhost:3457/mcp`，可以通过 `port` 参数更改端口。已有脚本将 MCP 的入口 JAR 改成 `cfggen-tools.jar` 即可，原命令参数可继续使用。
