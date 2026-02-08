package configgen.genbytes;

import configgen.genjava.ConfigOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringPool {
    private final Map<String, Integer> stringToIndex = new HashMap<>(512);
    private final List<String> strings = new ArrayList<>(512);

    /**
     * @return 字符串在字符串池中的索引，如果字符串池中没有这个字符串，会添加到字符串池中并返回新的索引
     */
    public int addString(String str) {
        return stringToIndex.computeIfAbsent(str, s -> {
            int idx = strings.size();
            strings.add(s);
            return idx;
        });
    }

    public void serialize(ConfigOutput out) {
        out.writeInt(strings.size());
        for (String str : strings) {
            out.writeString(str);
        }
    }
}
