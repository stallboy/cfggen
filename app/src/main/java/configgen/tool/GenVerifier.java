package configgen.tool;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.LocaleUtil;
import configgen.value.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static configgen.value.UnreferencedRecordCollector.*;

public class GenVerifier extends Generator {

    public GenVerifier(Parameter parameter) {
        super(parameter);
        parameter.title(LocaleUtil.getLocaleString("GenVerifier.Title",
            "Reference (data consistency) check; unreferenced record check (entry/enum counts as referenced)"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue value = ctx.makeValue();
        Unreferenced unreferenced = collectUnreferenced(value);
        unreferenced.print();
    }
}
