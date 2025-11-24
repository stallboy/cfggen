package configgen.tool;

import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static configgen.value.ForeachValue.ValueVisitorForSearch;

public class SearchService {

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

    /**
     * @param maxItems <=0 means no limit
     */
    public static SearchResult searchNumber(@NotNull CfgValue cfgValue, long value, int maxItems) {
        NumberVisitor visitor = NumberVisitor.of(value);

        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            ForeachValue.searchVTable(visitor, vTable);
            if (maxItems > 0 && visitor.result.size() >= maxItems) {
                break;
            }
        }

        List<SearchResultItem> items = maxItems > 0 ? visitor.result.subList(0, Math.min(maxItems, visitor.result.size()))
                : visitor.result;
        return new SearchResult(ResultCode.ok, String.valueOf(value), maxItems, items);
    }

    public static SearchResult searchNumber(@NotNull CfgValue.VTable vTable, long value, int maxItems) {
        NumberVisitor visitor = NumberVisitor.of(value);
        ForeachValue.searchVTable(visitor, vTable);
        List<SearchResultItem> items = maxItems > 0 ? visitor.result.subList(0, Math.min(maxItems, visitor.result.size()))
                : visitor.result;
        return new SearchResult(ResultCode.ok, String.valueOf(value), maxItems, items);
    }


    public static SearchResult searchStr(@NotNull CfgValue cfgValue, String keyword, int maxItems) {
        StringVisitor visitor = StringVisitor.of(keyword);
        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            ForeachValue.searchVTable(visitor, vTable);
            if (maxItems > 0 && visitor.result.size() >= maxItems) {
                break;
            }
        }
        List<SearchResultItem> items = maxItems > 0 ? visitor.result.subList(0, Math.min(maxItems, visitor.result.size()))
                : visitor.result;
        return new SearchResult(ResultCode.ok, keyword, maxItems, items);
    }


    public static SearchResult searchStr(@NotNull CfgValue.VTable vTable, String keyword, int maxItems) {
        StringVisitor visitor = StringVisitor.of(keyword);
        ForeachValue.searchVTable(visitor, vTable);
        List<SearchResultItem> items = maxItems > 0 ? visitor.result.subList(0, Math.min(maxItems, visitor.result.size()))
                : visitor.result;
        return new SearchResult(ResultCode.ok, keyword, maxItems, items);
    }


    public record NumberVisitor(long q, List<SearchResultItem> result) implements ValueVisitorForSearch {
        public static NumberVisitor of(long q) {
            return new NumberVisitor(q, new ArrayList<>(32));
        }

        @Override
        public void visit(CfgValue.PrimitiveValue primitiveValue, String table, CfgValue.Value pk, List<String> fieldChain) {
            switch (primitiveValue) {
                case CfgValue.VInt vInt -> {
                    if (q == (long) vInt.value()) {
                        result.add(new SearchResultItem(
                                table, pk.packStr(), String.join(".", fieldChain), String.valueOf(vInt.value())));
                    }
                }
                case CfgValue.VLong vLong -> {
                    if (q == vLong.value()) {
                        result.add(new SearchResultItem(
                                table, pk.packStr(), String.join(".", fieldChain), String.valueOf(vLong.value())));
                    }
                }
                default -> {
                }
            }
        }
    }

    public record StringVisitor(String q, List<SearchResultItem> result) implements ValueVisitorForSearch {
        public static StringVisitor of(String q) {
            return new StringVisitor(q, new ArrayList<>(32));
        }

        @Override
        public void visit(CfgValue.PrimitiveValue primitiveValue, String table, CfgValue.Value pk, List<String> fieldChain) {
            if (Objects.requireNonNull(primitiveValue) instanceof CfgValue.StringValue sv) {
                String v = sv.value();
                if (v.contains(q)) {
                    result.add(new SearchResultItem(
                            table, pk.packStr(), String.join(".", fieldChain), v));
                }
            }
        }
    }

}
