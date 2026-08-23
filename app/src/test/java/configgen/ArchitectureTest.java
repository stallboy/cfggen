package configgen;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 固化分层依赖方向（见 docs/01-architecture-overview.md 与 ARCHITECTURE_REVIEW §5.2）：
 *
 *   gen（含各语言生成器、write、editorserver、mcpserver、tool）
 *     -&gt; ctx -&gt; value -&gt; data -&gt; schema -&gt; util
 *     value 也可依赖 i18n（i18n 与 data 平级，仅依赖 schema/util）
 *     naming 仅依赖 schema/util（跨语言生成命名契约），位于 schema 之上，
 *     可被各 gen* 与 value 使用
 *
 * 历史上存在过四组包级循环（gen与ctx、schema与data、data与ctx、value与ctx），
 * 已分别解开；本测试防止回潮。下层对上层的 import 会让本测试失败。
 */
class ArchitectureTest {

    /// 顶层包（命令行、各语言生成器、写回、服务、工具）——最下层也不得依赖它们
    private static final String[] TOP = {
            "..configgen.gen..",
            "..configgen.genbytes..", "..configgen.genbyai..", "..configgen.gencs..",
            "..configgen.gengd..", "..configgen.genjava..", "..configgen.geni18n..",
            "..configgen.genjson..", "..configgen.genlua..", "..configgen.gengo..",
            "..configgen.gents..",
            "..configgen.write..", "..configgen.editorserver..", "..configgen.mcpserver..",
            "..configgen.tool..",
    };

    // 只检查主代码：测试类天然会跨层构造被测对象，不参与分层约束
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("configgen");

    private static String[] pkgs(String... others) {
        return Stream.concat(Arrays.stream(others), Arrays.stream(TOP)).toArray(String[]::new);
    }

    @Test
    void schema是最底层_不得依赖其他任何业务包() {
        noClasses().that().resideInAPackage("..configgen.schema..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        pkgs("..configgen.data..", "..configgen.i18n..", "..configgen.value..", "..configgen.ctx.."))
                .check(CLASSES);
    }

    @Test
    void naming仅依赖schema和util_不得依赖其他任何业务包() {
        noClasses().that().resideInAPackage("..configgen.naming..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        pkgs("..configgen.data..", "..configgen.i18n..", "..configgen.value..", "..configgen.ctx.."))
                .check(CLASSES);
    }

    @Test
    void i18n不得依赖_data_value_ctx_及顶层() {
        noClasses().that().resideInAPackage("..configgen.i18n..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        pkgs("..configgen.data..", "..configgen.value..", "..configgen.ctx.."))
                .check(CLASSES);
    }

    @Test
    void data不得依赖_value_ctx_及顶层() {
        noClasses().that().resideInAPackage("..configgen.data..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        pkgs("..configgen.value..", "..configgen.ctx.."))
                .check(CLASSES);
    }

    @Test
    void value不得依赖_ctx_及顶层() {
        noClasses().that().resideInAPackage("..configgen.value..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        pkgs("..configgen.ctx.."))
                .check(CLASSES);
    }

    @Test
    void ctx不得依赖顶层() {
        noClasses().that().resideInAPackage("..configgen.ctx..")
                .should().dependOnClassesThat().resideInAnyPackage(TOP)
                .check(CLASSES);
    }
}
