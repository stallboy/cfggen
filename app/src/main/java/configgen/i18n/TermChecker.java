package configgen.i18n;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static configgen.i18n.TextByIdFinder.*;


/**
 * 检查翻译文件中，包含原始文本包含原始术语，但翻译文本中却不包含翻译术语的情况，输出
 */
public class TermChecker extends Tool {

    /**
     * 单个语言翻译中间文件或目录
     */
    private final Path i8nFilepath;
    /**
     * 术语表excel文件，第一列时原始术语，第二列是翻译术语
     */
    private final Path termFilepath;

    public TermChecker(Parameter parameter) {
        super(parameter);
        i8nFilepath = Path.of(parameter.get("i18n", "language/en"));
        termFilepath = Path.of(parameter.get("term", "term_en.xlsx"));
    }

    @Override
    public void call() {
        List<OneText> terms = loadTerm(termFilepath);
        if (terms == null || terms.isEmpty()) {
            return;
        }
        LangTextFinder langFinder = LangTextFinder.read(i8nFilepath);
        List<Callable<OneTableResult>> tasks = makeTasks(langFinder, terms);
        Map<String, OneTableResult> orderedResult = new TreeMap<>();
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            List<Future<OneTableResult>> futures = executor.invokeAll(tasks);
            for (Future<OneTableResult> future : futures) {
                OneTableResult result = future.get();
                if (!result.noMatches.isEmpty()) {
                    orderedResult.put(result.table, result);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (OneTableResult result : orderedResult.values()) {
            System.out.println(result.table + ":");
            for (OneNoMatch noMatch : result.noMatches) {
                String noMatchTermsStr = noMatch.noMatchTerms.stream()
                        .map(t -> "%s -> %s".formatted(t.original(), t.translated()))
                        .collect(Collectors.joining(", "));
                System.out.printf("\t%s -> %s (%s)%n", noMatch.original, noMatch.translated, noMatchTermsStr);
            }
            System.out.println();
        }
    }


    record OneNoMatch(String original,
                      String translated,
                      List<OneText> noMatchTerms) {
    }

    record OneTableResult(String table,
                          List<OneNoMatch> noMatches) {
    }


    private static List<Callable<OneTableResult>> makeTasks(LangTextFinder translationFinder, List<OneText> terms) {
        List<Callable<OneTableResult>> tasks = new ArrayList<>();
        for (Map.Entry<String, LangTextFinder.TextFinder> e : translationFinder.entrySet()) {
            String table = e.getKey();
            LangTextFinder.TextFinder finder = e.getValue();

            tasks.add(() -> {
                OneTableResult result = new OneTableResult(table, new ArrayList<>());
                finder.foreachText((original, translated) -> {
                    if (!original.isEmpty() && !translated.isEmpty()) {
                        List<OneText> noMatchTerms = new ArrayList<>();
                        for (OneText term : terms) {
                            if (original.contains(term.original()) && !translated.contains(term.translated())) {
                                noMatchTerms.add(term);
                            }
                        }

                        if (!noMatchTerms.isEmpty()) {
                            result.noMatches.add(new OneNoMatch(original, translated, noMatchTerms));
                        }
                    }
                });
                return result;
            });
        }
        return tasks;
    }

    static List<OneText> loadTerm(Path termFilepath) {
        try (ReadableWorkbook wb = new ReadableWorkbook(termFilepath.toFile(), new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                List<Row> rows = sheet.read();
                List<OneText> result = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    String c0 = row.getCellAsString(0).orElse("");
                    String c1 = row.getCellAsString(1).orElse("");
                    String normalized = Utils.normalize(c0);
                    if (!c0.isEmpty() && !c1.isEmpty()) {
                        result.add(new OneText(normalized, c1));
                    }
                }
                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException("read term file=%s error".formatted(termFilepath), e);
        }
        return null;
    }
}
