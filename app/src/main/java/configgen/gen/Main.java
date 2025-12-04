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
import configgen.i18n.TermChecker;
import configgen.i18n.GenI18nByValue;
import configgen.i18n.GenI18nById;
import configgen.i18n.TermUpdater;
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
            int ret = main0(args);
            if (ret != 0) {
                System.exit(ret);
            }
        } catch (Throwable t) {
            String newLine = System.lineSeparator();
            StringBuilder sb = new StringBuilder();
            sb.append("-------------------------错误描述-------------------------").append(newLine);
            int stackCnt = 0;
            Throwable curr = t;
            while (curr != null && ++stackCnt < 30) {
                sb.append(curr.getMessage()).append(newLine);
                curr = curr.getCause();
            }
            sb.append("-------------------------错误堆栈-------------------------").append(newLine);
            System.out.print(sb);

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
        Tools.addProvider("termcheck", TermChecker::new);
        Tools.addProvider("termupdate", TermUpdater::new);

        Generators.addProvider("verify", GenVerifier::new);
        Generators.addProvider("search", ValueSearcher.GenValueSearcher::new);
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
                        switch (paramType) {
                            case "-usepoi" -> usePoi = true;
                            default -> {
                                return usage("unknown args " + args[i]);
                            }
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
