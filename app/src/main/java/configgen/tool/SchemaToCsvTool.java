package configgen.tool;

import configgen.ctx.DirectoryStructure;
import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.schema.*;
import configgen.util.CSVUtil;
import configgen.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SchemaToCsvTool extends Tool {
    private final Path dataDir;

    public SchemaToCsvTool(Parameter parameter) {
        super(parameter);
        this.dataDir = Path.of(parameter.get("datadir", "."));
    }

    @Override
    public void call() {
        DirectoryStructure structure = new DirectoryStructure(dataDir);
        CfgSchema cfgSchema = CfgSchemas.readFromDir(structure);
        cfgSchema.resolve().checkErrors();

        for (TableSchema table : cfgSchema.tableMap().values()) {
            if (table.isJson()) continue;

            Path csvPath = dataDir.resolve(table.namespace())
                                  .resolve(table.lastName() + ".csv");
            if (Files.exists(csvPath)) {
                Logger.log("Skip existing: %s", csvPath);
                continue;
            }

            SchemaToCsvHeader headerGen = new SchemaToCsvHeader();
            headerGen.flattenFields(table.fields());

            List<List<String>> rows = List.of(
                headerGen.getCommentRow(),
                headerGen.getNameRow()
            );
            try {
                CSVUtil.writeToFile(csvPath.toFile(), rows);
                Logger.log("Generated: %s", csvPath);
            } catch (IOException e) {
                Logger.log("Failed to write %s: %s", csvPath, e.getMessage());
            }
        }
    }
}
