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

        // When & Then: 验证抛出异常
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(AssertionError.class, parser::assureNoExtra);
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
    void error_ShouldThrowException_whenExtraParametersNotConsumed() {
        // Given: 带未使用参数的生成器参数
        String arg = "java,output=src,unknown=value";

        // When & Then: 验证抛出异常
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(AssertionError.class, parser::assureNoExtra);
    }
}