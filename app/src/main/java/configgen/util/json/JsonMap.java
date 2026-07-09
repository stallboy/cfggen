package configgen.util.json;

import java.util.LinkedHashMap;

/**
 * 替代 fastjson2 JSONObject 的最小 JSON 对象 DOM。
 * value 可以是 String / Number / Boolean / null / JsonMap / JsonArr，由 JsonWriter 统一序列化。
 */
public final class JsonMap extends LinkedHashMap<String, Object> {
    public JsonMap() {
        super();
    }

    public JsonMap(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public String toString() {
        return JsonWriter.toCompactString(this);
    }
}
