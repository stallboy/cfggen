package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.value.CfgValue.VStruct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForeachVStructTest {

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
                2,cd""";
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        List<VStruct> list = new ArrayList<>();
        ForeachVStruct.foreach((vStruct, ctx1) -> list.add(vStruct), cfgValue);
        assertEquals(2, list.size());
    }

    @Test
    void foreach_VStruct() {
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

        List<VStruct> list = new ArrayList<>();
        ForeachVStruct.foreach((vStruct, ctx1) -> list.add(vStruct), cfgValue);
        assertEquals(4, list.size());
    }

    @Test
    void foreach_VInterface() {
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
                3,and,cast(123),"talk(222,world)"
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        List<VStruct> list = new ArrayList<>();
        ForeachVStruct.foreach((vStruct, ctx1) -> list.add(vStruct), cfgValue);
        assertEquals(8, list.size());
    }

    @Test
    void foreach_listVStruct() {
        String cfgStr = """
                struct s {
                    a:int;
                    b:int;
                }
                table t[id] {
                    id:int;
                    s:list<s> (pack);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,s
                1,"(11,22),(33,44)"
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        List<VStruct> list = new ArrayList<>();
        ForeachVStruct.foreach((vStruct, ctx1) -> list.add(vStruct), cfgValue);
        assertEquals(3, list.size());
    }

    @Test
    void foreach_mapVStruct() {
        String cfgStr = """
                struct s {
                    a:int;
                    b:int;
                }
                table t[id] {
                    id:int;
                    s:map<int,s> (pack);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,s
                1,"11,(11,22),33,(33,44)"
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        List<VStruct> list = new ArrayList<>();
        ForeachVStruct.foreach((vStruct, ctx1) -> list.add(vStruct), cfgValue);
        assertEquals(3, list.size());
    }


}
