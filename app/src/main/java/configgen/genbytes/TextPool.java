package configgen.genbytes;

import configgen.genjava.ConfigOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个语言的文本数组
 * 包含文本索引列表和字符串池
 */
public class TextPool {
    private final String langName;
    private final List<Integer> indices;
    private final StringPool pool;

    public TextPool(String langName) {
        this.langName = langName;
        this.indices = new ArrayList<>();
        this.pool = new StringPool();
    }

    public String getLangName() {
        return langName;
    }

    /**
     * 添加文本
     */
    public void addText(String text) {
        int idx = pool.addString(text);
        indices.add(idx);
    }

    public void serialize(ConfigOutput out) {
        // 写入语言名称
        out.writeString(langName);

        // 写入索引数组
        out.writeInt(indices.size());
        for (int index : indices) {
            out.writeInt(index);
        }

        // 写入 StringPool
        pool.serialize(out);
    }
}
