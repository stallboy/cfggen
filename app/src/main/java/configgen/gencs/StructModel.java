package configgen.gencs;

import configgen.gen.Generator;
import configgen.schema.*;
import configgen.value.CfgValue;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;

public class StructModel {
    public final String topPkg;
    public final Name name;
    public final Structural structural;
    public final CfgValue.VTable _vTable;
    private final GenCs gen;

    public StructModel(GenCs gen, Structural structural, CfgValue.VTable _vTable) {
        this.gen = gen;
        this.topPkg = gen.pkg;
        this.name = new Name(gen.pkg, gen.prefix, structural);
        this.structural = structural;
        this._vTable = _vTable;
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.pkg, gen.prefix, nameable).fullName;
    }

    public String upper1(String value) {
        return GenCs.upper1(value);
    }

    public String lower1(String value) {
        return GenCs.lower1(value);
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
            case FMap fMap -> "KeyedList<" + type(fMap.key()) + ", " + type(fMap.value()) + ">";
        };
    }

    public String create(FieldType t) {
        return switch (t) {
            case BOOL -> "os.ReadBool()";
            case INT -> "os.ReadInt32()";
            case LONG -> "os.ReadInt64()";
            case FLOAT -> "os.ReadSingle()";
            case STRING -> "os.ReadString()";
            case TEXT -> gen.isLangSwitch ? topPkg + ".Text._create(os)" : "os.ReadString()";
            case StructRef structRef -> fullName(structRef.obj()) + "._create(os)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }


    public String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "List<" + fullName(fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return fullName(fk.refTableSchema());
                    }
                    case FList ignored2 -> {
                        return "List<" + fullName(fk.refTableSchema()) + ">";
                    }
                    case FMap fMap -> {
                        return "KeyedList<" + type(fMap.key()) + ", " + fullName(fk.refTableSchema()) + ">";
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

    public String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()) + "Key";
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
        String p = keySchema.fields().stream().map(Generator::lower1).collect(Collectors.joining(", "));
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
            return "CSV.ToString(" + upper1(n) + ")";
        else
            return upper1(n);
    }

    public String tableGet(TableSchema refTable, RefKey.RefSimple refSimple, String actualParam) {
        switch (refSimple) {
            case RefKey.RefPrimary ignored -> {
                return fullName(refTable) + ".Get(" + actualParam + ");";
            }
            case RefKey.RefUniq refUniq -> {
                return fullName(refTable) + ".GetBy" + refUniq.keyNames().stream().map(Generator::upper1).
                        collect(Collectors.joining()) + "(" + actualParam + ");";
            }
        }
    }
}
