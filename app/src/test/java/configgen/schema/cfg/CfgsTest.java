package configgen.schema.cfg;

import configgen.Resources;
import configgen.ctx.DirectoryStructure;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemas;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CfgsTest {

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
        CfgSchema cfg2 = CfgReader.parse(cfgStr);

        boolean equals = cfg1.equals(cfg2);

        if (!equals) {
            cfg1.printDiff(cfg2);
        }

        assertEquals(cfg1, cfg2);
        assertEquals(cfgStr, cfgStr2);
    }


    @Test
    public void readWriteSeparateReadEqual(@TempDir Path tempFolder) {
        String cfgStr = Resources.readResourceFile("config1.cfg");
        CfgSchema cfg1 = CfgReader.parse(cfgStr);

        Path path = tempFolder.resolve("config.cfg");
        Path path1 = tempFolder.resolve("config1.cfg");

        CfgSchemas.writeToDir(path, cfg1);
        CfgSchemas.writeToOneFile(path1, cfg1);
        CfgSchema cfgFromAllSubDir = CfgSchemas.readFromDir(new DirectoryStructure(tempFolder));
        CfgSchema cfgFromOneFile = CfgSchemas.readFromOneFile(path1);

        boolean equals = cfg1.equals(cfgFromAllSubDir);
        if (!equals) {
            cfg1.printDiff(cfgFromAllSubDir);
        }

        assertEquals(cfg1, cfgFromAllSubDir);
        assertEquals(cfgFromAllSubDir, cfgFromOneFile);
    }

}
