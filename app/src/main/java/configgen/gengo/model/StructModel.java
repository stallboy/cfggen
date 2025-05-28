package configgen.gengo.model;

import configgen.gengo.GoName;
import configgen.schema.StructSchema;
import configgen.schema.Structural;

public class StructModel {
    public final String pkg;
    public final GoName name;
    public final Structural structural;
    public final String className;

    public StructModel(String pkg,GoName name,Structural structural) {
        this.pkg = pkg;
        this.name = name;
        this.structural = structural;
        this.className = name.className;
    }
}
