package configgen.value;

import com.alibaba.fastjson2.JSON;
import configgen.schema.ForeignKeySchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static configgen.value.CfgValue.*;
import static org.junit.jupiter.api.Assertions.*;

class ValueReferencesQuerySafetyTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ambiguousLocalKeysNeverProduceAnEditorTarget(boolean nullable) {
        for (String labels : List.of("first,second", "second,first", "first,second,third", "third,second,first")) {
            String duplicates = Arrays.stream(labels.split(","))
                    .map(label -> "{\"key\":7,\"label\":\"" + label + "\"}")
                    .collect(java.util.stream.Collectors.joining(","));
            CfgValueErrs errors = CfgValueErrs.of();
            CfgValue cfg = InternalReferenceIntegrationTest.parse("""
                    struct Item { key:int; label:str; }
                    table t[id] (json) {
                        id:int; items:list<Item> (index='key');
                        selected:int ->::items %s;
                        healthy:int ->::items;
                    }
                    """.formatted(nullable ? "(nullable)" : ""), Map.of("t", List.of(
                    "{\"id\":1,\"items\":[" + duplicates + ",{\"key\":8,\"label\":\"unique\"}],\"selected\":7,\"healthy\":8}")), errors);
            assertTrue(errors.errs().isEmpty());
            VStruct row = cfg.getTable("t").valueList().getFirst();
            ForeignKeySchema selected = row.schema().findForeignKey("selected");
            ForeignKeySchema healthy = row.schema().findForeignKey("healthy");

            ValueReferences strict = new ValueReferences(cfg);
            strict.resolve(errors);
            assertTrue(errors.errs().stream().anyMatch(error -> error instanceof ValueReferences.ReferenceError ref
                    && ref.reference().equals("index") && ref.reason().equals("duplicate local key 7")));
            assertNull(strict.result(row, selected), "An ambiguous key is invalid even on a nullable reference");

            ValueReferences query = ValueReferences.forQuery(cfg);
            assertNull(query.result(row, selected), labels);
            VList items = (VList) ValueUtil.extractFieldValue(row, "items");
            assertSame(items.valueList().getLast(), query.result(row, healthy).target());
            var links = ValueRefCollector.collectRefs(row, cfg, query, "snapshot-a");
            assertEquals(List.of("healthy"), links.stream().map(ValueRefCollector.FieldRef::firstField).toList());
            assertEquals(List.of("items", items.valueList().size() - 1), links.getFirst().toPath());

            var json = JSON.parseObject(JSON.toJSONString(ValueToJson.forEditor(cfg, new LinkedHashMap<>(), query,
                    "snapshot-a").toJson(row)));
            assertEquals(1, json.getJSONArray("$refs").size());
            assertEquals("healthy", json.getJSONArray("$refs").getJSONObject(0).getString("firstField"));
        }
    }

    @Test
    void pathsPreserveListInterfaceMapKeyAndMapValuePositionsAndObjectIdentity() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = InternalReferenceIntegrationTest.parse("""
                struct Item { value:int; }
                struct Key { code:str; }
                interface Choice { struct Box { item:Item; } }
                table t[id] {
                    id:int; anchor:Item; choices:list<Choice> (pack); entries:map<Key,Choice> (pack);
                    ->fixed:[] ->::anchor;
                }
                """, Map.of("t", List.of("""
                {"id":1,"anchor":{"value":42},
                 "choices":[{"$type":"Box","item":{"value":42}},{"$type":"Box","item":{"value":42}}],
                 "entries":[{"key":{"code":"second"},"value":{"$type":"Box","item":{"value":42}}},
                            {"key":{"code":"first"},"value":{"$type":"Box","item":{"value":42}}}]}
                """)), errors);
        assertTrue(errors.errs().isEmpty());
        ValueReferences references = ValueReferences.forQuery(cfg);
        VStruct row = cfg.getTable("t").valueList().getFirst();
        assertEquals(List.of(), references.jsonPath(row));
        assertEquals("", references.occurrence(row).path());
        assertEquals("t-1", references.occurrence(row).recordId());
        assertSame(cfg.getTable("t"), references.occurrence(row).table());
        assertEquals(List.of("anchor"), references.jsonPath((SimpleValue) ValueUtil.extractFieldValue(row, "anchor")));

        VList choices = (VList) ValueUtil.extractFieldValue(row, "choices");
        VInterface first = (VInterface) choices.valueList().getFirst();
        VInterface second = (VInterface) choices.valueList().getLast();
        assertEquals(first, second);
        assertNotSame(first.child(), second.child());
        for (int i = 0; i < choices.valueList().size(); i++) {
            VInterface choice = (VInterface) choices.valueList().get(i);
            assertEquals(List.of("choices", i), references.jsonPath(choice));
            assertEquals(List.of("choices", i), references.jsonPath(choice.child()));
            assertEquals(List.of("choices", i, "item"),
                    references.jsonPath((SimpleValue) ValueUtil.extractFieldValue(choice.child(), "item")));
            assertSame(row, references.occurrence(choice.child()).root());
        }

        VMap entries = (VMap) ValueUtil.extractFieldValue(row, "entries");
        int index = 0;
        for (var entry : entries.valueMap().entrySet()) {
            VInterface choice = (VInterface) entry.getValue();
            assertEquals(List.of("entries", index, "key"), references.jsonPath(entry.getKey()));
            assertEquals(List.of("entries", index, "value"), references.jsonPath(choice));
            assertEquals(List.of("entries", index, "value"), references.jsonPath(choice.child()));
            assertEquals(List.of("entries", index, "value", "item"),
                    references.jsonPath((SimpleValue) ValueUtil.extractFieldValue(choice.child(), "item")));
            index++;
        }
    }

    @Test
    void diagnosticsKeepTheirOriginalPathsThroughListsAndBothSidesOfMaps() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = InternalReferenceIntegrationTest.parse("""
                struct Item { key:str; }
                struct Use { selected:str ->@Holder::items; }
                struct Holder { items:list<Item> (pack,index='key'); uses:list<Use> (pack); }
                table t[id] { id:int; holders:list<Holder> (pack); mapped:map<Use,Use> (pack); }
                """, Map.of("t", List.of("""
                {"id":1,"holders":[{"items":[{"key":"exists"},{"key":"exists"}],"uses":[{"selected":"Missing"}]}],
                 "mapped":[{"key":{"selected":"missingKey"},"value":{"selected":"missingValue"}}]}
                """)), errors);
        assertTrue(errors.errs().isEmpty());
        new ValueReferences(cfg).resolve(errors);
        Set<ValueReferences.ReferenceError> expected = Set.of(
                new ValueReferences.ReferenceError("t-1", ".holders[0].items", "index", "duplicate local key exists"),
                new ValueReferences.ReferenceError("t-1", ".holders[0].uses[0]", "selected",
                        "target @Holder::items not found at items using [Missing]"),
                new ValueReferences.ReferenceError("t-1", ".mapped[missingKey].key", "selected", "missing ancestor Holder"),
                new ValueReferences.ReferenceError("t-1", ".mapped[missingKey]", "selected", "missing ancestor Holder"));
        assertEquals(expected, new HashSet<>(errors.errs()));
    }
}
