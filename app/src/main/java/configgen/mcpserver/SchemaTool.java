package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.genbyai.TableRelatedInfoFinder;
import configgen.genbyai.TableRelatedInfoFinder.ModuleRule;
import configgen.genbyai.TableRelatedInfoFinder.RelatedInfo;
import configgen.mcpserver.CfgMcpServer.CfgValueWithContext;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 3级信息披露
 * 1. list module，这是信息入口
 * 2. list table in module
 * 3. read table schema
 */
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
    public TableSchemaResult readTableSchema(@McpToolParam(name = "table", description = "table name", required = true)
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

    public enum ListTableInModuleErrorCode {
        OK,
        ModuleNotSet,
        ModuleNotFound,
        NoTablesInModule,
    }

    public record ListTableInModuleResult(ListTableInModuleErrorCode errorCode,
                                          String inModule,
                                          List<String> tableNames,
                                          String description,
                                          String rule) implements McpStructuredContent {
    }


    @McpTool(description = "list table names in module")
    public ListTableInModuleResult listTable(
            @McpToolParam(name = "inModule", description = "module from <list module names>", required = true)
            String inModule) {
        if (inModule == null) {
            return new ListTableInModuleResult(ListTableInModuleErrorCode.ModuleNotSet, "", List.of(), "", "");
        }

        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        Set<String> moduleSet = getModuleSet(cfgValue);
        if (!moduleSet.contains(inModule)) {
            return new ListTableInModuleResult(ListTableInModuleErrorCode.ModuleNotFound, inModule, List.of(), "", "");
        }


        boolean isTop = TOP.equals(inModule);
        List<String> tableNames = new ArrayList<>(8);
        StringBuilder sb = new StringBuilder(2048);
        CfgValue.VTable firstTableInModule = null;
        for (CfgValue.VTable table : cfgValue.sortedTables()) {
            if ((isTop && table.schema().namespace().isEmpty()) ||
                    table.schema().namespace().equals(inModule)
            ) {
                tableNames.add(table.name());
                if (firstTableInModule == null) {
                    firstTableInModule = table;
                }
            }
        }

        if (firstTableInModule == null) {
            return new ListTableInModuleResult(ListTableInModuleErrorCode.NoTablesInModule, inModule, tableNames, "", "");
        }

        ModuleRule moduleRule = TableRelatedInfoFinder.findModuleRuleForTable(vc.context(), firstTableInModule.schema());
        String description = moduleRule != null && moduleRule.description() != null ? moduleRule.description() : "";
        String rule = moduleRule != null && moduleRule.rule() != null ? moduleRule.rule() : "";

        return new ListTableInModuleResult(ListTableInModuleErrorCode.OK, inModule, tableNames, description, rule);
    }

    public record ListModuleResult(Set<String> moduleNames) implements McpStructuredContent {
    }

    @McpTool(description = "list module names. information entry point")
    public ListModuleResult listModule() {
        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        return new ListModuleResult(getModuleSet(cfgValue));
    }


    private static final String TOP = "_top";

    private static Set<String> getModuleSet(CfgValue cfgValue) {
        Set<String> moduleNames = new LinkedHashSet<>(16);
        for (CfgValue.VTable table : cfgValue.sortedTables()) {
            String namespace = table.schema().namespace();
            if (!namespace.isEmpty()) {
                moduleNames.add(namespace);
            } else {
                moduleNames.add(TOP);
            }
        }
        return moduleNames;
    }
}