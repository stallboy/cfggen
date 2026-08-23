package configgen.gencs;

import configgen.naming.GenNaming;
import configgen.schema.Nameable;
import configgen.util.StringUtil;

public class Name {
    public final String pkg;
    public final String className;
    public final String fullName;
    public final String path;

    Name(String topPkg, String prefix, Nameable nameable) {
        String[] seps = GenNaming.classNameSegments(nameable).toArray(new String[0]);
        String[] pks = new String[seps.length - 1];
        for (int i = 0; i < pks.length; i++)
            pks[i] = StringUtil.underscoreToPascalCase(seps[i]);
        className = prefix + StringUtil.underscoreToPascalCase(seps[seps.length - 1]);

        if (pks.length == 0) {
            pkg = topPkg;
            fullName = className;
        }else {
            String join = String.join(".", pks);
            pkg = topPkg + "." + join;
            fullName = join + "." + className;
        }

        if (pks.length == 0)
            path = className + ".cs";
        else
            path = String.join("/", pks) + "/" + className + ".cs";
    }
}
