package configgen.ctx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * 每个表中的text字段，pk+fieldChain作为id---映射到--->翻译文本。
 * 这是最完备的机制，可以相同的原始本文，不同的翻译文本。
 */
public class TextFinderByPkAndFieldChain implements TextFinder {
    @Override
    public String findText(String pk, List<String> fieldChain, String original) {
        return "";
    }


    public static String fieldChainStr(List<String> fieldChain) {
        return fieldChain.size() == 1 ? fieldChain.getFirst() : String.join("-", fieldChain);
    }


    public static LangSwitch loadLangSwitch(Path path, String defaultLang) {
        Map<String, LangTextFinder> lang2i18n = new TreeMap<>();
        try (Stream<Path> plist = Files.list(path)) {
            plist.forEach(langFilePath -> {
                if (Files.isDirectory(langFilePath)) {
                    String langName = langFilePath.getFileName().toString();
                    lang2i18n.put(langName, loadOneLang(langFilePath));
                }

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new LangSwitch(lang2i18n, defaultLang);
    }

    public static LangTextFinder loadOneLang(Path path) {
        return null;

    }
}
