package configgen.i18n;

import java.util.List;

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


    interface TextVisitor {
        void visit(String original, String translated);
    }

    void foreachText(TextVisitor visitor);
}
