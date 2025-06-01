package configgen.gengo.model;

import configgen.gengo.GoName;
import configgen.schema.StructSchema;
import configgen.schema.Structural;
import configgen.value.CfgValue;

public class StructModel {
    public final String pkg;
    public final GoName name;
    public final Structural structural;
    public final String className;
    public final CfgValue.VTable vTable;
    public StructModel(String pkg,GoName name,Structural structural,CfgValue.VTable vTable) {
        this.pkg = pkg;
        this.name = name;
        this.structural = structural;
        this.className = name.className;
        this.vTable = vTable;
    }
}
