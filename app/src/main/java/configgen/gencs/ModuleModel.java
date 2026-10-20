package configgen.gencs;

import java.util.*;

/**
 * 一个顶层模块的所有数据，用于生成模块加载文件。
 * 顶层模块 = Name.path 的第一个目录段（如 "Ai", "Task", "Equip"）。
 * 根模块 = 没有子目录的类（moduleKey = ""）。
 */
public class ModuleModel {
    private final String moduleKey;

    // Grouped by namespace, preserves insertion order
    private final LinkedHashMap<String, NamespaceGroup> groups = new LinkedHashMap<>();
    public final boolean implicitUsings;

    public ModuleModel(CsCodeGenerator gen, String moduleKey) {
        this.moduleKey = moduleKey;
        this.implicitUsings = gen.implicitUsings;
    }

    public String outputFilePath() {
        return "_loaders/%sLoader.cs".formatted(moduleKey.isEmpty() ? "_root" : moduleKey);
    }

    void addStruct(StructModel model) {
        String ns = model.name.pkg;
        groups.computeIfAbsent(ns, NamespaceGroup::new).structs.add(model);
    }

    void addInterface(InterfaceModel model) {
        String ns = model.name.pkg;
        groups.computeIfAbsent(ns, NamespaceGroup::new).interfaces.add(model);
    }

    public List<NamespaceGroup> groups() {
        return new ArrayList<>(groups.values());
    }


    public boolean usesArray() {
        return groups.values().stream().flatMap(g -> g.structs.stream()).anyMatch(StructModel::usesArray);
    }

    public boolean usesCollections() {
        return groups.values().stream().flatMap(g -> g.structs.stream()).anyMatch(StructModel::usesCollections);
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
