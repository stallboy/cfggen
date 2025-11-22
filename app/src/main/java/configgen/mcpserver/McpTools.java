package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import configgen.genjson.TableRelatedInfoFinder;
import configgen.genjson.TableRelatedInfoFinder.RelatedInfo;
import configgen.mcpserver.CfgMcpServer.CfgValueWithContext;
import configgen.value.CfgValue;

import java.util.List;

public class McpTools {


    @McpTool(description = "read table schema")
    public String readTableSchema(@McpToolParam(name = "table", description = "table full name", required = true)
                                  String tableName) {
        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return "table %s not found".formatted(tableName);
        }

        RelatedInfo relatedInfo = TableRelatedInfoFinder.findRelatedInfo(cfgValue, vTable.schema(), List.of());
        return relatedInfo.prompt();
    }


}