package configgen.gents;

import configgen.gen.Generator;
import configgen.schema.*;
import configgen.util.StringUtil;
import configgen.value.CfgValue;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;

public class StructModel {
    public final Structural structural;
    public final CfgValue.VTable _vTable;
    public final String structClassName;
    private final TsCodeGenerator gen;

    public StructModel(TsCodeGenerator gen, Structural structural, CfgValue.VTable _vTable) {
        this.gen = gen;
        this.structural = structural;
        this._vTable = _vTable;
        this.structClassName = gen.className(structural);
    }

    public String className(Nameable nameable) {
        return gen.className(nameable);
    }

    public String upper1(String value) {
        return StringUtil.upper1(value);
    }

    public String lower1(String value) {
        return StringUtil.lower1(value);
    }

    public String type(FieldType t) {
        return _type(t, false);
    }

    private String _type(FieldType t, boolean asMapKey) {
        return switch (t) {
            case BOOL -> "boolean";
            case INT, LONG, FLOAT -> "number";
            case STRING -> "string";
            case TEXT -> gen.nullableLanguageSwitch != null ? "Text" : "string";
            case StructRef structRef -> asMapKey ? "number" : className(structRef.obj());
            case FList fList -> type(fList.item()) + "[]";
            case FMap fMap -> "Map<" + _type(fMap.key(), true) + ", " + type(fMap.value()) + ">";
        };
    }

    public String mapKeyType(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1) {
            return "number"; // 跟lua一样的约定，最多2个int，组装成一个大int
        } else {
            return _type(keySchema.fieldSchemas().getFirst().type(), true);
        }
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
                        return "Map<" + _type(fMap.key(), true) + ", " + className(fk.refTableSchema()) + ">";
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
        return "GetBy" + keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining());
    }

    public String uniqueKeyMapName(KeySchema keySchema) {
        return lower1(keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()) + "Map");
    }


    public String formalParams(List<FieldSchema> fs) {
        return fs.stream().map(f -> String.format("%s: %s", f.name(), type(f.type())))
                .collect(Collectors.joining(", "));
    }

    public String actualParamsKey(KeySchema keySchema) {
        return actualParamsKey(keySchema, "");
    }

    public String actualParamsKeySelf(KeySchema keySchema) {
        return actualParamsKey(keySchema, "self._");
    }

    public String actualParamsKeyThis(KeySchema keySchema) {
        int count = keySchema.fields().size();
        if (count == 1) {
            return "this._" + keySchema.fields().getFirst();
        } else if (count == 2) {
            String k = keySchema.fields().getFirst();
            String j = keySchema.fields().get(1);
            return String.format("this._%s, this.%s", k, j);
        } else {
            throw new RuntimeException("generate typescript, multi key not support > 2 count");
        }
    }

    private String actualParamsKey(KeySchema keySchema, String prefix) {
//        --- 2个字段可以做为uniqkey，但都必须是数字，并且第一个<1亿，第二个<1万
//        self[getname] = function(k, j)
//            return map[k + j * 100000000]
//        end
        int count = keySchema.fields().size();
        if (count == 1) {
            FieldSchema first = keySchema.fieldSchemas().getFirst();
            if (first.type() instanceof Primitive) {
                return prefix + keySchema.fields().getFirst();
            }
            if (first.type() instanceof StructRef structRef && structRef.obj() instanceof StructSchema structSchema &&
                    structSchema.fields().size() == 2) {
                String cur = prefix + keySchema.fields().getFirst();
                String k = structSchema.fields().getFirst().name();
                String j = structSchema.fields().get(1).name();
                return String.format("%s.%s + %s.%s * 100000000", cur, upper1(k), cur, upper1(j));
            }
            throw new RuntimeException("generate typescript, struct key not support > 2 fields");
        } else if (count == 2) {
            String k = keySchema.fields().getFirst();
            String j = keySchema.fields().get(1);
            return String.format("%s%s + %s%s * 100000000", prefix, k, prefix, j);
        } else {
            throw new RuntimeException("generate typescript, multi key not support > 2 count");
        }
    }

    public String toStrings(List<FieldSchema> fs) {
        return fs.stream().map(f -> "this._" + f.name()).collect(Collectors.joining(" + \",\" + "));
    }


    public String tableGet(TableSchema refTable, RefKey.RefSimple refSimple, String actualParam) {
        switch (refSimple) {
            case RefKey.RefPrimary ignored -> {
                return className(refTable) + ".Get(" + actualParam + ")";
            }
            case RefKey.RefUniq refUniq -> {
                return className(refTable) + ".GetBy" + refUniq.keyNames().stream().map(StringUtil::upper1).
                        collect(Collectors.joining()) + "(" + actualParam + ")";
            }
        }
    }
}
