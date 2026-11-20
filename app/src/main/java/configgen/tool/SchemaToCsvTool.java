package configgen.tool;

import configgen.ctx.DirectoryStructure;
import configgen.data.TableCollector;
import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.schema.*;
import configgen.util.CSVUtil;
import configgen.util.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SchemaToCsvTool extends Tool {
    private final Path dataDir;

    public SchemaToCsvTool(Parameter parameter) {
        super(parameter);
        this.dataDir = Path.of(parameter.get("datadir", "."));
    }

    @Override
    public void call() {
        DirectoryStructure structure = new DirectoryStructure(dataDir);
        CfgSchema cfgSchema = CfgSchemas.readFromDir(structure.getCfgFiles());
        cfgSchema.resolve().checkErrors();
        Set<String> exsitTableSet = TableCollector.collect(structure.getExcelFiles());

        for (TableSchema table : cfgSchema.tableMap().values()) {
            if (table.isJson()) {
                continue;
            }
            if (table.meta().hasEnumValues()){
                continue;
            }

            if (exsitTableSet.contains(table.fullName())) {
                continue;
            }

            String fromCfgFilepath = table.meta().getFromCfgFilepath();
            if (fromCfgFilepath == null) {
                Logger.log("Skip %s: no fromCfgFilepath, SHOULD NOT HAPPEN", table.fullName());
                continue;
            }

            Path csvPath = java.util.Objects.requireNonNull(Path.of(fromCfgFilepath).getParent()).resolve(table.lastName() + ".csv");
            SchemaToCsvHeader headerGen = new SchemaToCsvHeader();
            headerGen.flattenFields(table.fields());

            List<List<String>> rows = headerRows(headerGen, table.isColumnMode());
            try {
                CSVUtil.writeToFile(csvPath.toFile(), rows);
                Logger.log("Generated: %s", csvPath);
            } catch (IOException e) {
                Logger.log("Failed to write %s: %s", csvPath, e.getMessage());
            }
        }
    }
    static List<List<String>> headerRows(SchemaToCsvHeader header, boolean columnMode) {
        List<String> comments = header.getCommentRow();
        List<String> names = header.getNameRow();
        if (!columnMode) {
            return List.of(comments, names);
        }
        List<List<String>> rows = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            rows.add(List.of(comments.get(i), names.get(i)));
        }
        return rows;
    }
}
