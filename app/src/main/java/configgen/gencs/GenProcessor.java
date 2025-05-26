package configgen.gencs;

import configgen.schema.CfgSchema;
import configgen.schema.Nameable;
import configgen.schema.TableSchema;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;

public class GenProcessor {

    public static class ProcessorModel {
        public final String topPkg;
        public final Iterable<TableSchema> tableSchemas;
        private final GenCs gen;

        public ProcessorModel(GenCs gen, Iterable<TableSchema> tableSchemas) {
            this.gen = gen;
            this.topPkg = gen.pkg;
            this.tableSchemas = tableSchemas;
        }

        public String fullName(Nameable nameable) {
            return new Name(gen.pkg, gen.prefix, nameable).fullName;
        }
    }

    static void generate(GenCs gen, CfgSchema cfgSchema) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(gen.dstDir.resolve("Processor.cs"), gen.encoding)) {
            JteEngine.render("cs/Processor.jte", new ProcessorModel(gen, cfgSchema.sortedTables()), ps);
        }
    }
}
