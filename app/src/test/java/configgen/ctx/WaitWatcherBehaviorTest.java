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

    /**
     * 轮询等待listener被调用到expected次。触发链路（文件事件→tick→debounce→listener）是异步的，
     * 固定sleep在负载下不可靠，换成有界轮询。
     */
    private static void awaitListenerCalled(AtomicInteger count, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (count.get() < expected) {
            if (System.currentTimeMillis() > deadline) {
                fail("5秒内listener未被调用到" + expected + "次，当前" + count.get());
            }
            Thread.sleep(10);
        }
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
            // When: 触发文件事件
            Path testFile = tempDir.resolve("test.csv");
            Files.writeString(testFile, "test content");

            // Then: 监听器应该被调用一次
            awaitListenerCalled(listenerCallCount, 1);
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
            // When: 在等待时间内触发多个文件事件
            Path file1 = tempDir.resolve("file1.csv");
            Files.writeString(file1, "content1");

            Path file2 = tempDir.resolve("file2.csv");
            Files.writeString(file2, "content2");

            Path file3 = tempDir.resolve("file3.csv");
            Files.writeString(file3, "content3");

            // Then: 监听器应该只被调用一次（事件聚合）
            awaitListenerCalled(listenerCallCount, 1);
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
            // 触发第一次事件并等待监听器调用
            Path file1 = tempDir.resolve("file1.csv");
            Files.writeString(file1, "content1");
            awaitListenerCalled(listenerCallCount, 1);

            // When: 触发第二次事件
            Path file2 = tempDir.resolve("file2.csv");
            Files.writeString(file2, "content2");

            // Then: 监听器应该被调用两次
            awaitListenerCalled(listenerCallCount, 2);
            assertEquals(2, listenerCallCount.get(), "监听器应该被调用两次");
        } finally {
            watcher.stop();
            waitWatcher.stop();
        }
    }

    @Test
    void shouldSupportStopFromListenerThread() throws IOException, InterruptedException {
        // WatchAndPostRun的autoFix循环保护会在reloadData（即listener）里调用stopWatch→waitWatcher.stop()，
        // 即从轮询线程自身调用stop。必须支持：不抛异常、不自join挂死，且轮询循环在本次tick后退出
        Watcher watcher = new Watcher(tempDir, null);
        watcher.start();

        AtomicInteger listenerCallCount = new AtomicInteger(0);
        WaitWatcher[] holder = new WaitWatcher[1];
        WaitWatcher waitWatcher = new WaitWatcher(watcher, () -> {
            listenerCallCount.incrementAndGet();
            holder[0].stop();
        }, 30, 20);
        holder[0] = waitWatcher;
        waitWatcher.start();

        try {
            // When: 触发事件，listener内自停
            Files.writeString(tempDir.resolve("self_stop.csv"), "content");
            awaitListenerCalled(listenerCallCount, 1);

            // Then: 轮询线程应已退出——之后的新事件不再触发listener
            Files.writeString(tempDir.resolve("self_stop2.csv"), "content2");
            Thread.sleep(300);
            assertEquals(1, listenerCallCount.get(), "自停后轮询线程应退出，不应再触发listener");
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
            // When: 在等待时间内触发事件
            Path file1 = tempDir.resolve("file1.csv");
            Files.writeString(file1, "content1");

            // 等待时间不足，不应该触发
            Thread.sleep(10);

            // Then: 监听器不应该被调用
            assertEquals(0, listenerCallCount.get(), "等待时间不足时监听器不应该被调用");

            // 继续等待到超过等待时间
            awaitListenerCalled(listenerCallCount, 1);

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
