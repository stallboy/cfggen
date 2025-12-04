package configgen.tool;


import configgen.ctx.DirectoryStructure;
import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemas;
import configgen.schema.cfg.XmlReader;

import java.nio.file.Path;

public class XmlToCfg extends Tool {

    private final Path dataDir;

    public XmlToCfg(Parameter parameter) {
        super(parameter);
        dataDir = Path.of(parameter.get("datadir", "."));
    }

    @Override
    public void call() {
        CfgSchema cfg = XmlReader.readFromDir(dataDir);
        Path cfgPath = dataDir.resolve(DirectoryStructure.ROOT_CONFIG_FILENAME);
        CfgSchemas.writeToDir(cfgPath, cfg);

        DirectoryStructure sourceStructure = new DirectoryStructure(dataDir);
        CfgSchema cfg2 = CfgSchemas.readFromDir(sourceStructure);
        CfgSchemas.writeToDir(cfgPath, cfg2);
        CfgSchema cfg3 = CfgSchemas.readFromDir(sourceStructure);

        if (!cfg2.equals(cfg3)) {
            throw new IllegalStateException("should equal");
        }

        cfg2.resolve().checkErrors();
    }
}
