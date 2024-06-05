package configgen.ctx;

import configgen.util.CSVUtil;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static configgen.ctx.TextI18n.*;

public record LangSwitch(Map<String, TextI18n> lang2i18n,
                         String defaultLang) {

    public List<String> languages() {
        List<String> res = new ArrayList<>(lang2i18n.size() + 1);
        res.add(defaultLang);
        res.addAll(lang2i18n.keySet());
        return res;
    }

    public int languageCount() {
        return lang2i18n.size() + 1;
    }

    public static LangSwitch loadLangSwitch(Path path, String defaultLang, boolean crlfaslf) {
        Map<String, TextI18n> lang2i18n = new TreeMap<>();
        try (Stream<Path> plist = Files.list(path)) {
            plist.forEach(langFilePath -> {
                String langName = langFilePath.getFileName().toString();
                int i = langName.lastIndexOf(".");
                if (i >= 0) {
                    langName = langName.substring(0, i);
                }
                lang2i18n.put(langName, loadTextI18n(langFilePath, crlfaslf));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new LangSwitch(lang2i18n, defaultLang);
    }


    public static TextI18n loadTextI18n(Path path, boolean crlfaslf) {
        List<CsvRow> rows = CSVUtil.read(path, "UTF-8");

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("国际化i18n文件为空");
        }
        CsvRow row0 = rows.getFirst();
        if (row0.getFieldCount() != 3) {
            throw new IllegalArgumentException("国际化i18n文件列数不为3");
        }

        Map<String, TableI18n> tableI18nMap = new TreeMap<>();
        for (CsvRow row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            if (row.getFieldCount() != 3) {
                System.out.println(row + " 不是3列，被忽略");
            } else {
                String table = row.getField(0);
                String original = row.getField(1);
                String lang = row.getField(2);
                original = TextI18n.normalize(original, crlfaslf);

                TableI18n tableI18n = tableI18nMap.computeIfAbsent(table, t ->
                        new TableI18n(new LinkedHashMap<>(), crlfaslf));
                tableI18n.original2text().put(original, lang);
            }
        }
        return new TextI18n(tableI18nMap);
    }

}
