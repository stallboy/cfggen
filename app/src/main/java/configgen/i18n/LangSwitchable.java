package configgen.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;


/**
 * 多语言
 * @param langMap 在实现多种语言可切换时需要所有语言的信息
 * @param defaultLang 默认语言
 */
public record LangSwitchable(Map<String, LangTextFinder> langMap,
                             String defaultLang) {

    public LangSwitchable {
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

    public static LangSwitchable read(String langSwitchDir, String defaultLang) {
        Path path = Path.of(langSwitchDir);
        Map<String, LangTextFinder> langMap = isById(path) ?
                TextByIdFinder.loadMultiLang(path) :
                TextByValueFinder.loadMultiLang(path);
        return new LangSwitchable(langMap, defaultLang);
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
