package configgen.gents;

import configgen.naming.GenNaming;
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
            case INT, FLOAT -> "number";
            case LONG -> gen.hasRelations() ? "bigint" : "number";
            case STRING -> "string";
            case TEXT -> gen.nullableLanguageSwitch != null ? "Text" : "string";
            case StructRef structRef -> asMapKey && !gen.hasRelations() ? "number" : className(structRef.obj());
            case FList fList -> type(fList.item()) + "[]";
            case FMap fMap -> "Map<" + _type(fMap.key(), true) + ", " + type(fMap.value()) + ">";
        };
    }

    public String mapKeyType(KeySchema keySchema) {
        if (gen.hasRelations() && (keySchema.fields().size() > 1 || keySchema.fieldSchemas().getFirst().type() instanceof StructRef)) return "string";
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
            case LONG -> gen.hasRelations() ? "os.ReadInt64BigInt()" : "os.ReadInt64()";
            case FLOAT -> "os.ReadSingle()";
            case STRING -> "os.ReadStringInPool()";
            case TEXT -> gen.nullableLanguageSwitch != null ? "Text._create(os)" : "os.ReadTextInPool()";
            case StructRef structRef -> className(structRef.obj()) + "._create(os)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }


    public String refType(ForeignKeySchema fk) {
        if (fk.pathReference() != null) return className(fk.referenceResultType());
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
        return GenNaming.refFieldName(fk);
    }

    // 判断外键是否为 nullable
    public boolean isNullableRef(ForeignKeySchema fk) {
        return fk.refKey() instanceof RefKey.RefSimple refSimple && refSimple.nullable();
    }

    public String uniqueKeyGetByName(KeySchema keySchema) {
        return GenNaming.uniqueKeyGetByName(keySchema);
    }

    public String uniqueKeyMapName(KeySchema keySchema) {
        return lower1(GenNaming.uniqueKeyMapName(keySchema));
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
        if (gen.hasRelations()) return keySchema.fields().stream().map(f -> "this._" + f).collect(Collectors.joining(", "));
        int count = keySchema.fields().size();
        if (count == 1) {
            return "this._" + keySchema.fields().getFirst();
        } else if (count == 2) {
            String k = keySchema.fields().getFirst();
            String j = keySchema.fields().get(1);
            return String.format("this._%s, this._%s", k, j);
        } else {
            throw new RuntimeException("generate typescript, multi key not support > 2 count");
        }
    }

    private String actualParamsKey(KeySchema keySchema, String prefix) {
        if (gen.hasRelations() && (keySchema.fields().size() > 1 || keySchema.fieldSchemas().getFirst().type() instanceof StructRef)) {
            return "$relationKey$([" + keySchema.fields().stream().map(f -> prefix + f).collect(Collectors.joining(", ")) + "])";
        }
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
        return fs.stream()
                .map(this::fieldToStringExpr)
                .collect(Collectors.joining(" + \",\" + "));
    }

    private String fieldToStringExpr(FieldSchema f) {
        String fieldName = "this._" + f.name();
        return switch (f.type()) {
            case FList ignored -> "ToStringList(" + fieldName + ")";
            case FMap ignored -> "ToStringMap(" + fieldName + ")";
            case StructRef ignored -> "this." + upper1(f.name()) + ".toString()";
            default -> fieldName;
        };
    }


    public String tableGet(TableSchema refTable, RefKey.RefSimple refSimple, String actualParam) {
        switch (refSimple) {
            case RefKey.RefPrimary ignored -> {
                return className(refTable) + ".Get(" + actualParam + ")";
            }
            case RefKey.RefUniq refUniq -> {
                return className(refTable) + "." + GenNaming.uniqueKeyGetByName(refUniq.keyNames())
                        + "(" + actualParam + ")";
            }
        }
    }

    public boolean hasRelations() { return gen.hasRelations(); }
}
