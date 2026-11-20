package configgen.schema.cfg;

import configgen.Resources;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemas;
import configgen.util.CachedFiles;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CfgReaderWriterTest {

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    public void readWriteReadEqual() {
        String cfgStr = Resources.readResourceFile("config1.cfg");
        CfgSchema cfg1 = CfgReader.parse(cfgStr);
        String cfgStr2 = CfgWriter.stringify(cfg1);
        CfgSchema cfg2 = CfgReader.parse(cfgStr2);

        boolean equals = cfg1.equals(cfg2);

        if (!equals) {
            cfg1.printDiff(cfg2);
        }

        assertEquals(cfg1, cfg2);
    }

    @Test
    void preservesFileEndCommentsWhenSeparatingModules(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("nested.cfg");
        Files.writeString(source, "struct Item { id:int; }\n// End of nested module\n");
        CfgSchema schema = CfgReader.INSTANCE.readToSchema(source, "nested.");

        assertEquals("End of nested module", schema.getFileEndComment("nested"));
        CfgSchema nested = CfgUtil.separate(schema).get("nested");
        assertTrue(CfgWriter.stringify(nested).endsWith("// End of nested module\n"));

        Files.writeString(source, "// Comment-only module\n");
        CfgSchema commentOnly = CfgReader.INSTANCE.readToSchema(source, "nested.");
        assertEquals("// Comment-only module\n",
                CfgWriter.stringify(CfgUtil.separate(commentOnly).get("nested")));
    }

    @Test
    void writesCommentOnlyModuleToDirectoryWhenRootFileHasSameCodeName(@TempDir Path dir)
            throws IOException {
        Path root = dir.resolve("config.cfg");
        Files.writeString(root, "//Root notes\n");
        Path module = Files.createDirectory(dir.resolve("config_模块")).resolve("config.cfg");
        Files.writeString(module, "//Module notes\n");

        CfgSchema schema = CfgSchemas.readFromRoot(root);
        CfgSchemas.writeToDir(root, schema, new CachedFiles());

        assertEquals("// Root notes\n", Files.readString(root));
        assertEquals("// Module notes\n", Files.readString(module));
    }

    @Test
    void cfgTypesAndNumbersRoundTripUnderNonDefaultLocales() {
        Locale previous = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            for (String locale : new String[]{"tr-TR", "ar-EG"}) {
                Locale.setDefault(Locale.forLanguageTag(locale));
                CfgSchema schema = CfgReader.parse("""
                        struct Item { id:int; costs:list<float> (fix=3); }
                        enum Status { Initial=1; }
                        """);
                assertEquals(schema, CfgReader.parse(CfgWriter.stringify(schema)));
            }
        } finally {
            Locale.setDefault(previous);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }

    @Test
    public void enumCommentTextTagRoundTrips() {
        // commentText tag要原样写回，丢失会导致重读后comment类型退回str
        String cfgStr = """
                enum ArgCaptureMode (commentText) {
                    Snapshot; // 快照模式
                }
                """;
        CfgSchema cfg1 = CfgReader.parse(cfgStr);
        String out = CfgWriter.stringify(cfg1);
        assertTrue(out.contains("(commentText)"), "写回的enum声明应保留commentText tag:\n" + out);

        CfgSchema cfg2 = CfgReader.parse(out);
        assertEquals(cfg1, cfg2);
    }


}
