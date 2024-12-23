package configgen.ctx;

/**
 * 在一个table中
 */
public interface TextFinder {
    /**
     * 根据主键，field chain，原始文本 --- 返回映射的 ---> 翻译文本
     */
    String findText(String pk, String fieldChain, String original);

    // TODO remove
    default String findText(String original) {
        return findText(null, null, original);
    }
}
