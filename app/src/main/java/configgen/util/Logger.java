package configgen.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.UnaryOperator;

public class Logger {
    public interface Printer {
        void printf(String fmt, Object... args);


        Printer nullPrinter = (fmt, args) -> {
            // do nothing
        };
        Printer outPrinter = System.out::printf;

        static Printer of(PrintStream stream) {
            return stream::printf;
        }

        static Printer ofSeq(Printer... printers) {
            return (fmt, args) -> {
                for (Printer p : printers) {
                    p.printf(fmt, args);
                }
            };
        }
    }

    /// printerScope 的返回句柄：close 不抛受检异常，可直接 try-with-resources
    public interface PrinterScope extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * 一次性的不可变配置快照：整体替换，读侧每次调用读一次快照，无锁、无多字段撕裂。
     * 写侧（setter）synchronized 互斥，防并发配置时丢更新；日志路径不受影响。
     */
    private record LogConfig(Printer printer, int verboseLevel, boolean profileEnabled,
                             boolean profileGcEnabled, boolean warningEnabled, boolean weakWarningEnabled) {
    }

    private static volatile LogConfig config =
            new LogConfig(Printer.outPrinter, 0, false, false, true, false);

    /**
     * printerScope 的作用域栈：栈顶存在时日志走栈顶，否则走 config.printer。
     * 供"一段时间内 tee/改向输出"的场景（如 i18n 生成抓取 verbose 日志）使用，
     * 替代直接 setPrinter 换掉再手工换回（异常路径会漏恢复）。
     */
    private static final ConcurrentLinkedDeque<Printer> printerStack = new ConcurrentLinkedDeque<>();

    /**
     * 作用域内把日志改向到 scopedPrinter（通常用 Printer.ofSeq(base, of(stream)) 做 tee）。
     * close 时从栈顶向下移除第一个 equals 命中（printer 为 lambda，按身份比较）——
     * 正常的 try-with-resources 嵌套关闭即栈顶命中，乱序关闭也能正确恢复。
     */
    public static PrinterScope printerScope(Printer scopedPrinter) {
        Objects.requireNonNull(scopedPrinter);
        printerStack.push(scopedPrinter);
        return () -> printerStack.remove(scopedPrinter);
    }

    private static Printer currentPrinter() {
        Printer top = printerStack.peek();
        return top != null ? top : config.printer();
    }

    private static synchronized void updateConfig(UnaryOperator<LogConfig> f) {
        config = f.apply(config);
    }

    public static void enableProfileGc() {
        updateConfig(c -> new LogConfig(c.printer(), c.verboseLevel(), c.profileEnabled(), true, c.warningEnabled(), c.weakWarningEnabled()));
    }

    public static void enableProfile() {
        updateConfig(c -> new LogConfig(c.printer(), c.verboseLevel(), true, c.profileGcEnabled(), c.warningEnabled(), c.weakWarningEnabled()));
    }

    public static boolean isProfileEnabled() {
        return config.profileEnabled();
    }

    public static void setVerboseLevel(int lvl) {
        updateConfig(c -> new LogConfig(c.printer(), lvl, c.profileEnabled(), c.profileGcEnabled(), c.warningEnabled(), c.weakWarningEnabled()));
    }

    public static int verboseLevel() {
        return config.verboseLevel();
    }

    public static void setWarningEnabled(boolean isWarningEnabled) {
        updateConfig(c -> new LogConfig(c.printer(), c.verboseLevel(), c.profileEnabled(), c.profileGcEnabled(), isWarningEnabled, c.weakWarningEnabled()));
    }

    public static boolean isWarningEnabled() {
        return config.warningEnabled();
    }


    public static void setWeakWarningEnabled(boolean isWeakWarningEnabled) {
        updateConfig(c -> new LogConfig(c.printer(), c.verboseLevel(), c.profileEnabled(), c.profileGcEnabled(), c.warningEnabled(), isWeakWarningEnabled));
    }

    public static boolean isWeakWarningEnabled() {
        return config.weakWarningEnabled();
    }

    /**
     * 基础 printer（config 里配置的那个），不含 printerScope 栈顶。
     * 想构造 tee 时用它作为原样输出的一路。
     */
    public static Printer getPrinter() {
        return config.printer();
    }

    public static void setPrinter(Printer newPrinter) {
        Objects.requireNonNull(newPrinter);
        updateConfig(c -> new LogConfig(newPrinter, c.verboseLevel(), c.profileEnabled(), c.profileGcEnabled(), c.warningEnabled(), c.weakWarningEnabled()));
    }

    public static void verbose(String fmt, Object... args) {
        if (config.verboseLevel() > 0) {
            log(fmt, args);
        }
    }

    public static void verbose2(String fmt, Object... args) {
        if (config.verboseLevel() > 1) {
            log(fmt, args);
        }
    }

    public static void log(String fmt, Object... args) {
        currentPrinter().printf((fmt) + System.lineSeparator(), args);
    }

    private final static DateTimeFormatter df = DateTimeFormatter.ofPattern("HH.mm.ss.SSS");

    // profile 计时状态：仅开发期用，synchronized 足够；跨线程只影响计时显示
    private static final Object profileLock = new Object();
    private static long time;
    private static long firstTime;


    public static void profile(String step) {
        if (!config.profileEnabled()) {
            return;
        }
        if (config.profileGcEnabled()) {
            System.gc();
        }
        long memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        String elapse;
        synchronized (profileLock) {
            if (time == 0) {
                elapse = df.format(LocalDateTime.now());
                time = System.currentTimeMillis();
                firstTime = time;
            } else {
                long old = time;
                time = System.currentTimeMillis();
                elapse = String.format("%.1f/%.1f seconds", (time - old) / 1000f, (time - firstTime) / 1000f);
            }
        }
        log("%30s: %4dm %s", step, memory, elapse);
    }

}
