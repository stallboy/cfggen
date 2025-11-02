package configgen.gen;

import configgen.Resources;
import configgen.util.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path tempDstDir;

    @Test
    void shouldPrintUsage_whenNoArgumentsProvided() {
        // Given: 无参数
        String[] args = {};

        // When: 执行main0
        int result = Main.main0(args);

        // Then: 验证返回非零状态码
        assertEquals(1, result);
    }

    @Test
    void shouldPrintUsage_whenUnknownArgumentProvided() {
        // Given: 未知参数
        String[] args = {"-unknown"};

        // When: 执行main0
        int result = Main.main0(args);

        // Then: 验证返回非零状态码
        assertEquals(1, result);
    }

    @Test
    void shouldPrintUsage_whenMissingDataDir() {
        // Given: 缺少-datadir参数
        String[] args = {"-gen", "java"};

        // When: 执行main0
        int result = Main.main0(args);

        // Then: 验证返回非零状态码
        assertEquals(1, result);
    }

    @Test
    void shouldHandleVerify_whenValidDataDirProvided() {
        // Given: 有效的验证参数
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行验证
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8", "-verify"};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleGenerator_whenValidGeneratorAndDataDirProvided() {
        // Given: 有效的生成器和数据目录
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8",
                "-gen", "java,dir:" + tempDstDir.toString()};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleMultipleGenerators_whenMultipleGenFlagsProvided() {
        // Given: 多个生成器参数
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行多个生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8",
                "-gen", "java,dir:" + tempDstDir.toString(),
                "-gen", "javadata,file:" + tempDstDir.resolve("config.data").toString()};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleVerboseOptions_whenVerboseFlagsProvided() {
        // Given: 详细输出选项
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行带详细输出的生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8", "-v",
                "-gen", "java,dir:" + tempDstDir.toString()};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
        assertEquals(1, Logger.verboseLevel());
    }

    @Test
    void shouldHandleVeryVerboseOptions_whenVeryVerboseFlagProvided() {
        // Given: 非常详细输出选项
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行带非常详细输出的生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8", "-vv",
                "-gen", "java,dir:" + tempDstDir.toString()};
        int result = Main.main0(args);

        assertEquals(2, Logger.verboseLevel());
        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleProfileOptions_whenProfileFlagsProvided() {
        // Given: 性能分析选项
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行带性能分析的生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8", "-p",
                "-gen", "java,dir:" + tempDstDir.toString()};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleWarningOptions_whenWarningFlagsProvided() {
        // Given: 警告选项
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行带警告选项的生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8", "-nowarn",
                "-gen", "java,dir:" + tempDstDir.toString()};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleHeadRowOption_whenHeadRowProvided() {
        // Given: 自定义表头行选项
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行带自定义表头行的生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8", "-headrow", "2",
                "-gen", "java,dir:" + tempDstDir.toString()};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleEncodingOption_whenEncodingProvided() {
        // Given: 自定义编码选项
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行带自定义编码的生成器
        String[] args = {"-datadir", tempDir.toString(), "-encoding", "UTF-8",
                "-gen", "java,dir:" + tempDstDir.toString()};
        int result = Main.main0(args);

        // Then: 验证成功执行
        assertEquals(0, result);
    }

    @Test
    void shouldHandleInvalidGenerator_whenUnknownGeneratorProvided() {
        // Given: 未知生成器
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 执行未知生成器
        String[] args = {"-datadir", tempDir.toString(), "-gen", "unknown"};
        int result = Main.main0(args);

        // Then: 验证返回非零状态码
        assertEquals(1, result);
    }

}