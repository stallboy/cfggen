package configgen.value;

import com.alibaba.fastjson2.JSON;
import configgen.editorserver.SchemaService;
import configgen.editorserver.RecordService;
import configgen.editorserver.RecordRefIdsService;
import configgen.genbyai.SchemaToTs;
import configgen.schema.TableSchemaRefGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static configgen.value.CfgValue.*;
import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceQueryTest {
    private static class CountingTables extends TreeMap<String, VTable> {
        int scans;
        @Override public Collection<VTable> values() { scans++; return super.values(); }
    }

    @ParameterizedTest
    @EnumSource(RecordService.RequestType.class)
    void resolvesTheFullConfigurationOnlyOncePerRecordRequest(RecordService.RequestType type) {
        CfgValue fixture = fixture();
        var tables = new CountingTables();
        tables.putAll(fixture.vTableMap());
        var cfg = new CfgValue(fixture.schema(), tables, fixture.valueStat());
        boolean unreferenced = type == RecordService.RequestType.requestUnreferenced;
        var response = new RecordService(cfg, new TableSchemaRefGraph(cfg.schema()),
                unreferenced ? "source" : "ability", unreferenced ? null : "7", 2, true, 20, type, "editor-snapshot").retrieve();
        switch (response) {
            case RecordService.TableRecord record -> assertEquals(RecordService.ResultCode.ok, record.resultCode());
            case RecordService.TableRecordRefs refs -> assertEquals(RecordService.ResultCode.ok, refs.resultCode());
            case RecordService.UnreferencedRecords refs -> assertEquals(RecordService.ResultCode.ok, refs.resultCode());
            default -> fail("Unexpected response " + response);
        }
        assertEquals(1, tables.scans, "Expanding more records must not resolve the complete dataset again");
    }

    @ParameterizedTest
    @CsvSource({"true,1", "false,0"})
    void sharesOneBindingResultAcrossRecordIdTraversal(boolean internal, int expectedScans) {
        CfgValue fixture = fixture(internal);
        var tables = new CountingTables();
        tables.putAll(fixture.vTableMap());
        var cfg = new CfgValue(fixture.schema(), tables, fixture.valueStat());
        var response = new RecordRefIdsService(cfg, new TableSchemaRefGraph(cfg.schema()),
                "source", "1", 1, 2, 20).retrieve();
        assertEquals(RecordService.ResultCode.ok, response.resultCode());
        assertEquals(expectedScans, tables.scans, "Legacy queries need no resolver; internal queries share one per request");
    }

    @ParameterizedTest
    @CsvSource({
            "source,1,0,0,source/1/0",
            "source,1,0,2,source/1/0;ability/7/1",
            "ability,7,2,0,ability/7/0;source/1/-1"
    })
    void recordIdQueriesKeepRecordProjectionAndDepthForNewAndLegacyReferences(
            String table, String id, int inDepth, int outDepth, String expected) {
        for (boolean internal : List.of(false, true)) {
            CfgValue cfg = fixture(internal);
            var response = new RecordRefIdsService(cfg, new TableSchemaRefGraph(cfg.schema()),
                    table, id, inDepth, outDepth, 20).retrieve();
            assertEquals(RecordService.ResultCode.ok, response.resultCode());
            assertEquals(List.of(expected.split(";")), response.recordRefIds().stream()
                    .map(ref -> ref.table() + "/" + ref.id() + "/" + ref.depth()).toList());
        }
    }

    @Test
    void projectsInternalTargetsToTheirOwningRecordForRecordQueries() {
        CfgValue cfg = fixture();
        VStruct source = cfg.getTable("source").valueList().getFirst();
        var outgoing = ValueRefCollector.collectRefs(source, cfg);
        assertEquals(1, outgoing.size());
        assertEquals("ability", outgoing.getFirst().toTable());
        assertEquals("7", outgoing.getFirst().toId());
        assertEquals("ability", outgoing.getFirst().firstField());
        Map<ValueRefCollector.RefId, VStruct> frontier = new LinkedHashMap<>();
        var json = new ValueToJson(cfg, frontier).toJson(source);
        var linkJson = json.getJSONArray("links").getJSONObject(0);
        assertEquals("ability", linkJson.getJSONArray("$refs").getJSONObject(0).getString("firstField"));
        assertEquals(Set.of(new ValueRefCollector.RefId("ability", "7")), frontier.keySet());
        VTable ability = cfg.getTable("ability");
        Value pk = ability.primaryKeyMap().firstEntry().getKey();
        var incoming = new ValueRefInCollector(new TableSchemaRefGraph(cfg.schema()), cfg).collect(ability, pk);
        assertEquals(Set.of(new ValueRefCollector.RefId("source", "1")), incoming.keySet());
        var search = RefSearcher.search(cfg, "ability", null, Set.of());
        assertEquals(Set.of("source"), search.value2tables().get(pk));
    }

    @Test
    void localObjectLinksDoNotMakeTheRecordExternallyReferenced() {
        CfgValue cfg = InternalReferenceValueTest.fixture(false, "X");
        assertTrue(ValueRefCollector.collectRefs(cfg.getTable("t").valueList().getFirst(), cfg).isEmpty());
        var incoming = new ValueRefInCollector(new TableSchemaRefGraph(cfg.schema()), cfg);
        assertFalse(incoming.hasReference(cfg.getTable("t"), new VInt(1, configgen.data.Source.of())));
    }

    @Test
    void schemaExportsDescribePathsInsteadOfReadingLegacyKeys() {
        CfgValue cfg = fixture();
        String api = JSON.toJSONString(SchemaService.fromCfgValue(cfg));
        assertTrue(api.contains("pathReference"));
        assertTrue(api.contains("resultType"));
        assertTrue(api.contains("phases"));
        String ts = new SchemaToTs(cfg, cfg.schema().findTable("source"), List.of(), true).generate();
        assertTrue(ts.contains("->ability::phases"));
    }

    @Test
    void ancestorOnlySourcesUseTheExistingNodeHandleProtocol() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = InternalReferenceIntegrationTest.parse("""
                struct Phase { key:str; }
                table ability[id] (json) { id:int; phases:list<Phase> (index='key'); }
                struct Link { ->target:[::abilityId,::key] ->ability::phases; }
                table source[id] (json) { id:int; abilityId:int; key:str; link:Link; }
                """, Map.of("ability", List.of("""
                {"id":7,"phases":[{"key":"X"}]}
                """), "source", List.of("""
                {"id":1,"abilityId":7,"key":"X","link":{}}
                """)), errors);
        var json = new ValueToJson(cfg, new LinkedHashMap<>()).toJson(cfg.getTable("source").valueList().getFirst());
        assertEquals("@out", json.getJSONObject("link").getJSONArray("$refs").getJSONObject(0).getString("firstField"));
    }

    private static CfgValue fixture() {
        return fixture(true);
    }

    private static CfgValue fixture(boolean internal) {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = InternalReferenceIntegrationTest.parse("""
                struct Phase { key:str; }
                table ability[id] (json) { id:int; phases:list<Phase> (index='key'); }
                struct Link { ability:int; phase:str; %s }
                table source[id] (json) { id:int; links:list<Link>; }
                """.formatted(internal ? "->target:[ability,phase] ->ability::phases;" : "->target:[ability] ->ability;"),
                Map.of("ability", List.of("""
                {"id":7,"phases":[{"key":"X"}]}
                """), "source", List.of("""
                {"id":1,"links":[{"ability":7,"phase":"X"}]}
                """)), errors);
        assertTrue(errors.errs().isEmpty());
        return cfg;
    }
}
