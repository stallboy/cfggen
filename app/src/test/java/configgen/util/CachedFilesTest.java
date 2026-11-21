package configgen.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
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
        CachedFiles files = new CachedFiles();
        files.prepareOutputDirectory(root, ".meta", ".uid");
        files.prepareOutputDirectory(root, ".custom");
        assertEquals(0, Files.size(root.resolve(".cfggen-output")));
        Path asset = Files.createDirectory(root.resolve("Asset"));
        Path similar = Files.createDirectory(root.resolve("AssetExtra"));
        Path directoryMetadata = Files.createFile(root.resolve("Asset.META"));
        Path orphanMetadata = Files.createFile(root.resolve("Ass.meta"));
        Files.createDirectory(root.resolve("Ass"));
        Path generated = Files.createFile(asset.resolve("data.lua"));
        Path generatedMetadata = Files.createFile(asset.resolve("data.lua.UID"));
        Path similarlyNamed = Files.createFile(similar.resolve("data.lua"));

        Path customMetadata = Files.createFile(asset.resolve("data.lua.CUSTOM"));
        Path unknownMetadata = Files.createFile(asset.resolve("data.lua.unknown"));
        files.keepFile(generated);
        files.keepFile(similarlyNamed);
        files.finalizeRun();

        assertTrue(Files.exists(directoryMetadata));
        assertTrue(Files.exists(generatedMetadata));
        assertFalse(Files.exists(orphanMetadata));
        assertTrue(Files.exists(customMetadata));
        assertFalse(Files.exists(unknownMetadata));
    }

    @Test
    void requiresRegularMarkerBeforeWritingAndBeforeCleanup() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path stale = Files.writeString(root.resolve("stale.txt"), "old");
        CachedFiles files = new CachedFiles();
        assertThrows(IOException.class, () -> files.prepareOutputDirectory(root));
        Path marker = root.resolve(".cfggen-output");
        Files.createDirectory(marker);
        assertThrows(IOException.class, () -> files.prepareOutputDirectory(root));
        Files.delete(marker);
        Files.createFile(marker);

        files.prepareOutputDirectory(root);
        Files.delete(marker);
        files.finalizeRun();

        assertEquals("old", Files.readString(stale));
    }

    @Test
    void preservesNestedManagedDirectoriesAndRejectsOverlappingRegistrations() throws IOException {
        Path root = tempDir.resolve("root");
        Path group = root.resolve("group");
        Path nested = group.resolve("nested");
        CachedFiles files = new CachedFiles();
        files.prepareOutputDirectory(root, ".meta", ".uid");
        Path stale = Files.writeString(root.resolve("stale.txt"), "old");
        assertThrows(IOException.class, () -> files.prepareOutputDirectory(nested));

        CachedFiles nestedFiles = new CachedFiles();
        nestedFiles.prepareOutputDirectory(nested);
        Path nestedOutput = Files.writeString(nested.resolve("keep.go"), "nested");
        Path groupMetadata = Files.writeString(root.resolve("group.meta"), "group GUID");
        Path nestedMetadata = Files.writeString(group.resolve("nested.meta"), "nested GUID");
        files.finalizeRun();

        assertFalse(Files.exists(stale));
        assertEquals("nested", Files.readString(nestedOutput));
        assertEquals("group GUID", Files.readString(groupMetadata));
        assertEquals("nested GUID", Files.readString(nestedMetadata));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void doesNotTraverseDirectoryJunctionDuringCleanup() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        CachedFiles files = new CachedFiles();
        files.prepareOutputDirectory(root);
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Path sentinel = Files.writeString(outside.resolve("keep.txt"), "keep");
        Path link = root.resolve("junction");
        Process mklink = new ProcessBuilder("cmd.exe", "/d", "/c", "mklink", "/j",
                link.toString(), outside.toString()).redirectErrorStream(true).start();
        byte[] output = mklink.getInputStream().readAllBytes();
        assumeTrue(mklink.waitFor() == 0, () -> new String(output));
        try {
            assertTrue(Files.isDirectory(link, LinkOption.NOFOLLOW_LINKS));
            files.finalizeRun();
            assertEquals("keep", Files.readString(sentinel));
        } finally {
            Files.deleteIfExists(link);
        }
    }
}
