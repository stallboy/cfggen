package configgen.ctx;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import configgen.data.ExcelFileInfo;
import configgen.data.JsonFileInfo;
import configgen.data.JsonTableFiles;
import configgen.schema.CfgFileInfo;
import configgen.schema.cfg.CfgUtil;

import static configgen.data.DataUtil.*;
import static configgen.util.FileNameUtil.getCodeName;
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
/// - **优先（嵌套格式）**：在模块目录下的 `_子名` 子目录
///   ```
///   [moduleDir]/_[subName]/
///   ```
///   - 示例：table为`skill.buff` → `skill/_buff` 或 `技能skill/_buff`
///   - `[moduleDir]`是 codeName 等于模块名的目录（如 `skill` 或 `技能skill`）
/// - **备选（根级格式）**：当模块目录不存在时回退
///   ```
///   _[table.replace(".", "_")]/
///   ```
///   - 示例：table为`skill.buff` → `_skill_buff`
/// - **冲突**：同一表名不能同时存在嵌套和根级两种目录，否则报错
///
/// > 注：`[moduleDir]`指代通过 getCodeName 解析出模块名的目录，`[subName]`是表名中`.`后面的部分
public class DirectoryStructure implements JsonTableFiles {
    public static final String ROOT_CONFIG_FILENAME = "config.cfg";
    public static final String CONFIG_EXT = "cfg";

    static class JsonFileList {
        List<JsonFileInfo> list = new ArrayList<>();
        Map<String, JsonFileInfo> map = new LinkedHashMap<>();
        /// 该表 JSON 文件所在目录（相对 rootDir），在发现时记录。
        /// 写入/删除必须用这个目录，避免与发现位置不一致导致数据被拆到两个目录。
        @Nullable Path tableDirRelativePath;


        void sort() {
            list = new ArrayList<>(map.values());
            if (map.values().stream().allMatch(JsonFileInfo::isIntegerId)) {
                list.sort(Comparator.comparingInt(JsonFileInfo::integerId));
            }
        }

        void addFile(JsonFileInfo info) {
            map.put(info.relativePath().toString(), info);
        }

        JsonFileList copy() {
            JsonFileList c = new JsonFileList();
            c.map = new LinkedHashMap<>(map);
            c.list = new ArrayList<>(list);
            c.tableDirRelativePath = tableDirRelativePath;
            return c;
        }

        public @Nullable JsonFileInfo removeFile(String jsonFileRelativePath) {
            return map.remove(jsonFileRelativePath);
        }

    }


    private final Path rootDir;
    /**
     * 配置文件信息
     */
    private final Map<String, CfgFileInfo> cfgFiles = new LinkedHashMap<>();  // relative file path -> info
    /**
     * excel文件信息
     */
    private volatile Map<String, ExcelFileInfo> excelFiles = new LinkedHashMap<>(); // relative file path -> info
    /**
     * json文件信息，可能被不同的线程访问，所以每次改变就创建新对象，改变引用
     */
    private volatile Map<String, JsonFileList> jsonFiles = new LinkedHashMap<>(); // table -> json file list

    public DirectoryStructure(Path rootDir) {
        this.rootDir = rootDir;
        CfgUtil.findConfigFilesRecursively(rootDir.resolve(ROOT_CONFIG_FILENAME),
                CONFIG_EXT, "", rootDir, cfgFiles);
        findExcelFilesRecursively(rootDir);
        findTableToJsonFiles();
    }

    public DirectoryStructure reload() {
        return new DirectoryStructure(rootDir);
    }

    public Path getRootDir() {
        return rootDir;
    }

    public Collection<CfgFileInfo> getCfgFiles() {
        // Return sorted collection to ensure consistent ordering across platforms
        return cfgFiles.values().stream()
                .sorted(Comparator.comparing(CfgFileInfo::pkgNameDot))
                .toList();
    }

    public @Nullable Path getCfgFilePathByPkgName(String pkgName) {
        String pkgNameDot = pkgName.isEmpty() ? "" : pkgName + ".";
        for (CfgFileInfo c : cfgFiles.values()) {
            if (c.pkgNameDot().equals(pkgNameDot)) {
                return c.path();
            }
        }
        return null;
    }

    public Collection<ExcelFileInfo> getExcelFiles() {
        return excelFiles.values();
    }

    @Override
    public Collection<JsonFileInfo> jsonFilesOf(String tableName) {
        return getJsonFilesByTable(tableName);
    }

    public Set<String> jsonTableNames() { return Set.copyOf(jsonFiles.keySet()); }

    public Collection<JsonFileInfo> getJsonFilesByTable(String tableName) {
        JsonFileList list = jsonFiles.get(tableName);
        if (list == null) {
            return List.of();
        }
        return list.list;
    }

    /// 返回该表 JSON 文件所在的目录（相对 rootDir）。
    /// 这是发现阶段记录的真实位置（根级 _skill_buff 或嵌套 skill/_buff 都可能），
    /// 写入/删除应据此进行，避免重新推导出另一个目录而把同一表的数据拆到两处。
    /// 表不是 JSON 表或尚未被发现（没有目录）时返回 null。
    public @Nullable Path getJsonTableDir(String tableName) {
        JsonFileList list = jsonFiles.get(tableName);
        return list == null ? null : list.tableDirRelativePath;
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
                            excelFiles.put(relativePath.toString(),
                                    new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, CSV));
                        }
                        case EXCEL -> {
                            excelFiles.put(relativePath.toString(),
                                    new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, EXCEL));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findTableToJsonFiles() {
        // tableName → 绝对路径，用于冲突检测
        Map<String, Path> discovered = new LinkedHashMap<>();

        try (Stream<Path> paths = Files.list(rootDir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path)) {
                    continue;
                }
                if (!Files.isDirectory(path)) {
                    continue;
                }

                String dirName = path.getFileName().toString();

                // 1. 旧格式：根级 _xxx 目录（如 _skill_buff）,这个格式不支持table名称有_，因为这里_会被变成.，被解析为模块名了
                // 所以优先用新格式
                String tableName = getTableNameIfTableDirForJson(dirName);
                if (tableName != null) {
                    Path existing = discovered.put(tableName, path);
                    if (existing != null) {
                        throw new RuntimeException("JSON table directory conflict for table '%s': both '%s' and '%s' exist"
                                .formatted(tableName, existing, path));
                    }
                    JsonFileList list = this.jsonFiles.computeIfAbsent(tableName, (String j) -> new JsonFileList());
                    findOneTableJsonFiles(path, list);
                    continue;
                }

                // 2. 新格式：模块目录内的 _子目录（如 buff/_skill），支持多层递归（如 a/b/_c）
                String codeName = getCodeName(dirName);
                if (codeName != null) {
                    findNestedJsonTableFiles(path, codeName, discovered);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /// 递归扫描模块目录内部的 _ 前缀子目录和更深的模块目录。
    /// 例如 a/b/_c 对应表名 a.b.c，会先递归进入 a → b → 发现 _c。
    private void findNestedJsonTableFiles(Path moduleDir, String pkgNameDot,
                                          Map<String, Path> discovered) {
        try (Stream<Path> subPaths = Files.list(moduleDir)) {
            for (Path subPath : subPaths.toList()) {
                if (isFileIgnored(subPath)) {
                    continue;
                }
                if (!Files.isDirectory(subPath)) {
                    continue;
                }

                String subDirName = subPath.getFileName().toString();

                // 检查是否是 JSON 表子目录（_前缀）
                String subTableName = getSubTableNameIfJsonSubDir(subDirName);
                if (subTableName != null) {
                    String fullTableName = pkgNameDot + "." + subTableName;
                    Path existing = discovered.put(fullTableName, subPath);
                    if (existing != null) {
                        throw new RuntimeException("JSON table directory conflict for table '%s': both '%s' and '%s' exist"
                                .formatted(fullTableName, existing, subPath));
                    }
                    JsonFileList list = this.jsonFiles.computeIfAbsent(fullTableName, (String j) -> new JsonFileList());
                    findOneTableJsonFiles(subPath, list);
                    continue;
                }

                // 递归进入更深的模块目录
                String subCodeName = getCodeName(subDirName);
                if (subCodeName != null) {
                    findNestedJsonTableFiles(subPath, pkgNameDot + "." + subCodeName, discovered);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void findOneTableJsonFiles(Path tableDir, JsonFileList list) {
        list.tableDirRelativePath = rootDir.relativize(tableDir);
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


    /** ------------------------------
     * 改变或增加json文件后，会导致watcher触发，
     * 但这里再在运行时记录下来此json的lastModified，然后通过 lastModifiedEquals 比较来避免全量makeValue
     */
    public synchronized JsonFileInfo addJsonFile(String tableName, Path relativeJsonPath) {
        Map<String, JsonFileList> tmp = copyJsonFiles(tableName);
        JsonFileList list = tmp.computeIfAbsent(tableName, (String j) -> new JsonFileList());
        list.tableDirRelativePath = relativeJsonPath.getParent();
        Path path = rootDir.resolve(relativeJsonPath).toAbsolutePath().normalize();
        JsonFileInfo jf = JsonFileInfo.of(path, relativeJsonPath);
        list.addFile(jf);
        list.sort();

        jsonFiles = tmp;
        return jf;
    }

    public synchronized void removeJsonFile(String tableName, Path relativeJsonPath) {
        Map<String, JsonFileList> tmp = copyJsonFiles(tableName);
        JsonFileList list = tmp.get(tableName);
        if (list == null) {
            return;
        }
        String jsonKey = relativeJsonPath.toString();
        JsonFileInfo jf = list.removeFile(jsonKey);
        if (jf == null) {
            return;
        }
        list.sort();

        jsonFiles = tmp;
    }

    public synchronized void updateExcelFileLastModified(Path relativeExcelPath) {
        String key = relativeExcelPath.toString();
        ExcelFileInfo oldInfo = excelFiles.get(key);
        if (oldInfo == null) {
            return;
        }
        ExcelFileInfo newInfo = new ExcelFileInfo(
                oldInfo.path().toFile().lastModified(),
                oldInfo.path(),
                relativeExcelPath,
                oldInfo.fmt()
        );
        Map<String, ExcelFileInfo> tmp = new LinkedHashMap<>(excelFiles);
        tmp.put(key, newInfo);
        excelFiles = tmp;
    }


    //------------------------------

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

        Map<String, JsonFileList> currentJson = jsonFiles;
        Map<String, JsonFileList> otherJson = other.jsonFiles;
        if (!cfgFiles.equals(other.cfgFiles)
                || !excelFiles.equals(other.excelFiles)
                || !currentJson.keySet().equals(otherJson.keySet())) {
            return false;
        }

        for (Map.Entry<String, JsonFileList> entry : currentJson.entrySet()) {
            JsonFileList current = entry.getValue();
            JsonFileList compared = otherJson.get(entry.getKey());
            if (!Objects.equals(current.tableDirRelativePath, compared.tableDirRelativePath)
                    || !current.map.equals(compared.map)) {
                return false;
            }
        }
        return true;
    }
}
