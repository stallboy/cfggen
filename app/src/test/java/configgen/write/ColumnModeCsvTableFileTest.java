package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColumnModeCsvTableFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testReadWriteColumnMode() throws IOException {
        Path csvPath = tempDir.resolve("test_column.csv");
        // 列模式：数据完全按列存储
        // 逻辑层（RecordBlock）的数据：
        // 行0: ["1", "A"]
        // 行1: ["2", "B"]
        // 列模式物理存储（转置后）：
        // 列0: ["ID", "名字"]
        // 列1: ["id", "name"]
        // 列2: ["1", "2"]
        // 列3: ["A", "B"]
        String content = """
                ID,id,1,A
                名字,name,2,B
                """;
        Files.writeString(csvPath, content);

        ColumnModeCsvTableFile tableFile = new ColumnModeCsvTableFile(csvPath, "UTF-8", 2);

        // Test emptyRows - 在列模式下，emptyRows实际上是清空列
        // 清空第2列（从0开始），即清空数据列"1,2"
        tableFile.emptyRows(2, 1, null);

        // Save and verify
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);
        assertEquals(2, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        assertEquals("ID,id,,A", firstLine); // 第2列被清空
        assertEquals("名字,name,,B", lines.get(1)); // 第2列被清空
    }

    @Test
    void testInsertRecordBlockColumnMode() throws IOException {
        Path csvPath = tempDir.resolve("insert_column.csv");
        // 初始数据：
        // 逻辑层：行0: ["1", "A"]
        // 物理层（列模式）：
        // 列0: ["ID", "名字"]
        // 列1: ["id", "name"]
        // 列2: ["1", "A"]
        String content = """
                ID,id,1
                名字,name,A
                """;
        Files.writeString(csvPath, content);

        ColumnModeCsvTableFile tableFile = new ColumnModeCsvTableFile(csvPath, "UTF-8", 2);

        // 创建RecordBlock（逻辑层按行存储）
        // 要插入的数据：
        // 行0: ["2", "B"]
        // 行1: ["3", "C"]
        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        block.setCell(1, 0, "3");
        block.setCell(1, 1, "C");

        // Identity mapping
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at end (列模式：插入到最后一列)
        tableFile.insertRecordBlock(-1, 0, transformed);
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);
        assertEquals(2, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        // 验证转置后的结果：
        // 原始数据：列0: ["ID", "名字"], 列1: ["id", "name"], 列2: ["1", "A"]
        // 插入数据：列3: ["2", "B"], 列4: ["3", "C"]
        assertEquals("ID,id,1,2,3", firstLine);
        assertEquals("名字,name,A,B,C", lines.get(1));
    }


    @Test
    void testInsertWithShiftColumnMode() throws IOException {
        Path csvPath = tempDir.resolve("shift_column.csv");
        // 初始数据：
        // 列0: ["ID", "名字"]
        // 列1: ["id", "name"]
        // 列2: ["1", "A"]
        // 列3: ["3", "C"]
        String content = """
                ID,id,1,3
                名字,name,A,C
                """;
        Files.writeString(csvPath, content);

        ColumnModeCsvTableFile tableFile = new ColumnModeCsvTableFile(csvPath, "UTF-8", 2);

        // 要插入的数据：
        // 逻辑层：行0: ["2", "B"]
        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at column 2 (在列2的位置插入，即"1,A"列之前)
        tableFile.insertRecordBlock(2, 0, transformed);
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);
        assertEquals(2, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        // 验证插入和移动后的结果：
        // 原始：列0: ["ID", "名字"], 列1: ["id", "name"], 列2: ["1", "A"], 列3: ["3", "C"]
        // 插入后：列0: ["ID", "名字"], 列1: ["id", "name"], 列2: ["2", "B"], 列3: ["1", "A"], 列4: ["3", "C"]
        assertEquals("ID,id,2,1,3", firstLine);
        assertEquals("名字,name,B,A,C", lines.get(1));
    }

    @Test
    void testInsertMultipleColumnsColumnMode() throws IOException {
        Path csvPath = tempDir.resolve("multi_column.csv");
        // 初始数据：
        // 列0: ["ID", "名字"]
        // 列1: ["id", "name"]
        // 列2: ["1", "A"]
        // 列3: ["4", "D"]
        String content = """
                ID,id,1,4
                名字,name,A,D
                """;
        Files.writeString(csvPath, content);

        ColumnModeCsvTableFile tableFile = new ColumnModeCsvTableFile(csvPath, "UTF-8", 2);

        // 要插入的多列数据：
        // 逻辑层：
        // 行0: ["2", "B"]
        // 行1: ["3", "C"]
        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        block.setCell(1, 0, "3");
        block.setCell(1, 1, "C");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at column 2 (在列2的位置插入两列)
        tableFile.insertRecordBlock(2, 0, transformed);
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);
        assertEquals(2, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        // 验证多列插入后的结果：
        // 原始：列0: ["ID", "名字"], 列1: ["id", "name"], 列2: ["1", "A"], 列3: ["4", "D"]
        // 插入后：列0: ["ID", "名字"], 列1: ["id", "name"], 列2: ["2", "B"], 列3: ["3", "C"], 列4: ["1", "A"], 列5: ["4", "D"]
        assertEquals("ID,id,2,3,1,4", firstLine);
        assertEquals("名字,name,B,C,A,D", lines.get(1));
    }

    @Test
    void testEmptyRowsAndKeepCommentCellColumnMode() throws IOException {
        Path csvPath = tempDir.resolve("test_column_comment.csv");
        // 列模式：数据完全按列存储
        // 逻辑层（RecordBlock）的数据：
        // 行0: ["1", "注释1", "A"]
        // 行1: ["2", "注释2", "B"]
        // 列模式物理存储（转置后）：
        // 列0: ["ID", "注释", "名字"]
        // 列1: ["id", "", "name"]
        // 列2: ["1", "注释1", "A"]
        // 列3: ["2", "注释2", "B"]
        String content = """
                ID,id,1,2
                注释,,注释1,注释2
                名字,name,A,B
                """;
        Files.writeString(csvPath, content);

        ColumnModeCsvTableFile tableFile = new ColumnModeCsvTableFile(csvPath, "UTF-8", 2);

        // Test emptyRows with fieldIndices - 在列模式下，emptyRows实际上是清空列
        // 清空第2列（从0开始），即清空数据列"1,注释1,A"，但只清空第0列（id）和第2列（名字），保留注释列（索引1）
        tableFile.emptyRows(2, 1, List.of(0, 2));

        // Save and verify
        tableFile.saveAndClose();

        List<String> lines = Files.readAllLines(csvPath);
        assertEquals(3, lines.size());
        // Handle potential BOM in first line
        String firstLine = lines.get(0);
        if (firstLine.startsWith("\uFEFF")) {
            firstLine = firstLine.substring(1);
        }
        // 验证：第2列的第0列（id）和第2列（名字）被清空，第1列（注释）保持不变
        // 原始：列2: ["1", "注释1", "A"]
        // 清空后：列2: ["", "注释1", ""]
        assertEquals("ID,id,,2", firstLine); // 第2列的第0列被清空
        assertEquals("注释,,注释1,注释2", lines.get(1)); // 第2列的第1列保持不变
        assertEquals("名字,name,,B", lines.get(2)); // 第2列的第2列被清空
    }
}