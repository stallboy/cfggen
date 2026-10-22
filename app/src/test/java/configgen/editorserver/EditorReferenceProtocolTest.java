package configgen.editorserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.InternalReferenceFixture;
import configgen.schema.TableSchemaRefGraph;
import configgen.value.CfgValue;
import configgen.value.ValueReferences;
import configgen.value.ValueToJson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EditorReferenceProtocolTest {
    @TempDir static Path directory;
    private static CfgValue cfg;
    private static TableSchemaRefGraph graph;

    @BeforeAll
    static void load() throws Exception {
        InternalReferenceFixture.create(directory);
        Files.writeString(directory.resolve("mapping.csv"), "id,key,actions\nid,key,actions\n1,\"fire,2\",\"(ice,1),Idle(),(fire,2),Cast(9)\"\n");
        cfg = new configgen.ctx.Context(directory).makeValue();
        graph = new TableSchemaRefGraph(cfg.schema());
    }

    private static RecordService.RecordResponse query(String table, RecordService.RequestType type, String revision) {
        String id = cfg.getTable(table).primaryKeyMap().firstEntry().getKey().packStr();
        return new RecordService(cfg, graph, table, id, 2, true, 100, type, revision).retrieve();
    }

    private static RecordService.TableRecord record(String table, String revision) {
        return (RecordService.TableRecord) query(table, RecordService.RequestType.requestRecord, revision);
    }

    private static JSONObject json(String table) {
        return JSON.parseObject(JSON.toJSONString(record(table, "snapshot-a").object()));
    }

    private static JSONObject ref(JSONObject source, String firstField) {
        return source.getJSONArray("$refs").stream().map(item -> (JSONObject) item)
                .filter(item -> firstField.equals(item.getString("firstField"))).findFirst().orElseThrow();
    }

    @Test
    void localAndNearestAncestorPathsStayInsideTheirActualCollectionInstance() {
        var result = record("ability", "snapshot-a");
        assertEquals("snapshot-a", result.revision());
        assertTrue(result.refs().stream().allMatch(ref -> ref.depth() == -1), "Local references must not expand the outgoing frontier");
        JSONObject root = json("ability");
        JSONObject left = root.getJSONArray("branches").getJSONObject(0);
        JSONObject right = root.getJSONArray("branches").getJSONObject(1);
        assertEquals(List.of("branches", 0, "phases", 1), ref(left.getJSONArray("phases").getJSONObject(0), "next").getJSONArray("toPath"));
        assertEquals(List.of("branches", 1, "phases", 0), ref(right.getJSONArray("uses").getJSONObject(0), "key").getJSONArray("toPath"));
        assertNull(right.getJSONArray("phases").getJSONObject(0).get("$refs"), "Missing nullable target has no clickable link");
    }

    @Test
    void crossRecordAndInterfaceMapTargetsUseTheActualSerializedFieldChain() {
        JSONObject caller = ref(json("caller"), "ability");
        assertEquals("ability", caller.getString("toTable"));
        assertEquals(List.of("branches", 0), caller.getJSONArray("toPath"));
        assertEquals("snapshot-a", caller.getString("toRevision"));

        JSONObject mapping = json("mapping");
        JSONObject mapped = ref(mapping, "key");
        assertEquals(List.of("actions", 1, "value"), mapped.getJSONArray("toPath"));
        assertEquals("Effect.Cast", mapping.getJSONArray("actions").getJSONObject(1).getJSONObject("value").getString("$type"));
        assertFalse(mapped.getJSONArray("toPath").contains("$type"));
    }

    @Test
    void emptyObjectsRootEndpointsAndMixedSourcesRemainAddressable() {
        JSONObject addressed = json("addressed");
        JSONObject link = addressed.getJSONArray("groups").getJSONObject(0).getJSONArray("links").getJSONObject(0);
        assertTrue(link.getJSONArray("$refs").stream().map(item -> (JSONObject) item)
                .anyMatch(item -> new JSONArray(List.of("anchor")).equals(item.getJSONArray("toPath"))));
        assertEquals("AddressAnchor", addressed.getJSONObject("anchor").getString("$type"));
        JSONObject cast = json("combat.cast");
        assertEquals(List.of(), ref(cast, "@out").getJSONArray("toPath"));
        JSONObject use = cast.getJSONArray("uses").getJSONObject(0).getJSONArray("machines").getJSONObject(0)
                .getJSONArray("targets").getJSONObject(1);
        assertEquals(List.of("machines", 0, "phases", 1), ref(use, "phaseKey").getJSONArray("toPath"));
    }

    @Test
    void aNewSnapshotGetsNewPathRevisionsAndOrdinaryQueriesKeepTheirProjection() {
        JSONObject first = ref(json("caller"), "ability");
        var second = record("caller", "snapshot-b");
        JSONObject next = ref(JSON.parseObject(JSON.toJSONString(second.object())), "ability");
        assertNotEquals(first.getString("toRevision"), next.getString("toRevision"));
        assertEquals(second.revision(), next.getString("toRevision"));
        var references = ValueReferences.forQuery(cfg);
        var ordinary = new ValueToJson(cfg, new LinkedHashMap<>(), references).toJson(cfg.getTable("ability").valueList().getFirst());
        assertNull(ordinary.getJSONArray("branches").getJSONObject(0).getJSONArray("uses").getJSONObject(0).get("$refs"));
        JSONObject external = JSON.parseObject(JSON.toJSONString(new ValueToJson(cfg, new LinkedHashMap<>(), references)
                .toJson(cfg.getTable("caller").valueList().getFirst())));
        assertFalse(ref(external, "ability").containsKey("toPath"));
        assertFalse(ref(external, "ability").containsKey("toRevision"));
    }

    @Test
    void editorPathsDoNotChangeFrontierDepthOrUnreferencedRecordResults() {
        for (RecordService.RequestType type : RecordService.RequestType.values()) {
            var before = query("ability", type, null);
            var after = query("ability", type, "snapshot-a");
            assertEquals(briefIds(before), briefIds(after), type.toString());
        }
    }

    @Test
    void reorderedMapsProduceNewSnapshotPathsWithoutChangingTheOldSnapshot() throws Exception {
        Path reordered = directory.resolve("reordered");
        Files.createDirectories(reordered);
        Files.writeString(reordered.resolve("config.cfg"), """
                struct Key { label:str; variant:int; }
                interface Effect { struct Idle {} struct Cast { power:int; } }
                table mapping[id] { id:int; key:Key (pack); actions:map<Key,Effect> (pack); ->mapped:[key] ->::actions; }
                """);
        Files.writeString(reordered.resolve("mapping.csv"), "id,key,actions\nid,key,actions\n1,\"fire,2\",\"(fire,2),Cast(9),(ice,1),Idle()\"\n");
        var nextCfg = new configgen.ctx.Context(reordered).makeValue();
        var response = (RecordService.TableRecord) new RecordService(nextCfg, new TableSchemaRefGraph(nextCfg.schema()),
                "mapping", "1", 1, false, 20, RecordService.RequestType.requestRecord, "snapshot-b").retrieve();
        JSONObject next = ref(JSON.parseObject(JSON.toJSONString(response.object())), "key");
        assertEquals(List.of("actions", 0, "value"), next.getJSONArray("toPath"));
        assertEquals("snapshot-b", next.getString("toRevision"));
        assertEquals(List.of("actions", 1, "value"), ref(json("mapping"), "key").getJSONArray("toPath"));
    }

    private static List<String> briefIds(RecordService.RecordResponse response) {
        Collection<RecordService.BriefRecord> refs = switch (response) {
            case RecordService.TableRecord record -> record.refs();
            case RecordService.TableRecordRefs record -> record.refs();
            case RecordService.UnreferencedRecords record -> record.refs();
            default -> throw new AssertionError(response);
        };
        return refs.stream().map(ref -> ref.table() + "/" + ref.id() + "/" + ref.depth()).toList();
    }
}
