package configgen.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ReadByFastExcelTest {

    @Test
    void readExcels(@TempDir Path tempDir) {
        String fn = "ai行为.xlsx";
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(fn)){
            Path tmp = tempDir.resolve(fn);
            Files.copy(Objects.requireNonNull(is), tmp);
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



        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }
}