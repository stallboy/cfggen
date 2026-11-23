package configgen.value;

import configgen.layout.JsonFileInfo;
import configgen.schema.TableSchema;

import java.nio.file.Files;
import java.util.*;

import static configgen.data.Source.*;
import static configgen.value.CfgValue.*;

public class VTableJsonParser {
    private final TableSchema subTableSchema;
    private final CfgValueErrs errs;
    private final ValueJsonParser parser;
    private final CfgValueStat valueStat;

    public VTableJsonParser(TableSchema subTableSchema,
                            boolean isPartial,
                            CfgValueErrs errs,
                            CfgValueStat valueStat) {
        this.subTableSchema = subTableSchema;
        this.parser = new ValueJsonParser(subTableSchema, isPartial, errs);
        this.errs = errs;
        this.valueStat = valueStat;
    }

    public VTable parseTable(Collection<JsonFileInfo> jsonFiles) {
        return new VTableCreator(subTableSchema, errs).create(parseRecords(jsonFiles));
    }

    public List<VStruct> parseRecords(Collection<JsonFileInfo> jsonFiles) {
        List<VStruct> valueList = new ArrayList<>();
        String tableName = subTableSchema.name();
        Map<String, Long> idMap = new LinkedHashMap<>();

        for (JsonFileInfo jf : jsonFiles) {
            String jsonStr;
            try {
                jsonStr = Files.readString(jf.path());
            } catch (Exception e) {
                errs.addErr(new CfgValueErrs.JsonFileReadErr(jf.path().toString(), e.getMessage()));
                continue;
            }
            VStruct vStruct = parser.fromJson(jsonStr,
                    DFile.of(jf.relativePath().toString(), tableName));
            valueList.add(vStruct);
            Value pkValue = ValueUtil.extractPrimaryKeyValue(vStruct, subTableSchema);
            idMap.put(pkValue.packStr(), jf.lastModified());
        }

        valueStat.newTableLastModified(tableName, idMap);
        return valueList;
    }

}
