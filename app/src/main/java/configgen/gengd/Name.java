package configgen.gengd;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;
import configgen.schema.StructSchema;
import configgen.util.StringUtil;

public class Name {
    public final String className;
    public final String path;

    Name(String prefix, Nameable nameable) {
        String name;
        InterfaceSchema nullableInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
        if (nullableInterface != null) {
            name = nullableInterface.name().toLowerCase() + "." + nameable.name();
        } else {
            name = nameable.name();
        }
        String[] seps = name.split("\\.");

        // 用下划线连接nameable的所有部分
        StringBuilder classNameBuilder = new StringBuilder(prefix);
        for (int i = 0; i < seps.length; i++) {
            if (seps.length > 1 && i == seps.length - 1) {
                classNameBuilder.append("_");
            }
            classNameBuilder.append(StringUtil.upper1Only(seps[i]));
        }
        className = classNameBuilder.toString();

        // GDScript使用文件系统路径，类似C#
        if (seps.length == 1) {
            path = className + ".gd";
        } else {
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 0; i < seps.length - 1; i++) {
                if (i > 0) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(StringUtil.upper1Only(seps[i]));
            }
            pathBuilder.append("/").append(className).append(".gd");
            path = pathBuilder.toString();
        }
    }
}
