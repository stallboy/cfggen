package configgen.schema;

import configgen.ctx.DirectoryStructure;
import configgen.schema.cfg.CfgReader;
import configgen.schema.cfg.CfgUtil;
import configgen.schema.cfg.CfgWriter;
import configgen.util.CachedFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static configgen.ctx.DirectoryStructure.*;

public class CfgSchemas {

    public static CfgSchema readFromDir(DirectoryStructure sourceStructure) {
        CfgSchema destination = CfgSchema.of();
        for (CfgFileInfo c : sourceStructure.getCfgFiles().values()) {
            CfgReader.INSTANCE.readTo(destination, c.path(), c.pkgNameDot());
        }
        return destination;
    }

    public static CfgSchema readFromOneFile(Path filePath) {
        CfgSchema destination = CfgSchema.of();
        CfgReader.INSTANCE.readTo(destination, filePath, "");
        return destination;
    }

    public static void writeToDir(Path destination, CfgSchema root) {
        Path absoluteDst = destination.toAbsolutePath().normalize();
        Map<String, CfgSchema> cfgs = CfgUtil.separate(root);
        for (Map.Entry<String, CfgSchema> entry : cfgs.entrySet()) {
            String ns = entry.getKey();
            CfgSchema cfg = entry.getValue();
            Path dst = CfgUtil.getCfgFilePathByNamespace(ns, absoluteDst);
            writeToOneFile(dst, cfg, true);
        }
    }

    public static void writeToOneFile(Path dst, CfgSchema cfg) {
        writeToOneFile(dst, cfg, false);

    }

    private static void writeToOneFile(Path dst, CfgSchema cfg, boolean useLastName) {
        String content = CfgWriter.stringify(cfg, useLastName, false);
        try {
            CachedFiles.writeFile(dst, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
