# 测试指南 - 标准操作流程

## 概述

本指南详细说明cfggen项目的测试环境、测试策略、确保代码质量和功能稳定性。

## 测试环境设置

### 依赖配置
在 `build.gradle` 中配置测试依赖：

```gradle
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.1'
    // testImplementation 'org.mockito:mockito-inline:5.2.0'  // 当前注释，可选
    // testImplementation 'org.mockito:mockito-core:5.12.0'   // 当前注释，可选
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        csv.required = true
        xml.required = true
        html.required = true
        html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
    }
}
```

### 测试资源
测试资源文件位于 `src/test/resources/`：

```
src/test/resources/
├── testdata/          # 测试数据文件
│   ├── sample.csv     # CSV测试数据
│   ├── sample.xlsx    # Excel测试数据
│   └── sample.json    # JSON测试数据
└── schema/           # 测试Schema
    └── test.cfg      # 测试Schema定义
```

## 单元测试编写指南

## 测试原则
- 测试行为，而非实现：将测试重点放在代码的功能上，而不是它的实现方式上，以降低测试的脆弱性。

### 测试类命名规范
- 测试类名：`被测试类名 + Test`
- 包结构：与被测试类相同的包结构

### 测试方法命名
should[预期行为]_when[特定条件]


### 测试报告查看
测试报告位置：
- **测试结果**：`build/reports/tests/test/`
- **覆盖率报告**：`build/jacocoHtml/`

---
*最后更新：2025-11-04*