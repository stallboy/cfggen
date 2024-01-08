package configgen.value;

import configgen.gen.Context;
import configgen.schema.HasRef;
import configgen.util.Logger;
import configgen.data.CfgData;
import configgen.schema.CfgSchema;
import configgen.schema.Spans;
import configgen.schema.TableSchema;

import java.nio.file.Path;
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
    private final Context context;
    private final ValueErrs errs;

    /**
     * @param subSchema 这是返会目标CfgValue对应的schema
     * @param context   全局信息
     * @param errs      错误记录器
     */
    public CfgValueParser(CfgSchema subSchema, Context context, ValueErrs errs) {
        subSchema.requireResolved();
        context.cfgSchema().requireResolved();
        Objects.requireNonNull(errs);
        this.subSchema = subSchema;
        this.context = context;
        this.errs = errs;
    }

    public CfgValue parseCfgValue() {
        //预先计算hasRef，方便生成时使用
        HasRef.preCalculateAllHasRef(context.cfgSchema());

        Logger.profile("schema span calculate");

        List<Callable<OneTableParserResult>> tasks = new ArrayList<>();
        CfgValue cfgValue = new CfgValue(subSchema, new TreeMap<>());
        for (TableSchema subTable : subSchema.tableMap().values()) {
            String name = subTable.name();
            TableSchema table = context.cfgSchema().findTable(name);
            Objects.requireNonNull(table);

            TableI18n tableI18n = context.nullableI18n() != null ? context.nullableI18n().getTableI18n(name) : null;
            CfgData.DTable dTable = context.cfgData().tables().get(name);

            if (dTable != null) {
                tasks.add(() -> {
                    ValueErrs errs = ValueErrs.of();
                    VTableParser parser = new VTableParser(subTable, dTable, table, tableI18n, errs);
                    VTable vTable = parser.parseTable();
                    return new OneTableParserResult(vTable, errs);
                });

            } else {
                tasks.add(() -> {
                    ValueErrs errs = ValueErrs.of();
                    VTableJsonParser parser = new VTableJsonParser(subTable, context.dataDir(), table, tableI18n, errs);
                    VTable vTable = parser.parseTable();
                    return new OneTableParserResult(vTable, errs);
                });
            }
        }

        try {
            ExecutorService executor = Executors.newWorkStealingPool();
            List<Future<OneTableParserResult>> futures = executor.invokeAll(tasks);
            for (Future<OneTableParserResult> future : futures) {
                OneTableParserResult result = future.get();
                VTable vTable = result.vTable;
                cfgValue.vTableMap().put(vTable.schema().name(), vTable);
                errs.merge(result.errs);
            }
            executor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Logger.profile("value parse");

        new RefValidator(cfgValue, errs).validate();
        Logger.profile("value ref validate");

        return cfgValue;
    }

    record OneTableParserResult(VTable vTable,
                                ValueErrs errs) {
    }

}
