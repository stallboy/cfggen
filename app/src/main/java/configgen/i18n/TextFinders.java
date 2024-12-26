package configgen.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class TextFinders {

    public static LangSwitch loadLangSwitch(String langSwitchDir, String defaultLang, boolean isCrLfAsLf) {
        Path path = Path.of(langSwitchDir);
        if (isLangSwitchByPkAndFieldChain(path)) {
            return TextFinderByPkAndFieldChain.loadLangSwitch(path, defaultLang);
        } else {
            return TextFinderByOrig.loadLangSwitch(path, defaultLang, isCrLfAsLf);
        }
    }

    public static LangTextFinder loadOneLang(String i18nFilename, boolean isCrLfAsLf) {
        Path path = Path.of(i18nFilename);
        if (isLangTextFinderByByPkAndFieldChain(path)) {
            return TextFinderByPkAndFieldChain.loadOneLang(path);
        } else {
            return TextFinderByOrig.loadOneLang(path, isCrLfAsLf);
        }
    }

    private static boolean isLangTextFinderByByPkAndFieldChain(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * 只要有一个文件夹就是byPkAndFieldChain
     */
    private static boolean isLangSwitchByPkAndFieldChain(Path path) {
        try (Stream<Path> plist = Files.list(path)) {
            return plist.anyMatch(Files::isDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
