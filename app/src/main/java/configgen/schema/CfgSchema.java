package configgen.schema;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record CfgSchema(Map<String, Fieldable> structs,
                        Map<String, TableSchema> tables) {
    public CfgSchema {
        Objects.requireNonNull(structs);
        Objects.requireNonNull(tables);
    }

    public Fieldable add(Fieldable s) {
        return structs.put(s.name(), s);
    }

    public TableSchema add(TableSchema t) {
        return tables.put(t.name(), t);
    }

    public static CfgSchema of() {
        return new CfgSchema(new TreeMap<>(), new TreeMap<>());
    }
}


