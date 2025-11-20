package configgen.schema;

import java.util.*;

/**
 * 查询table之间的引用关系
 */
public class TableSchemaRefGraph {

    /**
     * table依赖中的一个节点
     * @param refInTables  引用到我的所有的table
     * @param refOutTables 我引用到的所有table
     */
    public record Refs(Map<String, TableSchema> refInTables,
                       Map<String, TableSchema> refOutTables) {

        public Set<String> refIn() {
            return refInTables.keySet();
        }

        public Set<String> refOut() {
            return refOutTables.keySet();
        }
    }

    private final CfgSchema schema;
    private final Map<String, Refs> refsMap = new HashMap<>();

    public TableSchemaRefGraph(CfgSchema schema) {
        this.schema = schema;
        schema.requireResolved();
        buildGraph();
    }

    private void buildGraph() {
        for (TableSchema table : schema.tableMap().values()) {
            refsMap.put(table.name(), new Refs(new HashMap<>(), findAllRefOuts(table)));
        }

        for (Map.Entry<String, Refs> e : refsMap.entrySet()) {
            TableSchema sourceTable = schema.findTable(e.getKey());
            Refs refs = e.getValue();
            for (TableSchema refToTable : refs.refOutTables.values()) {
                Refs refsTo = refsMap.get(refToTable.name());
                refsTo.refInTables.put(sourceTable.name(), sourceTable);
            }
        }
    }

    public Map<String, Refs> refsMap() {
        return refsMap;
    }

    public static Map<String, TableSchema> findAllRefOuts(TableSchema tableSchema) {
        Map<String, TableSchema> refOut = new HashMap<>();
        Map<String, Nameable> allIncludedStructs = IncludedStructs.findAllIncludedStructs(tableSchema);
        for (Nameable item : allIncludedStructs.values()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    TableSchema ref = interfaceSchema.nullableEnumRefTable();
                    if (ref != null) {
                        refOut.put(ref.name(), ref);
                    }
                }
                case Structural structural -> {
                    for (ForeignKeySchema fk : structural.foreignKeys()) {
                        TableSchema t = fk.refTableSchema();
                        refOut.put(t.name(), t);
                    }
                }
            }
        }
        return refOut;
    }
}
