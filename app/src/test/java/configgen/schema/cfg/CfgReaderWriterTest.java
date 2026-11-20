package configgen.schema.cfg;

import configgen.Resources;
import configgen.schema.CfgSchema;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
