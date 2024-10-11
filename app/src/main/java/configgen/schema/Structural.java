package configgen.schema;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public sealed interface Structural extends Nameable permits StructSchema, TableSchema {
    List<FieldSchema> fields();

    List<ForeignKeySchema> foreignKeys();

    default FieldSchema findField(String name) {
        for (FieldSchema f : fields()) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return null;
    }

    default ForeignKeySchema findForeignKey(String name) {
        for (ForeignKeySchema f : foreignKeys()) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return null;
    }

    default Set<String> fieldNameSet() {
        return fields().stream().map(FieldSchema::name).collect(Collectors.toSet());
    }

}
