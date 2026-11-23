package configgen.layout;

import configgen.layout.TableFileName.TableNameIndex;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static configgen.layout.FileFormat.getFileFormat;
import static configgen.layout.TableFileName.getTableNameIndex;
import static org.junit.jupiter.api.Assertions.*;

class LayoutNamingTest {

    @Test
    public void getCodeName_IgnoreFirstChineseCharAndAfter() {
        String e = FileNameUtil.getCodeName("test中文.xlsx");
        assertEquals("test", e);
    }

    @Test
    public void getCodeName_AlsoIgnoreFirstUnderlineBeforeChineseChar() {
        String e = FileNameUtil.getCodeName("test_中文.xlsx");
        assertEquals("test", e);

        e = FileNameUtil.getCodeName("test__中文.xlsx");
        assertEquals("test_", e);
    }


    @Test
    public void getCodeName_MustPrefixA_Z_or_a_z() {
        String e = FileNameUtil.getCodeName("_test_中文.xlsx");
        assertNull(e);

        e = FileNameUtil.getCodeName("中_test_中文.xlsx");
        assertNull(e);

        e = FileNameUtil.getCodeName("Test_中文.xlsx");
        assertEquals("test", e);
    }

    @Test
    public void getCodeName_Can_1Or2_ThenChineseChar() {
        String e = FileNameUtil.getCodeName("test_1_中文.xlsx");
        assertEquals("test_1", e);

        e = FileNameUtil.getCodeName("test_2中文.xlsx");
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
        assertEquals(FileFormat.EXCEL, getFileFormat(Path.of("../n/m/item.xlsx")));
        assertEquals(FileFormat.EXCEL, getFileFormat(Path.of("aa.xls")));

        assertEquals(FileFormat.CSV, getFileFormat(Path.of("../n/m/item.csv")));

        assertNull(getFileFormat(Path.of("aa.txt")));
    }

    @Test
    void getFileFormat_null() {
        assertNull(getFileFormat(Path.of("aa.bin")));
    }

    // --- tableNameForDirectory ---

    @Test
    void tableNameForDirectory_normal() {
        assertEquals("buff", JsonLayout.tableNameForDirectory("", "_buff"));
        assertEquals("skill", JsonLayout.tableNameForDirectory("", "_skill"));
        assertEquals("instancelogic", JsonLayout.tableNameForDirectory("", "_instancelogic"));
        assertEquals("skill.buff.over_time", JsonLayout.tableNameForDirectory("skill.buff.", "_over_time"));
        assertEquals("skill.buff.over.time", JsonLayout.tableNameForDirectory("", "_skill_buff_over_time"));
    }

    @Test
    void tableNameForDirectory_null_whenNoUnderscorePrefix() {
        assertNull(JsonLayout.tableNameForDirectory("", "buff"));
        assertNull(JsonLayout.tableNameForDirectory("", "skill"));
    }

    @Test
    void tableNameForDirectory_null_whenEmptyAfterUnderscore() {
        assertNull(JsonLayout.tableNameForDirectory("", "_"));
    }

    @Test
    void tableNameForDirectory_null_whenNotStartWithAz() {
        assertNull(JsonLayout.tableNameForDirectory("", "_1buff"));
        assertNull(JsonLayout.tableNameForDirectory("", "_中name"));
    }

    @Test
    void tableNameForDirectory_null_whenContainsChinese() {
        assertNull(JsonLayout.tableNameForDirectory("", "_buff技能"));
    }

}
