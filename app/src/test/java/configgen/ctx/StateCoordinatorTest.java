package configgen.ctx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StateCoordinator 的核心并发语义：
 * 写操作（runEdit）与换代（installState）互斥、写操作之间串行、
 * 两者完成后都统一刷新所有订阅者快照。
 */
class StateCoordinatorTest {

    @Test
    void runEditRefreshesAllSubscribersWithCurrentState() {
        StateCoordinator<String> c = new StateCoordinator<>("a");
        List<String> seen1 = new ArrayList<>();
        List<String> seen2 = new ArrayList<>();
        c.addRefresher(seen1::add);
        c.addRefresher(seen2::add);

        String result = c.runEdit(s -> s + "!");

        assertEquals("a!", result);
        assertEquals("a", c.state(), "编辑函数只应通过换代/install改变状态，runEdit本身不改");
        assertEquals(List.of("a"), seen1);
        assertEquals(List.of("a"), seen2);
    }

    @Test
    void installStateRefreshesAllSubscribersWithNewState() {
        StateCoordinator<String> c = new StateCoordinator<>("a");
        List<String> seen = new ArrayList<>();
        c.addRefresher(seen::add);

        c.installState("b");

        assertEquals("b", c.state());
        assertEquals(List.of("b"), seen);
    }

    @Test
    void setInitialDoesNotRefreshSubscribers() {
        StateCoordinator<String> c = new StateCoordinator<>();
        List<String> seen = new ArrayList<>();
        c.addRefresher(seen::add);

        c.setInitial("a");

        assertEquals("a", c.state());
        assertEquals(List.of(), seen, "启动期登记不应触发刷新（此刻通常还没有可用的订阅者快照）");
    }

    @Test
    void runEditAndInstallStateAreMutuallyExclusive() throws InterruptedException {
        // 回归：写handler基于旧数据算出的新值，不能覆盖掉换代装入的新一代状态。
        // 这里验证installState在runEdit临界区内被阻塞，直到编辑完成
        StateCoordinator<String> c = new StateCoordinator<>("old");
        CountDownLatch editStarted = new CountDownLatch(1);
        CountDownLatch releaseEdit = new CountDownLatch(1);
        AtomicBoolean installFinishedDuringEdit = new AtomicBoolean(false);

        Thread editor = new Thread(() -> {
            c.runEdit(s -> {
                editStarted.countDown();
                try {
                    releaseEdit.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return s + "-edited";
            });
        });
        editor.start();
        assertTrue(editStarted.await(5, TimeUnit.SECONDS), "编辑应已进入临界区");

        Thread installer = new Thread(() -> {
            c.installState("new");
            installFinishedDuringEdit.set(true);
        });
        installer.start();

        Thread.sleep(200);
        assertFalse(installFinishedDuringEdit.get(), "runEdit临界区内installState必须被阻塞");
        assertEquals("old", c.state(), "被阻塞的换代不得提前生效");

        releaseEdit.countDown();
        editor.join(5000);
        installer.join(5000);

        assertTrue(installFinishedDuringEdit.get(), "释放编辑后换代应完成");
        assertEquals("new", c.state());
    }

    @Test
    void concurrentEditsAreSerialized() throws InterruptedException {
        // 写操作之间必须串行：非原子的计数在runEdit内自增不允许丢更新
        StateCoordinator<Integer> c = new StateCoordinator<>(0);
        int[] counter = {0}; // 故意非原子
        int threads = 8;
        int perThread = 1000;

        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    c.runEdit(s -> {
                        counter[0]++;
                        return counter[0];
                    });
                }
            });
            ts.add(t);
            t.start();
        }
        for (Thread t : ts) {
            t.join(10000);
        }

        assertEquals(threads * perThread, counter[0], "并发编辑必须完全串行，不允许丢失更新");
    }

    @Test
    void editExceptionPropagatesWithoutRefresh() {
        StateCoordinator<String> c = new StateCoordinator<>("a");
        AtomicInteger refreshCount = new AtomicInteger();
        c.addRefresher(s -> refreshCount.incrementAndGet());

        assertThrows(RuntimeException.class, () -> c.runEdit(s -> {
            throw new RuntimeException("edit failed");
        }));

        assertEquals("a", c.state(), "编辑失败状态不变");
        assertEquals(0, refreshCount.get(), "编辑失败不应触发快照刷新（状态未变）");
    }

    @Test
    void oneRefresherFailureDoesNotBreakOthers() {
        StateCoordinator<String> c = new StateCoordinator<>("a");
        AtomicInteger goodCount = new AtomicInteger();
        c.addRefresher(s -> {
            throw new RuntimeException("bad refresher");
        });
        c.addRefresher(s -> goodCount.incrementAndGet());

        c.installState("b");

        assertEquals("b", c.state());
        assertEquals(1, goodCount.get(), "单个订阅者刷新失败不应中断其他订阅者");
    }
}
