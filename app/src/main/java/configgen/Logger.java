package configgen;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.util.FormatProcessor.FMT;

public class Logger {
    private static int verboseLevel = 0;
    private static boolean profileGcEnabled = false;
    private static boolean profileEnabled = false;


    public static void enableProfileGc() {
        profileGcEnabled = true;
    }

    public static void setVerboseLevel(int lvl) {
        verboseLevel = lvl;
    }

    public static void enableProfile() {
        profileEnabled = true;
    }

    public static int verboseLevel() {
        return verboseLevel;
    }

    public static void verbose(String s) {
        if (verboseLevel > 0) {
            log(s);
        }
    }

    public static void verbose2(String s) {
        if (verboseLevel > 1) {
            log(s);
        }
    }

    private final static SimpleDateFormat df = new SimpleDateFormat("HH.mm.ss.SSS");
    private static long time;
    private static long firstTime;

    public static void log(String s) {
        System.out.println(s);
    }

    public static void profile(String step) {
        if (profileEnabled) {
            if (profileGcEnabled) {
                System.gc();
            }
            long memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
            String elapse;
            if (time == 0) {
                elapse = df.format(Calendar.getInstance().getTime());
                time = System.currentTimeMillis();
                firstTime = time;
            } else {
                long old = time;
                time = System.currentTimeMillis();
                elapse = String.format("%.1f/%.1f seconds", (time - old) / 1000f, (time - firstTime) / 1000f);
            }
            System.out.println(FMT. "%30s\{ step }: %4d\{ memory }m %s\{ elapse }" );
        }
    }

}
