package configgen.ctx;

import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行为驱动测试：验证 WaitWatcher 类的公共行为
 * 专注于延迟触发、事件聚合、配置参数影响等外部行为
 */
class WaitWatcherBehaviorTest {

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
    void shouldTriggerListenerAfterWaitPeriodWhenSingleEventOccurs() throws IOException, InterruptedException {
        // Given: WaitWatcher 和监听器
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        AtomicInteger listenerCallCount = new AtomicInteger(0);
        WaitWatcher waitWatcher = new WaitWatcher(watcher, listenerCallCount::incrementAndGet, 50, 50);
        waitWatcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(30);

            // When: 触发文件事件
            Path testFile = tempDir.resolve("test.csv");
            Files.writeString(testFile, "test content");

            // 等待超过等待时间
            Thread.sleep(200);

            // Then: 监听器应该被调用一次
            assertEquals(1, listenerCallCount.get(), "监听器应该被调用一次");
        } finally {
            watcher.stop();
            waitWatcher.stop();
        }
    }

    @Test
    void shouldAggregateMultipleEventsWithinWaitPeriod() throws IOException, InterruptedException {
        // Given: WaitWatcher 和监听器
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        AtomicInteger listenerCallCount = new AtomicInteger(0);
        WaitWatcher waitWatcher = new WaitWatcher(watcher, listenerCallCount::incrementAndGet, 50, 50);
        waitWatcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(30);

            // When: 在等待时间内触发多个文件事件
            Path file1 = tempDir.resolve("file1.csv");
            Files.writeString(file1, "content1");

            Path file2 = tempDir.resolve("file2.csv");
            Files.writeString(file2, "content2");

            Path file3 = tempDir.resolve("file3.csv");
            Files.writeString(file3, "content3");

            // 等待超过等待时间（增加等待时间以适应非递归监控的延迟）
            Thread.sleep(200);

            // Then: 监听器应该只被调用一次（事件聚合）
            assertEquals(1, listenerCallCount.get(), "多个事件应该被聚合为一次调用");
        } finally {
            watcher.stop();
            waitWatcher.stop();
        }
    }

    @Test
    void shouldNotTriggerListenerWhenNoEventsOccur() throws InterruptedException {
        // Given: WaitWatcher 和监听器
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        AtomicInteger listenerCallCount = new AtomicInteger(0);
        WaitWatcher waitWatcher = new WaitWatcher(watcher, listenerCallCount::incrementAndGet, 50, 50);
        waitWatcher.start();

        try {
            // When: 等待超过等待时间（没有事件发生）
            Thread.sleep(100);

            // Then: 监听器不应该被调用
            assertEquals(0, listenerCallCount.get(), "没有事件时监听器不应该被调用");
        } finally {
            watcher.stop();
            waitWatcher.stop();
        }
    }

    @Test
    void shouldTriggerTwice() throws IOException, InterruptedException {
        // Given: WaitWatcher 和监听器
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        AtomicInteger listenerCallCount = new AtomicInteger(0);
        WaitWatcher waitWatcher = new WaitWatcher(watcher, listenerCallCount::incrementAndGet, 50, 50);
        waitWatcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(30);

            // 触发第一次事件并等待监听器调用
            Path file1 = tempDir.resolve("file1.csv");
            Files.writeString(file1, "content1");
            Thread.sleep(200);

            int firstCallCount = listenerCallCount.get();
            assertEquals(1, firstCallCount, "第一次调用");

            // When: 触发第二次事件
            Path file2 = tempDir.resolve("file2.csv");
            Files.writeString(file2, "content2");
            Thread.sleep(200);

            // Then: 监听器应该被调用两次
            assertEquals(2, listenerCallCount.get(), "监听器应该被调用两次");
        } finally {
            watcher.stop();
            waitWatcher.stop();
        }
    }

    @Test
    void shouldHandleZeroWaitTimeConfiguration() {
        // Given: 零等待时间的配置
        Watcher watcher = new Watcher(tempDir, null);
        AtomicInteger listenerCallCount = new AtomicInteger(0);

        // When & Then: 零等待时间应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            new WaitWatcher(watcher, listenerCallCount::incrementAndGet, 0);
        }, "零等待时间应该抛出异常");
    }

    @Test
    void shouldThrowExceptionWhenNegativeWaitTimeIsProvided() {
        // Given: 负等待时间
        Watcher watcher = new Watcher(tempDir, null);
        AtomicInteger listenerCallCount = new AtomicInteger(0);

        // When & Then: 应该抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new WaitWatcher(watcher, listenerCallCount::incrementAndGet, -1);
        }, "负等待时间应该抛出异常");
    }

    @Test
    void shouldHandleNullListenerGracefully() {
        // Given: null 监听器
        Watcher watcher = new Watcher(tempDir, null);

        // When & Then: 应该抛出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            new WaitWatcher(watcher, null, 100);
        }, "null 监听器应该抛出异常");
    }

    @Test
    void shouldHandleNullWatcherGracefully() {
        // Given: null Watcher
        AtomicInteger listenerCallCount = new AtomicInteger(0);

        // When & Then: 应该抛出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            new WaitWatcher(null, listenerCallCount::incrementAndGet, 1);
        }, "null Watcher 应该抛出异常");
    }

    @Test
    void shouldRespectConfiguredWaitTimeForEventAggregation() throws IOException, InterruptedException {
        // Given: 配置了较长等待时间的 WaitWatcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        AtomicInteger listenerCallCount = new AtomicInteger(0);
        WaitWatcher waitWatcher = new WaitWatcher(watcher, listenerCallCount::incrementAndGet, 50, 50);
        waitWatcher.start();

        try {
            // 等待监控器初始化
            Thread.sleep(30);

            // When: 在等待时间内触发事件
            Path file1 = tempDir.resolve("file1.csv");
            Files.writeString(file1, "content1");

            // 等待时间不足，不应该触发
            Thread.sleep(10);

            // Then: 监听器不应该被调用
            assertEquals(0, listenerCallCount.get(), "等待时间不足时监听器不应该被调用");

            // 继续等待到超过等待时间
            Thread.sleep(200);

            // 监听器应该被调用
            assertEquals(1, listenerCallCount.get(), "等待时间足够时监听器应该被调用");
        } finally {
            watcher.stop();
            waitWatcher.stop();
        }
    }

//    @Test
    void shouldHandleMultipleWaitWatchersIndependently() throws IOException, InterruptedException {
        // Given: 两个独立的 WaitWatcher
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        AtomicInteger listener1CallCount = new AtomicInteger(0);
        AtomicInteger listener2CallCount = new AtomicInteger(0);

        WaitWatcher waitWatcher1 = new WaitWatcher(watcher, listener1CallCount::incrementAndGet, 50, 50);
        WaitWatcher waitWatcher2 = new WaitWatcher(watcher, listener2CallCount::incrementAndGet, 150, 50);

        waitWatcher1.start();
        waitWatcher2.start();

        try {
            // 等待监控器初始化
            Thread.sleep(30);

            // When: 触发文件事件
            Path testFile = tempDir.resolve("test.csv");
            Files.writeString(testFile, "test content");

            // 等待较短的时间
            Thread.sleep(100);

            // Then: 第一个监听器应该被调用，第二个不应该
            assertEquals(1, listener1CallCount.get(), "第一个监听器应该被调用");
            assertEquals(0, listener2CallCount.get(), "第二个监听器不应该被调用（等待时间不足）");

            // 继续等待
            Thread.sleep(300);

            // 第二个监听器应该被调用
            assertEquals(1, listener2CallCount.get(), "第二个监听器应该被调用");
        } finally {
            watcher.stop();
            waitWatcher1.stop();
            waitWatcher2.stop();
        }
    }
}