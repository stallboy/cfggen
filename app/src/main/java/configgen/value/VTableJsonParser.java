package configgen.value;

import configgen.data.JsonFileInfo;
import configgen.data.JsonTableFiles;
import configgen.schema.TableSchema;

import java.nio.file.Files;
import java.util.*;

import static configgen.data.Source.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValue.VTable;

public class VTableJsonParser {
    private final TableSchema subTableSchema;
    private final TableSchema tableSchema;
    private final CfgValueErrs errs;
    private final ValueJsonParser parser;
    private final CfgValueStat valueStat;
    private final JsonTableFiles jsonTableFiles;

    public VTableJsonParser(TableSchema subTableSchema,
                            boolean isPartial,
                            JsonTableFiles jsonTableFiles,
                            TableSchema tableSchema,
                            CfgValueErrs errs,
                            CfgValueStat valueStat) {
        this.subTableSchema = subTableSchema;
        this.jsonTableFiles = jsonTableFiles;
        this.tableSchema = tableSchema;
        this.parser = new ValueJsonParser(subTableSchema, isPartial, errs);
        this.errs = errs;
        this.valueStat = valueStat;
    }

    public VTable parseTable() {
        return new VTableCreator(subTableSchema, errs).create(parseRecords());
    }

    public List<VStruct> parseRecords() {
        List<VStruct> valueList = new ArrayList<>();
        String tableName = tableSchema.name();
        Map<String, Long> idMap = new LinkedHashMap<>();
        Collection<JsonFileInfo> jsonFiles = jsonTableFiles.jsonFilesOf(tableName);

        for (JsonFileInfo jf : jsonFiles) {
            String jsonStr = null;
            long modified = 0;
            try {
                jsonStr = Files.readString(jf.path());
                modified = jf.lastModified();
            } catch (Exception e) {
                errs.addErr(new CfgValueErrs.JsonFileReadErr(jf.path().toString(), e.getMessage()));
            }
            if (jsonStr != null) {
                VStruct vStruct = parser.fromJson(jsonStr,
                        DFile.of(jf.relativePath().toString(), tableName));

                valueList.add(vStruct);
                Value pkValue = ValueUtil.extractPrimaryKeyValue(vStruct, subTableSchema);
                String id = pkValue.packStr();
                idMap.put(id, modified);
            }
        }

        valueStat.newTableLastModified(tableName, idMap);
        return valueList;
    }

}
