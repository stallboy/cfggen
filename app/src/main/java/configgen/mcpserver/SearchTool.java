package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.tool.SearchService;
import configgen.tool.SearchService.SearchResult;
import configgen.tool.SearchService.SearchResultItem;
import configgen.util.CSVUtil;
import configgen.value.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SearchTool {

    public enum ErrorCode {
        OK,
        qNotSet,
        TableNotFound,
    }

    public record SearchStringResult(ErrorCode errorCode,
                                     String q,
                                     String table,
                                     int max,
                                     List<SearchResultItem> items) implements McpStructuredContent {

        @Override
        public String asTextContent() {
            return format(errorCode, q, table, items, true);
        }
    }


    @McpTool(description = "search string")
    public SearchStringResult searchString(@McpToolParam(name = "q", description = "query", required = true)
                                           String q,
                                           @McpToolParam(name = "table", description = "if set, search in table only, otherwise search whole config")
                                           String table,
                                           @McpToolParam(name = "maxCount", description = "if not set, default to 100")
                                           int maxCount) {

        if (q == null || q.isEmpty()) {
            return new SearchStringResult(ErrorCode.qNotSet, q, table, maxCount, List.of());
        }

        if (maxCount <= 0) {
            maxCount = 100;
        }

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        SearchResult result;
        if (table == null || table.isEmpty()) {
            result = SearchService.searchStrInTable(cfgValue, q, maxCount);
        } else {
            CfgValue.VTable vTable = cfgValue.getTable(table);
            if (vTable == null) {
                return new SearchStringResult(ErrorCode.TableNotFound, q, table, maxCount, List.of());
            }
            result = SearchService.searchStrInTable(vTable, q, maxCount);
        }


        return new SearchStringResult(ErrorCode.OK, q, table, maxCount, result.items());
    }

    public record SearchNumberResult(ErrorCode errorCode,
                                     int q,
                                     String table,
                                     int max,
                                     List<SearchResultItem> items) implements McpStructuredContent {
        @Override
        public String asTextContent() {
            return format(errorCode, String.valueOf(q), table, items, false);
        }
    }


    @McpTool(description = "search number")
    public SearchNumberResult searchNumber(@McpToolParam(name = "q", description = "query", required = true)
                                           int q,
                                           @McpToolParam(name = "table", description = "if set, search in table only, otherwise search whole config")
                                           String table,
                                           @McpToolParam(name = "maxCount", description = "if not set, default to 100")
                                           int maxCount) {
        if (maxCount <= 0) {
            maxCount = 100;
        }

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        SearchResult result;
        if (table == null || table.isEmpty()) {
            result = SearchService.searchNumber(cfgValue, q, maxCount);
        } else {
            CfgValue.VTable vTable = cfgValue.getTable(table);
            if (vTable == null) {
                return new SearchNumberResult(ErrorCode.TableNotFound, q, table, maxCount, List.of());
            }
            result = SearchService.searchNumber(vTable, q, maxCount);
        }

        return new SearchNumberResult(ErrorCode.OK, q, table, maxCount, result.items());
    }


    private static String format(ErrorCode errorCode, String q, String table, List<SearchResultItem> items, boolean includeValue) {
        if (errorCode == ErrorCode.qNotSet) {
            return "Query q is not set.";
        } else if (errorCode == ErrorCode.TableNotFound) {
            return "Table %s not found.".formatted(table);
        }

        if (items.isEmpty()) {
            return "No value found for query q=%s table=%s".formatted(q, table);
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("Search Results for query q=%s table=%s:\n".formatted(q, table));
        sb.append("```\n");

        List<List<String>> rows = new ArrayList<>(items.size() + 1);
        if (includeValue) {
            rows.add(List.of("table", "pk", "fieldChain", "value"));
            for (SearchService.SearchResultItem item : items) {
                rows.add(List.of(item.table(), item.pk(), item.fieldChain(), shortenString(item.value(), 48)));
            }

        } else {
            rows.add(List.of("table", "pk", "fieldChain"));
            for (SearchService.SearchResultItem item : items) {
                rows.add(List.of(item.table(), item.pk(), item.fieldChain()));
            }
        }
        CSVUtil.write(sb, rows);
        sb.append("```\n");

        return sb.toString();
    }

    private static String shortenString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

}
