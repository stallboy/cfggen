package configgen.schema;

import configgen.schema.cfg.CfgReader;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncludeStructsTest {

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void includeSelf() {
        String str = """
                table t1[id]{
                    id:int;
                    v:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        Map<String, Nameable> r = IncludedStructs.findAllIncludedStructs(cfg.findItem("t1"));
        assertEquals(1, r.size());
        assertTrue(r.containsKey("t1"));
    }

    @Test
    void includeStructsInInterface() {
        String str = """
                interface f1{
                    struct s1{
                    }
                    struct s2{
                    }
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        Map<String, Nameable> r = IncludedStructs.findAllIncludedStructs(cfg.findItem("f1"));
        assertEquals(3, r.size());
        assertTrue(r.containsKey("f1"));
        assertTrue(r.containsKey("f1.s1"));
        assertTrue(r.containsKey("f1.s2"));
    }

    @Test
    void includeIndirect() {
        String str = """
                interface f1{
                    struct s1{
                    }
                    struct s2{
                    }
                }
                struct o1{
                    a:f1;
                }
                table t1[id]{
                    id:int;
                    v:o1;
                }
                
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        Map<String, Nameable> r = IncludedStructs.findAllIncludedStructs(cfg.findItem("t1"));
        assertEquals(5, r.size());
        assertTrue(r.containsKey("t1"));
        assertTrue(r.containsKey("o1"));
        assertTrue(r.containsKey("f1"));
        assertTrue(r.containsKey("f1.s1"));
        assertTrue(r.containsKey("f1.s2"));
    }

    @Test
    void includeListItem() {
        String str = """
                struct o1{
                    a:int;
                }
                table t1[id]{
                    id:int;
                    v:list<o1> (pack);
                }
                
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        Map<String, Nameable> r = IncludedStructs.findAllIncludedStructs(cfg.findItem("t1"));
        assertEquals(2, r.size());
        assertTrue(r.containsKey("t1"));
        assertTrue(r.containsKey("o1"));
    }

    @Test
    void includeMapKeyValue() {
        String str = """
                struct k1{
                    a:int;
                }
                struct v1{
                    a:int;
                }
                table t1[id]{
                    id:int;
                    v:map<k1, v1> (pack);
                }
                
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        Map<String, Nameable> r = IncludedStructs.findAllIncludedStructs(cfg.findItem("t1"));
        assertEquals(3, r.size());
        assertTrue(r.containsKey("t1"));
        assertTrue(r.containsKey("k1"));
        assertTrue(r.containsKey("v1"));
    }
}
