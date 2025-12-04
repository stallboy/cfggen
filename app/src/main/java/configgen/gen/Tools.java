package configgen.gen;

import configgen.util.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tools {
    public interface ToolProvider {
        Tool create(Parameter parameter);
    }

    private static final Map<String, ToolProvider> providers = new LinkedHashMap<>();

    public static Tool create(String arg) {
        ParameterParser parameter = new ParameterParser(arg);
        ToolProvider provider = providers.get(parameter.id());
        if (provider == null) {
            Logger.log(parameter.id() + " not support");
            return null;
        }
        Tool tool = provider.create(parameter);
        parameter.assureNoExtra();
        return tool;
    }

    public static void addProvider(String name, ToolProvider provider) {
        providers.put(name, provider);
    }

    public static Map<String, ToolProvider> getAllProviders() {
        return providers;
    }
}
