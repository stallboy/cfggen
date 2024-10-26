package configgen.tool;

import com.alibaba.fastjson2.JSON;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record AICfg(String baseUrl,
                    String apiKey,
                    String model,
                    List<TableCfg> tableCfgs) {


    private static final String DEFAULT_INIT = "请提供ID和描述，我将根据这些信息生成符合结构的JSON配置";

    public record TableCfg(String table,
                           String promptFile, // {table}.jte
                           String init, // 初始对白
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
        Objects.requireNonNull(table);
        TableCfg tc = findTable(table);
        if (tc != null && tc.promptFile != null && !tc.promptFile.isEmpty()) {
            return tc.promptFile;
        } else if (dir != null) {
            return dir.resolve(table + ".jte").toString();
        } else {
            return table + ".jte";
        }
    }

    public String findInit(String table) {
        TableCfg tc = findTable(table);
        if (tc != null && tc.init != null && !tc.init.isEmpty()) {
            return tc.init;
        }
        return DEFAULT_INIT;
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
        if  (jsonStr.isEmpty()) {
            throw new RuntimeException(cfgFn + " is empty!");
        }
        return JSON.parseObject(jsonStr, AICfg.class);
    }

}
