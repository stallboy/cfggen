package configgen.gengo;

import configgen.schema.*;
import configgen.util.StringUtil;
import configgen.value.CfgValue;

import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.FieldType.Primitive.TEXT;

public class StructModel {
    public final String pkg;
    public final GoName name;
    public final Structural structural;
    public final String className;
    public final CfgValue.VTable vTable;
    private final GoCodeGenerator gen;

    public StructModel(GoCodeGenerator gen, String pkg, GoName name, Structural structural, CfgValue.VTable vTable) {
        this.gen = gen;
        this.pkg = pkg;
        this.name = name;
        this.structural = structural;
        this.className = name.className;
        this.vTable = vTable;
    }

    public boolean isLangSwitch() {
        return gen.isLangSwitch;
    }

    public String genReadField(FieldType t) {
        return switch (t) {
            case BOOL -> "stream.ReadBool()";
            case INT -> "stream.ReadInt32()";
            case LONG -> "stream.ReadInt64()";
            case FLOAT -> "stream.ReadFloat32()";
            case STRING -> "stream.ReadStringInPool()";
            case TEXT -> gen.isLangSwitch ? "createText(stream)" : "stream.ReadTextInPool()";
            case StructRef structRef -> String.format("create%s(stream)", ClassName(structRef.obj()));
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }

    public String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int32";
            case LONG -> "int64";
            case FLOAT -> "float32";
            case STRING -> "string";
            case TEXT -> gen.isLangSwitch ? "*Text" : "string";
            case StructRef structRef -> {
                Fieldable fieldable = structRef.obj();
                yield switch (fieldable) {
                    case StructSchema ignored -> "*" + ClassName(fieldable);
                    case InterfaceSchema ignored -> ClassName(fieldable);
                };
            }
            case FList fList -> "[]" + type(fList.item());
            case FMap fMap -> String.format("map[%s]%s", type(fMap.key()), type(fMap.value()));
        };
    }

    public static String ClassName(Nameable variable) {
        var varName = new GoName(variable);
        return varName.className;
    }

    public static String refType(ForeignKeySchema fk) {
        GoName refTableName = new GoName(fk.refTableSchema());
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "[]*" + ClassName(fk.refTableSchema());
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return "*" + refTableName.className;
                    }
                    case FList ignored2 -> {
                        return "[]*" + ClassName(fk.refTableSchema());
                    }
                    case FMap fMap -> {
                        return String.format("map[%s]*%s", GoCodeGenerator.type(fMap.key()), ClassName(fk.refTableSchema()));
                    }
                }
            }
        }
    }

    public static String refName(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "ListRef" + upper1(fk.name());
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

    public static String upper1(String value) {
        return StringUtil.upper1(value);
    }

    public static String lower1(String value) {
        return StringUtil.lower1(value);
    }

    public static String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return "Key" + keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining());
        else return GoCodeGenerator.type(keySchema.fieldSchemas().getFirst().type());
    }

    public static String mapName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1) {
            return StringUtil.lower1(keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()));
        } else {
            return StringUtil.lower1(keySchema.fields().getFirst());
        }
    }

    public static String GetParamVars(KeySchema keySchema) {
        return keySchema.fieldSchemas().stream()
                .map(f -> StringUtil.lower1(f.name()))
                .collect(Collectors.joining(", "));
    }

    public static String GetParamVarsInV(KeySchema keySchema, String tempVarName) {
        return keySchema.fieldSchemas().stream()
                .map(f -> tempVarName + "." + StringUtil.lower1(f.name()))
                .collect(Collectors.joining(", "));
    }

    public static String GetVarDefines(KeySchema keySchema) {
        return keySchema.fieldSchemas().stream()
                .map(f -> StringUtil.lower1(f.name()) + " " + GoCodeGenerator.type(f.type()))
                .collect(Collectors.joining(", "));
    }

    public static String GetFuncName(KeySchema keySchema, boolean refPrimary) {
        var fieldSchemas = keySchema.fieldSchemas();
        var fieldCnt = fieldSchemas.size();
        if (refPrimary) {
            return "Get";
        } else if (fieldCnt > 1) {
            return "GetBy" + GoCodeGenerator.keyClassName(keySchema);
        } else {
            return "GetBy" + GetParamVars(keySchema);
        }
    }

    // 生成字段在 String() 方法中的打印格式
    public static String toStringField(FieldSchema f) {
        String fieldName = lower1(f.name());
        FieldType t = f.type();
        if (t instanceof FList) {
            return String.format("fmt.Sprintf(\"%%v\", t.%s)", fieldName);
        } else if (t instanceof FMap) {
            return String.format("fmt.Sprintf(\"%%v\", t.%s)", fieldName);
        } else if (t instanceof StructRef) {
            return String.format("fmt.Sprintf(\"%%v\", t.%s)", fieldName);
        } else {
            return "t." + fieldName;
        }
    }
}
