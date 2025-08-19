package configgen.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class TextFinders {

    public static LangSwitchable loadLangSwitch(String langSwitchDir, String defaultLang, boolean isCrLfAsLf) {
        Path path = Path.of(langSwitchDir);
        if (isLangSwitchById(path)) {
            return TextFinderById.loadLangSwitch(path, defaultLang);
        } else {
            return TextFinderByValue.loadLangSwitch(path, defaultLang, isCrLfAsLf);
        }
    }

    public static LangTextFinder loadOneLang(String i18nFilename, boolean isCrLfAsLf) {
        Path path = Path.of(i18nFilename);
        if (isLangTextFinderById(path)) {
            return TextFinderById.loadOneLang(path);
        } else {
            return TextFinderByValue.loadOneLang(path, isCrLfAsLf);
        }
    }

    private static boolean isLangTextFinderById(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * 只要有一个文件夹就是byId
     */
    private static boolean isLangSwitchById(Path path) {
        try (Stream<Path> plist = Files.list(path)) {
            return plist.anyMatch(Files::isDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
