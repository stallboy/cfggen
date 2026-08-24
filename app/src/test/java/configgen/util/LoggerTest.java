package configgen.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoggerTest {

    // 用 String.format 渲染，与真实 PrintStream.printf 同语义：fmt 非法时同样抛异常
    private static final class RecordingPrinter implements Logger.Printer {
        final List<String> lines = new ArrayList<>();

        @Override
        public void printf(String fmt, Object... args) {
            lines.add(String.format(fmt, args));
        }
    }

    private final RecordingPrinter printer = new RecordingPrinter();

    @AfterEach
    public void restorePrinter() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    public void plainMessageWithoutPercent() {
        Logger.setPrinter(printer);
        Logger.log("abc[table[field] set comment: old -> new");
        assertEquals(List.of("abc[table[field] set comment: old -> new" + System.lineSeparator()), printer.lines);
    }

    @Test
    public void messageWithStrayPercentDoesNotThrow() {
        Logger.setPrinter(printer);
        // r2044 崩溃场景：注释含 "100%"，拼进消息后整串被当格式串
        Logger.log("table[field] set comment: old -> 100% 回避");
        Logger.log("50%,x%%y %s %d");
        assertEquals(List.of(
                "table[field] set comment: old -> 100% 回避" + System.lineSeparator(),
                "50%,x%%y %s %d" + System.lineSeparator()), printer.lines);
    }

    @Test
    public void messageIsVerbatim() {
        Logger.setPrinter(printer);
        // 消息原文直出：%%、%s 都不作 printf 解释
        Logger.log("100%% off");
        assertEquals(List.of("100%% off" + System.lineSeparator()), printer.lines);
    }

    @Test
    public void formatArgsStillWork() {
        Logger.setPrinter(printer);
        Logger.log("%s[field] set comment: %s -> %s", "table", "old", "new");
        assertEquals(List.of("table[field] set comment: old -> new" + System.lineSeparator()), printer.lines);
    }

    @Test
    public void recordingPrinterStillSurfacesBadFormat() {
        Logger.setPrinter(printer);
        assertThrows(java.util.UnknownFormatConversionException.class,
                () -> printer.printf("100% 回避" + System.lineSeparator()));
    }
}
