package configgen.gen;

import configgen.util.CachedFiles;

import configgen.gengo.GoCodeGenerator;
import configgen.util.Logger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class GeneratorsTest {

    @Test
    void bytesRejectsRemovedFormatAndSchemaOptions() {
        for (String option : new String[]{"format:legacy", "format:blocks", "schema"}) {
            var parameter = new ParameterParser("bytes," + option);
            new configgen.genbytes.BytesGenerator(parameter);
            assertThrows(CliException.class, parameter::assureNoExtra);
        }
    }

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void shouldRejectEncodingParam_forGoGenerator() {
        // Go规范强制源码UTF-8，encoding参数已移除：传入应报unsupported parameter
        ParameterParser ok = new ParameterParser("go,dir:.");
        new GoCodeGenerator(ok);
        assertDoesNotThrow(ok::assureNoExtra, "合法参数应全部被消费");

        ParameterParser bad = new ParameterParser("go,dir:.,encoding:GBK");
        new GoCodeGenerator(bad);
        assertThrows(CliException.class, bad::assureNoExtra,
                "encoding参数已移除，传入必须报错而非静默忽略");
    }

    @Test
    void shouldCreateGenerator_whenRegisteredGeneratorNameProvided() {
        // Given: 注册一个生成器提供者
        Generators.addProvider("test", parameter -> new Generator(parameter) {
            @Override
            public void generate(configgen.ctx.Context context, CachedFiles outputs) {}
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
    void shouldReturnTheRegisteredProvidersResult() {
        Generator expected = new Generator(new ParameterParser("identity")) {
            @Override
            public void generate(configgen.ctx.Context context, CachedFiles outputs) {}
        };
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        Generators.addProvider("identity", parameter -> {
            calls.incrementAndGet();
            return expected;
        });

        assertSame(expected, Generators.create("identity"));
        assertEquals(1, calls.get());
    }

    @Test
    void shouldConsumeKnownParametersAndRejectUnknownOnes() {
        var seen = new java.util.ArrayList<String>();
        Generators.addProvider("validated", parameter -> {
            seen.add(parameter.get("mode", "default"));
            return new Generator(parameter) {
                @Override
                public void generate(configgen.ctx.Context context, CachedFiles outputs) {}
            };
        });

        assertNotNull(Generators.create("validated,mode:fast"));
        assertEquals(java.util.List.of("fast"), seen);
        assertThrows(CliException.class, () -> Generators.create("validated,mode:fast,unknown:value"));
    }
}
