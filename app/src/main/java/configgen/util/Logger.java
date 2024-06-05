package configgen.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static int verboseLevel = 0;
    private static boolean profileGcEnabled = false;
    private static boolean profileEnabled = false;
    private static boolean warningEnabled = true;

    public static void enableProfileGc() {
        profileGcEnabled = true;
    }

    public static void enableProfile() {
        profileEnabled = true;
    }

    public static void setVerboseLevel(int lvl) {
        verboseLevel = lvl;
    }

    public static int verboseLevel() {
        return verboseLevel;
    }

    public static void setWarningEnabled(boolean isWarningEnabled) {
        warningEnabled = isWarningEnabled;
    }

    public static boolean isWarningEnabled() {
        return warningEnabled;
    }

    public static void verbose(String fmt, Object... args) {
        if (verboseLevel > 0) {
            log(fmt, args);
        }
    }

    public static void verbose2(String fmt, Object... args) {
        if (verboseLevel > 1) {
            log(fmt, args);
        }
    }

    private final static DateTimeFormatter df = DateTimeFormatter.ofPattern("HH.mm.ss.SSS");
    private static long time;
    private static long firstTime;

    public static void log(String fmt, Object... args) {
        if (args.length == 0) {
            System.out.println(fmt);
        } else {
            System.out.printf((fmt) + System.lineSeparator(), args);
        }
    }

    public static void profile(String step) {
        if (profileEnabled) {
            if (profileGcEnabled) {
                System.gc();
            }
            long memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
            String elapse;
            if (time == 0) {
                elapse = df.format(LocalDateTime.now());
                time = System.currentTimeMillis();
                firstTime = time;
            } else {
                long old = time;
                time = System.currentTimeMillis();
                elapse = String.format("%.1f/%.1f seconds", (time - old) / 1000f, (time - firstTime) / 1000f);
            }
            System.out.printf("%30s: %4dm %s%n", step, memory, elapse);
        }
    }

}
