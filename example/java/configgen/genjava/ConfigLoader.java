package configgen.genjava;

import java.util.*;
import java.util.function.*;

public final class ConfigLoader<K, T> {
    private final String table;
    private final LoadValueErrs errors;
    private final Function<ConfigInput, T> decode;
    private final Function<T, K> key;
    private final Consumer<Map<K, T>> index;
    private final Consumer<T> resolve;
    private Map<K, T> rows = new LinkedHashMap<>();
    private Map<K, List<String>> origins = new HashMap<>();

    public ConfigLoader(String table, LoadValueErrs errors, Function<ConfigInput, T> decode, Function<T, K> key,
                        Consumer<Map<K, T>> index, Consumer<T> resolve) {
        this.table = table;
        this.errors = errors;
        this.decode = decode;
        this.key = key;
        this.index = index;
        this.resolve = resolve;
    }

    public void read(ConfigInput input, String pack, boolean base) {
        input.setErrors(errors);
        int count = input.readCount(1);
        for (int i = 0; i < count; i++) {
            T row = decode.apply(input);
            K id = key.apply(row);
            boolean duplicate = rows.put(id, row) != null;
            if (!base) {
                var chain = origins.computeIfAbsent(id, ignored -> new ArrayList<>(2));
                duplicate = !chain.isEmpty() && pack.equals(chain.getLast());
                if (!duplicate) {
                    chain.add(pack);
                }
            }
            if (duplicate) {
                errors.duplicateRecord(table, pack, id);
            }
        }
        input.requireEnd();
    }

    public Map<K, List<String>> origins() {
        return Collections.unmodifiableMap(origins);
    }

    public void index() {
        origins.replaceAll((id, packs) -> List.copyOf(packs));
        origins = Collections.unmodifiableMap(origins);
        rows = Collections.unmodifiableMap(rows);
        index.accept(rows);
    }

    public void resolveAll() {
        if (resolve != null) {
            rows.values().forEach(resolve);
        }
    }
}
