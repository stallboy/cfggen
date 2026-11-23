package configgen.layout;

import java.nio.file.Path;

public record JsonFileInfo(long lastModified,
                           Path path,
                           Path relativePath,
                           boolean isIntegerId,
                           int integerId) {

    public static JsonFileInfo of(Path absPath, Path relativePath) {
        String fn = relativePath.getFileName().toString();
        int id = -1;
        boolean isIntegerId = false;
        try {
            id = Integer.parseInt(JsonLayout.recordId(fn));
            isIntegerId = true;
        } catch (NumberFormatException ignored) {
        }
        return new JsonFileInfo(absPath.toFile().lastModified(), absPath, relativePath, isIntegerId, id);
    }
}
