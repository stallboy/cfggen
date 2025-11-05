# 添加新生成器 - 标准操作流程

## 概述

本流程指导如何为cfggen项目添加新的编程语言生成器，确保新生成器符合项目架构和代码规范。

## 前置条件

- 熟悉目标编程语言的基本语法和特性
- 了解cfggen项目的架构和生成器系统
- 具备Java开发经验
- 熟悉Gradle构建系统

## 操作步骤

### 步骤1：创建生成器类

#### 1.1 选择包位置
在 `src/main/java/configgen/` 目录下创建新的包，命名规则：`gen{语言缩写}`

例如：
- Go语言：`gengo`
- Python语言：`genpy`
- Rust语言：`genrust`

#### 1.2 创建生成器类
继承 `configgen.gen.Generator` 基类：

```java
package configgen.gen{语言缩写};

import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.ctx.Context;

import java.io.IOException;

public class Gen{语言名} extends Generator {

    public Gen{语言名}(Parameter parameter) {
        super(parameter);
        // 配置生成器参数
        parameter.add("output", "输出目录", "./generated/{语言缩写}");
        parameter.add("encoding", "文件编码", "UTF-8");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        // 实现生成逻辑
    }
}
```

### 步骤2：注册生成器

在 `configgen.gen.Main` 类的 `main0` 方法中注册新生成器：

```java
// 在Generators.addProvider调用处添加
Generators.addProvider("{语言缩写}", Gen{语言名}::new);
```

### 步骤3：创建模板文件

#### 3.1 创建模板目录
在 `src/main/resources/jte/` 下创建对应的模板目录：

```
src/main/resources/jte/
└── {语言缩写}/          # 新语言模板目录
    └── code.jte       
```

#### 3.2 编写模板文件
使用JTE模板语法编写模板：

```jte
@import configgen.schema.cfg.*
@import configgen.data.*

// 类定义模板
@param TableDef table

public class ${table.name} {
    @for FieldDef field in table.fields
    private ${mapType(field.type)} ${field.name};
    @endfor

    // Getter/Setter方法
    @for FieldDef field in table.fields
    public ${mapType(field.type)} get${upper1(field.name)}() {
        return this.${field.name};
    }

    public void set${upper1(field.name)}(${mapType(field.type)} ${field.name}) {
        this.${field.name} = ${field.name};
    }
    @endfor
}
```

### 步骤4：实现类型映射

#### 4.1 创建类型映射工具
```java
public class TypeMapper {

    public static String mapType(String schemaType) {
        switch (schemaType) {
            case "int32":
                return "int";
            case "string":
                return "string";
            case "bool":
                return "bool";
            case "float":
                return "float";
            default:
                return schemaType; // 自定义类型
        }
    }

    public static String mapArrayType(String schemaType) {
        return mapType(schemaType) + "[]";
    }
}
```

### 步骤5：实现生成逻辑

```java
private void generateCode(Context ctx) throws IOException {
    for (TableDef table : ctx.getSchema().getTables()) {
        File outputFile = new File(parameter.get("output"), table.getName() + ".{扩展名}");

        try (CachedIndentPrinter ps = createCode(outputFile, parameter.get("encoding"))) {
            // 应用模板
            String template = loadTemplate("code.jte");
            Map<String, Object> params = new HashMap<>();
            params.put("table", table);
            params.put("mapper", new TypeMapper());

            String code = applyTemplate(template, params);
            ps.print(code);
        }
    }
}
```


### 步骤6：添加测试

#### 6.1 创建测试类
在 `src/test/java/configgen/gen{语言缩写}/` 目录下创建测试类：

```java
package configgen.gen{语言缩写};

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Gen{语言名}Test {

    @Test
    public void testTypeMapping() {
        // 测试类型映射
        assertEquals("int", TypeMapper.mapType("int32"));
        assertEquals("string", TypeMapper.mapType("string"));
    }

    @Test
    public void testCodeGeneration() {
        // 测试代码生成
        // ...
    }
}
```

#### 6.2 集成测试
创建端到端测试，验证生成器功能：

```java
@Test
public void testEndToEnd() throws IOException {
    // 设置测试数据
    // 执行生成器
    // 验证输出文件
}
```

### 步骤7：更新文档

#### 7.1 更新使用文档
在相关文档中添加新生成器的使用说明：

- 命令行参数说明
- 配置选项说明
- 使用示例

#### 7.2 更新架构文档
在架构文档中添加新生成器的说明：

- 生成器位置
- 功能描述
- 技术特点

## 验证和测试

### 功能验证
1. **编译测试**：确保生成的代码可以编译
2. **功能测试**：验证生成代码的功能正确性
3. **性能测试**：测试生成性能
4. **兼容性测试**：验证与现有系统的兼容性

### 质量检查
1. **代码规范**：遵循项目代码规范
2. **测试覆盖**：确保足够的测试覆盖率
3. **文档完整**：更新所有相关文档

## 常见问题

### Q: 如何处理目标语言的特殊语法？
A: 在模板中使用条件判断处理语言特定语法，或创建语言特定的模板助手类。

### Q: 如何处理目标语言的包管理？
A: 在生成器中实现包结构生成，支持模块化和依赖管理。

## 最佳实践

1. **模块化设计**：将生成逻辑分解为可重用的模块
2. **模板复用**：尽可能复用现有模板模式,但不要跨Generator复用
3. **错误处理**：提供详细的错误信息和恢复建议
5. **可维护性**：保持代码清晰和文档完整

---
*最后更新：2025-11-05*