package configgen.util;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ArgParserTest {

    @Test
    void colonSeparator() {
        var m = ArgParser.parseToIdAndMap("go,dir:xxx,encoding:UTF-8").map();
        assertEquals("xxx", m.get("dir"));
        assertEquals("UTF-8", m.get("encoding"));
    }

    @Test
    void equalsSeparator() {
        var m = ArgParser.parseToIdAndMap("go,dir=xxx,encoding=UTF-8").map();
        assertEquals("xxx", m.get("dir"));
        assertEquals("UTF-8", m.get("encoding"));
    }

    @Test
    void equalsSeparatorWithWindowsAbsolutePath() {
        // GUI 生成的命令用 '='，值里的 ':'（如 Windows 绝对路径）不能被当作分隔符
        var m = ArgParser.parseToIdAndMap("go,dst=D:\\out").map();
        assertEquals("D:\\out", m.get("dst"));
    }

    @Test
    void colonSeparatorWithValueContainingEquals() {
        var m = ArgParser.parseToIdAndMap("go,dir:C:\\a=b").map();
        assertEquals("C:\\a=b", m.get("dir"));
    }

    @Test
    void firstSeparatorWins() {
        var m = ArgParser.parseToIdAndMap("go,a:b=c").map();
        assertEquals("b=c", m.get("a"));

        var m2 = ArgParser.parseToIdAndMap("go,a=b:c").map();
        assertEquals("b:c", m2.get("a"));
    }

    @Test
    void flagWithoutValue() {
        var m = ArgParser.parseToIdAndMap("go,flag,dir:xxx").map();
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
