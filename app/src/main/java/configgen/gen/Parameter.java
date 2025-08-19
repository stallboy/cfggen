package configgen.gen;

public interface Parameter {
    Parameter copy();

    String get(String key, String def);

    boolean has(String key);

    default String get(String key, String def, String messageId) {
        return get(key, def);
    }

    default boolean has(String key, String messageId) {
        return has(messageId);
    }
}
