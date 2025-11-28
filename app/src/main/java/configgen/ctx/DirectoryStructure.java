package configgen.ctx;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static configgen.data.DataUtil.*;
import static configgen.data.DataUtil.FileFmt.*;

/// # 文件目录规范
///
/// ## Table目录命名规则
/// - 首字母必须是英文字符
/// - 命名解析逻辑：
///   ```
///   截取第一个"."之前的内容 → 再截取"_汉字"或汉字之前的部分 → 作为table名
///```
///
/// ## 配置文件结构
/// - **Schema文件**：
///   - 根目录下必须存在 `config.cfg`
///   - 每个module目录下需有 `[module].cfg`
///
/// ## 数据文件规则
/// ### 通用规则
/// - 忽略以下文件：
///   - 以`~`开头的文件
///   - 隐藏文件
///
/// ### CSV文件规范
/// - 文件后缀：`.csv`
/// - 命名解析逻辑：
///   ```
///   截取".csv"之前的内容 → 再截取"_汉字"或汉字之前的部分 → 作为csv名
///```
/// - 合法命名格式：
///   - `[table]_[idx]`
///   - `[table]`
///
/// ### Excel文件规范
/// - 文件后缀：`.xls` 或 `.xlsx`
/// - Sheet命名规则：
///   ```
///   截取"."之前的内容 → 再截取"_汉字"或汉字之前的部分 → 作为sheet名
///```
/// - 合法命名格式：
///   - `[table]_[idx]`
///   - `[table]`
///
/// ## JSON文件规范
/// - 目录命名规则：
///   ```
///   _[table.replace(".", "_")]/
///```
/// - 示例：
///   - 若table为`skill.buff` → 对应目录为`_skill_buff`
///
/// > 注：`[table]`指代通过table目录规则解析出的名称
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
                                TableNameIndex csvOrTsvTableNameIndex,/*can be null*/
                                String nullableAddTag) {
        public ExcelFileInfo {
            Objects.requireNonNull(path);
            Objects.requireNonNull(relativePath);
            Objects.requireNonNull(fmt);
        }
    }

    public record JsonFileInfo(long lastModified,
                               Path path,
                               Path relativePath,
                               boolean isIntegerId,
                               int integerId) {

        static JsonFileInfo of(Path absPath, Path relativePath) {
            String fn = relativePath.getFileName().toString();
            int id = -1;
            boolean isIntegerId = false;
            try {
                id = Integer.parseInt(fn.substring(0, fn.length() - 5));
                isIntegerId = true;
            } catch (NumberFormatException ignored) {
            }
            return new JsonFileInfo(absPath.toFile().lastModified(), absPath, relativePath, isIntegerId, id);
        }
    }


    static class JsonFileList {
        List<JsonFileInfo> list = new ArrayList<>();
        Map<String, JsonFileInfo> map = new LinkedHashMap<>();


        void sort() {
            list = new ArrayList<>(map.values());
            if (map.values().stream().allMatch(j -> j.isIntegerId)) {
                list.sort(Comparator.comparingInt(o -> o.integerId));
            }
        }

        void addFile(JsonFileInfo info) {
            map.put(info.relativePath.toString(), info);
        }

        JsonFileList copy() {
            JsonFileList c = new JsonFileList();
            c.map = new LinkedHashMap<>(map);
            c.list = new ArrayList<>(list);
            return c;
        }

        public JsonFileInfo removeFile(String jsonFileRelativePath) {
            return map.remove(jsonFileRelativePath);
        }

    }


    private final Path rootDir;
    private final ExplicitDir explicitDir;
    /**
     * 配置文件信息
     */
    private final Map<String, CfgFileInfo> cfgFiles = new LinkedHashMap<>();  // file path -> info
    /**
     * excel文件信息
     */
    private final Map<String, ExcelFileInfo> excelFiles = new LinkedHashMap<>(); // file path -> info
    /**
     * json文件信息，可能被不同的线程访问，所以每次改变就创建新对象，改变引用
     */
    private volatile Map<String, JsonFileList> jsonFiles = new LinkedHashMap<>(); // table -> json file list

    public DirectoryStructure(Path rootDir) {
        this(rootDir, null);
    }

    public DirectoryStructure(Path rootDir, ExplicitDir explicitDir) {
        this.rootDir = rootDir;
        this.explicitDir = explicitDir;

        findConfigFilesFromRecursively(rootDir.resolve(ROOT_CONFIG_FILENAME),
                explicitDir != null ? explicitDir.excelFileDirs() : null,
                CONFIG_EXT, "",
                rootDir, cfgFiles);

        if (explicitDir == null) {
            findExcelFilesRecursively(rootDir);
        } else {
            for (Map.Entry<String, String> e : explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map().entrySet()) {
                Path dir = rootDir.resolve(e.getKey());
                if (Files.exists(dir) && Files.isDirectory(dir)) {
                    findTxtAsTsvFiles(dir, e.getValue());
                }
            }

            for (String p : explicitDir.excelFileDirs()) {
                Path dir = rootDir.resolve(p);
                if (Files.exists(dir) && Files.isDirectory(dir)) {
                    findExcelFilesRecursively(dir);
                }
            }
        }

        if (explicitDir == null) {
            findTableToJsonFiles();
        } else {
            for (String p : explicitDir.jsonFileDirs()) {
                findOneTableJsonFilesInDir(rootDir.resolve(p));
            }

        }
    }

    public DirectoryStructure reload() {
        return new DirectoryStructure(rootDir, explicitDir);
    }

    public Path getRootDir() {
        return rootDir;
    }

    public ExplicitDir getExplicitDir() {
        return explicitDir;
    }

    public Collection<CfgFileInfo> getCfgFiles() {
        // Return sorted collection to ensure consistent ordering across platforms
        return cfgFiles.values().stream()
                .sorted(Comparator.comparing(CfgFileInfo::pkgNameDot))
                .toList();
    }

    public Path getCfgFilePathByPkgName(String pkgName) {
        String pkgNameDot = pkgName.isEmpty() ? "" : pkgName + ".";
        for (CfgFileInfo c : cfgFiles.values()) {
            if (c.pkgNameDot.equals(pkgNameDot)) {
                return c.path;
            }
        }
        return null;
    }

    public Collection<ExcelFileInfo> getExcelFiles() {
        return excelFiles.values();
    }

    public Collection<JsonFileInfo> getJsonFilesByTable(String tableName) {
        JsonFileList list = jsonFiles.get(tableName);
        if (list == null) {
            return List.of();
        }
        return list.list;
    }

    public static void findConfigFilesFromRecursively(Path source,
                                                      Set<String> nullableWhiteListSubDirs,
                                                      String ext,
                                                      String pkgNameDot,
                                                      Path rootDir,
                                                      Map<String, CfgFileInfo> cfgFiles) {
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

                    if (nullableWhiteListSubDirs != null &&
                            !nullableWhiteListSubDirs.contains(path.getFileName().toString())) {
                        continue;
                    }

                    String lastDir = path.getFileName().toString().toLowerCase();
                    String subPkgName = getCodeName(lastDir);
                    if (subPkgName == null) {
                        continue;
                    }
                    Path subSource = path.resolve(subPkgName + "." + ext);
                    String subPkgNameDot = pkgNameDot + subPkgName + ".";
                    findConfigFilesFromRecursively(subSource, null, ext, subPkgNameDot,
                            rootDir, cfgFiles);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findExcelFilesRecursively(Path dir) {
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path)) {
                    continue;
                }

                if (Files.isDirectory(path)) {
                    String lastSeg = path.getFileName().toString();
                    String codeName = getCodeName(lastSeg);
                    if (codeName == null) {
                        continue;
                    }

                    findExcelFilesRecursively(path);

                } else if (Files.isRegularFile(path)) {
                    FileFmt fmt = getFileFormat(path);
                    if (fmt == null) {
                        continue;
                    }
                    Path relativePath = rootDir.relativize(path);
                    switch (fmt) {
                        case CSV -> {
                            String lastSeg = path.getFileName().toString();
                            String codeName = getCodeName(lastSeg);
                            if (codeName == null) {
                                continue;
                            }

                            TableNameIndex ti = getTableNameIndex(relativePath);
                            excelFiles.put(relativePath.toString(),
                                    new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, CSV, ti, null));
                        }
                        case EXCEL -> {
                            excelFiles.put(relativePath.toString(),
                                    new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, EXCEL, null, null));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findTxtAsTsvFiles(Path dir, String nullableAddTag) {
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path)) {
                    continue;
                }
                if (Files.isRegularFile(path)) {
                    FileFmt fmt = getFileFormat(path);
                    if (fmt != TXT_AS_TSV) {
                        continue;
                    }
                    Path relativePath = path.getFileName();
                    String lastSeg = relativePath.toString();
                    String codeName = getCodeName(lastSeg);
                    if (codeName == null) {
                        continue;
                    }

                    TableNameIndex ti = getTableNameIndex(relativePath);
                    excelFiles.put(relativePath.toString(),
                            new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, TXT_AS_TSV, ti, nullableAddTag));
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findTableToJsonFiles() {
        try (Stream<Path> paths = Files.list(rootDir)) {
            for (Path path : paths.toList()) {
                findOneTableJsonFilesInDir(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findOneTableJsonFilesInDir(Path path) {
        if (isFileIgnored(path)) {
            return;
        }
        if (!Files.isDirectory(path)) {
            return;
        }

        String dirName = path.getFileName().toString();
        String tableName = getTableNameIfTableDirForJson(dirName);
        if (tableName == null) {
            return;
        }

        JsonFileList jsonFiles = this.jsonFiles.computeIfAbsent(tableName, (String j) -> new JsonFileList());
        findOneTableJsonFiles(path, jsonFiles);
    }


    private void findOneTableJsonFiles(Path tableDir, JsonFileList list) {
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
                list.addFile(JsonFileInfo.of(absPath, relativePath));
            }
            list.sort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 改变或增加json文件后，会导致watcher触发，
     * 但这里再在运行时记录下来此json的lastModified，然后通过 lastModifiedEquals 比较来避免全量makeValue
     */
    public synchronized JsonFileInfo addJsonFile(String tableName, Path jsonPath) {
        Map<String, JsonFileList> tmp = copyJsonFiles(tableName);
        JsonFileList list = tmp.computeIfAbsent(tableName, (String j) -> new JsonFileList());
        Path relativePath = rootDir.relativize(jsonPath);
        Path path = jsonPath.toAbsolutePath().normalize();
        JsonFileInfo jf = JsonFileInfo.of(path, relativePath);
        list.addFile(jf);
        list.sort();

        this.jsonFiles = tmp;
        return jf;
    }

    public synchronized void removeJsonFile(String tableName, Path jsonPath) {
        Map<String, JsonFileList> tmp = copyJsonFiles(tableName);

        JsonFileList list = tmp.get(tableName);
        if (list == null) {
            return;
        }
        Path relativePath = rootDir.relativize(jsonPath);
        String jsonKey = relativePath.toString();
        JsonFileInfo jf = list.removeFile(jsonKey);
        if (jf == null) {
            return;
        }
        list.sort();

        jsonFiles = tmp;
    }


    private Map<String, JsonFileList> copyJsonFiles(String changedTable) {
        Map<String, JsonFileList> copy = new LinkedHashMap<>(jsonFiles.size());
        for (Map.Entry<String, JsonFileList> e : jsonFiles.entrySet()) {
            String key = e.getKey();
            JsonFileList list = key.equals(changedTable) ? e.getValue().copy() : e.getValue();
            copy.put(key, list);
        }
        return copy;
    }

    public boolean lastModifiedEquals(DirectoryStructure other) {
        Objects.requireNonNull(other);

        if (cfgFiles.size() != other.cfgFiles.size()) {
            return false;
        }

        if (excelFiles.size() != other.excelFiles.size()) {
            return false;
        }

        Map<String, JsonFileList> tmp1 = jsonFiles;
        Map<String, JsonFileList> tmp2 = other.jsonFiles;
        if (tmp1.size() != tmp2.size()) {
            return false;
        }

        for (Map.Entry<String, JsonFileList> e : tmp1.entrySet()) {
            JsonFileList t2 = tmp2.get(e.getKey());
            if (t2 == null) {
                return false;
            }
            List<JsonFileInfo> j1 = e.getValue().list;
            List<JsonFileInfo> j2 = t2.list;

            if (j2.size() != j1.size()) {
                return false;
            }
            for (int i = 0, sz = j1.size(); i < sz; i++) {
                JsonFileInfo jf1 = j1.get(i);
                JsonFileInfo jf2 = j2.get(i);

                if (jf2.lastModified != jf1.lastModified) {
                    return false;
                }
            }
        }

        for (Map.Entry<String, CfgFileInfo> e : cfgFiles.entrySet()) {
            CfgFileInfo f2 = other.cfgFiles.get(e.getKey());
            if (f2 == null) {
                return false;
            }
            CfgFileInfo f1 = e.getValue();
            if (f2.lastModified != f1.lastModified) {
                return false;
            }
        }

        for (Map.Entry<String, ExcelFileInfo> e : excelFiles.entrySet()) {
            ExcelFileInfo f2 = other.excelFiles.get(e.getKey());
            if (f2 == null) {
                return false;
            }
            ExcelFileInfo f1 = e.getValue();
            if (f2.lastModified != f1.lastModified) {
                return false;
            }
        }

        return true;
    }
}
