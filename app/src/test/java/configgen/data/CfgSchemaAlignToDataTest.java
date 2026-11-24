package configgen.data;

import configgen.Resources;
import configgen.layout.DirectoryStructure;
import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
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
        TableSchema t = requireNonNull(aligned.findTable("rank"));

        assertEquals(List.of("RankID"), t.primaryKey().fields());
        assertEquals(0, t.foreignKeys().size());
        assertEquals(0, t.uniqueKeys().size());
        assertEquals(3, t.fields().size());
        assertEquals(STRING, requireNonNull(t.findField("RankID")).type());
        assertEquals(STRING, requireNonNull(t.findField("RankName")).type());
        assertEquals(STRING, requireNonNull(t.findField("RankShowName")).type());

    }

    @Test
    void align_preservesFileEndComments() {
        CfgSchema schema = CfgSchema.of();
        schema.setFileEndComment("", "// root");
        schema.setFileEndComment("items", "// items");
        CfgData data = new CfgData(java.util.Map.of(), new CfgDataStat());

        CfgSchema aligned = new CfgSchemaAlignToData().align(schema, data, CfgSchemaErrs.of());

        assertEquals(schema.fileEndComments(), aligned.fileEndComments());
        assertNotSame(schema.fileEndComments(), aligned.fileEndComments());
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
        TableSchema t = requireNonNull(aligned.findTable("rank"));
        assertEquals(List.of("RankID"), t.primaryKey().fields());
        assertEquals(0, t.foreignKeys().size());
        assertEquals(0, t.uniqueKeys().size());

        assertEquals(3, t.fields().size());
        assertEquals(FieldType.Primitive.INT, requireNonNull(t.findField("RankID")).type());
        assertEquals(STRING, requireNonNull(t.findField("RankName")).type());
        assertEquals(STRING, requireNonNull(t.findField("RankShowName")).type());
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
        TableSchema t = requireNonNull(aligned.findTable("rank"));
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
        TableSchema t = requireNonNull(aligned.findTable("rank"));
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
        TableSchema t = requireNonNull(aligned.findTable("rank"));
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
        CfgSchema aligned = new CfgSchemaAlignToData().align(cfg, cfgData, errs);
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
        CfgSchema aligned = new CfgSchemaAlignToData().align(cfg, cfgData, errs);
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
        CfgSchema aligned = new CfgSchemaAlignToData().align(cfg, cfgData, errs);
        new CfgSchemaResolver(aligned, errs).resolve();

        assertEquals(1, errs.errs().size());
        CfgSchemaErrs.Err err = errs.errs().getFirst();
        assertInstanceOf(CfgSchemaErrs.PrimaryKeyNotEnumOrIntWhenEnum.class, err);
    }

    @Test
    void error_FieldHeaderSpanNotEnough() {
        String str = """
                struct Vec2 {
                    x:int;
                    y:int;
                }
                table span_err[id] {
                    id:int;
                    vec:Vec2;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        // CSV 只有 2 列：id, vec
        // vec 字段需要 span=2（x 和 y），但 header 只剩 1 列
        String csvContent = """
                ID,Vec
                id,vec
                1,1
                """;
        Resources.addTempFileFromText("span_err.csv", tempDir, csvContent);

        // 直接从文件系统读取
        ReadCsv csvReader = new ReadCsv(StandardCharsets.UTF_8);
        CfgDataReader fastDataReader = new CfgDataReader(HeadRows.A2_Default, csvReader, ReadByFastExcel.INSTANCE);
        CfgData cfgData = fastDataReader.readCfgData(new DirectoryStructure(tempDir).getExcelFiles(), null, CfgSchemaErrs.of());

        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgSchema aligned = new CfgSchemaAlignToData().align(cfg, cfgData, errs);
        new CfgSchemaResolver(aligned, errs).resolve();

        assertEquals(1, errs.errs().size());
        CfgSchemaErrs.Err err = errs.errs().getFirst();
        assertInstanceOf(CfgSchemaErrs.FieldHeaderSpanNotEnough.class, err);
        CfgSchemaErrs.FieldHeaderSpanNotEnough e = (CfgSchemaErrs.FieldHeaderSpanNotEnough) err;
        assertEquals("span_err", e.table());
        assertEquals("vec", e.field());
        assertEquals(2, e.expectedSpan());
        assertEquals(1, e.headerRemain());
    }


    private CfgSchema getAlignedCfgSchema(String str) {
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        CfgData cfgData = CfgDataReaderTest.readFile("rank.csv", tempDir);
        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgSchema aligned = new CfgSchemaAlignToData().align(cfg, cfgData, errs);
        new CfgSchemaResolver(aligned, errs).resolve();
        errs.checkErrors("align");
        return aligned;
    }


}