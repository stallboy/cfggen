package configgen.genjava.code;

import configgen.gen.Generator;
import configgen.genjava.GenJavaUtil;
import configgen.schema.*;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.*;

public class Name {

    static String codeTopPkg;

    static String uniqueKeyGetByName(KeySchema keySchema) {
        return "getBy" + keySchema.name().stream().map(Generator::upper1).collect(Collectors.joining());
    }

    static String uniqueKeyMapName(KeySchema keySchema) {
        return keySchema.name().stream().map(Generator::upper1).collect(Collectors.joining()) + "Map";
    }

    static String keyClassName(KeySchema keySchema) {
        if (keySchema.name().size() > 1)
            return keySchema.name().stream().map(Generator::upper1).collect(Collectors.joining()) + "Key";
        else
            try {
                return TypeStr.boxType(keySchema.obj().get(0).type());
            } catch (Exception e) {
                return null;
            }
    }

    static String multiKeyClassName(List<String> keys) {
        return keys.stream().map(Generator::upper1).collect(Collectors.joining()) + "Key";
    }


    static String fullName(Nameable nameable) {
        InterfaceSchema sInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
        return new NameableName(nameable, sInterface).fullName;
    }

    static String tableDataFullName(TableSchema table) {
        String fn = fullName(table);
        if (table.entry() instanceof EntryType.EEnum && !GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(table)) {
            fn = fn + "_Detail";
        }
        return fn;
    }


    static String refType(TableSchema table) {
        return new NameableName(table).fullName;
    }

    static String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList _ -> {
                return "java.util.List<" + refType(fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple _ -> {
                FieldSchema firstLocal = fk.key().obj().get(0);
                switch (firstLocal.type()) {

                    case SimpleType _ -> {
                        return refType(fk.refTableSchema());
                    }
                    case FList _ -> {
                        return "java.util.List<" + refType(fk.refTableSchema()) + ">";
                    }
                    case FMap fMap -> {
                        return "java.util.Map<"
                                + (TypeStr.boxType((fMap.key()))) + ", "
                                + refType(fk.refTableSchema()) + ">";
                    }
                }
            }
        }
    }

    static String refName(ForeignKeySchema fk) {
        String prefix = switch (fk.refKey()) {
            case RefKey.RefList _ -> "ListRef";
            case RefKey.RefSimple refSimple -> refSimple.nullable() ? "NullableRef" : "Ref";
        };
        return prefix + Generator.upper1(fk.name());
    }

}
