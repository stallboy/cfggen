package configgen.gencs;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public void writeToStream(DataOutputStream stream) throws IOException {
        stream.writeInt(strings.size());
        for (String str : strings) {
            byte[] b = str.getBytes(StandardCharsets.UTF_8);
            stream.writeInt(b.length);
            stream.write(b);
        }
    }
}
