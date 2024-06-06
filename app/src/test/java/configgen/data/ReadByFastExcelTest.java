package configgen.data;

import configgen.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReadByFastExcelTest {

    private @TempDir Path tempDir;

    @Test
    void readExcels() throws IOException {
        String fn = "ai行为.xlsx";
        Path tmp = Resources.addTempFileFromResourceFile(fn, tempDir);
        ExcelReader.AllResult allResult = ReadByFastExcel.INSTANCE.readExcels(tmp, Path.of(fn));

        assertEquals(4, allResult.sheets().size());

        {
            ExcelReader.OneSheetResult o = allResult.sheets().get(0);
            assertEquals("ai", o.tableName());
            CfgData.DRawSheet sheet = o.sheet();
            assertEquals(fn, sheet.fileName());
            assertEquals(54, sheet.rows().size());
        }

        {
            ExcelReader.OneSheetResult o = allResult.sheets().get(1);
            assertEquals("ai_condition", o.tableName());
            assertEquals(20, o.sheet().rows().size());
        }

        {
            ExcelReader.OneSheetResult o = allResult.sheets().get(2);
            assertEquals("ai_action", o.tableName());
            assertEquals(0, o.sheet().index());
            assertEquals(35, o.sheet().rows().size());
        }
        {
            ExcelReader.OneSheetResult o = allResult.sheets().get(3);
            assertEquals("ai_action", o.tableName());
            assertEquals(1, o.sheet().index());
            assertEquals(22, o.sheet().rows().size());
        }
    }
}