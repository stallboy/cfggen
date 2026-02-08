package configgen.genbytes;

import configgen.genjava.ConfigOutput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 多语言文本池
 * 按语言分组，每个语言有独立的 TextPool
 */
public class LangTextPool {
    private final TextPool[] textPools;
    private int nextTextIndex;

    public LangTextPool(@NotNull List<String> langNames) {
        this.textPools = new TextPool[langNames.size()];
        for (int i = 0; i < langNames.size(); i++) {
            textPools[i] = new TextPool(langNames.get(i));
        }
    }

    public TextPool[] getTextPools() {
        return textPools;
    }

    /**
     * 添加多语言文本，返回文本索引
     */
    public int addText(String[] i18nStrings) {
        if (textPools.length != i18nStrings.length) {
            throw new IllegalArgumentException("Language count mismatch: expected " + textPools.length + ", got " + i18nStrings.length);
        }

        for (int i = 0; i < textPools.length; i++) {
            TextPool textPool = textPools[i];
            textPool.addText(i18nStrings[i]);
        }

        int thisIndex = nextTextIndex;
        nextTextIndex++;
        return thisIndex;
    }

    public void serialize(ConfigOutput out) {
        out.writeInt(textPools.length);
        for (TextPool textPool : textPools) {
            textPool.serialize(out);
        }
    }

    public void serializeFirst(ConfigOutput out) {
        out.writeInt(1);
        textPools[0].serialize(out);
    }
}
