package configgen.schema.cfg;

import org.jspecify.annotations.Nullable;

import configgen.schema.CfgFileInfo;
import configgen.util.FileNameUtil;
import configgen.schema.CfgSchema;
import configgen.schema.Nameable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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

        // 传递 fileEndComments 到分离后的 CfgSchema
        for (Map.Entry<String, CfgSchema> entry : cfgMap.entrySet()) {
            String ns = entry.getKey();
            CfgSchema cfg = entry.getValue();
            String fileEndComment = root.getFileEndComment(ns);
            if (!fileEndComment.isEmpty()) {
                // 分离后的 CfgSchema 使用空字符串作为 key（单文件场景）
                cfg.setFileEndComment("", fileEndComment);
            }
        }

        return cfgMap;
    }

    public static Path getCfgFilePathByNamespace(String ns, Path absoluteTopDst) {
        if (ns.isEmpty()) {
            return absoluteTopDst;
        }

        Path cur = java.util.Objects.requireNonNull(absoluteTopDst.getParent());

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
                String codeName = FileNameUtil.getCodeName(fn);
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

    public static void findConfigFilesRecursively(Path source,
                                                  @Nullable Set<String> nullableWhiteListSubDirs,
                                                  String ext,
                                                  String pkgNameDot,
                                                  Path rootDir,
                                                  Map<String, CfgFileInfo> cfgFiles) {
        if (Files.exists(source)) {
            Path relativizeSource = rootDir.relativize(source);
            cfgFiles.put(relativizeSource.toString(),
                    new CfgFileInfo(source.toFile().lastModified(), source, relativizeSource, pkgNameDot));
        }
        try {
            try (Stream<Path> paths = Files.list(source.getParent())) {
                for (Path path : paths.toList()) {
                    if (!Files.isDirectory(path)) {
                        continue;
                    }

                    if (nullableWhiteListSubDirs != null &&
                            !nullableWhiteListSubDirs.contains(path.getFileName().toString())) {
                        continue;
                    }

                    String lastDir = path.getFileName().toString().toLowerCase();
                    String subPkgName = FileNameUtil.getCodeName(lastDir);
                    if (subPkgName == null) {
                        continue;
                    }
                    Path subSource = path.resolve(subPkgName + "." + ext);
                    String subPkgNameDot = pkgNameDot + subPkgName + ".";
                    findConfigFilesRecursively(subSource, null, ext, subPkgNameDot,
                            rootDir, cfgFiles);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
