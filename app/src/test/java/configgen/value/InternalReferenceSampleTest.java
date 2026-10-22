package configgen.value;

import configgen.ctx.Context;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceSampleTest {
    @Test
    void resolvesDocumentedMixedAncestorKeysToTheirOwnMachine() {
        var value = new Context(Path.of("../samples/internal-reference")).makeValue();
        var references = new ValueReferences(value);
        var errors = CfgValueErrs.of();
        references.resolve(errors);
        assertTrue(errors.errs().isEmpty());
        var targets = references.occurrences().stream().filter(o -> o.value().name().equals("PhaseTarget")).toList();
        assertEquals(3, targets.size());
        var first = references.result(targets.getFirst().value(), targets.getFirst().value().schema().findForeignKey("target"));
        var last = references.result(targets.getLast().value(), targets.getLast().value().schema().findForeignKey("target"));
        assertEquals("Idle", ValueUtil.extractFieldValueStr((CfgValue.VStruct) first.target(), "phaseKey"));
        assertEquals("Idle", ValueUtil.extractFieldValueStr((CfgValue.VStruct) last.target(), "phaseKey"));
        assertNotSame(first.target(), last.target());
        assertSame(first.targetRecord(), last.targetRecord());
    }
}
