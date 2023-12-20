package configgen.editorserver;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.*;
import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.TableSchemaRefGraph;
import configgen.value.CfgValue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static configgen.editorserver.RecordEditService.ResultCode.*;
import static configgen.editorserver.RecordService.*;
import static configgen.editorserver.RecordEditService.*;

public class EditorServer extends Generator {
    private final int port;

    private Path dataDir;
    private volatile CfgValue cfgValue;  // 可能会被改变
    private TableSchemaRefGraph graph;
    private HttpServer server;


    public EditorServer(Parameter parameter) {
        super(parameter);
        port = Integer.parseInt(parameter.get("port", "3456"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        dataDir = ctx.dataDir();
        cfgValue = ctx.makeValue(tag);
        graph = new TableSchemaRefGraph(cfgValue.schema());
        System.gc();
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] %5$s %n");

        InetSocketAddress listenAddr = new InetSocketAddress(port);
        server = HttpServer.create(listenAddr, 0);

        handle("/schemas", this::handleSchemas);
        handle("/record", this::handleRecord);
        handle("/search", this::handleSearch);

        handle("/recordAddOrUpdate", this::handleRecordAddOrUpdate);
        handle("/recordDelete", this::handleRecordDelete);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        logger.info("Server is started at " + listenAddr);
    }


    private void handleSchemas(HttpExchange exchange) throws IOException {
        SchemaService.Schema schema = SchemaService.fromCfgValue(cfgValue);
        sendResponse(exchange, schema);
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String q = query.get("q");
        String maxStr = query.get("max");

        int max = 30;
        if (maxStr != null) {
            try {
                max = Integer.parseInt(maxStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        SearchService.SearchResult result = SearchService.search(cfgValue, q, max);
        sendResponse(exchange, result);
    }

    private void handleRecord(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        String id = query.get("id");
        String depthStr = query.get("depth");
        String maxObjsStr = query.get("maxObjs");
        String inStr = query.get("in");
        String refsStr = query.get("refs");

        int depth = 1;
        if (depthStr != null) {
            try {
                depth = Integer.parseInt(depthStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        int maxObjs = 30;
        if (depthStr != null) {
            try {
                maxObjs = Integer.parseInt(maxObjsStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        boolean in = inStr != null;

        RequestType requestType = refsStr != null ? RequestType.requestRefs : RequestType.requestRecord;
        RecordResponse record = new RecordService(cfgValue, graph, table, id, depth, in, maxObjs, requestType).retrieve();
        sendResponse(exchange, record);
    }

    private void handleRecordAddOrUpdate(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            sendOptionsResponse(exchange);
            return;
        }

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonStr = new String(bytes, StandardCharsets.UTF_8);
        logger.info(jsonStr);

        RecordEditResult result;
        synchronized (this) {
            RecordEditService service = new RecordEditService(dataDir, cfgValue);
            result = service.addOrUpdateRecord(table, jsonStr);
            if (result.resultCode() == addOk || result.resultCode() == updateOk) {
                cfgValue = service.newCfgValue();
            }
        }

        logger.info(result.toString());
        sendResponse(exchange, result);
    }

    private void handleRecordDelete(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            sendOptionsResponse(exchange);
            return;
        }

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        String id = query.get("id");

        RecordEditResult result;
        synchronized (this) {
            RecordEditService service = new RecordEditService(dataDir, cfgValue);
            result = service.deleteRecord(table, id);
            if (result.resultCode() == deleteOk) {
                cfgValue = service.newCfgValue();
            }
        }
        logger.info(result.toString());
        sendResponse(exchange, result);
    }

    private void handle(String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        context.getFilters().add(logging);
    }

    private static final Logger logger = Logger.getLogger("http");
    private static final Filter logging = new Filter() {
        @Override
        public void doFilter(HttpExchange http, Chain chain) throws IOException {
            try {
                chain.doFilter(http);
            } finally {
                logger.info(String.format("%s %s %s",
                        http.getRequestMethod(),
                        http.getRequestURI(),
                        http.getRemoteAddress()));
            }
        }

        @Override
        public String description() {
            return "logging";
        }
    };

    private static void sendOptionsResponse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "X-PINGOTHER, Content-Type");
        exchange.sendResponseHeaders(200, -1);
        exchange.getRequestBody().close();
    }

    private static void sendResponse(HttpExchange exchange, Object object) throws IOException {
        byte[] jsonBytes = JSON.toJSONBytes(object);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "X-PINGOTHER, Content-Type");

        exchange.sendResponseHeaders(200, jsonBytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(jsonBytes);
        out.flush();
        out.close();
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    result.put(pair[0], pair[1]);
                } else {
                    result.put(pair[0], "");
                }
            }
        }
        return result;
    }

}
