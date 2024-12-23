package configgen.ctx;

import java.util.List;

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
}
