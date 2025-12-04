package configgen.gen;

import java.util.List;

public interface Parameter {

    String get(String key, String def, String messageId);

    boolean has(String key, String messageId);

    default String get(String key, String def) {
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
