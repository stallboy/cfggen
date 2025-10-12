package configgen.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.FakeRows.*;
import static org.junit.jupiter.api.Assertions.*;

class CellParserTest {

    @Test
    void parse_ignoreSignLine() {
        List<FakeRows.FakeRow> fakeRows = getFakeRows();
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("t1.csv", "t1", 0,
                new ArrayList<>(fakeRows), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("t1", List.of(sheet));

        CfgDataStat ds = new CfgDataStat();
        HeadParser.parse(dt, ds); // 必须先解析表头

        CellParser.parse(dt, ds, 2);
        assertEquals(2, dt.rows().size());
        {
            List<CfgData.DCell> r1 = dt.rows().getFirst();
            assertEquals("1", r1.get(0).value());
            assertEquals(0, r1.get(0).col());
            assertEquals("note1", r1.get(1).value());
            assertEquals(1, r1.get(1).col());
        }

        {
            List<CfgData.DCell> r2 = dt.rows().get(1);
            assertEquals("2", r2.get(0).value());
            assertEquals(0, r2.get(0).col());
            assertEquals("note2", r2.get(1).value());
            assertEquals(1, r2.get(1).col());
        }
        assertEquals(1, ds.ignoredRowCount);
        assertEquals(2, ds.rowCount);
    }

    @Test
    void parse_columnMode() {
        List<FakeRows.FakeRow> fakeRows = getFakeColumnRows();
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("t1.csv", "t1", 0,
                new ArrayList<>(fakeRows), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("t1", List.of(sheet));

        CfgDataStat ds = new CfgDataStat();
        HeadParser.parse(dt, ds, 2, true); // 必须先解析表头

        CellParser.parse(dt, ds, 2, true);
        assertEquals(2, dt.rows().size());
        {
            List<CfgData.DCell> r1 = dt.rows().getFirst();
            assertEquals("1", r1.get(0).value());
            assertEquals(0, r1.get(0).col());
            assertEquals("note1", r1.get(1).value());
            assertEquals(2, r1.get(1).col());
        }

        {
            List<CfgData.DCell> r2 = dt.rows().get(1);
            assertEquals("2", r2.get(0).value());
            assertEquals(0, r2.get(0).col());
            assertEquals("note2", r2.get(1).value());
            assertEquals(2, r2.get(1).col());
        }
        assertEquals(1, ds.ignoredRowCount);
        assertEquals(2, ds.rowCount);
    }


    @Test
    void parse_multiSheet() {
        CfgData.DRawSheet sheet1 = new CfgData.DRawSheet("t1.csv", "t1", 0,
                new ArrayList<>(getFakeRows()), new ArrayList<>());
        CfgData.DRawSheet sheet2 = new CfgData.DRawSheet("t1.csv", "t2", 1,
                new ArrayList<>(getFakeRows2()), new ArrayList<>());

        CfgData.DTable dt = CfgData.DTable.of("t1", new ArrayList<>(List.of(sheet2, sheet1)));
        CfgDataStat ds = new CfgDataStat();
        HeadParser.parse(dt, ds); // 必须先解析表头

        CellParser.parse(dt, ds, 2);
        assertEquals(4, dt.rows().size());
        {
            List<CfgData.DCell> r1 = dt.rows().getFirst();
            assertEquals("1", r1.get(0).value());
            assertEquals(0, r1.get(0).col());
            assertEquals("note1", r1.get(1).value());
            assertEquals(1, r1.get(1).col());
        }

        {
            List<CfgData.DCell> r2 = dt.rows().get(1);
            assertEquals("2", r2.get(0).value());
            assertEquals(0, r2.get(0).col());
            assertEquals("note2", r2.get(1).value());
            assertEquals(1, r2.get(1).col());
        }
        {
            List<CfgData.DCell> r3 = dt.rows().get(2);
            assertEquals("3", r3.get(0).value());
            assertEquals(0, r3.get(0).col());
            assertEquals("note3", r3.get(1).value());
            assertEquals(1, r3.get(1).col());
        }

        {
            List<CfgData.DCell> r4 = dt.rows().get(3);
            assertEquals("4", r4.get(0).value());
            assertEquals(0, r4.get(0).col());
            assertEquals("note4", r4.get(1).value());
            assertEquals(1, r4.get(1).col());
        }

        assertEquals(4, ds.rowCount);
    }


}