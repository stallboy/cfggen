package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import configgen.gen.Generator;
import configgen.ctx.TextI18n;
import configgen.schema.TableSchema;
import configgen.util.CachedFiles;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.*;
import static configgen.value.CfgValue.VTable;

public class VTableJsonParser {
    private final TableSchema subTableSchema;
    private final Path dataDir;
    private final Path jsonDir;
    private final TableSchema tableSchema;
    private final ValueErrs errs;
    private final ValueJsonParser parser;

    public VTableJsonParser(TableSchema subTableSchema, Path dataDir, TableSchema tableSchema,
                            TextI18n.TableI18n nullableTableI18n, ValueErrs errs) {
        this.subTableSchema = subTableSchema;
        this.dataDir = dataDir;
        this.jsonDir = getJsonTableDir(tableSchema, dataDir);
        this.tableSchema = tableSchema;
        this.parser = new ValueJsonParser(subTableSchema, nullableTableI18n);
        this.errs = errs;
    }

    public VTable parseTable() {
        List<VStruct> valueList = new ArrayList<>();
        if (Files.isDirectory(jsonDir)) {
            File[] files = jsonDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        try {
                            String jsonStr = Files.readString(file.toPath());
                            try {
                                VStruct vStruct = parser.fromJson(jsonStr, file.getName());
                                valueList.add(vStruct);
                            } catch (ValueJsonParser.JsonParseException e) {
                                errs.addErr(new ValueErrs.JsonFileParseErr(file.getAbsolutePath(), e.getMessage()));
                            }
                        } catch (Exception e) {
                            errs.addErr(new ValueErrs.JsonFileReadErr(file.getAbsolutePath(), e.getMessage()));
                        }
                    }
                }
            }
        } else { //创建默认的一个record，供cfgeditor开始update或add
            VStruct defaultValue = ValueDefault.ofStructural(tableSchema);
            Value pkValue = ValueUtil.extractPrimaryKeyValue(defaultValue, tableSchema);
            String id = pkValue.packStr();
            Path writePath = dataDir;
            try {
                writePath = addOrUpdateRecordStore(defaultValue, tableSchema, id, dataDir);
                valueList.add(defaultValue);
            } catch (Exception e) {
                errs.addErr(new ValueErrs.JsonFileWriteErr(writePath.toAbsolutePath().toString(), e.getMessage()));
            }
        }
        return new VTableCreator(subTableSchema, errs).create(valueList);
    }

    private static Path getJsonTableDir(TableSchema tableSchema, Path dataDir) {
        String dir = "_" + tableSchema.name().replace(".", "_");
        return dataDir.resolve(dir);
    }

    public static Path addOrUpdateRecordStore(VStruct record, TableSchema tableSchema, String id, Path dataDir) throws IOException {
        Path jsonDir = getJsonTableDir(tableSchema, dataDir);
        Path recordPath = jsonDir.resolve(id + ".json");
        try (OutputStreamWriter writer = Generator.createUtf8Writer(recordPath.toFile())) {
            JSONObject jsonObject = new ValueToJson().toJson(record);
            String jsonString = JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
            writer.write(jsonString);
            return recordPath;
        }
    }

    public static boolean deleteRecordStore(TableSchema tableSchema, String id, Path dataDir) {
        Path jsonDir = getJsonTableDir(tableSchema, dataDir);
        Path recordPath = jsonDir.resolve(id + ".json");
        return CachedFiles.delete(recordPath.toFile());
    }

}
