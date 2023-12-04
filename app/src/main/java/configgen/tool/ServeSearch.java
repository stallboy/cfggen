package configgen.tool;

import configgen.value.CfgValue;
import configgen.value.ForeachPrimitiveValue;
import configgen.value.RefSearcher;

import java.util.*;

public class ServeSearch {

    public record SearchNumber(Set<Long> numberSet) {
    }

    public record SearchNumberResultItem(String table,
                                         String primaryKey,
                                         String fieldChain,
                                         long value) {
    }

    public record SearchNumberResult(List<SearchNumberResultItem> items) {
    }

    public record SearchStr(String keyword) {
    }

    public record SearchStrResultItem(String table,
                                      String primaryKey,
                                      String fieldChain,
                                      String value) {
    }

    public record SearchStrResult(List<SearchStrResultItem> items) {
    }


    public record SearchRef(String table,
                            List<String> uniqKeys,  // 默认为空，搜索索引对主键的索引
                            Set<String> ignoredTables) {
    }

    public record SearchRefResultItem(String key,
                                      Set<String> refTables) {
    }


    public record SearchRefResult(List<SearchRefResultItem> items) {
    }


    public static SearchNumberResult searchNumber(CfgValue cfgValue, SearchNumber param) {
        SearchNumberResult res = new SearchNumberResult(new ArrayList<>(32));
        ForeachPrimitiveValue.foreach((primitiveValue, table, pk, fieldChain) -> {
            switch (primitiveValue) {
                case CfgValue.VInt vInt -> {
                    if (param.numberSet.contains((long) vInt.value())) {
                        res.items.add(new SearchNumberResultItem(
                                table, pk.repr(), String.join(".", fieldChain), vInt.value()));
                    }
                }
                case CfgValue.VLong vLong -> {
                    if (param.numberSet.contains(vLong.value())) {
                        res.items.add(new SearchNumberResultItem(
                                table, pk.repr(), String.join(".", fieldChain), vLong.value()));
                    }
                }
                default -> {
                }
            }
        }, cfgValue);
        return res;
    }

    public static SearchStrResult searchStr(CfgValue cfgValue, SearchStr param) {
        SearchStrResult res = new SearchStrResult(new ArrayList<>(32));
        ForeachPrimitiveValue.foreach((primitiveValue, table, pk, fieldChain) -> {
            if (Objects.requireNonNull(primitiveValue) instanceof CfgValue.StringValue sv) {
                String v = sv.value();
                if (v.contains(param.keyword)) {
                    res.items.add(new SearchStrResultItem(
                            table, pk.repr(), String.join(".", fieldChain), v));
                }
            }
        }, cfgValue);
        return res;
    }

    public static SearchRefResult searchRef(CfgValue cfgValue, SearchRef param) {
        SearchRefResult res = new SearchRefResult(new ArrayList<>(32));

        RefSearcher.RefSearchResult rs = RefSearcher.search(cfgValue, param.table, param.uniqKeys, param.ignoredTables);
        if (rs.err() == RefSearcher.RefSearchErr.Ok) {
            for (Map.Entry<CfgValue.Value, Set<String>> e : rs.value2tables().entrySet()) {
                Set<String> tables = e.getValue();
                res.items.add(new SearchRefResultItem(e.getKey().repr(), tables));
            }
        }
        return res;
    }
}
