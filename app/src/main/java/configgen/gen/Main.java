package configgen.gen;

import configgen.gencs.GenBytes;
import configgen.gencs.GenCs;
import configgen.genjava.BinaryToText;
import configgen.genjava.GenJavaData;
import configgen.genjava.code.GenJavaCode;
import configgen.genlua.GenLua;
import configgen.tool.GenI18n;
import configgen.tool.XmlToCfg;
import configgen.util.Localize;
import configgen.util.Logger;
import configgen.tool.ValueSearcher;
import configgen.util.CachedFiles;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Main {
    private static void usage(String reason) {
        System.out.println(reason);

        System.out.println("Usage: java --enable-preview -jar configgen.jar [options] -datadir [dir] [options] [gens]");
        System.out.println();
        System.out.println("-----schema & data");
        System.out.println("    -datadir          " + Localize.getMessage("Usage.DataDir"));
        System.out.println("    -encoding         " + Localize.getMessage("Usage.Encoding"));

        System.out.println();
        System.out.println("-----i18n support");
        System.out.println("    -i18nfile         " + Localize.getMessage("Usage.I18nFile"));
        System.out.println("    -i18nencoding     " + Localize.getMessage("Usage.I18nEncoding"));
        System.out.println("    -i18ncrlfaslf     " + Localize.getMessage("Usage.I18nCrLfAsLf"));
        System.out.println("    -langswitchdir    " + Localize.getMessage("Usage.LangSwitchDir"));
        System.out.println("    -defaultlang      " + Localize.getMessage("Usage.DefaultLang"));

        System.out.println();
        System.out.println("-----tools");
        System.out.println("    -verify           " + Localize.getMessage("Usage.Verify"));
        System.out.println("    -searchto         " + Localize.getMessage("Usage.SearchTo"));
        System.out.println("    -searchOwn        " + Localize.getMessage("Usage.SearchOwn"));
        System.out.println("    -search           " + Localize.getMessage("Usage.Search"));
        ValueSearcher.printUsage("        ");
        System.out.println("    -binarytotext     " + Localize.getMessage("Usage.BinaryToText"));
        System.out.println("    -binarytotextloop " + Localize.getMessage("Usage.BinaryToTextLoop"));
        System.out.println("    -xmltocfg         " + Localize.getMessage("Usage.XmlToCfg"));
        System.out.println("    -checkcomma       " + Localize.getMessage("Usage.CheckComma"));

        System.out.println("-----options");
        System.out.println("    -v                " + Localize.getMessage("Usage.V"));
        System.out.println("    -vv               " + Localize.getMessage("Usage.VV"));
        System.out.println("    -p                " + Localize.getMessage("Usage.P"));
        System.out.println("    -pp               " + Localize.getMessage("Usage.PP"));
        System.out.println("    -nowarn           " + Localize.getMessage("Usage.NOWARN"));

        System.out.println();
        System.out.println("-----" + Localize.getMessage("Usage.GenStart"));
        Generators.getAllProviders().forEach((k, v) -> {
                    System.out.printf("    -gen %s\n", k);
                    Usage usage = Usage.of(k);
                    v.create(usage);
                    usage.print();
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

    private static void main0(String[] args) throws Exception {
        Generators.addProvider("i18n", GenI18n::new);

        Generators.addProvider("java", GenJavaCode::new);
        Generators.addProvider("javadata", GenJavaData::new);

        Generators.addProvider("cs", GenCs::new);
        Generators.addProvider("bytes", GenBytes::new);

        Generators.addProvider("lua", GenLua::new);


        String datadir = null;
        boolean xmlToCfg = false;
        int headRow = 2;
        boolean checkComma = false;

        String encoding = "GBK";

        String i18nfile = null;
        String i18nencoding = "GBK";
        boolean i18ncrlfaslf = false;
        String langSwitchDir = null;
        String defaultLang = "zh_cn";

        boolean verify = false;

        record NamedGenerator(String name, Generator gen) {
        }
        List<NamedGenerator> generators = new ArrayList<>();

        boolean binaryToTextLoop = false;
        String binaryToTextFile = null;
        String match = null;

        String searchTo = null;
        String searchOwn = null;
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
            switch (args[i].toLowerCase()) {
                case "-locale" -> {
                    String language = args[++i];
                    Locale locale = Locale.of(language);
                    if (!Localize.isSupported(locale)) {
                        System.err.println("Specified Locale is not supported: " + locale.toString());
                        return;
                    }
                    Localize.setLocale(locale);
                }
                case "-datadir" -> datadir = args[++i];
                case "-xmltocfg" -> xmlToCfg = true;
                case "-headrow" -> headRow = Integer.parseInt(args[++i]);
                case "-checkcomma" -> checkComma = true;
                case "-encoding" -> encoding = args[++i];
                case "-verify" -> verify = true;
                case "-i18nfile" -> i18nfile = args[++i];
                case "-i18nencoding" -> i18nencoding = args[++i];
                case "-i18ncrlfaslf" -> i18ncrlfaslf = true;
                case "-langswitchdir" -> langSwitchDir = args[++i];
                case "-defaultlang" -> defaultLang = args[++i];
                case "-v" -> Logger.setVerboseLevel(1);
                case "-vv" -> Logger.setVerboseLevel(2);
                case "-p" -> Logger.enableProfile();
                case "-pp" -> {
                    Logger.enableProfile();
                    Logger.enableProfileGc();
                }
                case "-nowarn" -> Logger.setNoWarning();
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
                case "-searchown" -> searchOwn = args[++i];

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
                default -> usage("unknown args " + args[i]);
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

        if (i18nfile != null && langSwitchDir != null) {
            usage("-不能同时配置-i18nfile和-langswitchdir");
            return;
        }

        Logger.profile(String.format("start total memory %dm", Runtime.getRuntime().maxMemory() / 1024 / 1024));
        Context ctx = new Context(dataDir, headRow, checkComma, encoding);
        ctx.setI18nOrLangSwitch(i18nfile, i18nencoding, i18ncrlfaslf, langSwitchDir, defaultLang);

        if (searchParam != null) {
            ValueSearcher searcher = new ValueSearcher(ctx.makeValue(searchOwn), searchTo);
            if (searchParam.isEmpty()) {
                searcher.loop();
            } else {
                searcher.search(searchParam.get(0), searchParam.subList(1, searchParam.size()));
            }
            return;
        }

        if (verify) {
            Logger.verbose("-----start verify");
            ctx.makeValue(null);
        }

        for (NamedGenerator ng : generators) {
            Logger.verbose("-----generate " + ng.gen.parameter);
            ng.gen.generate(ctx);
            Logger.profile("generate " + ng.name);
        }

        CachedFiles.finalExit();
        Logger.profile("end");
    }

}
