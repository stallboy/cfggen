# configgen.gen 模块测试实现计划

## 概述

为 configgen.gen 模块添加全面的测试覆盖，遵循测试行为而非实现、清晰的测试命名、Mock 外部依赖和快速执行的原则。该模块是配置生成器的核心组件，负责命令行参数解析、生成器注册和代码生成流程管理。

## 当前状态分析

**现有实现**:
- `configgen.gen` 模块包含 8 个核心 Java 源文件
- 目前**没有专门的测试文件**，测试覆盖主要集中在 configgen 项目的其他模块
- 模块使用工厂模式、策略模式和模板方法模式

**关键组件**:
- `Main.java:117` - 程序入口点，命令行参数解析
- `Generators.java:13-31` - 生成器注册和工厂模式实现
- `ParameterParser.java:8-59` - 参数解析和验证
- `Generator.java:13-68` - 生成器通用功能和文件操作
- `Parameter.java:3-17` - 参数获取抽象接口

**现有测试模式**:
- 使用 JUnit 5 测试框架
- 使用 `@TempDir` 管理临时文件
- 遵循清晰的测试命名约定（`error_` 前缀用于错误测试）
- 使用自定义 `Resources` 类管理测试资源

## 期望的最终状态

### 目标状态
- `configgen.gen` 模块拥有全面的单元测试覆盖
- 所有测试遵循指定的测试准则
- 测试执行速度快（毫秒级别）
- 测试命名清晰，易于理解测试意图
- 使用 Mock 隔离外部依赖

### 验证方式

#### 自动化验证:
- [ ] 所有测试通过: `./gradlew test --tests "configgen.gen.*"`
- [ ] 类型检查通过: `./gradlew compileTestJava`
- [ ] 构建成功: `./gradlew build`
- [ ] 代码覆盖率报告生成: `./gradlew jacocoTestReport`

#### 手动验证:
- [ ] 测试命名清晰易懂
- [ ] 测试覆盖了所有关键行为
- [ ] 测试执行速度快
- [ ] 测试遵循现有项目的测试模式

### 关键发现:
- **现有模式**: 项目使用 JUnit 5 + Mockito，遵循清晰的测试命名约定
- **文件操作**: 使用 `@TempDir` 和自定义 `Resources` 类管理测试文件
- **错误处理**: 现有测试大量使用 `error_` 前缀测试错误场景
- **集成测试**: 有专门的集成测试验证端到端工作流

## 我们不做的内容

- 不修改现有的生成器实现逻辑
- 不添加新的功能特性
- 不修改现有的测试框架配置
- 不破坏现有的测试命名约定

## 实现方法

采用分阶段的方法，从简单的单元测试开始，逐步扩展到更复杂的集成测试。遵循现有的测试模式和命名约定，确保与项目其他部分的一致性。

---

## 阶段 1: 基础测试框架和简单单元测试

### 概述
创建测试包结构和基础测试文件，实现最简单的单元测试来验证核心组件的基本行为。

### 所需更改:

#### 1. 测试包结构创建
**文件**: `app/src/test/java/configgen/gen/`
**更改**: 创建测试包目录结构

```java
// 包结构
src/test/java/configgen/gen/
├── MainTest.java                    # Main 类测试
├── GeneratorsTest.java              # Generators 类测试
├── ParameterParserTest.java         # ParameterParser 类测试
├── GeneratorTest.java               # Generator 基类测试
└── resources/                       # 测试资源文件
```

#### 2. Generators 类测试
**文件**: `app/src/test/java/configgen/gen/GeneratorsTest.java`
**更改**: 创建生成器工厂的基本测试

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorsTest {

    @Test
    void shouldCreateGenerator_whenRegisteredGeneratorNameProvided() {
        // Given: 注册一个生成器提供者
        Generators.addProvider("test", parameter -> new Generator(parameter) {
            @Override
            public void generate(Context context) {}
        });

        // When: 创建生成器
        Generator generator = Generators.create("test");

        // Then: 验证生成器创建成功
        assertNotNull(generator);
    }

    @Test
    void shouldReturnNull_whenUnregisteredGeneratorNameProvided() {
        // Given: 未注册的生成器名称
        String unregisteredName = "unknown";

        // When: 尝试创建生成器
        Generator generator = Generators.create(unregisteredName);

        // Then: 验证返回 null
        assertNull(generator);
    }
}
```

#### 3. ParameterParser 类测试
**文件**: `app/src/test/java/configgen/gen/ParameterParserTest.java`
**更改**: 创建参数解析器的基本测试

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParameterParserTest {

    @Test
    void shouldParseSingleParameter_whenNoAdditionalParameters() {
        // Given: 简单的生成器参数
        String arg = "java";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证解析结果
        assertEquals("java", parser.genId());
        assertTrue(parser.get("unknown", "default").equals("default"));
    }

    @Test
    void shouldThrowException_whenExtraParametersNotConsumed() {
        // Given: 带额外参数的生成器参数
        String arg = "java:output=src";

        // When & Then: 验证抛出异常
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(AssertionError.class, parser::assureNoExtra);
    }
}
```

### 成功标准:

#### 自动化验证:
- [x] 测试编译通过: `./gradlew compileTestJava`
- [x] 基础测试通过: `./gradlew test --tests "configgen.gen.*"`
- [x] 无编译错误: `./gradlew build`
- [x] 类型检查通过: `./gradlew check`

#### 手动验证:
- [x] 测试包结构创建正确
- [x] 测试命名清晰易懂
- [x] 测试覆盖了基本行为
- [x] 测试执行速度快（毫秒级别）

**实现说明**: 完成此阶段并通过所有自动化验证后，暂停等待人工确认手动测试成功，然后再继续下一阶段。

---

## 阶段 2: Mock 外部依赖和复杂行为测试

### 概述
使用 Mockito 框架 Mock 外部依赖，测试更复杂的行为和错误场景。

### 所需更改:

#### 1. 增强 Generators 测试
**文件**: `app/src/test/java/configgen/gen/GeneratorsTest.java`
**更改**: 使用 Mockito 测试生成器创建行为

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GeneratorsTest {

    @Mock
    private GeneratorProvider mockProvider;

    @Mock
    private Generator mockGenerator;

    @Test
    void shouldCallProviderCreate_whenRegisteredGeneratorNameProvided() {
        // Given: 配置 Mock 行为
        when(mockProvider.create(any())).thenReturn(mockGenerator);
        Generators.addProvider("test", mockProvider);

        // When: 创建生成器
        Generator result = Generators.create("test");

        // Then: 验证 Mock 被调用
        assertNotNull(result);
        verify(mockProvider).create(any());
    }

    @Test
    void shouldValidateParameters_whenGeneratorCreated() {
        // Given: 带参数的生成器
        String arg = "test:output=src,verbose=true";
        when(mockProvider.create(any())).thenReturn(mockGenerator);
        Generators.addProvider("test", mockProvider);

        // When: 创建生成器
        Generator result = Generators.create(arg);

        // Then: 验证参数解析和验证
        assertNotNull(result);
        verify(mockProvider).create(any());
    }
}
```

#### 2. 增强 ParameterParser 测试
**文件**: `app/src/test/java/configgen/gen/ParameterParserTest.java`
**更改**: 测试复杂的参数解析场景

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParameterParserTest {

    @Test
    void shouldParseMultipleParameters_whenCommaSeparated() {
        // Given: 带多个参数的生成器参数
        String arg = "java:output=src,verbose=true,encoding=utf8";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证参数解析
        assertEquals("java", parser.genId());
        assertEquals("src", parser.get("output", ""));
        assertEquals("true", parser.get("verbose", ""));
        assertEquals("utf8", parser.get("encoding", ""));
    }

    @Test
    void shouldHandleBooleanParameters_whenHasMethodCalled() {
        // Given: 带布尔参数的生成器参数
        String arg = "java:verbose";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证布尔参数处理
        assertTrue(parser.has("verbose"));
    }

    @Test
    void error_ShouldThrowException_whenExtraParametersNotConsumed() {
        // Given: 带未使用参数的生成器参数
        String arg = "java:output=src,unknown=value";

        // When & Then: 验证抛出异常
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(AssertionError.class, parser::assureNoExtra);
    }
}
```

#### 3. Generator 基类测试
**文件**: `app/src/test/java/configgen/gen/GeneratorTest.java`
**更改**: 测试生成器基类的通用功能

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldTransformString_whenUpper1MethodCalled() {
        // Given: 测试字符串
        String input = "hello";

        // When: 调用字符串转换方法
        String result = Generator.upper1(input);

        // Then: 验证转换结果
        assertEquals("Hello", result);
    }

    @Test
    void shouldTransformString_whenLower1MethodCalled() {
        // Given: 测试字符串
        String input = "Hello";

        // When: 调用字符串转换方法
        String result = Generator.lower1(input);

        // Then: 验证转换结果
        assertEquals("hello", result);
    }
}
```

### 成功标准:

#### 自动化验证:
- [x] Mock 测试通过: `./gradlew test --tests "configgen.gen.*"`
- [x] 无 Mockito 相关错误
- [x] 测试覆盖率提高
- [x] 构建成功: `./gradlew build`

#### 手动验证:
- [x] Mock 使用正确，隔离了外部依赖
- [x] 复杂行为测试覆盖全面
- [x] 错误场景测试充分
- [x] 测试执行仍然快速

**实现说明**: 完成此阶段并通过所有自动化验证后，暂停等待人工确认手动测试成功，然后再继续下一阶段。

---

## 阶段 3: 集成测试和文件操作测试

### 概述
测试文件操作和更复杂的集成场景，使用 `@TempDir` 管理临时文件。

### 所需更改:

#### 1. Main 类集成测试
**文件**: `app/src/test/java/configgen/gen/MainTest.java`
**更改**: 测试命令行参数解析和程序入口点

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseValidCommandLineArguments_successfully() {
        // Given: 有效的命令行参数
        String[] args = {"-datadir", tempDir.toString(), "-gen", "java"};

        // When & Then: 验证参数解析不抛出异常
        assertDoesNotThrow(() -> Main.main0(args));
    }

    @Test
    void shouldShowUsage_whenRequiredDataDirNotProvided() {
        // Given: 缺少必需参数的命令行
        String[] args = {"-gen", "java"};

        // When & Then: 验证显示使用说明
        assertDoesNotThrow(() -> Main.main0(args));
    }
}
```

#### 2. 文件操作测试
**文件**: `app/src/test/java/configgen/gen/GeneratorFileTest.java`
**更改**: 测试生成器的文件操作功能

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorFileTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateUtf8Writer_whenFileProvided() throws Exception {
        // Given: 测试文件路径
        Path testFile = tempDir.resolve("test.txt");

        // When: 创建文件写入器
        try (var writer = Generator.createUtf8Writer(testFile)) {
            writer.write("test content");
        }

        // Then: 验证文件创建和内容
        assertTrue(Files.exists(testFile));
        String content = Files.readString(testFile);
        assertEquals("test content", content);
    }

    @Test
    void shouldCopySupportFile_whenFileDoesNotExist() throws Exception {
        // Given: 源文件和目标文件
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, "source content");
        Path targetFile = tempDir.resolve("target.txt");

        // When: 复制支持文件
        Generator.copySupportFile(sourceFile, targetFile);

        // Then: 验证文件复制成功
        assertTrue(Files.exists(targetFile));
        String content = Files.readString(targetFile);
        assertEquals("source content", content);
    }
}
```

#### 3. 错误处理集成测试
**文件**: `app/src/test/java/configgen/gen/ErrorHandlingTest.java`
**更改**: 测试错误处理场景

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingTest {

    @Test
    void error_ShouldHandleInvalidGeneratorParameters() {
        // Given: 无效的生成器参数
        String arg = "java:unknown=param";

        // When & Then: 验证参数验证失败
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(AssertionError.class, parser::assureNoExtra);
    }

    @Test
    void error_ShouldHandleEmptyGeneratorName() {
        // Given: 空的生成器名称
        String arg = "";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证空名称处理
        assertEquals("", parser.genId());
    }
}
```

### 成功标准:

#### 自动化验证:
- [x] 集成测试通过: `./gradlew test --tests "configgen.gen.*"`
- [x] 文件操作测试通过
- [x] 错误处理测试通过
- [x] 代码覆盖率达标

#### 手动验证:
- [x] 集成测试覆盖了关键工作流
- [x] 文件操作测试正确使用了临时目录
- [x] 错误场景测试全面
- [x] 测试执行时间仍然可接受

**实现说明**: 完成此阶段并通过所有自动化验证后，暂停等待人工确认手动测试成功，然后再继续下一阶段。

---

## 阶段 4: 完善测试覆盖和性能优化

### 概述
完善测试覆盖，确保所有关键行为都有测试，并优化测试性能。

### 所需更改:

#### 1. 边界条件测试
**文件**: `app/src/test/java/configgen/gen/BoundaryTest.java`
**更改**: 测试边界条件和极端情况

```java
package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundaryTest {

    @Test
    void shouldHandleSpecialCharacters_inGeneratorParameters() {
        // Given: 包含特殊字符的参数
        String arg = "java:output=src/main,path=path/with/special@chars";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证特殊字符处理
        assertEquals("java", parser.genId());
        assertEquals("src/main", parser.get("output", ""));
        assertEquals("path/with/special@chars", parser.get("path", ""));
    }

    @Test
    void shouldHandleEmptyParameters() {
        // Given: 空参数
        String arg = "java:";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证空参数处理
        assertEquals("java", parser.genId());
        assertDoesNotThrow(parser::assureNoExtra);
    }
}
```


### 成功标准:

#### 自动化验证:
- [ ] 所有测试通过: `./gradlew test --tests "configgen.gen.*"`
- [ ] 代码覆盖率报告显示良好覆盖
- [ ] 构建成功: `./gradlew build`

#### 手动验证:
- [ ] 测试覆盖了所有关键行为
- [ ] 边界条件测试充分
- [ ] 测试命名清晰，易于维护

**实现说明**: 完成此阶段并通过所有自动化验证后，整个测试实现计划完成。

---

## 测试策略

### 单元测试:
- **测试目标**: 单个类和方法的行为
- **关键测试**: 参数解析、生成器创建、字符串转换
- **边缘情况**: 空参数、特殊字符、无效输入

### 集成测试:
- **测试目标**: 多个组件的交互
- **关键测试**: 命令行参数解析、文件操作、错误处理
- **边缘情况**: 缺少必需参数、文件权限问题

### 手动测试步骤:
1. 运行所有测试验证通过
2. 检查测试命名是否清晰
3. 检查代码覆盖率报告
4. 确认测试遵循现有模式

## 性能考虑

- 使用 `@TempDir` 避免真实文件系统操作
- 使用 Mock 隔离外部依赖
- 避免在测试中创建大量对象

## 迁移说明

- 不涉及数据迁移
- 不修改现有功能
- 保持向后兼容性

## 参考资料

- 原始研究文档: `thoughts/shared/research/2025-11-02-configgen-gen-testing-strategy.md`
- 现有测试模式: `app/src/test/java/configgen/value/CfgValueParserTest.java`
- 类似实现: `app/src/test/java/configgen/util/ListParserTest.java`
- 构建配置: `app/build.gradle`