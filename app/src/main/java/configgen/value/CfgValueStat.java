package configgen.value;

import configgen.schema.Stat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * json文件的最后修改时间信息，用于cfgeditor
 */
public class CfgValueStat implements Stat {
    private final Map<String, Map<String, Long>> lastModifiedMap = new LinkedHashMap<>();
    private int translateMissCount = 0;

    public void incTranslateMissCount(){
        translateMissCount++;
    }

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

    private void removeLastModified(String table, String id) {
        Map<String, Long> m = lastModifiedMap.get(table);
        if (m != null) {
            m.remove(id);
        }
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

    private CfgValueStat copy() {
        CfgValueStat newStat = new CfgValueStat();
        for (Map.Entry<String, Map<String, Long>> e : lastModifiedMap.entrySet()) {
            newStat.lastModifiedMap.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
        }
        return newStat;
    }
}
