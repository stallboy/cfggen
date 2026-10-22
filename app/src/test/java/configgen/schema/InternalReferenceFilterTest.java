package configgen.schema;

import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceFilterTest {
    @Test
    void invalidReferenceCannotRemoveAValidIndexWithTheSameName() {
        CfgSchema full = CfgReader.parse("""
                struct Phase (c) { key:str (c); }
                table t[id] (json,c) {
                    id:int (c); phases:list<Phase> (index='key',c);
                    key:str (c); removedSource:str;
                    ->phases:[removedSource] ->::phases;
                    ->kept:[key] ->::phases;
                }
                """);
        full.resolve().checkErrors();
        CfgSchema filtered = new CfgSchemaFilterByTag(full, "c", CfgSchemaErrs.of()).filter();
        filtered.resolve().checkErrors();
        assertNotNull(filtered.findTable("t").findField("phases").meta().get("index"));
        assertNotNull(filtered.findTable("t").findForeignKey("kept"));
        assertNull(filtered.findTable("t").findForeignKey("phases"));
    }

    @Test
    void removesAnIndexAndItsReferencesWhenAKeyComponentIsFiltered() {
        CfgSchema full = CfgReader.parse("""
                struct Phase (c) { key:str (c); variant:int; }
                table t[id] (json,c) {
                    id:int (c); phases:list<Phase> (index='key,variant',c);
                    key:str (c); variant:int (c);
                    ->target:[key,variant] ->::phases;
                }
                """);
        full.resolve().checkErrors();
        CfgSchemaErrs errors = CfgSchemaErrs.of();
        CfgSchema filtered = new CfgSchemaFilterByTag(full, "c", errors).filter();
        filtered.resolve().checkErrors();
        assertNull(filtered.findTable("t").findField("phases").meta().get("index"));
        assertTrue(filtered.findTable("t").foreignKeys().isEmpty());
        assertFalse(errors.weakWarns().isEmpty());
    }

    @Test
    void preservesCompleteRelationsAndDropsUnavailableSourcePaths() {
        CfgSchema full = CfgReader.parse("""
                struct Phase (c) { key:str (c); }
                table t[id] (json,c) {
                    id:int (c); phases:list<Phase> (index='key',c);
                    key:str (c); missing:str;
                    ->kept:[key] ->::phases;
                    ->removed:[::missing] ->::phases;
                }
                """);
        full.resolve().checkErrors();
        CfgSchemaErrs errors = CfgSchemaErrs.of();
        CfgSchema filtered = new CfgSchemaFilterByTag(full, "c", errors).filter();
        filtered.resolve().checkErrors();
        assertNotNull(filtered.findTable("t").findForeignKey("kept"));
        assertNull(filtered.findTable("t").findForeignKey("removed"));
        assertNotNull(filtered.references().index(filtered.findTable("t").findField("phases")));
    }
}
