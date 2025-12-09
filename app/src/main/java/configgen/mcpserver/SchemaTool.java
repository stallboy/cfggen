package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import configgen.genbyai.TableRelatedInfoFinder;
import configgen.genbyai.TableRelatedInfoFinder.RelatedInfo;
import configgen.mcpserver.CfgMcpServer.CfgValueWithContext;
import configgen.value.CfgValue;

@SuppressWarnings("unused")
public class SchemaTool {

    @McpTool(description = "read table schema")
    public String readTableSchema(@McpToolParam(name = "table", description = "table full name", required = true)
                                  String tableName) {
        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return "table %s not found".formatted(tableName);
        }

        RelatedInfo relatedInfo = TableRelatedInfoFinder.findRelatedInfo(vc.context(), cfgValue, vTable);
        return relatedInfo.prompt();
    }


    @McpTool(description = "list table names")
    public String listTable(@McpToolParam(name = "inModule", description = "in module")
                            String inModule) {
        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        StringBuilder sb = new StringBuilder(2048);
        for (CfgValue.VTable table : cfgValue.sortedTables()) {
            if (inModule != null && !inModule.isEmpty()) {
                if (!table.name().startsWith(inModule)) {
                    continue;
                }
            }
            sb.append(table.name()).append("\n");
        }
        return "```\n%s```".formatted(sb.toString());
    }
}