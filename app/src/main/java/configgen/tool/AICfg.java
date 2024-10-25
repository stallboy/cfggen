package configgen.tool;

import java.util.List;
import java.util.Objects;

public record AICfg(String baseUrl,
                    String apiKey,
                    String model,
                    List<TableCfg> tableCfgs) {

    public AICfg {
        Objects.requireNonNull(baseUrl);
        Objects.requireNonNull(apiKey);
        Objects.requireNonNull(model);
        Objects.requireNonNull(tableCfgs);
    }


    public record TableCfg(String table,
                           String promptFile, // {table}.jte
                           List<String> extraRefTables,
                           List<OneExample> examples) {

        public TableCfg {
            Objects.requireNonNull(table);
            Objects.requireNonNull(promptFile);
            Objects.requireNonNull(extraRefTables);
            Objects.requireNonNull(examples);
        }
    }

    public record OneExample(String id,
                             String description) {

        public OneExample {
            Objects.requireNonNull(id);
            Objects.requireNonNull(description);
        }
    }

    public TableCfg findTable(String table) {
        for (TableCfg tableCfg : tableCfgs) {
            if (tableCfg.table.equals(table)) {
                return tableCfg;
            }
        }
        return null;
    }
}