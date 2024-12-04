package configgen.editorserver;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.*;
import configgen.ctx.*;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.genjava.GenJavaData;
import configgen.schema.TableSchemaRefGraph;
import configgen.tool.AICfg;
import configgen.value.CfgValue;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static configgen.editorserver.CheckJsonService.*;
import static configgen.editorserver.RecordEditService.ResultCode.*;
import static configgen.editorserver.RecordService.*;
import static configgen.editorserver.RecordEditService.*;

public class EditorServer extends Generator {
    private final int port;
    private final String noteCsvPath;
    private final String aiCfgFn;

    private Context.ContextCfg contextCfg;
    private volatile DirectoryStructure sourceStructure;
    private volatile LangSwitch langSwitch;
    private volatile CfgValue cfgValue;  // 引用可以被改变，指向不同的CfgValue
    private volatile TableSchemaRefGraph graph;

    private HttpServer server;
    private NoteEditService noteEditService;
    private AICfg aiCfg;
    private Path aiDir;
    private final String postRun;
    private final String postRunJavaData;
    private final int waitSecondsAfterWatchEvt;

    public EditorServer(Parameter parameter) {
        super(parameter);
        port = Integer.parseInt(parameter.get("port", "3456", "为cfgeditor.exe提供服务的端口"));
        noteCsvPath = parameter.get("note", "_note.csv");
        aiCfgFn = parameter.get("aicfg", null, "llm大模型选择，需要兼容openai的api");
        postRun = parameter.get("postrun", null, "可以是个xx.bat，用于自动提交服务器及时生效");
        postRunJavaData = parameter.get("postrunjavadata", "configdata.zip", "如果设置了postrun，增加或更新json后，会先生成javadata文件，然后运行postrun");
        waitSecondsAfterWatchEvt = Integer.parseInt(parameter.get("watch", "0", "如x>0，则表示x秒后自动重载配置"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (aiCfgFn != null) {
            aiDir = Path.of(aiCfgFn).getParent();
            aiCfg = AICfg.readFromFile(aiCfgFn);
        }

        noteEditService = new NoteEditService(Path.of(noteCsvPath));

        contextCfg = ctx.getContextCfg();
        initFromCtx(ctx);

        System.gc();
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] %5$s %n");

        InetSocketAddress listenAddr = new InetSocketAddress(port);
        server = HttpServer.create(listenAddr, 0);

        handle("/schemas", this::handleSchemas);
        handle("/notes", this::handleNotes);
        handle("/noteUpdate", this::handleNoteUpdate);

        handle("/search", this::handleSearch);
        handle("/prompt", this::handlePrompt);
        handle("/checkJson", this::handleCheckJson);

        handle("/record", this::handleRecord);
        handle("/recordAddOrUpdate", this::handleRecordAddOrUpdate);
        handle("/recordDelete", this::handleRecordDelete);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        logger.info("Server is started at " + listenAddr);

        if (waitSecondsAfterWatchEvt > 0) {
            Watcher watcher = new Watcher(sourceStructure.getRootDir());
            WaitWatcher waitWatcher = new WaitWatcher(watcher, this::reloadData, waitSecondsAfterWatchEvt);
            waitWatcher.start();
            watcher.start();
            logger.info("file change watcher started");
        }
    }

    private void initFromCtx(Context ctx) {
        sourceStructure = ctx.getSourceStructure();
        langSwitch = ctx.nullableLangSwitch();
        cfgValue = ctx.makeValue(tag, true);
        graph = new TableSchemaRefGraph(cfgValue.schema());
    }

    private void reloadData() {
        DirectoryStructure newStructure = new DirectoryStructure(sourceStructure.getRootDir());
        if (newStructure.lastModifiedEquals(sourceStructure)) {
            configgen.util.Logger.verbose("lastModified not change");
            return;
        }
        Context newCtx;
        try {
            newCtx = new Context(contextCfg, newStructure);
            initFromCtx(newCtx);
            logger.info("reload ok");
        } catch (Exception e) {
            logger.info("reload ignored");
        }
    }

    private void handleSchemas(HttpExchange exchange) throws IOException {
        SchemaService.Schema schema = SchemaService.fromCfgValue(cfgValue);
        sendResponse(exchange, schema);
    }

    private void handleNotes(HttpExchange exchange) throws IOException {
        NoteEditService.Notes notes = noteEditService.getNotes();
        sendResponse(exchange, notes);
    }

    private void handleNoteUpdate(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            sendOptionsResponse(exchange);
            return;
        }

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String key = query.get("key");

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String note = new String(bytes, StandardCharsets.UTF_8).trim();
        logger.info(note);

        NoteEditService.NoteEditResult result = noteEditService.updateNote(key, note);
        sendResponse(exchange, result);
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
//        logger.info(jsonStr);

        RecordEditResult result;
        boolean ok = false;
        synchronized (this) {
            RecordEditService service = new RecordEditService(cfgValue, sourceStructure);
            result = service.addOrUpdateRecord(table, jsonStr);
            if (result.resultCode() == addOk || result.resultCode() == updateOk) {
                cfgValue = service.newCfgValue();
                ok = true;
            }
        }

        if (ok && postRun != null) {
            Thread.startVirtualThread(() -> {
                try {
                    GenJavaData.generateToFile(cfgValue, langSwitch, new File(postRunJavaData));
                    // "cmd", "/c", "start", "/B",
                    String[] cmds = new String[]{postRun};
                    Process process = Runtime.getRuntime().exec(cmds);

                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                    process.waitFor(10, TimeUnit.SECONDS);
                    System.out.println("postrun ok!");
                    in.close();
                } catch (IOException e) {
                    logger.warning("postrun err: " + e.getMessage());
                } catch (InterruptedException e) {
                    logger.warning("postrun interrupted: " + e.getMessage());
                }
            });
        }

//        logger.info(result.toString());
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
            RecordEditService service = new RecordEditService(cfgValue, sourceStructure);
            result = service.deleteRecord(table, id);
            if (result.resultCode() == deleteOk) {
                cfgValue = service.newCfgValue();
            }
        }
        logger.info(result.toString());
        sendResponse(exchange, result);
    }


    private void handlePrompt(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        PromptService.PromptResult result = PromptService.gen(cfgValue, aiCfg, aiDir, table);
        sendResponse(exchange, result);
    }


    private void handleCheckJson(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            sendOptionsResponse(exchange);
            return;
        }
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String raw = new String(bytes, StandardCharsets.UTF_8);

        CheckJsonResult result = CheckJsonService.checkJson(cfgValue, table, raw);
//        logger.info(result.toString());
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(200, -1);
        exchange.getRequestBody().close();
    }

    private static void sendResponse(HttpExchange exchange, Object object) throws IOException {
        byte[] jsonBytes = JSON.toJSONBytes(object);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");

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
