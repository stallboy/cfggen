package configgen.schema;

import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceResolutionTest {
    @Test
    void resolvesTablesWithoutBeingShadowedByUnrelatedLocalTypes() {
        CfgSchema cfg = CfgReader.parse("""
                struct foo.target { key:str; }
                struct foo.Link { id:int; ->ref:[id] ->target::item; }
                interface Choice {
                    struct target { key:str; }
                    struct Link { id:int; ->ref:[id] ->target::item; }
                }
                struct Item { key:str; }
                table target[id] { id:int; item:Item; }
                table source[id] (json) { id:int; link:foo.Link; choice:Choice; }
                """);
        assertTrue(cfg.resolve().errs().isEmpty());
    }
    @Test
    void resolvesExternalLocalAndAncestorPaths() {
        CfgSchema cfg = CfgReader.parse("""
                struct Phase { key:str; next:str ->@Machine::phases (nullable); }
                struct Machine { key:str; phases:list<Phase> (index='key'); }
                struct Link {
                    phase:str;
                    ->target:[@Use::ability,@Use::machine,phase] ->ability::machines.phases;
                }
                struct Use { ability:int; machine:str; links:list<Link>; }
                table ability[id] (json) { id:int; machines:list<Machine> (index='key'); }
                table use[id] (json) { id:int; uses:list<Use>; }
                """);
        assertTrue(cfg.resolve().errs().isEmpty());
        assertDoesNotThrow(() -> new CfgSchemaStat(cfg));
    }

    @Test
    void rejectsInvalidIndexesAndKeyBindings() {
        for (String index : new String[]{"missing", "key,key", "", "key,"}) {
            CfgSchema cfg = CfgReader.parse("struct Phase { key:str; } table t[id] (json) { id:int; phases:list<Phase> (index='"
                    + index + "'); }");
            assertFalse(cfg.resolve().errs().isEmpty(), index);
        }
        CfgSchema cfg = CfgReader.parse("""
                struct Phase { key:str; }
                table t[id] { id:int; phases:list<Phase> (index='key'); bad:int ->::phases; }
                """);
        assertFalse(cfg.resolve().errs().isEmpty());
    }

    @Test
    void rejectsCrossingInterfaceOrUnindexedList() {
        for (String field : new String[]{"phases:list<Phase>;", "phases:Choice;"}) {
            CfgSchema cfg = CfgReader.parse("struct Phase { key:str; } interface Choice { struct One { key:str; } } "
                    + "table t[id] { id:int; " + field + " key:str; ->bad:[key] ->::phases.key; }");
            assertFalse(cfg.resolve().errs().isEmpty());
        }
    }
}
