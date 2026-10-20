package configgen.genjava;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {
    private record Row(int id, int value) {}

    @Test
    void mergesThenTransfersReadOnlyRowsAndSourcesWithoutRetainingWriteAccess() {
        var errors = new LoadValueErrs();
        var published = new AtomicReference<Map<Integer, Row>>();
        var resolved = new ArrayList<Row>();
        var loader = new ConfigLoader<Integer, Row>("unit", errors, input -> new Row(input.readInt(), input.readInt()),
                Row::id, published::set, resolved::add);
        loader.read(input(1, 10, 2, 20), "base", true);
        assertTrue(loader.origins().isEmpty(), "Base rows must not allocate provenance entries");
        loader.read(input(1, 30, 3, 40), "mod", false);
        var sources = loader.origins();
        loader.index();
        loader.resolveAll();
        errors.checkErrors();
        assertEquals(List.of(new Row(1, 30), new Row(2, 20), new Row(3, 40)), resolved);
        assertEquals(List.of(1, 2, 3), List.copyOf(published.get().keySet()));
        assertEquals(Map.of(1, List.of("mod"), 3, List.of("mod")), sources);
        assertThrows(UnsupportedOperationException.class, () -> published.get().clear());
        assertThrows(UnsupportedOperationException.class, sources::clear);
        assertThrows(UnsupportedOperationException.class, () -> sources.get(1).add("late"));
        assertThrows(UnsupportedOperationException.class, () -> loader.read(input(4, 50), "late", false));
        assertFalse(sources.containsKey(4));
        assertEquals(3, published.get().size());
    }

    @Test
    void missingReferenceStoresItsOwnFieldsWithoutLoaderContext() {
        var errors = new LoadValueErrs();
        var loader = new ConfigLoader<Integer, Row>("unit", errors, input -> new Row(input.readInt(), input.readInt()),
                Row::id, rows -> {}, row -> errors.missingReference("Unit", "targetId", "target", row.id()));
        loader.read(input(1, 10), "base", true);
        loader.read(input(1, 20), "mod", false);
        loader.index();
        loader.resolveAll();
        var failure = assertThrows(ConfigErr.class, errors::checkErrors);
        var error = assertInstanceOf(LoadValueErrs.MissingReference.class, failure.getErrors().getFirst());
        assertEquals(new LoadValueErrs.MissingReference("Unit", "targetId", "target", 1), error);
        assertEquals(List.of("mod"), loader.origins().get(1));
    }

    @Test
    void tracksOnlyModsAndCollectsDuplicatesWithoutRepeatingTheirOrigin() {
        var errors = new LoadValueErrs();
        var loader = new ConfigLoader<Integer, Row>("unit", errors, input -> new Row(input.readInt(), input.readInt()),
                Row::id, rows -> {}, null);
        loader.read(input(1, 10, 2, 20), "game", true);
        loader.read(input(1, 30, 3, 40, 3, 50), "first", false);
        loader.read(input(1, 60, 3, 70, 4, 80), "second", false);
        loader.index();
        assertEquals(Map.of(1, List.of("first", "second"), 3, List.of("first", "second"),
                4, List.of("second")), loader.origins());
        var failure = assertThrows(ConfigErr.class, errors::checkErrors);
        assertEquals(List.of(new LoadValueErrs.DuplicateRecord("unit", "first", 3)), failure.getErrors());
    }

    @Test
    void collectsAllDuplicateKeysAndConsumesTheWholeBlock() {
        var errors = new LoadValueErrs();
        var loader = new ConfigLoader<Integer, Row>("unit", errors, input -> new Row(input.readInt(), input.readInt()),
                Row::id, rows -> {}, null);
        var input = input(1, 10, 1, 20, 1, 30);
        assertDoesNotThrow(() -> loader.read(input, "base", true));
        input.requireEnd();
        var failure = assertThrows(ConfigErr.class, errors::checkErrors);
        assertEquals(2, failure.getErrors().size());
        for (var issue : failure.getErrors()) {
            var duplicate = assertInstanceOf(LoadValueErrs.DuplicateRecord.class, issue);
            assertEquals(1, duplicate.key());
            assertEquals("unit", duplicate.table());
            assertEquals("base", duplicate.pack());
        }
    }

    private static ConfigInput input(int... fields) {
        var bytes = new ByteArrayOutputStream();
        try (var out = new ConfigOutput(bytes)) {
            out.writeInt(fields.length / 2);
            for (int value : fields) out.writeInt(value);
        }
        return new ConfigInput(bytes.toByteArray());
    }
}
