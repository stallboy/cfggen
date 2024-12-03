package configgen.ctx;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static configgen.data.DataUtil.*;
import static configgen.data.DataUtil.FileFmt.CSV;
import static configgen.data.DataUtil.FileFmt.EXCEL;

/**
 * <p> table目录： 首字母是英文字符。 截取.之前的，再截取 _汉字或汉字之前的，作为table名 </p>
 * <ul>
 * <li> schema文件：根目录下有config.cfg，table目录下有[table].cfg</li>
 * <li> csv/excel文件：根目录或table目录下，忽略~开头的，忽略隐藏的。 截取.之前的，再截取 _汉字或汉字之前的，作为文件名
 *      <ul>
 *          <li> 文件.csv后缀：[table]_[idx].csv，或[table].csv</li>
 *          <li> 文件.xls或.xlsx后缀：对每个sheet的名称：截取.之前的，再截取 _汉字或汉字之前的，作为sheet名
 *              <ul>
 *              <li>sheet名：[table]_[idx]，[table]</li>
 *              </ul>
 *          </li>
 *      </ul>
 * </li>
 *
 * <li> json文件：存在于_[table.replace(".", "_")]/目录下，比如table为skill.buff,则目录为_skill_buff</li>
 * <ul>
 */
public class DirectoryStructure {
    public static final String ROOT_CONFIG_FILENAME = "config.cfg";
    public static final String CONFIG_EXT = "cfg";


    public record CfgFileInfo(long lastModified,
                              Path path,
                              Path relativePath,
                              String pkgNameDot) {

    }

    public record ExcelFileInfo(long lastModified,
                                Path path,
                                Path relativePath,
                                FileFmt fmt,
                                TableNameIndex csvTableNameIndex /*can be null*/) {
    }

    public record JsonFileInfo(long lastModified,
                               Path path,
                               Path relativePath) {
    }


    private final Path rootDir;
    private final Map<String, CfgFileInfo> cfgFiles = new LinkedHashMap<>();  // file path -> info
    private final Map<String, ExcelFileInfo> excelFiles = new LinkedHashMap<>(); // file path -> info
    private final Map<String, Map<String, JsonFileInfo>> tableToJsonFiles = new LinkedHashMap<>(); // table -> file path -> info

    public DirectoryStructure(Path rootDir) {
        this.rootDir = rootDir;
        findConfigFilesFromRecursively(rootDir.resolve(ROOT_CONFIG_FILENAME), CONFIG_EXT, "",
                rootDir, cfgFiles);
        findExcelFilesRecursively(rootDir);
        findTableToJsonFiles();
    }

    public Path getRootDir() {
        return rootDir;
    }

    public Map<String, CfgFileInfo> getCfgFiles() {
        return cfgFiles;
    }

    public Map<String, ExcelFileInfo> getExcelFiles() {
        return excelFiles;
    }

    public Map<String, Map<String, JsonFileInfo>> getTableToJsonFiles() {
        return tableToJsonFiles;
    }

    private static void findConfigFilesFromRecursively(Path source, String ext, String pkgNameDot,
                                                       Path rootDir, Map<String, CfgFileInfo> cfgFiles) {
        if (Files.exists(source)) {
            Path relativizeSource = rootDir.relativize(source);
            cfgFiles.put(relativizeSource.toString(),
                    new CfgFileInfo(source.toFile().lastModified(), source, relativizeSource, pkgNameDot));
        }
        try {
            try (Stream<Path> paths = Files.list(source.getParent())) {
                for (Path path : paths.toList()) {
                    if (!Files.isDirectory(path)) {
                        continue;
                    }
                    String lastDir = path.getFileName().toString().toLowerCase();
                    String subPkgName = getCodeName(lastDir);
                    if (subPkgName == null) {
                        continue;
                    }
                    Path subSource = path.resolve(subPkgName + "." + ext);
                    String subPkgNameDot = pkgNameDot + subPkgName + ".";
                    findConfigFilesFromRecursively(subSource, ext, subPkgNameDot,
                            rootDir, cfgFiles);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, CfgFileInfo> findAllXmlFiles(Path rootDir) {
        Map<String, CfgFileInfo> result = new LinkedHashMap<>();
        findConfigFilesFromRecursively(rootDir.resolve("config.xml"), "xml", "",
                rootDir, result);
        return result;
    }

    private void findExcelFilesRecursively(Path dir) {
        try {
            try (Stream<Path> paths = Files.list(dir)) {
                for (Path path : paths.toList()) {
                    if (isFileIgnored(path)) {
                        continue;
                    }
                    String lastSeg = path.getFileName().toString();
                    String codeName = getCodeName(lastSeg);
                    if (codeName == null) {
                        continue;
                    }

                    if (Files.isDirectory(path)) {
                        findExcelFilesRecursively(path);

                    } else if (Files.isRegularFile(path)) {
                        FileFmt fmt = getFileFormat(path);
                        if (fmt == null) {
                            continue;
                        }
                        Path relativePath = rootDir.relativize(path);
                        switch (fmt) {
                            case CSV -> {
                                TableNameIndex ti = getTableNameIndex(relativePath);
                                excelFiles.put(relativePath.toString(),
                                        new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, CSV, ti));
                            }
                            case EXCEL -> {
                                excelFiles.put(relativePath.toString(),
                                        new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, EXCEL, null));
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findTableToJsonFiles() {
        tableToJsonFiles.clear();
        try (Stream<Path> paths = Files.list(rootDir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path)) {
                    continue;
                }
                if (!Files.isDirectory(path)) {
                    continue;
                }


                String dirName = path.getFileName().toString();
                String tableName = getTableNameFromDir(dirName);
                if (tableName == null) {
                    continue;
                }

                Map<String, JsonFileInfo> jsonFiles = tableToJsonFiles.computeIfAbsent(tableName, (String j) -> new LinkedHashMap<>());
                findOneTableJsonFiles(path, jsonFiles);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findOneTableJsonFiles(Path tableDir, Map<String, JsonFileInfo> jsonFiles) throws IOException {
        try (Stream<Path> paths = Files.list(tableDir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path)) {
                    continue;
                }
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(".json")) {
                    continue;
                }

                Path relativePath = rootDir.relativize(tableDir.resolve(fileName));
                Path absPath = path.toAbsolutePath().normalize();
                jsonFiles.put(relativePath.toString(),
                        new JsonFileInfo(path.toFile().lastModified(), absPath, relativePath));
            }
        }
    }


    public void addJsonInPlace(String tableName, Path jsonPath) {
        Map<String, JsonFileInfo> jsonFiles = tableToJsonFiles.computeIfAbsent(tableName, (String j) -> new LinkedHashMap<>());
        Path relativePath = rootDir.relativize(jsonPath);
        Path path = jsonPath.toAbsolutePath().normalize();
        jsonFiles.put(relativePath.toString(),
                new JsonFileInfo(jsonPath.toFile().lastModified(), path, relativePath));
    }

    public record Care(boolean isCare,
                       long lastModified) {
        public static Care NO = new Care(false, 0);
    }

    public Care getCare(Path filePath) {
        if (isFileIgnored(filePath)) {
            return Care.NO;
        }

        return null;
    }
}
