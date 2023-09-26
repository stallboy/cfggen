package configgen.data;

import configgen.Logger;
import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;

import java.util.Map;
import java.util.TreeMap;

import static configgen.data.CfgData.TableData;

public record CfgDataHeader(Map<String, TableDataHeader> tables) {

    public static CfgDataHeader of() {
        return new CfgDataHeader(new TreeMap<>());
    }

    public static CfgDataHeader of(CfgData cfgData, CfgSchema nullableCfgSchema) {
        CfgDataHeader headers = CfgDataHeader.of();
        for (TableData tableData : cfgData.tables().values()) {
            String name = tableData.tableName();
            boolean isColumnMode = false;
            if (nullableCfgSchema != null) {
                TableSchema schema = nullableCfgSchema.findTable(name);
                if (schema != null){
                    isColumnMode = schema.isColumnMode();
                }else{
                    Logger.log(STR. "\{name} in data not in schema");
                }
            }

            TableDataHeader header = TableDataHeader.of(tableData, isColumnMode);
            headers.tables.put(tableData.tableName(), header);
        }
        return headers;
    }
}
