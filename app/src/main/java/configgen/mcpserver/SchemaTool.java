package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.genbyai.TableRelatedInfoFinder;
import configgen.genbyai.TableRelatedInfoFinder.RelatedInfo;
import configgen.mcpserver.CfgMcpServer.CfgValueWithContext;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SchemaTool {

    public enum ErrorCode {
        OK,
        TableNotFound,
    }

    public record TableSchemaResult(ErrorCode errorCode,
                                    String table,
                                    RelatedInfo relatedInfo) implements McpStructuredContent {

        @Override
        public String asTextContent() {
            if (errorCode != ErrorCode.OK) {
                return "Error: " + errorCode;
            }
            StringBuilder sb = new StringBuilder(4096);
            sb.append("Table: ").append(table).append("\n");
            if (relatedInfo != null) {
                sb.append(relatedInfo.asTextContent());
            }
            return sb.toString();
        }

    }

    @McpTool(description = "read table schema")
    public TableSchemaResult readTableSchema(@McpToolParam(name = "table", description = "table full name", required = true)
                                             String tableName) {
        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new TableSchemaResult(ErrorCode.TableNotFound, tableName, null);
        }

        RelatedInfo relatedInfo = TableRelatedInfoFinder.findRelatedInfo(vc.context(), cfgValue, vTable);
        return new TableSchemaResult(ErrorCode.OK, tableName, relatedInfo);
    }


    public record ListTableResult(String inModule,
                                  List<String> tableNames) implements McpStructuredContent {
    }


    @McpTool(description = "list table names")
    public ListTableResult listTable(@McpToolParam(name = "inModule", description = "in module")
                                     String inModule) {
        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        List<String> tableNames = new ArrayList<>(8);
        StringBuilder sb = new StringBuilder(2048);
        for (CfgValue.VTable table : cfgValue.sortedTables()) {
            if (inModule != null && !inModule.isEmpty()) {
                if (!table.name().startsWith(inModule)) {
                    continue;
                }
            }
            tableNames.add(table.name());
        }
        return new ListTableResult(inModule, tableNames);
    }
}