package configgen.genlua;

import configgen.gen.Generator;
import configgen.naming.GenNaming;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.List;

public class Name {

    // primary key的容器名称是“all”， 函数名称是"get"
    static final String primaryKeyMapName = "all";
    static final String primaryKeyGetName = "get";


    static String uniqueKeyGetByName(KeySchema key) {
        return StringUtil.lower1(GenNaming.uniqueKeyGetByName(key));
    }

    static String uniqueKeyMapName(KeySchema key) {
        return GenNaming.uniqueKeyMapName(key);
    }

    static String uniqueKeyGetByName(List<String> keyFields) {
        return StringUtil.lower1(GenNaming.uniqueKeyGetByName(keyFields));
    }


    static String refName(ForeignKeySchema fk) {
        return GenNaming.refFieldName(fk);
    }

    static String fullName(AContext aCtx, Nameable nameable) {
        switch (nameable) {
            case InterfaceSchema ignored -> {
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
                return aCtx.getPkgPrefixStr() + table.name().toLowerCase();
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
