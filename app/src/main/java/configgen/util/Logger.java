package configgen.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Logger {
    public interface Printer {
        void printf(String fmt, Object... args);


        Printer nullPrinter = (fmt, args) -> {
            // do nothing
        };
        Printer outPrinter = System.out::printf;

        static Printer of(PrintStream stream){
            return stream::printf;
        }

        static Printer ofSeq(Printer... printers){
            return (fmt, args) -> {
                for (Printer p : printers) {
                    p.printf(fmt, args);
                }
            };
        }
    }


    private static int verboseLevel = 0;
    private static Printer printer = Printer.outPrinter;
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

    public static Printer getPrinter(){
        return printer;
    }

    public static void setPrinter(Printer newPrinter) {
        Objects.requireNonNull(newPrinter);
        printer = newPrinter;
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

    public static void log(String fmt, Object... args) {
        printer.printf((fmt) + System.lineSeparator(), args);
    }

    private final static DateTimeFormatter df = DateTimeFormatter.ofPattern("HH.mm.ss.SSS");
    private static long time;
    private static long firstTime;


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
            log("%30s: %4dm %s", step, memory, elapse);
        }
    }

}
