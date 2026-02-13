package configgen.gengd;

import configgen.schema.*;
import configgen.util.StringUtil;
import configgen.value.CfgValue;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;

public class StructModel {
    public final Name name;
    public final Structural structural;
    public final CfgValue.VTable _vTable;
    private final GdCodeGenerator gen;

    public StructModel(GdCodeGenerator gen, Structural structural, CfgValue.VTable _vTable) {
        this.gen = gen;
        this.name = new Name(gen.prefix, structural);
        this.structural = structural;
        this._vTable = _vTable;
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.prefix, nameable).className;
    }

    public String upper1(String value) {
        return StringUtil.upper1(value);
    }

    public String lower1(String value) {
        return StringUtil.lower1(value);
    }

    public String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int";
            case LONG -> "int";
            case FLOAT -> "float";
            case STRING -> "String";
            case TEXT -> gen.isLangSwitch ? "ConfigText" : "String";
            case StructRef structRef -> fullName(structRef.obj());
            case FList fList -> "Array[" + type(fList.item()) + "]";
            case FMap fMap -> "Dictionary[" + type(fMap.key()) + ", " + type(fMap.value()) + "]";
        };
    }

    public String create(FieldType t) {
        return switch (t) {
            case BOOL -> "stream.read_bool()";
            case INT -> "stream.read_int32()";
            case LONG -> "stream.read_int64()";
            case FLOAT -> "stream.read_float()";
            case STRING -> "stream.read_string_in_pool()";
            case TEXT -> gen.isLangSwitch ? "ConfigText._create(stream)" : "stream.read_text_in_pool()";
            case StructRef structRef -> fullName(structRef.obj()) + "._create(stream)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }

    public String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "Array[" + fullName(fk.refTableSchema()) + "]";
            }
            case RefKey.RefSimple refSimple -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return fullName(fk.refTableSchema());
                    }
                    case FList ignored2 -> {
                        return "Array[" + fullName(fk.refTableSchema()) + "]";
                    }
                    case FMap fMap -> {
                        // FMap类型的外键引用：键类型保持不变，值类型变成引用表的类型
                        return "Dictionary[" + type(fMap.key()) + ", " + fullName(fk.refTableSchema()) + "]";
                    }
                }
            }
        }
    }

    public String refName(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "RefList" + upper1(fk.name());
            }
            case RefKey.RefSimple refSimple -> {
                if (refSimple.nullable()) {
                    return "NullableRef" + upper1(fk.name());
                } else {
                    return "Ref" + upper1(fk.name());
                }
            }
        }
    }

    public String uniqueKeyGetByName(KeySchema keySchema) {
        return "find_by_" + keySchema.fields().stream()
                .map(StringUtil::lower1)
                .collect(Collectors.joining("_"));
    }

    public String uniqueKeyMapName(KeySchema keySchema) {
        return "_" + keySchema.fields().stream()
                .map(StringUtil::lower1)
                .collect(Collectors.joining("_")) + "_map";
    }

    public String keyClassName(KeySchema keySchema) {
        // GDScript没有元组类型，直接返回字段类型组合
        if (keySchema.fieldSchemas().size() > 1) {
            return "Dictionary"; // 复合键使用Dictionary表示
        } else {
            return type(keySchema.fieldSchemas().getFirst().type());
        }
    }

    public String actualParams(KeySchema keySchema) {
        return keySchema.fields().stream()
                .map(StringUtil::lower1)
                .collect(Collectors.joining(", "));
    }

    public String equals(List<FieldSchema> fs) {
        return fs.stream()
                .map(f -> lower1(f.name()) + " == other." + lower1(f.name()))
                .collect(Collectors.joining(" and "));
    }

    public String toStrings(List<FieldSchema> fs) {
        return fs.stream()
                .map(f -> toString(f.name(), f.type()))
                .collect(Collectors.joining(" + \",\" + "));
    }

    public String toString(String n, FieldType t) {
        // 所有字段都需要用 str() 包装，因为 GDScript 是强类型语言
        // String 和 int/float 等类型不能直接用 + 连接
        String varName = lower1(n);
        switch (t) {
            case Primitive.STRING -> {
                return varName;
            }
            case Primitive.TEXT -> {
                if (gen.isLangSwitch) {
                    return "str(" + varName + ")";
                } else {
                    return varName;
                }
            }
            default -> {
                return "str(" + varName + ")";
            }
        }
    }

    public String tableGet(TableSchema refTable, RefKey.RefSimple refSimple, String actualParam) {
        switch (refSimple) {
            case RefKey.RefPrimary ignored -> {
                return fullName(refTable) + ".find(" + actualParam + ")";
            }
            case RefKey.RefUniq refUniq -> {
                return fullName(refTable) + ".find_by_" + refUniq.keyNames().stream()
                        .map(StringUtil::lower1)
                        .collect(Collectors.joining("_")) + "(" + actualParam + ")";
            }
        }
    }

    public String fieldName(FieldSchema field) {
        return lower1(field.name());
    }

    public String primaryKeyFieldName() {
        if (structural instanceof TableSchema tableSchema) {
            return fieldName(tableSchema.primaryKey().fieldSchemas().getFirst());
        }
        return null;
    }

    public String keyType() {
        if (structural instanceof TableSchema tableSchema) {
            return type(tableSchema.primaryKey().fieldSchemas().getFirst().type());
        }
        return null;
    }

    public String dictionaryType(KeySchema keySchema, String valueType) {
        // 生成Dictionary类型声明，如 Dictionary[int, ClassName]
        if (keySchema.fieldSchemas().size() > 1) {
            throw new RuntimeException("gd script not support composite key");
        } else {
            String keyType = type(keySchema.fieldSchemas().getFirst().type());
            return "Dictionary[" + keyType + ", " + valueType + "]";
        }
    }

    public String actualParamsKeySelf(KeySchema keySchema) {
        // GDScript中，复合键使用Dictionary，单键直接返回值
        if (keySchema.fieldSchemas().size() > 1) {
            throw new RuntimeException("gd script not support composite key");
        } else {
            return lower1(keySchema.fieldSchemas().getFirst().name());
        }
    }
}
