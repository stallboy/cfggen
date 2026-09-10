package configgen.schema.cfg;

import configgen.Resources;
import configgen.schema.CfgSchema;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
