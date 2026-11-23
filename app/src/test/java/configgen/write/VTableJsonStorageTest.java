package configgen.write;

import configgen.layout.JsonLayout;
import com.sun.nio.file.ExtendedOpenOption;
import configgen.ctx.Context;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

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
    void editsAndDeletesRecordAtItsOriginalFilename() throws IOException {
        Files.writeString(tempDir.resolve("config.cfg"), "table t[id] (json) { id:int; score:int; }");
        Path record = Files.createDirectory(tempDir.resolve("_t")).resolve("custom.json");
        Files.writeString(record, "{\"id\":1,\"score\":10}");
        var context = Context.load(tempDir);

        var update = AddOrUpdateService.addOrUpdateRecord(
                context, context.makeValue(), "t", "{\"id\":1,\"score\":20}");
        assertEquals(AddOrUpdateService.AddOrUpdateErrorCode.UpdateOK, update.errorCode());
        assertFalse(Files.exists(record.resolveSibling("1.json")));
        var reloaded = Context.load(tempDir).makeValue();
        var score = (configgen.value.CfgValue.VInt) reloaded.getTable("t").valueList().getFirst().values().get(1);
        assertEquals(20, score.value());

        var deletion = DeleteService.deleteRecord(context, context.makeValue(), "t", "1");
        assertEquals(DeleteService.DeleteErrorCode.OK, deletion.errorCode());
        assertFalse(Files.exists(record));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void failedJsonReplacementLeavesOldRecordAndRemovesStagingFile() throws IOException {
        Files.writeString(tempDir.resolve("config.cfg"), "table t[id] { id:int; }");
        Files.writeString(tempDir.resolve("t.csv"), "ID\nid\n1\n");
        var context = Context.load(tempDir);
        var table = context.makeValue().getTable("t");
        assertNotNull(table);
        var record = table.primaryKeyMap().values().iterator().next();
        Path dir = Files.createDirectory(tempDir.resolve("_target"));
        Path target = dir.resolve("1.json");
        String old = "{\"id\":1,\"$note\":\"original\"}";
        Files.writeString(target, old);

        // Permit in-place writes while preventing removal or replacement of the path.
        try (var channel = FileChannel.open(target, StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)) {
            assertThrows(IOException.class, () -> VTableJsonStorage.addOrUpdateRecord(
                    record, "target", "1", tempDir, context.sourceStructure()));
            assertEquals(old, Files.readString(target));
        }
        try (var paths = Files.list(dir)) {
            assertEquals(List.of(target), paths.toList());
        }
        assertEquals(Path.of("_target", "1.json"), VTableJsonStorage.addOrUpdateRecord(
                record, "target", "1", tempDir, context.sourceStructure()));
        assertNotEquals(old, Files.readString(target));
    }

    @Test
    void shouldUseNestedPathWhenModuleDirExists() throws IOException {
        // Given: 存在模块目录 buff
        Files.createDirectories(tempDir.resolve("buff"));

        // When
        Path result = JsonLayout.newTableDirectory("buff.skill", tempDir);

        // Then: buff/_skill
        assertEquals(Path.of("buff").resolve("_skill"), result);
    }

    @Test
    void shouldUseNestedPathWhenModuleDirHasChineseSuffix() throws IOException {
        // Given: 模块目录名为 "skill_技能"（codeName = "skill"）
        Files.createDirectories(tempDir.resolve("skill_技能"));

        // When
        Path result = JsonLayout.newTableDirectory("skill.buff", tempDir);

        // Then: "skill_技能/_buff"
        assertEquals(Path.of("skill_技能").resolve("_buff"), result);
    }

    @Test
    void shouldFallbackToRootLevelWhenModuleDirDoesNotExist() {
        // Given: 没有模块目录

        // When
        Path result = JsonLayout.newTableDirectory("buff.skill", tempDir);

        // Then: 回退旧格式 _buff_skill
        assertEquals(Path.of("_buff_skill"), result);
    }

    @Test
    void shouldHandleDeeplyNestedPath() throws IOException {
        // Given: 多层模块目录 a/b
        Files.createDirectories(tempDir.resolve("a").resolve("b"));

        // When
        Path result = JsonLayout.newTableDirectory("a.b.c", tempDir);

        // Then: a/b/_c
        assertEquals(Path.of("a").resolve("b").resolve("_c"), result);
    }

    @Test
    void shouldFallbackWhenPartialModuleChainMissing() throws IOException {
        // Given: 只有 a/ 目录，没有 a/b/
        Files.createDirectories(tempDir.resolve("a"));

        // When
        Path result = JsonLayout.newTableDirectory("a.b.c", tempDir);

        // Then: 链不完整，回退 _a_b_c
        assertEquals(Path.of("_a_b_c"), result);
    }

    @Test
    void shouldHandleTableWithoutDot() {
        // Given: 表名没有点号

        // When
        Path result = JsonLayout.newTableDirectory("simpletable", tempDir);

        // Then: 回退 _simpletable
        assertEquals(Path.of("_simpletable"), result);
    }

    @Test
    void shouldDeeplyNestedWithChineseSuffixDir() throws IOException {
        // Given: a/b_数据/ 目录结构（codeName 分别为 a, b）
        Files.createDirectories(tempDir.resolve("a").resolve("b_数据"));

        // When
        Path result = JsonLayout.newTableDirectory("a.b.c", tempDir);

        // Then: a/b_数据/_c
        assertEquals(Path.of("a").resolve("b_数据").resolve("_c"), result);
    }

}
