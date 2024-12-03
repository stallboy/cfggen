package configgen.data;

import configgen.data.DataUtil.TableNameIndex;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static configgen.data.DataUtil.getFileFormat;
import static configgen.data.DataUtil.getTableNameIndex;
import static org.junit.jupiter.api.Assertions.*;

class DataUtilTest {

    @Test
    public void getCodeName_IgnoreFirstChineseCharAndAfter() {
        String e = DataUtil.getCodeName("test中文.xlsx");
        assertEquals("test", e);
    }

    @Test
    public void getCodeName_AlsoIgnoreFirstUnderlineBeforeChineseChar() {
        String e = DataUtil.getCodeName("test_中文.xlsx");
        assertEquals("test", e);

        e = DataUtil.getCodeName("test__中文.xlsx");
        assertEquals("test_", e);
    }


    @Test
    public void getCodeName_MustPrefixA_Z_or_a_z() {
        String e = DataUtil.getCodeName("_test_中文.xlsx");
        assertNull(e);

        e = DataUtil.getCodeName("中_test_中文.xlsx");
        assertNull(e);

        e = DataUtil.getCodeName("Test_中文.xlsx");
        assertEquals("test", e);
    }

    @Test
    public void getCodeName_Can_1Or2_ThenChineseChar() {
        String e = DataUtil.getCodeName("test_1_中文.xlsx");
        assertEquals("test_1", e);

        e = DataUtil.getCodeName("test_2中文.xlsx");
        assertEquals("test_2", e);
    }

    @Test
    void getTableNameIndex_normal() {
        assertEquals(new TableNameIndex("module1.table1", 0), getTableNameIndex(Path.of("module1/table1.xlsx")));
        assertEquals(new TableNameIndex("table1", 0), getTableNameIndex(Path.of("table1.xlsx")));
        assertEquals(new TableNameIndex("table1", 0), getTableNameIndex(Path.of("table1物品.xlsx")));
        assertEquals(new TableNameIndex("m.table1", 0), getTableNameIndex(Path.of("m/table1_物品.xlsx")));
    }

    @Test
    void getTableNameIndex_withIndex() {
        assertEquals(new TableNameIndex("table1", 0), getTableNameIndex(Path.of("table1_0.xlsx")));
        assertEquals(new TableNameIndex("table1", 1), getTableNameIndex(Path.of("table1_1.xlsx")));
        assertEquals(new TableNameIndex("m.n.table1", 8), getTableNameIndex(Path.of("m/n/table1_8中文.xlsx")));
        assertEquals(new TableNameIndex("m.n.table1", 8), getTableNameIndex(Path.of("m/n/table1_8_中文.xlsx")));
    }

    @Test
    void getTableNameIndex_null() {
        assertNull(getTableNameIndex(Path.of("m/物品item.xlsx")));
        assertNull(getTableNameIndex(Path.of("m/_item.xlsx")));
    }

    @Test
    void getTableNameIndexWithSheet_normal() {
        assertEquals(new TableNameIndex("item", 2), getTableNameIndex(Path.of("items.xlsx"), "item_2"));
        assertEquals(new TableNameIndex("m.item", 0), getTableNameIndex(Path.of("m/物品item.xlsx"), "item"));
    }

    @Test
    void getTableNameIndexWithSheet_null() {
        assertNull(getTableNameIndex(Path.of("items.xlsx"), "物品"));
        assertNull(getTableNameIndex(Path.of("items.xlsx"), "_item"));
    }

    @Test
    void getFileFormat_normal() {
        assertEquals(DataUtil.FileFmt.EXCEL, getFileFormat(Path.of("../n/m/item.xlsx")));
        assertEquals(DataUtil.FileFmt.EXCEL, getFileFormat(Path.of("aa.xls")));

        assertEquals(DataUtil.FileFmt.CSV, getFileFormat(Path.of("../n/m/item.csv")));
    }

    @Test
    void getFileFormat_null() {
        assertNull(getFileFormat(Path.of("aa.txt")));
    }

}
