package configgen.gengo;

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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Go 生成器测试。Go 规范要求源码 UTF-8——回归：默认编码曾是 GBK，
 * 含中文注释的表会生成无法编译的 .go 文件。
 */
class GoCodeGeneratorTest {

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
    void generate_defaultEncodingIsUtf8WithChineseComment() throws IOException {
        String cfgStr = """
                table user[id] {
                    id:int; // 主键
                    name:str; // 名字
                }
                """;
        String csv = """
                用户ID,名字
                id,name
                1,Alice
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("user", csv));
        Path outDir = tempDir.resolve("goout");
        // 不传encoding，验证默认值为UTF-8
        new GoCodeGenerator(new ParameterParser("go,dir:" + outDir + ",pkg:config")).generate(ctx);

        Path userGo;
        try (Stream<Path> files = Files.walk(outDir)) {
            userGo = files.filter(p -> p.getFileName().toString().equals("user.go"))
                    .findFirst().orElseThrow(() -> new AssertionError("应生成user.go"));
        }

        // 严格解码：任何非法UTF-8字节序列直接失败，而不是被替换字符掩盖
        byte[] bytes = Files.readAllBytes(userGo);
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            fail("生成的.go不是合法UTF-8（默认编码曾是GBK）: " + e);
            return;
        }
        // 注释来自schema对齐后的字段注释（csv表头中文名）
        assertTrue(content.contains("//用户ID"), "中文注释应保留");
        assertTrue(content.contains("//名字"), "中文注释应保留");
    }
}
