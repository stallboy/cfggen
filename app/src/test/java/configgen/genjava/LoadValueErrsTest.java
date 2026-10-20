package configgen.genjava;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class LoadValueErrsTest {
    @Test
    void collectsFlatRecordsWithOnlyTheirActualParameters() throws Exception {
        var errors = new LoadValueErrs();
        var packs = new java.util.ArrayList<>(List.of("base", "mod"));
        errors.missingReference("Unit", "targetId", "target", 9);
        errors.duplicateKey("links", 2);
        errors.invalidSeq("unit", "id", 7, -1, 3, packs);
        packs.add("later");
        var failure = assertThrows(ConfigErr.class, errors::checkErrors);
        var missing = assertInstanceOf(LoadValueErrs.MissingReference.class, failure.getErrors().getFirst());
        assertEquals("targetId", missing.field());
        assertEquals("target", missing.target());
        assertEquals(9, missing.key());
        assertEquals("Unit", missing.source());
        var seq = assertInstanceOf(LoadValueErrs.InvalidSeq.class, failure.getErrors().getLast());
        assertEquals(-1, seq.value());
        assertEquals(3, seq.size());
        assertEquals("unit", seq.table());
        assertEquals(7, seq.key());
        assertEquals(List.of("base", "mod"), seq.packs());
        assertTrue(LoadValueErrs.Error.class.isSealed());
        assertTrue(java.util.Arrays.stream(LoadValueErrs.Error.class.getPermittedSubclasses()).allMatch(Class::isRecord));
        assertTrue(java.util.Arrays.stream(LoadValueErrs.class.getDeclaredClasses()).noneMatch(c -> c.getSimpleName().equals("Row")));
        assertTrue(java.lang.reflect.Modifier.isPrivate(LoadValueErrs.class.getDeclaredMethod("add", LoadValueErrs.Error.class).getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isPrivate(LoadValueErrs.class.getDeclaredMethod("fail", LoadValueErrs.Error.class).getModifiers()));
        assertThrows(UnsupportedOperationException.class, () -> failure.getErrors().clear());
    }

    @Test
    void missingReferenceReportsOnlyTheProvidedParameters() throws Exception {
        var errors = new LoadValueErrs();
        errors.missingReference("Unit", "target", "target", List.of(1, "a"));
        assertEquals(List.of(new LoadValueErrs.MissingReference("Unit", "target", "target", List.of(1, "a"))), errors.getErrors());
        assertEquals(void.class, LoadValueErrs.class.getMethod("missingReference", String.class, String.class, String.class, Object.class).getReturnType());
    }

    @Test
    void symbolAndSchemaReportersOnlyRecordTheirParameters() {
        var errors = new LoadValueErrs();
        errors.missingSymbol("rank", "Green", 10);
        errors.schemaMismatch("map", "Different fixed symbols");
        assertEquals(List.of(new LoadValueErrs.MissingSymbol("rank", "Green", 10),
                new LoadValueErrs.SchemaMismatch("map", "Different fixed symbols")), errors.getErrors());
        assertTrue(java.util.Arrays.stream(LoadValueErrs.class.getDeclaredMethods())
                .flatMap(method -> java.util.Arrays.stream(method.getParameterTypes()))
                .noneMatch(type -> Schema.class.isAssignableFrom(type) || type == ConfigPackage.class));
    }

    @Test
    void fatalDecodeErrorsKeepPreviouslyCollectedErrors() {
        var errors = new LoadValueErrs();
        errors.duplicateKey("links", 1);
        var failure = assertThrows(ConfigErr.class, () -> { throw errors.unknownVariant("Choice", "Bad"); });
        assertEquals(2, failure.getErrors().size());
        var unknown = assertInstanceOf(LoadValueErrs.UnknownVariant.class, failure.getErrors().getLast());
        assertEquals("Choice", unknown.union());
        assertEquals("Bad", unknown.tag());
        errors.duplicateKey("links", 2);
        assertEquals(2, failure.getErrors().size(), "Exceptions preserve a snapshot");
    }

    @Test
    void independentLoadsStayIndependentOnTheSameThreadAndAcrossThreads() throws Exception {
        var bad = new LoadValueErrs();
        bad.missingReference("Unit", "targetId", "target", 1);
        assertThrows(ConfigErr.class, bad::checkErrors);
        var good = new LoadValueErrs();
        assertDoesNotThrow(good::checkErrors);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.IntStream.range(0, 12).<java.util.concurrent.Callable<Integer>>mapToObj(id -> () -> {
                var own = new LoadValueErrs();
                own.missingReference("Unit", "targetId", "target", id);
                var error = assertInstanceOf(LoadValueErrs.MissingReference.class, own.getErrors().getFirst());
                return (Integer) error.key();
            }).toList();
            var results = executor.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) assertEquals(i, results.get(i).get());
        }
        assertEquals(1, bad.getErrors().size());
        assertTrue(good.getErrors().isEmpty());
    }
}
