package configgen.schema;

import java.util.*;

public class TableSchemaRefGraph {

    public record Refs(Set<String> refIn,
                       Set<String> refOut) {
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
            Set<String> refOut = getAllRefOuts(table);
            refsMap.put(table.name(), new Refs(new HashSet<>(), refOut));
        }

        for (Map.Entry<String, Refs> e : refsMap.entrySet()) {
            String sourceTable = e.getKey();
            Refs refs = e.getValue();
            for (String refToTable : refs.refOut) {
                Refs refsTo = refsMap.get(refToTable);
                refsTo.refIn.add(sourceTable);
            }
        }
    }

    public Map<String, Refs> refsMap() {
        return refsMap;
    }

    private Set<String> getAllRefOuts(Nameable notImplNameable) {
        Set<String> refOut = new HashSet<>();
        Set<String> deps = collectAllDepStructs(notImplNameable);
        for (String dep : deps) {
            Nameable item = schema.findItem(dep);
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    if (interfaceSchema.nullableEnumRefTable()!= null){
                        refOut.add(interfaceSchema.nullableEnumRefTable().name());
                    }

                    for (StructSchema impl : interfaceSchema.impls()) {
                        for (ForeignKeySchema fk : impl.foreignKeys()) {
                            refOut.add(fk.refTableNormalized());
                        }
                    }
                }
                case Structural structural -> {
                    for (ForeignKeySchema fk : structural.foreignKeys()) {
                        refOut.add(fk.refTableNormalized());
                    }
                }
            }
        }
        return refOut;
    }


    private Set<String> collectAllDepStructs(Nameable notImplNameable) {
        Set<String> result = new HashSet<>();
        List<Nameable> frontier = List.of(notImplNameable);

        while (!frontier.isEmpty()) {
            for (Nameable nameable : frontier) {
                result.add(nameable.name());
            }
            Set<String> newFrontierIds = new HashSet<>();
            for (Nameable nameable : frontier) {
                collectDirectDepStructs(nameable, newFrontierIds);
            }
            for (String s : result) {
                newFrontierIds.remove(s);
            }
            List<Nameable> newFrontier = new ArrayList<>(newFrontierIds.size());
            for (String id : newFrontierIds) {
                newFrontier.add(schema.findItem(id));
            }
            frontier = newFrontier;
        }
        return result;
    }


    private static void collectDirectDepStructs(Nameable notImplNameable, Set<String> result) {
        switch (notImplNameable) {
            case InterfaceSchema interfaceSchema -> {
                for (StructSchema impl : interfaceSchema.impls()) {
                    for (FieldSchema field : impl.fields()) {
                        collectDirectDepStructs(field.type(), result);
                    }
                }
            }
            case Structural structural -> {
                for (FieldSchema field : structural.fields()) {
                    collectDirectDepStructs(field.type(), result);
                }
            }
        }
    }

    private static void collectDirectDepStructs(FieldType fieldType, Set<String> result) {
        switch (fieldType) {
            case FieldType.StructRef structRef -> {
                Fieldable obj = structRef.obj();
                switch (obj) {
                    case InterfaceSchema interfaceObj -> {
                        result.add(interfaceObj.name());
                    }
                    case StructSchema structObj -> {
                        if (structObj.nullableInterface() == null) { // 不影响interface的deps的收集
                            result.add(structObj.name());
                        }
                    }
                }
            }
            case FieldType.FList fList -> {
                collectDirectDepStructs(fList.item(), result);
            }
            case FieldType.FMap fMap -> {
                collectDirectDepStructs(fMap.key(), result);
                collectDirectDepStructs(fMap.value(), result);
            }
            case FieldType.Primitive ignored -> {
            }
        }
    }
}
