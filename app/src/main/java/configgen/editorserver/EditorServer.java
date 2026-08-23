package configgen.editorserver;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.*;
import configgen.ctx.*;
import configgen.gen.GeneratorWithTag;
import configgen.gen.WatchAndPostRun;
import configgen.gen.Parameter;
import configgen.schema.TableSchemaRefGraph;
import configgen.value.SearchService;
import configgen.value.CfgValue;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import configgen.util.Logger;

import static configgen.editorserver.CheckJsonService.*;
import static configgen.editorserver.RecordService.*;
import static configgen.editorserver.RecordEditService.*;

/**
 * 为cfgeditor提供restful api
 */
public class EditorServer extends GeneratorWithTag {
    private final int port;
    private final String bindAddress;
    private final Set<String> extraAllowedOrigins;
    private final String noteCsvPath;

    // context/cfgValue/graph 三者必须配套（同一代数据），合并为单一快照，
    // 消除读handler两次读字段期间reload插入导致的跨代错配
    private volatile State state;

    private record State(Context context, CfgValue cfgValue, TableSchemaRefGraph graph) {
    }

    private HttpServer server;
    private NoteEditService noteEditService;
    private final String postRun;
    private final int waitSecondsAfterWatchEvt;

    public EditorServer(Parameter parameter) {
        super(parameter);
        port = Integer.parseInt(parameter.get("port", "3456"));
        // 默认只绑回环地址，避免配置写接口暴露给局域网；需要外部访问时显式配置 bind=0.0.0.0
        bindAddress = parameter.get("bind", "127.0.0.1");
        // 额外放行的跨域来源（逗号分隔），供编辑器部署在非本机origin时使用
        String allowOrigin = parameter.get("alloworigin", null);
        extraAllowedOrigins = allowOrigin == null ? Set.of() : Arrays.stream(allowOrigin.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
        noteCsvPath = parameter.get("note", "_note.csv");
        waitSecondsAfterWatchEvt = Integer.parseInt(parameter.get("watch", "0"));
        postRun = parameter.get("postrun", null);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        noteEditService = new NoteEditService(Path.of(noteCsvPath));
        initFromCtx(ctx);

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] %5$s %n");

        InetSocketAddress listenAddress;
        try {
            listenAddress = new InetSocketAddress(InetAddress.getByName(bindAddress), port);
        } catch (UnknownHostException e) {
            throw new IOException("无法解析bind地址: " + bindAddress, e);
        }
        server = HttpServer.create(listenAddress, 0);

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
        Logger.log("Server is started at " + listenAddress);

        // 无论是否开启watch都注册：reload换代后与编辑写操作（含同进程McpServer的写）成功后，
        // 都会经此刷新内存快照（见WatchAndPostRun/StateCoordinator）
        WatchAndPostRun.INSTANCE.registerPostRunCallback(this::initFromCtx);
        if (waitSecondsAfterWatchEvt > 0) {
            WatchAndPostRun.INSTANCE.startWatch(ctx, waitSecondsAfterWatchEvt);
            if (postRun != null) {
                WatchAndPostRun.INSTANCE.registerPostRunBat(postRun);
            }
        }
    }

    // 在WatchAndPostRun的editLock内被调用（reload换代后、编辑写成功后的统一刷新），或启动期单线程调用
    private void initFromCtx(Context newContext) {
        // 可以包含tag，这样更灵活，方便查看filter过后的数据
        // 此时所有的修改指令将返回错误 serverNotEditable
        CfgValue newCfgValue = newContext.makeValue(tag, true);
        state = new State(newContext, newCfgValue, new TableSchemaRefGraph(newCfgValue.schema()));
    }

    private void handleSchemas(HttpExchange exchange) throws IOException {
        SchemaService.Schema schema = SchemaService.fromCfgValue(state.cfgValue());
        sendResponse(exchange, schema);
    }

    private void handleNotes(HttpExchange exchange) throws IOException {
        NoteEditService.Notes notes = noteEditService.getNotes();
        sendResponse(exchange, notes);
    }

    private void handleNoteUpdate(HttpExchange exchange) throws IOException {
        if (!checkPostMethod(exchange)) {
            return;
        }

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String key = query.get("key");

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String note = new String(bytes, StandardCharsets.UTF_8).trim();
        Logger.log(note);

        NoteEditService.NoteEditResult result = noteEditService.updateNote(key, note);
        sendResponse(exchange, result);
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String q = query.get("q");
        String maxStr = query.get("max");

        int max = parseIntAndIgnoreErr(maxStr, 30);
        SearchService.SearchResult result = SearchService.search(state.cfgValue(), q, max);
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


        State st = state;
        RecordRefIdsService.RecordRefIdsResponse response = new RecordRefIdsService(st.cfgValue(), st.graph(), table, id, inDepth, outDepth, maxIds).retrieve();
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
        String noRefInStr = query.get("noRefIn");

        int depth = parseIntAndIgnoreErr(depthStr, 1);
        int maxObjs = parseIntAndIgnoreErr(maxObjsStr, 30);
        boolean in = inStr != null;
        boolean noRefIn = noRefInStr != null;

        // 根据参数确定请求类型
        RequestType requestType;
        if (noRefIn) {
            requestType = RequestType.requestUnreferenced;
        } else {
            requestType = refsStr != null ? RequestType.requestRefs : RequestType.requestRecord;
        }

        State st = state;
        RecordResponse record = new RecordService(st.cfgValue(), st.graph(), table, id, depth, in, maxObjs, requestType).retrieve();
        sendResponse(exchange, record);
    }

    private void handleRecordAddOrUpdate(HttpExchange exchange) throws IOException {
        if (!checkPostMethod(exchange)) {
            return;
        }
        // 强制JSON类型：浏览器发text/plain是简单请求可绕过预检，application/json必须预检
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonStr = new String(bytes, StandardCharsets.UTF_8);

        RecordEditResult result = editRecord(
                (ctx, cfgValue) -> RecordEditService.addOrUpdateRecord(ctx, cfgValue, table, jsonStr));
        sendResponse(exchange, result);
    }

    private void handleRecordDelete(HttpExchange exchange) throws IOException {
        if (!checkPostMethod(exchange)) {
            return;
        }

        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        String id = query.get("id");

        RecordEditResult result = editRecord(
                (ctx, cfgValue) -> RecordEditService.deleteRecord(ctx, cfgValue, table, id));
        sendResponse(exchange, result);
    }

    /**
     * 写操作统一临界区：基于当前State快照调用编辑服务，整个编辑（读快照→写文件→context换代缓存）在
     * WatchAndPostRun的editLock内，与reload、其他写（含同进程McpServer的写工具）互斥；
     * 编辑完成后由统一刷新重建State。所有写接口必须经此进入，不得自行取快照。
     */
    private RecordEditResult editRecord(BiFunction<Context, CfgValue, RecordEditService.ResultWithNewCfgValue> editCall) {
        return WatchAndPostRun.INSTANCE.runEdit(ctx -> {
            State st = state; // editLock内，快照必为当前代
            return editCall.apply(st.context(), st.cfgValue()).result();
        });
    }


    private void handlePrompt(HttpExchange exchange) throws IOException {
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        State st = state;
        PromptService.PromptResult result = PromptService.gen(st.context(), st.cfgValue(), table);
        sendResponse(exchange, result);
    }


    private void handleCheckJson(HttpExchange exchange) throws IOException {
        if (!checkPostMethod(exchange)) {
            return;
        }
        Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
        String table = query.get("table");
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String raw = new String(bytes, StandardCharsets.UTF_8);

        CheckJsonResult result = CheckJsonService.checkJson(state.cfgValue(), table, raw);
        sendResponse(exchange, result);
    }

    private void handle(String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        context.getFilters().add(logging);
    }

    // 浏览器跨站请求（CSRF/DNS rebinding）防线：带外域Origin的请求直接403。
    // 非浏览器客户端（curl、编辑器本地进程）通常不带Origin，默认放行
    private final Filter logging = new Filter() {
        @Override
        public void doFilter(HttpExchange http, Chain chain) {
            try {
                String origin = http.getRequestHeaders().getFirst("Origin");
                if (origin != null && !isOriginAllowed(origin)) {
                    Logger.log("rejected origin: " + origin);
                    sendError(http, 403, "origin not allowed");
                    return;
                }
                chain.doFilter(http);
            } catch (Throwable e) {
                Logger.log(e.toString());
                try {
                    http.sendResponseHeaders(500, -1);
                } catch (IOException ignored) {
                    // ignore
                } finally {
                    http.close();
                }
            } finally {
                Logger.log(String.format("%s %s %s",
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

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private boolean isOriginAllowed(String origin) {
        if (origin.isBlank() || origin.equalsIgnoreCase("null")) {
            return false;
        }
        String o = origin.toLowerCase(Locale.ROOT);
        for (String host : LOOPBACK_HOSTS) {
            if (o.equals("http://" + host) || o.equals("https://" + host)
                    || o.startsWith("http://" + host + ":") || o.startsWith("https://" + host + ":")) {
                return true;
            }
        }
        // cfgeditor的Tauri桌面端webview origin
        if (o.equals("tauri://localhost") || o.equals("http://tauri.localhost")) {
            return true;
        }
        return extraAllowedOrigins.contains(origin);
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 有Origin时回显（配合Credentials不能再用*），无Origin（非浏览器）用*
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin != null ? origin : "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
    }

    private static void sendOptionsResponse(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(200, -1);
        exchange.getRequestBody().close();
    }

    /**
     * 写接口只接受POST（及OPTIONS预检）。GET等子资源加载（如 img src=...）不带头，
     * 会绕过Origin校验直接触发写操作，必须按方法拒绝。
     */
    private static boolean checkPostMethod(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equals("OPTIONS")) {
            sendOptionsResponse(exchange);
            return false;
        }
        if (!method.equals("POST")) {
            sendError(exchange, 405, "method not allowed");
            return false;
        }
        return true;
    }

    private static void sendError(HttpExchange exchange, int status, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendResponse(HttpExchange exchange, Object object) throws IOException {
        byte[] jsonBytes = JSON.toJSONBytes(object);
        setCorsHeaders(exchange);

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
                // split("=")会把取值里的=切断，且未decode，中文/编码字符会解析错误
                int eq = param.indexOf('=');
                try {
                    if (eq >= 0) {
                        result.put(URLDecoder.decode(param.substring(0, eq), StandardCharsets.UTF_8),
                                URLDecoder.decode(param.substring(eq + 1), StandardCharsets.UTF_8));
                    } else if (!param.isEmpty()) {
                        result.put(URLDecoder.decode(param, StandardCharsets.UTF_8), "");
                    }
                } catch (IllegalArgumentException e) {
                    Logger.log("bad query param: " + param);
                }
            }
        }
        return result;
    }

}
