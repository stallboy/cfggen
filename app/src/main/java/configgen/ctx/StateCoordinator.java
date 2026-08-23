package configgen.ctx;

import configgen.util.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 可变共享状态（如当前代Context）与订阅者内存快照的协调器。
 *
 * <p>解决的问题是：状态换代（watch reload 装入新一代）与编辑写操作（基于旧状态算新值、写文件、更新状态）
 * 必须互斥，否则写handler基于旧数据算出的新值会覆盖掉换代装入的新一代；
 * 且同进程可能并存多个状态持有者（如 {@code -gen server,-gen mcpserver} 同跑），任一方的写操作
 * 都要让所有持有者的快照立刻换代，而不是各自持锁、互相不可见。
 *
 * <p>使用单一 editLock 串行化两条路径，锁内完成状态安装与全部快照刷新：
 * <ul>
 *   <li>{@link #installState}：reload 换代；</li>
 *   <li>{@link #runEdit}：编辑写操作（EditorServer 的 record 写接口、McpServer 的 WriteRecordTool 都必须经此进入），
 *       编辑完成后统一刷新快照——写路径会更新共享状态的lastModified抑制watch reload，
 *       其他持有者只能靠这里的刷新看到新值。</li>
 * </ul>
 * 锁内回调只做内存快照构建（makeValue 在写后命中缓存），不做文件IO，持有时间可控。
 *
 * @param <T> 状态类型
 */
public final class StateCoordinator<T> {

    public interface Refresher<T> {
        void refresh(T state);
    }

    private final Object editLock = new Object();
    private volatile T state;
    // 注册发生在主线程（server的generate）、迭代发生在锁内刷新，用COW保证可见性
    private final List<Refresher<T>> refreshers = new CopyOnWriteArrayList<>();

    public StateCoordinator() {
        this(null);
    }

    public StateCoordinator(T initial) {
        this.state = initial;
    }

    public T state() {
        return state;
    }

    /**
     * 启动期登记初始状态：不触发刷新（此刻通常还没有订阅者），也不与写路径竞争（server尚未开始监听）。
     */
    public void setInitial(T initial) {
        state = initial;
    }

    /**
     * 换代安装新状态（reload路径）。在锁内安装并刷新所有订阅者快照，与编辑写操作互斥、顺序一致。
     */
    public void installState(T newState) {
        synchronized (editLock) {
            state = newState;
            refreshAll();
        }
    }

    /**
     * 编辑写操作的统一临界区：与 {@link #installState}、其他 {@link #runEdit} 互斥。
     * 编辑（写文件 + 就地更新状态）完成后统一刷新所有订阅者快照。编辑抛异常则直接传播、不刷新（状态未变）。
     */
    public <R> R runEdit(Function<T, R> edit) {
        synchronized (editLock) {
            R result = edit.apply(state);
            refreshAll();
            return result;
        }
    }

    public void addRefresher(Refresher<T> refresher) {
        refreshers.add(refresher);
    }

    private void refreshAll() {
        T cur = state;
        for (Refresher<T> refresher : refreshers) {
            try {
                refresher.refresh(cur);
            } catch (Exception e) {
                // 单个订阅者刷新失败不能中断其他订阅者，也不能破坏换代安装
                Logger.log("refresh snapshot err: %s", e.toString());
            }
        }
    }
}
