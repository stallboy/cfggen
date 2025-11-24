package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import configgen.tool.SearchService;
import configgen.tool.SearchService.SearchResult;
import configgen.value.*;

public class SearchTool {

    @McpTool(description = "search string")
    public String searchString(@McpToolParam(name = "q", description = "query", required = true)
                               String q,
                               @McpToolParam(name = "maxCount", description = "max count", required = true)
                               int maxCount) {

        if (q == null || q.isEmpty()) {
            return "q is empty";
        }

        if (maxCount <= 0) {
            return "maxCount must be positive";
        }

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        SearchResult result = SearchService.searchStr(cfgValue, q, maxCount);
        return format(result, true);
    }


    @McpTool(description = "search number")
    public String searchNumber(@McpToolParam(name = "q", description = "query", required = true)
                               int q,
                               @McpToolParam(name = "maxCount", description = "max count", required = true)
                               int maxCount) {
        if (maxCount <= 0) {
            return "maxCount must be positive";
        }

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        SearchResult result = SearchService.searchNumber(cfgValue, q, maxCount);
        return format(result, false);
    }

    private String format(SearchResult result, boolean includeValue) {
        if (result.items().isEmpty()) {
            return "No value found for query q=%s".formatted(result.q());
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("Search Results for query q=%s:\n".formatted(result.q()));
        sb.append("```\n");
        if (includeValue) {
            sb.append("table,pk,fieldChain,value\n");
            for (SearchService.SearchResultItem item : result.items()) {
                sb.append(String.format("%s,%s,%s,%s\n", item.table(), item.pk(), item.fieldChain(), item.value()));
            }
        } else {
            sb.append("table,pk,fieldChain\n");
            for (SearchService.SearchResultItem item : result.items()) {
                sb.append(String.format("%s,%s,%s\n", item.table(), item.pk(), item.fieldChain()));
            }
        }
        sb.append("```\n");
        return sb.toString();
    }

}
