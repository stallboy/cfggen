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

    /**
     * 更新字段的类型（用于 enum 类型转换为 STRING）
     */
    default void updateFieldType(String fieldName, FieldType newType) {
        List<FieldSchema> fieldList = fields();
        for (int i = 0; i < fieldList.size(); i++) {
            if (fieldList.get(i).name().equals(fieldName)) {
                FieldSchema old = fieldList.get(i);
                fieldList.set(i, new FieldSchema(old.name(), newType, old.fmt(), old.meta()));
                return;
            }
        }
    }

    /**
     * 添加外键
     */
    default void addForeignKey(ForeignKeySchema fk) {
        foreignKeys().add(fk);
    }

}
