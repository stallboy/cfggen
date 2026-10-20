package configgen.genbytes;

import configgen.TestCtx;
import configgen.gen.ParameterParser;
import configgen.genjava.ConfigInput;
import configgen.genjava.ConfigPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiLanguagePackageTest {
    @TempDir Path root;

    @Test
    void separatedLanguagesAttachByPackageAndReadInRequestedLanguageOrder() throws Exception {
        Path data = Files.createDirectories(root.resolve("data"));
        Path languages = Files.createDirectories(root.resolve("languages"));
        Files.writeString(languages.resolve("en.csv"), "t,你好,hello\n");
        var ctx = TestCtx.newLangSwitchContext(data, "table t[id] { id:int; text:text; }",
                Map.of("t", "id,text\nid,text\n1,你好\n"), languages, "zh_cn");
        Path out = root.resolve("out");
        new BytesGenerator(new ParameterParser("bytes,pack:base,langSeparated,dir:" + out)).generate(ctx);
        var base = ConfigPackage.read(new ConfigInput(out.resolve("config.bytes")));
        var english = ConfigPackage.read(new ConfigInput(out.resolve("en.bytes")));
        assertFalse(base.blockNames().contains("text/en"));
        var combined = base.attach(english);
        Path generated = root.resolve("java");
        new configgen.genjava.code.JavaCodeGenerator(new ParameterParser("java,pkg:generatedtext,dir:" + generated)).generate(ctx);
        Path textSource = generated.resolve("generatedtext/Text.java");
        assertFalse(Files.readString(textSource).contains("readTextsInPool()"));
        Path classes = Files.createDirectories(root.resolve("text-classes"));
        String runtimeClasses = Path.of(ConfigInput.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        assertEquals(0, javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-encoding", "UTF-8", "-classpath", runtimeClasses, "-d", classes.toString(), textSource.toString()));
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            var textClass = loader.loadClass("generatedtext.Text");
            var input = combined.block(".t", "zh_cn", "en");
            assertEquals(1, input.readInt()); assertEquals(1, input.readInt());
            var text = textClass.getMethod("_create", ConfigInput.class).invoke(null, input);
            assertEquals("你好", textClass.getMethod("getZh_cn").invoke(text));
            assertEquals("hello", textClass.getMethod("getEn").invoke(text));
            input.requireEnd();
        }
        assertFalse(base.blockNames().contains("text/en"), "Attaching does not mutate a shared base package");
        String[] selectedLanguages = {"en", "zh_cn"};
        var table = combined.block(".t", selectedLanguages);
        selectedLanguages[0] = "changed";
        var otherTable = combined.block(".t", "zh_cn", "en");
        assertEquals(1, table.readInt()); assertEquals(1, table.readInt());
        assertArrayEquals(new String[]{"hello", "你好"}, table.readTextsInPool());
        table.requireEnd();
        assertEquals(1, otherTable.readInt()); assertEquals(1, otherTable.readInt());
        assertArrayEquals(new String[]{"你好", "hello"}, otherTable.readTextsInPool());
        otherTable.requireEnd();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<String[]>>();
            for (int i = 0; i < 12; i++) tasks.add(() -> {
                var input = combined.block(".t", "en", "zh_cn");
                assertEquals(1, input.readInt()); assertEquals(1, input.readInt());
                var texts = input.readTextsInPool(); input.requireEnd();
                return texts;
            });
            for (var result : executor.invokeAll(tasks)) assertArrayEquals(new String[]{"hello", "你好"}, result.get());
        }
        var defaultTable = combined.block(".t");
        assertEquals(1, defaultTable.readInt()); assertEquals(1, defaultTable.readInt());
        assertEquals("你好", defaultTable.readTextInPool());
        defaultTable.requireEnd();
        assertThrows(RuntimeException.class, () -> combined.attach(english));
    }
}
