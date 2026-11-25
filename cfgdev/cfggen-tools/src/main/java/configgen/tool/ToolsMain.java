package configgen.tool;

import configgen.gen.Generators;
import configgen.gen.Main;
import configgen.gen.Tools;
import configgen.mcpserver.CfgMcpServer;

public final class ToolsMain {
    static void main(String[] args) {
        try {
            Class.forName("configgen.gen.Tool", false, ToolsMain.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            System.err.println("cfggen.jar is required. Place the matching cfggen.jar next to cfggen-tools.jar.");
            System.exit(1);
            return;
        }

        Commands.run(args);
    }

    private static final class Commands {
        private static void run(String[] args) {
            Main.registerAllProviders();
            Tools.addProvider("csvencoding", CsvEncodingTool::new);
            Tools.addProvider("fastexcelcheck", ExcelReadDiffTool::new);
            Generators.addProvider("mcpserver", CfgMcpServer::new);

            int exitCode = Main.runWithCatch(args.length == 0 ? new String[]{"-h"} : args, "cfggen-tools.jar");
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        }
    }
}
