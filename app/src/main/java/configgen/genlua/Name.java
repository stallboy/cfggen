package configgen.genlua;

import configgen.naming.GenNaming;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.Locale;
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
                return "Beans." + nameable.name().toLowerCase(Locale.ROOT);
            }
            case StructSchema ignored -> {
                return "Beans." + String.join(".", GenNaming.classNameSegments(nameable).stream()
                        .map(segment -> segment.toLowerCase(Locale.ROOT)).toList());
            }
            case TableSchema table -> {
                return aCtx.getPkgPrefixStr() + table.name().toLowerCase(Locale.ROOT);
            }
        }
    }

    static String tablePath(String tableName) {
        return tableName.replace('.', '/').toLowerCase(Locale.ROOT) + ".lua";
    }

    static String tableExtraPath(String tableName, int extraIndex) {
        return tableName.replace('.', '/').toLowerCase(Locale.ROOT) + "_" + extraIndex + ".lua";
    }
}
