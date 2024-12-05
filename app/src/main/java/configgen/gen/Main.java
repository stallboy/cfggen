package configgen.gen;

import configgen.ctx.Context;
import configgen.data.*;
import configgen.editorserver.EditorServer;
import configgen.gencs.GenBytes;
import configgen.gencs.GenCs;
import configgen.genjava.BinaryToText;
import configgen.genjava.GenJavaData;
import configgen.genjava.code.GenJavaCode;
import configgen.genlua.GenLua;
import configgen.tool.*;
import configgen.util.CachedFiles;
import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Main {
    private static void usage(String reason) {
        System.out.println(reason);

        System.out.println("Usage: java -jar cfggen.jar [options] -datadir [dir] [options] [gens]");
        System.out.println();
        System.out.println("-----schema & data");
        System.out.println("    -datadir          " + LocaleUtil.getLocaleString("Usage.DataDir",
                "configuration data directory, must contains file:config.cfg"));
        System.out.println("    -headrow          " + LocaleUtil.getLocaleString("Usage.HeadRow",
                "csv/Excel file head row count, default 2"));
        System.out.println("    -encoding         " + LocaleUtil.getLocaleString("Usage.Encoding",
                "csv encoding, default GBK, if csv file has BOM head, use that encoding"));

        System.out.println();
        System.out.println("-----i18n support");
        System.out.println("    -i18nfile         " + LocaleUtil.getLocaleString("Usage.I18nFile",
                "replace all type=text data, must be csv file with 3 columns(table name, original, replaced). default null"));
        System.out.println("    -i18ncrlfaslf     " + LocaleUtil.getLocaleString("Usage.I18nCrLfAsLf",
                "replace \\r\\n to \\n, default false."));
        System.out.println("    -langswitchdir    " + LocaleUtil.getLocaleString("Usage.LangSwitchDir",
                "language switch support"));
        System.out.println("    -defaultlang      " + LocaleUtil.getLocaleString("Usage.DefaultLang",
                "the default language when use lang switch"));

        System.out.println();
        System.out.println("-----tools");
        System.out.println("    -verify           " + LocaleUtil.getLocaleString("Usage.Verify",
                "validate all data"));
        System.out.println("    -searchto         " + LocaleUtil.getLocaleString("Usage.SearchTo",
                "save search result to file, default stdout"));
        System.out.println("    -searchtag        " + LocaleUtil.getLocaleString("Usage.SearchTag",
                "search value with tag, default full value."));
        System.out.println("    -search           " + LocaleUtil.getLocaleString("Usage.Search",
                "enter read-eval-print-loop if no param after this"));
        ValueSearcher.printUsage("        ");
        System.out.println("    -binarytotext     " + LocaleUtil.getLocaleString("Usage.BinaryToText",
                "print table schema & data, 1/2 params. 1:javadata file, 2:table name(use startsWith to match)"));
        System.out.println("    -binarytotextloop " + LocaleUtil.getLocaleString("Usage.BinaryToTextLoop",
                "enter read-eval-print-loop, 1 param: javadata file"));
        System.out.println("    -xmltocfg         " + LocaleUtil.getLocaleString("Usage.XmlToCfg",
                "convert schema from .xml to .cfg"));
        if (BuildSettings.isIncludePoi()) {
            System.out.println("    -usepoi           " + LocaleUtil.getLocaleString("Usage.UsePoi",
                    "use poi lib to read Excel file, slow speed, default false"));
            System.out.println("    -comparepoiandfastexcel   " + LocaleUtil.getLocaleString("Usage.ComparePoiAndFastExcel",
                    "compare fastexcel lib read to poi lib read"));
        }

        System.out.println("-----options");
        System.out.println("    -v                " + LocaleUtil.getLocaleString("Usage.V",
                "verbose level 1, print statistic & warning"));
        System.out.println("    -vv               " + LocaleUtil.getLocaleString("Usage.VV",
                "verbose level 2, print extra info"));
        System.out.println("    -p                " + LocaleUtil.getLocaleString("Usage.P",
                "profiler, print memory usage & time elapsed"));
        System.out.println("    -pp               " + LocaleUtil.getLocaleString("Usage.PP",
                "profiler, gc before print memory usage"));
        System.out.println("    -nowarn           " + LocaleUtil.getLocaleString("Usage.NOWARN",
                "do not print warning"));

        System.out.println();
        System.out.println("-----" + LocaleUtil.getLocaleString("Usage.GenStart",
                "parameters in gen are separated by , and the parameter name and parameter value are separated = or :."));
        Generators.getAllProviders().forEach((k, v) -> {
                    System.out.printf("    -gen %s\n", k);
                    ParameterInfoCollector collector = ParameterInfoCollector.of(k);
                    v.create(collector);
                    collector.print();
                }
        );


        Runtime.getRuntime().exit(1);
    }

    public static void main(String[] args) throws Exception {
        try {
            main0(args);
        } catch (Throwable t) {
            String newLine = System.lineSeparator();
            StringBuilder sb = new StringBuilder();
            sb.append("-------------------------错误描述-------------------------").append(newLine);
            int stackCnt = 0;
            Throwable curr = t;
            while (curr != null && ++stackCnt < 10) {
                sb.append(curr.getMessage()).append(newLine);
                curr = curr.getCause();
            }
            sb.append("-------------------------错误堆栈-------------------------").append(newLine);
            System.out.print(sb);

            throw t;
        }
    }

    record NamedGenerator(String name, Generator gen) {
    }

    private static void main0(String[] args) throws Exception {
        Generators.addProvider("i18n", GenI18n::new);

        Generators.addProvider("java", GenJavaCode::new);
        Generators.addProvider("javadata", GenJavaData::new);

        Generators.addProvider("cs", GenCs::new);
        Generators.addProvider("bytes", GenBytes::new);

        Generators.addProvider("lua", GenLua::new);

        Generators.addProvider("server", EditorServer::new);
        Generators.addProvider("json", GenJson::new);
        Generators.addProvider("ts", GenTs::new);
        Generators.addProvider("jsonbyai", GenJsonByAI::new);

        String datadir = null;
        boolean xmlToCfg = false;
        boolean comparePoiAndFastExcel = false;
        int headRow = 2;
        boolean usePoi = false;
        String csvDefaultEncoding = "GBK";

        String i18nfile = null;
        boolean i18ncrlfaslf = false;
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

        String row = System.getProperty("configgen.headrow");
        //noinspection StatementWithEmptyBody
        if (row == null || row.equals("2")) {
        } else if (row.equals("3")) {
            headRow = 3; //第三行可以是类型信息，随便，这里不会读取这个数据，会忽略掉，也就是说类型的权威数据在xml里
        } else {
            System.err.printf("-Dconfiggen.headrow，设置为[%s], 它只能设置为2或3，不设置的话默认是2\n", row);
        }

        for (int i = 0; i < args.length; ++i) {
            String paramType = args[i].toLowerCase();
            switch (paramType) {
                case "-locale" -> {
                    String language = args[++i];
                    Locale locale = Locale.of(language);
                    if (!LocaleUtil.isSupported(locale)) {
                        System.err.println("Specified Locale is not supported: " + locale.toString());
                        return;
                    }
                    LocaleUtil.setLocale(locale);
                }
                case "-datadir" -> datadir = args[++i];
                case "-xmltocfg" -> xmlToCfg = true;
                case "-headrow" -> headRow = Integer.parseInt(args[++i]);

                case "-encoding" -> csvDefaultEncoding = args[++i];
                case "-verify" -> verify = true;
                case "-i18nfile" -> i18nfile = args[++i];
                case "-i18ncrlfaslf" -> i18ncrlfaslf = true;
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
                    if (generator == null)
                        usage("");
                    generators.add(new NamedGenerator(name, generator));
                }
                default -> {
                    if (BuildSettings.isIncludePoi()) {
                        switch (paramType) {
                            case "-usepoi" -> usePoi = true;
                            case "-comparepoiandfastexcel" -> comparePoiAndFastExcel = true;
                            default -> usage("unknown args " + args[i]);
                        }
                    } else {
                        usage("unknown args " + args[i]);
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
            return;
        }
        if (datadir == null) {
            usage("请配置-datadir");
            return;
        }

        Path dataDir = Paths.get(datadir);
        if (xmlToCfg) {
            XmlToCfg.convertAndCheck(dataDir);
            return;
        }

        if (comparePoiAndFastExcel) {
            if (BuildSettings.isIncludePoi()) {
                ReadCsv csvReader = new ReadCsv(csvDefaultEncoding);
                CfgDataReader fastDataReader = new CfgDataReader(headRow, csvReader, ReadByFastExcel.INSTANCE);
                CfgDataReader poiDataReader = new CfgDataReader(headRow, csvReader, BuildSettings.getPoiReader());
                ComparePoiAndFastExcel.compareCellData(dataDir, fastDataReader, poiDataReader);
            } else {
                usage("-comparePoiAndFastExcel，但jar里没有包含poi包");
            }
        }

        if (i18nfile != null && langSwitchDir != null) {
            usage("-不能同时配置-i18nfile和-langswitchdir");
            return;
        }

        Logger.profile(String.format("start total memory %dm", Runtime.getRuntime().maxMemory() / 1024 / 1024));

        Context context = new Context(new Context.ContextCfg(dataDir, usePoi, headRow, csvDefaultEncoding,
                i18nfile, i18ncrlfaslf, langSwitchDir, langSwitchDefaultLang));

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
            ng.gen.generate(context);
            Logger.profile("generate " + ng.name);
        }

        CachedFiles.finalExit();
        Logger.profile("end");
    }

}
