package configgen.i18n;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 一种语言,包含多个table的翻译信息
 */
public class LangTextFinder extends TreeMap<String, LangTextFinder.TextFinder> {


    public TextFinder getTextFinder(String table) {
        return get(table);
    }

    public interface TextVisitor {
        void visit(String original, String translated);
    }

    /**
     * 在一个table中
     */
    public interface TextFinder {
        /**
         * 根据主键，field chain，原始文本 --- 返回映射的 ---> 翻译文本
         *
         * @return 返回翻译文本，如果未找到返回null
         */
        String findText(String pk, List<String> fieldChain, String original);


        void foreachText(TextVisitor visitor);
    }


    public static LangTextFinder read(String i18nFilename) {
        Path path = Path.of(i18nFilename);
        if (isById(path)) {
            return TextByIdFinder.loadOneLang(path);
        } else {
            return TextByValueFinder.loadOneLang(path);
        }
    }

    private static boolean isById(Path path) {
        return Files.isDirectory(path);
    }

}

