package configgen.gengo;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;
import configgen.schema.StructSchema;
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
        // 不用 naming.GenNaming.classNameSegments：这里既有规则是把 interface 名整体小写后作前缀段
        // （会抹掉 TriggerTick 这类 camel，属历史行为，下游已依赖，改动需专门决策）
        String name;
        InterfaceSchema nullableInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
        if (nullableInterface != null) {
            name = nullableInterface.name().toLowerCase() + "." + nameable.name();
        } else {
            name = nameable.name();
        }
        pkgName = nameable.name();
        String[] seps = name.split("\\.");

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