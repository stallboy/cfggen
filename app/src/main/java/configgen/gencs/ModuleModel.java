package configgen.gencs;

import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.*;

/**
 * 一个顶层模块的所有数据，用于生成模块加载文件。
 * 顶层模块 = Name.path 的第一个目录段（如 "Ai", "Task", "Equip"）。
 * 根模块 = 没有子目录的类（moduleKey = ""）。
 */
public class ModuleModel {
    public final String topPkg;
    public final String moduleKey;     // "" for root, "Ai", "Task", etc.
    public final String loaderPath;    // output file path

    // Grouped by namespace, preserves insertion order
    private final LinkedHashMap<String, NamespaceGroup> groups = new LinkedHashMap<>();

    private final CsCodeGenerator gen;

    public ModuleModel(CsCodeGenerator gen, String moduleKey) {
        this.gen = gen;
        this.topPkg = gen.pkg;
        this.moduleKey = moduleKey;
        this.loaderPath = moduleKey.isEmpty()
                ? "RootLoader.cs"
                : moduleKey + "/" + moduleKey + "Loader.cs";
    }

    public void addStruct(StructModel model) {
        String ns = model.name.pkg;
        groups.computeIfAbsent(ns, NamespaceGroup::new).structs.add(model);
    }

    public void addInterface(InterfaceModel model) {
        String ns = model.name.pkg;
        groups.computeIfAbsent(ns, NamespaceGroup::new).interfaces.add(model);
    }

    public List<NamespaceGroup> groups() {
        return new ArrayList<>(groups.values());
    }


    public String fullName(Nameable nameable) {
        return new Name(gen.pkg, gen.prefix, nameable).fullName;
    }

    public String upper1(String value) {
        return StringUtil.upper1(value);
    }

    public static class NamespaceGroup {
        public final String ns;  // full namespace like "Config.Task"
        public final List<StructModel> structs = new ArrayList<>();
        public final List<InterfaceModel> interfaces = new ArrayList<>();

        NamespaceGroup(String ns) {
            this.ns = ns;
        }
    }
}
