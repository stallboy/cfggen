package configgen.gengo;

import configgen.naming.GenNaming;
import configgen.schema.Nameable;
import configgen.util.StringUtil;

/// go命名规范
/// 文件名全小写，用下划线分割
/// 包名一律小写，不使用下划线或驼峰
/// 类型名CamelCase
/// 变量名camlCase,私有变量首字母小写，导出变量首字母大写
/// 函数名，导出函数首字母大写，包内函数小写
public class GoName {

    public final String filePath;
    public final String className;
    public final String pkgName;

    public GoName(Nameable nameable) {
        pkgName = nameable.name();
        String[] seps = GenNaming.classNameSegments(nameable).toArray(new String[0]);

        String _filePath = "";
        String _className = "";
        for (int i = 0; i < seps.length; i++) {
            _filePath = _filePath + seps[i].toLowerCase();
            if (i < seps.length - 1)
                _filePath = _filePath + '_';

            _className = _className + StringUtil.upper1(seps[i]);
        }
        filePath = _filePath + ".go";
        className = _className;
    }
}