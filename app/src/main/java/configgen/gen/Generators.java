package configgen.gen;

import configgen.util.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class Generators {
    public interface GeneratorProvider {
        Generator create(Parameter parameter);
    }

    private static final Map<String, GeneratorProvider> providers = new LinkedHashMap<>();

    public static Generator create(String arg) {
        ParameterParser parameter = new ParameterParser(arg);
        GeneratorProvider provider = providers.get(parameter.genId());
        if (provider == null) {
            Logger.log(parameter.genId() + " not support");
            return null;
        }
        Generator generator = provider.create(parameter);
        parameter.assureNoExtra();
        return generator;
    }

    public static void addProvider(String name, GeneratorProvider provider) {
        providers.put(name, provider);
    }

    public static Map<String, GeneratorProvider> getAllProviders() {
        return providers;
    }
}
