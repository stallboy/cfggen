package configgen.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ArgParserTest {

    @Test
    void colonSeparator() {
        Map<String, String> m = ArgParser.parseToMap("dir:xxx,encoding:UTF-8");
        assertEquals("xxx", m.get("dir"));
        assertEquals("UTF-8", m.get("encoding"));
    }

    @Test
    void equalsSeparator() {
        Map<String, String> m = ArgParser.parseToMap("dir=xxx,encoding=UTF-8");
        assertEquals("xxx", m.get("dir"));
        assertEquals("UTF-8", m.get("encoding"));
    }

    @Test
    void equalsSeparatorWithWindowsAbsolutePath() {
        // GUI 生成的命令用 '='，值里的 ':'（如 Windows 绝对路径）不能被当作分隔符
        Map<String, String> m = ArgParser.parseToMap("dst=D:\\out");
        assertEquals("D:\\out", m.get("dst"));
    }

    @Test
    void colonSeparatorWithValueContainingEquals() {
        Map<String, String> m = ArgParser.parseToMap("dir:C:\\a=b");
        assertEquals("C:\\a=b", m.get("dir"));
    }

    @Test
    void firstSeparatorWins() {
        Map<String, String> m = ArgParser.parseToMap("a:b=c");
        assertEquals("b=c", m.get("a"));

        Map<String, String> m2 = ArgParser.parseToMap("a=b:c");
        assertEquals("b:c", m2.get("a"));
    }

    @Test
    void flagWithoutValue() {
        Map<String, String> m = ArgParser.parseToMap("flag,dir:xxx");
        assertNull(m.get("flag"));
        assertEquals("xxx", m.get("dir"));
    }

    @Test
    void idAndMap() {
        ArgParser.IdAndMap r = ArgParser.parseToIdAndMap("go,dir:.,dst=D:\\out");
        assertEquals("go", r.id());
        assertEquals(".", r.map().get("dir"));
        assertEquals("D:\\out", r.map().get("dst"));
    }
}
