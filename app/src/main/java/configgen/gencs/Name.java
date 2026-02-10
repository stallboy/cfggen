package configgen.gencs;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;
import configgen.schema.StructSchema;
import configgen.util.StringUtil;

public class Name {
    public final String pkg;
    public final String className;
    public final String fullName;
    public final String path;

    Name(String topPkg, String prefix, Nameable nameable) {
        String name;
        InterfaceSchema nullableInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
        if (nullableInterface != null) {
            name = nullableInterface.name().toLowerCase() + "." + nameable.name();
        } else {
            name = nameable.name();
        }
        String[] seps = name.split("\\.");
        String[] pks = new String[seps.length - 1];
        for (int i = 0; i < pks.length; i++)
            pks[i] = StringUtil.upper1Only(seps[i]);
        className = prefix + StringUtil.upper1Only(seps[seps.length - 1]);

        if (pks.length == 0)
            pkg = topPkg;
        else
            pkg = topPkg + "." + String.join(".", pks);

        if (pkg.isEmpty())
            fullName = className;
        else
            fullName = pkg + "." + className;

        if (pks.length == 0)
            path = className + ".cs";
        else
            path = String.join("/", pks) + "/" + className + ".cs";
    }
}
