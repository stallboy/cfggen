package configgen.gen;

import configgen.ctx.*;
import configgen.editorserver.EditorServer;
import configgen.gen.ui.GuiLauncher;
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
    private static final int MAX_EXCEPTION_DEPTH = 30;

    private static int usage(String reason) {
        Usage.printUsage(reason);
        return 1;
    }

    public static void main(String[] args) {
        registerAllProviders();

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

    public static void registerAllProviders() {
        Tools.addProvider("xmltocfg", XmlToCfg::new);
        Tools.addProvider("fastexcelcheck", ComparePoiAndFastExcel::new);
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
            System.err.print(formatException(t));
            return 1;
        }
    }

    private static String formatException(Throwable t) {
        String newLine = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        Throwable curr = t;
        int depth = 0;

        while (curr != null && depth < MAX_EXCEPTION_DEPTH) {
            depth++;

            if (depth == 1) {
                sb.append("-------------------------异常描述-------------------------").append(newLine);
            } else {
                sb.append("Caused by: ");
            }

            sb.append(curr.getClass().getName());
            String msg = curr.getMessage();
            if (msg != null) {
                sb.append(": ").append(msg);
            }
            sb.append(newLine);

            StackTraceElement[] stackTrace = curr.getStackTrace();
            if (stackTrace != null) {
                for (StackTraceElement element : stackTrace) {
                    sb.append("\tat ").append(element).append(newLine);
                }
            }

            curr = curr.getCause();

            if (curr != null) {
                sb.append(newLine);
            }
        }

        return sb.toString();
    }

    private static int run(String[] args) {
        String datadir = null;
        String headRowId = System.getProperty("configgen.headrow");
        String csvDefaultEncoding = "GBK";
        String asRoot = null;
        String excelDirs = null;
        String jsonDirs = null;

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

                case "-datadir" -> datadir = args[++i];
                case "-headrow" -> headRowId = args[++i];
                case "-encoding" -> csvDefaultEncoding = args[++i];

                case "-asroot" -> asRoot = args[++i];
                case "-exceldirs" -> excelDirs = args[++i];
                case "-jsondirs" -> jsonDirs = args[++i];

                case "-i18nfile" -> i18nfile = args[++i];
                case "-langswitchdir" -> langSwitchDir = args[++i];
                case "-defaultlang" -> langSwitchDefaultLang = args[++i];

                case "-gen" -> {
                    String name = args[++i];
                    Generator generator = Generators.create(name);
                    if (generator == null) {
                        return usage("-gen " + name + " UNKNOWN");
                    }
                    generators.add(new NamedGenerator(name, generator));
                }
                default -> {
                    return usage("unknown args " + args[i]);
                }
            }
        }

        for (NamedTool nt : tools) {
            Logger.verbose("-----tool %s", nt.tool.parameter);
            nt.tool.call();
        }

        if (datadir == null) {
            if (!generators.isEmpty()) {
                return usage("-datadir is required");
            }
            return 0;
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

        Context context = new Context(new Context.ContextCfg(dataDir, explicitDir, headRow, csvDefaultEncoding,
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