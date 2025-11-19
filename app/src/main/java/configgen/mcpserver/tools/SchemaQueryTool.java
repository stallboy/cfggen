package configgen.mcpserver.tools;

import configgen.mcpserver.models.McpRequest;
import configgen.mcpserver.models.McpResponse;
import configgen.mcpserver.services.SchemaService;
import configgen.schema.TableSchema;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VTable;

import java.util.Map;

/**
 * 模式查询工具
 */
public class SchemaQueryTool implements McpTool {
    private final CfgValue cfgValue;
    private final SchemaService schemaService;

    public SchemaQueryTool(CfgValue cfgValue, SchemaService schemaService) {
        this.cfgValue = cfgValue;
        this.schemaService = schemaService;
    }

    @Override
    public McpResponse execute(McpRequest request) {
        String table = request.getArguments().getString("table");
        String requestId = request.getRequestId();

        if (table == null || table.isEmpty()) {
            return McpResponse.error(400, "Table name is required", requestId);
        }

        VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return McpResponse.error(400, "Table not found: " + table, requestId);
        }

        try {
            TableSchema mainTable = vTable.schema();
            String schemaText = schemaService.buildSchemaText(mainTable);
            return McpResponse.success(Map.of("schema", schemaText), requestId);
        } catch (Exception e) {
            return McpResponse.error(500, "Error processing schema: " + e.getMessage(), requestId);
        }
    }
}