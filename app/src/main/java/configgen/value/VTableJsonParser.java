package configgen.value;

import configgen.ctx.DirectoryStructure;
import configgen.schema.TableSchema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.ctx.DirectoryStructure.*;
import static configgen.ctx.TextI18n.*;
import static configgen.data.Source.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValue.VTable;

public class VTableJsonParser {
    private final TableSchema subTableSchema;
    private final Path dataDir;
    private final TableSchema tableSchema;
    private final ValueErrs errs;
    private final ValueJsonParser parser;
    private final ValueStat valueStat;
    private final DirectoryStructure sourceStructure;

    public VTableJsonParser(TableSchema subTableSchema,
                            boolean isPartial,
                            DirectoryStructure sourceStructure,
                            TableSchema tableSchema,
                            TableI18n nullableTableI18n,
                            ValueErrs errs,
                            ValueStat valueStat) {
        this.subTableSchema = subTableSchema;
        this.dataDir = sourceStructure.getRootDir();
        this.sourceStructure = sourceStructure;
        this.tableSchema = tableSchema;
        this.parser = new ValueJsonParser(subTableSchema, isPartial, nullableTableI18n, errs);
        this.errs = errs;
        this.valueStat = valueStat;
    }

    public VTable parseTable() {
        List<VStruct> valueList = new ArrayList<>();
        String tableName = tableSchema.name();
        Map<String, Long> idMap = valueStat.getIdLastModifiedMap(tableName);
        Map<String, JsonFileInfo> jsonFiles = sourceStructure.getTableToJsonFiles().get(tableName);

        if (jsonFiles != null) {
            for (JsonFileInfo jf : jsonFiles.values()) {

                String jsonStr = null;
                long modified = 0;
                try {
                    jsonStr = Files.readString(jf.path());
                    modified = jf.lastModified();
                } catch (Exception e) {
                    errs.addErr(new ValueErrs.JsonFileReadErr(jf.path().toString(), e.getMessage()));
                }
                if (jsonStr != null) {
                    VStruct vStruct = parser.fromJson(jsonStr,
                            DFile.of(jf.relativePath().toString(), tableName));

                    valueList.add(vStruct);
                    Value pkValue = ValueUtil.extractPrimaryKeyValue(vStruct, tableSchema);
                    String id = pkValue.packStr();
                    idMap.put(id, modified);
                }
            }
        }
        if (idMap.isEmpty()) { // no json file
            //创建默认的一个record，供cfgeditor开始update或add
            VStruct defaultValue = ValueDefault.ofStructural(tableSchema, DFile.of("<new>", tableName));
            Value pkValue = ValueUtil.extractPrimaryKeyValue(defaultValue, tableSchema);
            String id = pkValue.packStr();
            Path writePath = dataDir;
            try {
                writePath = VTableJsonStore.addOrUpdateRecordStore(defaultValue, tableSchema, id, dataDir, valueStat);
                valueList.add(defaultValue);
                sourceStructure.addJsonInPlace(tableName, writePath);
            } catch (Exception e) {
                errs.addErr(new ValueErrs.JsonFileWriteErr(writePath.toAbsolutePath().toString(), e.getMessage()));
            }

        }
        return new VTableCreator(subTableSchema, errs).create(valueList);
    }

}
