package configgen.genjava.code;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;

import java.util.Arrays;

class NameableName {
    final String pkg;
    final String className;
    final String fullName;
    final String path;
    final String containerPrefix;

    NameableName(Nameable nameable) {
        this(nameable, null, "");
    }

    NameableName(Nameable nameable, String postfix) {
        this(nameable, null, postfix);
    }

    NameableName(Nameable nameable, InterfaceSchema nullableInterface) {
        this(nameable, nullableInterface, "");
    }

    NameableName(Nameable nameable, InterfaceSchema nullableInterface, String postfix) {
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
