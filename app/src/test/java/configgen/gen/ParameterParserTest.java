package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParameterParserTest {

    @Test
    void shouldParseSingleParameter_whenNoAdditionalParameters() {
        // Given: 简单的生成器参数
        String arg = "java";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证解析结果
        assertEquals("java", parser.id());
        assertEquals("default", parser.get("unknown", "default"));
    }

    @Test
    void shouldThrowException_whenExtraParametersNotConsumed() {
        // Given: 带额外参数的生成器参数
        String arg = "java,output=src";

        // When & Then: 验证抛出异常（命令行使用错误，非程序断言失败）
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(CliException.class, parser::assureNoExtra);
    }

    @Test
    void shouldParseMultipleParameters_whenCommaSeparated() {
        // Given: 带多个参数的生成器参数
        String arg = "java,output=src,verbose=true,encoding=utf8";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证参数解析
        assertEquals("java", parser.id());
        assertEquals("src", parser.get("output", ""));
        assertEquals("true", parser.get("verbose", ""));
        assertEquals("utf8", parser.get("encoding", ""));
    }

    @Test
    void shouldHandleBooleanParameters_whenHasMethodCalled() {
        // Given: 带布尔参数的生成器参数
        String arg = "java,verbose";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证布尔参数处理
        assertTrue(parser.has("verbose"));
    }

    @Test
    void shouldParseExplicitBooleanValue() {
        ParameterParser parser = new ParameterParser("java,verbose=false");
        assertFalse(parser.has("verbose"));
        assertTrue(new ParameterParser("java,verbose=true").has("verbose"));
    }

    @Test
    void shouldThrow_whenBooleanValueInvalid() {
        // yes/ok/ture这类垃圾值原来被Boolean.parseBoolean静默当false，必须报错
        ParameterParser parser = new ParameterParser("java,verbose=yes");
        assertThrows(CliException.class, () -> parser.has("verbose"));
    }

    @Test
    void error_ShouldThrowException_whenExtraParametersNotConsumed() {
        // Given: 带未使用参数的生成器参数
        String arg = "java,output=src,unknown=value";

        // When & Then: 验证抛出异常（命令行使用错误，非程序断言失败）
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(CliException.class, parser::assureNoExtra);
    }
}