package configgen.gen;

import configgen.ctx.*;
import configgen.editorserver.EditorServer;
import configgen.gencs.GenBytes;
import configgen.gencs.GenCs;
import configgen.gents.GenTs;
import configgen.gengo.GenGo;
import configgen.genjava.JavaData;
import configgen.genjava.GenJavaData;
import configgen.genjava.code.GenJavaCode;
import configgen.genjson.GenJson;
import configgen.genbyai.GenByAI;
import configgen.genbyai.GenTsSchema;
import configgen.genlua.GenLua;
import configgen.geni18n.TodoTermListerAndChecker;
import configgen.geni18n.GenI18nByValue;
import configgen.geni18n.GenI18nById;
import configgen.geni18n.TodoTranslator;
import configgen.mcpserver.CfgMcpServer;
import configgen.tool.*;
import configgen.util.CachedFiles;
import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Main {
    private static int usage(String reason) {
        Usage.printUsage(reason);
        return 1;
    }


    public static void main(String[] args) {
        // 先注册所有的 tools 和 generators
        registerAllProviders();

        // 如果没有参数，启动 GUI
        if (args.length == 0) {
            GuiLauncher.launch();
            return;
        }

        int ret = runWithCatch(args);
        if (ret != 0) {
            System.exit(ret);
        }
    }

    record NamedTool(String name, Tool tool) {
    }

    record NamedGenerator(String name, Generator gen) {
    }

    /**
     * 注册所有的 Tools 和 Generators
     * 这个方法需要在启动 GUI 之前调用，以便 GUI 能够获取完整的注册信息
     */
    public static void registerAllProviders() {
        Tools.addProvider("xmltocfg", XmlToCfg::new);
        if (BuildSettings.isIncludePoi()) {
            Tools.addProvider("fastexcelcheck", ComparePoiAndFastExcel::new);
        }
        Tools.addProvider("readjavadata", JavaData.ReadJavaData::new);
        Tools.addProvider("term", TodoTermListerAndChecker::new);
        Tools.addProvider("translate", TodoTranslator::new);
        Tools.addProvider("usage", Usage::new);

        Generators.addProvider("verify", GenVerifier::new);
        Generators.addProvider("search", GenValueSearcher::new);
        Generators.addProvider("i18n", GenI18nByValue::new);
        Generators.addProvider("i18nbyid", GenI18nById::new);

        Generators.addProvider("java", GenJavaCode::new);
        Generators.addProvider("javadata", GenJavaData::new);
        Generators.addProvider("cs", GenCs::new);
        Generators.addProvider("bytes", GenBytes::new);
        Generators.addProvider("lua", GenLua::new);
        Generators.addProvider("ts", GenTs::new);
        Generators.addProvider("go", GenGo::new);
        Generators.addProvider("tsschema", GenTsSchema::new);
        Generators.addProvider("json", GenJson::new);

        Generators.addProvider("server", EditorServer::new);
        Generators.addProvider("mcpserver", CfgMcpServer::new);
        Generators.addProvider("byai", GenByAI::new);
    }

    public static int runWithCatch(String[] args) {
        try {
            return run(args);
        } catch (Throwable t) {
            String newLine = System.lineSeparator();
            StringBuilder sb = new StringBuilder();

            // 定义最大遍历深度，防止循环引用导致死循环
            int MAX_DEPTH = 30;
            Throwable curr = t;
            int depth = 0;

            // --- 循环遍历异常链 ---
            while (curr != null && depth < MAX_DEPTH) {
                depth++;

                // 1. 打印当前异常的标题栏
                // 对于第一个异常打印 "Exception"，后续打印 "Caused by"
                if (depth == 1) {
                    sb.append("-------------------------异常描述-------------------------").append(newLine);
                } else {
                    sb.append("Caused by: ");
                }

                // 打印异常类型和消息
                sb.append(curr.getClass().getName());
                String msg = curr.getMessage();
                if (msg != null) {
                    sb.append(": ").append(msg);
                }
                sb.append(newLine);

                // 2. 打印当前异常的堆栈轨迹
                // 获取当前这个异常对象自己的堆栈
                StackTraceElement[] stackTrace = curr.getStackTrace();
                if (stackTrace != null) {
                    for (StackTraceElement element : stackTrace) {
                        sb.append("\tat ").append(element).append(newLine);
                    }
                }

                // 3. 处理 "Suppressed" 异常（可选，通常与 try-with-resources 相关）
                // 如果需要打印被抑制的异常，可以在这里遍历 curr.getSuppressed()

                // 4. 移动到下一个异常
                curr = curr.getCause();

                // 如果还有下一个异常，且不是第一个，加个空行分隔（可选，为了好看）
                if (curr != null) {
                    sb.append(newLine);
                }
            }

            // 4. 打印最终结果并退出
            // 建议使用 System.err 打印错误，这样方便重定向日志
            System.err.print(sb);
            return 1;
        }
    }

    private static int run(String[] args) {
        String datadir = null;
        String headRowId = System.getProperty("configgen.headrow");
        String csvDefaultEncoding = "GBK";
        String asRoot = null;
        String excelDirs = null;
        String jsonDirs = null;


        boolean usePoi = false;

        String i18nfile = null;
        String langSwitchDir = null;
        String langSwitchDefaultLang = "zh_cn";


        List<NamedTool> tools = new ArrayList<>();
        List<NamedGenerator> generators = new ArrayList<>();


        for (int i = 0; i < args.length; ++i) {
            String paramType = args[i].toLowerCase();
            switch (paramType) {
                case "-locale" -> {
                    String language = args[++i];
                    Locale locale = Locale.of(language);
                    if (!LocaleUtil.isSupported(locale)) {
                        System.err.println("Specified Locale is not supported: " + locale.toString());
                        return 1;
                    }
                    LocaleUtil.setLocale(locale);
                }
                case "-datadir" -> datadir = args[++i];
                case "-headrow" -> headRowId = args[++i];
                case "-encoding" -> csvDefaultEncoding = args[++i];

                case "-asroot" -> asRoot = args[++i];
                case "-exceldirs" -> excelDirs = args[++i];
                case "-jsondirs" -> jsonDirs = args[++i];


                case "-i18nfile" -> i18nfile = args[++i];
                case "-langswitchdir" -> langSwitchDir = args[++i];
                case "-defaultlang" -> langSwitchDefaultLang = args[++i];
                case "-v" -> Logger.setVerboseLevel(1);
                case "-vv" -> Logger.setVerboseLevel(2);
                case "-p" -> Logger.enableProfile();
                case "-pp" -> {
                    Logger.enableProfile();
                    Logger.enableProfileGc();
                }
                case "-nowarn" -> Logger.setWarningEnabled(false);
                case "-weakwarn" -> Logger.setWeakWarningEnabled(true);

                case "-tool" -> {
                    String name = args[++i];
                    Tool tool = Tools.create(name);
                    if (tool == null) {
                        return usage("-tool " + name + " UNKNOWN");
                    }
                    tools.add(new NamedTool(name, tool));
                }
                case "-gen" -> {
                    String name = args[++i];
                    Generator generator = Generators.create(name);
                    if (generator == null) {
                        return usage("-gen " + name + " UNKNOWN");
                    }
                    generators.add(new NamedGenerator(name, generator));
                }
                default -> {
                    if (BuildSettings.isIncludePoi()) {
                        if (paramType.equals("-usepoi")) {
                            usePoi = true;
                        } else {
                            return usage("unknown args " + args[i]);
                        }
                    } else {
                        return usage("unknown args " + args[i]);
                    }
                }
            }
        }

        for (NamedTool nt : tools) {
            Logger.verbose("-----tool %s", nt.tool.parameter);
            nt.tool.call();
        }

        if (datadir == null) {
            if (tools.isEmpty()) {
                return usage("");
            } else {
                return 0;
            }
        }
        Path dataDir = Paths.get(datadir);

        if (headRowId == null) {
            headRowId = "2";
        }
        HeadRow headRow = HeadRows.getById(headRowId);

        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        if (i18nfile != null && langSwitchDir != null) {
            return usage("-不能同时配置-i18nfile和-langswitchdir");
        }

        Logger.profile(String.format("start total memory %dm", Runtime.getRuntime().maxMemory() / 1024 / 1024));

        Context context = new Context(new Context.ContextCfg(dataDir, explicitDir, usePoi, headRow, csvDefaultEncoding,
                i18nfile, langSwitchDir, langSwitchDefaultLang));

        for (NamedGenerator ng : generators) {
            Logger.verbose("-----generate %s", ng.gen.parameter.toString());
            try {
                ng.gen.generate(context);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Logger.profile("generate " + ng.name);
        }

        CachedFiles.finalExit();
        Logger.profile("end");
        return 0;
    }

}