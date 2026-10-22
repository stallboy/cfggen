package configgen.schema.cfg;

import configgen.schema.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceSyntaxTest {
    @Test
    void roundTripsInternalTargetsAndIndependentSourcePaths() {
        String input = """
                struct Target {
                    phase:str;
                    ->phase:[phase] ->::phases (custom='ref-meta'); // independent reference comment
                    ->target:[@AbilityUse::abilityId,@MachineUse::machineKey,phase]
                        ->ability[code]::machines.phases (nullable); // target comment
                    ->local:[::machineKey,binding.phase] ->::machines.phases;
                    ->fixed:[] ->::movement;
                    next:str ->@Act.StateMachine::phases (nullable);
                    movement:int;
                    ->movement:[] ->::movement;
                }
                struct Machine {
                    phases:list<Target> (index='phase,variant');
                }
                """;
        CfgSchema first = CfgReader.parse(input);
        String written = CfgWriter.stringify(first);
        assertEquals(first.items(), CfgReader.parse(written).items());
        assertTrue(written.contains("[@AbilityUse::abilityId,@MachineUse::machineKey,phase]"));
        assertTrue(written.contains("->ability[code]::machines.phases"));
        assertTrue(written.contains("->fixed:[] ->::movement"));
        assertTrue(written.contains("->movement:[] ->::movement"));
        assertTrue(written.contains("target comment"));
        assertTrue(written.contains("->phase:[phase] ->::phases (custom='ref-meta'); // independent reference comment"));
    }

    @Test
    void emptySourceListDoesNotAllowEmptyTableKeys() {
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("table t[] { id:int; }"));
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("table t[id] { id:int; []; }"));
    }

    @Test
    void rejectsPathSourcesOnListReferencesEvenWithoutARemoteKey() {
        assertThrows(CfgSyntaxException.class,
                () -> CfgReader.parse("struct Link { id:int; ->target:[::id] =>table; }"));
        assertThrows(CfgSyntaxException.class,
                () -> CfgReader.parse("struct Link { ->target:[] =>table; }"));
    }

    @Test
    void doesNotRewriteLegacyNullableDeclarations() {
        String input = "struct Link { id:int ->target (nullable); ->other:[id] ->target; }";
        String written = CfgWriter.stringify(CfgReader.parse(input));
        assertFalse(written.contains("::"));
        assertEquals(CfgReader.parse(input).items(), CfgReader.parse(written).items());
    }

    @Test
    void doesNotTreatUnimplementedPathsAsLegacyTableReferences() {
        CfgSchema schema = CfgReader.parse("table t[id] { id:int; ->local:[] ->::fixed; }");
        assertTrue(schema.resolve().errs().stream().anyMatch(e -> e instanceof CfgSchemaErrs.InvalidReference));
    }
}
