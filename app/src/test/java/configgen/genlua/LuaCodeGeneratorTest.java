package configgen.genlua;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lua 生成器产物内容测试：重点是 map 字符串键的合法性与转义。
 */
class LuaCodeGeneratorTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void generate_mapStringKey_onlyIdentifierKeysBareOthersBracketed() throws IOException {
        // map键会拼进 {...} 构造器的 `k = v` 位置：合法标识符裸输出，
        // 含空格/点/连字符、关键字、纯数字的键必须用 ["..."] 括号字符串形式
        String cfgStr = """
                table t[id] {
                    id:int;
                    m:map<str,int> (pack);
                }
                """;
        // pack格式：k1,v1,k2,v2 平铺逗号；含逗号的单元格按csv规则加引号
        String csv = """
                ID,映射
                id,m
                1,"normal,1,a b,2,123,3,break,4,a.b,5,a-b,6"
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("t", csv));
        Path outDir = tempDir.resolve("luaout");
        new LuaCodeGenerator(new ParameterParser("lua,dir:" + outDir + ",pkg:cfgdata")).generate(ctx);

        String content = Files.readString(outDir.resolve("cfgdata/t.lua"));

        assertTrue(content.contains("normal = 1"), "合法标识符键应裸输出");
        assertTrue(content.contains("[\"a b\"] = 2"), "含空格的键必须用括号字符串形式");
        assertTrue(content.contains("[\"123\"] = 3"), "纯数字键必须用括号字符串形式（裸输出会变成number键）");
        assertTrue(content.contains("[\"break\"] = 4"), "关键字键必须用括号字符串形式");
        assertTrue(content.contains("[\"a.b\"] = 5"), "含点的键必须用括号字符串形式");
        assertTrue(content.contains("[\"a-b\"] = 6"), "含连字符的键必须用括号字符串形式");

        assertFalse(content.contains("\n123 = "), "纯数字键不允许裸输出");
    }

    @Test
    void generate_stringValue_escaped() throws IOException {
        String cfgStr = """
                table t[id] {
                    id:int;
                    s:str;
                }
                """;
        // 值含引号与反斜杠（csv内引号按规则双写），lua侧需转义为合法字符串字面量
        String csv = """
                ID,字符串
                id,s
                1,"say ""hi""\\path"
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("t", csv));
        Path outDir = tempDir.resolve("luaout2");
        new LuaCodeGenerator(new ParameterParser("lua,dir:" + outDir + ",pkg:cfgdata")).generate(ctx);

        String content = Files.readString(outDir.resolve("cfgdata/t.lua"));
        assertTrue(content.contains("\"say \\\"hi\\\"\\\\path\""), "字符串值应正确转义引号与反斜杠");
    }

    @Test
    void generate_supportFilesCreated() throws IOException {
        String cfgStr = """
                table t[id] {
                    id:int;
                    s:str;
                }
                """;
        String csv = """
                ID,字符串
                id,s
                1,hello
                """;
        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("t", csv));
        Path outDir = tempDir.resolve("luaout3");
        new LuaCodeGenerator(new ParameterParser("lua,dir:" + outDir + ",pkg:cfgdata")).generate(ctx);

        assertTrue(Files.exists(outDir.resolve("cfgdata/_cfgs.lua")), "应生成_cfgs.lua");
        assertTrue(Files.exists(outDir.resolve("cfgdata/_beans.lua")), "应生成_beans.lua");
        assertTrue(Files.exists(outDir.resolve("cfgdata/t.lua")), "应生成表的lua文件");
    }
}
