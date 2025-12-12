package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvTableFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testReadWrite() throws IOException {
        Path csvPath = tempDir.resolve("test.csv");
        String content = """
                ID,名字
                id,name
                1,A
                2,B
                """;
        Files.writeString(csvPath, content);

        CsvTableFile tableFile = new CsvTableFile(csvPath, "UTF-8", 2);

        // Test emptyRows
        // Data starts at row 2. Row 2 is "1,A", Row 3 is "2,B"
        tableFile.emptyRows(3, 1, null);

        // Save and verify
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);
        assertEquals(4, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        assertEquals("ID,名字", firstLine);
        assertEquals("id,name", lines.get(1));
        assertEquals("1,A", lines.get(2));
        assertEquals(",", lines.get(3)); // Cleared row
    }

    @Test
    void testInsertRecordBlock() throws IOException {
        Path csvPath = tempDir.resolve("insert.csv");
        String content = """
                ID,名字
                id,name
                1,A
                """;
        Files.writeString(csvPath, content);

        CsvTableFile tableFile = new CsvTableFile(csvPath, "UTF-8", 2);

        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        block.setCell(1, 0, "3");
        block.setCell(1, 1, "C");

        // Identity mapping
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at end
        tableFile.insertRecordBlock(-1, 0, transformed);
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);
        assertEquals(5, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        assertEquals("ID,名字", firstLine);
        assertEquals("id,name", lines.get(1));
        assertEquals("1,A", lines.get(2));
        assertEquals("2,B", lines.get(3));
        assertEquals("3,C", lines.get(4));
    }

    @Test
    void testInsertWithShift() throws IOException {
        Path csvPath = tempDir.resolve("shift.csv");
        String content = """
                ID,名字
                id,name
                1,A
                3,C
                """;
        Files.writeString(csvPath, content);

        CsvTableFile tableFile = new CsvTableFile(csvPath, "UTF-8", 2);

        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at row 3 (between 1,A and 3,C)
        tableFile.insertRecordBlock(3, 0, transformed);
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);

        assertEquals(5, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        assertEquals("ID,名字", firstLine);
        assertEquals("id,name", lines.get(1));
        assertEquals("1,A", lines.get(2));
        assertEquals("2,B", lines.get(3));
        assertEquals("3,C", lines.get(4));
    }

    @Test
    void testInsertMultipleRowsWithShift() throws IOException {
        Path csvPath = tempDir.resolve("shift_multi.csv");
        String content = """
                ID,名字
                id,name
                1,A
                4,D
                """;
        Files.writeString(csvPath, content);

        CsvTableFile tableFile = new CsvTableFile(csvPath, "UTF-8", 2);

        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        block.setCell(1, 0, "3");
        block.setCell(1, 1, "C");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at row 3 (between 1,A and 4,D)
        tableFile.insertRecordBlock(3, 0, transformed);
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);

        assertEquals(6, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        assertEquals("ID,名字", firstLine);
        assertEquals("id,name", lines.get(1));
        assertEquals("1,A", lines.get(2));
        assertEquals("2,B", lines.get(3));
        assertEquals("3,C", lines.get(4));
        assertEquals("4,D", lines.get(5));
    }
}
