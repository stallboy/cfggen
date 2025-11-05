# 配置说明

## 构建配置

### Gradle 配置
项目使用 Gradle 8.1+ 作为构建工具，主要配置在 `build.gradle` 文件中。

#### 插件配置
```gradle
plugins {
    id 'application'        // 应用程序插件
    id 'jacoco'            // 代码覆盖率插件
}
```

#### 依赖管理
```gradle
dependencies {
    // 测试框架
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.1'
    // testImplementation 'org.mockito:mockito-inline:5.2.0'  // 当前注释，可选
    // testImplementation 'org.mockito:mockito-core:5.12.0'   // 当前注释，可选
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"

    // 数据处理
    implementation 'org.antlr:antlr4-runtime:4.13.1'
    implementation 'org.dhatim:fastexcel-reader:0.19.0'
    implementation 'org.dhatim:fastexcel:0.19.0'
    implementation 'de.siegmar:fastcsv:2.2.2'
    implementation 'com.alibaba.fastjson2:fastjson2:2.0.43'

    // AI集成
    implementation 'io.github.sashirestela:simple-openai:3.8.2'

    // 模板引擎
    implementation("gg.jte:jte:3.2.1")

    // 可选依赖
    if (!project.hasProperty("noPoi")) {
        implementation 'org.apache.poi:poi:5.4.1'
        implementation 'org.apache.poi:poi-ooxml:5.4.1'
    }
}
```

#### Java 配置
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

compileJava {
    if (project.hasProperty("noPoi")) {
        sourceSets.main.java.exclude("configgen/data/ReadByPoi.java")
    }
    options.encoding = "UTF-8"
}

compileTestJava {
    options.encoding = "UTF-8"
}
```

#### 打包配置
```gradle
jar {
    manifest {
        attributes 'Main-Class': 'configgen.gen.Main'
    }
}

tasks.register('fatJar', Jar) {
    archiveFileName = 'configgen.jar'
    manifest.from jar.manifest
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from sourceSets.main.output
    dependsOn configurations.runtimeClasspath
    from {
        configurations.runtimeClasspath.findAll { it.name.endsWith('jar') }.collect { zipTree(it) }
    }
}

application {
    mainClass = 'configgen.gen.Main'
}
```

## 运行时配置

### 命令行参数

#### 数据目录和Schema
```bash
-datadir <目录>          # 配置数据目录，必须包含config.cfg文件
-headrow <行号>         # CSV/TXT/Excel文件表头行类型，默认2
-encoding <编码>        # CSV/TXT编码，默认GBK，支持BOM检测
-asroot <配置>          # 显式目录配置，格式：'ClientTables:noserver,PublicTables,ServerTables:noclient'
-exceldirs <目录>       # Excel文件目录配置
-jsondirs <目录>        # JSON文件目录配置
```

#### 国际化支持
```bash
-i18nfile <文件/目录>    # 国际化文件配置
-langswitchdir <目录>   # 语言切换支持目录
-defaultlang <语言>     # 默认语言，默认zh_cn
```

#### 工具功能
```bash
-verify                # 验证所有数据
-searchto <文件>       # 保存搜索结果到文件，默认标准输出
-searchtag <标签>      # 按标签搜索值，默认全值搜索
-search [参数...]      # 进入交互式搜索模式
-binarytotext <文件> [匹配]  # 二进制到文本转换
-binarytotextloop <文件>    # 交互式二进制到文本转换
-xmltocfg              # XML到CFG格式转换
-compareterm <文件>    # 检查国际化文件兼容性
```

#### 选项参数
```bash
-v                     # 详细级别1，打印统计信息和警告
-vv                    # 详细级别2，打印额外信息
-p                     # 性能分析，打印内存使用和时间
-pp                    # 性能分析，GC后打印内存使用
-nowarn                # 不打印警告
-weakwarn              # 打印弱警告
```

#### 生成器参数
```bash
-gen <生成器名> [参数...]  # 执行特定生成器
```

### 支持的生成器

- `i18n` - 基于值的国际化生成
- `i18nbyid` - 基于ID的国际化生成
- `i18nbyidtest` - 国际化测试生成
- `java` - Java代码生成
- `javadata` - Java数据生成
- `cs` - C#代码生成
- `bytes` - 字节码生成
- `lua` - Lua代码生成
- `ts` - TypeScript代码生成
- `go` - Go代码生成
- `server` - 编辑器服务器
- `tsschema` - TypeScript Schema生成
- `json` - JSON生成
- `jsonbyai` - AI辅助JSON生成

## 环境配置

### Java 环境
- **JDK版本**：Java 21+
- **编码设置**：UTF-8
- **内存配置**：根据数据量调整JVM堆内存

### 构建属性
```bash
-PnoPoi                 # 构建时不包含POI依赖
```

## 数据文件配置

### config.cfg 文件
数据目录必须包含 `config.cfg` 文件，定义Schema和配置结构。

### 数据文件格式
- **CSV文件**：支持GBK编码，可包含BOM头
- **Excel文件**：支持.xlsx格式
- **JSON文件**：标准JSON格式

### 目录结构规则
- **根目录**：必须包含 `config.cfg` 文件
- **模块目录**：每个模块目录包含对应的 `.cfg` 文件和数据文件
- **JSON目录**：以下划线开头的目录用于JSON数据存储
- **文件命名**：
  - CSV/Excel文件：文件名作为表名
  - JSON文件：以主键值命名（如 `1.json`）
  - 分表文件：使用 `表名_序号` 格式（如 `lootitem_1.csv`）

### 目录结构示例
```
datadir/
│   config.cfg          # 根目录Schema配置
│
├───ai_行为             # 模块目录：AI行为
│       ai.cfg          # 模块Schema配置
│       ai行为.xlsx     # Excel数据文件
│
├───equip               # 模块目录：装备
│       ability.csv     # CSV数据文件
│       equip.cfg       # 模块Schema配置
│       equipconfig.csv
│       jewelry.csv
│       jewelryrandom.csv
│       jewelrysuit.csv
│       jewelrytype.csv
│       rank.csv
│
├───other               # 模块目录：其他
│       drop.csv
│       loot.csv
│       lootitem.csv
│       lootitem_1.csv  # 分表文件
│       lootitem_2.csv  # 分表文件
│       monster.csv
│       other.cfg       # 模块Schema配置
│       signin.csv
│
├───task                # 模块目录：任务
│       completeconditiontype任务完成条件类型.csv
│       task.cfg        # 模块Schema配置
│       taskextraexp.csv
│       task_任务.csv
│
├───_other_keytest      # JSON数据目录（下划线开头）
│       0.json          # JSON数据文件
│       1,2.json
│
└───_task_task2         # JSON数据目录
        1.json
        2.json
        3.json
        4.json
        5.json
        6.json
        7.json
        8.json
```

## 性能配置

### 内存优化
- 使用缓存文件输出流减少IO操作
- 支持大文件分块处理
- 增量生成避免重复处理

### 处理优化
- 并行数据读取
- 模板预编译
- 缓存重复计算

## 日志配置

### 日志级别
- **默认**：只显示错误和重要信息
- **-v**：显示统计信息和警告
- **-vv**：显示详细处理信息

### 性能分析
- **-p**：启用性能分析，显示内存使用和时间
- **-pp**：启用GC后性能分析

---
*最后更新：2025-11-04*