package configgen.layout;

import java.nio.file.Path;

public record CfgFileInfo(long lastModified,
                          Path path,
                          Path relativePath,
                          String pkgNameDot) {
}
