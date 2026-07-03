package configgen.gencs;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;

public class InterfaceModel {
    public final String topPkg;
    public final Name name;
    public final InterfaceSchema sInterface;
    private final CsCodeGenerator gen;
    public final boolean unity;

    public InterfaceModel(CsCodeGenerator gen, InterfaceSchema sInterface) {
        this.gen = gen;
        this.topPkg = gen.pkg;
        this.name = new Name(gen.pkg, gen.prefix, sInterface);
        this.sInterface = sInterface;
        this.unity = gen.unity;
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.pkg, gen.prefix, nameable).fullName;
    }

    public String nsLine() {
        return unity ? "namespace " + name.pkg + "\n{" : "namespace " + name.pkg + ";";
    }
}
