package configgen.gen;

import java.util.Map;

public class Tools {
    public interface ToolProvider extends ProviderRegistry.Provider<Tool> {
    }

    private static final ProviderRegistry<ToolProvider, Tool> registry = new ProviderRegistry<>();

    public static Tool create(String arg) {
        return registry.create(arg);
    }

    public static void addProvider(String name, ToolProvider provider) {
        registry.addProvider(name, provider);
    }

    public static Map<String, ToolProvider> getAllProviders() {
        return registry.getAllProviders();
    }
}
