---
title: SchemaToCsvTool 工具
description: 根据 Schema 自动生成 CSV 模板文件
sidebar:
  order: 2
---

> 最后更新：2026-03-08

## Context

为所有缺失 CSV 文件的 table 生成 CSV 模板，包含 A2_Default 格式的 header：
- **第一行**：注释行（comment row）
- **第二行**：name 行（name row）

## 核心命名规则

### 规则总结
1. **占 1 列的字段**：列名 = `fieldname`（直接用字段名）
2. **占多列的字段**：
   - **第一列**：`fieldname._xxx`（带顶层字段名前缀）
   - **后续列**：`_xxx`（只有 `_` 前缀）

### 示例

| 字段定义 | 生成列名 |
|---------|---------|
| `id:int` | `id` |
| `lvlRank:LevelRank (auto)` | `lvlRank._level`, `_rank` |
| `items:list<int> (fix=3)` | `items._1`, `_2`, `_3` |
| `ranges:list<Range> (fix=2)` | `ranges._rmin`, `_rmax`, `_rmin`, `_rmax` |
| `attrs:map<int,int> (fix=2)` | `attrs._k1`, `_v1`, `_k2`, `_v2` |
| `ranges:map<int,Range> (fix=2)` | `ranges._k1`, `_rmin`, `_rmax`, `_k2`, `_rmin`, `_rmax` |

### 嵌套 Struct 示例

```cfg
struct B { y:int; z:int; }
struct A { x:int; b:B; }

table t[id] { a:A; }
```
生成列名：`a._x`, `_y`, `_z`
- 嵌套的字段**不**再带父字段名 `b`

### Interface 示例

```cfg
interface Reward {
    struct Item { itemId:int; count:int; }
    struct Gold { gold:int; }
}

table t[id] { reward:Reward; }
```
生成列名：`reward._type`, `_p1`, `_p2`
- 第一列是类型标识 `reward._type`
- 后续列用通用命名 `_p1`, `_p2`, ...（数量取 span 最大的 impl）

## Comment 规则

### 规则总结

Comment 只添加到**每个顶层字段的第一列**，后续列的 comment 为空。

1. **占 1 列的字段**：直接使用字段的 comment
2. **占多列的字段**：
   - 第一列使用顶层字段的 comment
   - 后续列 comment 为空

### 各类型处理

| 字段类型 | Comment 位置 |
|---------|-------------|
| 基础类型 `int`, `string` 等 | 该唯一列 |
| `Struct` (auto) | 第一个子字段列 |
| `list<T>` (fix) | 第一个元素 (`_1`) |
| `map<K,V>` (fix) | 第一个 key (`_k1`) |
| `Interface` | `_type` 列 |

### 示例

```cfg
items:list<int> (fix=3);  // 物品列表
```
生成：
- `items._1` → comment: "物品列表"
- `_2` → comment: ""
- `_3` → comment: ""

```cfg
attrs:map<int,int> (fix=2); // 属性映射
```
生成：
- `attrs._k1` → comment: "属性映射"
- `_v1` → comment: ""
- `_k2` → comment: ""
- `_v2` → comment: ""

## 实现方案

### SchemaToCsvHeader 类

**文件**: `app/src/main/java/configgen/tool/SchemaToCsvHeader.java`

```java
package configgen.tool;

import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.PACK;

class SchemaToCsvHeader {
    private final List<String> commentRow = new ArrayList<>();
    private final List<String> nameRow = new ArrayList<>();
    private boolean isFirstOfField = true;
    private String topLevelName = "";
    private String topLevelComment = "";

    void flattenFields(List<FieldSchema> fields) {
        for (FieldSchema field : fields) {
            isFirstOfField = true;
            topLevelName = field.name();
            topLevelComment = field.comment() != null ? field.comment() : "";
            flattenField(field);
        }
    }

    private void flattenField(FieldSchema field) {
        int span = Span.fieldSpan(field);
        String name = field.name();
        // isFirstOfField 时用顶层 comment，否则用字段自己的
        String comment = isFirstOfField ? topLevelComment : (field.comment() != null ? field.comment() : "");

        if (span == 1) {
            // 占 1 列
            String colName = isFirstOfField ? name : "_" + name;
            addColumn(colName, comment);
            return;
        }

        // span > 1 的情况
        // 注意：PACK 或 Sep 格式时 span == 1，已在前面处理并 return
        switch (field.type()) {
            case FieldType.StructRef structRef -> {
                flattenFieldable(structRef.obj());
            }
            case FieldType.FList fList -> {
                int count = getCount(field.fmt());
                for (int i = 1; i <= count; i++) {
                    // 只有第一个元素加 comment（使用顶层 comment）
                    String itemComment = isFirstOfField ? topLevelComment : "";
                    flattenSimpleType(fList.item(), String.valueOf(i), itemComment);
                }
            }
            case FieldType.FMap fMap -> {
                int count = getCount(field.fmt());
                for (int i = 1; i <= count; i++) {
                    // 只有第一个 key 加 comment（使用顶层 comment）
                    String keyComment = isFirstOfField ? topLevelComment : "";
                    flattenSimpleType(fMap.key(), "k" + i, keyComment);
                    flattenSimpleType(fMap.value(), "v" + i, "");
                }
            }
            default -> { }
        }
    }

    // 抽取公共逻辑：展开 Fieldable (Struct 或 Interface)
    private void flattenFieldable(Fieldable obj) {
        if (obj instanceof StructSchema ss) {
            for (FieldSchema sub : ss.fields()) {
                flattenField(sub);
            }
        } else if (obj instanceof InterfaceSchema is) {
            flattenInterface(is);
        }
    }

    private void flattenInterface(InterfaceSchema is) {
        // 类型列
        String typeColName = isFirstOfField ? topLevelName + "._type" : "_type";
        String typeComment = isFirstOfField ? topLevelComment : "";
        addColumn(typeColName, typeComment);

        // 使用通用 _p1, _p2, ... 命名（数量 = Span.span(is) - 1，去掉 _type 列）
        int dataSpan = Span.span(is) - 1;
        for (int i = 1; i <= dataSpan; i++) {
            addColumn("_p" + i, "");
        }
    }

    private void flattenSimpleType(FieldType.SimpleType type, String suffix, String comment) {
        int span = Span.simpleTypeSpan(type);

        if (span == 1) {
            // 占 1 列
            String colName = isFirstOfField ? topLevelName + "._" + suffix : "_" + suffix;
            addColumn(colName, comment);
            return;
        }

        // span > 1 的情况，递归处理
        switch (type) {
            case FieldType.StructRef structRef -> {
                flattenFieldable(structRef.obj());
            }
        }
    }

    private int getCount(FieldFormat fmt) {
        if (fmt instanceof FieldFormat.Fix fix) return fix.count();
        if (fmt instanceof FieldFormat.Block block) return block.fix();
        return 0;
    }

    private void addColumn(String name, String comment) {
        nameRow.add(name);
        commentRow.add(comment);
        isFirstOfField = false;
    }

    List<String> getCommentRow() { return commentRow; }
    List<String> getNameRow() { return nameRow; }
}
```

### SchemaToCsvTool 类

**文件**: `app/src/main/java/configgen/tool/SchemaToCsvTool.java`

```java
package configgen.tool;

import configgen.ctx.DirectoryStructure;
import configgen.gen.Tool;
import configgen.gen.ParameterParser;
import configgen.schema.*;
import configgen.util.CSVUtil;
import configgen.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SchemaToCsvTool extends Tool {
    private final Path dataDir;

    public SchemaToCsvTool(ParameterParser parameter) {
        super(parameter);
        this.dataDir = Path.of(parameter.get("datadir", "."));
    }

    @Override
    public void call() {
        DirectoryStructure structure = new DirectoryStructure(dataDir);
        CfgSchema cfgSchema = CfgSchemas.readFromDir(structure);
        cfgSchema.resolve().checkErrors();

        for (TableSchema table : cfgSchema.tableMap().values()) {
            if (table.isJson()) continue;

            Path csvPath = dataDir.resolve(table.namespace())
                                  .resolve(table.lastName() + ".csv");
            if (Files.exists(csvPath)) {
                Logger.log("Skip existing: %s", csvPath);
                continue;
            }

            SchemaToCsvHeader headerGen = new SchemaToCsvHeader();
            headerGen.flattenFields(table.fields());

            List<List<String>> rows = List.of(
                headerGen.getCommentRow(),
                headerGen.getNameRow()
            );
            try {
                CSVUtil.writeToFile(csvPath.toFile(), rows);
                Logger.log("Generated: %s", csvPath);
            } catch (IOException e) {
                Logger.log("Failed to write %s: %s", csvPath, e.getMessage());
            }
        }
    }
}
```

### 注册工具

**文件**: `app/src/main/java/configgen/gen/Main.java`

在 `registerAllProviders()` 方法中添加：
```java
Tools.addProvider("schematocsv", SchemaToCsvTool::new);
```

## 关键文件

| 文件 | 作用 |
|------|------|
| `configgen/schema/Span.java` | 字段列数计算 |
| `configgen/schema/TableSchema.java` | 表 Schema |
| `configgen/util/CSVUtil.java` | CSV 写入 |
| `configgen/tool/Tool.java` | Tool 基类 |
| `configgen/gen/Main.java` | 工具注册入口 |

## 验证

```bash
java -jar cfggen.jar -tool schematocsv,datadir=example/config
```

## 单元测试

**文件**: `app/src/test/java/configgen/tool/SchemaToCsvHeaderTest.java`

```java
package configgen.tool;

import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaToCsvHeaderTest {

    @Test
    void testPrimitiveField() {
        // id:int -> ["id"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id"), header.getNameRow());
    }

    @Test
    void testStructField() {
        // struct Range { rmin:int; rmax:int; }
        // range:Range -> ["range._rmin", "_rmax"]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct Range { rmin:int; rmax:int; }
                table t[id] { id:int; range:Range; }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "range._rmin", "_rmax"), header.getNameRow());
    }

    @Test
    void testNestedStruct() {
        // struct B { y:int; z:int; }
        // struct A { x:int; b:B; }
        // a:A -> ["a._x", "_y", "_z"]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct B { y:int; z:int; }
                struct A { x:int; b:B; }
                table t[id] { id:int; a:A; }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "a._x", "_y", "_z"), header.getNameRow());
    }

    @Test
    void testListPrimitive() {
        // items:list<int> (fix=3) -> ["items._1", "_2", "_3"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; items:list<int> (fix=3); }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "items._1", "_2", "_3"), header.getNameRow());
    }

    @Test
    void testListStruct() {
        // struct Range { rmin:int; rmax:int; }
        // ranges:list<Range> (fix=2) -> ["ranges._rmin", "_rmax", "_rmin", "_rmax"]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct Range { rmin:int; rmax:int; }
                table t[id] { id:int; ranges:list<Range> (fix=2); }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "ranges._rmin", "_rmax", "_rmin", "_rmax"), header.getNameRow());
    }

    @Test
    void testMapPrimitive() {
        // attrs:map<int,int> (fix=2) -> ["attrs._k1", "_v1", "_k2", "_v2"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; attrs:map<int,int> (fix=2); }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "attrs._k1", "_v1", "_k2", "_v2"), header.getNameRow());
    }

    @Test
    void testInterface() {
        // interface Reward { struct Item { itemId:int; count:int; } struct Gold { gold:int; } }
        // reward:Reward -> ["reward._type", "_p1", "_p2"]
        CfgSchema cfgSchema = CfgReader.parse("""
                interface Reward {
                    struct Item { itemId:int; count:int; }
                    struct Gold { gold:int; }
                }
                table t[id] { id:int; reward:Reward; }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "reward._type", "_p1", "_p2"), header.getNameRow());
    }

    // ========== Comment 测试 ==========

    @Test
    void testCommentPrimitive() {
        // id:int // 主键 -> comment: ["主键"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; // 主键\n }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("主键"), header.getCommentRow());
    }

    @Test
    void testCommentStruct() {
        // struct Range { rmin:int; rmax:int; }
        // range:Range // 范围 -> comment: ["", "范围", ""]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct Range { rmin:int; rmax:int; }
                table t[id] { id:int; range:Range; // 范围\n }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "范围", ""), header.getCommentRow());
    }

    @Test
    void testCommentList() {
        // items:list<int> (fix=3) // 物品列表 -> comment: ["", "物品列表", "", ""]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; items:list<int> (fix=3); // 物品列表\n }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "物品列表", "", ""), header.getCommentRow());
    }

    @Test
    void testCommentMap() {
        // attrs:map<int,int> (fix=2) // 属性映射 -> comment: ["", "属性映射", "", "", ""]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; attrs:map<int,int> (fix=2); // 属性映射\n }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "属性映射", "", "", ""), header.getCommentRow());
    }

    @Test
    void testCommentInterface() {
        // interface Reward { ... }
        // reward:Reward // 奖励 -> comment: ["", "奖励", "", ""]
        CfgSchema cfgSchema = CfgReader.parse("""
                interface Reward {
                    struct Item { itemId:int; count:int; }
                    struct Gold { gold:int; }
                }
                table t[id] { id:int; reward:Reward; // 奖励\n }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "奖励", "", ""), header.getCommentRow());
    }
}
```

### 测试用例总结

#### Name 行测试

| 测试用例 | 输入 Schema | 期望输出 name 行 |
|---------|------------|-----------------|
| `testPrimitiveField` | `id:int` | `["id"]` |
| `testStructField` | `range:Range` | `["range._rmin", "_rmax"]` |
| `testNestedStruct` | `a:A` (A 嵌套 B) | `["a._x", "_y", "_z"]` |
| `testListPrimitive` | `items:list<int> (fix=3)` | `["items._1", "_2", "_3"]` |
| `testListStruct` | `ranges:list<Range> (fix=2)` | `["ranges._rmin", "_rmax", "_rmin", "_rmax"]` |
| `testMapPrimitive` | `attrs:map<int,int> (fix=2)` | `["attrs._k1", "_v1", "_k2", "_v2"]` |
| `testInterface` | `reward:Reward` | `["reward._type", "_p1", "_p2"]` |

#### Comment 行测试

| 测试用例 | 输入 Schema | 期望输出 comment 行 |
|---------|------------|-------------------|
| `testCommentPrimitive` | `id:int // 主键` | `["主键"]` |
| `testCommentStruct` | `range:Range // 范围` | `["", "范围", ""]` |
| `testCommentList` | `items:list<int> (fix=3) // 物品列表` | `["", "物品列表", "", ""]` |
| `testCommentMap` | `attrs:map<int,int> (fix=2) // 属性映射` | `["", "属性映射", "", "", ""]` |
| `testCommentInterface` | `reward:Reward // 奖励` | `["", "奖励", "", ""]` |
