package configgen.i18n;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static configgen.i18n.TextFinderById.*;


public class CompareI18nTerm {
    record OneNoMatch(String original,
                      String translated,
                      List<OneText> noMatchTerms) {
    }

    record OneTableResult(String table,
                          List<OneNoMatch> noMatches) {
    }

    /**
     * 检查翻译文件中，包含原始文本包含原始术语，但翻译文本中却不包含翻译术语的情况，输出
     *
     * @param i8nFilename 单个语言翻译中间文件或目录
     * @param termFile    术语表excel文件，第一列时原始术语，第二列是翻译术语
     */
    public static void compare(String i8nFilename, String termFile) {
        List<OneText> terms = loadTerm(termFile);
        if (terms == null || terms.isEmpty()) {
            return;
        }
        LangTextFinder langTextFinder = TextFinders.loadOneLang(i8nFilename);
        List<Callable<OneTableResult>> tasks = makeTasks(langTextFinder, terms);
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

    private static List<Callable<OneTableResult>> makeTasks(LangTextFinder langTextFinder, List<OneText> terms) {
        List<Callable<OneTableResult>> tasks = new ArrayList<>();
        for (Map.Entry<String, TextFinder> e : langTextFinder.getMap().entrySet()) {
            String table = e.getKey();
            TextFinder finder = e.getValue();

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

    static List<OneText> loadTerm(String termFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(new File(termFile), new ReadingOptions(true, false))) {
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
            throw new RuntimeException("read term file=%s error".formatted(termFile), e);
        }
        return null;
    }
}
