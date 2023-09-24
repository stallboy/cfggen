package configgen.data;

import configgen.Logger;
import configgen.util.*;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.checkerframework.checker.units.qual.A;
import org.dhatim.fastexcel.reader.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static configgen.util.EFileFormat.CSV;
import static configgen.util.EFileFormat.EXCEL;

public class DataReader {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Logger.enableVerbose();
        readByFast_multiThread();
//        readByFastExcel();
//        readByPOI();

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

        void merge(Stat s) {
            nullCount += s.nullCount;
            csvCount += s.csvCount;
            excelCount += s.excelCount;
            sheetCount += s.sheetCount;

            for (Map.Entry<CellType, Integer> e : s.cellTypeCountMap.entrySet()) {
                CellType t = e.getKey();
                int old = cellTypeCountMap.getOrDefault(t, 0);
                cellTypeCountMap.put(t, old + e.getValue());
            }
        }

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
//                    readCsvByFastCsv(path);
                } else if (format == EXCEL) {
                    readExcelByFastExcel(path);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        stat.print();
        Logger.mm("end fast read");
    }

    private static void readByFast_multiThread() throws IOException, InterruptedException, ExecutionException {

        Logger.mm("start multithread fast read");
        Stat stat = new Stat();
        List<Callable<Result>> tasks = new ArrayList<>();

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
//                    CSV.readFromFile(path.toFile(), "GBK");
                    tasks.add(() -> readCsvByFastCsv(path));
                } else if (format == EXCEL) {
                    tasks.add(() -> readExcelByFastExcel(path));
                }
                return FileVisitResult.CONTINUE;
            }
        });

//        try(ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            List<Future<Result>> futures = executor.invokeAll(tasks);
            for (Future<Result> future : futures) {
                Result result = future.get();
                if (!result.isCsv) {
                    stat.merge(result.excelStat);
                }
            }
        }

        stat.print();
        Logger.mm("end multithread fast read");
    }

    static class Result {
        boolean isCsv;
        List<List<String>> csvRes;

        List<OneSheetResult> excelRes;
        Stat excelStat;

    }

    static class OneSheetResult {
        String sheetName;
        List<Row> rows;

        public OneSheetResult(String sheetName, List<Row> rows) {
            this.sheetName = sheetName;
            this.rows = rows;
        }
    }


    private static Result readCsvByFastCsv(Path path) throws IOException {
        int count = 0;
        List<List<String>> res = new ArrayList<>();
        try (CsvReader reader = CsvReader.builder().build(path, Charset.forName("GBK"))) {

            int cnt = 0;
            for (CsvRow csvRow : reader) {
                if (count == 0) {
                    count = csvRow.getFieldCount();
                } else if (count != csvRow.getFieldCount()) {
                    System.out.println(STR. "\{ path } \{ csvRow.getOriginalLineNumber() } count \{ csvRow.getFieldCount() } not eq \{ count }" );
                }
                res.add(csvRow.getFields());
                cnt++;

                if (cnt == 2) { //header only
                    break;
                }
            }


        }

        Result result = new Result();
        result.isCsv = true;
        result.csvRes = res;

        return result;
    }

    private static Result readExcelByFastExcel(Path path) throws IOException {
        Stat stat = new Stat();

        Result result = new Result();
        result.excelRes = new ArrayList<>();
        result.excelStat = stat;

        stat.excelCount++;
        try (ReadableWorkbook wb = new ReadableWorkbook(path.toFile())) {
            for (Sheet sheet : wb.getSheets().toList()) {
                String sheetName = sheet.getName().trim();

                String codeName = FileNameExtract.extractFileName(sheetName);
                if (codeName == null) {
                    continue;
                }
                stat.sheetCount++;

                try (Stream<Row> stream = sheet.openStream()) {
                    Iterator<Row> it = stream.iterator();
                    Row first = it.next();
                    Row second = it.next();
                }

                /*
                boolean hasFormula = false;
                List<Row> rows = sheet.read();
                result.excelRes.add(new OneSheetResult(sheetName, rows));

                for (Row row : rows) {
                    for (Cell cell : row) {
                        if (cell != null) {
                            CellType type = cell.getType();
                            int old = stat.cellTypeCountMap.getOrDefault(type, 0);
                            stat.cellTypeCountMap.put(type, old + 1);

                            if (type == CellType.FORMULA) {
                                hasFormula = true;
//                                System.out.println(cell.getAddress() + ": " + cell.getFormula() + " " + cell);
                            }
                        } else {
                            stat.nullCount++;
                        }
                    }
                }

                if (hasFormula) {
                    System.out.println(path + "/" + sheetName);
                }
                */
            }
        }
        return result;
    }


}


