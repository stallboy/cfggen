package configgen.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CfgDataReaderTest {

    static CfgData readFile(String fn, Path tempDir) {
        try (InputStream is = CfgDataReaderTest.class.getClassLoader().getResourceAsStream(fn)) {
            Path tmp = tempDir.resolve(fn);
            Files.copy(Objects.requireNonNull(is), tmp, StandardCopyOption.REPLACE_EXISTING);

            ReadCsv csvReader = new ReadCsv("GBK");
            CfgDataReader fastDataReader = new CfgDataReader(2, csvReader, ReadByFastExcel.INSTANCE);
            return fastDataReader.readCfgData(tempDir, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void readCsv(@TempDir Path tempDir) {
        String fn = "rank.csv";
        CfgData cfgData = readFile(fn, tempDir);

        assertEquals(1, cfgData.tables().size());
        assertEquals(1, cfgData.stat().tableCount);

        CfgData.DTable dt = cfgData.tables().get("rank");
        assertEquals("rank", dt.tableName());
        assertEquals(1, dt.rawSheets().size());

        CfgData.DRawSheet sheet = dt.rawSheets().getFirst();

        assertEquals("rank.csv", sheet.fileName());
        assertEquals("", sheet.sheetName());
        assertEquals(0, sheet.index());
        assertEquals(0, sheet.rows().size()); // 因为为省内存，删除中间对象

        assertEquals(3, dt.fields().size());
        assertEquals(5, dt.rows().size());

        {
            List<CfgData.DCell> row = dt.rows().get(1);
            assertEquals("2", row.get(0).value());
            assertEquals(0, row.get(0).col());

            assertEquals("green", row.get(1).value());
            assertEquals(1, row.get(1).col());

            assertEquals("中品", row.get(2).value()); // 忽略的列直接不读的
            assertEquals(3, row.get(2).col());
        }
    }


    @Test
    void readExcel(@TempDir Path tempDir) {
        String fn = "ai行为.xlsx";
        CfgData cfgData = readFile(fn, tempDir);

        assertEquals(3, cfgData.tables().size());
        assertEquals(3, cfgData.stat().tableCount);
        assertEquals(1, cfgData.stat().excelCount);
        assertEquals(4, cfgData.stat().sheetCount);

        {
            CfgData.DTable dt = cfgData.tables().get("ai");
            assertEquals("ai", dt.tableName());
            assertEquals(7, dt.fields().size());
            assertEquals(52, dt.rows().size());
        }
        {
            CfgData.DTable dt = cfgData.tables().get("ai_action");
            assertEquals("ai_action", dt.tableName());
            assertEquals(12, dt.fields().size());
            assertEquals(53, dt.rows().size());
        }
    }
}