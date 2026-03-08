package configgen.tool;

import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaToCsvHeaderTest {

    @Test
    void testPrimitiveField() {
        // id:int -> ["id"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id"), header.getNameRow());
    }

    @Test
    void testStructField() {
        // struct Range { rmin:int; rmax:int; }
        // range:Range -> ["range._rmin", "_rmax"]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct Range { rmin:int; rmax:int; }
                table t[id] { id:int; range:Range; }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "range._rmin", "_rmax"), header.getNameRow());
    }

    @Test
    void testNestedStruct() {
        // struct B { y:int; z:int; }
        // struct A { x:int; b:B; }
        // a:A -> ["a._x", "_y", "_z"]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct B { y:int; z:int; }
                struct A { x:int; b:B; }
                table t[id] { id:int; a:A; }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "a._x", "_y", "_z"), header.getNameRow());
    }

    @Test
    void testListPrimitive() {
        // items:list<int> (fix=3) -> ["items._1", "_2", "_3"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; items:list<int> (fix=3); }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "items._1", "_2", "_3"), header.getNameRow());
    }

    @Test
    void testListStruct() {
        // struct Range { rmin:int; rmax:int; }
        // ranges:list<Range> (fix=2) -> ["ranges._rmin", "_rmax", "_rmin", "_rmax"]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct Range { rmin:int; rmax:int; }
                table t[id] { id:int; ranges:list<Range> (fix=2); }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "ranges._rmin", "_rmax", "_rmin", "_rmax"), header.getNameRow());
    }

    @Test
    void testMapPrimitive() {
        // attrs:map<int,int> (fix=2) -> ["attrs._k1", "_v1", "_k2", "_v2"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; attrs:map<int,int> (fix=2); }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "attrs._k1", "_v1", "_k2", "_v2"), header.getNameRow());
    }

    @Test
    void testInterface() {
        // interface Reward { struct Item { itemId:int; count:int; } struct Gold { gold:int; } }
        // reward:Reward -> ["reward._type", "_p1", "_p2"]
        CfgSchema cfgSchema = CfgReader.parse("""
                interface Reward {
                    struct Item { itemId:int; count:int; }
                    struct Gold { gold:int; }
                }
                table t[id] { id:int; reward:Reward; }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "reward._type", "_p1", "_p2"), header.getNameRow());
    }

    // ========== Comment 测试 ==========

    @Test
    void testCommentPrimitive() {
        // id:int // 主键 -> comment: ["主键"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; // 主键\n }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("主键"), header.getCommentRow());
    }

    @Test
    void testCommentStruct() {
        // struct Range { rmin:int; rmax:int; }
        // range:Range // 范围 -> comment: ["", "范围", ""]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct Range { rmin:int; rmax:int; }
                table t[id] { id:int; range:Range; // 范围\n }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "范围", ""), header.getCommentRow());
    }

    @Test
    void testCommentList() {
        // items:list<int> (fix=3) // 物品列表 -> comment: ["", "物品列表", "", ""]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; items:list<int> (fix=3); // 物品列表\n }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "物品列表", "", ""), header.getCommentRow());
    }

    @Test
    void testCommentMap() {
        // attrs:map<int,int> (fix=2) // 属性映射 -> comment: ["", "属性映射", "", "", ""]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; attrs:map<int,int> (fix=2); // 属性映射\n }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "属性映射", "", "", ""), header.getCommentRow());
    }

    @Test
    void testCommentInterface() {
        // interface Reward { ... }
        // reward:Reward // 奖励 -> comment: ["", "奖励", "", ""]
        CfgSchema cfgSchema = CfgReader.parse("""
                interface Reward {
                    struct Item { itemId:int; count:int; }
                    struct Gold { gold:int; }
                }
                table t[id] { id:int; reward:Reward; // 奖励\n }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("", "奖励", "", ""), header.getCommentRow());
    }

    @Test
    void testMapStruct() {
        // struct Range { rmin:int; rmax:int; }
        // ranges:map<int,Range> (fix=2) -> ["ranges._k1", "_rmin", "_rmax", "_k2", "_rmin", "_rmax"]
        CfgSchema cfgSchema = CfgReader.parse("""
                struct Range { rmin:int; rmax:int; }
                table t[id] { id:int; ranges:map<int,Range> (fix=2); }
                """);
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "ranges._k1", "_rmin", "_rmax", "_k2", "_rmin", "_rmax"), header.getNameRow());
    }

    @Test
    void testListBlock() {
        // items:list<int> (block=3) -> ["items._1", "_2", "_3"]
        CfgSchema cfgSchema = CfgReader.parse("table t[id] { id:int; items:list<int> (block=3); }");
        CfgSchemaErrs errs = cfgSchema.resolve();
        assertEquals(0, errs.errs().size());
        TableSchema table = cfgSchema.findTable("t");

        SchemaToCsvHeader header = new SchemaToCsvHeader();
        header.flattenFields(table.fields());

        assertEquals(List.of("id", "items._1", "_2", "_3"), header.getNameRow());
    }
}
