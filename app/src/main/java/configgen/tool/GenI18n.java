package configgen.tool;

import configgen.gen.*;
import configgen.util.CSVUtil;
import configgen.value.CfgValue;
import configgen.value.ForeachPrimitiveValue;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static configgen.value.CfgValue.*;

public final class GenI18n extends Generator {
    private final File file;
    private List<List<String>> data;

    public GenI18n(Parameter parameter) {
        super(parameter);
        file = new File(parameter.get("file", "../i18n/i18n-config.csv"));
        parameter.end();
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);

        data = new ArrayList<>(64 * 1024);
        for (VTable vTable : cfgValue.sortedTables()) {
            ForeachPrimitiveValue.foreachVTable(this::visit, vTable);
        }

        CSVUtil.writeToFile(file, data);
    }

    private void visit(PrimitiveValue primitiveValue, String table, Value pk, List<String> fieldChain) {
        if (primitiveValue instanceof VText vText) {
            if (!vText.original().trim().isEmpty() || !vText.value().trim().isEmpty()) {
                data.add(List.of(table, vText.original(), vText.value()));
            }
        }
    }
}
