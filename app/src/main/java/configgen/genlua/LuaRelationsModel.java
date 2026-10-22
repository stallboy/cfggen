package configgen.genlua;

import configgen.gen.Relations;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public record LuaRelationsModel(String pkg, CfgSchema schema) {
    public List<Relations.Type> types() { return Relations.of(schema); }
    public String ints(List<Integer> indices) { return "{" + indices.stream().map(i -> Integer.toString(i + 1)).collect(Collectors.joining(",")) + "}"; }
    public String fields(Structural type) { return "{" + type.fields().stream().map(f -> "'" + StringUtil.lower1(f.name()) + "'").collect(Collectors.joining(",")) + "}"; }
    public boolean hasLong() {
        return types().stream().anyMatch(t -> t.owner().fields().stream().anyMatch(f -> containsLong(f.type())));
    }
    private static boolean containsLong(FieldType type) {
        return switch (type) {
            case FieldType.Primitive p -> p == FieldType.Primitive.LONG;
            case FieldType.FList l -> containsLong(l.item());
            case FieldType.FMap m -> containsLong(m.key()) || containsLong(m.value());
            default -> false;
        };
    }
    static String keys(TableSchema table) {
        List<KeySchema> keys = new ArrayList<>(); keys.add(table.primaryKey()); keys.addAll(table.uniqueKeys());
        return "{" + keys.stream().map(k -> "{'" + (k == table.primaryKey() ? "all" : Name.uniqueKeyMapName(k))
                + "','" + (k == table.primaryKey() ? "get" : Name.uniqueKeyGetByName(k)) + "',{" + k.fields().stream()
                .map(n -> "'" + StringUtil.lower1(n) + "'").collect(Collectors.joining(",")) + "}}")
                .collect(Collectors.joining(",")) + "}";
    }
}
