package configgen.tool;


import configgen.schema.CfgSchema;
import configgen.schema.SchemaErrs;
import configgen.schema.cfg.Cfgs;

import java.nio.file.Path;

public class XmlToCfg {
    public static void convertAndCheck(Path dataDir) {
        CfgSchema cfg = Cfgs.readFromXml(dataDir.resolve("config.xml"), true);
        Path cfgPath = dataDir.resolve("config.cfg");

        Cfgs.writeTo(cfgPath, true, cfg);
        CfgSchema cfg2 = Cfgs.readFrom(cfgPath, true);

        Cfgs.writeTo(cfgPath, true, cfg2);
        CfgSchema cfg3 = Cfgs.readFrom(cfgPath, true);

        if (!cfg2.equals(cfg3)) {
            throw new IllegalStateException("should equal");
        }

        SchemaErrs fullErr = cfg2.resolve();
        fullErr.print("schema");
    }
}
