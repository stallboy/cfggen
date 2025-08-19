package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.*;
import configgen.schema.HasText;
import configgen.util.CSVUtil;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static configgen.value.CfgValue.*;

public final class GenI18nByValue extends Generator {
    private final File file;
    private List<List<String>> data;

    public GenI18nByValue(Parameter parameter) {
        super(parameter);
        file = new File(parameter.get("file", "../i18n/en.csv"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue();

        data = new ArrayList<>(64 * 1024);
        for (VTable vTable : cfgValue.sortedTables()) {
            if (HasText.hasText(vTable.schema())) {
                ForeachValue.searchVTable(this::visit, vTable);
            }
        }

        CSVUtil.writeToFile(file, data);
    }


    private void visit(PrimitiveValue primitiveValue, String table, Value pk, List<String> fieldChain) {
        if (primitiveValue instanceof VText vText) {
            String original = vText.original().trim();
            String translated = vText.translated();
            if (!original.isEmpty() || !translated.isEmpty()) {
                data.add(List.of(table, original, translated));
            }
        }
    }
}
