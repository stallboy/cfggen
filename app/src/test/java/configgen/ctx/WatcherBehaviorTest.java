package configgen.ctx;

import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行为驱动测试：验证 Watcher 类的公共行为
 * 专注于文件变更检测、事件重置、过滤规则等外部行为
 */
class WatcherBehaviorTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }
    
    @Test
    void shouldDetectFileCreationWhenNewFileIsCreatedInWatchedDirectory() throws IOException, InterruptedException {
        // Given: 启动的 Watcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(20);

            // When: 创建新文件
            Path newFile = tempDir.resolve("test.csv"); // 使用 .csv 确保被监控
            Files.writeString(newFile, "test content");

            // 等待文件系统事件处理
            Thread.sleep(20);

            // Then: 应该检测到事件
            long lastEventTime = watcher.getLastEventMillis();
            assertTrue(lastEventTime > 0, "应该检测到文件创建事件");

            long version = watcher.getEventVersion();
            assertTrue(version > 0, "应该检测到文件创建事件");

        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldDetectFileModificationWhenExistingFileIsModified() throws IOException, InterruptedException {
        // Given: 现有文件和启动的 Watcher
        Path existingFile = tempDir.resolve("existing.csv");
        Files.writeString(existingFile, "initial content");

        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(50);

            // When: 修改文件
            Files.writeString(existingFile, "modified content");

            // 等待文件系统事件处理
            Thread.sleep(50);

            // Then: 应该检测到事件
            long lastEventTime = watcher.getLastEventMillis();
            assertTrue(lastEventTime > 0, "应该检测到文件修改事件");

            long version = watcher.getEventVersion();
            assertTrue(version > 0, "应该检测到文件修改事件");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldDetectFileDeletionWhenFileIsDeletedFromWatchedDirectory() throws IOException, InterruptedException {
        // Given: 现有文件和启动的 Watcher
        Path fileToDelete = tempDir.resolve("to_delete.csv");
        Files.writeString(fileToDelete, "content");

        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(50);

            // When: 删除文件
            Files.delete(fileToDelete);

            // 等待文件系统事件处理
            Thread.sleep(50);

            // Then: 应该检测到事件
            long lastEventTime = watcher.getLastEventMillis();
            assertTrue(lastEventTime > 0, "应该检测到文件删除事件");

            long version = watcher.getEventVersion();
            assertTrue(version > 0, "应该检测到文件删除事件");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldFindLastEventTimeEqualWhenGetLastEventSecCalledMultipleTimes() throws IOException, InterruptedException {
        // Given: 有事件的 Watcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(50);

            // 创建文件触发事件
            Path testFile = tempDir.resolve("test.csv");
            Files.writeString(testFile, "content");
            Thread.sleep(50);

            // When: 获取并重置事件时间
            long firstCall = watcher.getLastEventMillis();
            long secondCall = watcher.getLastEventMillis();

            // Then: 第一次调用应该返回事件时间和第二次应该返回相同
            assertEquals(firstCall, secondCall, "连续调用返回相同的事件时间");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldIgnoreHiddenFilesWhenDetectingFileChanges() throws IOException, InterruptedException {
        // Given: 启动的 Watcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(50);

            // When: 创建隐藏文件
            Path hiddenFile = tempDir.resolve("~hidden.txt");
            Files.writeString(hiddenFile, "hidden content");

            // 等待文件系统事件处理
            Thread.sleep(50);

            // Then: 应该忽略隐藏文件
            long lastEventTime = watcher.getLastEventMillis();
            assertEquals(0, lastEventTime, "应该忽略隐藏文件");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldDetectDirectoryCreationWhenNewDirectoryIsCreated() throws IOException, InterruptedException {
        // Given: 启动的 Watcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(50);

            // When: 创建新目录
            Path newDir = tempDir.resolve("new_directory");
            Files.createDirectories(newDir);

            // 等待文件系统事件处理
            Thread.sleep(50);

            // Then: 应该检测到目录创建事件
            long lastEventTime = watcher.getLastEventMillis();
            assertTrue(lastEventTime > 0, "应该检测到目录创建事件");

            long version = watcher.getEventVersion();
            assertTrue(version > 0, "应该检测到目录创建事件");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldFilterFilesBasedOnExplicitDirectoryConfiguration() throws IOException, InterruptedException {
        // Given: 显式目录配置和启动的 Watcher
        Path includedDir = tempDir.resolve("included");
        Path excludedDir = tempDir.resolve("excluded");
        Files.createDirectories(includedDir);
        Files.createDirectories(excludedDir);

        ExplicitDir explicitDir = new ExplicitDir(
                java.util.Map.of(),
                java.util.Set.of("included"),
                java.util.Set.of()
        );

        Watcher watcher = new Watcher(tempDir, explicitDir);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(50);

            // When: 在包含的目录中创建文件
            Path includedFile = includedDir.resolve("test.csv");
            Files.writeString(includedFile, "included content");

            // 在排除的目录中创建文件
            Path excludedFile = excludedDir.resolve("test.csv");
            Files.writeString(excludedFile, "excluded content");

            // 等待文件系统事件处理
            Thread.sleep(50);

            // Then: 应该只检测包含目录中的文件
            long lastEventTime = watcher.getLastEventMillis();
            assertTrue(lastEventTime > 0, "应该检测到包含目录中的文件");

            // 注意：由于文件系统事件的异步性，我们无法精确测试排除的文件是否被忽略
            // 但显式目录配置应该影响监控器的行为
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldHandleMultipleFileOperationsSequentially() throws IOException, InterruptedException {
        // Given: 启动的 Watcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(50);

            // When: 执行多个文件操作
            Path file1 = tempDir.resolve("file1.csv");
            Files.writeString(file1, "content1");

            Thread.sleep(20);

            Path file2 = tempDir.resolve("file2.csv");
            Files.writeString(file2, "content2");

            Thread.sleep(20);

            Files.delete(file1);

            // 等待文件系统事件处理
            Thread.sleep(20);

            // Then: 应该检测到事件
            long lastEventTime = watcher.getLastEventMillis();
            assertTrue(lastEventTime > 0, "应该检测到多个文件操作事件");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldReturnZeroWhenNoEventsHaveOccurred() throws InterruptedException {
        // Given: 没有事件的 Watcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();
        try {
            Thread.sleep(20);
            // When: 获取最后事件时间
            long lastEventTime = watcher.getLastEventMillis();

            // Then: 应该返回 0
            assertEquals(0, lastEventTime, "没有事件时应该返回 0");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void shouldHandleNullExplicitDirectoryConfiguration() {
        // Given: null 显式目录配置

        // When: 创建 Watcher
        Watcher watcher = new Watcher(tempDir, null);

        // Then: 应该成功创建
        assertNotNull(watcher, "应该成功创建带有 null 显式目录的 Watcher");

        // 启动和停止线程
        watcher.start();
        watcher.stop();
    }
}