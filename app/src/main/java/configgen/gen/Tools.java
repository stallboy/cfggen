package configgen.gen;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public class Tools {
    public interface ToolProvider extends ProviderRegistry.Provider<Tool> {
    }

    private static final ProviderRegistry<ToolProvider, Tool> registry = new ProviderRegistry<>();

    public static @Nullable Tool create(String arg) {
        return registry.create(arg);
    }

    public static void addProvider(String name, ToolProvider provider) {
        registry.addProvider(name, provider);
    }

    public static Map<String, ToolProvider> getAllProviders() {
        return registry.getAllProviders();
    }
}
