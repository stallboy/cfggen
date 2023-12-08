package configgen.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;

public final class StructSchema implements Fieldable, Structural, Nameable {
    private final String name;
    private final FieldFormat fmt;
    private final Metadata meta;
    private final List<FieldSchema> fields;
    private final List<ForeignKeySchema> foreignKeys;

    private InterfaceSchema nullableInterface;

    public StructSchema(String name, FieldFormat fmt, Metadata meta, List<FieldSchema> fields, List<ForeignKeySchema> foreignKeys) {
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
        this.name = name;
        this.fmt = fmt;
        this.meta = meta;
        this.fields = fields;
        this.foreignKeys = foreignKeys;
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

    @Override
    public String fullName() {
        if (nullableInterface != null) {
            return String.format("%s.%s", nullableInterface.name(), name);
        }
        return name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public FieldFormat fmt() {
        return fmt;
    }

    @Override
    public Metadata meta() {
        return meta;
    }

    @Override
    public List<FieldSchema> fields() {
        return fields;
    }

    @Override
    public List<ForeignKeySchema> foreignKeys() {
        return foreignKeys;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StructSchema) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.fmt, that.fmt) &&
                Objects.equals(this.meta, that.meta) &&
                Objects.equals(this.fields, that.fields) &&
                Objects.equals(this.foreignKeys, that.foreignKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fmt, meta, fields, foreignKeys);
    }

    @Override
    public String toString() {
        return "StructSchema[" +
                "name=" + name + ", " +
                "fmt=" + fmt + ", " +
                "meta=" + meta + ", " +
                "fields=" + fields + ", " +
                "foreignKeys=" + foreignKeys + ']';
    }


    public InterfaceSchema nullableInterface() {
        return nullableInterface;
    }

    void setNullableInterface(InterfaceSchema nullableInterface) {
        this.nullableInterface = nullableInterface;
    }
}
