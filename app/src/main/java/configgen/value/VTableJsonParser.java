package configgen.value;

import configgen.schema.TableSchema;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.ctx.TextI18n.*;
import static configgen.data.Source.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValue.VTable;

public class VTableJsonParser {
    private final TableSchema subTableSchema;
    private final Path dataDir;
    private final String jsonDirName;
    private final Path jsonDir;
    private final TableSchema tableSchema;
    private final ValueErrs errs;
    private final ValueJsonParser parser;
    private final ValueStat valueStat;

    public VTableJsonParser(TableSchema subTableSchema,
                            boolean isPartial,
                            Path dataDir,
                            TableSchema tableSchema,
                            TableI18n nullableTableI18n,
                            ValueErrs errs,
                            ValueStat valueStat) {
        this.subTableSchema = subTableSchema;
        this.dataDir = dataDir;
        this.jsonDirName = VTableJsonStore.getJsonTableDirName(tableSchema);
        this.jsonDir = dataDir.resolve(jsonDirName);
        this.tableSchema = tableSchema;
        this.parser = new ValueJsonParser(subTableSchema, isPartial, nullableTableI18n, errs);
        this.errs = errs;
        this.valueStat = valueStat;
    }

    public VTable parseTable() {
        List<VStruct> valueList = new ArrayList<>();
        Map<String, Long> idMap = valueStat.getIdLastModifiedMap(tableSchema.name());
        if (Files.isDirectory(jsonDir)) {
            File[] files = jsonDir.toFile().listFiles();
            if (files != null) {

                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        Path path = file.toPath();
                        String jsonStr = null;
                        long modified = 0;
                        try {
                            jsonStr = Files.readString(path);
                            modified = Files.getLastModifiedTime(path).toMillis();
                        } catch (Exception e) {
                            errs.addErr(new ValueErrs.JsonFileReadErr(file.getAbsolutePath(), e.getMessage()));
                        }
                        if (jsonStr != null) {
                            VStruct vStruct = parser.fromJson(jsonStr,
                                    DFile.of(jsonDirName + "/" + file.getName(), tableSchema.name()));

                            valueList.add(vStruct);
                            Value pkValue = ValueUtil.extractPrimaryKeyValue(vStruct, tableSchema);
                            String id = pkValue.packStr();
                            idMap.put(id, modified);
                        }

                    }
                }
            }
        } else { //创建默认的一个record，供cfgeditor开始update或add
            VStruct defaultValue = ValueDefault.ofStructural(tableSchema, DFile.of("<new>", tableSchema.name()));
            Value pkValue = ValueUtil.extractPrimaryKeyValue(defaultValue, tableSchema);
            String id = pkValue.packStr();
            Path writePath = dataDir;
            try {
                writePath = VTableJsonStore.addOrUpdateRecordStore(defaultValue, tableSchema, id, dataDir, valueStat);
                valueList.add(defaultValue);
            } catch (Exception e) {
                errs.addErr(new ValueErrs.JsonFileWriteErr(writePath.toAbsolutePath().toString(), e.getMessage()));
            }
        }
        return new VTableCreator(subTableSchema, errs).create(valueList);
    }

}
