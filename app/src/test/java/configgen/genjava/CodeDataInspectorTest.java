package configgen.genjava;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.genjava.JsonValue.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用手搭的 Schema + 普通 POJO（带 getXxx）测 {@link CodeDataInspector} 的组装与 {@link CodeDataPrinter} 的渲染。
 * 因渲染纯反射，无需编译生成代码。
 */
class CodeDataInspectorTest {

    // ---------- fixtures ----------

    static class User {
        final int id;
        final String name;

        User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    record PointKey(int x, int y) {
    }

    static class Point {
        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    static class Mgr {
        public final Map<Integer, User> test_user_All = new LinkedHashMap<>();
        public final Map<PointKey, Point> test_point_All = new LinkedHashMap<>();
    }

    private static SchemaInterface buildSchema() {
        SchemaInterface root = new SchemaInterface();
        SchemaBean user = new SchemaBean(true);
        user.addColumn("id", SchemaPrimitive.SInt);
        user.addColumn("name", SchemaPrimitive.SStr);
        root.addImp("test.user", user);

        SchemaBean point = new SchemaBean(true);
        point.addColumn("x", SchemaPrimitive.SInt);
        point.addColumn("y", SchemaPrimitive.SInt);
        root.addImp("test.point", point);

        // 纯枚举表（无 _All 字段、无 _Detail）
        SchemaEnum color = new SchemaEnum(false, true);
        color.addValue("Red", 1);
        color.addValue("Blue", 2);
        root.addImp("test.color", color);
        return root;
    }

    private static Mgr buildMgr() {
        Mgr m = new Mgr();
        m.test_user_All.put(1, new User(1, "alice"));
        m.test_user_All.put(2, new User(2, "bob"));
        m.test_point_All.put(new PointKey(3, 5), new Point(3, 5));
        return m;
    }

    // ---------- inspector: get ----------

    @Test
    void getMap_byIntKey() {
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        JsonValue v = ins.getJson("test.user", "1");
        assertInstanceOf(Obj.class, v);
        assertEquals(1, ((Num) memberValue(v, "id")).value().intValue());
        assertEquals("alice", ((Str) memberValue(v, "name")).value());
    }

    @Test
    void getMap_byCompositeKey() {
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        JsonValue v = ins.getJson("test.point", "3,5");
        assertInstanceOf(Obj.class, v);
        assertEquals(3, ((Num) memberValue(v, "x")).value().intValue());
        assertEquals(5, ((Num) memberValue(v, "y")).value().intValue());
    }

    @Test
    void getMap_missingTable_returnsError() {
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        JsonValue v = ins.getJson("nope", "1");
        Str err = (Str) memberValue(v, "error");
        assertNotNull(err);
        assertTrue(err.value().contains("表未找到"));
    }

    @Test
    void getMap_missingKey_returnsError() {
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        JsonValue v = ins.getJson("test.user", "999");
        Str err = (Str) memberValue(v, "error");
        assertNotNull(err);
        assertTrue(err.value().contains("键未找到"));
    }

    // ---------- inspector: query ----------

    @Test
    void queryJson_returnsListOfRecordsTaggedByTable() {
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        JsonValue q = ins.queryJson("ALI", "");
        assertInstanceOf(Arr.class, q);
        List<JsonValue> items = ((Arr) q).items();
        assertEquals(1, items.size());
        Obj rec = (Obj) items.get(0);
        assertEquals("test.user", rec.impl(), "记录 impl 应为表名");
        assertEquals("alice", ((Str) memberValue(rec, "name")).value());
    }

    @Test
    void queryJson_capsAt50PerTable() {
        Mgr m = new Mgr();
        for (int i = 0; i < 60; i++) {
            m.test_user_All.put(i, new User(i, "name" + i));
        }
        CodeDataInspector ins = new CodeDataInspector(m, buildSchema());
        JsonValue q = ins.queryJson("name", "");
        assertInstanceOf(Arr.class, q);
        assertEquals(50, ((Arr) q).items().size(), "每表上限 50");
    }

    // ---------- inspector: schema ----------

    @Test
    void schemaJson_describesColumns() {
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        JsonValue s = ins.schemaJson("user");
        Obj table = (Obj) memberValue(s, "test.user");
        assertNotNull(table, "应包含 test.user 块");
        assertEquals("int", ((Str) memberValue(table, "id")).value());
        assertEquals("str", ((Str) memberValue(table, "name")).value());
    }

    @Test
    void schemaJson_includesPureEnumTable() {
        // 纯枚举表（无数据 _All 字段）也应能被 schema 列出；有 int 值渲染为 {Red: 1, Blue: 2}
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        JsonValue s = ins.schemaJson("color");
        Obj enumBlock = (Obj) memberValue(s, "test.color");
        assertNotNull(enumBlock, "应包含 test.color 枚举块");
        assertEquals(1, ((Num) memberValue(enumBlock, "Red")).value().intValue());
        assertEquals(2, ((Num) memberValue(enumBlock, "Blue")).value().intValue());
        String out = CodeDataPrinter.render(s);
        assertTrue(out.contains("test.color"), "渲染应含 test.color");
        assertTrue(out.contains("Red: 1"), "枚举值应渲染为 Red: 1");
    }

    @Test
    void schemaRendersTableAndColumns() {
        CodeDataInspector ins = new CodeDataInspector(buildMgr(), buildSchema());
        String out = CodeDataPrinter.render(ins.schemaJson("user"));
        assertTrue(out.contains("test.user"));
        assertTrue(out.contains("id: int"));
        assertTrue(out.contains("name: str"));
    }

    // ---------- printer: render ----------

    @Test
    void render_smallObj_inline() {
        JsonValue v = obj(member("a", of(1)), member("b", new Str("x")));
        assertEquals("{a: 1, b: x}", CodeDataPrinter.render(v));
    }

    @Test
    void render_smallArr_inline() {
        JsonValue v = new Arr(List.of(of(1), of(2)));
        assertEquals("[1, 2]", CodeDataPrinter.render(v));
    }

    @Test
    void render_bigObj_expandsMultiline() {
        // 内联形式超宽（>80）才展开
        List<Member> ms = new ArrayList<>();
        ms.add(member("firstLongFieldName", new Str("value-with-some-length-aaaa")));
        ms.add(member("secondLongFieldName", new Str("value-with-some-length-bbbb")));
        ms.add(member("thirdLongFieldName", new Str("value-with-some-length-cccc")));
        String out = CodeDataPrinter.render(new Obj(null, ms));
        assertTrue(out.contains("\n"), "内联超宽应展开为多行");
        assertTrue(out.contains("firstLongFieldName: value-with-some-length-aaaa"));
        assertTrue(out.trim().startsWith("{"));
        assertTrue(out.trim().endsWith("}"));
    }

    @Test
    void render_shortObjStaysInline() {
        // 字段虽多但内联不超宽 → 保持一行
        List<Member> ms = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ms.add(member("f" + i, of(i)));
        }
        String out = CodeDataPrinter.render(new Obj(null, ms));
        assertFalse(out.contains("\n"), "短对象应保持单行");
    }

    @Test
    void render_expandedArrayOfScalars_onePerLine() {
        List<JsonValue> items = new ArrayList<>();
        items.add(new Str("a-very-long-item-string-aaaa"));
        items.add(new Str("a-very-long-item-string-bbbb"));
        items.add(new Str("a-very-long-item-string-cccc"));
        String out = CodeDataPrinter.render(new Arr(items));
        assertTrue(out.startsWith("["));
        assertTrue(out.endsWith("]"));
        assertTrue(out.contains("a-very-long-item-string-aaaa,"));
        assertTrue(out.contains("a-very-long-item-string-cccc"));
        assertFalse(out.contains("- "), "标量数组不再用 - 列表项");
    }

    @Test
    void render_expandedArrayOfObjects_chainedCompact() {
        // 对象数组：[{ 开头、}] 结尾，对象之间 }, 换行后 {。字段足够长使每个对象自身展开（多行项→链式）
        String longA = "a".repeat(80);
        String longB = "b".repeat(80);
        JsonValue v = new Arr(List.of(
                obj(member("name", new Str(longA))),
                obj(member("name", new Str(longB)))));
        String out = CodeDataPrinter.render(v);
        assertTrue(out.contains("\n"), "应展开为多行");
        assertTrue(out.contains("[{"), "应以 [{ 开头");
        assertTrue(out.endsWith("}]"), "应以 }] 结尾");
        assertFalse(out.contains("}, {"), "对象之间不应 }, { 同行");
    }

    @Test
    void render_interfaceAsTypePrefixedBlock() {
        // 接口多态值：impl 非空，渲染为 TypeName{...}
        JsonValue v = obj("ByLevel", member("init", of(10)));
        String out = CodeDataPrinter.render(v);
        assertTrue(out.contains("ByLevel{"), "接口应渲染为 TypeName{...}");
        assertTrue(out.contains("init: 10"));
    }

    @Test
    void render_nestedBeanUnderField() {
        // posList: [ {x:1, y:2} ] —— list 下挂 obj，验证缩进与展开
        JsonValue v = obj(member("id", of(5)),
                member("pos", new Arr(List.of(obj(member("x", of(1)), member("y", of(2)))))));
        String out = CodeDataPrinter.render(v);
        assertTrue(out.contains("id: 5"));
        assertTrue(out.contains("pos:"));
        assertTrue(out.contains("x: 1"));
    }

    // ---------- helper ----------

    private static JsonValue memberValue(JsonValue v, String name) {
        Obj o = (Obj) v;
        for (Member m : o.members()) {
            if (m.name().equals(name)) {
                return m.value();
            }
        }
        return null;
    }
}
