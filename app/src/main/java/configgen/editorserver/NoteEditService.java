package configgen.editorserver;

import org.jspecify.annotations.Nullable;

import configgen.util.CSVUtil;
import configgen.util.Logger;
import de.siegmar.fastcsv.reader.CsvRecord;

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
        keyNotFoundOnDelete,
        storeErr,
    }

    public record NoteEditResult(ResultCode resultCode,
                                 Notes notes) {
    }

    private Map<String, String> noteMap;
    private final Path noteCsvPath;

    public NoteEditService(Path noteCsvPath) {
        this.noteCsvPath = noteCsvPath;
        noteMap = new LinkedHashMap<>(256);
        if (Files.exists(noteCsvPath)) {
            List<CsvRecord> rows = CSVUtil.read(noteCsvPath);
            for (CsvRecord row : rows) {
                if (row.getFieldCount() == 2) {
                    noteMap.put(row.getField(0), row.getField(1));
                } else {
                    Logger.log(row.toString() + " field count not 2, ignore!");
                }
            }
        }
    }

    private void writeNoteMap(Map<String, String> updated) throws IOException {
        List<List<String>> list = updated.entrySet().stream().map(e -> List.of(e.getKey(), e.getValue())).toList();
        CSVUtil.writeToFile(noteCsvPath.toFile(), list);
    }

    public synchronized Notes getNotes() {
        return new Notes(noteMap.entrySet().stream().map(e -> new Note(e.getKey(), e.getValue())).toList());
    }

    public synchronized NoteEditResult updateNote(@Nullable String key, String note) {
        if (key == null || key.isEmpty()) {
            return new NoteEditResult(ResultCode.keyNotSet, getNotes());
        }

        try {
            Map<String, String> updated = new LinkedHashMap<>(noteMap);
            ResultCode code;
            if (note.isEmpty()) {
                if (updated.remove(key) == null) {
                    return new NoteEditResult(ResultCode.keyNotFoundOnDelete, getNotes());
                }
                code = ResultCode.deleteOk;
            } else {
                String old = updated.put(key, note);
                code = old != null ? ResultCode.updateOk : ResultCode.addOk;
            }
            writeNoteMap(updated);
            noteMap = updated;
            return new NoteEditResult(code, getNotes());
        } catch (Exception e) {
            return new NoteEditResult(ResultCode.storeErr, getNotes());
        }

    }
}
