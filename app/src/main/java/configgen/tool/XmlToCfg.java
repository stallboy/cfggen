package configgen.tool;


import configgen.ctx.DirectoryStructure;
import configgen.schema.CfgSchema;
import configgen.schema.cfg.Cfgs;

import java.nio.file.Path;

public class XmlToCfg {
    public static void convertAndCheck(Path dataDir) {
        CfgSchema cfg = Cfgs.readXmlFromRootDir(dataDir);
        Path cfgPath = dataDir.resolve(DirectoryStructure.ROOT_CONFIG_FILENAME);
        Cfgs.writeTo(cfgPath, true, cfg);

        DirectoryStructure sourceStructure = new DirectoryStructure(dataDir);
        CfgSchema cfg2 = Cfgs.readFromDir(sourceStructure);
        Cfgs.writeTo(cfgPath, true, cfg2);
        CfgSchema cfg3 = Cfgs.readFromDir(sourceStructure);

        if (!cfg2.equals(cfg3)) {
            throw new IllegalStateException("should equal");
        }

        cfg2.resolve().checkErrors();
    }
}
