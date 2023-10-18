package configgen.value;

import configgen.schema.HasRef;
import configgen.util.Logger;
import configgen.data.CfgData;
import configgen.schema.CfgSchema;
import configgen.schema.Spans;
import configgen.schema.TableSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.value.TextI18n.*;
import static configgen.value.CfgValue.*;

public class CfgValueParser {
    private final CfgSchema subSchema;
    private final CfgData data;
    private final CfgSchema schema;
    private final TextI18n nullableI18n;
    private final boolean checkComma;
    private final ValueErrs errs;

    /**
     * @param subSchema    这是返会目标CfgValue对应的schema
     * @param data         全部的数据
     * @param schema       全部的数据对应的schema
     * @param nullableI18n 国际化信息，会替换text里的value
     * @param checkComma   检查excel中包含逗号的number格子。
     * @param errs         错误记录器
     */
    public CfgValueParser(CfgSchema subSchema, CfgData data, CfgSchema schema, TextI18n nullableI18n, boolean checkComma, ValueErrs errs) {
        subSchema.requireResolved();
        schema.requireResolved();
        Objects.requireNonNull(data);
        Objects.requireNonNull(errs);
        this.subSchema = subSchema;
        this.data = data;
        this.schema = schema;
        this.nullableI18n = nullableI18n;
        this.checkComma = checkComma;
        this.errs = errs;
    }

    public CfgValue parseCfgValue() {
        //预先计算下span，这样在多线程中parseTable过程中，就只读不会写了。
        Spans.preCalculateAllSpan(schema);

        //预先计算hasRef，方便生成时使用
        HasRef.preCalculateAllHasRef(schema);

        Logger.profile("schema span calculate");

        List<Callable<OneTableParserResult>> tasks = new ArrayList<>();
        CfgValue value = new CfgValue(subSchema, new TreeMap<>());
        for (TableSchema subTable : subSchema.tableMap().values()) {
            String name = subTable.name();
            CfgData.DTable dTable = data.tables().get(name);
            Objects.requireNonNull(dTable);
            TableSchema table = schema.findTable(name);
            Objects.requireNonNull(table);
            TableI18n tableI18n = nullableI18n != null ? nullableI18n.getTableI18n(name) : null;

            tasks.add(() -> {
                ValueErrs errs = ValueErrs.of();
                TableValueParser parser = new TableValueParser(subTable, dTable, table, tableI18n, errs);
                VTable vTable = parser.parseTable();
                if (checkComma) {
                    CommaNumberCellChecker checker = new CommaNumberCellChecker(vTable, dTable, errs);
                    checker.check();
                }
                return new OneTableParserResult(vTable, errs);
            });
        }

        try {
            ExecutorService executor = Executors.newWorkStealingPool();
            List<Future<OneTableParserResult>> futures = executor.invokeAll(tasks);
            for (Future<OneTableParserResult> future : futures) {
                OneTableParserResult result = future.get();
                VTable vTable = result.vTable;
                value.vTableMap().put(vTable.schema().name(), vTable);
                errs.merge(result.errs);
            }
            executor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Logger.profile("value parse");

        new RefValidator(value, errs).validate();
        Logger.profile("value ref validate");

        return value;
    }

    record OneTableParserResult(VTable vTable,
                                ValueErrs errs) {
    }

}
