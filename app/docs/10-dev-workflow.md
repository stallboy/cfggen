# 开发流程

怎么构建、运行、调试、改模板、给 cfggen 自身做性能 profile。

## 构建

- **Java 25** toolchain（见 `../build.gradle`）。
- 可执行 jar：在 `app/` 下 `./gradlew.bat fatjar` → `build/libs/cfggen.jar`（fat jar，含全部依赖）。
- 仓库根的 `genjar.bat` 只是个便捷封装：`cd app && call gradlew.bat fatjar && copy cfggen.jar 到根 && pause`。它是 **Windows cmd 语法**（`call` / `copy /B` / `pause`），**在 Git Bash 下不工作**——直接用上面的 `./gradlew.bat fatjar` 即可。

构建期 `build.gradle` 做三件影响运行时的事：

| 任务 | 作用 |
|---|---|
| `normalizeJteLineEndings` | 先于 `generateJte` 跑（`generateJte` / `processResources` 都 `dependsOn` 它）。强制所有 `.jte` 行尾为 **LF**——jte 预编译按字节读模板，工作区若是 CRLF 会原样透传到生成的代码文件；这步原地改源文件，但 `.gitattributes` 设了 `eol=lf`，所以 git 不会报 modified |
| `jte { generate() }` | 紧随其后把 `src/main/resources/jte/*.jte` **预编译成 class** 烤进 jar；运行时 `JteEngine` 走 `createPrecompiled`，省每次启动 ~1.6s 的模板编译（见 [`05`](05-codegen-and-extension.md)） |
| `copyGenJavaSources` | 把 `genjava` 的**运行时读取侧**源码（`Schema*` / `ConfigInput` / `BytesInspector` 等）拷进 jar 的 `resources/support/`，供读取侧使用 |

还有两个细节：`fatJar` 排除 `META-INF/*.SF|DSA|RSA`（依赖的签名文件，不排会 `java -jar` 报 `SecurityException`）；因为 fastjson2 在 JDK25 下用 `sun.misc.Unsafe`（JEP 471 弃用），`applicationDefaultJvmArgs` 加了 `--sun-misc-unsafe-memory-access=allow`。

## 运行 / 调试

- **开发期**：`./gradlew.bat run --args="..."`。此时没有预编译产物，`JteEngine` 回退到**动态编译到临时目录**（按 cwd hashCode 缓存复用）——改模板立即见效，不用重打包。
- **发布 jar**：`java -jar cfggen.jar ...`，走预编译 class，零编译启动。
- **改了模板后，必须重新 `fatjar`** 才能让预编译 class 进 jar（开发期 `gradlew run` 不受影响）。
- GUI 拼命令行：`-gui`。
- 示例数据：仓库 `example/`。

## 改模板

模板在 `src/main/resources/jte/`。开发期 `gradlew run` 动态编译，改完立即见效；发布前重新 `fatjar`。AI / 翻译用的 prompt 模板走 `JteEngine.renderTryFileFirst`——优先用工作区文件，可以**不放 jar 就覆盖**默认 prompt。

## 性能 profile

`util/Logger`（见 `util/Logger.java`）提供：

| 参数 | 作用 |
|---|---|
| `-p` | 每步打印**耗时**（自上步 / 自启动，秒）+ **内存**（MB） |
| `-pp` | 每步前先 `System.gc()`，拿更稳的内存数 |
| `-v` / `-vv` | 详细统计（表数、单元格类型分布等） |

**关键原则：测工作秒，不测墙上时间。** 服务端（`server`/`mcpserver`）场景墙上时间噪声能到 ±50%（HTTP、GC、JIT 抖动），单次墙上不可信——要看 `-p` 的**工作秒 / 分配量**，或用 **JFR** 对比。`CfgValueParser` 在 profile 开启时会**逐表计时**（>10ms 才打印），用来定位慢表。

并发已铺到 schema 读、data 读、value 解析、生成渲染各处（各处都有 ~2–3x 的记录）；上限是被某张超大表封顶，那种情况只能做**表内行级并发**。

## 几个 gotcha

- **JTE 预编译坑**：改模板要重新 `fatjar`，否则 jar 里还是旧模板。
- **GDScript 属性递归不是 bug**：Godot 4.x 的 `var x: Array[int]: get: return x` 是标准语法，会自动处理，别当无限递归修（见 `gengd`）。
- **`_skill_buff` 目录陷阱**：同一份 json 不能同时存在于两个目录（如 `_skill_buff` 和 `skill/_buff`）。
- **Java25 Unsafe 警告**：已用 JVM arg 抑制，看到是正常的。

## 测试 / 覆盖率

- `./gradlew.bat test`（JUnit 5）。
- `./gradlew.bat jacocoTestReport` 出覆盖率（csv / xml / html）。

## 关键文件速查

| 关注点 | 位置 |
|---|---|
| 构建脚本 | `../build.gradle` |
| 便捷打包（Windows） | `../../genjar.bat`（Git Bash 下用 `./gradlew.bat fatjar`） |
| 生成 exe | `../script/mkexe.bat`（含 `genexe_step1/2.bat`） |
| 模板 | `../src/main/resources/jte/` |
| 日志 / profile | `util/Logger.java` |
| 示例数据 | `../../example/` |

---

系列到此结束。回到 [`README`](README.md) 看索引，或回 [`01`](01-architecture-overview.md) 重看主干。
