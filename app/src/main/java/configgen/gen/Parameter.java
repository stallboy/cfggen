package configgen.gen;

public interface Parameter {
    String get(String key, String def);

    boolean has(String key);

    default void end() {
    }

    default String get(String key, String def, String messageId) {
        return get(key, def);
    }
}
