package configgen.schema.cfg;

import configgen.schema.CfgSchema;
import configgen.util.CachedFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public class Cfgs {

    public static void saveInAllSubDirectory(CfgSchema root, Path dst) {
        Path absoluteDst = dst.toAbsolutePath().normalize();
        Map<String, CfgSchema> cfgs = CfgUtil.separate(root);
        for (Map.Entry<String, CfgSchema> entry : cfgs.entrySet()) {
            String ns = entry.getKey();
            CfgSchema cfg = entry.getValue();

            Path savePath = CfgUtil.getCfgFilePathByNamespace(ns, absoluteDst);
            saveInOneFile(cfg, savePath, true);
        }
    }

    public static void saveInOneFile(CfgSchema cfg, Path dst) {
        saveInOneFile(cfg, dst, false);
    }

    private static void saveInOneFile(CfgSchema cfg, Path dst, boolean useLastName) {
        StringBuilder sb = new StringBuilder(4 * 1024);
        CfgWriter cfgWriter = new CfgWriter(sb, useLastName);
        cfgWriter.writeCfg(cfg);
        String content = sb.toString();

        try {
            CachedFiles.writeFile(dst, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        CfgSchema cfg = XmlParser.INSTANCE.parse(Path.of("config.xml"), true);
        saveInAllSubDirectory(cfg, Path.of("config.cfg"));
    }

}
