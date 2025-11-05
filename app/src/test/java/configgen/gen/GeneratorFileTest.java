package configgen.gen;

import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorFileTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void shouldCreateUtf8Writer_whenFileProvided() throws Exception {
        // Given: 测试文件路径
        Path testFile = tempDir.resolve("test.txt");

        // When: 创建文件写入器
        try (var writer = Generator.createUtf8Writer(testFile.toFile())) {
            writer.write("test content");
        }

        // Then: 验证文件创建和内容
        assertTrue(Files.exists(testFile));
        String content = Files.readString(testFile);
        assertEquals("test content", content);
    }

    @Test
    void shouldNotOverwriteFile_whenFileAlreadyExists() throws Exception {
        // Given: 目标文件已存在
        Path targetFile = tempDir.resolve("existing.txt");
        Files.writeString(targetFile, "existing content");

        // When: 尝试复制支持文件
        Generator.copySupportFileIfNotExist("existing.txt", tempDir, "UTF-8");

        // Then: 验证文件内容未被覆盖
        String content = Files.readString(targetFile);
        assertEquals("existing content", content);
    }
}