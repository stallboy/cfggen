package configgen.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CachedFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void keepsMetadataWithLegacySuffixCasingAndMatchesWholePathSegments() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path asset = Files.createDirectory(root.resolve("Asset"));
        Path similar = Files.createDirectory(root.resolve("AssetExtra"));
        Path directoryMetadata = Files.createFile(root.resolve("Asset.META"));
        Path orphanMetadata = Files.createFile(root.resolve("Ass.meta"));
        Files.createDirectory(root.resolve("Ass"));
        Path generated = Files.createFile(asset.resolve("data.lua"));
        Path generatedMetadata = Files.createFile(asset.resolve("data.lua.UID"));
        Path similarlyNamed = Files.createFile(similar.resolve("data.lua"));

        CachedFiles files = new CachedFiles();
        files.keepFile(generated);
        files.keepFile(similarlyNamed);
        files.keepMetaAndDeleteOtherFiles(root.toFile());
        files.finalizeRun();

        assertTrue(Files.exists(directoryMetadata));
        assertTrue(Files.exists(generatedMetadata));
        assertFalse(Files.exists(orphanMetadata));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void doesNotTraverseDirectoryJunctionDuringCleanup() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Path sentinel = Files.writeString(outside.resolve("keep.txt"), "keep");
        Path link = root.resolve("junction");
        Process mklink = new ProcessBuilder("cmd.exe", "/d", "/c", "mklink", "/j",
                link.toString(), outside.toString()).redirectErrorStream(true).start();
        byte[] output = mklink.getInputStream().readAllBytes();
        assumeTrue(mklink.waitFor() == 0, () -> new String(output));
        try {
            assertTrue(Files.isDirectory(link, LinkOption.NOFOLLOW_LINKS));
            CachedFiles files = new CachedFiles();
            files.deleteOtherFiles(root.toFile());
            files.finalizeRun();
            assertEquals("keep", Files.readString(sentinel));
        } finally {
            Files.deleteIfExists(link);
        }
    }
}
