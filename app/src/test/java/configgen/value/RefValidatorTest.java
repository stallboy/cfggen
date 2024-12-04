package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.data.CfgData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RefValidatorTest {

    private @TempDir Path tempDir;

    @Test
    void error_RefNotNullableButCellEmpty() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    next:int ->t;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,next
                1,2
                2,""";
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValueErrs valueErrs = CfgValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(ctx.cfgSchema(), ctx, valueErrs);
        clientValueParser.parseCfgValue();

        assertEquals(1, valueErrs.errs().size());
        assertInstanceOf(CfgValueErrs.RefNotNullableButCellEmpty.class, valueErrs.errs().getFirst());
        CfgValueErrs.RefNotNullableButCellEmpty err = (CfgValueErrs.RefNotNullableButCellEmpty) valueErrs.errs().getFirst();
        assertEquals("t-2", err.recordId());
        assertTrue(err.value().source() instanceof CfgData.DCell cell &&
                cell.rowId().row() == 3 &&
                cell.col() == 1);
    }


    @Test
    void error_ForeignValueNotFound() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    next:int ->t;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,next
                1,2
                2,3""";
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValueErrs valueErrs = CfgValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(ctx.cfgSchema(), ctx, valueErrs);
        clientValueParser.parseCfgValue();

        assertEquals(1, valueErrs.errs().size());
        assertInstanceOf(CfgValueErrs.ForeignValueNotFound.class, valueErrs.errs().getFirst());
        CfgValueErrs.ForeignValueNotFound err = (CfgValueErrs.ForeignValueNotFound) valueErrs.errs().getFirst();
        assertEquals("t-2", err.recordId());
        assertTrue(err.value().source() instanceof CfgData.DCell cell &&
                cell.rowId().row() == 3 &&
                cell.col() == 1);
    }


    @Test
    void error_ForeignValueNotFound_InSubStruct() {
        String cfgStr = """
                struct s {
                    next:int ->t;
                }
                table t[id] {
                    id:int;
                    s:s;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,s
                1,2
                2,3""";
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValueErrs valueErrs = CfgValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(ctx.cfgSchema(), ctx, valueErrs);
        clientValueParser.parseCfgValue();

        assertEquals(1, valueErrs.errs().size());
        assertInstanceOf(CfgValueErrs.ForeignValueNotFound.class, valueErrs.errs().getFirst());
        CfgValueErrs.ForeignValueNotFound err = (CfgValueErrs.ForeignValueNotFound) valueErrs.errs().getFirst();
        assertEquals("t-2", err.recordId());
        assertTrue(err.value().source() instanceof CfgData.DCell cell &&
                cell.rowId().row() == 3 &&
                cell.col() == 1);
    }

    @Test
    void error_ForeignValueNotFound_InList() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    prevList:list<int> ->t (pack);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,prevList
                1,
                2,"1,3"
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValueErrs valueErrs = CfgValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(ctx.cfgSchema(), ctx, valueErrs);
        clientValueParser.parseCfgValue();

        assertEquals(1, valueErrs.errs().size());
        assertInstanceOf(CfgValueErrs.ForeignValueNotFound.class, valueErrs.errs().getFirst());
        CfgValueErrs.ForeignValueNotFound err = (CfgValueErrs.ForeignValueNotFound) valueErrs.errs().getFirst();
        assertEquals("t-2", err.recordId());
        assertTrue(err.value().source() instanceof CfgData.DCell cell &&
                cell.rowId().row() == 3 &&
                cell.col() == 1);
    }


    @Test
    void error_ForeignValueNotFound_InMap() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    map:map<str,int> ->t (pack);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,map
                1,"exist,2"
                2,"notExist,3"
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValueErrs valueErrs = CfgValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(ctx.cfgSchema(), ctx, valueErrs);
        clientValueParser.parseCfgValue();

        assertEquals(1, valueErrs.errs().size());
        assertInstanceOf(CfgValueErrs.ForeignValueNotFound.class, valueErrs.errs().getFirst());
        CfgValueErrs.ForeignValueNotFound err = (CfgValueErrs.ForeignValueNotFound) valueErrs.errs().getFirst();
        assertEquals("t-2", err.recordId());
        assertTrue(err.value().source() instanceof CfgData.DCell cell &&
                cell.rowId().row() == 3 &&
                cell.col() == 1);
    }


}
