package configgen.genjson;

import configgen.ctx.Context;
import configgen.data.DataUtil;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.HasMap;
import configgen.util.CachedFiles;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.write.VTableJsonStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class JsonGenerator extends GeneratorWithTag {
    private final String tables;
    private final String dst;

    public JsonGenerator(Parameter parameter) {
        super(parameter);
        tables = parameter.get("tables", "");
        dst = parameter.get("dst", ".");
    }

    @Override
    public void generate(Context ctx, CachedFiles batchOutputs) throws IOException {
        if (tables.isBlank()) {
            return;
        }
        String[] tableNames = tables.split(";");
        if (tableNames.length == 0) {
            return;
        }
        if (tag != null) {
            Logger.log("gen json with tag=%s, be careful!!!", tag);
        }

        exportTables(ctx, ctx.makeValue(tag), tableNames, Path.of(dst).toAbsolutePath().normalize(), batchOutputs);
    }

    private record Export(String table, Map<Path, VStruct> records) {}

    private static synchronized void exportTables(Context ctx, CfgValue cfgValue, String[] tableNames,
                                                   Path destination, CachedFiles outputFiles) throws IOException {
        Path outputDirectory = resolveOutputDirectory(destination);
        Path dataDirectory = ctx.rootDir().toRealPath();
        if (outputDirectory.startsWith(dataDirectory) || dataDirectory.startsWith(outputDirectory)) {
            throw new IOException("JSON export directory " + outputDirectory + " overlaps the data directory "
                    + dataDirectory + ". Set dst outside the data directory.");
        }

        Map<Path, Export> exports = new LinkedHashMap<>();
        for (String table : tableNames) {
            VTable vTable = cfgValue.getTable(table);
            if (vTable == null) {
                Logger.log("ignore gen json: table=%s not found!", table);
                continue;
            }

            if (HasMap.hasMap(vTable.schema())) {
                Logger.log("ignore gen json: table=%s has map!", table);
                continue;
            }

            Path directory = outputDirectory.resolve(outputTableDirectory(table));
            if (!resolveOutputDirectory(directory).equals(directory)) {
                throw new IOException("JSON export table directory must not redirect to another path: " + directory);
            }
            Export previous = exports.get(directory);
            if (previous != null) {
                if (!previous.table().equals(table)) {
                    throw directoryConflict(directory, previous.table(), table);
                }
                continue;
            }
            exports.put(directory, new Export(table, planRecords(vTable, directory)));
        }
        if (exports.isEmpty()) {
            return;
        }
        outputFiles.prepareOutputDirectory(outputDirectory);
        for (Export export : exports.values()) {
            for (Map.Entry<Path, VStruct> entry : export.records().entrySet()) {
                VTableJsonStorage.writeRecord(entry.getValue(), entry.getKey(), outputFiles);
            }
        }
    }

    private static Path outputTableDirectory(String table) {
        Path name = Path.of(table.replace('.', '/'));
        return name.resolveSibling("_" + name.getFileName());
    }

    private static Path resolveOutputDirectory(Path path) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) || path.getParent() == null) {
            Path directory = path.toRealPath();
            if (!Files.isDirectory(directory)) {
                throw new IOException("JSON export needs a directory, but the path is occupied by a file: " + path);
            }
            return directory;
        }
        return resolveOutputDirectory(path.getParent()).resolve(path.getFileName());
    }

    private static Map<Path, VStruct> planRecords(VTable table, Path directory) throws IOException {
        Map<Path, VStruct> records = new LinkedHashMap<>();
        for (Map.Entry<Value, VStruct> entry : table.primaryKeyMap().entrySet()) {
            Path fileName = DataUtil.jsonRecordFileName(entry.getKey().packStr(), directory.getFileSystem());
            Path path = directory.resolve(fileName);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("JSON export needs a regular file at " + path);
            }
            if (records.putIfAbsent(path, entry.getValue()) != null) {
                throw new IOException("Multiple records in table " + table.name() + " export to " + path);
            }
        }
        return records;
    }

    private static IOException directoryConflict(Path directory, String owner, String table) {
        return new IOException("JSON export directory " + directory + " is shared by tables " + owner + " and " + table
                + ". Export the tables to different directories.");
    }
}
