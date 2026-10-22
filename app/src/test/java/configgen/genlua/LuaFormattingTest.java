package configgen.genlua;

import configgen.InternalReferenceFixture;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LuaFormattingTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emitsCleanLinesAndPreservesStringContents(boolean relations, @TempDir Path root) throws Exception {
        Path data = Files.createDirectories(root.resolve("data"));
        Files.writeString(data.resolve("config.cfg"), """
                struct Point {
                    x:int; // Horizontal position
                    rank:int ->rank;
                }
                interface Action { struct Hit { amount:int; } struct Idle {} }
                table rank[id] { id:int; name:str; }
                table t[id] (json) {
                    id:int; point:Point; action:Action; rank:int ->rank; ranks:list<int> ->rank; note:str;
                    %s
                }
                """.formatted(relations ? "->chosen:[] ->::point;" : ""));
        Files.writeString(data.resolve("rank.csv"), "id,name\nid,name\n7,Ready\n");
        InternalReferenceFixture.write(data, "t", """
                {"id":1,"point":{"x":2,"rank":7},"action":{"$type":"Hit","amount":5},
                 "rank":7,"ranks":[7],"note":"marker  "}
                """);
        Path output = root.resolve("output");
        new LuaCodeGenerator(new ParameterParser("lua,emmylua,dir:" + output)).generate(new Context(data));

        assertTrue(Files.readString(output.resolve("cfg/t.lua")).contains("\"marker  \""), "数据字符串中的空格应保留");
        assertTrue(Files.readString(output.resolve("cfg/_beans.lua")).contains("---@field x number , Horizontal position"), "保留字段类型和注释间的分隔");
        try (var files = Files.walk(output)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                for (String line : Files.readAllLines(file)) {
                    assertEquals(line.stripTrailing(), line, () -> file + ": " + line);
                }
            }
        }
    }
}
