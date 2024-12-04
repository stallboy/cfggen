package configgen.tool;


import configgen.ctx.DirectoryStructure;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemas;

import java.nio.file.Path;

public class XmlToCfg {
    public static void convertAndCheck(Path dataDir) {
        CfgSchema cfg = CfgSchemas.readXmlFromRootDir(dataDir);
        Path cfgPath = dataDir.resolve(DirectoryStructure.ROOT_CONFIG_FILENAME);
        CfgSchemas.writeTo(cfgPath, true, cfg);

        DirectoryStructure sourceStructure = new DirectoryStructure(dataDir);
        CfgSchema cfg2 = CfgSchemas.readFromDir(sourceStructure);
        CfgSchemas.writeTo(cfgPath, true, cfg2);
        CfgSchema cfg3 = CfgSchemas.readFromDir(sourceStructure);

        if (!cfg2.equals(cfg3)) {
            throw new IllegalStateException("should equal");
        }

        cfg2.resolve().checkErrors();
    }
}
