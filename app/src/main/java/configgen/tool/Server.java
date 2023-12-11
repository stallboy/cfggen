package configgen.tool;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.Logger;
import configgen.value.CfgValue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class Server extends Generator {
    private final int port;
    private CfgValue cfgValue;

    public Server(Parameter parameter) {
        super(parameter);
        port = Integer.parseInt(parameter.get("port", "3456"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        cfgValue = ctx.makeValue(tag);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/schemas", this::handleSchemas);
        server.createContext("/record", this::handleRecord);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.start();

        Logger.log("server started at " + port);
    }

    private void handleSchemas(HttpExchange exchange) throws IOException {
        System.out.println("/schemas");
        ServeSchema.Schema schema = ServeSchema.fromCfgValue(cfgValue, 999999);
        sendResponse(exchange, schema);
    }

    private static void sendResponse(HttpExchange exchange, Object object) throws IOException {
        byte[] jsonBytes = JSON.toJSONBytes(object);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, jsonBytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(jsonBytes);
        out.flush();
        out.close();
    }

    private void handleRecord(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        String id = query.get("id");
        ServeRecord.TableRecord record = ServeRecord.getRecord(cfgValue, table, id);
        sendResponse(exchange, record);
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
