package configgen.geni18n;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.geni18n.TodoFile.Line;
import configgen.util.Logger;

import java.nio.file.Path;
import java.util.*;


/**
 * 列出专用术语
 * 检查专用术语
 */
public class TodoTermListerAndChecker extends Tool {

    private final String todoFilepath;
    private final String termFilepath;
    private final boolean check;
    private final boolean checkAll;

    public TodoTermListerAndChecker(Parameter parameter) {
        super(parameter);
        todoFilepath = parameter.get("todo", null);
        termFilepath = parameter.get("term", null);

        check = parameter.has("check");
        checkAll = parameter.has("checkall");
    }

    @Override
    public void call() {
        if (todoFilepath == null) {
            throw new IllegalArgumentException("todo file not set");
        }

        if (termFilepath == null) {
            throw new IllegalArgumentException("term file not set");
        }

        TodoFile todoFile = TodoFile.read(Path.of(todoFilepath));
        TermCfg termCfg = TermCfg.load(Path.of(termFilepath));


        var termsAndOthers = termCfg.extractTermsAndOthers(todoFile.done());
        Map<String, String> terms = termsAndOthers.terms();


        if (checkAll) {
            check(todoFile.todo(), terms);
            Logger.log("----------");
            check(termsAndOthers.others(), terms);

        } else if (check) {
            check(todoFile.todo(), terms);

        } else { // list term
            for (var e : terms.entrySet()) {
                Logger.log("%s,%s", e.getKey(), e.getValue());
            }
        }
    }

    private static void check(List<Line> lines, Map<String, String> terms) {
        for (Line line : lines) {
            checkOne(line, terms);
        }
    }


    private static void checkOne(Line line, Map<String, String> terms) {
        String original = line.original();
        String translated = line.translated();
        if (!original.isEmpty() && !translated.isEmpty()) {
            Map<String, String> noMatchTerms = new LinkedHashMap<>();
            for (var t : terms.entrySet()) {
                if (original.contains(t.getKey()) && !translated.contains(t.getValue())) {
                    noMatchTerms.put(t.getKey(), t.getValue());
                }
            }
            if (!noMatchTerms.isEmpty()) {
                Logger.log("%s %s -> %s", line.table(), original, translated);
                for (var e : noMatchTerms.entrySet()) {
                    Logger.log("\t%s -> %s", e.getKey(), e.getValue());
                }
            }
        }
    }


}
