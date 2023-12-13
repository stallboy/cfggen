package configgen.schema;

import java.util.*;

/**
 * 整个schema，注意不是完全只读，分为两个状态：
 * 1，初始化。
 * 2，resolved。resolved后不要再修改了
 */
public class CfgSchema {
    private final List<Nameable> items;

    private Map<String, Nameable> itemMap;
    private Map<String, Fieldable> fieldableMap;
    private Map<String, TableSchema> tableMap;
    private boolean isResolved = false;
    private boolean isForeignKeyValueCached = false;

    public static CfgSchema of() {
        return new CfgSchema(new ArrayList<>());
    }

    public CfgSchema(List<Nameable> items) {
        Objects.requireNonNull(items);
        this.items = items;
    }

    public SchemaErrs resolve() {
        SchemaErrs errs = SchemaErrs.of();
        new CfgSchemaResolver(this, errs).resolve();
        return errs;
    }

    void setResolved() {
        isResolved = true;
    }

    public void setForeignKeyValueCached() {
        isForeignKeyValueCached = true;
    }

    public void requireResolved() {
        if (!isResolved) {
            throw new IllegalStateException("cfgSchema not resolved");
        }
    }

    public void requireForeignKeyValueCached() {
        if (!isForeignKeyValueCached) {
            throw new IllegalStateException("cfgSchema not foreignKeyValueCached");
        }
    }

    public void add(Nameable item) {
        items.add(item);
    }

    public List<Nameable> items() {
        return items;
    }

    public Fieldable findFieldable(String name) {
        return fieldableMap.get(name);
    }

    public TableSchema findTable(String name) {
        return tableMap.get(name);
    }

    public Nameable findItem(String name) {
        return itemMap.get(name);
    }

    void setMap(Map<String, Nameable> itemMap,
                Map<String, Fieldable> fieldableMap,
                Map<String, TableSchema> tableMap) {
        this.itemMap = itemMap;
        this.fieldableMap = fieldableMap;
        this.tableMap = tableMap;
    }

    public Map<String, Fieldable> fieldableMap() {
        return fieldableMap;
    }

    public Map<String, TableSchema> tableMap() {
        return tableMap;
    }

    public Iterable<Fieldable> sortedFieldables() {
        Map<String, Fieldable> sorted = new TreeMap<>(fieldableMap);
        return sorted.values();
    }

    public Iterable<TableSchema> sortedTables() {
        Map<String, TableSchema> sorted = new TreeMap<>(tableMap);
        return sorted.values();
    }

    public void printDiff(CfgSchema cfg2) {
        int i = 0;
        for (Nameable item1 : items) {
            Nameable item2 = cfg2.items().get(i);
            if (!item1.equals(item2)) {
                System.out.println("=========not eq=========");
                System.out.println(item1);
                System.out.println(item2);
            }
            i++;
        }
    }

    @Override
    public String toString() {
        return "CfgSchema{" +
                "items=" + items +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CfgSchema cfgSchema = (CfgSchema) o;
        return Objects.equals(items, cfgSchema.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}


