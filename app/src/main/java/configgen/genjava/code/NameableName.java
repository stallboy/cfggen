package configgen.genjava.code;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;
import configgen.schema.StructSchema;

import java.util.Arrays;

class NameableName {
    final String pkg;
    final String className;
    final String fullName;
    final String path;
    final String containerPrefix;

    NameableName(Nameable nameable) {
        this(nameable, "");
    }

    NameableName(Nameable nameable, String postfix) {
        InterfaceSchema nullableInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
        String topPkg = Name.codeTopPkg;
        String name;
        if (nullableInterface != null) {
            name = nullableInterface.name().toLowerCase() + "." + nameable.name();
        } else {
            name = nameable.name();
        }

        name += postfix;
        containerPrefix = nameable.name().replace('.', '_') + "_";
        String[] seps = name.split("\\.");
        String c = seps[seps.length - 1];
        className = c.substring(0, 1).toUpperCase() + c.substring(1);

        String[] pks = Arrays.copyOf(seps, seps.length - 1);
        if (pks.length == 0)
            pkg = topPkg;
        else
            pkg = topPkg + "." + String.join(".", pks);

        fullName = pkg + "." + className;
        if (pks.length == 0)
            path = className + ".java";
        else
            path = String.join("/", pks) + "/" + className + ".java";
    }
}
