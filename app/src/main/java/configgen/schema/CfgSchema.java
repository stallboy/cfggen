package configgen.schema;

import java.util.*;

/**
 * 整个schema，注意不是完全只读，分为两个状态：
 * 1，初始化。
 * 2，resolved。resolved后不要再修改了
 */
public class CfgSchema {
    private final List<Nameable> items;
    private Map<String, Fieldable> structMap;
    private Map<String, TableSchema> tableMap;

    public static CfgSchema of() {
        return new CfgSchema(new ArrayList<>());
    }

    public CfgSchema(List<Nameable> items) {
        Objects.requireNonNull(items);
        this.items = items;
    }

    public void add(Nameable item) {
        items.add(item);
    }

    public List<Nameable> items() {
        return items;
    }

    public Fieldable findFieldable(String name) {
        return structMap.get(name);
    }

    public TableSchema findTable(String name) {
        return tableMap.get(name);
    }

    void resolve(Map<String, Fieldable> structMap, Map<String, TableSchema> tableMap) {
        this.structMap = structMap;
        this.tableMap = tableMap;
    }
}


