package configgen.schema;

import java.util.Map;
import java.util.Objects;

public record CfgSchema(Map<String, Fieldable> structs,
                        Map<String, TableSchema> tables) {
    public CfgSchema {
        Objects.requireNonNull(structs);
        Objects.requireNonNull(tables);
    }
}


