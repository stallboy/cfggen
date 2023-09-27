package configgen.schema.cfg;

import configgen.data.DataUtil;
import configgen.schema.CfgSchema;
import configgen.util.CachedFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class Cfgs {

    public static CfgSchema readFrom(Path source, boolean includeSubDirectory) {
        return readFrom(source, includeSubDirectory, CfgReader.INSTANCE, "cfg");
    }

    public static CfgSchema readFromXml(Path source, boolean includeSubDirectory) {
        return readFrom(source, includeSubDirectory, XmlReader.INSTANCE, "xml");
    }


    private static CfgSchema readFrom(Path source, boolean includeSubDirectory,
                                      CfgSchemaReader reader, String ext) {
        CfgSchema destination = CfgSchema.of();
        if (includeSubDirectory) {
            readFromAllSubDirectory(destination, source, "", reader, ext);
        } else {
            reader.readTo(destination, source, "");
        }
        return destination;
    }

    private static void readFromAllSubDirectory(CfgSchema destination, Path source, String pkgNameDot,
                                                CfgSchemaReader reader, String ext) {
        if (Files.exists(source)) {
            reader.readTo(destination, source, pkgNameDot);
        }
        try {
            try (Stream<Path> paths = Files.list(source.toAbsolutePath().getParent())) {
                for (Path path : paths.toList()) {
                    if (Files.isDirectory(path)) {
                        String lastDir = path.getFileName().toString().toLowerCase();
                        String subPkgName = DataUtil.getCodeName(lastDir);

                        Path subSource = path.resolve(subPkgName + "." + ext);
                        readFromAllSubDirectory(destination, subSource, pkgNameDot + subPkgName + ".",
                                reader, ext);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void writeTo(Path destination, boolean isWriteToEachSubDirectory, CfgSchema root) {
        Path absoluteDst = destination.toAbsolutePath().normalize();
        if (isWriteToEachSubDirectory) {
            Map<String, CfgSchema> cfgs = CfgUtil.separate(root);
            for (Map.Entry<String, CfgSchema> entry : cfgs.entrySet()) {
                String ns = entry.getKey();
                CfgSchema cfg = entry.getValue();
                Path dst = CfgUtil.getCfgFilePathByNamespace(ns, absoluteDst);
                writeToOneFile(dst, cfg, true);
            }
        } else {
            writeToOneFile(absoluteDst, root, false);
        }
    }

    private static void writeToOneFile(Path dst, CfgSchema cfg, boolean useLastName) {
        String content = CfgWriter.stringify(cfg, useLastName);
        try {
            CachedFiles.writeFile(dst, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        CfgSchema cfg = readFromXml(Path.of("config.xml"), true);
        Path root = Path.of("config.cfg");
        System.out.println("-----write");
        writeTo(root, true, cfg);
        CfgSchema cfg2 = readFrom(root, true);
        System.out.println("-----rewrite");
        writeTo(root, true, cfg2);
        CfgSchema cfg3 = readFrom(root, true);

        System.out.println(cfg2.items().size());
        System.out.println(cfg2.equals(cfg3));
    }

}
