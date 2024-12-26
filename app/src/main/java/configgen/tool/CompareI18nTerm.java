package configgen.tool;

import configgen.ctx.LangTextFinder;
import configgen.ctx.TextFinder;
import configgen.ctx.TextFinderByPkAndFieldChain;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.ctx.TextFinderByPkAndFieldChain.*;

public class CompareI18nTerm {
    record OneNoMatch(String original,
                      String translated,
                      OneText term) {

    }

    record OneTableResult(String table,
                          List<OneNoMatch> noMatches) {
    }

    public static void compare(String langDir, String termFile) {
        List<OneText> terms = loadTerm(termFile);
        if (terms == null || terms.isEmpty()) {
            return;
        }
        LangTextFinder langTextFinder = loadOneLang(Path.of(langDir));
        List<Callable<OneTableResult>> tasks = new ArrayList<>();
        for (Map.Entry<String, TextFinder> e : langTextFinder.getMap().entrySet()) {
            String table = e.getKey();
            TextFinderByPkAndFieldChain finder = (TextFinderByPkAndFieldChain) e.getValue();

            tasks.add(() -> {
                OneTableResult result = new OneTableResult(table, new ArrayList<>());
                Map<String, OneText[]> pkToTexts = finder.getPkToTexts();
                for (OneText[] line : pkToTexts.values()) {
                    for (OneText t : line) {
                        if (t != null && !t.original().isEmpty() && !t.translated().isEmpty()) {
                            for (OneText term : terms) {
                                if (t.original().contains(term.original()) && !t.translated().contains(term.translated())) {
                                    result.noMatches.add(new OneNoMatch(t.original(), t.translated(), term));
                                    break;
                                }
                            }
                        }
                    }
                }
                return result;
            });
        }

        Map<String, OneTableResult> orderedResult = new TreeMap<>();
        try {
            ExecutorService executor = Executors.newWorkStealingPool();
            List<Future<OneTableResult>> futures = executor.invokeAll(tasks);
            for (Future<OneTableResult> future : futures) {
                OneTableResult result = future.get();
                if (!result.noMatches.isEmpty()) {
                    orderedResult.put(result.table, result);
                }
            }
            executor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (OneTableResult result : orderedResult.values()) {
            System.out.println(result.table + ":");
            for (OneNoMatch noMatch : result.noMatches) {
                System.out.printf("\t%s -> %s (%s -> %s)%n", noMatch.original, noMatch.translated, noMatch.term.original(), noMatch.term.translated());
            }
            System.out.println();
        }
    }

    static List<OneText> loadTerm(String termFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(new File(termFile), new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                List<Row> rows = sheet.read();
                List<OneText> result = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    Optional<String> c0 = row.getCellAsString(0);
                    Optional<String> c1 = row.getCellAsString(1);
                    if (c0.isPresent() && c1.isPresent()) {
                        result.add(new OneText(c0.get(), c1.get()));
                    }
                }
                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
