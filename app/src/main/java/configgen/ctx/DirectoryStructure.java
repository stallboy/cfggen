package configgen.ctx;

import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static configgen.data.DataUtil.*;
import static configgen.data.DataUtil.FileFmt.CSV;
import static configgen.data.DataUtil.FileFmt.EXCEL;
import static configgen.value.VTableJsonStore.getJsonTableDirName;
import static java.nio.file.FileVisitResult.*;

/**
 * <p> 目录视为table规则： 首字母是英文字符的视为table目录。 截取.之前的，再截取 _汉字或汉字之前的，作为table名 </p>
 * <ul>
 * <li> schema文件：根目录下有config.cfg，子目录下有[table].cfg</li>
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

    public record DataFileInfo(long lastModified,
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
    private final Map<String, DataFileInfo> dataFiles = new LinkedHashMap<>(); // file path -> info
    private final Map<String, Map<String, JsonFileInfo>> tableToJsonFiles = new LinkedHashMap<>(); // table -> file path -> info

    public DirectoryStructure(Path rootDir) {
        this.rootDir = rootDir;
        findConfigFilesFromAllSubDirectory(rootDir.resolve(ROOT_CONFIG_FILENAME), CONFIG_EXT, "",
                rootDir, cfgFiles);
        findDataFiles();
    }

    public Path getRootDir() {
        return rootDir;
    }

    public Map<String, CfgFileInfo> getCfgFiles() {
        return cfgFiles;
    }

    public Map<String, DataFileInfo> getDataFiles() {
        return dataFiles;
    }

    public Map<String, Map<String, JsonFileInfo>> getTableToJsonFiles() {
        return tableToJsonFiles;
    }

    private static void findConfigFilesFromAllSubDirectory(Path source, String ext, String pkgNameDot,
                                                           Path rootDir, Map<String, CfgFileInfo> cfgFiles) {
        if (Files.exists(source)) {
            Path relativizeSource = rootDir.relativize(source);
            cfgFiles.put(relativizeSource.toString(),
                    new CfgFileInfo(source.toFile().lastModified(), source, relativizeSource, pkgNameDot));
        }
        try {
            try (Stream<Path> paths = Files.list(source.getParent())) {
                for (Path path : paths.toList()) {
                    if (Files.isDirectory(path)) {
                        String lastDir = path.getFileName().toString().toLowerCase();
                        String subPkgName = getCodeName(lastDir);

                        Path subSource = path.resolve(subPkgName + "." + ext);
                        String subPkgNameDot = pkgNameDot + subPkgName + ".";
                        findConfigFilesFromAllSubDirectory(subSource, ext, subPkgNameDot,
                                rootDir, cfgFiles);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, CfgFileInfo> findAllXmlFiles(Path rootDir) {
        Map<String, CfgFileInfo> result = new LinkedHashMap<>();
        findConfigFilesFromAllSubDirectory(rootDir.resolve("config.xml"), "xml", "",
                rootDir, result);
        return result;
    }

    private void findDataFiles() {
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes a) {
                    Path relativePath = rootDir.relativize(filePath);
                    Path path = filePath.toAbsolutePath().normalize();
                    if (isFileIgnored(path)) {
                        return CONTINUE;
                    }
                    FileFmt fmt = getFileFormat(path);
                    if (fmt == null) {
                        return CONTINUE;
                    }
                    switch (fmt) {
                        case CSV -> {
                            TableNameIndex ti = getTableNameIndex(relativePath);
                            dataFiles.put(relativePath.toString(),
                                    new DataFileInfo(path.toFile().lastModified(), path, relativePath, CSV, ti));
                        }
                        case EXCEL -> {
                            dataFiles.put(relativePath.toString(),
                                    new DataFileInfo(path.toFile().lastModified(), path, relativePath, EXCEL, null));
                        }
                    }
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void findJsonFilesFromSchema(CfgSchema schema) {
        tableToJsonFiles.clear();
        for (TableSchema t : schema.sortedTables()) {
            if (!t.meta().isJson()) {
                continue;
            }
            Path jsonDir = Path.of(getJsonTableDirName(t));
            if (!Files.isDirectory(jsonDir)) {
                continue;
            }

            File[] files = jsonDir.toFile().listFiles();
            if (files == null) {
                continue;
            }
            Map<String, JsonFileInfo> jsonFiles = tableToJsonFiles.computeIfAbsent(t.name(), (String j) -> new LinkedHashMap<>());
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    Path relativePath = jsonDir.resolve(file.getName());
                    Path path = file.toPath().toAbsolutePath().normalize();
                    jsonFiles.put(relativePath.toString(),
                            new JsonFileInfo(file.lastModified(), path, relativePath));
                }
            }
        }
    }

    public void addJsonInPlace(Map<String, JsonFileInfo> jsonFiles, Path jsonPath) {
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
