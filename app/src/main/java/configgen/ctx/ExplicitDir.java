package configgen.ctx;

import configgen.util.ArgParser;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ExplicitDir(Map<String, String> txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map,
                          Set<String> excelFileDirs,
                          Set<String> jsonFileDirs) {

    public ExplicitDir {
        Objects.requireNonNull(txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map);
        Objects.requireNonNull(excelFileDirs);
        Objects.requireNonNull(jsonFileDirs);
    }

    public static ExplicitDir parse(String asRoot, String excelDirs, String jsonDirs){
        Map<String, String> root = ArgParser.parseToMap(asRoot);
        Set<String> excels = ArgParser.parseToSet(excelDirs);
        Set<String> jsons = ArgParser.parseToSet(jsonDirs);

        // 一旦一个包含有数据，就必须全部明确声明
        if (root.isEmpty() && excels.isEmpty() && jsons.isEmpty()) {
            return null;
        }
        return new ExplicitDir(root, excels, jsons);
    }
}
