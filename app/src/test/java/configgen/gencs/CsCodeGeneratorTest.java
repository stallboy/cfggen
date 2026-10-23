package configgen.gencs;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.CliException;
import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * C# 生成器冒烟：生成成功 + 关键产物存在 + 内容含预期类型。
 */
class CsCodeGeneratorTest {

    private @TempDir Path tempDir;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void minusEditorPreservesInterfaceReferencesAndExcludesTaggedFields(boolean editorFields) throws IOException {
        Files.createDirectory(tempDir.resolve("td"));
        String editorTag = editorFields ? "(editor)" : "";
        Context ctx = TestCtx.newContext(tempDir, """
                table td.buff[id] { id:int; }
                table td.damagetype[id] { id:int; }
                table td.hero[id] { id:int; }
                interface td.Action {
                    struct ApplyBuff {
                        buff:int ->buff;
                        damageType:int ->damagetype;
                        hero:int ->hero;
                        editorBuff:int ->buff %s;
                        note:str %s;
                    }
                }
                """.formatted(editorTag, editorTag), Map.of(
                "td/buff", "id\nid\n1\n",
                "td/damagetype", "id\nid\n1\n",
                "td/hero", "id\nid\n1\n"));
        Path fullOutput = tempDir.resolve("full");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + fullOutput)).generate(ctx);
        String fullCode = Files.readString(fullOutput.resolve("Config/Td/Action/DApplyBuff.cs"));
        assertTrue(fullCode.contains("int EditorBuff {"));
        assertTrue(fullCode.contains("RefEditorBuff {"));
        assertTrue(fullCode.contains("string Note {"));

        Path filteredOutput = tempDir.resolve("filtered");
        new CsCodeGenerator(new ParameterParser("cs,own:-editor,dir:" + filteredOutput)).generate(ctx);
        String filteredCode = Files.readString(filteredOutput.resolve("Config/Td/Action/DApplyBuff.cs"));

        for (String field : java.util.List.of("Buff", "DamageType", "Hero")) {
            assertTrue(filteredCode.contains("int " + field + " {"), field);
            assertTrue(filteredCode.contains("Ref" + field + " {"), "Ref" + field);
        }
        assertEquals(!editorFields, filteredCode.contains("int EditorBuff {"));
        assertEquals(!editorFields, filteredCode.contains("RefEditorBuff {"));
        assertEquals(!editorFields, filteredCode.contains("string Note {"));
        if (!editorFields) {
            assertEquals(fullCode, filteredCode);
        }
    }

    @Test
    void extensionsDoNotAddResolveMethodsOrRegistrationState() throws Exception {
        var ctx = TestCtx.newContext(tempDir, """
                struct key { id:int ->unit; }
                struct plain { value:int; }
                struct bundle { values:list<plain>; }
                table unit[id] { id:int (seq); name:str; [name]; }
                table holder[id] { id:int; keys:list<key> (pack); }
                """, Map.of("unit", "id,name\nid,name\n0,zero\n", "holder", "id,keys\nid,keys\n1,(0)\n"));
        Path output = tempDir.resolve("out");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + output)).generate(ctx);
        try (var files = Files.walk(output)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".cs")).toList()) {
                String code = Files.readString(path);
                for (String old : java.util.List.of("SimpleIssueHandler", "IIssueHandler", "SetRowContext", "_collectExtensions", "TakeExtensions", "reader.Capture(", "registered", "AddExtension", "List<IConfigExtension>", "catch (", "try {")) {
                    assertFalse(code.contains(old), path + ": " + old);
                }
            }
        }
        String loader = Files.readString(output.resolve("Config/Loader.cs"));
        assertTrue(loader.contains("sealed class LoadErrors"));
        assertTrue(loader.contains("record MissingReference("));
        assertTrue(loader.contains("private void Add(LoadError error)"));
        String module = Files.readString(output.resolve("Config/_loaders/_rootLoader.cs"));
        assertEquals(2, module.lines().filter(line -> line.contains("void _resolve(")).count());
        String processor = Files.readString(output.resolve("Config/Processor.cs"));
        assertFalse(processor.contains("Ready(global::Config.DPlain "));
        assertTrue(processor.contains("Ready(global::Config.DBundle "));
    }

    @Test
    void usingDirectivesFollowUsageAndImplicitUsingsOption() throws Exception {
        var ctx = TestCtx.newContext(tempDir, """
                struct point { x:int; }
                interface shape { struct none { } }
                table unit[id] { id:int; }
                """, Map.of("unit", "id\nid\n1\n"));
        for (boolean implicit : java.util.List.of(false, true)) {
            Path output = tempDir.resolve(implicit ? "implicit" : "explicit");
            new CsCodeGenerator(new ParameterParser("cs,dir:" + output + (implicit ? ",implicitUsings" : ""))).generate(ctx);
            Path config = output.resolve("Config");
            assertFalse(Files.readString(config.resolve("DPoint.cs")).contains("using "));
            assertFalse(Files.readString(config.resolve("DShape.cs")).contains("using "));
            String unit = Files.readString(config.resolve("DUnit.cs"));
            assertFalse(unit.contains("using System;"));
            org.junit.jupiter.api.Assertions.assertEquals(!implicit, unit.contains("using System.Collections.Generic;"));
            for (String file : java.util.List.of("Loader.cs", "Processor.cs", "ConfigMgr.cs", "ConfigPackage.cs")) {
                String code = Files.readString(config.resolve(file));
                org.junit.jupiter.api.Assertions.assertEquals(!implicit, code.contains("using System;"), file);
                org.junit.jupiter.api.Assertions.assertEquals(!implicit, code.contains("using System.Collections.Generic;"), file);
            }
            String pack = Files.readString(config.resolve("ConfigPackage.cs"));
            org.junit.jupiter.api.Assertions.assertEquals(!implicit, pack.contains("using System.IO;"));
            assertTrue(pack.contains("using System.Text;"));
        }
    }

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void generate_simpleTable() throws IOException {
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;
        String csv = """
                用户ID,名字
                id,name
                1,Alice
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("user", csv));
        Path outDir = tempDir.resolve("csout");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + outDir)).generate(ctx);

        Path configDir = outDir.resolve("Config");
        Path userCs = configDir.resolve("DUser.cs");
        assertTrue(Files.exists(userCs), "应生成Config/DUser.cs（默认前缀D、pkg默认Config）");
        String content = Files.readString(userCs);
        assertTrue(content.contains("DUser"), "应包含类型名DUser");
        assertTrue(content.contains("Name"), "应包含字段Name");

        assertTrue(Files.exists(configDir.resolve("Loader.cs")), "应生成Config/Loader.cs");
        String processor = Files.readString(configDir.resolve("Processor.cs"));
        assertFalse(processor.contains("\"default\""));
        assertFalse(processor.contains("params string[] languages"));
        assertTrue(processor.contains("pack.Block(block)"));
        assertFalse(Files.readString(configDir.resolve("ConfigMgr.cs")).contains("switch"));
    }

    @Test
    void zeroAndOneBranchMappingsDoNotGenerateSwitches() throws Exception {
        var ctx = TestCtx.newContext(tempDir, """
                struct point { value:int; }
                interface one { struct node { point:point; } }
                table rank[id] (enum='name') { id:int; name:str; }
                table empty[id] (enum='name') { id:int; name:str; }
                table entry[id] (entry='name') { id:int; name:str; }
                """, Map.of("rank", "id,name\nid,name\n1,Green\n", "empty", "id,name\nid,name\n", "entry", "id,name\nid,name\n1,Home\n"));
        Path output = tempDir.resolve("out");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + output)).generate(ctx);
        assertFalse(Files.readString(output.resolve("Config/_loaders/_rootLoader.cs")).contains("switch"));
        assertFalse(Files.readString(output.resolve("Config/Processor.cs")).contains("switch"));
    }

    @Test
    void generate_enumTable() throws IOException {
        String cfgStr = """
                table ability[id] (enum='name') {
                    id:int;
                    name:str;
                }
                """;
        String csv = """
                技能ID,技能名称
                id,name
                1,Fireball
                2,IceSpike
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("ability", csv));
        Path outDir = tempDir.resolve("csout_enum");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + outDir)).generate(ctx);

        Path abilityCs = outDir.resolve("Config/DAbility.cs");
        assertTrue(Files.exists(abilityCs), "应生成Config/DAbility.cs");
        String content = Files.readString(abilityCs);
        assertTrue(content.contains("Fireball"), "枚举值应写入生成的枚举类型");
        assertTrue(content.contains("IceSpike"), "枚举值应写入生成的枚举类型");
    }

    @Test
    void constructor_invalidPrefix_throws() {
        // 非法前缀应 fail-fast；C# 标识符不允许 $ 和连字符、数字不能开头
        assertThrows(CliException.class, () -> new CsCodeGenerator(new ParameterParser("cs,prefix:1a")));
        assertThrows(CliException.class, () -> new CsCodeGenerator(new ParameterParser("cs,prefix:my-c")));
        assertThrows(CliException.class, () -> new CsCodeGenerator(new ParameterParser("cs,prefix:$x")));
        assertDoesNotThrow(() -> new CsCodeGenerator(new ParameterParser("cs,prefix:D")));
        assertDoesNotThrow(() -> new CsCodeGenerator(new ParameterParser("cs,prefix:_Cfg")));
    }
}
