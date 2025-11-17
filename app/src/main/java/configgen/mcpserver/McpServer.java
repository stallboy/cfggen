package configgen.mcpserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.*;
import configgen.ctx.Context;
import configgen.editorserver.RecordEditService;
import configgen.editorserver.RecordService;
import configgen.editorserver.SchemaService;
import configgen.ctx.DirectoryStructure;
import configgen.ctx.WaitWatcher;
import configgen.ctx.Watcher;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.genjson.AICfg;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import static configgen.schema.FieldType.*;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.CfgValue.Value;
import configgen.value.ValueUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.UUID;
import configgen.util.Logger;

/**
 * MCP (Model Context Protocol) 服务器实现
 * 提供配置数据的标准化访问接口
 */
public class McpServer extends GeneratorWithTag {

    // MCP 标准端点
    private static final String INITIALIZE_PATH = "/initialize";
    private static final String HEALTH_PATH = "/health";
    private static final String TOOLS_LIST_PATH = "/tools/list";
    private static final String TOOLS_CALL_PATH = "/tools/call";
    private static final String RESOURCES_LIST_PATH = "/resources/list";
    private static final String RESOURCES_READ_PATH = "/resources/read";

    // MCP 协议版本
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_VERSION = "1.0.0";

    // 工具名称
    private static final String TOOL_SCHEMA_QUERY = "schema_query";
    private static final String TOOL_DATA_QUERY = "data_query";
    private static final String TOOL_DATA_UPDATE = "data_update";
    private static final String TOOL_DATA_REMOVE = "data_remove";

    // 资源URI前缀
    private static final String RESOURCE_URI_PREFIX = "table://";

    // HTTP方法
    private static final String HTTP_GET = "GET";
    private static final String HTTP_POST = "POST";

    private final String aiCfgFn;
    private final int port;
    private Path aiDir;
    private AICfg aiCfg;
    private Context context;
    private CfgValue cfgValue;
    private HttpServer server;
    private long startTime;
    private final int waitSecondsAfterWatchEvt;

    public McpServer(Parameter parameter) {
        super(parameter);
        aiCfgFn = parameter.get("aicfg", null);
        port = Integer.parseInt(parameter.get("port", "3000"));
        waitSecondsAfterWatchEvt = Integer.parseInt(parameter.get("watch", "0"));
        Logger.log("McpServer constructor called with port: " + port);
        System.out.println("DEBUG: McpServer constructor called with port: " + port);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        Logger.log("McpServer.generate() called");

        if (aiCfgFn != null) {
            aiDir = Path.of(aiCfgFn).getParent();
            aiCfg = AICfg.readFromFile(aiCfgFn);
        }

        this.context = ctx;
        this.cfgValue = ctx.makeValue();

        System.gc();

        startMcpServer();
    }

    /**
     * 启动MCP服务器
     */
    private void startMcpServer() throws IOException {
        startTime = System.currentTimeMillis();
        InetSocketAddress listenAddr = new InetSocketAddress(port);
        server = HttpServer.create(listenAddr, 0);

        // 设置MCP标准端点
        handle(INITIALIZE_PATH, this::handleInitialize);
        handle(HEALTH_PATH, this::handleHealth);
        handle(TOOLS_LIST_PATH, this::handleToolsList);
        handle(TOOLS_CALL_PATH, this::handleToolsCall);
        handle(RESOURCES_LIST_PATH, this::handleResourcesList);
        handle(RESOURCES_READ_PATH, this::handleResourcesRead);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        Logger.log(String.format("MCP Server started on port %d", port));
        Logger.log("Available tables: " + cfgValue.vTableMap().keySet());
        Logger.log("Registered endpoints: " +
            INITIALIZE_PATH + ", " +
            HEALTH_PATH + ", " +
            TOOLS_LIST_PATH + ", " +
            TOOLS_CALL_PATH + ", " +
            RESOURCES_LIST_PATH + ", " +
            RESOURCES_READ_PATH);

        if (waitSecondsAfterWatchEvt > 0) {
            Watcher watcher = new Watcher(context.getSourceStructure().getRootDir(), context.getContextCfg().explicitDir());
            WaitWatcher waitWatcher = new WaitWatcher(watcher, this::reloadData, waitSecondsAfterWatchEvt * 1000);
            waitWatcher.start();
            watcher.start();
            Logger.log("file change watcher started");
        }
    }

    private void handle(String path, HttpHandler handler) {
        try {
            HttpContext context = server.createContext(path, handler);
            context.getFilters().add(logging);
            Logger.log("Successfully registered endpoint: " + path);
        } catch (Exception e) {
            Logger.verbose("Failed to register endpoint " + path + ": " + e.getMessage());
        }
    }

    private static final Filter logging = new Filter() {
        @Override
        public void doFilter(HttpExchange http, Chain chain) throws IOException {
            try {
                chain.doFilter(http);
            } finally {
                Logger.log(String.format("%s %s",
                        http.getRequestMethod(),
                        http.getRequestURI()));
            }
        }

        @Override
        public String description() {
            return "logging";
        }
    };

    /**
     * 处理初始化请求
     */
    private void handleInitialize(HttpExchange exchange) throws IOException {
        if (!HTTP_GET.equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> response = Map.of(
            "protocolVersion", MCP_PROTOCOL_VERSION,
            "serverVersion", SERVER_VERSION,
            "capabilities", Map.of(
                "tools", Map.of(
                    "listChanged", false,
                    "subscribe", false
                ),
                "resources", Map.of(
                    "listChanged", false,
                    "subscribe", false
                )
            ),
            "serverInfo", Map.of(
                "name", "cfggen-mcp-server",
                "version", SERVER_VERSION
            )
        );

        sendJsonResponse(exchange, response);
    }

    /**
     * 处理健康检查请求
     */
    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!HTTP_GET.equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> response = Map.of(
            "status", "healthy",
            "timestamp", System.currentTimeMillis(),
            "uptime", System.currentTimeMillis() - startTime,
            "tables", cfgValue.vTableMap().size(),
            "memory", Map.of(
                "used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                "total", Runtime.getRuntime().totalMemory()
            )
        );

        sendJsonResponse(exchange, response);
    }

    /**
     * 处理工具列表请求
     */
    private void handleToolsList(HttpExchange exchange) throws IOException {
        if (!HTTP_GET.equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        List<Map<String, Object>> tools = List.of(
            createTool("schema_query", "查询表结构信息",
                Map.of("table", Map.of("type", "string", "description", "表名"))),
            createTool("data_query", "查询表数据",
                Map.of(
                    "table", Map.of("type", "string", "description", "表名"),
                    "condition", Map.of("type", "string", "description", "查询条件"))),
            createTool("data_update", "更新表数据",
                Map.of(
                    "table", Map.of("type", "string", "description", "表名"),
                    "id", Map.of("type", "string", "description", "记录ID"),
                    "newValue", Map.of("type", "string", "description", "新值（JSON格式）"))),
            createTool("data_remove", "删除表数据",
                Map.of(
                    "table", Map.of("type", "string", "description", "表名"),
                    "id", Map.of("type", "string", "description", "记录ID")))
        );

        Map<String, Object> response = Map.of("tools", tools);
        sendJsonResponse(exchange, response);
    }

    /**
     * 处理工具调用请求
     */
    private void handleToolsCall(HttpExchange exchange) throws IOException {
        if (!HTTP_POST.equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String requestId = generateRequestId();

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (requestBody.isEmpty()) {
                sendErrorWithRequestId(exchange, 400, "Request body is empty", requestId);
                return;
            }

            JSONObject request = JSON.parseObject(requestBody);
            if (request == null) {
                sendErrorWithRequestId(exchange, 400, "Invalid JSON request", requestId);
                return;
            }

            String toolName = request.getString("name");
            if (toolName == null || toolName.isEmpty()) {
                sendErrorWithRequestId(exchange, 400, "Tool name is required", requestId);
                return;
            }

            JSONObject arguments = request.getJSONObject("arguments");
            if (arguments == null) {
                sendErrorWithRequestId(exchange, 400, "Arguments are required", requestId);
                return;
            }

            Map<String, Object> result = switch (toolName) {
                case TOOL_SCHEMA_QUERY -> handleSchemaQuery(
                    arguments.getString("table"), requestId);
                case TOOL_DATA_QUERY -> handleDataQuery(
                    arguments.getString("table"),
                    arguments.getString("condition"), requestId);
                case TOOL_DATA_UPDATE -> handleDataUpdate(
                    arguments.getString("table"),
                    arguments.getString("id"),
                    arguments.getString("newValue"), requestId);
                case TOOL_DATA_REMOVE -> handleDataRemove(
                    arguments.getString("table"),
                    arguments.getString("id"), requestId);
                default -> createErrorResponse(400, "Unknown tool: " + toolName, requestId);
            };

            sendJsonResponse(exchange, Map.of("content", List.of(
                Map.of("type", "text", "text", JSON.toJSONString(result))
            )));

        } catch (Exception e) {
            Logger.log("Error handling tool call: " + e.getMessage());
            sendErrorWithRequestId(exchange, 500, "Internal server error: " + e.getMessage(), requestId);
        }
    }

    /**
     * 处理资源列表请求
     */
    private void handleResourcesList(HttpExchange exchange) throws IOException {
        if (!HTTP_GET.equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        List<Map<String, Object>> resources = new ArrayList<>();
        for (String tableName : cfgValue.vTableMap().keySet()) {
            resources.add(Map.of(
                "uri", "table://" + tableName,
                "name", tableName,
                "description", "Table: " + tableName,
                "mimeType", "application/json"
            ));
        }

        sendJsonResponse(exchange, Map.of("resources", resources));
    }

    /**
     * 处理资源读取请求
     */
    private void handleResourcesRead(HttpExchange exchange) throws IOException {
        if (!HTTP_GET.equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String uri = exchange.getRequestURI().getQuery();
        if (uri == null || !uri.startsWith(RESOURCE_URI_PREFIX)) {
            sendError(exchange, 400, "Invalid resource URI. Expected format: " + RESOURCE_URI_PREFIX + "{table_name}");
            return;
        }

        String tableName = uri.substring(RESOURCE_URI_PREFIX.length());
        if (tableName.isEmpty()) {
            sendError(exchange, 400, "Table name is required in resource URI");
            return;
        }

        String requestId = generateRequestId();
        Map<String, Object> result = handleDataQuery(tableName, "", requestId);

        sendJsonResponse(exchange, Map.of(
            "contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "application/json",
                "text", JSON.toJSONString(result)
            ))
        ));
    }

    /**
     * 查询表结构信息
     */
    private Map<String, Object> handleSchemaQuery(String table, String requestId) {
        if (table == null || table.isEmpty()) {
            return createErrorResponse(400, "Table name is required", requestId);
        }

        VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return createErrorResponse(400, "Table not found: " + table, requestId);
        }

        try {
            SchemaService.SNameable schemaItem = SchemaService.fromNameable(vTable.schema(), cfgValue);
            if (schemaItem instanceof SchemaService.STable sTable) {
                TableSchema mainTable = (TableSchema) vTable.schema();
                String schemaText = buildSchemaText(mainTable);
                return createSuccessResponse(Map.of("schema", schemaText), requestId);
            }
            return createErrorResponse(500, "Unexpected schema type", requestId);
        } catch (Exception e) {
            Logger.log("Error processing schema: " + e.getMessage());
            return createErrorResponse(500, "Error processing schema: " + e.getMessage(), requestId);
        }
    }

    /**
     * 查询数据
     */
    private Map<String, Object> handleDataQuery(String table, String condition, String requestId) {
        if (table == null || table.isEmpty()) {
            return createErrorResponse(400, "Table name is required", requestId);
        }

        VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return createErrorResponse(400, "Table not found: " + table, requestId);
        }

        try {
            List<VStruct> results = filterRecords(vTable, condition);
            Map<String, Object> data = formatDataResponse(table, results);
            return createSuccessResponse(data, requestId);
        } catch (Exception e) {
            Logger.log("Error querying data: " + e.getMessage());
            return createErrorResponse(500, "Error querying data: " + e.getMessage(), requestId);
        }
    }

    /**
     * 更新数据
     */
    private Map<String, Object> handleDataUpdate(String table, String id, String newValue, String requestId) {
        if (table == null || table.isEmpty()) {
            return createErrorResponse(400, "Table name is required", requestId);
        }

        if (id == null || id.isEmpty()) {
            return createErrorResponse(400, "Record ID is required", requestId);
        }

        if (newValue == null || newValue.isEmpty()) {
            return createErrorResponse(400, "New value is required", requestId);
        }

        try {
            RecordEditService editService = new RecordEditService(cfgValue, context);
            RecordEditService.RecordEditResult result = editService.addOrUpdateRecord(table, newValue);
            Map<String, Object> data = formatEditResponse(result);
            return createSuccessResponse(data, requestId);
        } catch (Exception e) {
            Logger.log("Error updating data: " + e.getMessage());
            return createErrorResponse(500, "Error updating data: " + e.getMessage(), requestId);
        }
    }

    /**
     * 删除数据
     */
    private Map<String, Object> handleDataRemove(String table, String id, String requestId) {
        if (table == null || table.isEmpty()) {
            return createErrorResponse(400, "Table name is required", requestId);
        }

        if (id == null || id.isEmpty()) {
            return createErrorResponse(400, "Record ID is required", requestId);
        }

        try {
            RecordEditService editService = new RecordEditService(cfgValue, context);
            RecordEditService.RecordEditResult result = editService.deleteRecord(table, id);
            Map<String, Object> data = formatEditResponse(result);
            return createSuccessResponse(data, requestId);
        } catch (Exception e) {
            Logger.log("Error removing data: " + e.getMessage());
            return createErrorResponse(500, "Error removing data: " + e.getMessage(), requestId);
        }
    }

    /**
     * 根据条件过滤记录
     */
    private List<VStruct> filterRecords(VTable vTable, String condition) {
        List<VStruct> allRecords = vTable.valueList();

        if (condition == null || condition.isEmpty()) {
            return allRecords;
        }

        List<VStruct> filtered = new ArrayList<>();
        for (VStruct record : allRecords) {
            if (matchesCondition(record, condition)) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    /**
     * 检查记录是否匹配条件
     */
    private boolean matchesCondition(VStruct record, String condition) {
        String recordStr = record.packStr().toLowerCase();
        return recordStr.contains(condition.toLowerCase());
    }

    /**
     * 格式化模式查询响应
     */
    private Map<String, Object> formatSchemaResponse(SchemaService.STable sTable) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("table", sTable.name());
        response.put("comment", sTable.comment());
        response.put("primaryKey", sTable.pk());
        response.put("uniqueKeys", sTable.uks());

        List<Map<String, Object>> fields = new ArrayList<>();
        for (SchemaService.SField field : sTable.fields()) {
            fields.add(Map.of(
                "name", field.name(),
                "type", field.type(),
                "comment", field.comment()
            ));
        }
        response.put("fields", fields);

        List<Map<String, Object>> foreignKeys = new ArrayList<>();
        for (SchemaService.SForeignKey fk : sTable.foreignKeys()) {
            foreignKeys.add(Map.of(
                "name", fk.name(),
                "keys", fk.keys(),
                "refTable", fk.refTable(),
                "refType", fk.refType().toString(),
                "refKeys", fk.refKeys()
            ));
        }
        response.put("foreignKeys", foreignKeys);

        return response;
    }

    private void reloadData() {
        DirectoryStructure newStructure = context.getSourceStructure().reload();
        if (newStructure.lastModifiedEquals(context.getSourceStructure())) {
            configgen.util.Logger.verbose("lastModified not change");
            return;
        }
        try {
            this.context = new Context(context.getContextCfg(), newStructure);
            this.cfgValue = context.makeValue();
            Logger.log("reload ok");
        } catch (Exception e) {
            Logger.log("reload ignored");
        }
    }

    private String buildSchemaText(TableSchema mainTable) {
        Set<String> visited = new LinkedHashSet<>();
        List<Nameable> ordered = new ArrayList<>();

        visited.add(mainTable.name());
        ordered.add(mainTable);

        TableSchemaRefGraph graph = new TableSchemaRefGraph(context.cfgSchema());
        TableSchemaRefGraph.Refs refs = graph.refsMap().get(mainTable.name());
        if (refs != null) {
            for (TableSchema t : refs.refOutTables().values()) {
                if (visited.add(t.name())) {
                    ordered.add(t);
                }
            }
            for (TableSchema t : refs.refInTables().values()) {
                if (visited.add(t.name())) {
                    ordered.add(t);
                }
            }
        }

        // 收集所有相关的结构体定义
        Set<StructSchema> relatedStructs = new LinkedHashSet<>();
        for (Nameable item : ordered) {
            if (item instanceof TableSchema table) {
                collectRelatedStructs(table, relatedStructs);
            }
        }

        Logger.log("Collected structs: " + relatedStructs.size());
        for (StructSchema struct : relatedStructs) {
            Logger.log("Struct: " + struct.name());
        }

        CfgSchema sub = CfgSchema.ofPartial();
        // 先添加结构体定义
        for (StructSchema struct : relatedStructs) {
            sub.add(struct);
        }
        // 再添加表定义
        for (Nameable item : ordered) {
            sub.add(item);
        }
        return CfgWriter.stringify(sub, true, false);
    }

    /**
     * 收集表相关的所有结构体定义
     */
    private void collectRelatedStructs(TableSchema table, Set<StructSchema> structs) {
        // 收集表字段中使用的结构体类型
        for (FieldSchema field : table.fields()) {
            collectStructsFromFieldType(field.type(), structs);
        }

        // 收集外键引用中使用的结构体
        for (ForeignKeySchema fk : table.foreignKeys()) {
            // 外键本身不直接包含结构体，但引用的表可能包含
            TableSchema refTable = fk.refTableSchema();
            if (refTable != null) {
                collectRelatedStructs(refTable, structs);
            }
        }
    }

    /**
     * 从字段类型中收集结构体定义
     */
    private void collectStructsFromFieldType(FieldType fieldType, Set<StructSchema> structs) {
        Logger.log("collectStructsFromFieldType: " + fieldType.getClass().getSimpleName());
        switch (fieldType) {
            case StructRef structRef -> {
                Logger.log("Found StructRef: " + structRef.name());
                Nameable ref = structRef.obj();
                Logger.log("Ref obj type: " + (ref != null ? ref.getClass().getSimpleName() : "null"));
                if (ref instanceof StructSchema structSchema) {
                    Logger.log("Adding struct: " + structSchema.name());
                    structs.add(structSchema);
                    // 递归收集该结构体的字段中的结构体
                    for (FieldSchema field : structSchema.fields()) {
                        collectStructsFromFieldType(field.type(), structs);
                    }
                }
            }
            case FList fList -> {
                Logger.log("Found FList");
                collectStructsFromFieldType(fList.item(), structs);
            }
            case FMap fMap -> {
                Logger.log("Found FMap");
                collectStructsFromFieldType(fMap.key(), structs);
                collectStructsFromFieldType(fMap.value(), structs);
            }
            default -> {
                Logger.log("Basic type: " + fieldType);
                // 基本类型，不需要处理
            }
        }
    }

    /**
     * 格式化数据查询响应
     */
    private Map<String, Object> formatDataResponse(String table, List<VStruct> records) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("table", table);
        response.put("count", records.size());

        List<Map<String, Object>> recordList = new ArrayList<>();
        for (VStruct record : records) {
            TableSchema tableSchema = (TableSchema) record.schema();
            Value pkValue = ValueUtil.extractPrimaryKeyValue(record, tableSchema);
            String id = pkValue.packStr();
            String title = RecordService.getBriefTitle(record);

            recordList.add(Map.of(
                "id", id,
                "title", title,
                "value", record.packStr()
            ));
        }
        response.put("records", recordList);

        return response;
    }

    /**
     * 格式化编辑操作响应
     */
    private Map<String, Object> formatEditResponse(RecordEditService.RecordEditResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resultCode", result.resultCode().toString());
        response.put("table", result.table());
        response.put("id", result.id());

        if (!result.valueErrs().isEmpty()) {
            response.put("errors", result.valueErrs());
        }

        List<Map<String, Object>> recordIds = new ArrayList<>();
        for (SchemaService.RecordId recordId : result.recordIds()) {
            recordIds.add(Map.of(
                "id", recordId.id(),
                "title", recordId.title()
            ));
        }
        response.put("recordIds", recordIds);

        return response;
    }

    /**
     * 创建工具定义
     */
    private Map<String, Object> createTool(String name, String description, Map<String, Object> inputSchema) {
        return Map.of(
            "name", name,
            "description", description,
            "inputSchema", Map.of("type", "object", "properties", inputSchema)
        );
    }

    /**
     * 发送JSON响应
     */
    private void sendJsonResponse(HttpExchange exchange, Object response) throws IOException {
        String jsonResponse = JSON.toJSONString(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 发送错误响应
     */
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        Map<String, Object> errorResponse = Map.of("error", message);
        String jsonResponse = JSON.toJSONString(errorResponse);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 发送带请求ID的错误响应
     */
    private void sendErrorWithRequestId(HttpExchange exchange, int code, String message, String requestId) throws IOException {
        Map<String, Object> errorResponse = createErrorResponse(code, message, requestId);
        String jsonResponse = JSON.toJSONString(errorResponse);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(Object data, String requestId) {
        return Map.of(
            "request_id", requestId,
            "code", 200,
            "message", "success",
            "data", data
        );
    }

    /**
     * 创建失败响应
     */
    private Map<String, Object> createErrorResponse(int code, String message, String requestId) {
        return Map.of(
            "request_id", requestId,
            "code", code,
            "message", message,
            "data", Map.of()
        );
    }
}
