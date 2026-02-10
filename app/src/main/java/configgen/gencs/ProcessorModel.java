package configgen.gencs;

import configgen.schema.Nameable;
import configgen.schema.TableSchema;

public class ProcessorModel {
    public final String topPkg;
    public final Iterable<TableSchema> tableSchemas;
    private final CsCodeGenerator gen;

    public ProcessorModel(CsCodeGenerator gen, Iterable<TableSchema> tableSchemas) {
        this.gen = gen;
        this.topPkg = gen.pkg;
        this.tableSchemas = tableSchemas;
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.pkg, gen.prefix, nameable).fullName;
    }
}
