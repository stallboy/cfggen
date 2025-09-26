package configgen.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static int verboseLevel = 0;
    private static PrintStream verboseStream = null;
    private static boolean profileGcEnabled = false;

    private static boolean profileEnabled = false;
    private static boolean warningEnabled = true;
    private static boolean weakWarningEnabled = false;

    public static void enableProfileGc() {
        profileGcEnabled = true;
    }

    public static void enableProfile() {
        profileEnabled = true;
    }

    public static boolean isProfileEnabled() {
        return profileEnabled;
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


    public static void setWeakWarningEnabled(boolean isWeakWarningEnabled) {
        weakWarningEnabled = isWeakWarningEnabled;
    }

    public static boolean isWeakWarningEnabled() {
        return weakWarningEnabled;
    }

    public static PrintStream setVerboseStream(PrintStream stream) {
        PrintStream old = verboseStream;
        verboseStream = stream;
        return old;
    }

    public static void verbose(String fmt, Object... args) {
        if (verboseStream != null) {
            logTo(verboseStream, fmt, args);
        }else if (verboseLevel > 0) {
            logTo(System.out, fmt, args);
        }
    }

    public static void verbose2(String fmt, Object... args) {
        if (verboseStream != null) {
            logTo(verboseStream, fmt, args);
        }else if (verboseLevel > 1) {
            logTo(System.out, fmt, args);
        }
    }

    public static void log(String fmt, Object... args) {
        logTo(System.out, fmt, args);
        if (verboseStream != null) {
            logTo(verboseStream, fmt, args);
        }
    }

    private final static DateTimeFormatter df = DateTimeFormatter.ofPattern("HH.mm.ss.SSS");
    private static long time;
    private static long firstTime;


    public static void logTo(PrintStream ps, String fmt, Object... args) {
        if (args.length == 0) {
            ps.println(fmt);
        } else {
            ps.printf((fmt) + System.lineSeparator(), args);
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
