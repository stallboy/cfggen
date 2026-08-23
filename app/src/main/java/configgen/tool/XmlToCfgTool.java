package configgen.tool;


import configgen.ctx.DirectoryStructure;
import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemas;
import configgen.util.CachedFiles;
import configgen.schema.cfg.XmlReader;

import java.nio.file.Path;

public class XmlToCfgTool extends Tool {

    private final Path dataDir;

    public XmlToCfgTool(Parameter parameter) {
        super(parameter);
        dataDir = Path.of(parameter.get("datadir", "."));
    }

    @Override
    public void call() {
        CfgSchema cfg = XmlReader.readFromDir(dataDir);
        Path cfgPath = dataDir.resolve(DirectoryStructure.ROOT_CONFIG_FILENAME);
        // 独立tool无Context，自建一份登记册（tool只写config.cfg，不参与生成目录的清理协议）
        CachedFiles cachedFiles = new CachedFiles();
        CfgSchemas.writeToDir(cfgPath, cfg, cachedFiles);

        DirectoryStructure sourceStructure = new DirectoryStructure(dataDir);
        CfgSchema cfg2 = CfgSchemas.readFromDir(sourceStructure.getCfgFiles());
        CfgSchemas.writeToDir(cfgPath, cfg2, cachedFiles);
        CfgSchema cfg3 = CfgSchemas.readFromDir(sourceStructure.getCfgFiles());

        if (!cfg2.equals(cfg3)) {
            throw new IllegalStateException("should equal");
        }

        cfg2.resolve().checkErrors();
    }
}
