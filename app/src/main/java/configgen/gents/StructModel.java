package configgen.gents;

import configgen.gen.Generator;
import configgen.schema.*;
import configgen.value.CfgValue;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;

public class StructModel {
    public final Structural structural;
    public final CfgValue.VTable _vTable;
    public final String structClassName;
    private final GenTs gen;

    public StructModel(GenTs gen, Structural structural, CfgValue.VTable _vTable) {
        this.gen = gen;
        this.structural = structural;
        this._vTable = _vTable;
        this.structClassName = gen.className(structural);
    }

    public String className(Nameable nameable) {
        return gen.className(nameable);
    }

    public String upper1(String value) {
        return Generator.upper1(value);
    }

    public String lower1(String value) {
        return Generator.lower1(value);
    }

    public String type(FieldType t) {
        return switch (t) {
            case BOOL -> "boolean";
            case INT, LONG, FLOAT -> "number";
            case STRING -> "string";
            case TEXT -> gen.nullableLanguageSwitch != null ? "Text" : "string";
            case StructRef structRef -> className(structRef.obj());
            case FList fList -> type(fList.item()) + "[]";
            case FMap fMap -> "Map<" + type(fMap.key()) + ", " + type(fMap.value()) + ">";
        };
    }

    public String create(FieldType t) {
        return switch (t) {
            case BOOL -> "os.ReadBool()";
            case INT -> "os.ReadInt32()";
            case LONG -> "os.ReadInt64()";
            case FLOAT -> "os.ReadSingle()";
            case STRING -> "os.ReadString()";
            case TEXT -> gen.nullableLanguageSwitch != null ? "Text._create(os)" : "os.ReadString()";
            case StructRef structRef -> className(structRef.obj()) + "._create(os)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }


    public String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return className(fk.refTableSchema()) + "[]";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return className(fk.refTableSchema());
                    }
                    case FList ignored2 -> {
                        return className(fk.refTableSchema()) + "[]";
                    }
                    case FMap fMap -> {
                        return "Map<" + type(fMap.key()) + ", " + className(fk.refTableSchema()) + ">";
                    }
                }
            }
        }
    }

    public String refName(ForeignKeySchema fk) {
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


    public String uniqueKeyGetByName(KeySchema keySchema) {
        return "GetBy" + keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining());
    }

    public String uniqueKeyMapName(KeySchema keySchema) {
        return lower1(keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()) + "Map");
    }

    public String mapKeyType(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return "number"; // 跟lua一样的约定，最多2个int，组装成一个大int
        else
            return type(keySchema.fieldSchemas().getFirst().type());
    }

    public String formalParams(List<FieldSchema> fs) {
        return fs.stream().map(f -> type(f.type()) + " " + lower1(f.name())).collect(Collectors.joining(", "));
    }

    public String actualParams(KeySchema keySchema) {
        return keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining(", "));
    }

    public String actualParamsKey(KeySchema keySchema) {
//        --- 2个字段可以做为uniqkey，但都必须是数字，并且第一个<1亿，第二个<1万
//        self[getname] = function(k, j)
//            return map[k + j * 100000000]
//        end
        int count = keySchema.fields().size();
        if (count == 1) {
            return Generator.lower1(keySchema.fields().getFirst());
        } else if (count == 2) {
            String k = keySchema.fields().getFirst();
            String j = keySchema.fields().get(1);
            return String.format("%s + %s * 100000000", k, j);
        } else {
            throw new RuntimeException("generate typescript, multi key not support > 2 count");
        }
    }

    public String actualParamsKeySelf(KeySchema keySchema) {
        String p = keySchema.fields().stream().map(n -> "self." + upper1(n)).collect(Collectors.joining(", "));
        return keySchema.fields().size() > 1 ? "new " + mapKeyType(keySchema) + "(" + p + ")" : p;
    }

    public String tableGet(TableSchema refTable, RefKey.RefSimple refSimple, String actualParam) {
        switch (refSimple) {
            case RefKey.RefPrimary ignored -> {
                return className(refTable) + ".Get(" + actualParam + ");";
            }
            case RefKey.RefUniq refUniq -> {
                return className(refTable) + ".GetBy" + refUniq.keyNames().stream().map(Generator::upper1).
                        collect(Collectors.joining()) + "(" + actualParam + ");";
            }
        }
    }
}
