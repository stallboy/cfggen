package configgen.data;

import configgen.ctx.HeadRows;
import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static configgen.schema.FieldType.Primitive.STRING;
import static org.junit.jupiter.api.Assertions.*;

class CfgSchemaAlignToDataTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void align_addRemoveTable_firstFieldAsPrimaryKey() {
        String str = """
                table action[id] {
                    id:int;
                    name:str;
                }
                """;
        CfgSchema aligned = getAlignedCfgSchema(str);

        assertEquals(1, aligned.items().size());
        TableSchema t = aligned.findTable("rank");

        assertEquals(List.of("RankID"), t.primaryKey().fields());
        assertEquals(0, t.foreignKeys().size());
        assertEquals(0, t.uniqueKeys().size());
        assertEquals(3, t.fields().size());
        assertEquals(STRING, t.findField("RankID").type());
        assertEquals(STRING, t.findField("RankName").type());
        assertEquals(STRING, t.findField("RankShowName").type());

    }

    @Test
    void align_addRemoveField_typeKeep() {
        String str = """
                table rank[RankID] {
                    RankID:int;
                    RankName2:str;
                }
                """;
        CfgSchema aligned = getAlignedCfgSchema(str);

        assertEquals(1, aligned.items().size());
        TableSchema t = aligned.findTable("rank");
        assertEquals(List.of("RankID"), t.primaryKey().fields());
        assertEquals(0, t.foreignKeys().size());
        assertEquals(0, t.uniqueKeys().size());

        assertEquals(3, t.fields().size());
        assertEquals(FieldType.Primitive.INT, t.findField("RankID").type());
        assertEquals(STRING, t.findField("RankName").type());
        assertEquals(STRING, t.findField("RankShowName").type());
    }


    @Test
    void align_entryRemovedIfNotExist() {
        String str = """
                table rank[RankID] (enum='RankName2'){
                    RankID:int;
                    RankName2:str;
                }
                """;
        CfgSchema aligned = getAlignedCfgSchema(str);

        assertEquals(1, aligned.items().size());
        TableSchema t = aligned.findTable("rank");
        assertEquals(EntryType.ENo.NO, t.entry());
        assertEquals(3, t.fields().size());
    }


    @Test
    void align_foreignKeyRemovedIfNotExist() {
        String str = """
                table rank[RankID] {
                    RankID:int;
                    upperRank:int ->rank;
                }
                """;
        CfgSchema aligned = getAlignedCfgSchema(str);

        assertEquals(1, aligned.items().size());
        TableSchema t = aligned.findTable("rank");
        assertEquals(3, t.fields().size());
    }

    @Test
    void align_uniqKeyRemovedIfNotExist() {
        String str = """
                table rank[RankID] {
                    [upperRank];
                    RankID:int;
                    upperRank:int ->rank;
                }
                """;
        CfgSchema aligned = getAlignedCfgSchema(str);

        assertEquals(1, aligned.items().size());
        TableSchema t = aligned.findTable("rank");
        assertEquals(0, t.uniqueKeys().size());
    }


    @Test
    void error_JsonTableNotSupportExcel() {
        String str = """
                table rank[RankID] (json){
                    RankID:int;
                    RankName2:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        CfgData cfgData = CfgDataReaderTest.readFile("rank.csv", tempDir);
        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgSchema aligned = new CfgSchemaAlignToData(HeadRows.A2_Default).align(cfg, cfgData, errs);
        new CfgSchemaResolver(aligned, errs).resolve();

        assertEquals(1, errs.errs().size());
        CfgSchemaErrs.Err err = errs.errs().getFirst();
        assertInstanceOf(CfgSchemaErrs.JsonTableNotSupportExcel.class, err);
        CfgSchemaErrs.JsonTableNotSupportExcel e = (CfgSchemaErrs.JsonTableNotSupportExcel) err;
        assertEquals("rank", e.table());
        assertEquals(List.of("rank.csv[]"), e.excelSheetList());
    }

    @Test
    void error_DataHeadNameNotIdentifier_addTable() {
        check_DataHeadNameNotIdentifier(CfgSchema.of());
    }

    @Test
    void error_DataHeadNameNotIdentifier_updateTable() {
        String str = """
                table rank[RankID]{
                    RankID:int;
                    RankName:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        check_DataHeadNameNotIdentifier(cfg);
    }

    private void check_DataHeadNameNotIdentifier(CfgSchema cfg) {
        CfgData cfgData = CfgDataReaderTest.readFile("err.csv", tempDir);
        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgSchema aligned = new CfgSchemaAlignToData(HeadRows.A2_Default).align(cfg, cfgData, errs);
        new CfgSchemaResolver(aligned, errs).resolve();

        assertEquals(1, errs.errs().size());
        CfgSchemaErrs.Err err = errs.errs().getFirst();
        assertInstanceOf(CfgSchemaErrs.DataHeadNameNotIdentifier.class, err);
        CfgSchemaErrs.DataHeadNameNotIdentifier e = (CfgSchemaErrs.DataHeadNameNotIdentifier) err;
        assertEquals("err", e.table());
        assertEquals("中RankName", e.notIdentifierName());
    }

    @Test
    void error_PrimaryKeyNotEnumOrIntWhenEnum() {
        String str = """
                table rank[id] (enum='RankName') {
                    id:int;
                    RankName:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        CfgData cfgData = CfgDataReaderTest.readFile("rank.csv", tempDir);
        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgSchema aligned = new CfgSchemaAlignToData(HeadRows.A2_Default).align(cfg, cfgData, errs);
        new CfgSchemaResolver(aligned, errs).resolve();

        assertEquals(1, errs.errs().size());
        CfgSchemaErrs.Err err = errs.errs().getFirst();
        assertInstanceOf(CfgSchemaErrs.PrimaryKeyNotEnumOrIntWhenEnum.class, err);
    }


    private CfgSchema getAlignedCfgSchema(String str) {
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        CfgData cfgData = CfgDataReaderTest.readFile("rank.csv", tempDir);
        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgSchema aligned = new CfgSchemaAlignToData(HeadRows.A2_Default).align(cfg, cfgData, errs);
        new CfgSchemaResolver(aligned, errs).resolve();
        errs.checkErrors("align");
        return aligned;
    }


}