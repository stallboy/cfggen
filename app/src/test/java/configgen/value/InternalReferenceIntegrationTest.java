package configgen.value;

import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import configgen.Resources;
import configgen.ctx.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static configgen.value.CfgValue.*;
import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceIntegrationTest {
    @Test
    void normalCsvContextPreservesFixedMemberReferences(@TempDir Path directory) {
        Resources.addTempFileFromText("config.cfg", directory, """
                struct Position { x:int; }
                table t[id] { id:int; movement:Position; ->fixed:[] ->::movement; }
                """);
        Resources.addTempFileFromText("t.csv", directory, ",,\nid,movement\n1,42\n");
        CfgValue cfg = new Context(directory).makeValue();
        ValueReferences references = new ValueReferences(cfg);
        CfgValueErrs errors = CfgValueErrs.of();
        references.resolve(errors);
        assertEquals(List.of(), errors.errs());
        VStruct root = cfg.getTable("t").valueList().getFirst();
        assertSame(root.values().get(1), references.result(root, root.schema().findForeignKey("fixed")).target());
    }

    @Test
    void readsEverySourceFromItsOriginalContextAndUsesTheTargetUniqueKey() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = parse("""
                struct Phase { key:str; }
                struct Machine { key:str; phases:list<Phase> (index='key'); }
                table ability[id] (json) { [code]; id:int; code:str; machines:list<Machine> (index='key'); }
                struct Use { abilityCode:str; machine:str; targets:list<Target>; }
                struct Target {
                    key:str;
                    ->target:[@Use::abilityCode,@Use::machine,key] ->ability[code]::machines.phases;
                }
                table source[id] (json) { id:int; uses:list<Use>; }
                """, Map.of(
                "ability", List.of("""
                        {"id":99,"code":"Fire","machines":[{"key":"Combat","phases":[{"key":"Hit"}]}]}
                        """),
                "source", List.of("""
                        {"id":1,"uses":[{"abilityCode":"Fire","machine":"Combat","targets":[{"key":"Hit"}]}]}
                        """)), errors);
        ValueReferences references = new ValueReferences(cfg);
        references.resolve(errors);
        assertEquals(List.of(), errors.errs());
        var source = references.occurrences().stream().filter(o -> o.value().name().equals("Target")).findFirst().orElseThrow();
        VStruct ability = cfg.getTable("ability").valueList().getFirst();
        var result = references.result(source.value(), source.value().schema().findForeignKey("target"));
        assertEquals("Hit", ValueUtil.extractFieldValueStr((VStruct) result.target(), "key"));
        assertSame(ability, result.targetRecord());
    }

    @Test
    void checksActualScopeEvenWhenNullableAndInterfaceAlsoAppearsAtTheRoot() {
        String schema = """
                interface Act {
                    struct Machine { phases:list<Phase> (index='key'); }
                    struct Jump { key:str ->@Act.Machine::phases (nullable); }
                }
                struct Phase { key:str; act:Act; }
                table t[id] (json) { id:int; action:Act; }
                """;
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = parse(schema, Map.of("t", List.of("""
                {"id":1,"action":{"$type":"Machine","phases":[{"key":"X","act":{"$type":"Jump","key":"X"}}]}}
                """)), errors);
        new ValueReferences(cfg).resolve(errors);
        assertEquals(List.of(), errors.errs());
        CfgValueErrs badErrors = CfgValueErrs.of();
        CfgValue bad = parse(schema, Map.of("t", List.of("""
                {"id":1,"action":{"$type":"Jump","key":"X"}}
                """)), badErrors);
        new ValueReferences(bad).resolve(badErrors);
        assertEquals(1, badErrors.errs().size());
        assertTrue(badErrors.errs().getFirst().msg().contains("missing ancestor Act.Machine"));
    }

    @Test
    void resolvesCompositeIndexesAndZeroSourceFixedMembers() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = parse("""
                struct Phase { key:str; variant:int; }
                table t[id] (json) {
                    id:int; phases:list<Phase> (index='key,variant'); movement:Phase;
                    key:str; variant:int;
                    ->target:[key,variant] ->::phases;
                    ->fixed:[] ->::movement;
                }
                """, Map.of("t", List.of("""
                {"id":1,"key":"Hit","variant":2,"phases":[{"key":"Hit","variant":1},{"key":"Hit","variant":2}],
                 "movement":{"key":"Move","variant":0}}
                """)), errors);
        ValueReferences references = new ValueReferences(cfg);
        references.resolve(errors);
        assertEquals(List.of(), errors.errs());
        VStruct root = cfg.getTable("t").valueList().getFirst();
        assertEquals("2", ValueUtil.extractFieldValueStr(
                (VStruct) references.result(root, root.schema().findForeignKey("target")).target(), "variant"));
        assertSame(root.values().get(2), references.result(root, root.schema().findForeignKey("fixed")).target());
    }

    @Test
    void mapKeysSelectInterfaceEndpoints() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = parse("""
                interface Action { struct Idle {} struct Run {} }
                table t[id] { id:int; actions:map<str,Action> (pack); selected:str ->::actions; }
                """, Map.of("t", List.of("""
                {"id":1,"selected":"go","actions":[{"key":"go","value":{"$type":"Run"}}]}
                """)), errors);
        ValueReferences references = new ValueReferences(cfg);
        references.resolve(errors);
        assertEquals(List.of(), errors.errs());
        VStruct root = cfg.getTable("t").valueList().getFirst();
        VInterface target = (VInterface) references.result(root, root.schema().findForeignKey("selected")).target();
        assertEquals("Run", target.child().name());
    }

    @Test
    void sourceRootDoesNotSwitchToExternalTargetRoot() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = parse("""
                struct Phase { key:str; }
                struct Link { ->target:[::abilityId,::key] ->ability::phases; }
                table ability[id] (json) { id:int; key:str; phases:list<Phase> (index='key'); }
                table source[id] (json) { id:int; abilityId:int; key:str; link:Link; }
                """, Map.of("ability", List.of("""
                {"id":2,"key":"Wrong","phases":[{"key":"Right"}]}
                """), "source", List.of("""
                {"id":1,"abilityId":2,"key":"Right","link":{}}
                """)), errors);
        ValueReferences references = new ValueReferences(cfg);
        references.resolve(errors);
        assertEquals(List.of(), errors.errs());
        VStruct link = (VStruct) cfg.getTable("source").valueList().getFirst().values().get(3);
        assertEquals("Right", ValueUtil.extractFieldValueStr(
                (VStruct) references.result(link, link.schema().findForeignKey("target")).target(), "key"));
    }

    @Test
    void nestedScopeMissNeverFallsBackToAnOuterScope() {
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue cfg = parse("""
                interface Act {
                    struct Machine { phases:list<Phase> (index='key'); }
                    struct Jump { key:str ->@Act.Machine::phases (nullable); }
                    struct None {}
                }
                struct Phase { key:str; act:Act; }
                table t[id] (json) { id:int; action:Act; }
                """, Map.of("t", List.of("""
                {"id":1,"action":{"$type":"Machine","phases":[
                  {"key":"Done","act":{"$type":"None"}},
                  {"key":"Nested","act":{"$type":"Machine","phases":[
                    {"key":"Other","act":{"$type":"Jump","key":"Done"}}
                  ]}}
                ]}}
                """)), errors);
        ValueReferences references = new ValueReferences(cfg);
        references.resolve(errors);
        assertEquals(List.of(), errors.errs());
        VStruct jump = references.occurrences().stream().map(ValueReferences.Occurrence::value)
                .filter(v -> v.name().equals("Jump")).findFirst().orElseThrow();
        assertNull(references.result(jump, jump.schema().findForeignKey("key")).target());
    }

    static CfgValue parse(String text, Map<String, List<String>> tables, CfgValueErrs errors) {
        CfgSchema schema = CfgReader.parse(text);
        schema.resolve().checkErrors();
        CfgValue cfg = CfgValue.of(schema);
        for (var entry : tables.entrySet()) {
            TableSchema table = schema.findTable(entry.getKey());
            ValueJsonParser parser = new ValueJsonParser(table, errors);
            List<VStruct> rows = entry.getValue().stream().map(parser::fromJson).toList();
            cfg.vTableMap().put(table.name(), new VTableCreator(table, errors).create(rows));
        }
        return cfg;
    }
}
