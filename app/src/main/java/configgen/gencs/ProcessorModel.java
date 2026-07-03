package configgen.gencs;

import configgen.schema.EntryType;
import configgen.schema.TableSchema;

public class ProcessorModel {
    public final String topPkg;
    public final Iterable<TableSchema> tableSchemas;
    private final CsCodeGenerator gen;
    public final boolean unity;

    public ProcessorModel(CsCodeGenerator gen, Iterable<TableSchema> tableSchemas) {
        this.gen = gen;
        this.topPkg = gen.pkg;
        this.tableSchemas = tableSchemas;
        this.unity = gen.unity;
    }

    public String fullName(TableSchema tableSchema) {
        String v = new Name(gen.pkg, gen.prefix, tableSchema).fullName;
        if (tableSchema.entry() instanceof EntryType.EEnum) {
            return v + "Info";
        } else {
            return v;
        }
    }

    public String nsLine() {
        return unity ? "namespace " + topPkg + "\n{" : "namespace " + topPkg + ";";
    }
}
