---
title: Schema 级别 Enum 类型支持
description: 在 .cfg schema 文件中定义编译时常量的枚举类型
sidebar:
  order: 1
---

> 最后更新：2026-03-08

## 上下文

用户需要在 .cfg schema 文件中定义编译时常量的枚举类型：

```cfg
enum ArgCaptureMode {
    Snapshot; // 快照模式
    Dynamic;  // 动态模式
}
```

然后在字段中使用：

```cfg
captureMode: ArgCaptureMode;
// 等价于: captureMode: str ->ArgCaptureMode;
```

**核心设计：**
- enum 定义转换成 **TableSchema**（带 MetaEnumValues）
- enum 字段 = **str + 外键引用**
- **零代码生成器改动**

## 实施计划

### 1. Metadata 层

#### 1.1 新增 MetaEnumValues record
- 路径: `app/src/main/java/configgen/schema/Metadata.java`

```java
public sealed interface MetaValue permits MetaTag, MetaInt, MetaFloat, MetaStr, MetaEnumValues {
    // ...
}

public record MetaEnumValues(List<EnumValue> values) implements MetaValue {
    public record EnumValue(String name, String comment) {}
}
```

#### 1.2 添加 Metadata 方法
```java
// enum table 的值列表
public void putEnumValues(List<MetaEnumValues.EnumValue> values) {
    data.put("enumValues", new MetaEnumValues(values));
}

public MetaEnumValues removeEnumValues() {
    MetaValue v = data.remove("enumValues");
    return v instanceof MetaEnumValues e ? e : null;
}

public MetaEnumValues getEnumValues() {
    MetaValue v = data.get("enumValues");
    return v instanceof MetaEnumValues e ? e : null;
}

public boolean hasEnumValues() {
    return data.get("enumValues") instanceof MetaEnumValues;
}

// 外键来自 enum 类型（用于 CfgWriter 还原）
public void putFromEnumType() {
    data.put("fromEnumType", TAG);
}

public boolean removeFromEnumType() {
    return data.remove("fromEnumType") != null;
}

public boolean isFromEnumType() {
    return data.containsKey("fromEnumType");
}
```

### 2. ANTLR 语法层

#### 2.1 修改 Cfg.g4
- 路径: `app/src/main/java/configgen/schema/cfg/Cfg.g4`

```antlr
// 新增关键字
ENUM       : 'enum';

// 修改 schema_ele
schema_ele
    : struct_decl
    | interface_decl
    | table_decl
    | enum_decl
    ;

// enum 声明规则
enum_decl
    : comment* ENUM ns_ident metadata LC_COMMENT enum_value* RC
    ;

enum_value
    : comment* identifier SEMI_COMMENT
    ;

// identifier 添加 ENUM 关键字
identifier
    : IDENT | STRUCT | INTERFACE | TABLE | ENUM | TLIST | TMAP | TBASE
    ;
```

### 3. 解析器层

#### 3.1 修改 CfgReader
- 路径: `app/src/main/java/configgen/schema/cfg/CfgReader.java`

添加 `read_enum()` 方法：

```java
private TableSchema read_enum(Enum_declContext ctx, String pkgNameDot) {
    String name = read_ns_ident(ctx.ns_ident());  // 保持大小写
    String lcComment = extractCommentFromToken(ctx.LC_COMMENT().getText());
    Metadata meta = read_metadata_with_comments(ctx.comment(), ctx.metadata(), lcComment);

    // 解析 enum 值
    List<MetaEnumValues.EnumValue> enumValues = new ArrayList<>();
    for (Enum_valueContext evc : ctx.enum_value()) {
        String valueName = evc.identifier().getText();
        String valueComment = extractCommentFromToken(evc.SEMI_COMMENT().getText());
        // 合并 leading comment
        for (CommentContext cc : evc.comment()) {
            valueComment = CommentUtils.buildComment(evc.comment(), valueComment);
        }
        enumValues.add(new MetaEnumValues.EnumValue(valueName, valueComment));
    }

    meta.putEnumValues(enumValues);

    // 创建虚拟 table schema
    return new TableSchema(
        pkgNameDot + name,
        new KeySchema(List.of("name")),
        new EEnum("name"),
        false,  // isColumnMode
        meta,
        List.of(
            new FieldSchema("name", Primitive.STRING, AUTO, Metadata.of()),
            new FieldSchema("comment", Primitive.STRING, AUTO, Metadata.of())
        ),
        List.of(),
        List.of()
    );
}
```

#### 3.2 修改 CfgWriter
- 路径: `app/src/main/java/configgen/schema/cfg/CfgWriter.java`

**写 enum 定义：**

```java
public void writeNamable(Nameable item, String prefix) {
    switch (item) {
        case StructSchema s -> writeStruct(s, prefix);
        case InterfaceSchema i -> writeInterface(i, prefix);
        case TableSchema t -> {
            MetaEnumValues enumValues = t.meta().getEnumValues();
            if (enumValues != null) {
                writeEnum(t, enumValues, prefix);
            } else {
                writeTable(t, prefix);
            }
        }
    }
}

private void writeEnum(TableSchema table, MetaEnumValues enumValues, String prefix) {
    // 写 enum 格式
    println("%senum %s {", prefix, table.lastName());
    for (MetaEnumValues.EnumValue ev : enumValues.values()) {
        String comment = ev.comment().isEmpty() ? "" : " // " + ev.comment();
        println("%s\t%s;%s", prefix, ev.name(), comment);
    }
    println("%s}", prefix);
    println();
}
```

**写 enum 字段（还原原始格式）：**

```java
private void writeStructural(Structural structural, String prefix) {
    for (FieldSchema f : structural.fields()) {
        ForeignKeySchema fk = structural.findForeignKey(f.name());

        // 检查是否是 enum 类型的字段
        String typeStr;
        String fkStr;
        if (fk != null && fk.meta().isFromEnumType()) {
            // enum 字段：还原为 enum 类型名，不写外键符号
            typeStr = fk.refTable();  // 如 "ArgCaptureMode"
            fkStr = "";
        } else {
            typeStr = typeStr(f.type());
            fkStr = fk == null ? "" : foreignStr(fk);
        }

        println("%s\t%s:%s%s%s;%s", prefix, f.name(), typeStr, fkStr, metadataStr(meta), comment.formatTrailing());
    }

    // 写独立外键时跳过 fromEnumType 的
    for (ForeignKeySchema fk : structural.foreignKeys()) {
        if (fk.meta().isFromEnumType()) {
            continue;  // 已在字段中处理
        }
        // ... 正常外键处理 ...
    }
}
```

### 4. Schema Resolver 层

#### 4.1 修改 CfgSchemaResolver
- 路径: `app/src/main/java/configgen/schema/CfgSchemaResolver.java`

**跳过 enum table 的小写检查：**

```java
private void step0_setImplInterfaceAndCheckTableName() {
    for (Nameable item : cfgSchema.items()) {
        if (item instanceof TableSchema table) {
            // 有 enumValues 的是 schema 定义的 enum，跳过小写检查
            if (table.meta().hasEnumValues()) {
                continue;
            }
            if (!table.name().equals(table.name().toLowerCase())) {
                errs.addErr(new TableNameNotLowerCase(table.name()));
            }
        }
        // ... 其他逻辑 ...
    }
}
```

**enum 字段类型转换（str + 外键）：**

```java
private SimpleType resolveSimpleType(SimpleType simpleType, FieldSchema field, Structural structural) {
    if (simpleType instanceof StructRef structRef) {
        // 首先检查是否是 enum table 引用
        TableSchema enumTable = findTableInLocalThenGlobal(structRef.name());
        if (enumTable != null && enumTable.meta().hasEnumValues()) {
            // enum 字段：转换为 STRING + 外键
            structural.updateFieldType(field.name(), STRING);

            // 创建外键（meta 标记 fromEnumType）
            Metadata fkMeta = Metadata.of();
            fkMeta.putFromEnumType();
            ForeignKeySchema fk = new ForeignKeySchema(
                field.name(),
                new KeySchema(List.of(field.name())),
                enumTable.name(),
                new RefPrimary(false),
                fkMeta
            );
            structural.addForeignKey(fk);
            return STRING;
        } else {
            // 普通 struct 引用
            // ... 原有逻辑 ...
        }
    }
    return simpleType;
}
```

### 5. Structural 接口扩展

添加方法支持修改字段类型和添加外键：

```java
default void updateFieldType(String fieldName, FieldType newType) {
    List<FieldSchema> fieldList = fields();
    for (int i = 0; i < fieldList.size(); i++) {
        if (fieldList.get(i).name().equals(fieldName)) {
            FieldSchema old = fieldList.get(i);
            fieldList.set(i, new FieldSchema(old.name(), newType, old.fmt(), old.meta()));
            return;
        }
    }
}

default void addForeignKey(ForeignKeySchema fk) {
    foreignKeys().add(fk);
}
```

### 6. 值解析层

#### 6.1 修改 VTableCreator
- 路径: `app/src/main/java/configgen/value/VTableCreator.java`

```java
public VTable create(List<VStruct> valueList) {
    // 检查是否是 schema 定义的 enum
    MetaEnumValues enumValues = tableSchema.meta().getEnumValues();

    if (enumValues != null) {
        // 生成虚拟数据
        valueList = new ArrayList<>();
        Source autoSource = Source.of();
        for (MetaEnumValues.EnumValue ev : enumValues.values()) {
            VStruct vStruct = new VStruct(
                tableSchema,
                List.of(
                    new VString(ev.name(), autoSource),
                    new VString(ev.comment(), autoSource)
                ),
                autoSource
            );
            valueList.add(vStruct);
        }
    }

    // 现有逻辑
    // ...
}
```

## 关键文件列表

| 文件 | 修改类型 |
|------|----------|
| `schema/Metadata.java` | 修改 |
| `schema/Structural.java` | 修改 |
| `schema/cfg/Cfg.g4` | 修改 |
| `schema/cfg/CfgReader.java` | 修改 |
| `schema/cfg/CfgWriter.java` | 修改 |
| `schema/CfgSchemaResolver.java` | 修改 |
| `value/VTableCreator.java` | 修改 |

## 验证方法

```bash
cd app && ./gradlew.bat test
```

## 使用示例

定义 enum：

```cfg
enum ArgCaptureMode {
    Snapshot; // 快照模式
    Dynamic;  // 动态模式
}
```

使用 enum：

```cfg
table trigger[id] {
    id:int;
    name:str;
    captureMode:ArgCaptureMode;  // 等价于 captureMode:str ->ArgCaptureMode
}
```

在容器中使用：

```cfg
table config[id] {
    id:int;
    modes:list<ArgCaptureMode>;  // 元素类型会转换为 str，并创建外键
    idToMode:map<int,ArgCaptureMode>;  // value 类型会转换为 str，并创建外键
    modeToId:map<ArgCaptureMode,int>;  // key 类型会转换为 str，但不创建外键（map key 不支持外键）
}
```
