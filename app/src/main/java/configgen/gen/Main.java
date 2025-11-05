package configgen.gen;

import configgen.ctx.*;
import configgen.data.*;
import configgen.editorserver.EditorServer;
import configgen.gencs.GenBytes;
import configgen.gencs.GenCs;
import configgen.gents.GenTs;
import configgen.gengo.GenGo;
import configgen.genjava.BinaryToText;
import configgen.genjava.GenJavaData;
import configgen.genjava.code.GenJavaCode;
import configgen.genjson.GenJson;
import configgen.genjson.GenJsonByAI;
import configgen.genjson.GenTsSchema;
import configgen.genlua.GenLua;
import configgen.i18n.TermChecker;
import configgen.i18n.GenI18nByValue;
import configgen.i18n.GenI18nById;
import configgen.i18n.GenI18nByIdTest;
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
        Logger.log(reason);

        Logger.log("Usage: java -jar cfggen.jar [options] -datadir [dir] [options] [gens]");
        Logger.log("");
        Logger.log("-----schema & data");
        Logger.log("    -datadir          " + LocaleUtil.getLocaleString("Usage.DataDir",
                "configuration data directory, must contains file:config.cfg"));
        Logger.log("    -headrow          " + LocaleUtil.getLocaleString("Usage.HeadRow",
                "csv/txt/excel file head row type, default 2"));
        Logger.log("    -encoding         " + LocaleUtil.getLocaleString("Usage.Encoding",
                "csv/txt encoding, default GBK, if csv file has BOM head, use that encoding"));
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
        Logger.log("-----tools");
        Logger.log("    -verify           " + LocaleUtil.getLocaleString("Usage.Verify",
                "validate all data"));
        Logger.log("    -searchto         " + LocaleUtil.getLocaleString("Usage.SearchTo",
                "save search result to file, default stdout"));
        Logger.log("    -searchtag        " + LocaleUtil.getLocaleString("Usage.SearchTag",
                "search value with tag, default full value."));
        Logger.log("    -search           " + LocaleUtil.getLocaleString("Usage.Search",
                "enter read-eval-print-loop if no param after this"));
        ValueSearcher.printUsage("        ");
        Logger.log("    -binarytotext     " + LocaleUtil.getLocaleString("Usage.BinaryToText",
                "print table schema & data, 1/2 params. 1:javadata file, 2:table name(use startsWith to match)"));
        Logger.log("    -binarytotextloop " + LocaleUtil.getLocaleString("Usage.BinaryToTextLoop",
                "enter read-eval-print-loop, 1 param: javadata file"));
        Logger.log("    -xmltocfg         " + LocaleUtil.getLocaleString("Usage.XmlToCfg",
                "convert schema from .xml to .cfg"));
        Logger.log("    -compareterm      " + LocaleUtil.getLocaleString("Usage.CompareTerm",
                "check -i18nfile (2) compatible with term"));
        if (BuildSettings.isIncludePoi()) {
            Logger.log("    -usepoi           " + LocaleUtil.getLocaleString("Usage.UsePoi",
                    "use poi lib to read Excel file, slow speed, default false"));
            Logger.log("    -comparepoiandfastexcel   " + LocaleUtil.getLocaleString("Usage.ComparePoiAndFastExcel",
                    "compare fastexcel lib read to poi lib read"));
        }

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
        Logger.log("-----" + LocaleUtil.getLocaleString("Usage.GenStart",
                "parameters in gen are separated by , and the parameter name and parameter value are separated = or :."));
        Generators.getAllProviders().forEach((k, v) -> {
                    Logger.log("    -gen %s", k);
                    ParameterInfoCollector parameter = ParameterInfoCollector.of(k);
                    v.create(parameter);
                    parameter.print();
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

    record NamedGenerator(String name, Generator gen) {
    }

    public static int main0(String[] args) {
        Generators.addProvider("i18n", GenI18nByValue::new);
        Generators.addProvider("i18nbyid", GenI18nById::new);
        Generators.addProvider("i18nbyidtest", GenI18nByIdTest::new);

        Generators.addProvider("java", GenJavaCode::new);
        Generators.addProvider("javadata", GenJavaData::new);

        Generators.addProvider("cs", GenCs::new);
        Generators.addProvider("bytes", GenBytes::new);
        Generators.addProvider("lua", GenLua::new);
        Generators.addProvider("ts", GenTs::new);
        Generators.addProvider("go", GenGo::new);

        Generators.addProvider("server", EditorServer::new);
        Generators.addProvider("tsschema", GenTsSchema::new);
        Generators.addProvider("json", GenJson::new);
        Generators.addProvider("jsonbyai", GenJsonByAI::new);

        String datadir = null;
        String headRowId = System.getProperty("configgen.headrow");
        String csvDefaultEncoding = "GBK";
        String asRoot = null;
        String excelDirs = null;
        String jsonDirs = null;


        boolean xmlToCfg = false;
        String compareTerm = null;
        boolean comparePoiAndFastExcel = false;
        boolean usePoi = false;

        String i18nfile = null;
        String langSwitchDir = null;
        String langSwitchDefaultLang = "zh_cn";

        boolean verify = false;

        List<NamedGenerator> generators = new ArrayList<>();

        boolean binaryToTextLoop = false;
        String binaryToTextFile = null;
        String match = null;

        String searchTo = null;
        String searchTag = null;
        List<String> searchParam = null;

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

                case "-xmltocfg" -> xmlToCfg = true;
                case "-verify" -> verify = true;
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
                case "-binarytotext" -> {
                    binaryToTextFile = args[++i];
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        match = args[++i];
                    }
                }
                case "-binarytotextloop" -> {
                    binaryToTextLoop = true;
                    binaryToTextFile = args[++i];
                }
                case "-compareterm" -> compareTerm = args[++i];

                case "-searchto" -> searchTo = args[++i];
                case "-searchtag" -> searchTag = args[++i];

                case "-search" -> {
                    searchParam = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        searchParam.add(args[++i]);
                    }
                }
                case "-gen" -> {
                    String name = args[++i];
                    Generator generator = Generators.create(name);
                    if (generator == null) {
                        return usage("");
                    }
                    generators.add(new NamedGenerator(name, generator));
                }
                default -> {
                    if (BuildSettings.isIncludePoi()) {
                        switch (paramType) {
                            case "-usepoi" -> usePoi = true;
                            case "-comparepoiandfastexcel" -> comparePoiAndFastExcel = true;
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

        if (binaryToTextFile != null) {
            if (binaryToTextLoop) {
                BinaryToText.loop(binaryToTextFile);
            } else {
                BinaryToText.parse(binaryToTextFile, match);
            }
            return 0;
        }

        if (compareTerm != null) {
            if (i18nfile == null) {
                return usage("请配置-i18nfile");

            }
            TermChecker.compare(Path.of(i18nfile), Path.of(compareTerm));
            return 0;
        }

        if (datadir == null) {
            return usage("请配置-datadir");
        }

        Path dataDir = Paths.get(datadir);
        if (xmlToCfg) {
            XmlToCfg.convertAndCheck(dataDir);
            return 0;
        }

        if (headRowId == null) {
            headRowId = "2";
        }
        HeadRow headRow = HeadRows.getById(headRowId);

        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);
        if (comparePoiAndFastExcel) {
            // 测试fastexcel和poi读取数据的一致性
            if (BuildSettings.isIncludePoi()) {
                ComparePoiAndFastExcel.compare(dataDir, explicitDir, csvDefaultEncoding, headRow);
            } else {
                return usage("-comparePoiAndFastExcel，但jar里没有包含poi包");
            }
        }

        if (i18nfile != null && langSwitchDir != null) {
            return usage("-不能同时配置-i18nfile和-langswitchdir");

        }

        Logger.profile(String.format("start total memory %dm", Runtime.getRuntime().maxMemory() / 1024 / 1024));

        Context context = new Context(new Context.ContextCfg(dataDir, explicitDir, usePoi, headRow, csvDefaultEncoding,
                i18nfile, langSwitchDir, langSwitchDefaultLang));

        if (searchParam != null) {
            ValueSearcher searcher = new ValueSearcher(context.makeValue(searchTag), searchTo);
            if (searchParam.isEmpty()) {
                searcher.loop();
            } else {
                searcher.search(searchParam.getFirst(), searchParam.subList(1, searchParam.size()));
            }
            searcher.close();
        }

        if (verify) {
            Logger.verbose("-----start verify");
            context.makeValue(null);
        }

        for (NamedGenerator ng : generators) {
            Logger.verbose("-----generate " + ng.gen.parameter);
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
