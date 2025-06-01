package configgen.gengo.model;

import configgen.gen.Generator;
import configgen.gencs.GenCs;
import configgen.gengo.GenGo;
import configgen.gengo.GoName;
import configgen.schema.*;
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

    public StructModel(String pkg, GoName name, Structural structural, CfgValue.VTable vTable) {
        this.pkg = pkg;
        this.name = name;
        this.structural = structural;
        this.className = name.className;
        this.vTable = vTable;
    }


    public static String genReadField(FieldType t) {
        return switch (t) {
            case BOOL -> "stream.ReadBool()";
            case INT -> "stream.ReadInt32()";
            case LONG -> "stream.ReadInt64()";
            case FLOAT -> "stream.ReadFloat32()";
            case STRING, TEXT -> "stream.ReadString()";
            case StructRef structRef -> String.format("create%s(stream)", ClassName(structRef.obj()));
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }

    public static String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int32";
            case LONG -> "int64";
            case FLOAT -> "float32";
            case STRING -> "string";
            case TEXT -> "string";
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
                        return String.format("map[%s]*%s", type(fMap.key()), ClassName(fk.refTableSchema()));
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
        return GenGo.upper1(value);
    }

    public static String lower1(String value) {
        return GenGo.lower1(value);
    }

    public static String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return "Key" + keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining());
        else return type(keySchema.fieldSchemas().getFirst().type());
    }

    public static String mapName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1) {
            return Generator.lower1(keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()));
        } else {
            return Generator.lower1(keySchema.fields().getFirst());
        }
    }

    public static String GetParamVars(KeySchema keySchema) {
        return keySchema.fieldSchemas().stream()
                .map(f -> Generator.lower1(f.name()))
                .collect(Collectors.joining(", "));
    }

    public static String GetParamVarsInV(KeySchema keySchema, String tempVarName) {
        return keySchema.fieldSchemas().stream()
                .map(f -> tempVarName + "." + Generator.lower1(f.name()))
                .collect(Collectors.joining(", "));
    }

    public static String GetVarDefines(KeySchema keySchema) {
        return keySchema.fieldSchemas().stream()
                .map(f -> Generator.lower1(f.name()) + " " + GenGo.type(f.type()))
                .collect(Collectors.joining(", "));
    }

    public static String GetFuncName(KeySchema keySchema, boolean refPrimary) {
        var fieldSchemas = keySchema.fieldSchemas();
        var fieldCnt = fieldSchemas.size();
        if (refPrimary) {
            return "Get";
        } else if (fieldCnt > 1) {
            return "GetBy" + GenGo.keyClassName(keySchema);
        } else {
            return "GetBy" + GetParamVars(keySchema);
        }
    }
}
