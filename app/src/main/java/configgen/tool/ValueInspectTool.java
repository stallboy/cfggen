package configgen.tool;

import org.jspecify.annotations.Nullable;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.value.CfgValue;

import java.io.IOException;
import java.util.List;

public class ValueInspectTool extends GeneratorWithTag {
    private final @Nullable String searchTo;
    private final @Nullable List<String> query;

    public ValueInspectTool(Parameter parameter) {
        super(parameter);
        searchTo = parameter.get("to", null);
        String q = parameter.get("q", null);

        query = q == null || q.isBlank() ? null : List.of(q.trim().split("\\s+"));

        parameter.extra(ValueInspector.help());
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue value = ctx.makeValue(tag);
        try (ValueInspector searcher = new ValueInspector(value, searchTo)) {
            if (query == null) {
                searcher.loop();
            } else {
                searcher.search(query.getFirst(), query.subList(1, query.size()));
            }
        }
    }
}
