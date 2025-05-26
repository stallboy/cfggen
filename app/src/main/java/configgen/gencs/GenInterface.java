package configgen.gencs;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;

public class GenInterface {

    public static class InterfaceModel {
        public final String topPkg;
        public final Name name;
        public final InterfaceSchema sInterface;
        private final GenCs gen;

        public InterfaceModel(GenCs gen, Name name, InterfaceSchema sInterface) {
            this.gen = gen;
            this.topPkg = gen.pkg;
            this.name = name;
            this.sInterface = sInterface;
        }

        public String fullName(Nameable nameable) {
            return new Name(gen.pkg, gen.prefix, nameable).fullName;
        }
    }

    static void generate(GenCs gen, InterfaceSchema sInterface) {
        Name name = new Name(gen.pkg, gen.prefix, sInterface);
        try (CachedIndentPrinter ps = new CachedIndentPrinter(gen.dstDir.resolve(name.path), gen.encoding)) {
            JteEngine.render("cs/GenInterface.jte", new InterfaceModel(gen, name, sInterface), ps);
        }
    }

}
