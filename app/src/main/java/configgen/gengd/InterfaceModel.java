package configgen.gengd;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;

public class InterfaceModel {
    public final Name name;
    public final InterfaceSchema sInterface;
    private final GenGd gen;

    public InterfaceModel(GenGd gen, InterfaceSchema sInterface) {
        this.gen = gen;
        this.name = new Name(gen.prefix, sInterface);
        this.sInterface = sInterface;
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.prefix, nameable).className;
    }
}
