package configgen.tool;

import com.alibaba.fastjson2.JSON;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record AICfg(String baseUrl,
                    String apiKey,
                    String model,
                    List<TableCfg> tableCfgs) {


    public record TableCfg(String table,
                           String promptFile, // {table}.jte
                           List<String> extraRefTables,
                           List<OneExample> examples) {
    }

    public record OneExample(String id,
                             String description) {

    }

    public TableCfg findTable(String table) {
        for (TableCfg tableCfg : tableCfgs) {
            if (tableCfg.table.equals(table)) {
                return tableCfg;
            }
        }
        return null;
    }

    public String findPromptFile(String table, Path dir) {
        AICfg.TableCfg tableCfg = findTable(table);
        if (tableCfg != null && tableCfg.promptFile() != null && !tableCfg.promptFile().isEmpty()) {
            return tableCfg.promptFile();
        } else {
            return dir.resolve(table + ".jte").toString();
        }
    }

    public static AICfg readFromFile(String cfgFn) {
        Path path = Path.of(cfgFn);
        if (!Files.exists(path)) {
            throw new RuntimeException(cfgFn + " not exist!");
        }
        String jsonStr;
        try {
            jsonStr = Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return JSON.parseObject(jsonStr, AICfg.class);
    }

}