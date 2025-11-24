package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import configgen.tool.SearchService;
import configgen.tool.SearchService.SearchResult;
import configgen.util.CSVUtil;
import configgen.value.*;

import java.util.ArrayList;
import java.util.List;

public class SearchTool {

    @McpTool(description = "search string")
    public String searchString(@McpToolParam(name = "q", description = "query", required = true)
                               String q,
                               @McpToolParam(name = "table", description = "if set, search in table only, otherwise search whole config")
                               String table,
                               @McpToolParam(name = "maxCount", description = "if not set, default to 100")
                               int maxCount) {

        if (q == null || q.isEmpty()) {
            return "q is empty";
        }

        if (maxCount <= 0) {
            maxCount = 100;
        }

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        SearchResult result;
        if (table == null || table.isEmpty()) {
            result = SearchService.searchStr(cfgValue, q, maxCount);
        } else {
            CfgValue.VTable vTable = cfgValue.getTable(table);
            if (vTable == null) {
                return "table %s not found".formatted(table);
            }
            result = SearchService.searchStr(vTable, q, maxCount);
        }


        return format(result, true);
    }


    @McpTool(description = "search number")
    public String searchNumber(@McpToolParam(name = "q", description = "query", required = true)
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
                return "table %s not found".formatted(table);
            }
            result = SearchService.searchNumber(vTable, q, maxCount);
        }

        return format(result, false);
    }


    private String format(SearchResult result, boolean includeValue) {
        if (result.items().isEmpty()) {
            return "No value found for query q=%s".formatted(result.q());
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("Search Results for query q=%s:\n".formatted(result.q()));
        sb.append("```\n");

        List<List<String>> rows = new ArrayList<>(result.items().size() + 1);
        if (includeValue) {
            rows.add(List.of("table", "pk", "fieldChain", "value"));
            for (SearchService.SearchResultItem item : result.items()) {
                rows.add(List.of(item.table(), item.pk(), item.fieldChain(), item.value()));
            }

        } else {
            rows.add(List.of("table", "pk", "fieldChain"));
            for (SearchService.SearchResultItem item : result.items()) {
                rows.add(List.of(item.table(), item.pk(), item.fieldChain()));
            }
        }
        CSVUtil.write(sb, rows);

        sb.append("```\n");
        return sb.toString();
    }

}
