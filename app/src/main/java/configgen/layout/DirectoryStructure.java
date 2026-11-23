package configgen.layout;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static configgen.layout.FileFormat.CSV;
import static configgen.layout.FileFormat.EXCEL;
import static configgen.layout.FileNameUtil.getCodeName;
import static configgen.layout.FileNameUtil.isFileIgnored;

/// # 文件目录规范
///
/// ## 模块目录与表名
/// - 模块目录、CSV 文件和 Excel 工作表的名称以英文字母开头。
/// - 提取代码名时，先截去第一个 `.` 及其后面的内容，再截去第一个汉字及其后面的内容。
///   汉字前紧邻的一个下划线也会被截去，结果转为小写。
/// - 例如 `skill_技能` 的模块名是 `skill`。多级模块名用 `.` 连接。
///
/// ## Schema 文件
/// - 默认根文件为 `config.cfg`。普通数据目录缺少该文件时仍会发现模块 CFG。
/// - 各级模块目录中的 CFG 以模块代码名命名。例如 `skill_技能/buff_效果/buff.cfg` 属于 `skill.buff`。
///
/// ## 数据文件
/// 表格和 JSON 扫描会忽略隐藏文件、隐藏目录，以及名称以 `~` 开头的文件和目录。
///
/// ### CSV
/// - 文件后缀为 `.csv`，扩展名识别忽略大小写。
/// - 文件代码名使用 `[table]` 或 `[table]_[idx]`。文件所在的模块目录补全表名。
/// - 例如 `skill_技能/buff_2_效果.csv` 属于表 `skill.buff`，分片编号是 `2`。
///
/// ### Excel
/// - 文件后缀为 `.xls` 或 `.xlsx`，扩展名识别忽略大小写。
/// - 以工作簿所在的模块目录和工作表名确定表名。工作表代码名使用 `[table]` 或 `[table]_[idx]`。
/// - CSV 和 Excel 的分片后缀都按整数解析，缺少整数后缀时分片编号为 `0`。
///
/// ### JSON
/// - **嵌套格式**：模块目录下的 `_[subName]`，例如 `skill_技能/_buff` 对应表 `skill.buff`。
///   支持多级模块目录，子表名中的下划线会保留。
/// - **根级格式**：根目录下的 `_[table.replace(".", "_")]`，例如 `_skill_buff` 对应表 `skill.buff`。
///   扫描根级表目录时，名称前缀之后的下划线会解析为模块分隔符。
/// - **冲突**：同一表名出现于多个目录时报告冲突，例如同时存在 `_skill_buff` 和 `skill/_buff`。
/// - 表目录中的常规文件使用小写 `.json` 后缀，每个文件对应一条记录。
/// - 索引会保留已有表目录和记录的实际位置。新表写入时优先使用能完整匹配的模块目录链，
///   无法匹配时使用根级格式。
public class DirectoryStructure {
    private record JsonTable(@Nullable Path directory,
                             Map<String, JsonFileInfo> files,
                             List<JsonFileInfo> orderedFiles) {
        private JsonTable(@Nullable Path directory, Map<String, JsonFileInfo> files) {
            this(directory, Collections.unmodifiableMap(files), orderedFiles(files));
        }

        private static List<JsonFileInfo> orderedFiles(Map<String, JsonFileInfo> files) {
            if (files.values().stream().allMatch(JsonFileInfo::isIntegerId)) {
                return files.values().stream()
                        .sorted(Comparator.comparingInt(JsonFileInfo::integerId))
                        .toList();
            }
            return List.copyOf(files.values());
        }
    }

    private final Path rootDir;
    private final Set<CfgFileInfo> cfgFiles;
    // Writers replace the maps so readers can continue using the snapshot they obtained.
    private volatile Map<String, ExcelFileInfo> excelFiles;
    private volatile Map<String, JsonTable> jsonTables;

    public DirectoryStructure(Path rootDir) {
        this.rootDir = rootDir;
        cfgFiles = SchemaLayout.findCfgFiles(SchemaLayout.cfgRootFile(rootDir));
        Map<String, ExcelFileInfo> spreadsheets = new LinkedHashMap<>();
        scanSpreadsheets(rootDir, spreadsheets);
        excelFiles = Collections.unmodifiableMap(spreadsheets);

        Map<String, JsonTable> tables = new LinkedHashMap<>();
        scanJsonTables(rootDir, "", tables);
        jsonTables = tables;
    }

    public DirectoryStructure reload() {
        return new DirectoryStructure(rootDir);
    }

    public Path getRootDir() {
        return rootDir;
    }

    public Collection<CfgFileInfo> getCfgFiles() {
        // Return sorted collection to ensure consistent ordering across platforms
        return cfgFiles.stream()
                .sorted(Comparator.comparing(CfgFileInfo::pkgNameDot))
                .toList();
    }

    public @Nullable Path getCfgFilePathByPkgName(String pkgName) {
        String pkgNameDot = pkgName.isEmpty() ? "" : pkgName + ".";
        for (CfgFileInfo c : cfgFiles) {
            if (c.pkgNameDot().equals(pkgNameDot)) {
                return c.path();
            }
        }
        return null;
    }

    public Collection<ExcelFileInfo> getExcelFiles() {
        return excelFiles.values();
    }

    public Set<String> jsonTableNames() {
        return Set.copyOf(jsonTables.keySet());
    }

    public List<JsonFileInfo> getJsonFilesByTable(String tableName) {
        JsonTable table = jsonTables.get(tableName);
        return table == null ? List.of() : table.orderedFiles();
    }

    /// 相对根目录的实际表目录，空表也保留其位置。
    public @Nullable Path getJsonTableDir(String tableName) {
        JsonTable table = jsonTables.get(tableName);
        return table == null ? null : table.directory();
    }

    private void scanSpreadsheets(Path dir, Map<String, ExcelFileInfo> spreadsheets) {
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path)) {
                    continue;
                }
                String fileName = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    if (getCodeName(fileName) != null) {
                        scanSpreadsheets(path, spreadsheets);
                    }
                } else if (Files.isRegularFile(path)) {
                    FileFormat format = FileFormat.getFileFormat(fileName);
                    if (format == EXCEL || (format == CSV && getCodeName(fileName) != null)) {
                        Path relativePath = rootDir.relativize(path);
                        spreadsheets.put(relativePath.toString(),
                                new ExcelFileInfo(path.toFile().lastModified(), path, relativePath, format));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void scanJsonTables(Path dir, String modulePrefix, Map<String, JsonTable> tables) {
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path) || !Files.isDirectory(path)) {
                    continue;
                }
                String dirName = path.getFileName().toString();
                String tableName = JsonLayout.tableNameForDirectory(modulePrefix, dirName);
                if (tableName != null) {
                    JsonTable existing = tables.get(tableName);
                    if (existing != null) {
                        throw new RuntimeException("JSON table directory conflict for table '%s': both '%s' and '%s' exist"
                                .formatted(tableName, rootDir.resolve(Objects.requireNonNull(existing.directory())), path));
                    }
                    tables.put(tableName, scanJsonTable(path));
                } else {
                    String moduleName = getCodeName(dirName);
                    if (moduleName != null) {
                        scanJsonTables(path, modulePrefix + moduleName + ".", tables);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonTable scanJsonTable(Path tableDir) throws IOException {
        Map<String, JsonFileInfo> files = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(tableDir)) {
            for (Path path : paths.toList()) {
                if (isFileIgnored(path) || !Files.isRegularFile(path)
                        || !JsonLayout.isRecordFileName(path.getFileName().toString())) {
                    continue;
                }
                Path relativePath = rootDir.relativize(path);
                files.put(relativePath.toString(), JsonFileInfo.of(path.toAbsolutePath().normalize(), relativePath));
            }
        }
        return new JsonTable(rootDir.relativize(tableDir), files);
    }

    /// 写回后更新索引，避免 Watcher 将本地写入误判为需要重新加载的外部修改。
    public synchronized JsonFileInfo addJsonFile(String tableName, Path relativeJsonPath) {
        JsonTable table = jsonTables.get(tableName);
        Map<String, JsonFileInfo> files = table == null ? new LinkedHashMap<>() : new LinkedHashMap<>(table.files());
        Path path = rootDir.resolve(relativeJsonPath).toAbsolutePath().normalize();
        JsonFileInfo file = JsonFileInfo.of(path, relativeJsonPath);
        files.put(relativeJsonPath.toString(), file);
        updateJsonTable(tableName, new JsonTable(relativeJsonPath.getParent(), files));
        return file;
    }

    public synchronized void removeJsonFile(String tableName, Path relativeJsonPath) {
        JsonTable table = jsonTables.get(tableName);
        String key = relativeJsonPath.toString();
        if (table == null || !table.files().containsKey(key)) {
            return;
        }
        Map<String, JsonFileInfo> files = new LinkedHashMap<>(table.files());
        files.remove(key);
        updateJsonTable(tableName, new JsonTable(table.directory(), files));
    }

    private void updateJsonTable(String tableName, JsonTable table) {
        Map<String, JsonTable> updated = new LinkedHashMap<>(jsonTables);
        updated.put(tableName, table);
        jsonTables = updated;
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
        excelFiles = Collections.unmodifiableMap(tmp);
    }


    public boolean lastModifiedEquals(DirectoryStructure other) {
        Objects.requireNonNull(other);

        Map<String, JsonTable> currentJson = jsonTables;
        Map<String, JsonTable> otherJson = other.jsonTables;
        if (!cfgFiles.equals(other.cfgFiles)
                || !excelFiles.equals(other.excelFiles)
                || currentJson.size() != otherJson.size()) {
            return false;
        }

        for (Map.Entry<String, JsonTable> entry : currentJson.entrySet()) {
            JsonTable current = entry.getValue();
            JsonTable compared = otherJson.get(entry.getKey());
            if (compared == null || !Objects.equals(current.directory(), compared.directory())
                    || !current.files().equals(compared.files())) {
                return false;
            }
        }
        return true;
    }
}
