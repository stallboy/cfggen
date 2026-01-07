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
        if (reason != null && !reason.isBlank()) {
            Logger.log(reason);
        }

        Logger.log("Usage: java -jar cfggen.jar [tools] -datadir [dir] [options] [gens]");
        Logger.log("");
        Logger.log("-----schema & data");
        Logger.log("    -datadir          " + LocaleUtil.getLocaleString("Usage.DataDir",
                "configuration data directory, must contains file:config.cfg"));
        Logger.log("    -headrow          " + LocaleUtil.getLocaleString("Usage.HeadRow",
                "csv/txt/excel file head row type, default 2"));
        Logger.log("    -encoding         " + LocaleUtil.getLocaleString("Usage.Encoding",
                "csv/txt encoding, default GBK, if csv file has BOM head, use that encoding"));
        if (BuildSettings.isIncludePoi()) {
            Logger.log("    -usepoi           " + LocaleUtil.getLocaleString("Usage.UsePoi",
                    "use poi lib to read Excel file, slow speed, default false"));
        }

        Logger.log("    -asroot           " + LocaleUtil.getLocaleString("Usage.AsRoot",
                "ExplicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map， default null, can be 'ClientTables:noserver,PublicTables,ServerTables:noclient'"));
        Logger.log("    -exceldirs        " + LocaleUtil.getLocaleString("Usage.ExcelDirs",
                "ExplicitDir.excelFileDirs， default null"));
        Logger.log("    -jsondirs         " + LocaleUtil.getLocaleString("Usage.JsonDirs",
                "ExplicitDir.jsonFileDirs， default null"));


        Logger.log("");
        Logger.log("-----i18n support");
        Logger.log("    -i18nfile         " + LocaleUtil.getLocaleString("Usage.I18nFile",
                "two choices: 1,csv file use original str as Id per table. 2,directory,has multiply xlsx file and use pk&fieldChain as Id per table. default null"));
        Logger.log("    -langswitchdir    " + LocaleUtil.getLocaleString("Usage.LangSwitchDir",
                "language switch support"));
        Logger.log("    -defaultlang      " + LocaleUtil.getLocaleString("Usage.DefaultLang",
                "the default language when use lang switch"));

        Logger.log("");
        Logger.log("-----options");
        Logger.log("    -v                " + LocaleUtil.getLocaleString("Usage.V",
                "verbose level 1, print statistic & warning"));
        Logger.log("    -vv               " + LocaleUtil.getLocaleString("Usage.VV",
                "verbose level 2, print extra info"));
        Logger.log("    -p                " + LocaleUtil.getLocaleString("Usage.P",
                "profiler, print memory usage & time elapsed"));
        Logger.log("    -pp               " + LocaleUtil.getLocaleString("Usage.PP",
                "profiler, gc before print memory usage"));
        Logger.log("    -nowarn           " + LocaleUtil.getLocaleString("Usage.NOWARN",
                "do not print warning"));
        Logger.log("    -weakwarn         " + LocaleUtil.getLocaleString("Usage.WEAKWARN",
                "print weak warning"));

        Logger.log("");
        Logger.log("-----" + LocaleUtil.getLocaleString("Usage.ToolGenStart",
                "parameters in tool/gen are separated by , and the parameter name and parameter value are separated = or :."));

        Logger.log("");
        Logger.log("-----tools");
        Tools.getAllProviders().forEach((k, v) -> {
                    ParameterInfoCollector info = new ParameterInfoCollector("tool", k);
                    v.create(info);
                    info.print();
                }
        );

        Logger.log("");
        Logger.log("-----generators");
        Generators.getAllProviders().forEach((k, v) -> {
                    ParameterInfoCollector info = new ParameterInfoCollector("gen", k);
                    v.create(info);
                    info.print();
                }
        );

        return 1;
    }


    public static void main(String[] args) {
        try {
            int ret = main0(args); // 假设 main0 是实际的业务入口
            if (ret != 0) {
                System.exit(ret);
            }
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

            System.exit(1);
        }
    }

    record NamedTool(String name, Tool tool) {
    }

    record NamedGenerator(String name, Generator gen) {
    }

    public static int main0(String[] args) {
        Tools.addProvider("xmltocfg", XmlToCfg::new);
        if (BuildSettings.isIncludePoi()) {
            Tools.addProvider("fastexcelcheck", ComparePoiAndFastExcel::new);
        }
        Tools.addProvider("readjavadata", JavaData.ToolJavaData::new);
        Tools.addProvider("term", TodoTermListerAndChecker::new);
        Tools.addProvider("translate", TodoTranslator::new);

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