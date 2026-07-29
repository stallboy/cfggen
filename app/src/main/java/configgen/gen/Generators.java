package configgen.gen;

import java.util.Map;

public class Generators {
    public interface GeneratorProvider extends ProviderRegistry.Provider<Generator> {
    }

    private static final ProviderRegistry<GeneratorProvider, Generator> registry = new ProviderRegistry<>();

    public static Generator create(String arg) {
        return registry.create(arg);
    }

    public static void addProvider(String name, GeneratorProvider provider) {
        registry.addProvider(name, provider);
    }

    public static Map<String, GeneratorProvider> getAllProviders() {
        return registry.getAllProviders();
    }
}
