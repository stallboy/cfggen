package configgen.editorserver;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.*;
import configgen.ctx.*;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Generators;
import configgen.gen.Parameter;
import configgen.schema.TableSchemaRefGraph;
import configgen.genjson.AICfg;
import configgen.value.CfgValue;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

public class EditorServer extends GeneratorWithTag {
    private final int port;
    private final String noteCsvPath;
    private final String aiCfgFn;

    private Context context;
    private volatile CfgValue cfgValue;  // 引用可以被改变，指向不同的CfgValue
    private volatile TableSchemaRefGraph graph;

    private HttpServer server;
    private NoteEditService noteEditService;
    private AICfg aiCfg;
    private Path aiDir;
    private final String postRun;
    private final int waitSecondsAfterWatchEvt;
    private Thread postRunThread;

    public EditorServer(Parameter parameter) {
        super(parameter);
        port = Integer.parseInt(parameter.get("port", "3456"));
        noteCsvPath = parameter.get("note", "_note.csv");
        aiCfgFn = parameter.get("aicfg", null);
        waitSecondsAfterWatchEvt = Integer.parseInt(parameter.get("watch", "0"));
        postRun = parameter.get("postrun", null);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (aiCfgFn != null) {
            aiDir = Path.of(aiCfgFn).getParent();
            aiCfg = AICfg.readFromFile(aiCfgFn);
        }

        noteEditService = new NoteEditService(Path.of(noteCsvPath));
        this.context = ctx;
        initFromCtx();

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
        handle("/recordRefIds", this::handleRecordRefIds);

        handle("/record", this::handleRecord);
        handle("/recordAddOrUpdate", this::handleRecordAddOrUpdate);
        handle("/recordDelete", this::handleRecordDelete);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        logger.info("Server is started at " + listenAddr);

        if (waitSecondsAfterWatchEvt > 0) {
            Watcher watcher = new Watcher(context.getSourceStructure().getRootDir(), context.getContextCfg().explicitDir());
            WaitWatcher waitWatcher = new WaitWatcher(watcher, this::reloadData, waitSecondsAfterWatchEvt);
            waitWatcher.start();
            watcher.start();
            logger.info("file change watcher started");
        }
    }

    private void initFromCtx() {
        cfgValue = context.makeValue(tag, true);
        graph = new TableSchemaRefGraph(cfgValue.schema());
    }

    private void reloadData() {
        DirectoryStructure newStructure = context.getSourceStructure().reload();
        if (newStructure.lastModifiedEquals(context.getSourceStructure())) {
            configgen.util.Logger.verbose("lastModified not change");
            return;
        }
        try {
            this.context = new Context(context.getContextCfg(), newStructure);
            initFromCtx();
            logger.info("reload ok");
            tryPostRun();
        } catch (Exception e) {
            logger.info("reload ignored");
        }
    }

    private void tryPostRun() {
        if (postRun == null) {
            return;
        }

        if (postRunThread != null) {
            try {
                postRunThread.join();
            } catch (InterruptedException e) {
                logger.warning("postrun thread join interrupted: " + e.getMessage());
            }

        }
        postRunThread = Thread.startVirtualThread(() -> {
            try {
                String genPrefix = null;
                if (postRun.endsWith(".bat")) {
                    genPrefix = ":: -gen ";
                } else if (postRun.endsWith(".sh")) {
                    genPrefix = "# -gen ";
                }
                if (genPrefix != null) {
                    for (String line : Files.readAllLines(Path.of(postRun))) {
                        if (line.startsWith(genPrefix)) {
                            String parameter = line.substring(genPrefix.length());
                            Generator generator = Generators.create(parameter);
                            if (generator != null) {
                                logger.info("-gen " + parameter);
                                generator.generate(context);
                            }
                        } else {
                            break;
                        }
                    }
                }

                String[] cmds = new String[]{postRun};
                Process process = Runtime.getRuntime().exec(cmds);

                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    logger.info("postrun output: " + line);
                }
                if (process.waitFor(10, TimeUnit.SECONDS)) {
                    logger.info("postrun ok!");
                } else {
                    logger.info("postrun timeout");
                }
                in.close();
            } catch (IOException e) {
                logger.warning("postrun err: " + e.getMessage());
            } catch (InterruptedException e) {
                logger.warning("postrun interrupted: " + e.getMessage());
            }
        });
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

        int max = parseIntAndIgnoreErr(maxStr, 30);
        SearchService.SearchResult result = SearchService.search(cfgValue, q, max);
        sendResponse(exchange, result);
    }

    private static int parseIntAndIgnoreErr(String str, int def) {
        int value = def;
        if (str != null) {
            try {
                value = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return value;
    }


    private void handleRecordRefIds(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        String id = query.get("id");
        String inStr = query.get("in");
        String outStr = query.get("out");
        String maxIdsStr = query.get("maxIds");

        int inDepth = parseIntAndIgnoreErr(inStr, 1);
        int outDepth = parseIntAndIgnoreErr(outStr, 1);
        int maxIds = parseIntAndIgnoreErr(maxIdsStr, 30);


        RecordRefIdsService.RecordRefIdsResponse response = new RecordRefIdsService(cfgValue, graph, table, id, inDepth, outDepth, maxIds).retrieve();
        sendResponse(exchange, response);
    }

    private void handleRecord(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        String id = query.get("id");
        String depthStr = query.get("depth");
        String maxObjsStr = query.get("maxObjs");
        String inStr = query.get("in");
        String refsStr = query.get("refs");

        int depth = parseIntAndIgnoreErr(depthStr, 1);
        int maxObjs = parseIntAndIgnoreErr(maxObjsStr, 30);
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
            RecordEditService service = new RecordEditService(cfgValue, context);
            result = service.addOrUpdateRecord(table, jsonStr);
            if (result.resultCode() == addOk || result.resultCode() == updateOk) {
                cfgValue = service.newCfgValue();
                ok = true;
            }
        }

        if (ok) {
            tryPostRun();
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
        boolean ok = false;
        synchronized (this) {
            RecordEditService service = new RecordEditService(cfgValue, context);
            result = service.deleteRecord(table, id);
            if (result.resultCode() == deleteOk) {
                cfgValue = service.newCfgValue();
                ok = true;
            }
        }
        if (ok) {
            tryPostRun();
        }

//        logger.info(result.toString());
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
