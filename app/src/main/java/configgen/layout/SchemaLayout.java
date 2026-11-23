package configgen.layout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/// Root and module paths for CFG and legacy XML schemas.
public final class SchemaLayout {
    static final String CFG_EXTENSION = "cfg";
    private static final String XML_EXTENSION = "xml";

    private SchemaLayout() {
    }

    public static Path cfgRootFile(Path rootDir) {
        return rootDir.resolve("config." + CFG_EXTENSION);
    }

    /// Discovery continues into module directories even when the root file is absent.
    public static Set<CfgFileInfo> findCfgFiles(Path rootFile) {
        return findFiles(rootFile, CFG_EXTENSION);
    }

    public static Set<CfgFileInfo> findXmlFiles(Path rootDir) {
        return findFiles(rootDir.resolve("config." + XML_EXTENSION), XML_EXTENSION);
    }

    public static Path cfgFileForNamespace(String namespace, Path rootFile) {
        if (namespace.isEmpty()) {
            return rootFile;
        }

        Path current = Objects.requireNonNull(rootFile.getParent());
        String moduleName = "config";
        for (String name : namespace.split("\\.")) {
            current = moduleDirectoryForWrite(current, name);
            moduleName = name;
        }
        return current.resolve(moduleName + "." + CFG_EXTENSION);
    }

    private static Path moduleDirectoryForWrite(Path parent, String name) {
        Path exact = parent.resolve(name);
        if (Files.isDirectory(exact)) {
            return exact;
        }

        try {
            Path matched = FileNameUtil.findModuleDirectory(parent, name);
            return matched != null ? matched : exact;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<CfgFileInfo> findFiles(Path rootFile, String extension) {
        Path rootDir = Objects.requireNonNull(rootFile.getParent());
        Set<CfgFileInfo> files = new LinkedHashSet<>();
        findFilesRecursively(rootFile, extension, "", rootDir, files);
        return files;
    }

    private static void findFilesRecursively(Path source, String extension, String pkgNameDot,
                                             Path rootDir, Set<CfgFileInfo> files) {
        if (Files.exists(source)) {
            files.add(new CfgFileInfo(source.toFile().lastModified(), source, rootDir.relativize(source), pkgNameDot));
        }

        try (Stream<Path> paths = Files.list(source.getParent())) {
            for (Path path : paths.toList()) {
                if (!Files.isDirectory(path)) {
                    continue;
                }

                String dirName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                String moduleName = FileNameUtil.getCodeName(dirName);
                if (moduleName != null) {
                    Path moduleFile = path.resolve(moduleName + "." + extension);
                    findFilesRecursively(moduleFile, extension, pkgNameDot + moduleName + ".", rootDir, files);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
