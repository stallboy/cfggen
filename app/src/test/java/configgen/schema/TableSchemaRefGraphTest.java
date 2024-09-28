package configgen.schema;

import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static configgen.schema.TableSchemaRefGraph.*;
import static org.junit.jupiter.api.Assertions.*;

class TableSchemaRefGraphTest {

    @Test
    void noRefInAndOut_returnEmptySet() {
        String str = """
                table t1[id]{
                    id:int;
                    v:int;
                }
                table t2[id] {
                    id:int;
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();


        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfg);
        assertEquals(2, graph.refsMap().size());
        {
            Refs ref1 = graph.refsMap().get("t1");
            assertEquals(Set.of(), ref1.refOut());
            assertEquals(Set.of(), ref1.refIn());
        }

        {
            Refs ref2 = graph.refsMap().get("t2");
            assertEquals(Set.of(), ref2.refOut());
            assertEquals(Set.of(), ref2.refIn());
        }

    }

    @Test
    void directRefInAndOut() {
        String str = """
                table t1[id]{
                    id:int;
                    v:int ->t2;
                }
                table t2[id] {
                    id:int;
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();


        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfg);
        assertEquals(2, graph.refsMap().size());
        {
            Refs ref1 = graph.refsMap().get("t1");
            assertEquals(Set.of("t2"), ref1.refOut());
            assertEquals(Set.of(), ref1.refIn());
        }

        {
            Refs ref2 = graph.refsMap().get("t2");
            assertEquals(Set.of(), ref2.refOut());
            assertEquals(Set.of("t1"), ref2.refIn());
        }

    }


    @Test
    void directRefSelf() {
        String str = """
                table t1[id]{
                    id:int;
                    v:int ->t1;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfg);
        {
            Refs ref1 = graph.refsMap().get("t1");
            assertEquals(Set.of("t1"), ref1.refOut());
            assertEquals(Set.of("t1"), ref1.refIn());
        }
    }


    @Test
    void indirectRefInAndOut_byStruct() {
        String str = """
                struct s1 {
                     v:int ->t2;
                }
                table t1[id]{
                    id:int;
                    v:s1;
                }
                table t2[id] {
                    id:int;
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfg);
        assertEquals(2, graph.refsMap().size());
        {
            Refs ref1 = graph.refsMap().get("t1");
            assertEquals(Set.of("t2"), ref1.refOut());
            assertEquals(Set.of(), ref1.refIn());
        }

        {
            Refs ref2 = graph.refsMap().get("t2");
            assertEquals(Set.of(), ref2.refOut());
            assertEquals(Set.of("t1"), ref2.refIn());
        }

    }


    @Test
    void indirectRefInAndOut_byInterface() {
        String str = """
                interface action {
                    struct s1 {
                         v:int ->t2;
                    }
                }
                table t1[id]{
                    id:int;
                    v:action;
                }
                table t2[id] {
                    id:int;
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfg);
        assertEquals(2, graph.refsMap().size());
        {
            Refs ref1 = graph.refsMap().get("t1");
            assertEquals(Set.of("t2"), ref1.refOut());
            assertEquals(Set.of(), ref1.refIn());
        }

        {
            Refs ref2 = graph.refsMap().get("t2");
            assertEquals(Set.of(), ref2.refOut());
            assertEquals(Set.of("t1"), ref2.refIn());
        }

    }


}
