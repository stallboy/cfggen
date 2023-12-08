package configgen.genjava.code;

import configgen.gen.Generator;
import configgen.schema.*;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.gen.Generator.lower1;
import static configgen.schema.RefKey.*;

class MethodStr {

    static String formalParams(List<FieldSchema> fs) {
        return fs.stream().map(f -> TypeStr.type(f.type()) + " " + lower1(f.name())).collect(Collectors.joining(", "));
    }

    static String actualParams(List<String> keys) {
        return keys.stream().map(Generator::lower1).collect(Collectors.joining(", "));
    }

    static String actualParamsKey(KeySchema keySchema, String pre, NameableName nullableName) {
        String p = actualParamsKeyRaw(keySchema, pre);
        return keySchema.fields().size() > 1 ? "new " + Name.keyClassName(keySchema, nullableName) + "(" + p + ")" : p;
    }

    static String actualParamsKeyRaw(KeySchema keySchema, String pre) {
        return keySchema.fields().stream().map(e -> pre + lower1(e)).collect(Collectors.joining(", "));
    }

    static String hashCodes(List<FieldSchema> fs) {
        String paramList = fs.stream().map(f -> lower1(f.name())).collect(Collectors.joining(", "));
        return String.format("java.util.Objects.hash(%s)", paramList) ;
    }

    static String equals(List<FieldSchema> fs) {
        return fs.stream().map(f -> equal(lower1(f.name()), "o." + lower1(f.name()), f.type())).collect(Collectors.joining(" && "));
    }

    static String equal(String a, String b, FieldType t) {
        return TypeStr.isJavaPrimitive(t) ? a + " == " + b : a + ".equals(" + b + ")";
    }

    static String tableGet(TableSchema refTable, RefSimple refSimple, String actualParam) {
        NameableName name = new NameableName(refTable);

        if (refTable.entry() instanceof EntryType.EEnum) {
            return name.fullName + ".get(" + actualParam + ");";
        } else {
            String pre = "mgr." + name.containerPrefix;
            switch (refSimple) {
                case RefPrimary ignored -> {
                    if (refTable.primaryKey().fieldSchemas().size() == 1) {
                        return pre + "All.get(" + actualParam + ");";
                    } else {
                        return pre + "All.get(new " + Name.keyClassName(refTable.primaryKey(), name) +
                                "(" + actualParam + ") );";
                    }
                }

                case RefUniq refUniq -> {
                    if (refUniq.key().fields().size() == 1) {
                        return pre + Name.uniqueKeyMapName(refUniq.key()) + ".get(" + actualParam + ");";
                    } else {
                        return pre + Name.uniqueKeyMapName(refUniq.key()) + ".get( new " +
                                Name.keyClassName(refUniq.key(), name) + "(" + actualParam + ") );";
                    }
                }
            }
        }
    }

}
