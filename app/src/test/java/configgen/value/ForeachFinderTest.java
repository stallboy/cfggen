package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.schema.InterfaceSchema;
import configgen.value.ForeachFinder.FStructField;
import configgen.value.ForeachFinder.Finder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static configgen.value.ForeachFinder.*;
import static org.junit.jupiter.api.Assertions.*;

class ForeachFinderTest {

    private @TempDir Path tempDir;

    @Test
    void foreach_simple() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    s:text;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,s
                1,ab
                2,ab
                3,cd
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        List<CfgValue.Value> list = new ArrayList<>();
        Finder finder = new Finder(List.of(new FStructField(1)));
        foreachVTable(list::add, cfgValue.getTable("t"), finder);
        assertEquals(3, list.size());
    }


    @Test
    void foreach_inVStruct() {
        String cfgStr = """
                struct s {
                    a:int;
                    b:int;
                }
                table t[id] {
                    id:int;
                    s:s;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,s.a,s.b
                1,11,22
                2,33,44""";
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        List<Integer> list = new ArrayList<>();
        Finder finder = new Finder(List.of(new FStructField(1), new FStructField(1)));
        foreachVTable((v) -> list.add(((CfgValue.VInt) v).value()), cfgValue.getTable("t"), finder);
        assertEquals(List.of(22, 44), list);
    }

    @Test
    void foreach_inVInterface() {
        String cfgStr = """
                interface action {
                    struct cast{
                        skillId:int;
                    }
                    struct talk{
                        talkId:int;
                        str:text;
                    }
                    struct and {
                        a1:action (pack);
                        a2:action (pack);
                    }
                }
                table t[id] {
                    id:int;
                    action:action;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,
                id,action.name,action.param1,action.param2
                1,cast,123,
                2,talk,111,hello
                3,and,cast(333),"talk(444,world)"
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        InterfaceSchema action = (InterfaceSchema) ctx.cfgSchema().findItem("action");

        List<Integer> list = new ArrayList<>();
        Finder finder = new Finder(List.of(new FStructField(1),
                new FInterfaceImpl(action.findImpl("talk")),
                new FStructField(0)));
        foreachVTable((v) -> list.add(((CfgValue.VInt) v).value()), cfgValue.getTable("t"), finder);
        assertEquals(List.of(111), list);
    }
}
