package configgen.editorserver;

import configgen.util.CSVUtil;
import configgen.util.Logger;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NoteEditService {

    public record Note(String key,
                       String note) {
    }

    public record Notes(List<Note> notes) {
    }

    public enum ResultCode {
        addOk,
        updateOk,
        deleteOk,

        keyNotSet,
        noteNotSet,
        keyNotFound,
        storeErr,
    }

    public record NoteEditResult(ResultCode resultCode,
                                 Notes notes) {
    }

    private final Map<String, String> noteMap;
    private final Path noteCsvPath;

    public NoteEditService(Path noteCsvPath) {
        this.noteCsvPath = noteCsvPath;
        noteMap = new LinkedHashMap<>(256);
        if (Files.exists(noteCsvPath)){
            List<CsvRow> rows = CSVUtil.read(noteCsvPath, "UTF-8");
            for (CsvRow row : rows) {
                if (row.getFieldCount() == 2) {
                    noteMap.put(row.getField(0), row.getField(1));
                } else {
                    Logger.log(row.toString() + " field count not 2, ignore!");
                }
            }
        }
    }

    private void writeNoteMap() throws IOException {
        List<List<String>> list = noteMap.entrySet().stream().map(e -> List.of(e.getKey(), e.getValue())).toList();
        CSVUtil.writeToFile(noteCsvPath.toFile(), list);
    }

    public synchronized Notes getNotes() {
        return new Notes(noteMap.entrySet().stream().map(e -> new Note(e.getKey(), e.getValue())).toList());
    }

    public synchronized NoteEditResult addOrUpdateNote(String key, String note) {
        if (key.isEmpty()) {
            return new NoteEditResult(ResultCode.keyNotSet, getNotes());
        }
        if (note.isEmpty()) {
            return new NoteEditResult(ResultCode.noteNotSet, getNotes());
        }

        try {
            String old = noteMap.put(key, note);
            writeNoteMap();
            return new NoteEditResult(old != null ? ResultCode.updateOk : ResultCode.addOk, getNotes());
        } catch (Exception e) {
            return new NoteEditResult(ResultCode.storeErr, getNotes());
        }

    }

    public synchronized NoteEditResult deleteNode(String key) {
        if (key.isEmpty()) {
            return new NoteEditResult(ResultCode.keyNotSet, getNotes());
        }

        try {
            String old = noteMap.remove(key);
            writeNoteMap();
            return new NoteEditResult(old != null ? ResultCode.deleteOk : ResultCode.keyNotFound, getNotes());
        } catch (Exception e) {
            return new NoteEditResult(ResultCode.storeErr, getNotes());
        }
    }

}
