package configgen.genjava;

import java.util.ArrayList;
import java.util.List;

public final class SchemaBean implements Schema {

    public record Column(String name, Schema schema) {
        public boolean compatible(Column other) {
            return name.equals(other.name) && schema.compatible(other.schema);
        }
    }

    public final boolean isTable;
    public final List<Column> columns = new ArrayList<>();

    public SchemaBean(ConfigInput input) {
        isTable = input.readBool();
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            columns.add(new Column(input.readStr(), Schema.create(input)));
        }
    }

    public SchemaBean(boolean isTable) {
        this.isTable = isTable;
    }

    public void addColumn(String name, Schema schema) {
        columns.add(new Column(name, schema));
    }

    @Override
    public boolean compatible(Schema other) {
        if (!(other instanceof SchemaBean sb)) {
            return false;
        }
        if (isTable != sb.isTable) {
            return false;
        }
        if (columns.size() != sb.columns.size()) {
            return false;
        }

        for (int i = 0; i < columns.size(); i++) {
            Column t1 = columns.get(i);
            Column t2 = sb.columns.get(i);
            if (!t1.compatible(t2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void write(ConfigOutput output) {
        output.writeInt(BEAN);
        output.writeBool(isTable);
        output.writeInt(columns.size());
        for (Column column : columns) {
            output.writeStr(column.name);
            column.schema.write(output);
        }
    }

}
