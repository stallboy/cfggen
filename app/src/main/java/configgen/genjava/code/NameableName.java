package configgen.genjava.code;

import configgen.schema.InterfaceSchema;
import configgen.schema.Nameable;
import configgen.schema.StructSchema;

import java.util.Arrays;

public class NameableName {

    static boolean isSealedInterface = false;

    public final Nameable nameable;
    public final String pkg;
    public final String className;
    public final String fullName;
    public final String path;
    public final String containerPrefix;

    public NameableName(Nameable nameable) {
        this(nameable, "");
    }

    public NameableName(Nameable nameable, String postfix) {
        this.nameable = nameable;
        InterfaceSchema nullableInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
        String topPkg = Name.codeTopPkg;
        String name;
        if (nullableInterface != null) {
            name = nullableInterface.name().toLowerCase() + "." + nameable.name();
        } else if (isSealedInterface && nameable instanceof InterfaceSchema sInterface) { //java要求：sealed interface需要跟impl在同一个package下
            String[] split = sInterface.name().split("\\.");
            String interfaceName = split[split.length - 1];
            name = sInterface.name().toLowerCase() + "." + interfaceName;
        } else {
            name = nameable.name();
        }

        name += postfix;
        containerPrefix = nameable.name().replace('.', '_') + "_";
        String[] seps = name.split("\\.");
        String c = seps[seps.length - 1];
        // _Entry/_Detail 是生成器为区隔 entry 包装类/enum 详情类而加的后缀，须原样保留，只对基础名做
        // pascal 化：equip_config + _Entry -> EquipConfig_Entry（而非 EquipConfigEntry），否则分不清表名与后缀。
        // 老行为（upper1 整段）本就只改首字母、后缀不动，这里拆开后对 non-beautiful 等价。
        String base = postfix.isEmpty() ? c : c.substring(0, c.length() - postfix.length());
        className = Name.pascalName(base) + postfix;

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
