package configgen.gen;

import configgen.gencs.GenBytes;
import configgen.gencs.GenCs;
import configgen.genjava.BinaryToText;
import configgen.genjava.GenJavaData;
import configgen.genjava.code.GenJavaCode;
import configgen.genlua.GenLua;
import configgen.tool.GenI18n;
import configgen.tool.XmlToCfg;
import configgen.util.Logger;
import configgen.tool.ValueSearcher;
import configgen.util.CachedFiles;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class Main {
    private static void usage(String reason) {
        System.out.println(reason);

        System.out.println("Usage: java --enable-preview -jar configgen.jar -datadir [dir] [options] [gens]");
        System.out.println();
        System.out.println("----配置表信息--------------------------------------");
        System.out.println("    -datadir      配表根目录，根目录可以有个config.xml");
        System.out.println("    -encoding     csv编码，默认是GBK，如果文件中含有bom则用bom标记的编码");
        System.out.println("    -verify       检查配表约束");
        System.out.println("    -checkcomma   检查带逗号的number格子，如果设置为list<number>,或由number组成bean类型且逗号分割，就报错");
        System.out.println("                  生成会慢一些");

        System.out.println();
        System.out.println("----国际化支持--------------------------------------");
        System.out.println("    -i18nfile     国际化需要的文件，如果不用国际化，就不要配置");
        System.out.println("    -i18nencoding 国际化需要的文件的编码，默认是GBK，如果文件中含有bom则用bom标记的编码");
        System.out.println("    -i18ncrlfaslf 把字符串里的\\r\\n 替换为 \\n，默认是false");
        System.out.println("    -langSwitchDir 国际化并且可随时切换语言");
        System.out.println("    -defaultLang  langSwitchDir设置时有效，表示默认的语言，默认为zh_cn");

        System.out.println();
        System.out.println("----小工具--------------------------------------");
        System.out.println("    -binaryToText       后可接1或2个参数（java data的file，table名称-用startsWith匹配），打印table的定义和数据");
        System.out.println("    -binaryToTextLoop   后可接1个参数（java data的file），打印table的定义和数据");
        System.out.println("    -search             后接命令，找到匹配的数据");
        System.out.println("    -xmlToCfg           .xml变成.cfg文件");


        System.out.println("    -v            verbose，级别1，输出统计和warning信息");
        System.out.println("    -vv           verbose，级别2，输出额外信息");
        System.out.println("    -p            profiler，内存和时间监测");
        System.out.println("    -pp           profiler，内存监测前加gc");
        System.out.println("    -nowarn       不打印警告信息，默认打印");


        System.out.println();
        System.out.println("----以下gen参数之间由,分割,参数名和参数取值之间由=或:分割--------------------------------------");
        Generators.getAllProviders().forEach((k, v) -> {
                    System.out.printf("    -gen %s\n", k);
                    Usage usage = new Usage();
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
            usage("请需要配置-datadir");
            return;
        }

        Path dataDir = Paths.get(datadir);
        if (xmlToCfg) {
            XmlToCfg.convertAndCheck(dataDir);
            return;
        }

        if (i18nfile != null && langSwitchDir != null) {
            usage("-不能同时配置-i18nfile和-langSwitchDir");
            return;
        }

        Logger.profile(String.format("start total memory %dm", Runtime.getRuntime().maxMemory() / 1024 / 1024));
        Context ctx = new Context(dataDir, headRow, checkComma, encoding);
        ctx.setI18nOrLangSwitch(i18nfile, i18nencoding, i18ncrlfaslf, langSwitchDir, defaultLang);

        if (searchParam != null) {
            ValueSearcher searcher = new ValueSearcher(ctx.makeValue());
            if (searchParam.isEmpty()) {
                searcher.loop();
            } else {
                searcher.search(searchParam.get(0), searchParam.subList(1, searchParam.size()));
            }
            return;
        }

        if (verify) {
            Logger.verbose("-----start verify");
            ctx.makeValue();
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
