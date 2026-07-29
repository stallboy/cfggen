package configgen.gen;

import configgen.util.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用的 provider 注册表，封装 Tools 与 Generators 原本重复的
 * 注册、按 id 查找创建、assureNoExtra 校验逻辑。
 *
 * @param <P> provider 函数接口类型（如 Tools.ToolProvider）
 * @param <T> provider 创建的目标类型（如 Tool）
 */
public class ProviderRegistry<P extends ProviderRegistry.Provider<T>, T> {
    public interface Provider<T> {
        T create(Parameter parameter);
    }

    private final Map<String, P> providers = new LinkedHashMap<>();

    public T create(String arg) {
        ParameterParser parameter = new ParameterParser(arg);
        P provider = providers.get(parameter.id());
        if (provider == null) {
            Logger.log(parameter.id() + " not support");
            return null;
        }
        T result = provider.create(parameter);
        parameter.assureNoExtra();
        return result;
    }

    public void addProvider(String name, P provider) {
        providers.put(name, provider);
    }

    public Map<String, P> getAllProviders() {
        return providers;
    }
}
