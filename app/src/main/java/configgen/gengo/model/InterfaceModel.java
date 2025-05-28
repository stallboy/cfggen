package configgen.gengo.model;

import configgen.gengo.GoName;
import configgen.schema.InterfaceSchema;

public class InterfaceModel {
    public final String pkg;
    public final GoName name;
    public final InterfaceSchema sInterface;

    public InterfaceModel(String pkg,GoName name,InterfaceSchema sInterface) {
        this.pkg = pkg;
        this.name = name;
        this.sInterface = sInterface;
    }
}
