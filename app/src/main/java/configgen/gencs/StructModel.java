package configgen.gencs;

import configgen.schema.*;
import configgen.util.StringUtil;
import configgen.value.CfgValue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;

public class StructModel {
    public final String topPkg;
    public final Name name;
    public final Structural structural;
    public final CfgValue.VTable _vTable;
    private final CsCodeGenerator gen;

    public StructModel(CsCodeGenerator gen, Structural structural, CfgValue.VTable _vTable) {
        this.gen = gen;
        this.topPkg = gen.pkg;
        this.name = new Name(gen.pkg, gen.prefix, structural);
        this.structural = structural;
        this._vTable = _vTable;
    }

    public boolean isEnum() {
        return _vTable != null && _vTable.schema().entry() instanceof EntryType.EEnum;
    }

    public static boolean isEnum(TableSchema tableSchema) {
        return tableSchema.entry() instanceof EntryType.EEnum;
    }

    public boolean hasEntry() {
        return _vTable != null && _vTable.schema().entry() instanceof EntryType.EEntry
                && !_vTable.enumNames().isEmpty();
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.pkg, gen.prefix, nameable).fullName;
    }

    public String upper1(String value) {
        return StringUtil.upper1(value);
    }

    public String lower1(String value) {
        return StringUtil.lower1(value);
    }

    private static final Set<String> reserved = Set.of(
            "object", "string", "event", "params", "ref", "base", "namespace", "class", "struct");

    public static String _lower1(String value) {
        String v = StringUtil.lower1(value);
        if (reserved.contains(v)) {
            return "_" + v;
        } else {
            return v;
        }
    }

    public String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case STRING -> "string";
            case TEXT -> gen.isLangSwitch ? topPkg + ".Text" : "string";
            case StructRef structRef -> fullName(structRef.obj());
            case FList fList -> "List<" + type(fList.item()) + ">";
            case FMap fMap -> "OrderedDictionary<" + type(fMap.key()) + ", " + type(fMap.value()) + ">";
        };
    }

    public String toStringOrNot(FieldType t) {
        return switch (t) {
            case STRING, TEXT -> "";
            default -> ".ToString()";
        };
    }

    public String create(FieldType t) {
        return switch (t) {
            case BOOL -> "reader.ReadBool()";
            case INT -> "reader.ReadInt32()";
            case LONG -> "reader.ReadInt64()";
            case FLOAT -> "reader.ReadSingle()";
            case STRING -> "reader.ReadStringInPool()";
            case TEXT -> gen.isLangSwitch ? topPkg + ".Text._create(reader)" : "reader.ReadTextInPool()";
            case StructRef structRef -> fullName(structRef.obj()) + "._create(reader)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }


    public String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "List<" + fullName(fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple rs -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored -> {
                        return fullName(fk.refTableSchema()) + (rs.nullable() ? "?" : "");
                    }
                    case FList ignored2 -> {
                        return "List<" + fullName(fk.refTableSchema()) + ">";
                    }
                    case FMap fMap -> {
                        return "OrderedDictionary<" + type(fMap.key()) + ", " + fullName(fk.refTableSchema()) + ">";
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
        return "_" + lower1(keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()) + "Map");
    }

    public String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()) + "Key";
        else
            return type(keySchema.fieldSchemas().getFirst().type());
    }

    public String formalParams(List<FieldSchema> fs) {
        return fs.stream().map(f -> type(f.type()) + " " + _lower1(f.name())).collect(Collectors.joining(", "));
    }

    public String actualParams(KeySchema keySchema) {
        return keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining(", "));
    }

    public String actualParamsKey(KeySchema keySchema) {
        String p = keySchema.fields().stream().map(StructModel::_lower1).collect(Collectors.joining(", "));
        return keySchema.fields().size() > 1 ? "new " + keyClassName(keySchema) + "(" + p + ")" : p;
    }

    public String actualParamsKeySelf(KeySchema keySchema) {
        String p = keySchema.fields().stream().map(n -> "self." + upper1(n)).collect(Collectors.joining(", "));
        return keySchema.fields().size() > 1 ? "new " + keyClassName(keySchema) + "(" + p + ")" : p;
    }

    public String equals(List<FieldSchema> fs) {
        return fs.stream().map(f -> upper1(f.name()) + ".Equals(o." + upper1((f.name())) + ")").collect(Collectors.joining(" && "));
    }

    public String hashCodes(List<FieldSchema> fs) {
        return fs.stream().map(f -> upper1(f.name()) + ".GetHashCode()").collect(Collectors.joining(" + "));
    }

    public String toStrings(List<FieldSchema> fs) {
        return fs.stream().map(f -> toString(f.name(), f.type())).collect(Collectors.joining(" + \",\" + "));
    }

    public String toString(String n, FieldType t) {
        if (t instanceof FList)
            return "StringUtil.ToString(" + upper1(n) + ")";
        else
            return upper1(n);
    }

    public String tableGet(TableSchema refTable, RefKey.RefSimple refSimple, String actualParam) {
        boolean isEnum = refTable.entry() instanceof EntryType.EEnum;
        String post = isEnum ? "Info" : "";
        return switch (refSimple) {
            case RefKey.RefPrimary ignored -> fullName(refTable) + post + ".Get(" + actualParam + ")";

            case RefKey.RefUniq refUniq ->
                    fullName(refTable) + post + ".GetBy" + refUniq.keyNames().stream().map(StringUtil::upper1).
                            collect(Collectors.joining()) + "(" + actualParam + ")";
        };
    }

    public String dictValueType() {
        return name.className + (isEnum() ? "Info" : "");
    }

    public String dictType(KeySchema keySchema) {
        return "System.Collections.Frozen.FrozenDictionary<" + keyClassName(keySchema) + ", " + dictValueType() + ">";
    }

    public String dictTypeWhenInit(KeySchema keySchema) {
        return "Dictionary<" + keyClassName(keySchema) + ", " + dictValueType() + ">";
    }

    public static String refInit(ForeignKeySchema fk) {
        if (fk.refKey() instanceof RefKey.RefSimple refSimple) {
            if (refSimple.nullable()) {
                return "";
            }
            boolean isContainer = false;
            for (FieldSchema fs : fk.key().fieldSchemas()) {
                if (fs.type() instanceof ContainerType) {
                    isContainer = true;
                    break;
                }
            }
            if (!isContainer && fk.refTableSchema().entry() instanceof EntryType.EEnum) {
                return "";
            }
        }

        return " = null!;";
    }

}
