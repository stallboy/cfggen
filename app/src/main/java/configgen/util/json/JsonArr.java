package configgen.util.json;

import java.util.ArrayList;

/**
 * 替代 fastjson2 JSONArray 的最小 JSON 数组 DOM。
 * element 可以是 String / Number / Boolean / null / JsonMap / JsonArr，由 JsonWriter 统一序列化。
 */
public final class JsonArr extends ArrayList<Object> {
    public JsonArr() {
        super();
    }

    public JsonArr(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public String toString() {
        return JsonWriter.toCompactString(this);
    }
}
