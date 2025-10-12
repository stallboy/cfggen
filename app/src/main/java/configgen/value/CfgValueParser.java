package configgen.value;

import configgen.ctx.Context;
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

public record CfgValueParser(CfgSchema subSchema,
                             Context context,
                             CfgValueErrs errs) {
    /**
     * @param subSchema 这是返会目标CfgValue对应的schema
     * @param context   全局信息
     * @param errs      错误记录器
     */
    public CfgValueParser {
        Objects.requireNonNull(subSchema);
        Objects.requireNonNull(context);
        Objects.requireNonNull(errs);
        subSchema.requireResolved();
        context.cfgSchema().requireResolved();
    }

    public CfgValue parseCfgValue() {
        if (Logger.verboseLevel() > 1) {
            Logger.log(CfgWriter.stringify(context.cfgSchema(), false, true));
        }

        List<Callable<OneTableParserResult>> tasks = new ArrayList<>();
        CfgValue cfgValue = of(subSchema);
        for (TableSchema subTable : subSchema.tableMap().values()) {
            String name = subTable.name();
            TableSchema table = context.cfgSchema().findTable(name);
            Objects.requireNonNull(table);

            CfgData.DTable dTable = context.cfgData().tables().get(name);

            if (dTable != null) {
                tasks.add(() -> {
                    long start = System.currentTimeMillis();
                    CfgValueErrs errs = CfgValueErrs.of();
                    VTableParser parser = new VTableParser(subTable, dTable, table, context.getContextCfg().headRow(), errs);
                    VTable vTable = parser.parseTable();
                    TextValue.setTranslatedForTable(vTable, context.nullableLangTextFinder());
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
                            context.getSourceStructure(), table, errs, cfgValue.valueStat());
                    VTable vTable = parser.parseTable();
                    TextValue.setTranslatedForTable(vTable, context.nullableLangTextFinder());
                    if (Logger.isProfileEnabled()) {
                        System.out.printf("%40s: %d%n", name, System.currentTimeMillis() - start);
                    }
                    return new OneTableParserResult(vTable, errs);
                });
            }
        }

        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            List<Future<OneTableParserResult>> futures = executor.invokeAll(tasks);
            for (Future<OneTableParserResult> future : futures) {
                OneTableParserResult result = future.get();
                VTable vTable = result.vTable;
                cfgValue.vTableMap().put(vTable.schema().name(), vTable);
                errs.merge(result.errs);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Logger.profile("value parse");
        if (Logger.verboseLevel() > 0) {
            cfgValue.valueStat().print();
        }

        new RefValidator(cfgValue, errs).validate();
        Logger.profile("value ref validate");

        return cfgValue;
    }

    record OneTableParserResult(VTable vTable,
                                CfgValueErrs errs) {
    }

}
