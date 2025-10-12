package configgen.ctx;

import configgen.util.ArgParser;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 明确指定子目录，只用这些
 * @param txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map 这些目录的文件被视为在rootDir下，当这些目录下有新的文件添加时，自动加上tag
 *                                                    一般为ClientTables:noserver,PublicTables,ServerTables:noclient
 * @param excelFileDirs 用这些目录下的excel文件
 * @param jsonFileDirs 用这些目录下的json文件
 */
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
