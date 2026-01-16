package configgen.tool;

import configgen.gen.BuildSettings;
import configgen.gen.Generators;
import configgen.gen.Parameter;
import configgen.gen.ParameterInfoCollector;
import configgen.gen.Tool;
import configgen.gen.Tools;
import configgen.util.LocaleUtil;
import configgen.util.Logger;

/**
 * 打印使用帮助信息
 */
public class Usage extends Tool {

    public Usage(Parameter parameter) {
        super(parameter);
        parameter.title("print usage help");
    }

    @Override
    public void call() {
        printUsage();
    }

    public static void printUsage() {
        printUsage(null);
    }

    public static void printUsage(String reason) {
        if (reason != null && !reason.isBlank()) {
            Logger.log(reason);
        }

        Logger.log("Usage: java -jar cfggen.jar [tools] -datadir [dir] [options] [gens]");
        Logger.log(LocaleUtil.getLocaleString("Usage.NoArgs",
                "    (no args)        launch GUI for interactive configuration"));
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
    }
}
