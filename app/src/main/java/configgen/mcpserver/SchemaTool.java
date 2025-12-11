package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.genbyai.TableRelatedInfoFinder;
import configgen.genbyai.TableRelatedInfoFinder.ModuleRule;
import configgen.genbyai.TableRelatedInfoFinder.RelatedInfo;
import configgen.mcpserver.CfgMcpServer.CfgValueWithContext;
import configgen.value.CfgValue;

import java.util.*;

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

        boolean isTop = TOP.equals(inModule);
        List<String> tableNames = new ArrayList<>(8);
        StringBuilder sb = new StringBuilder(2048);
        CfgValue.VTable firstTableInModule = null;
        for (CfgValue.VTable table : cfgValue.sortedTables()) {
            boolean isMatch = (isTop && table.schema().namespace().isEmpty()) ||
                    table.schema().namespace().equals(inModule);
            if (isMatch) {
                tableNames.add(table.name());
                if (firstTableInModule == null) {
                    firstTableInModule = table;
                }
            }
        }

        if (firstTableInModule == null) {
            return new ListTableInModuleResult(ListTableInModuleErrorCode.ModuleNotFound, inModule, tableNames, "", "");
        }

        ModuleRule moduleRule = TableRelatedInfoFinder.findModuleRuleForTable(vc.context(), firstTableInModule.schema());
        String description = moduleRule != null && moduleRule.description() != null ? moduleRule.description() : "";
        String rule = moduleRule != null && moduleRule.rule() != null ? moduleRule.rule() : "";

        return new ListTableInModuleResult(ListTableInModuleErrorCode.OK, inModule, tableNames, description, rule);
    }

    public record ModuleDescription(String moduleName,
                                    String description) {
    }

    public record ListModuleResult(List<ModuleDescription> modules) implements McpStructuredContent {

        @Override
        public String asTextContent() {
            StringBuilder sb = new StringBuilder(2048);
            for (ModuleDescription mi : modules) {
                if (mi.description == null || mi.description.isBlank()) {
                    sb.append("- ").append(mi.moduleName).append("\n");
                } else {
                    sb.append("- ").append(mi.moduleName).append(": ").append(mi.description).append("\n");
                }
            }
            return sb.toString();
        }
    }

    @McpTool(description = "list module names. information entry point")
    public ListModuleResult listModule() {
        CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();
        Map<String, String> moduleDescriptionMap = new LinkedHashMap<>();

        for (CfgValue.VTable table : cfgValue.sortedTables()) {
            String namespace = table.schema().namespace();
            String moduleName = namespace.isEmpty() ? TOP : namespace;

            if (!moduleDescriptionMap.containsKey(moduleName)) {
                ModuleRule moduleRule = TableRelatedInfoFinder.findModuleRuleForTable(vc.context(), table.schema());
                String description = moduleRule != null && moduleRule.description() != null ?
                        moduleRule.description() : "";
                moduleDescriptionMap.put(moduleName, description);
            }

        }

        return new ListModuleResult(moduleDescriptionMap.entrySet().stream()
                .map(e -> new ModuleDescription(e.getKey(), e.getValue()))
                .toList());
    }


    private static final String TOP = "_top";


}