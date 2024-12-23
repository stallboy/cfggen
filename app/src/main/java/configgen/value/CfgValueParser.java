package configgen.value;

import configgen.ctx.Context;
import configgen.ctx.TextFinder;
import configgen.schema.cfg.CfgWriter;
import configgen.util.Logger;
import configgen.data.CfgData;
import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.value.CfgValue.*;

public class CfgValueParser {
    private final CfgSchema subSchema;
    private final Context context;
    private final CfgValueErrs errs;

    /**
     * @param subSchema 这是返会目标CfgValue对应的schema
     * @param context   全局信息
     * @param errs      错误记录器
     */
    public CfgValueParser(CfgSchema subSchema, Context context, CfgValueErrs errs) {
        subSchema.requireResolved();
        context.cfgSchema().requireResolved();
        Objects.requireNonNull(errs);
        this.subSchema = subSchema;
        this.context = context;
        this.errs = errs;
    }

    public CfgValue parseCfgValue() {
        if (Logger.verboseLevel() > 1) {
            Logger.log(CfgWriter.stringify(context.cfgSchema(), false, true));
        }

        List<Callable<OneTableParserResult>> tasks = new ArrayList<>();
        CfgValue cfgValue = CfgValue.of(subSchema);
        for (TableSchema subTable : subSchema.tableMap().values()) {
            String name = subTable.name();
            TableSchema table = context.cfgSchema().findTable(name);
            Objects.requireNonNull(table);

            TextFinder tableTextFinder = context.nullableI18n() != null ? context.nullableI18n().getTableTextFinder(name) : null;
            CfgData.DTable dTable = context.cfgData().tables().get(name);

            if (dTable != null) {
                tasks.add(() -> {
                    long start = System.currentTimeMillis();
                    CfgValueErrs errs = CfgValueErrs.of();
                    VTableParser parser = new VTableParser(subTable, dTable, table, tableTextFinder, errs);
                    VTable vTable = parser.parseTable();
                    if (Logger.isProfileEnabled()) {
                        long e = System.currentTimeMillis() - start;
                        if (e > 10) {
                            System.out.printf("%40s: %d%n", name, e);
                        }
                    }
                    return new OneTableParserResult(vTable, errs);
                });

            } else {
                tasks.add(() -> {
                    long start = System.currentTimeMillis();
                    CfgValueErrs errs = CfgValueErrs.of();
                    VTableJsonParser parser = new VTableJsonParser(subTable, subSchema.isPartial(),
                            context.getSourceStructure(), table, tableTextFinder, errs, cfgValue.valueStat());
                    VTable vTable = parser.parseTable();
                    if (Logger.isProfileEnabled()) {
                        System.out.printf("%40s: %d%n", name, System.currentTimeMillis() - start);
                    }
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
                                CfgValueErrs errs) {
    }

}
