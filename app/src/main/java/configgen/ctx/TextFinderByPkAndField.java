package configgen.ctx;

/**
 * 每个表中的text字段，pk+fieldChain作为id---映射到--->翻译文本。
 * 这是最完备的机制，可以相同的原始本文，不同的翻译文本。
 */
public class TextFinderByPkAndField implements TextFinder{
    @Override
    public String findText(String pk, String fieldChain, String original) {
        return "";
    }
}
