package configgen.data;

import configgen.schema.CfgSchema;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.FakeRows.*;
import static org.junit.jupiter.api.Assertions.*;

class HeadParserTest {


    @Test
    void isColumnMode_falseIfNoCfgSchema() {
        assertFalse(HeadParser.isColumnMode(null, null));
        assertFalse(HeadParser.isColumnMode(new CfgData.DTable("tableName", List.of(), List.of(), List.of()), null));
    }

    @Test
    void isColumnMode_falseFromCfgSchemaMeta() {
        CfgData.DTable dt1 = new CfgData.DTable("t1", List.of(), List.of(), List.of());
        String str = """
                table t1[id] {
                    id:int;
                    note:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve();

        assertFalse(HeadParser.isColumnMode(dt1, cfg));
    }

    @Test
    void isColumnMode_trueFromCfgSchemaMeta() {
        CfgData.DTable dt1 = new CfgData.DTable("t1", List.of(), List.of(), List.of());
        String str = """
                table t1[id] (columnMode, entry='note'){
                    id:int;
                    note:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        assertTrue(HeadParser.isColumnMode(dt1, cfg));
    }

    @Test
    void parse_normal() {
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("t1.csv", "t1", 0,
                List.of(new FakeRows.FakeRow(new String[]{"id", "记录"}),
                        new FakeRows.FakeRow(new String[]{"id", "note"})
                ), new ArrayList<>());
        CfgData.DTable dt = new CfgData.DTable("t1", new ArrayList<>(), List.of(), List.of(sheet));
        CfgDataStat ds = new CfgDataStat();
        HeadParser.parse(dt, ds, null);

        assertEquals(2, dt.fields().size());
        CfgData.DField f1 = dt.fields().get(0);
        CfgData.DField f2 = dt.fields().get(1);
        assertEquals("id", f1.name());
        assertEquals("", f1.comment()); // 如果comment和name相同，忽略comment

        assertEquals("note", f2.name());
        assertEquals("记录", f2.comment());

        assertEquals(List.of(0, 1), sheet.fieldIndices());
        assertEquals(2, ds.columnCount);
        assertEquals(0, ds.ignoredColumnCount);
    }

    @Test
    void parse_ignoreOneColumn() {
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("t1.csv", "t1", 0,
                List.of(new FakeRows.FakeRow(new String[]{"id", "策划注释", "记录"}),
                        new FakeRows.FakeRow(new String[]{"id", "", "note"})
                ), new ArrayList<>());
        CfgData.DTable dt = new CfgData.DTable("t1", new ArrayList<>(), List.of(), List.of(sheet));
        CfgDataStat ds = new CfgDataStat();
        HeadParser.parse(dt, ds, null);

        assertEquals(2, dt.fields().size());
        CfgData.DField f1 = dt.fields().get(0);
        CfgData.DField f2 = dt.fields().get(1);
        assertEquals("id", f1.name());
        assertEquals("", f1.comment()); // 如果comment和name相同，忽略comment

        assertEquals("note", f2.name());
        assertEquals("记录", f2.comment());

        assertEquals(List.of(0, 2), sheet.fieldIndices());
        assertEquals(2, ds.columnCount);
        assertEquals(1, ds.ignoredColumnCount);
    }


    @Test
    void parse_ignoreColumnAndNameAfterComma() {
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("t1.csv", "t1", 0,
                List.of(new FakeRows.FakeRow(new String[]{"id", "策划注释", "记录"}),
                        new FakeRows.FakeRow(new String[]{"id", "", "note,willIgnored", "value@a"})
                ), new ArrayList<>());
        CfgData.DTable dt = new CfgData.DTable("t1", new ArrayList<>(), List.of(), List.of(sheet));
        CfgDataStat ds = new CfgDataStat();
        HeadParser.parse(dt, ds, null);

        assertEquals(3, dt.fields().size());
        CfgData.DField f1 = dt.fields().get(0);
        CfgData.DField f2 = dt.fields().get(1);
        CfgData.DField f3 = dt.fields().get(2);
        assertEquals("id", f1.name());
        assertEquals("", f1.comment()); // 如果comment和name相同，忽略comment

        assertEquals("note", f2.name());
        assertEquals("记录", f2.comment());
        assertEquals("value", f3.name());

        assertEquals(List.of(0, 2, 3), sheet.fieldIndices());
        assertEquals(3, ds.columnCount);
        assertEquals(1, ds.ignoredColumnCount);
    }

    @Test
    void parse_columnMode() {
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("t1.csv", "t1", 0,
                new ArrayList<>(getFakeColumnRows()), new ArrayList<>());
        CfgData.DTable dt = new CfgData.DTable("t1", new ArrayList<>(), List.of(), List.of(sheet));
        CfgDataStat ds = new CfgDataStat();

        HeadParser.parse(dt, ds, true);

        assertEquals(2, dt.fields().size());
        CfgData.DField f1 = dt.fields().get(0);
        CfgData.DField f2 = dt.fields().get(1);
        assertEquals("id", f1.name());
        assertEquals("", f1.comment()); // 如果comment和name相同，忽略comment

        assertEquals("note", f2.name());
        assertEquals("说明", f2.comment());

        assertEquals(List.of(0, 2), sheet.fieldIndices());
        assertEquals(2, ds.columnCount);
        assertEquals(1, ds.ignoredColumnCount);
    }


    @Test
    void parse_multiSheet_orderByIndex() {
        CfgData.DRawSheet sheet1 = new CfgData.DRawSheet("t1.csv", "t1", 0,
                new ArrayList<>(getFakeRows()), new ArrayList<>());
        CfgData.DRawSheet sheet2 = new CfgData.DRawSheet("t1.csv", "t2", 1,
                new ArrayList<>(getFakeRows2()), new ArrayList<>());

        CfgData.DTable dt = new CfgData.DTable("t1",
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(List.of(sheet2, sheet1)));
        CfgDataStat ds = new CfgDataStat();
        HeadParser.parse(dt, ds, null); // 必须先解析表头

        assertEquals(2, dt.rawSheets().size());
        assertEquals(sheet1, dt.rawSheets().get(0));
        assertEquals(sheet2, dt.rawSheets().get(1));

    }

}