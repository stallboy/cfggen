package configgen.tool;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.LocaleUtil;
import configgen.value.CfgValue;
import configgen.value.EntryRecordCollector;
import configgen.value.UnreferencedRecordCollector;

import java.io.IOException;

public class ValueVerifyTool extends Generator {

    private final boolean printUnreferenced;
    private final boolean printEntry;

    public ValueVerifyTool(Parameter parameter) {
        super(parameter);

        parameter.title(LocaleUtil.getLocaleString("GenVerifier.Title",
                "Reference check; unreferenced records check (entry/enum/root counts as referenced); entry records check"));

        printUnreferenced = parameter.has("unreferenced");
        printEntry = parameter.has("entry");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue value = ctx.makeValue();

        if (printUnreferenced) {
            UnreferencedRecordCollector.Unreferenced unreferenced = UnreferencedRecordCollector.collectUnreferenced(value);
            unreferenced.print();
        }

        if (printEntry) {
            EntryRecordCollector.Entry entry = EntryRecordCollector.collectEntry(value);
            entry.print();
        }
    }
}
