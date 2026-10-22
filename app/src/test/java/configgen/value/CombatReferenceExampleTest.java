package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import configgen.InternalReferenceFixture;
import configgen.ctx.Context;
import configgen.data.HeadRows;
import configgen.schema.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.*;
import java.util.*;

import static configgen.value.CfgValue.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatReferenceExampleTest {
    @TempDir Path root;

    private Context example() throws Exception {
        Files.writeString(root.resolve("config.cfg"), "");
        InternalReferenceFixture.copyCombatExample(root);
        return new Context(root);
    }

    private record Snapshot(CfgValue value, ValueReferences references) {
        VStruct row(String table) { return value.getTable("combat." + table).valueList().getFirst(); }
        SimpleValue target(VStruct source, String reference) {
            return references.result(source, source.schema().findForeignKey(reference)).target();
        }
    }

    private Snapshot snapshot() throws Exception {
        var value = example().makeValue();
        return new Snapshot(value, ValueReferences.forQuery(value));
    }

    @Test
    void theCompleteExampleDirectoryStillValidatesWithLegacyData() {
        var cfg = new Context(new Context.ContextCfg(Path.of("../example/config"), null, HeadRows.A2_Default,
                "GBK", null, null, null, false)).makeValue();
        assertNotNull(cfg.getTable("combat.ability"));
        assertNotNull(cfg.getTable("equip.jewelry"));
    }

    @Test
    void indexesEverySupportedKeyAndKeepsEachMachineItsOwnScope() throws Exception {
        var snapshot = snapshot();
        var ability = snapshot.row("ability");
        var machines = structs(ability, "machines");
        for (var machine : machines) {
            for (var pair : Map.of("initial", "phases", "impact", "impacts", "frame", "frames",
                    "stance", "stances", "variant", "variants", "combo", "combos").entrySet()) {
                assertSame(structs(machine, pair.getValue()).getFirst(), snapshot.target(machine, pair.getKey()), pair.getKey());
            }
            var onExit = (VStruct) field(machine, "onExit");
            for (var phase : structs(machine, "phases")) assertSame(field(onExit, "effect"), snapshot.target(phase, "exit"));
            for (var anchor : structs(machine, "anchors")) assertSame(field(machine, "anchor"), snapshot.target(anchor, "anchor"));
        }
        var phases = structs(machines.getFirst(), "phases");
        assertSame(phases.get(1), snapshot.target(phases.getFirst(), "next"));
        assertSame(phases.getFirst(), snapshot.target(phases.get(1), "next"));
        assertNull(snapshot.target(phases.get(2), "next"));
        assertNull(snapshot.target(structs(machines.getLast(), "phases").getFirst(), "next"));
        assertSame(phases.getFirst(), snapshot.target(ability, "initial"));
        assertSame(field(ability, "origin"), snapshot.target(ability, "spawn"));
        assertNotSame(field(machines.getFirst(), "anchor"), field(machines.getLast(), "anchor"));
        assertNotSame(structs(machines.getFirst(), "anchors").getFirst(), structs(machines.getFirst(), "anchors").getLast());
    }

    @Test
    void bindsMixedSourcesCompositeRecordKeysAndWholeReferenceNullable() throws Exception {
        var snapshot = snapshot();
        var cast = snapshot.row("cast");
        var machines = structs(snapshot.row("ability"), "machines");
        var abilityUse = structs(cast, "uses").getFirst();
        assertSame(snapshot.row("ability"), snapshot.target(abilityUse, "ability"));
        var uses = structs(abilityUse, "machines");
        for (int i = 0; i < uses.size(); i++) {
            var targets = structs(uses.get(i), "targets");
            var phases = structs(machines.get(i), "phases");
            for (int j = 0; j < targets.size(); j++) {
                assertSame(phases.get(j), snapshot.target(targets.get(j), "target"));
                assertSame(phases.get(j), snapshot.target(targets.get(j), "byCode"));
                assertSame(field(cast, "origin"), snapshot.target(targets.get(j), "origin"));
            }
        }
        var optional = structs(cast, "optional");
        for (int i = 0; i < 3; i++) assertNull(snapshot.target(optional.get(i), "target"));
        assertSame(structs(machines.getFirst(), "phases").get(2), snapshot.target(optional.getLast(), "target"));
        assertSame(field(snapshot.row("phasebook"), "entry"), snapshot.target(cast, "book"));
        assertSame(snapshot.target(cast, "book"), snapshot.target(cast, "bookByCode"));
        assertSame(snapshot.row("phasebook"), snapshot.target(cast, "bookRecord"));
        assertSame(field(snapshot.row("branchreward"), "reward"), snapshot.target(cast, "reward"));
    }

    @Test
    void mapsSelectTypedInterfaceEndpointsWithAllKeyTypes() throws Exception {
        var snapshot = snapshot();
        var timeline = snapshot.row("timeline");
        for (var pair : Map.of("eventKey", "events", "channel", "channels", "slot", "slots", "frame", "clock", "airborne", "modes").entrySet()) {
            var values = ((VMap) field(timeline, pair.getValue())).valueMap();
            assertSame(values.values().iterator().next(), snapshot.target(timeline, pair.getKey()), pair.getKey());
            assertInstanceOf(VInterface.class, snapshot.target(timeline, pair.getKey()));
        }
        assertSame(field(timeline, "fallback"), snapshot.target(timeline, "default"));
    }

    @Test
    void recursiveInterfaceScopeUsesTheNearestMachineWithoutFallback() throws Exception {
        var snapshot = snapshot();
        var outer = ((VInterface) field(snapshot.row("encounter"), "command")).child();
        var inner = ((VInterface) field(structs(outer, "phases").getLast(), "action")).child();
        var phases = structs(inner, "phases");
        var missing = ((VInterface) field(phases.getFirst(), "action")).child();
        var local = ((VInterface) field(phases.getLast(), "action")).child();
        assertNull(snapshot.target(missing, "key"));
        assertSame(phases.getFirst(), snapshot.target(local, "key"));
    }

    @Test
    void tagFilteringRemovesTheWholeCompositeIndexAndDependentReference() throws Exception {
        var context = example();
        var filtered = context.makeValue("-server").schema();
        var machine = (Structural) filtered.findFieldable("combat.Machine");
        assertNull(machine.findField("combos").meta().get("index"));
        assertNull(machine.findForeignKey("combo"));
        assertNotNull(machine.findForeignKey("initial"));
    }

    enum InvalidCase { DUPLICATE_UNUSED_INDEX, MISSING_REQUIRED_TARGET, MISSING_ANCESTOR,
        RECORD_MISS_WITH_MISSING_SOURCE_SCOPE, DUPLICATE_STRUCT_MAP_KEY, WRONG_STRUCT_KEY_TYPE,
        CONTAINER_SOURCE, UNINDEXED_LIST, TRAVERSE_INTERFACE }

    @ParameterizedTest
    @EnumSource(InvalidCase.class)
    void rejectsInvalidVariantsWithoutCommittingInvalidGameData(InvalidCase invalid) throws Exception {
        example();
        Path module = root.resolve("combat");
        Path schema = module.resolve("combat.cfg");
        switch (invalid) {
            case DUPLICATE_UNUSED_INDEX, MISSING_REQUIRED_TARGET -> {
                Path file = module.resolve("_ability/1001.json");
                JSONObject ability = JSON.parseObject(Files.readString(file));
                var machine = ability.getJSONArray("machines").getJSONObject(0);
                if (invalid == InvalidCase.DUPLICATE_UNUSED_INDEX) machine.getJSONArray("markers").add(new JSONObject(machine.getJSONArray("markers").getJSONObject(0)));
                else machine.put("initial", "NotConfigured");
                Files.writeString(file, ability.toJSONString());
            }
            case MISSING_ANCESTOR -> Files.writeString(module.resolve("_encounter/1.json"),
                    "{\"id\":1,\"command\":{\"$type\":\"Jump\",\"key\":\"Done\"}}");
            case RECORD_MISS_WITH_MISSING_SOURCE_SCOPE -> replace(schema,
                    "[abilityId,machineKey,phaseKey]", "[abilityId,@MachineUse::machineKey,phaseKey]");
            case DUPLICATE_STRUCT_MAP_KEY -> replace(module.resolve("timeline.csv"),
                    "(Hit,0),Damage(25)", "(Hit,0),Damage(25),(Hit,0),Heal(1)");
            case WRONG_STRUCT_KEY_TYPE -> {
                replace(schema, "rewardKey:PhaseKey", "rewardKey:OtherPhaseKey");
                Files.writeString(schema, "\nstruct OtherPhaseKey { name:str; variant:int; }", StandardOpenOption.APPEND);
            }
            case CONTAINER_SOURCE -> replace(schema, "->reward:[rewardKey]", "->reward:[uses]");
            case UNINDEXED_LIST -> replace(schema, " (index='code')", "");
            case TRAVERSE_INTERFACE -> replace(schema, "->::fallback;", "->::fallback.amount;");
        }
        var errorType = switch (invalid) {
            case WRONG_STRUCT_KEY_TYPE, CONTAINER_SOURCE, UNINDEXED_LIST, TRAVERSE_INTERFACE -> CfgSchemaException.class;
            default -> CfgValueException.class;
        };
        assertThrows(errorType, () -> new Context(root).makeValue(), invalid.name());
    }

    private static void replace(Path file, String from, String to) throws Exception {
        String text = Files.readString(file);
        assertTrue(text.contains(from), from);
        Files.writeString(file, text.replace(from, to));
    }
    private static Value field(VStruct value, String name) { return ValueUtil.extractFieldValue(value, name); }
    private static List<VStruct> structs(VStruct value, String name) {
        return ((VList) field(value, name)).valueList().stream().map(VStruct.class::cast).toList();
    }
}
