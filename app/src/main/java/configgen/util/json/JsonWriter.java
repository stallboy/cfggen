package configgen.util.json;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 替代 fastjson2 的 JSON.toJSONString / JSON.toJSONBytes。
 * 支持 JsonMap/JsonArr、原生 Map/Iterable/数组、record（含 @JsonField 合成字段与重命名）、enum（按 name）。
 * 既不依赖反射缓存框架，也不使用 sun.misc.Unsafe。
 */
public final class JsonWriter {
    private static final String INDENT_UNIT = "    ";

    private JsonWriter() {
    }

    public static String toPrettyString(Object node) {
        StringBuilder sb = new StringBuilder();
        write(node, sb, 0, true);
        return sb.toString();
    }

    public static String toCompactString(Object node) {
        StringBuilder sb = new StringBuilder();
        write(node, sb, -1, false);
        return sb.toString();
    }

    public static byte[] toBytes(Object node) {
        return toCompactString(node).getBytes(StandardCharsets.UTF_8);
    }

    private static void write(Object node, StringBuilder sb, int indent, boolean pretty) {
        if (node == null) {
            sb.append("null");
        } else if (node instanceof String s) {
            writeString(s, sb);
        } else if (node instanceof Boolean b) {
            sb.append(b.booleanValue());
        } else if (node instanceof Number n) {
            writeNumber(n, sb);
        } else if (node instanceof Enum<?> e) {
            writeString(e.name(), sb);
        } else if (node instanceof JsonMap m) {
            writeMap(m, sb, indent, pretty);
        } else if (node instanceof Map<?, ?> m) {
            writeMap(m, sb, indent, pretty);
        } else if (node instanceof JsonArr a) {
            writeArray(a, sb, indent, pretty);
        } else if (node instanceof Iterable<?> it) {
            writeArray(it, sb, indent, pretty);
        } else if (node instanceof Object[] arr) {
            writeArray(Arrays.asList(arr), sb, indent, pretty);
        } else {
            writePojo(node, sb, indent, pretty);
        }
    }

    private static void writeMap(Map<?, ?> m, StringBuilder sb, int indent, boolean pretty) {
        if (m.isEmpty()) {
            sb.append("{}");
            return;
        }
        int child = pretty ? indent + 1 : -1;
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            if (pretty) {
                sb.append("\n").append(indent(child));
            }
            writeString(String.valueOf(e.getKey()), sb);
            sb.append(pretty ? ": " : ":");
            write(e.getValue(), sb, child, pretty);
        }
        if (pretty) {
            sb.append("\n").append(indent(indent));
        }
        sb.append("}");
    }

    private static void writeArray(Iterable<?> items, StringBuilder sb, int indent, boolean pretty) {
        sb.append("[");
        int child = pretty ? indent + 1 : -1;
        boolean first = true;
        boolean any = false;
        for (Object o : items) {
            any = true;
            if (!first) {
                sb.append(",");
            }
            first = false;
            if (pretty) {
                sb.append("\n").append(indent(child));
            }
            write(o, sb, child, pretty);
        }
        if (pretty && any) {
            sb.append("\n").append(indent(indent));
        }
        sb.append("]");
    }

    /**
     * record（或普通 bean）序列化：先按 record 组件（@JsonField 可重命名），
     * 再追加 @JsonField 标注的普通方法作为合成字段。顺序保持声明顺序，前端按 key 取值，顺序无关。
     */
    private static void writePojo(Object node, StringBuilder sb, int indent, boolean pretty) {
        Class<?> c = node.getClass();
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        if (c.isRecord()) {
            for (RecordComponent rc : c.getRecordComponents()) {
                String name = rc.getName();
                JsonField jf = rc.getAnnotation(JsonField.class);
                if (jf != null && !jf.name().isEmpty()) {
                    name = jf.name();
                }
                fields.put(name, invoke(rc.getAccessor(), node));
            }
        }

        for (Method m : c.getMethods()) {
            if (!m.isAnnotationPresent(JsonField.class)) {
                continue;
            }
            // 跳过 record 组件的访问器（已在上面处理）
            if (c.isRecord() && isRecordAccessor(c, m)) {
                continue;
            }
            String name = m.getName();
            JsonField jf = m.getAnnotation(JsonField.class);
            if (!jf.name().isEmpty()) {
                name = jf.name();
            }
            fields.put(name, invoke(m, node));
        }

        writeMap(fields, sb, indent, pretty);
    }

    private static boolean isRecordAccessor(Class<?> recordType, Method m) {
        for (RecordComponent rc : recordType.getRecordComponents()) {
            if (rc.getAccessor().equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static Object invoke(Method m, Object node) {
        try {
            return m.invoke(node);
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeNumber(Number n, StringBuilder sb) {
        // Float/Double 用各自的 toString，避免 BigDecimal 等意外格式；其余（Integer/Long/Short/Byte/BigInteger）直接 toString
        if (n instanceof Float || n instanceof Double) {
            sb.append(n.toString());
        } else {
            sb.append(n.toString());
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static String indent(int level) {
        if (level <= 0) {
            return "";
        }
        return INDENT_UNIT.repeat(level);
    }
}
