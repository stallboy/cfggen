package configgen.genlua;

import configgen.gen.Generator;
import configgen.schema.*;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.gen.Generator.upper1;

public class Name {

    // primary key的容器名称是“all”， 函数名称是"get"
    static final String primaryKeyMapName = "all";
    static final String primaryKeyGetName = "get";


    static String uniqueKeyGetByName(KeySchema key) {
        return "getBy" + key.fields().stream().map(Generator::upper1).collect(Collectors.joining());
    }

    static String uniqueKeyMapName(KeySchema key) {
        return key.fields().stream().map(Generator::upper1).collect(Collectors.joining()) + "Map";
    }

    static String uniqueKeyGetByName(List<String> keyFields) {
        if (keyFields.isEmpty()) //ref to primary key
            return "get";
        else
            return "getBy" + keyFields.stream().map(Generator::upper1).collect(Collectors.joining());
    }


    static String refName(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList _ -> {
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

    static String fullName(Nameable nameable) {
        switch (nameable) {
            case InterfaceSchema _ -> {
                return "Beans." + nameable.name().toLowerCase();
            }
            case StructSchema struct -> {
                InterfaceSchema interfaceSchema = struct.nullableInterface();
                if (interfaceSchema != null) {
                    return "Beans." + interfaceSchema.name().toLowerCase() + "." + struct.name().toLowerCase();
                } else {
                    return "Beans." + nameable.name().toLowerCase();
                }
            }
            case TableSchema table -> {
                return AContext.getInstance().getPkgPrefixStr() + table.name().toLowerCase();
            }
        }
    }

    static String tablePath(String tableName) {
        return tableName.replace('.', '/').toLowerCase() + ".lua";
    }

    static String tableExtraPath(String tableName, int extraIndex) {
        return tableName.replace('.', '/').toLowerCase() + "_" + extraIndex + ".lua";
    }
}
