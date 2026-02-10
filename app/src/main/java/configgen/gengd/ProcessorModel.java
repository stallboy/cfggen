package configgen.gengd;

import configgen.schema.Nameable;
import configgen.schema.TableSchema;

public class ProcessorModel {
    public final Iterable<TableSchema> tableSchemas;
    private final GdCodeGenerator gen;

    public ProcessorModel(GdCodeGenerator gen, Iterable<TableSchema> tableSchemas) {
        this.gen = gen;
        this.tableSchemas = tableSchemas;
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.prefix, nameable).className;
    }
}
