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
        assertEquals(e, "test");
    }

    @Test
    public void getCodeName_AlsoIgnoreFirstUnderlineBeforeChineseChar() {
        String e = DataUtil.getCodeName("test_中文.xlsx");
        assertEquals(e, "test");

        e = DataUtil.getCodeName("test__中文.xlsx");
        assertEquals(e, "test_");
    }


    @Test
    public void getCodeName_MustPrefixA_Z_or_a_z() {
        String e = DataUtil.getCodeName("_test_中文.xlsx");
        assertNull(e);

        e = DataUtil.getCodeName("中_test_中文.xlsx");
        assertNull(e);

        e = DataUtil.getCodeName("Test_中文.xlsx");
        assertEquals(e, "test");
    }

    @Test
    public void getCodeName_Can_1Or2_ThenChineseChar() {
        String e = DataUtil.getCodeName("test_1_中文.xlsx");
        assertEquals(e, "test_1");

        e = DataUtil.getCodeName("test_2中文.xlsx");
        assertEquals(e, "test_2");
    }

    @Test
    void getTableNameIndex_normal() {
        assertEquals(getTableNameIndex(Path.of("module1/table1.xlsx")), new TableNameIndex("module1.table1", 0));
        assertEquals(getTableNameIndex(Path.of("table1.xlsx")), new TableNameIndex("table1", 0));
        assertEquals(getTableNameIndex(Path.of("table1物品.xlsx")), new TableNameIndex("table1", 0));
        assertEquals(getTableNameIndex(Path.of("m/table1_物品.xlsx")), new TableNameIndex("m.table1", 0));
    }

    @Test
    void getTableNameIndex_withIndex() {
        assertEquals(getTableNameIndex(Path.of("table1_0.xlsx")), new TableNameIndex("table1", 0));
        assertEquals(getTableNameIndex(Path.of("table1_1.xlsx")), new TableNameIndex("table1", 1));
        assertEquals(getTableNameIndex(Path.of("m/n/table1_8中文.xlsx")), new TableNameIndex("m.n.table1", 8));
        assertEquals(getTableNameIndex(Path.of("m/n/table1_8_中文.xlsx")), new TableNameIndex("m.n.table1", 8));
    }

    @Test
    void getTableNameIndex_null() {
        assertNull(getTableNameIndex(Path.of("m/物品item.xlsx")));
        assertNull(getTableNameIndex(Path.of("m/_item.xlsx")));
    }

    @Test
    void getTableNameIndexWithSheet_normal() {
        assertEquals(getTableNameIndex(Path.of("items.xlsx"), "item_2"), new TableNameIndex("item", 2));
        assertEquals(getTableNameIndex(Path.of("m/物品item.xlsx"), "item"), new TableNameIndex("m.item", 0));
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