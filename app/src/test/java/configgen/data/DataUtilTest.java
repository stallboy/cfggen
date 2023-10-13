package configgen.data;

import org.junit.jupiter.api.Test;

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
        String e =DataUtil.getCodeName("_test_中文.xlsx");
        assertNull(e);

        e =DataUtil.getCodeName("中_test_中文.xlsx");
        assertNull(e);

        e =DataUtil.getCodeName("Test_中文.xlsx");
        assertEquals(e, "test");
    }

    @Test
    public void getCodeName_Can_1Or2_ThenChineseChar() {
        String e =DataUtil.getCodeName("test_1_中文.xlsx");
        assertEquals(e, "test_1");

        e =DataUtil.getCodeName("test_2中文.xlsx");
        assertEquals(e, "test_2");
    }

}