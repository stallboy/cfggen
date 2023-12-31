package configgen.schema.cfg;

import configgen.data.DataUtil;
import configgen.schema.CfgSchema;
import configgen.schema.Nameable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CfgUtil {

    public static Map<String, CfgSchema> separate(CfgSchema root) {
        Map<String, CfgSchema> cfgMap = new LinkedHashMap<>();
        for (Nameable item : root.items()) {
            String ns = item.namespace();
            CfgSchema cfg = cfgMap.get(ns);
            if (cfg == null) {
                cfg = CfgSchema.of();
                cfgMap.put(ns, cfg);
            }
            cfg.items().add(item);
        }
        return cfgMap;
    }

    public static Path getCfgFilePathByNamespace(String ns, Path absoluteTopDst) {
        if (ns.isEmpty()) {
            return absoluteTopDst;
        }

        Path cur = absoluteTopDst.getParent();

        String lastName = "config";
        for (String name : ns.split("\\.")) {
            cur = subDir(name, cur);
            lastName = name;
        }
        return cur.resolve(lastName + ".cfg");
    }

    private static Path subDir(String name, Path cur) {
        Path p = cur.resolve(name);
        if (Files.isDirectory(p)) {
            return p;
        }

        try (Stream<Path> subPaths = Files.list(cur)) {
            for (Path path : subPaths.toList()) {
                String fn = path.getFileName().toString();
                String codeName = DataUtil.getCodeName(fn);
                if (codeName != null && codeName.equals(name)) {
                    return path;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return p;
    }

    private static final Pattern identifierPattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    public static boolean isIdentifier(String name) {
        return identifierPattern.matcher(name).matches();
    }


}
