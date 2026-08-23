package configgen.gengd;

import configgen.naming.GenNaming;
import configgen.schema.Nameable;
import configgen.util.StringUtil;

public class Name {
    public final String className;
    public final String path;

    Name(String prefix, Nameable nameable) {
        String[] seps = GenNaming.classNameSegments(nameable).toArray(new String[0]);

        // 用下划线连接nameable的所有部分
        StringBuilder classNameBuilder = new StringBuilder(prefix);
        for (int i = 0; i < seps.length; i++) {
            if (seps.length > 1 && i == seps.length - 1) {
                classNameBuilder.append("_");
            }
            classNameBuilder.append(StringUtil.upper1(seps[i]));
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
                pathBuilder.append(StringUtil.upper1(seps[i]));
            }
            pathBuilder.append("/").append(className).append(".gd");
            path = pathBuilder.toString();
        }
    }
}
