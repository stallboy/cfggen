package configgen.schema.cfg;

import configgen.Resources;
import configgen.ctx.DirectoryStructure;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CfgsTest {

    @Test
    public void readWriteReadEqual() {
        String cfgStr = Resources.readResourceFile("config1.cfg");
        CfgSchema cfg1 = CfgReader.parse(cfgStr);
        String cfgStr2 = CfgWriter.stringify(cfg1);
        CfgSchema cfg2 = CfgReader.parse(cfgStr);

        boolean equals = cfg1.equals(cfg2);
        System.out.println(equals);
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

        CfgSchemas.writeTo(path, true, cfg1);
        CfgSchemas.writeTo(path1, false, cfg1);
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
