package configgen.editorserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import configgen.schema.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Converts only schema-declared long fields at the editor transport boundary. */
final class EditorValueJson {
    static final String ENCODING = "long-decimal-string";
    private static final BigInteger MAX_SAFE_INTEGER = BigInteger.valueOf(9007199254740991L);

    private EditorValueJson() {}

    static String decode(TableSchema table, String text) {
        try {
            JSONObject object = JSON.parseObject(text, JSONReader.Feature.UseBigDecimalForDoubles);
            if (object != null) decodeStructural(table, object, table.name());
            return JSON.toJSONString(object);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static void decodeStructural(Structural schema, JSONObject object, String path) {
        for (FieldSchema field : schema.fields()) {
            Object value = object.get(field.name());
            if (value != null) object.put(field.name(), decodeValue(field.type(), value, path + "." + field.name()));
        }
    }

    private static Object decodeValue(FieldType type, Object value, String path) {
        switch (type) {
            case FieldType.Primitive.LONG -> {
                try {
                    if (value instanceof String text && text.matches("-?[0-9]+")) return Long.parseLong(text);
                    if (value instanceof Number number) {
                        BigInteger integer = new BigDecimal(number.toString()).toBigIntegerExact();
                        if (integer.abs().compareTo(MAX_SAFE_INTEGER) <= 0) return integer.longValueExact();
                    }
                } catch (ArithmeticException | NumberFormatException ignored) {
                    // Report the field path through the existing editor error channel.
                }
                throw new IllegalArgumentException(path + ": long requires a 64-bit decimal string or a safe integer number");
            }
            case FieldType.StructRef ref when value instanceof JSONObject object -> {
                switch (ref.obj()) {
                    case Structural structural -> decodeStructural(structural, object, path);
                    case InterfaceSchema iface -> {
                        String name = object.getString("$type");
                        if (name != null && name.startsWith(iface.name() + ".")) name = name.substring(iface.name().length() + 1);
                        StructSchema impl = name == null ? null : iface.findImpl(name);
                        if (impl != null) decodeStructural(impl, object, path);
                    }
                }
            }
            case FieldType.FList list when value instanceof JSONArray array -> {
                for (int i = 0; i < array.size(); i++) array.set(i, decodeValue(list.item(), array.get(i), path + "[" + i + "]"));
            }
            case FieldType.FMap map when value instanceof JSONArray array -> {
                for (int i = 0; i < array.size(); i++) {
                    if (array.get(i) instanceof JSONObject entry) {
                        for (String name : new String[]{"key", "value"}) {
                            Object item = entry.get(name);
                            if (item != null) entry.put(name, decodeValue(name.equals("key") ? map.key() : map.value(), item,
                                    path + "[" + i + "]." + name));
                        }
                    }
                }
            }
            default -> { }
        }
        return value;
    }
}
