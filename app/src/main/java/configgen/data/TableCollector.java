package configgen.data;

import configgen.ctx.DirectoryStructure;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TableCollector {

    public static Set<String> collect(DirectoryStructure sourceStructure) {
        Set<String> tableSet = new HashSet<>();
        for (DirectoryStructure.ExcelFileInfo df : sourceStructure.getExcelFiles()) {
            switch (df.fmt()) {
                case CSV, TXT_AS_TSV -> {
                    DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(df.relativePath());
                    if (ti != null) {
                        tableSet.add(ti.tableName());
                    }
                }
                case EXCEL -> {
                    // use ReadByFastExcel
                    try (ReadableWorkbook wb = new ReadableWorkbook(df.path().toFile(),
                            new ReadingOptions(true, false))) {
                        for (Sheet sheet : wb.getSheets().toList()) {
                            String sheetName = sheet.getName().trim();
                            DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(df.relativePath(), sheetName);
                            if (ti != null) {
                                tableSet.add(ti.tableName());
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return tableSet;
    }

}
