package configgen.value;

import configgen.schema.Stat;

import java.util.LinkedHashMap;
import java.util.Map;

public class ValueStat implements Stat {
    private final Map<String, Map<String, Long>> lastModifiedMap = new LinkedHashMap<>();

    /**
     * @return table -> recordId -> lastModified
     */
    public Map<String, Map<String, Long>> getLastModifiedMap() {
        return lastModifiedMap;
    }

    public Map<String, Long> getIdLastModifiedMap(String table) {
        return lastModifiedMap.computeIfAbsent(table, k -> new LinkedHashMap<>());
    }

    public void addLastModified(String table, String id, long time) {
        lastModifiedMap.computeIfAbsent(table, k -> new LinkedHashMap<>()).put(id, time);
    }

    public void removeLastModified(String table, String id) {
        Map<String, Long> m = lastModifiedMap.get(table);
        if (m != null) {
            m.remove(id);
        }
    }
}
