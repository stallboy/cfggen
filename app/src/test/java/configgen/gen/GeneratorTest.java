package configgen.gen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldTransformString_whenUpper1MethodCalled() {
        // Given: 测试字符串
        String input = "hello";

        // When: 调用字符串转换方法
        String result = Generator.upper1(input);

        // Then: 验证转换结果
        assertEquals("Hello", result);
    }

    @Test
    void shouldTransformString_whenLower1MethodCalled() {
        // Given: 测试字符串
        String input = "Hello";

        // When: 调用字符串转换方法
        String result = Generator.lower1(input);

        // Then: 验证转换结果
        assertEquals("hello", result);
    }
}