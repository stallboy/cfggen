package configgen.genjson;

import com.alibaba.fastjson2.JSON;
import configgen.ctx.Context;
import configgen.data.Source.DFile;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.HasMap;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.write.VTableJsonStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static configgen.value.CfgValue.*;

public class JsonGenerator extends GeneratorWithTag {
    private static final String MANIFEST = ".cfggen-json-files";
    private final String tables;
    private final String dst;

    public JsonGenerator(Parameter parameter) {
        super(parameter);
        tables = parameter.get("tables", "");
        dst = parameter.get("dst", ".");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        String[] tableNames = tables.split(";");
        if (tableNames.length == 0) {
            return;
        }
        if (tag != null) {
            Logger.log("gen json with tag=%s, be careful!!!", tag);
        }

        CfgValue cfgValue = ctx.makeValue(tag);

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

            exportTable(ctx, vTable, Path.of(dst).toAbsolutePath().normalize());
        }
    }

    private static synchronized void exportTable(Context ctx, VTable table, Path destination) throws IOException {
        Path directory = destination.resolve(VTableJsonStorage.relativeTableDirectory(
                table.name(), destination, ctx.sourceStructure()));
        Path source = ctx.sourceStructure().getJsonTableDir(table.name());
        if (table.schema().isJson() && source != null && Files.isDirectory(ctx.rootDir().resolve(source))
                && Files.isDirectory(directory)
                && Files.isSameFile(directory, ctx.rootDir().resolve(source))) {
            for (VStruct record : table.primaryKeyMap().values()) {
                DFile file = (DFile) record.source();
                VTableJsonStorage.writeRecord(record, ctx.rootDir().resolve(file.fileName()), ctx.outputFiles());
            }
            return;
        }

        Path manifest = directory.resolve(MANIFEST);
        Set<String> previous = new LinkedHashSet<>();
        if (Files.exists(manifest)) {
            previous.addAll(JSON.parseArray(Files.readString(manifest), String.class));
        }

        Set<String> written = new LinkedHashSet<>();
        try {
            for (Map.Entry<Value, VStruct> entry : table.primaryKeyMap().entrySet()) {
                Path path = VTableJsonStorage.addOrUpdateRecord(entry.getValue(), table.name(), entry.getKey().packStr(),
                        destination, ctx.sourceStructure(), ctx.outputFiles());
                written.add(path.getFileName().toString());
            }
            Set<Path> retained = written.stream().map(directory::resolve).collect(Collectors.toSet());
            for (String name : previous) {
                if (isRecordFileName(name)) {
                    Path stale = directory.resolve(name);
                    if (!retained.contains(stale) && !Files.isDirectory(stale, LinkOption.NOFOLLOW_LINKS)) {
                        Files.deleteIfExists(stale);
                    }
                }
            }
            if (!written.isEmpty() || Files.exists(manifest)) {
                writeManifest(ctx, manifest, written);
            }
        } catch (IOException | RuntimeException failure) {
            if (!written.isEmpty()) {
                previous.addAll(written);
                try {
                    writeManifest(ctx, manifest, previous);
                } catch (IOException | RuntimeException manifestFailure) {
                    failure.addSuppressed(manifestFailure);
                }
            }
            throw failure;
        }
    }

    private static boolean isRecordFileName(String name) {
        if (name == null || !name.endsWith(".json") || name.contains("/") || name.contains("\\")) {
            return false;
        }
        try {
            return Path.of(name).getRoot() == null;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    private static void writeManifest(Context context, Path manifest, Set<String> files) throws IOException {
        context.outputFiles().writeFileReplacement(manifest,
                JSON.toJSONString(files).getBytes(StandardCharsets.UTF_8));
    }
}
