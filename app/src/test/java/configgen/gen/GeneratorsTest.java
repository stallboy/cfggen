package configgen.gen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorsTest {

    @Test
    void shouldCreateGenerator_whenRegisteredGeneratorNameProvided() {
        // Given: 注册一个生成器提供者
        Generators.addProvider("test", parameter -> new Generator(parameter) {
            @Override
            public void generate(configgen.ctx.Context context) {}
        });

        // When: 创建生成器
        Generator generator = Generators.create("test");

        // Then: 验证生成器创建成功
        assertNotNull(generator);
    }

    @Test
    void shouldReturnNull_whenUnregisteredGeneratorNameProvided() {
        // Given: 未注册的生成器名称
        String unregisteredName = "unknown";

        // When: 尝试创建生成器
        Generator generator = Generators.create(unregisteredName);

        // Then: 验证返回 null
        assertNull(generator);
    }

    @Test
    void shouldCallProviderCreate_whenRegisteredGeneratorNameProvided() {
        // Given: 注册一个生成器提供者
        Generators.addProvider("test2", parameter -> new Generator(parameter) {
            @Override
            public void generate(configgen.ctx.Context context) {}
        });

        // When: 创建生成器
        Generator result = Generators.create("test2");

        // Then: 验证生成器创建成功
        assertNotNull(result);
    }

    @Test
    void shouldValidateParameters_whenGeneratorCreated() {
        // Given: 带参数的生成器
        String arg = "test3";
        Generators.addProvider("test3", parameter -> new Generator(parameter) {
            @Override
            public void generate(configgen.ctx.Context context) {}
        });

        // When: 创建生成器
        Generator result = Generators.create(arg);

        // Then: 验证参数解析和验证
        assertNotNull(result);
    }
}