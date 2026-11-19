package configgen.i18n;

import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 一种语言,包含多个table的翻译信息
 */
public class TranslationCatalog extends TreeMap<String, TranslationCatalog.TextFinder> {


    public @Nullable TextFinder getTextFinder(String table) {
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
        @Nullable String findText(String pk, List<String> fieldChain, String original);


        void foreachText(TextVisitor visitor);
    }


    public static TranslationCatalog read(Path path) {
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

