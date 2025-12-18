package configgen.tool;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.value.CfgValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GenValueSearcher extends GeneratorWithTag {
    private final String searchTo;
    private final List<String> query;

    public GenValueSearcher(Parameter parameter) {
        super(parameter);
        searchTo = parameter.get("to", null);
        String q = parameter.get("q", null);

        if (q != null) {
            String[] split = q.trim().split("\\s+");
            if (split.length > 0) {
                query = Arrays.stream(split).toList();
            } else {
                query = null;
            }
        } else {
            query = null;
        }

        parameter.extra(ValueSearcher.usage());
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue value = ctx.makeValue(tag);
        ValueSearcher searcher = new ValueSearcher(value, searchTo);

        if (query == null) {
            searcher.loop();
        } else {
            searcher.search(query.getFirst(), query.subList(1, query.size()));

        }
        searcher.close();
    }
}
