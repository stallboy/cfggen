package configgen.gen;

import org.jetbrains.annotations.Contract;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface Parameter {

    @Contract("_, !null, _ -> !null")
    @Nullable String get(String key, @Nullable String def, @Nullable String messageId);

    boolean has(String key, @Nullable String messageId);

    @Contract("_, !null -> !null")
    default @Nullable String get(String key, @Nullable String def) {
        return get(key, def, null);
    }

    default boolean has(String key) {
        return has(key, null);
    }

    default void title(String title) {
    }

    default void extra(List<String> extra) {
    }

}
