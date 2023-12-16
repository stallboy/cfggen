package configgen.editorserver;

import configgen.value.CfgValue;
import configgen.value.RefSearcher;

import java.util.*;

import static configgen.value.ForeachPrimitiveValue.PrimitiveValueVisitor;
import static configgen.value.ForeachPrimitiveValue.foreachVTable;

public class ServeSearch {

    public enum ResultCode {
        ok,
        qNotSet,
    }

    public record SearchResultItem(String table,
                                   String pk,
                                   String fieldChain,
                                   String value) {
    }

    public record SearchResult(ResultCode resultCode,
                               String q,
                               int max,
                               List<SearchResultItem> items) {
    }

    public static SearchResult search(CfgValue cfgValue, String q, int maxItems) {
        if (q == null || q.isBlank()) {
            return new SearchResult(ResultCode.qNotSet, q == null ? "" : q, maxItems, List.of());
        }

        boolean isNumber = false;
        long value = 0;
        try {
            value = Long.parseLong(q);
            isNumber = true;
        } catch (NumberFormatException e) {
            // ignore
        }

        if (isNumber) {
            return searchNumber(cfgValue, value, maxItems);
        } else {
            return searchStr(cfgValue, q, maxItems);
        }
    }

    public static SearchResult searchNumber(CfgValue cfgValue, long value, int maxItems) {
        SearchResult res = new SearchResult(ResultCode.ok, String.valueOf(value), maxItems, new ArrayList<>(32));
        PrimitiveValueVisitor visitor = (primitiveValue, table, pk, fieldChain) -> {
            switch (primitiveValue) {
                case CfgValue.VInt vInt -> {
                    if (value == (long) vInt.value()) {
                        res.items.add(new SearchResultItem(
                                table, pk.packStr(), String.join(".", fieldChain), String.valueOf(vInt.value())));
                    }
                }
                case CfgValue.VLong vLong -> {
                    if (value == vLong.value()) {
                        res.items.add(new SearchResultItem(
                                table, pk.packStr(), String.join(".", fieldChain), String.valueOf(vLong.value())));
                    }
                }
                default -> {
                }
            }
        };

        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            foreachVTable(visitor, vTable);
            if (res.items.size() >= maxItems) {
                break;
            }
        }
        return res;
    }

    public static SearchResult searchStr(CfgValue cfgValue, String keyword, int maxItems) {
        SearchResult res = new SearchResult(ResultCode.ok, keyword, maxItems, new ArrayList<>(32));
        PrimitiveValueVisitor visitor = (primitiveValue, table, pk, fieldChain) -> {
            if (Objects.requireNonNull(primitiveValue) instanceof CfgValue.StringValue sv) {
                String v = sv.value();
                if (v.contains(keyword)) {
                    res.items.add(new SearchResultItem(
                            table, pk.packStr(), String.join(".", fieldChain), v));
                }
            }
        };

        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            foreachVTable(visitor, vTable);
            if (res.items.size() >= maxItems) {
                break;
            }
        }
        return res;
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
