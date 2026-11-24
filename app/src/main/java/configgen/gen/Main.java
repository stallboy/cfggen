package configgen.gen;

import configgen.schema.CfgSchemas;
import org.jspecify.annotations.Nullable;
import configgen.ctx.Context;
import configgen.ctx.Context.I18nCfg;
import configgen.editorserver.EditorServer;
import configgen.gen.ui.GuiLauncher;
import configgen.genbytes.BytesGenerator;
import configgen.gencs.CsCodeGenerator;
import configgen.tool.BytesViewTool;
import configgen.gents.TsCodeGenerator;
import configgen.gengo.GoCodeGenerator;
import configgen.genjava.code.JavaCodeGenerator;
import configgen.genjson.JsonGenerator;
import configgen.genbyai.ByAIGenerator;
import configgen.genbyai.TsSchemaGenerator;
import configgen.genlua.LuaCodeGenerator;
import configgen.gengd.GdCodeGenerator;
import configgen.geni18n.TodoTermListerAndChecker;
import configgen.geni18n.I18nByValueGenerator;
import configgen.geni18n.I18nByIdGenerator;
import configgen.geni18n.TodoTranslator;
import configgen.mcpserver.CfgMcpServer;
import configgen.tool.*;
import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Main {
    private static final int MAX_EXCEPTION_DEPTH = 30;

    private static int help(@Nullable String reason) {
        Help.printHelp(reason);
        return 1;
    }

    static void main(String[] args) {
        registerAllProviders();

        if (args.length == 0) {
            Help.printHelp();
            return;
        }

        int ret = runWithCatch(args);
        if (ret != 0) {
            System.exit(ret);
        }
    }

    record NamedTool(String name, Tool tool) {
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static void registerAllProviders() {
        Tools.addProvider("xmltocfg", XmlToCfgTool::new);
        Tools.addProvider("fastexcelcheck", ExcelReadDiffTool::new);
        Tools.addProvider("bytesview", BytesViewTool::new);
        Tools.addProvider("term", TodoTermListerAndChecker::new);
        Tools.addProvider("translate", TodoTranslator::new);
        Tools.addProvider("schematocsv", SchemaToCsvTool::new);
        Tools.addProvider("blockmigrate", BlockMigrationTool::new);

        Generators.addProvider("verify", ValueVerifyTool::new);
        Generators.addProvider("search", ValueInspectTool::new);
        Generators.addProvider("i18n", I18nByValueGenerator::new);
        Generators.addProvider("i18nbyid", I18nByIdGenerator::new);

        Generators.addProvider("java", JavaCodeGenerator::new);
        Generators.addProvider("cs", CsCodeGenerator::new);
        Generators.addProvider("bytes", BytesGenerator::new);
        Generators.addProvider("lua", LuaCodeGenerator::new);
        Generators.addProvider("ts", TsCodeGenerator::new);
        Generators.addProvider("go", GoCodeGenerator::new);
        Generators.addProvider("gd", GdCodeGenerator::new);
        Generators.addProvider("tsschema", TsSchemaGenerator::new);
        Generators.addProvider("json", JsonGenerator::new);

        Generators.addProvider("server", EditorServer::new);
        Generators.addProvider("mcpserver", CfgMcpServer::new);
        Generators.addProvider("byai", ByAIGenerator::new);
    }

    public static int runWithCatch(String[] args) {
        try {
            return run(args);
        } catch (CliException e) {
            // 命令行使用错误：打印简短原因和usage，不打印堆栈
            return help(e.getMessage());
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

    private static int run(String[] args) throws IOException {
        boolean allowValueErr = false;
        String dataDir = null;
        String sharedSchema = null;
        String headRowId = null;
        String encodingArg = null;

        String i18nfile = null;
        String langSwitchDir = null;
        String langSwitchDefaultLang = "zh_cn";

        List<NamedTool> tools = new ArrayList<>();
        List<Generator> generators = new ArrayList<>();


        for (int i = 0; i < args.length; ++i) {
            String paramType = args[i].toLowerCase(Locale.ROOT);
            switch (paramType) {
                case "-locale" -> {
                    String language = nextArg(args, paramType, ++i);
                    Locale locale = Locale.of(language);
                    if (!LocaleUtil.isSupported(locale)) {
                        System.err.println("Specified Locale is not supported: " + locale);
                        return 1;
                    }
                    LocaleUtil.setLocale(locale);
                }

                case "-gui" -> {
                    GuiLauncher.launch();
                    return 0;
                }
                case "-h" -> {
                    Help.printHelp();
                    return 0;
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
                case "-allowvalueerr" -> allowValueErr = true;

                case "-tool" -> {
                    String name = nextArg(args, paramType, ++i);
                    Tool tool = Tools.create(name);
                    if (tool == null) {
                        return help("-tool " + name + " UNKNOWN");
                    }
                    tools.add(new NamedTool(name, tool));
                }

                case "-datadir" -> dataDir = nextArg(args, paramType, ++i);
                case "-schema" -> sharedSchema = nextArg(args, paramType, ++i);
                case "-headrow" -> headRowId = nextArg(args, paramType, ++i);
                case "-encoding" -> encodingArg = nextArg(args, paramType, ++i);

                case "-i18nfile" -> i18nfile = nextArg(args, paramType, ++i);
                case "-langswitchdir" -> langSwitchDir = nextArg(args, paramType, ++i);
                case "-defaultlang" -> langSwitchDefaultLang = nextArg(args, paramType, ++i);

                case "-gen" -> {
                    String name = nextArg(args, paramType, ++i);
                    Generator generator = Generators.create(name);
                    if (generator == null) {
                        return help("-gen " + name + " UNKNOWN");
                    }
                    generators.add(generator);
                }
                default -> {
                    return help("unknown args " + args[i]);
                }
            }
        }

        // 参数组合校验必须先于任何执行（tools/generators都有副作用）
        if (i18nfile != null && langSwitchDir != null) {
            return help("-i18nfile and -langswitchdir cannot be used together");
        }
        if (dataDir == null && !generators.isEmpty()) {
            return help("-datadir is required");
        }

        if (dataDir == null) {
            runTools(tools);
            return 0;
        }
        Path dataDirPath = Path.of(dataDir);
        DataReadOptions dataOptions = DataReadOptions.resolve(dataDirPath, headRowId, encodingArg);
        runTools(tools);

        Logger.profile(String.format("start total memory %dm", Runtime.getRuntime().maxMemory() / 1024 / 1024));

        I18nCfg i18n = I18nCfg.Disabled.INSTANCE;
        if (i18nfile != null) {
            i18n = new I18nCfg.Replace(Path.of(i18nfile));
        } else if (langSwitchDir != null) {
            i18n = new I18nCfg.Switchable(Path.of(langSwitchDir), langSwitchDefaultLang);
        }
        var contextCfg = new Context.ContextCfg(dataDirPath, dataOptions.headRow(), dataOptions.csvEncoding(),
                i18n, allowValueErr);
        Context context = sharedSchema == null ? Context.loadAndAlignSchema(contextCfg)
                : Context.load(contextCfg, CfgSchemas.readFromRoot(Path.of(sharedSchema)));
        GenerationBatch.run(context, generators);
        Logger.profile("end");
        return 0;
    }

    private static void runTools(List<NamedTool> tools) {
        for (NamedTool nt : tools) {
            Logger.verbose("-----tool %s", nt.tool.parameter);
            nt.tool.call();
        }
    }

    private static String nextArg(String[] args, String paramType, int valueIndex) {
        if (valueIndex >= args.length) {
            throw new CliException("missing value for " + paramType);
        }
        return args[valueIndex];
    }

}
