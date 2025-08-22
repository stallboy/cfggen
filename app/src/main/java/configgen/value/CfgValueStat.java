package configgen.value;

import configgen.schema.Stat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * json文件的最后修改时间信息，用于cfgeditor
 */
public class CfgValueStat implements Stat {
    private final Map<String, Map<String, Long>> lastModifiedMap = new LinkedHashMap<>();

    /**
     * @return table -> recordId -> lastModified
     */
    public synchronized Map<String, Map<String, Long>> getLastModifiedMap() {
        return lastModifiedMap;
    }

    public synchronized Map<String, Long> getIdLastModifiedMap(String table) {
        return lastModifiedMap.computeIfAbsent(table, k -> new LinkedHashMap<>());
    }

    public synchronized void addLastModified(String table, String id, long time) {
        lastModifiedMap.computeIfAbsent(table, k -> new LinkedHashMap<>()).put(id, time);
    }

    private synchronized void removeLastModified(String table, String id) {
        Map<String, Long> m = lastModifiedMap.get(table);
        if (m != null) {
            m.remove(id);
        }
    }

    private synchronized CfgValueStat copy() {
        CfgValueStat newStat = new CfgValueStat();
        for (Map.Entry<String, Map<String, Long>> e : lastModifiedMap.entrySet()) {
            newStat.lastModifiedMap.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
        }
        return newStat;
    }

    public CfgValueStat newAddLastModified(String table, String id, long time) {
        CfgValueStat newStat = copy();
        newStat.addLastModified(table, id, time);
        return newStat;
    }

    public CfgValueStat newRemoveLastModified(String table, String id) {
        CfgValueStat newStat = copy();
        newStat.removeLastModified(table, id);
        return newStat;
    }


}
