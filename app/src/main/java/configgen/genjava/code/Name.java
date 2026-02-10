package configgen.genjava.code;

import configgen.gen.Generator;
import configgen.genjava.GenJavaUtil;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import static configgen.util.StringUtil.upper1;
import static configgen.schema.FieldType.*;

public class Name {

    static String codeTopPkg;

    public static String GetByKeyFunctionNameInConfigMgr(KeySchema keySchema, boolean isPrimaryKey, Nameable nameable) {
        String name = "get" + Arrays.stream(nameable.name().split("\\.")).map(StringUtil::upper1).collect(Collectors.joining());

        if (isPrimaryKey){
            return name;
        }
        return name + "By" + keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining());
    }

    public static String GetByKeyFunctionName(KeySchema keySchema, boolean isPrimaryKey) {
        if (isPrimaryKey){
            return "get";
        }
        return "getBy" + keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining());
    }

    public static String uniqueKeyMapName(KeySchema keySchema) {
        return keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()) + "Map";
    }

    public static String keyClassName(KeySchema keySchema){
        return keyClassName(keySchema, null);
    }

    public static String keyClassName(KeySchema keySchema, NameableName nullableName) {
        if (keySchema.fields().size() > 1) {
            String klsName = keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()) + "Key";
            if (nullableName != null) {
                return nullableName.fullName + "." + klsName;
            } else {
                return klsName;
            }

        } else {
            try {
                return TypeStr.boxType(keySchema.fieldSchemas().getFirst().type());
            } catch (Exception e) {
                return null;
            }
        }
    }


    public static String fullName(Nameable nameable) {
        return new NameableName(nameable).fullName;
    }

    public static String tableDataFullName(TableSchema table) {
        String fn = fullName(table);
        if (table.entry() instanceof EntryType.EEnum && !GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(table)) {
            fn = fn + "_Detail";
        }
        return fn;
    }


    public static String refType(TableSchema table) {
        return new NameableName(table).fullName;
    }

    public static String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "java.util.List<" + refType(fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {

                    case SimpleType ignored2 -> {
                        return refType(fk.refTableSchema());
                    }
                    case FList ignored2 -> {
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

    public static String refName(ForeignKeySchema fk) {
        String prefix = switch (fk.refKey()) {
            case RefKey.RefList ignored -> "ListRef";
            case RefKey.RefSimple refSimple -> refSimple.nullable() ? "NullableRef" : "Ref";
        };
        return prefix + upper1(fk.name());
    }

}
