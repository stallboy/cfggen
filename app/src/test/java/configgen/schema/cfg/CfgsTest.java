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
            System.out.println("=== Schema Differences Detected ===");
            System.out.println("Original schema hash: " + cfg1.hashCode());
            System.out.println("Deserialized schema hash: " + cfgFromAllSubDir.hashCode());
            System.out.println("Original schema item count: " + cfg1.items().size());
            System.out.println("Deserialized schema item count: " + cfgFromAllSubDir.items().size());

            // 打印详细的差异信息
            cfg1.printDiff(cfgFromAllSubDir);

            // 打印前几个项目的详细信息
            System.out.println("\n=== First few items comparison ===");
            int maxItemsToCompare = Math.min(5, Math.min(cfg1.items().size(), cfgFromAllSubDir.items().size()));
            for (int i = 0; i < maxItemsToCompare; i++) {
                System.out.println("Item " + i + " original: " + cfg1.items().get(i).name());
                System.out.println("Item " + i + " deserialized: " + cfgFromAllSubDir.items().get(i).name());
            }
        }

        assertEquals(cfg1, cfgFromAllSubDir);
        assertEquals(cfgFromAllSubDir, cfgFromOneFile);
    }

}
