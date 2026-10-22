package configgen.value;

import configgen.data.Source;
import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.*;

import static configgen.value.CfgValue.*;
import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceValueTest {
    private static final String SCHEMA = """
            struct Phase { key:str; next:str ->@Machine::phases (nullable); }
            struct Machine { key:str; phases:list<Phase> (index='key'); }
            table t[id] (json) {
                id:int; machines:list<Machine> (index='key');
                machine:str; phase:str;
                ->target:[machine,phase] ->::machines.phases;
            }
            """;

    @Test
    void indexesBelongToCollectionInstances() {
        CfgValue cfg = fixture(false, "X");
        CfgValueErrs errors = CfgValueErrs.of();
        new RefValidator(cfg, errors).validate();
        assertEquals(List.of(), errors.errs());
        ValueReferences references = new ValueReferences(cfg);
        references.resolve(errors);
        VStruct root = cfg.getTable("t").valueList().getFirst();
        VList machines = (VList) root.values().get(1);
        VStruct firstMachine = (VStruct) machines.valueList().getFirst();
        VStruct secondMachine = (VStruct) machines.valueList().getLast();
        VStruct firstPhase = (VStruct) ((VList) firstMachine.values().get(1)).valueList().getFirst();
        VStruct secondPhase = (VStruct) ((VList) secondMachine.values().get(1)).valueList().getFirst();
        ForeignKeySchema next = firstPhase.schema().findForeignKey("next");
        assertEquals(firstPhase, secondPhase);
        assertNotSame(firstPhase, secondPhase);
        assertSame(firstPhase, references.result(firstPhase, next).target());
        assertSame(root, references.result(firstPhase, next).targetRecord());
        assertSame(secondPhase, references.result(secondPhase, next).target());
        assertSame(firstPhase, references.result(root, root.schema().findForeignKey("target")).target());
        assertTrue(references.referencesTo(firstPhase).stream().anyMatch(o -> o.value() == firstPhase));
        assertFalse(references.referencesTo(firstPhase).stream().anyMatch(o -> o.value() == secondPhase));
    }

    @Test
    void rejectsDuplicateLocalKeysEvenWithoutReferencesToThatCollection() {
        CfgValue cfg = fixture(true, "X");
        CfgValueErrs errors = CfgValueErrs.of();
        new RefValidator(cfg, errors).validate();
        assertFalse(errors.errs().isEmpty());
    }

    @Test
    void requiredMissingTargetFails() {
        CfgValue cfg = fixture(false, "Missing");
        CfgValueErrs errors = CfgValueErrs.of();
        new RefValidator(cfg, errors).validate();
        assertFalse(errors.errs().isEmpty());
    }

    @Test
    void nullableMissingTargetDoesNotFallBackToAnotherCollection() {
        CfgValue cfg = fixture(false, "X");
        VStruct root = cfg.getTable("t").valueList().getFirst();
        VStruct machine = (VStruct) ((VList) root.values().get(1)).valueList().getLast();
        VStruct oldPhase = (VStruct) ((VList) machine.values().get(1)).valueList().getFirst();
        VStruct missing = struct(oldPhase.schema(), str("Other"), str("X"));
        // Rebuild only the second machine so X exists in A but not in B.
        VStruct changed = struct(machine.schema(), str("B"), list(missing));
        CfgValue updated = replaceRoot(cfg, struct(root.schema(), root.values().getFirst(),
                list(((VList) root.values().get(1)).valueList().getFirst(), changed), str("A"), str("X")));
        CfgValueErrs errors = CfgValueErrs.of();
        ValueReferences references = new ValueReferences(updated);
        references.resolve(errors);
        assertEquals(List.of(), errors.errs());
        assertNull(references.result(missing, missing.schema().findForeignKey("next")).target());
    }

    private static CfgValue replaceRoot(CfgValue cfg, VStruct root) {
        CfgValue updated = CfgValue.of(cfg.schema());
        SequencedMap<Value, VStruct> rows = new LinkedHashMap<>();
        rows.put(root.values().getFirst(), root);
        updated.vTableMap().put("t", new VTable((TableSchema) root.schema(), List.of(root), rows,
                new LinkedHashMap<>(), null, null));
        return updated;
    }

    static CfgValue fixture(boolean duplicate, String target) {
        CfgSchema schema = CfgReader.parse(SCHEMA);
        schema.resolve().checkErrors();
        Structural phase = (Structural) schema.findFieldable("Phase");
        Structural machine = (Structural) schema.findFieldable("Machine");
        VStruct a = struct(machine, str("A"), list(struct(phase, str("X"), str("X"))));
        List<SimpleValue> phases = new ArrayList<>();
        phases.add(struct(phase, str("X"), str("X")));
        if (duplicate) phases.add(struct(phase, str("X"), str("X")));
        VStruct b = struct(machine, str("B"), new VList(phases, Source.of()));
        TableSchema table = schema.findTable("t");
        VStruct root = struct(table, new VInt(1, Source.of()), list(a, b), str("A"), str(target));
        CfgValue cfg = CfgValue.of(schema);
        SequencedMap<Value, VStruct> rows = new LinkedHashMap<>();
        rows.put(root.values().getFirst(), root);
        cfg.vTableMap().put("t", new VTable(table, List.of(root), rows, new LinkedHashMap<>(), null, null));
        return cfg;
    }

    static VStruct struct(Structural schema, Value... values) {
        return new VStruct(schema, List.of(values), Source.of());
    }

    static VString str(String text) { return new VString(text, Source.of()); }

    static VList list(SimpleValue... values) { return new VList(List.of(values), Source.of()); }
}
