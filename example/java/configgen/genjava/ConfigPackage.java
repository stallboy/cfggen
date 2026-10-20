package configgen.genjava;

import java.util.*;

/** Versioned metadata and named blocks. Block views share input bytes but have independent cursors. */
public final class ConfigPackage {
    public static final int FORMAT_VERSION = 1;
    private final Map<String, String> metadata;
    private final Map<String, ConfigInput> blocks;
    private final String name;
    private final List<String> dependencies;
    private final List<String> defaultLanguages;
    private final Map<List<String>, ConfigInput> pools = new HashMap<>();

    private ConfigPackage(Map<String, String> metadata, Map<String, ConfigInput> blocks, String name,
                          List<String> dependencies) {
        this.metadata = Collections.unmodifiableMap(metadata);
        this.blocks = Collections.unmodifiableMap(blocks);
        this.name = name;
        this.dependencies = dependencies;
        this.defaultLanguages = List.of(metadata.getOrDefault("defaultLanguage", "default"));
    }

    public static ConfigPackage read(ConfigInput input) {
        var errors = input.errors();
        int version = input.readInt();
        if (version != FORMAT_VERSION) {
            throw errors.unsupportedVersion(version, FORMAT_VERSION);
        }
        int count = input.readCount(8);
        Map<String, String> metadata = LinkedHashMap.newLinkedHashMap(count);
        for (int i = 0; i < count; i++) {
            String key = input.readString();
            if (metadata.putIfAbsent(key, input.readString()) != null) {
                errors.duplicateKey("metadata", key);
            }
        }
        count = input.readCount(8);
        Map<String, ConfigInput> blocks = LinkedHashMap.newLinkedHashMap(count);
        for (int i = 0; i < count; i++) {
            String name = input.readString();
            ConfigInput block = input.slice(input.readInt());
            if (blocks.putIfAbsent(name, block) != null) errors.duplicateKey("block", name);
        }
        input.requireEnd();
        String name = metadata.get("pack");
        if (name != null && (name.isBlank() || !name.equals(name.strip()) || name.contains(","))) {
            errors.invalidPackageName(name);
        }
        var dependencies = parseDependencies(metadata.getOrDefault("requires", ""), name, errors);
        errors.checkErrors();
        return new ConfigPackage(metadata, blocks, name, dependencies);
    }

    public Map<String, String> metadata() { return metadata; }
    public Set<String> blockNames() { return blocks.keySet(); }

    public boolean validateSchema(Schema expected, LoadValueErrs errors) {
        var input = block("schema");
        Schema actual = SchemaDeserializer.deserialize(input);
        input.requireEnd();
        String reason = "Incompatible schema or fixed symbols";
        try {
            if (expected.compatible(actual) && actual.compatible(expected)) return true;
        } catch (SchemaCompatibleException failure) {
            reason = failure.getMessage();
        }
        errors.schemaMismatch(name(), reason);
        return false;
    }

    public ConfigInput block(String name, String... languages) {
        ConfigInput block = blocks.get(name);
        if (block == null) throw new ConfigErr(new LoadValueErrs.MissingBlock(name));
        var input = block.fork();
        return name.startsWith(".") ? input.withPools(pools(languages)) : input;
    }

    public ConfigPackage attach(ConfigPackage... resources) {
        if (resources.length == 0) return this;
        var errors = new LoadValueErrs();
        var combined = new LinkedHashMap<>(blocks);
        for (var resource : resources) {
            if (!name().equals(resource.name())) errors.foreignLanguageResource(name(), resource.name());
            if (resource.blocks.isEmpty()) errors.emptyLanguageResource(resource.name());
            resource.blocks.forEach((name, block) -> {
                if (!name.startsWith("text/")) errors.unexpectedLanguageBlock(name);
                if (combined.putIfAbsent(name, block) != null) errors.duplicateKey("language", name);
            });
        }
        errors.checkErrors();
        return new ConfigPackage(metadata, combined, name, dependencies);
    }

    private synchronized ConfigInput pools(String... languages) {
        var selected = languages.length == 0 ? defaultLanguages : List.of(languages);
        var cached = pools.get(selected);
        if (cached != null) return cached;
        ConfigInput decoded = block("strings");
        decoded.readStringPool();
        decoded.requireEnd();
        String[] names = selected.toArray(String[]::new);
        ConfigInput[] inputs = new ConfigInput[names.length];
        for (int i = 0; i < names.length; i++) inputs[i] = block("text/" + names[i]);
        decoded.readLanguageBlocks(names, inputs);
        pools.put(selected, decoded);
        return decoded;
    }

    public String name() {
        if (name == null) throw new ConfigErr(new LoadValueErrs.MissingPackageName());
        return name;
    }

    public List<String> dependencies() { return dependencies; }

    private static List<String> parseDependencies(String value, String name, LoadValueErrs errors) {
        if (value.isEmpty()) return List.of();
        Set<String> result = new LinkedHashSet<>();
        for (String item : value.split(",", -1)) {
            String dependency = item.strip();
            if (dependency.isEmpty() || dependency.equals(name) || !result.add(dependency)) {
                errors.invalidDependency(name, item);
            }
        }
        return List.copyOf(result);
    }

    public static void validateOrder(List<ConfigPackage> packages) {
        var errors = new LoadValueErrs();
        if (packages.isEmpty()) errors.missingBasePackage();
        Set<String> seen = new HashSet<>();
        for (ConfigPackage pack : packages) {
            String name = pack.name();
            if (!seen.add(name)) errors.duplicatePackage(name);
            for (String dependency : pack.dependencies()) {
                if (!seen.contains(dependency)) errors.missingDependency(name, dependency);
            }
        }
        errors.checkErrors();
    }
}
