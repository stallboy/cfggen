package configgen.genbytes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StringPool {
    private final Map<String, Integer> stringToIndex = new HashMap<>(512);
    private final List<String> strings = new ArrayList<>(512);

    public int add(String str) {
        return stringToIndex.computeIfAbsent(str, s -> {
            int idx = strings.size();
            strings.add(s);
            return idx;
        });
    }

    public List<String> getStrings() {
        return strings;
    }

}
