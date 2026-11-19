package configgen.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;


/**
 * 可切换语言模式使用的一组译文。
 * @param langMap 按语言代码索引的译文目录
 * @param defaultLang 原文对应的语言
 */
public record TranslationBundle(Map<String, TranslationCatalog> langMap,
                                String defaultLang) {

    public TranslationBundle {
        Objects.requireNonNull(langMap);
        Objects.requireNonNull(defaultLang);
    }

    public List<String> languages() {
        List<String> res = new ArrayList<>(langMap.size() + 1);
        res.add(defaultLang);
        res.addAll(langMap.keySet());
        return res;
    }

    public int languageCount() {
        return langMap.size() + 1;
    }

    public static TranslationBundle read(Path dir, String defaultLang) {
        Map<String, TranslationCatalog> langMap = isById(dir) ?
                TextByIdFinder.loadMultiLang(dir) :
                TextByValueFinder.loadMultiLang(dir);
        return new TranslationBundle(langMap, defaultLang);
    }

    /**
     * 只要有一个文件夹就是byId
     */
    private static boolean isById(Path path) {
        try (Stream<Path> plist = Files.list(path)) {
            return plist.anyMatch(Files::isDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
