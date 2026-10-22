package configgen.gengo;

import configgen.naming.GenNaming;
import configgen.schema.*;
import configgen.util.StringUtil;
import configgen.value.CfgValue;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.FieldType.Primitive.TEXT;

public class StructModel {
    public final String pkg;
    public final GoName name;
    public final Structural structural;
    public final String className;
    public final CfgValue.VTable vTable;
    public final List<FieldSchema> reverseFields;
    private final GoCodeGenerator gen;

    public StructModel(GoCodeGenerator gen, String pkg, GoName name, Structural structural, CfgValue.VTable vTable) {
        this.gen = gen;
        this.pkg = pkg;
        this.name = name;
        this.structural = structural;
        this.className = name.className;
        this.vTable = vTable;
        this.reverseFields = vTable == null ? List.of() : gen.reverseFields(vTable.schema());
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

    /**
     * 字段的Go类型。TEXT按isLangSwitch映射（*Text/string），容器递归使用同一规则——
     * 读代码模板（GenStruct.jte）对list/map元素也按本方法生成，声明与读取必须同源，
     * 拆出第二个"忽略 langswitch "的类型函数曾导致 []string 声明 + []*Text 读取的自相矛盾代码。
     * 键类型为 BOOL/INT/LONG/STRING 或仅包含这些字段的扁平 struct，不会走到 TEXT 分支。
     */
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

    public String refType(ForeignKeySchema fk) {
        if (fk.pathReference() != null) return (fk.referenceResultType() instanceof InterfaceSchema ? "" : "*") + ClassName(fk.referenceResultType());
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
                        return String.format("map[%s]*%s", type(fMap.key()), ClassName(fk.refTableSchema()));
                    }
                }
            }
        }
    }

    public static String refName(ForeignKeySchema fk) {
        return GenNaming.refFieldName(fk);
    }

    public String refGetter(ForeignKeySchema fk) {
        return switch (fk.refKey()) {
            case RefKey.RefPrimary ignored -> "Get";
            case RefKey.RefUniq unique -> GetFuncName(unique.key(), false);
            case RefKey.RefList list -> "GetAllBy" + StringUtil.upper1(list.key().fields().getFirst());
        };
    }

    public String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return "Key" + GenNaming.keyFieldsPascalName(keySchema);
        else if (structKey(keySchema) != null)
            return "key" + className + StringUtil.upper1(keySchema.fields().getFirst());
        else return type(keySchema.fieldSchemas().getFirst().type());
    }

    public static StructSchema structKey(KeySchema keySchema) {
        return keySchema.fieldSchemas().size() == 1
                && keySchema.fieldSchemas().getFirst().type() instanceof StructRef ref
                ? (StructSchema) ref.obj() : null;
    }

    public String keyExpression(KeySchema keySchema, String prefix) {
        String fields = keySchema.fieldSchemas().stream()
                .map(f -> prefix + StringUtil.lower1(f.name())).collect(Collectors.joining(", "));
        StructSchema struct = structKey(keySchema);
        if (struct != null) {
            String key = fields;
            fields = struct.fields().stream().map(f -> key + "." + StringUtil.lower1(f.name()))
                    .collect(Collectors.joining(", "));
        }
        return keySchema.fieldSchemas().size() > 1 || struct != null
                ? keyClassName(keySchema) + "{" + fields + "}" : fields;
    }

    public String reverseKeyType(FieldSchema field) {
        if (field.type() instanceof StructRef ref && KeySchema.isKeyType(field.type())) {
            return "struct { " + ((StructSchema) ref.obj()).fields().stream()
                    .map(f -> StringUtil.lower1(f.name()) + " " + type(f.type()))
                    .collect(Collectors.joining("; ")) + " }";
        }
        return type(field.type());
    }

    public String reverseKeyExpression(FieldSchema field, String value) {
        if (field.type() instanceof StructRef ref && KeySchema.isKeyType(field.type())) {
            return reverseKeyType(field) + "{" + ((StructSchema) ref.obj()).fields().stream()
                    .map(f -> value + "." + StringUtil.lower1(f.name())).collect(Collectors.joining(", ")) + "}";
        }
        return value;
    }

    public static String mapName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1) {
            return StringUtil.lower1(GenNaming.keyFieldsPascalName(keySchema));
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

    public String GetVarDefines(KeySchema keySchema) {
        return keySchema.fieldSchemas().stream()
                .map(f -> StringUtil.lower1(f.name()) + " " + type(f.type()))
                .collect(Collectors.joining(", "));
    }

    public String GetFuncName(KeySchema keySchema, boolean refPrimary) {
        var fieldSchemas = keySchema.fieldSchemas();
        var fieldCnt = fieldSchemas.size();
        if (refPrimary) {
            return "Get";
        } else if (fieldCnt > 1) {
            return "GetBy" + keyClassName(keySchema);
        } else {
            return "GetBy" + GetParamVars(keySchema);
        }
    }

    public boolean hasRelations() { return gen.hasRelations; }
}
