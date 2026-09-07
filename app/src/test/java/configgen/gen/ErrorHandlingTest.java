package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingTest {

    @Test
    void error_ShouldHandleInvalidGeneratorParameters() {
        // Given: 无效的生成器参数
        String arg = "java,unknown=param";

        // When & Then: 验证参数验证失败（命令行使用错误，非程序断言失败）
        ParameterParser parser = new ParameterParser(arg);
        assertThrows(CliException.class, parser::assureNoExtra);
    }

    @Test
    void error_ShouldHandleEmptyGeneratorName() {
        // Given: 空的生成器名称
        String arg = "";

        // When: 解析参数
        ParameterParser parser = new ParameterParser(arg);

        // Then: 验证空名称处理
        assertEquals("", parser.id());
    }
}