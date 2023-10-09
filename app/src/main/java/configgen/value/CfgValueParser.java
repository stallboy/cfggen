package configgen.value;

import configgen.data.CfgData;
import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;

import java.util.Objects;
import java.util.TreeMap;

public class CfgValueParser {
    private final CfgSchema subSchema;
    private final CfgData data;
    private final CfgSchema schema;
    private final ValueErrs errs;

    /**
     * @param subSchema 这是返会目标CfgValue对应的schema
     * @param data      全部的数据
     * @param schema    全部的数据对应的schema
     * @param errs      错误记录器
     */
    public CfgValueParser(CfgSchema subSchema, CfgData data, CfgSchema schema, ValueErrs errs) {
        subSchema.requireResolved();
        schema.requireResolved();
        Objects.requireNonNull(data);
        Objects.requireNonNull(errs);
        this.subSchema = subSchema;
        this.data = data;
        this.schema = schema;
        this.errs = errs;
    }

    public CfgValue parseCfgValue() {
        CfgValue value = new CfgValue(new TreeMap<>());
        for (TableSchema subTable : subSchema.tableMap().values()) {
            String name = subTable.name();
            CfgData.DTable dTable = data.tables().get(name);
            Objects.requireNonNull(dTable);
            TableSchema table = schema.findTable(name);
            Objects.requireNonNull(table);

            TableParser parser = new TableParser(subTable, dTable, table, errs);
            CfgValue.VTable vTable = parser.parseTable();
            value.vTableMap().put(name, vTable);
        }
        return value;
    }

}
