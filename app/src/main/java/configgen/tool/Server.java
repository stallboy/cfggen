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
        server.createContext("/ids", this::handleIds);
        server.createContext("/record", this::handleRecord);

        server.start();

        Logger.log("server started at " + port);
    }

    private void handleSchemas(HttpExchange exchange) throws IOException {
        ServeSchema.Schema schema = ServeSchema.fromCfgValue(cfgValue, 999999);
        byte[] jsonBytes = JSON.toJSONBytes(schema);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        exchange.sendResponseHeaders(200, jsonBytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(jsonBytes);
        out.flush();
        out.close();
    }


    private void handleIds(HttpExchange exchange) throws IOException {

    }

    private void handleRecord(HttpExchange exchange) throws IOException {

    }

}
