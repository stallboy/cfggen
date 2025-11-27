package configgen.value;

import configgen.ctx.DirectoryStructure;
import configgen.schema.TableSchema;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static configgen.ctx.DirectoryStructure.*;
import static configgen.data.Source.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValue.VTable;

public class VTableJsonParser {
    private final TableSchema subTableSchema;
    private final TableSchema tableSchema;
    private final CfgValueErrs errs;
    private final ValueJsonParser parser;
    private final CfgValueStat valueStat;
    private final DirectoryStructure sourceStructure;

    public VTableJsonParser(TableSchema subTableSchema,
                            boolean isPartial,
                            DirectoryStructure sourceStructure,
                            TableSchema tableSchema,
                            CfgValueErrs errs,
                            CfgValueStat valueStat) {
        this.subTableSchema = subTableSchema;
        this.sourceStructure = sourceStructure;
        this.tableSchema = tableSchema;
        this.parser = new ValueJsonParser(subTableSchema, isPartial, errs);
        this.errs = errs;
        this.valueStat = valueStat;
    }

    public VTable parseTable() {
        List<VStruct> valueList = new ArrayList<>();
        String tableName = tableSchema.name();
        Map<String, Long> idMap = valueStat.getIdLastModifiedMap(tableName);
        Collection<JsonFileInfo> jsonFiles = sourceStructure.getJsonFilesByTable(tableName);

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
                Value pkValue = ValueUtil.extractPrimaryKeyValue(vStruct, tableSchema);
                String id = pkValue.packStr();
                idMap.put(id, modified);
            }
        }
        return new VTableCreator(subTableSchema, errs).create(valueList);
    }

}
