package configgen.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CSVUtilTest {

    @Test
    void replacementRoundTripsCsvCharactersAndPreservesOldFileOnFailure(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("data.csv");
        Files.writeString(target, "old content");
        var row = List.of("a\rb", "a\nb", "a\r\nb", "a,b", "a\"b");
        CSVUtil.writeToFile(target.toFile(), List.of(row));
        assertEquals(row, CSVUtil.read(target).getFirst().getFields());
        byte[] expected = Files.readAllBytes(target);

        assertThrows(IllegalArgumentException.class, () ->
                CSVUtil.writeToFile(target.toFile(), List.of(row, List.of("wrong width"))));
        assertArrayEquals(expected, Files.readAllBytes(target));
        try (var paths = Files.list(dir)) {
            assertEquals(List.of(target), paths.toList());
        }
    }

    @Test
    void testEscapeCsv() {
        // 测试空值和空字符串
        assertEquals("", CSVUtil.escapeCsv(null));
        assertEquals("", CSVUtil.escapeCsv(""));

        // 测试普通字符串（不需要转义）
        assertEquals("hello", CSVUtil.escapeCsv("hello"));
        assertEquals("测试", CSVUtil.escapeCsv("测试"));

        // 测试包含逗号的字符串
        assertEquals("\"hello,world\"", CSVUtil.escapeCsv("hello,world"));
        assertEquals("\"测试,逗号\"", CSVUtil.escapeCsv("测试,逗号"));

        // 测试包含双引号的字符串
        assertEquals("\"hello\"\"world\"", CSVUtil.escapeCsv("hello\"world"));
        assertEquals("\"测试\"\"引号\"", CSVUtil.escapeCsv("测试\"引号"));

        // 测试同时包含逗号和引号的字符串
        assertEquals("\"hello,\"\"world\"\"\"", CSVUtil.escapeCsv("hello,\"world\""));
    }
}
