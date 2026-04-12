package configgen.write;

import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VTableJsonStorageTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void shouldUseNestedPathWhenModuleDirExists() throws IOException {
        // Given: 存在模块目录 buff
        Files.createDirectories(tempDir.resolve("buff"));

        // When
        Path result = VTableJsonStorage.resolveJsonDirRelativePath("buff.skill", tempDir);

        // Then: buff/_skill
        assertEquals(Path.of("buff").resolve("_skill"), result);
    }

    @Test
    void shouldUseNestedPathWhenModuleDirHasChineseSuffix() throws IOException {
        // Given: 模块目录名为 "skill_技能"（codeName = "skill"）
        Files.createDirectories(tempDir.resolve("skill_技能"));

        // When
        Path result = VTableJsonStorage.resolveJsonDirRelativePath("skill.buff", tempDir);

        // Then: "skill_技能/_buff"
        assertEquals(Path.of("skill_技能").resolve("_buff"), result);
    }

    @Test
    void shouldFallbackToRootLevelWhenModuleDirDoesNotExist() {
        // Given: 没有模块目录

        // When
        Path result = VTableJsonStorage.resolveJsonDirRelativePath("buff.skill", tempDir);

        // Then: 回退旧格式 _buff_skill
        assertEquals(Path.of("_buff_skill"), result);
    }

    @Test
    void shouldHandleDeeplyNestedPath() throws IOException {
        // Given: 多层模块目录 a/b
        Files.createDirectories(tempDir.resolve("a").resolve("b"));

        // When
        Path result = VTableJsonStorage.resolveJsonDirRelativePath("a.b.c", tempDir);

        // Then: a/b/_c
        assertEquals(Path.of("a").resolve("b").resolve("_c"), result);
    }

    @Test
    void shouldFallbackWhenPartialModuleChainMissing() throws IOException {
        // Given: 只有 a/ 目录，没有 a/b/
        Files.createDirectories(tempDir.resolve("a"));

        // When
        Path result = VTableJsonStorage.resolveJsonDirRelativePath("a.b.c", tempDir);

        // Then: 链不完整，回退 _a_b_c
        assertEquals(Path.of("_a_b_c"), result);
    }

    @Test
    void shouldHandleTableWithoutDot() {
        // Given: 表名没有点号

        // When
        Path result = VTableJsonStorage.resolveJsonDirRelativePath("simpletable", tempDir);

        // Then: 回退 _simpletable
        assertEquals(Path.of("_simpletable"), result);
    }

    @Test
    void shouldDeeplyNestedWithChineseSuffixDir() throws IOException {
        // Given: a/b_数据/ 目录结构（codeName 分别为 a, b）
        Files.createDirectories(tempDir.resolve("a").resolve("b_数据"));

        // When
        Path result = VTableJsonStorage.resolveJsonDirRelativePath("a.b.c", tempDir);

        // Then: a/b_数据/_c
        assertEquals(Path.of("a").resolve("b_数据").resolve("_c"), result);
    }
}
