package configgen.editorserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static configgen.editorserver.NoteEditService.ResultCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteEditServiceTest {
    @Test
    void failedSavePreservesNotesAndRetryPersistsThem(@TempDir Path tempDir) throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("notes"));
        Path file = directory.resolve("notes.csv");
        var service = new NoteEditService(file);
        assertEquals(addOk, service.updateNote("one", "原始,内容\r第二行").resultCode());
        var original = service.getNotes();
        assertEquals(original, new NoteEditService(file).getNotes());

        Path moved = Files.move(directory, tempDir.resolve("moved"));
        var failed = service.updateNote("one", "new value");
        assertEquals(storeErr, failed.resultCode());
        assertEquals(original, failed.notes());
        assertEquals(original, new NoteEditService(moved.resolve(file.getFileName())).getNotes());

        Files.move(moved, directory);
        assertEquals(updateOk, service.updateNote("one", "new value").resultCode());
        assertEquals(service.getNotes(), new NoteEditService(file).getNotes());
        assertEquals(deleteOk, service.updateNote("one", "").resultCode());
        assertEquals(keyNotFoundOnDelete, service.updateNote("one", "").resultCode());
        assertEquals(service.getNotes(), new NoteEditService(file).getNotes());
    }
}
