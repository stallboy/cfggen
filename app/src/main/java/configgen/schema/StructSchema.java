package configgen.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;

public record StructSchema(String name,
                           FieldFormat fmt,
                           Metadata meta,
                           List<FieldSchema> fields,
                           List<ForeignKeySchema> foreignKeys) implements Fieldable, Structural, Nameable {
    public StructSchema {
        Objects.requireNonNull(name);
        Objects.requireNonNull(fmt);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(fields);
        Objects.requireNonNull(foreignKeys);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("struct name empty");
        }
        if (fmt != AUTO && fmt != PACK && !(fmt instanceof FieldFormat.Sep)) {
            throw new IllegalArgumentException("struct fmt must be auto/pack/sep");
        }
    }

    @Override
    public StructSchema copy() {
        List<FieldSchema> fieldsCopy = new ArrayList<>(fields.size());
        for (FieldSchema f : fields) {
            fieldsCopy.add(f.copy());
        }

        List<ForeignKeySchema> fksCopy = new ArrayList<>(foreignKeys.size());
        for (ForeignKeySchema fk : foreignKeys) {
            fksCopy.add(fk.copy());
        }

        return new StructSchema(name, fmt, meta.copy(), fieldsCopy, fksCopy);
    }


}
