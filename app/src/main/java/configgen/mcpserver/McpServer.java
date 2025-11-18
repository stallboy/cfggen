package configgen.mcpserver;

import com.sun.net.httpserver.*;
import configgen.ctx.Context;
import configgen.ctx.DirectoryStructure;
import configgen.ctx.WaitWatcher;
import configgen.ctx.Watcher;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.genjson.AICfg;
import configgen.mcpserver.handlers.*;
import configgen.mcpserver.services.*;
import configgen.mcpserver.tools.*;
import configgen.util.Logger;
import configgen.value.CfgValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * MCP (Model Context Protocol) 服务器实现
 * 提供配置数据的标准化访问接口
 */
public class McpServer extends GeneratorWithTag {

    private final String aiCfgFn;
    private final int port;
    private Path aiDir;
    private AICfg aiCfg;
    private Context context;
    private CfgValue cfgValue;
    private HttpServer server;
    private long startTime;
    private final int waitSecondsAfterWatchEvt;

    // 服务组件
    private ResponseFormatter responseFormatter;
    private DataQueryService dataQueryService;
    private DataUpdateService dataUpdateService;
    private SchemaService schemaService;
    private ToolRegistry toolRegistry;

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

        // 初始化服务组件
        initializeServices();

        // 调试schema状态
        schemaService.debugSchemaState();

        System.gc();

        startMcpServer();
    }

    /**
     * 初始化服务组件
     */
    private void initializeServices() {
        responseFormatter = new ResponseFormatter();
        dataQueryService = new DataQueryService(cfgValue);
        dataUpdateService = new DataUpdateService(context, cfgValue);
        schemaService = new SchemaService(context);

        // 初始化工具注册表
        toolRegistry = new ToolRegistry();
        toolRegistry.registerTool(McpConstants.TOOL_SCHEMA_QUERY,
            new SchemaQueryTool(cfgValue, schemaService));
        toolRegistry.registerTool(McpConstants.TOOL_DATA_QUERY,
            new DataQueryTool(dataQueryService));
        toolRegistry.registerTool(McpConstants.TOOL_DATA_UPDATE,
            new DataUpdateTool(dataUpdateService));
        toolRegistry.registerTool(McpConstants.TOOL_DATA_REMOVE,
            new DataRemoveTool(dataUpdateService));
    }

    /**
     * 启动MCP服务器
     */
    private void startMcpServer() throws IOException {
        startTime = System.currentTimeMillis();
        InetSocketAddress listenAddr = new InetSocketAddress(port);
        server = HttpServer.create(listenAddr, 0);

        // 注册处理器
        registerHandlers();

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        Logger.log(String.format("MCP Server started on port %d", port));
        Logger.log("Available tables: " + cfgValue.vTableMap().keySet());
        Logger.log("Registered endpoints: " +
            McpConstants.INITIALIZE_PATH + ", " +
            McpConstants.HEALTH_PATH + ", " +
            McpConstants.TOOLS_LIST_PATH + ", " +
            McpConstants.TOOLS_CALL_PATH + ", " +
            McpConstants.RESOURCES_LIST_PATH + ", " +
            McpConstants.RESOURCES_READ_PATH);

        if (waitSecondsAfterWatchEvt > 0) {
            Watcher watcher = new Watcher(context.getSourceStructure().getRootDir(), context.getContextCfg().explicitDir());
            WaitWatcher waitWatcher = new WaitWatcher(watcher, this::reloadData, waitSecondsAfterWatchEvt * 1000);
            waitWatcher.start();
            watcher.start();
            Logger.log("file change watcher started");
        }
    }

    /**
     * 注册所有处理器
     */
    private void registerHandlers() {
        // 创建处理器实例
        InitializeHandler initializeHandler = new InitializeHandler(responseFormatter);
        HealthHandler healthHandler = new HealthHandler(responseFormatter, cfgValue, startTime);
        ToolsListHandler toolsListHandler = new ToolsListHandler(responseFormatter);
        ToolsCallHandler toolsCallHandler = new ToolsCallHandler(responseFormatter, toolRegistry);
        ResourcesListHandler resourcesListHandler = new ResourcesListHandler(responseFormatter, cfgValue);
        ResourcesReadHandler resourcesReadHandler = new ResourcesReadHandler(responseFormatter, dataQueryService);

        // 注册处理器
        registerHandler(McpConstants.INITIALIZE_PATH, initializeHandler);
        registerHandler(McpConstants.HEALTH_PATH, healthHandler);
        registerHandler(McpConstants.TOOLS_LIST_PATH, toolsListHandler);
        registerHandler(McpConstants.TOOLS_CALL_PATH, toolsCallHandler);
        registerHandler(McpConstants.RESOURCES_LIST_PATH, resourcesListHandler);
        registerHandler(McpConstants.RESOURCES_READ_PATH, resourcesReadHandler);
    }

    /**
     * 注册处理器
     */
    private void registerHandler(String path, McpHandler handler) {
        try {
            HttpContext context = server.createContext(path, handler::handle);
            context.getFilters().add(createLoggingFilter());
            Logger.log("Successfully registered endpoint: " + path);
        } catch (Exception e) {
            Logger.verbose("Failed to register endpoint " + path + ": " + e.getMessage());
        }
    }

    /**
     * 创建日志过滤器
     */
    private Filter createLoggingFilter() {
        return new Filter() {
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
    }

    /**
     * 重新加载数据
     */
    private void reloadData() {
        DirectoryStructure newStructure = context.getSourceStructure().reload();
        if (newStructure.lastModifiedEquals(context.getSourceStructure())) {
            configgen.util.Logger.verbose("lastModified not change");
            return;
        }
        try {
            this.context = new Context(context.getContextCfg(), newStructure);
            this.cfgValue = context.makeValue();
            // 重新初始化服务
            initializeServices();
            Logger.log("reload ok");
        } catch (Exception e) {
            Logger.log("reload ignored");
        }
    }
}