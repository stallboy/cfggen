package configgen.ctx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;


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

    public static LangSwitch loadFromDirectory(Path path, String defaultLang, boolean crlfaslf) {
        Map<String, TextI18n> lang2i18n = new TreeMap<>();
        try (Stream<Path> plist = Files.list(path)) {
            plist.forEach(langFilePath -> {
                String langName = langFilePath.getFileName().toString();
                int i = langName.lastIndexOf(".");
                if (i >= 0) {
                    langName = langName.substring(0, i);
                }
                lang2i18n.put(langName, TextI18n.loadFromCsvFile(langFilePath, crlfaslf));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new LangSwitch(lang2i18n, defaultLang);
    }
}
