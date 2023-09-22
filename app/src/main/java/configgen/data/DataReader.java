package configgen.data;

import configgen.Logger;
import configgen.util.EFileFormat;
import configgen.util.FileNameExtract;
import configgen.util.SheetUtils;
import org.dhatim.fastexcel.reader.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static configgen.util.EFileFormat.CSV;
import static configgen.util.EFileFormat.EXCEL;

public class DataReader {

    public static void main(String[] args) throws IOException {
        Logger.enableVerbose();
        readByFastExcel();
        readByPOI();

    }

    private static void read() throws IOException {
        try (ReadableWorkbook wb = new ReadableWorkbook(new File("activity/ActivityOpen.xlsx"))) {
            Sheet sheet = wb.getFirstSheet();
            List<Row> rows = sheet.read();
            for (Row row : rows) {
                System.out.println(row);
            }
        }
    }

    private static void readByPOI() throws IOException {
        Logger.mm("start poi read");
        Files.walkFileTree(Path.of("."), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes a) {

                if (path.toFile().isHidden()) {
                    return FileVisitResult.CONTINUE;
                }

                if (path.getFileName().toString().startsWith("~")) {
                    return FileVisitResult.CONTINUE;
                }

                SheetUtils.readFromFile(path.toFile(), "GBK");
                return FileVisitResult.CONTINUE;
            }
        });

        Logger.mm("end poi read");
    }

    static class Stat {
        int nullCount;
        int csvCount;
        int excelCount;
        int sheetCount;
        Map<CellType, Integer> cellTypeCountMap = new HashMap<>();

        void print() {
            for (Map.Entry<CellType, Integer> entry : cellTypeCountMap.entrySet()) {
                System.out.println(STR. "\{ entry.getKey().toString() }  \{ entry.getValue() }" );
            }
            System.out.println(STR. "null  \{ nullCount }" );
            System.out.println(STR. "csv   \{ csvCount }" );
            System.out.println(STR. "excel \{ excelCount }" );
            System.out.println(STR. "sheet \{ sheetCount }" );
        }
    }


    private static void readByFastExcel() throws IOException {

        Logger.mm("start fast read");
        Stat stat = new Stat();
        Files.walkFileTree(Path.of("."), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes a) throws IOException {

                if (path.toFile().isHidden()) {
                    return FileVisitResult.CONTINUE;
                }

                if (path.getFileName().toString().startsWith("~")) {
                    return FileVisitResult.CONTINUE;
                }

                EFileFormat format = SheetUtils.getFileFormat(path.toFile());
                if (format == CSV) {
                    stat.csvCount++;
                    CSV.readFromFile(path.toFile(), "GBK");
                } else if (format == EXCEL) {
                    stat.excelCount++;
                    try (ReadableWorkbook wb = new ReadableWorkbook(path.toFile())) {
                        for (Sheet sheet : wb.getSheets().toList()) {
                            String sheetName = sheet.getName().trim();

                            String codeName = FileNameExtract.extractFileName(sheetName);
                            if (codeName == null) {
                                continue;
                            }
                            stat.sheetCount++;
                            List<Row> rows = sheet.read();
                            for (Row row : rows) {
                                boolean hasFormula = false;
                                for (Cell cell : row) {
                                    if (cell != null) {
                                        CellType type = cell.getType();
                                        int old = stat.cellTypeCountMap.getOrDefault(type, 0);
                                        stat.cellTypeCountMap.put(type, old + 1);

                                        if (type == CellType.FORMULA){
                                            hasFormula = true;
                                            System.out.println(cell.getFormula() + ":  " +  cell);
                                        }
                                    } else {
                                        stat.nullCount++;
                                    }
                                }
                                if (hasFormula){
//                                    System.out.println(row);
                                }
                            }
                        }

                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });

        stat.print();
        Logger.mm("end fast read");
    }


}


