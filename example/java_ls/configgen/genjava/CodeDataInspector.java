package configgen.genjava;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static configgen.genjava.JsonValue.*;

/**
 * 检查已加载 ConfigMgr 里的数据，按 {@code ConfigCodeSchema.getCodeSchema()} 返回的 Schema 递归翻译成
 * 通用的 {@link JsonValue} 结构。
 *
 * <p>纯反射，不依赖生成的 ConfigMgr / ConfigCodeSchema 类型 —— 构造器收 {@code Object mgr} + {@link SchemaInterface}，
 * 调用方自行接线：{@code new CodeDataInspector(ConfigMgr.getMgr(), ConfigCodeSchema.getCodeSchema())}。
 *
 * <p>领域→JSON 翻译约定：
 * <ul>
 *   <li>结构/bean → {@link JsonValue.Obj}（字段名→值）</li>
 *   <li>接口多态值 → {@link JsonValue.Obj}，{@code impl} 字段为实现类简名（渲染为 {@code TypeName{...}}）</li>
 *   <li>map → {@link JsonValue.Obj}（键字符串化→值）</li>
 *   <li>list → {@link JsonValue.Arr}</li>
 *   <li>原始值/枚举名 → {@link JsonValue.Num}/{@link JsonValue.Str}/{@link JsonValue.Bool}</li>
 *   <li>表/键找不到 → {@link JsonValue.Obj} 单成员 {@code error: 信息}</li>
 * </ul>
 * 不展示 ref：schema 里本就没有外键引用（refs 是 codegen 的运行期附加），只渲染 schema 数据列。
 */
public class CodeDataInspector {

    private final Object mgr;
    private final SchemaInterface rootSchema;
    private List<TableData> cachedTables;

    public CodeDataInspector(Object mgr, SchemaInterface rootSchema) {
        this.mgr = mgr;
        this.rootSchema = rootSchema;
    }

    // ==================== 公开 API（均返回通用 JsonValue） ====================

    /**
     * schema 描述：按 schema 名匹配，扁平的 {@code {名: {列名: 类型串, ...}}}；
     * 枚举渲染为 {@code {Red: 1, Blue: 2}}（有 int 值）或 {@code [Red, Blue]}，接口为 {@code {ImplName: {列}, ...}}。
     * schema 驱动 —— 涵盖表、结构、纯枚举表（如 equip.jewelrytype）等所有具名类型。
     */
    public JsonValue schemaJson(String tableMatch) {
        List<Member> members = new ArrayList<>();
        HashSet<String> done = new HashSet<>();
        for (String name : rootSchema.implementations.keySet()) {
            if (!nameMatches(name, tableMatch)) continue;
            dedupAddType(members, name, done);
        }
        return new Obj(null, members);
    }

    /**
     * get(table, key)：返回该记录的 {@link JsonValue}（impl 标为表名，渲染为 {@code 表名{...}}）；
     * 表/键找不到返回 {@code {error:...}}。key 位置式："5" / "sword" / 复合 "3,5" / 数组表按下标。
     */
    public JsonValue getJson(String table, String key) {
        TableData td = findTable(table);
        if (td == null) {
            return obj(member("error", new Str("表未找到: " + table + "（用 schema 查看全部表名）")));
        }
        Object record = getByKey(td, key);
        if (record == null) {
            return obj(member("error", new Str("键未找到: " + key + " @ " + td.displayName)));
        }
        return recordToJson(record, td.recordSchema, td.displayName);
    }

    /**
     * query(recordMatch, tableMatch)：全字段子串匹配（大小写不敏感）。
     * 直接返回记录数组，每条记录 impl 标为所属表名（渲染为 {@code 表名{...}}，跨表可区分来源）。
     * 命中上限 50/表、200 总。
     */
    public JsonValue queryJson(String recordMatch, String tableMatch) {
        final int perTableCap = 50;
        final int totalCap = 200;
        String needle = recordMatch == null ? "" : recordMatch.toLowerCase();

        List<JsonValue> records = new ArrayList<>();
        int totalShown = 0;
        for (TableData td : discoverTables()) {
            if (totalShown >= totalCap) break;
            if (!nameMatches(td.displayName, tableMatch)) continue;
            int shownThisTable = 0;
            for (Object record : iterRecords(td)) {
                if (shownThisTable >= perTableCap || totalShown >= totalCap) break;
                JsonValue jv = recordToJson(record, td.recordSchema, td.displayName);
                if (!needle.isEmpty() && !flatContains(jv, needle)) continue;
                records.add(jv);
                shownThisTable++;
                totalShown++;
            }
        }
        return new Arr(records);
    }

    /** 把一条记录组装成 {@code Obj(表名, 字段)}，便于渲染为 {@code 表名{...}}。 */
    private JsonValue recordToJson(Object record, SchemaBean schema, String tableName) {
        if (record == null) {
            return new Null();
        }
        return new Obj(tableName, collectMembers(record, schema));
    }

    // ==================== 表发现 ====================

    private record TableData(String displayName, SchemaBean recordSchema, Object data, boolean isArray) {
    }

    /**
     * 枚举所有有数据的表：schema 里 isTable 的 SchemaBean，按表名推 ConfigMgr 的 {@code *_All} 字段。
     * enum 表的数据 bean 在 {@code <table>_Detail}，去掉后缀得到表名。
     */
    private List<TableData> discoverTables() {
        if (cachedTables != null) {
            return cachedTables;
        }
        List<TableData> list = new ArrayList<>();
        if (mgr != null) {
            Class<?> cls = mgr.getClass();
            for (Map.Entry<String, Schema> e : rootSchema.implementations.entrySet()) {
                if (!(e.getValue() instanceof SchemaBean bean) || !bean.isTable) continue;
                String key = e.getKey();
                String base = key.endsWith("_Detail")
                        ? key.substring(0, key.length() - "_Detail".length())
                        : key;
                String fieldName = base.replace('.', '_') + "_All";
                try {
                    Field f = cls.getField(fieldName);
                    Object data = f.get(mgr);
                    if (data == null) continue;
                    list.add(new TableData(base, bean, data, data.getClass().isArray()));
                } catch (NoSuchFieldException ignored) {
                    // 纯枚举表等无 _All 字段，跳过
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        cachedTables = list;
        return list;
    }

    private TableData findTable(String table) {
        if (table == null || table.isEmpty()) return null;
        TableData contains = null;
        int containsHits = 0;
        for (TableData td : discoverTables()) {
            if (td.displayName.equals(table)) return td;
            if (td.displayName.contains(table)) {
                contains = td;
                containsHits++;
            }
        }
        return containsHits == 1 ? contains : null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean nameMatches(String name, String match) {
        if (match == null || match.isEmpty()) return true;
        return name.toLowerCase().contains(match.toLowerCase());
    }

    // ==================== 取记录 ====================

    private Object getByKey(TableData td, String keyStr) {
        if (td.isArray) {
            int idx;
            try {
                idx = Integer.parseInt(keyStr.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
            int len = Array.getLength(td.data);
            if (idx < 0 || idx >= len) return null;
            return Array.get(td.data, idx);
        }
        if (!(td.data instanceof Map<?, ?> map)) return null;
        if (map.isEmpty()) return null;
        Object sampleKey = map.keySet().iterator().next();
        Object key;
        try {
            key = buildKey(sampleKey.getClass(), keyStr);
        } catch (Exception ex) {
            return null;
        }
        if (key == null) return null;
        @SuppressWarnings("unchecked")
        Map<Object, ?> raw = (Map<Object, ?>) map;
        return raw.get(key);
    }

    /**
     * 按位置式 key 构造：单值（无逗号）的原始/字符串 key 直接转换，绕开反射构造器；
     * 复合 "3,5" 或单字段 record 走反射构造器按形参强转；枚举走 valueOf。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object buildKey(Class<?> keyClass, String keyStr) throws Exception {
        if (keyStr.indexOf(',') < 0) {
            Object single = singlePrimitive(keyClass, keyStr.trim());
            if (single != null) return single;
        }
        String[] parts = keyStr.split(",");
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        if (keyClass.isEnum()) {
            return Enum.valueOf((Class<Enum>) keyClass.asSubclass(Enum.class), parts[0]);
        }
        Constructor<?> ctor = pickCtor(keyClass, parts.length);
        if (ctor == null) return null;
        ctor.setAccessible(true);
        Class<?>[] ptypes = ctor.getParameterTypes();
        Object[] args = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            args[i] = coerce(ptypes[i], parts[i]);
        }
        return ctor.newInstance(args);
    }

    private static Constructor<?> pickCtor(Class<?> cls, int paramCount) {
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterCount() == paramCount) return c;
        }
        return null;
    }

    /** 单值原始/字符串 key 直转；非这些类型（枚举/record/bean）返回 null，交由构造器路径处理。 */
    private static Object singlePrimitive(Class<?> keyClass, String s) {
        if (keyClass == String.class) return s;
        if (keyClass == Integer.class || keyClass == int.class) return Integer.parseInt(s);
        if (keyClass == Long.class || keyClass == long.class) return Long.parseLong(s);
        if (keyClass == Float.class || keyClass == float.class) return Float.parseFloat(s);
        if (keyClass == Double.class || keyClass == double.class) return Double.parseDouble(s);
        if (keyClass == Boolean.class || keyClass == boolean.class) return Boolean.parseBoolean(s);
        return null;
    }

    private static Object coerce(Class<?> targetType, String s) {
        Object v = singlePrimitive(targetType, s);
        return v != null ? v : s;
    }

    // ==================== 记录遍历 / 匹配 ====================

    private static Iterable<Object> iterRecords(TableData td) {
        List<Object> list = new ArrayList<>();
        if (td.isArray) {
            int len = Array.getLength(td.data);
            for (int i = 0; i < len; i++) list.add(Array.get(td.data, i));
        } else if (td.data instanceof Map<?, ?> map) {
            list.addAll(map.values());
        } else if (td.data instanceof Collection<?> col) {
            list.addAll(col);
        }
        return list;
    }

    /** 把记录所有叶子值拍平成串做大小写不敏感子串匹配（needle 已小写）。 */
    private static boolean flatContains(JsonValue v, String lowerNeedle) {
        StringBuilder sb = new StringBuilder();
        appendFlat(v, sb);
        return sb.toString().toLowerCase().contains(lowerNeedle);
    }

    /** 对领域无关的 {@link JsonValue} 拍平：只收集叶子值/键/类型名，与 {@link #valueToJson} 共用同一棵翻译树。 */
    private static void appendFlat(JsonValue v, StringBuilder sb) {
        switch (v) {
            case Obj o -> {
                if (o.impl() != null) sb.append(o.impl()).append(' ');
                for (Member m : o.members()) {
                    sb.append(m.name()).append(' ');
                    appendFlat(m.value(), sb);
                }
            }
            case Arr a -> {
                for (JsonValue e : a.items()) appendFlat(e, sb);
            }
            case Null ignored -> {
            }
            case Bool b -> sb.append(b.value()).append(' ');
            case Num n -> sb.append(n.value()).append(' ');
            case Str s -> sb.append(s.value()).append(' ');
        }
    }

    // ==================== 值 → JsonValue ====================

    private JsonValue valueToJson(Object value, Schema sc) {
        return switch (sc) {
            case SchemaBean bean -> {
                if (value == null) yield new Null();
                yield new Obj(null, collectMembers(value, bean));
            }
            case SchemaInterface si -> {
                if (value == null) yield new Null();
                String implName = value.getClass().getSimpleName();
                Schema impl = si.implementations.get(implName);
                List<Member> members = impl instanceof SchemaBean bean
                        ? collectMembers(value, bean)
                        : reflectiveMembers(value);
                yield new Obj(implName, members);
            }
            case SchemaList sl -> {
                List<JsonValue> items = new ArrayList<>();
                if (value instanceof Iterable<?> it) {
                    for (Object e : it) items.add(valueToJson(e, sl.ele()));
                }
                yield new Arr(items);
            }
            case SchemaMap sm -> {
                List<Member> members = new ArrayList<>();
                if (value instanceof Map<?, ?> raw) {
                    for (Map.Entry<?, ?> e : raw.entrySet()) {
                        members.add(member(String.valueOf(e.getKey()), valueToJson(e.getValue(), sm.value())));
                    }
                }
                yield new Obj(null, members);
            }
            case SchemaRef ref -> {
                Schema target = rootSchema.implementations.get(ref.type);
                if (target instanceof SchemaBean bean) yield valueToJson(value, bean);
                if (target instanceof SchemaInterface si) yield valueToJson(value, si);
                // 枚举等：getter 返回名字字符串
                yield of(value);
            }
            case SchemaPrimitive ignored -> of(value);
            case SchemaEnum ignored -> of(value);
        };
    }

    private List<Member> collectMembers(Object value, SchemaBean bean) {
        List<Member> members = new ArrayList<>();
        for (SchemaBean.Column col : bean.columns) {
            Object v;
            try {
                v = getterValue(value, col.name());
            } catch (Exception ex) {
                members.add(member(col.name(), new Str("<不可读>")));
                continue;
            }
            members.add(member(col.name(), valueToJson(v, col.schema())));
        }
        return members;
    }

    /** 退化路径：schema 找不到对应实现（如小写名 aa→类 Aa）时，按反射塞公开 getter。 */
    private List<Member> reflectiveMembers(Object value) {
        List<Member> members = new ArrayList<>();
        for (Method md : value.getClass().getMethods()) {
            String n = md.getName();
            if (!n.startsWith("get") || n.equals("getClass") || md.getParameterCount() != 0) continue;
            String prop = n.substring(3);
            if (prop.isEmpty()) continue;
            try {
                members.add(member(Character.toLowerCase(prop.charAt(0)) + prop.substring(1), of(md.invoke(value))));
            } catch (Exception ignored) {
            }
        }
        return members;
    }

    private static Object getterValue(Object obj, String colName) throws Exception {
        String getter = "get" + Character.toUpperCase(colName.charAt(0)) + colName.substring(1);
        Method m = obj.getClass().getMethod(getter);
        return m.invoke(obj);
    }

    // ==================== schema() 描述 ====================

    /** 把一个 bean/表加为 {@code name { col: type }} 块（类型串用 schema toString）。调用方负责去重。 */
    private void addBeanSchema(List<Member> members, String name, SchemaBean bean) {
        List<Member> cols = new ArrayList<>();
        for (SchemaBean.Column col : bean.columns) {
            cols.add(member(col.name(), new Str(typeStr(col.schema()))));
        }
        members.add(member(name, new Obj(null, cols)));
    }

    /** 递归收集列里引用到的具名类型（bean/枚举/接口），各自加一块。 */
    private void collectDepSchema(List<Member> members, Schema sc, HashSet<String> done) {
        switch (sc) {
            case SchemaRef ref -> dedupAddType(members, ref.type, done);
            case SchemaList sl -> collectDepSchema(members, sl.ele(), done);
            case SchemaMap sm -> {
                collectDepSchema(members, sm.key(), done);
                collectDepSchema(members, sm.value(), done);
            }
            default -> {
            }
        }
    }

    private void dedupAddType(List<Member> members, String name, HashSet<String> done) {
        if (!done.add(name)) return;
        Schema target = rootSchema.implementations.get(name);
        switch (target) {
            case SchemaBean tb -> {
                addBeanSchema(members, name, tb);
                for (SchemaBean.Column col : tb.columns)
                    collectDepSchema(members, col.schema(), done);
            }
            case SchemaEnum te -> {
                // 枚举：有 int 值 → {Red: 1, Blue: 2}；否则 → [Red, Blue]
                if (te.hasIntValue) {
                    List<Member> vals = new ArrayList<>(te.values.size());
                    for (Map.Entry<String, Integer> e : te.values.entrySet()) {
                        vals.add(member(e.getKey(), of(e.getValue())));
                    }
                    members.add(member(name, new Obj(null, vals)));
                } else {
                    List<JsonValue> vals = new ArrayList<>(te.values.size());
                    for (String n : te.values.keySet()) {
                        vals.add(new Str(n));
                    }
                    members.add(member(name, new Arr(vals)));
                }
            }
            case SchemaInterface ti -> {
                // 接口：{ImplName: {列}, ...}
                List<Member> impls = new ArrayList<>(ti.implementations.size());
                for (Map.Entry<String, Schema> e : ti.implementations.entrySet()) {
                    if (e.getValue() instanceof SchemaBean tb) {
                        List<Member> cols = new ArrayList<>(tb.columns.size());
                        for (SchemaBean.Column col : tb.columns) {
                            cols.add(member(col.name(), new Str(typeStr(col.schema()))));
                        }
                        impls.add(member(e.getKey(), new Obj(null, cols)));
                    }
                }
                members.add(member(name, new Obj(null, impls)));
                for (Schema s : ti.implementations.values()) {
                    if (s instanceof SchemaBean tb) {
                        for (SchemaBean.Column col : tb.columns)
                            collectDepSchema(members, col.schema(), done);
                    }
                }
            }
            default -> {
            }
        }
    }

    /** 友好的类型串：int/long/float/bool/str/text、list<...>、map<...,...>、ref 用其名。 */
    private String typeStr(Schema sc) {
        return switch (sc) {
            case SchemaPrimitive p -> primStr(p);
            case SchemaRef ref -> ref.type;
            case SchemaList sl -> "list<" + typeStr(sl.ele()) + ">";
            case SchemaMap sm -> "map<" + typeStr(sm.key()) + "," + typeStr(sm.value()) + ">";
            case SchemaBean ignored -> "bean";
            case SchemaInterface ignored -> "interface";
            case SchemaEnum ignored -> "enum";
        };
    }

    private static String primStr(SchemaPrimitive p) {
        return switch (p) {
            case SBool -> "bool";
            case SInt -> "int";
            case SLong -> "long";
            case SFloat -> "float";
            case SStr -> "str";
            case SText -> "text";
        };
    }
}
