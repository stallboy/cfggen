package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.schema.TableSchemaRefGraph;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UnreferencedRecordCollectorTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void testCollectUnreferencedInTable() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                table item[id] {
                    id:int;
                    type_id:int ->type;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                1,weapon
                2,armor
                3,unused
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,
                id,type_id
                1,1
                2,2
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        assertEquals("type", result.tableName());
        assertEquals(1, result.unreferencedRecords().size());
        assertEquals("3", result.unreferencedRecords().get(0).primaryKey());
    }

    @Test
    void testCollectUnreferencedInTable_withEntry() {
        String cfgStr = """
                table type[id](entry='entry') {
                    id:int;
                    name:str;
                    entry:str;
                }
                table item[id] {
                    id:int;
                    type_id:int ->type;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,,,
                id,name,entry
                1,weapon,WeaponType
                2,armor,
                3,unused,
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,
                id,type_id
                1,2
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        // id=1 有 entry 值，不应被视为未引用
        // id=2 被引用，不应被视为未引用
        // id=3 既没有 entry 也没有被引用，应被视为未引用
        assertEquals(1, result.unreferencedRecords().size());
        assertEquals("3", result.unreferencedRecords().get(0).primaryKey());
    }

    @Test
    void testCollectUnreferencedInTable_noReferences() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                table item[id] {
                    id:int;
                    type_id:int ->type;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                1,weapon
                2,armor
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,
                id,type_id
                1,1
                2,2
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        assertEquals(0, result.unreferencedRecords().size());
    }

    @Test
    void testCollectUnreferencedInTable_allUnreferenced() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                table item[id] {
                    id:int;
                    other_id:int;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                1,weapon
                2,armor
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,
                id,other_id
                1,100
                2,200
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        assertEquals(2, result.unreferencedRecords().size());
    }

    @Test
    void testCollectUnreferencedInTable_withListForeignKey() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                table item[id] {
                    id:int;
                    tags:list<int> ->type (pack);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                1,common
                2,rare
                3,unused
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,,
                id,tags
                1,"1,2"
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        assertEquals(1, result.unreferencedRecords().size());
        assertEquals("3", result.unreferencedRecords().get(0).primaryKey());
    }

    @Test
    void testCollectUnreferencedInTable_withMapForeignKey() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                table config[id] {
                    id:int;
                    type_map:map<str,int> ->type (pack);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                1,fire
                2,ice
                3,unused
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String configCsv = """
                ,,
                id,type_map
                1,"hot,1,cold,2"
                """;
        Resources.addTempFileFromText("config.csv", tempDir, configCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        assertEquals(1, result.unreferencedRecords().size());
        assertEquals("3", result.unreferencedRecords().get(0).primaryKey());
    }

    @Test
    void testCollectUnreferenced_multipleTables() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                table item[id] {
                    id:int;
                    type_id:int ->type;
                }
                table config[id] {
                    id:int;
                    item_id:int ->item;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                1,weapon
                2,armor
                3,unused_type
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,
                id,type_id
                1,1
                2,2
                3,2
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);
        String configCsv = """
                ,
                id,item_id
                1,1
                """;
        Resources.addTempFileFromText("config.csv", tempDir, configCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        UnreferencedRecordCollector.Unreferenced result =
                UnreferencedRecordCollector.collectUnreferenced(cfgValue);

        assertEquals(4, result.total());
        assertEquals(3, result.tableToUnreferenced().size());

        // type 表有 1 个未引用记录
        assertTrue(result.tableToUnreferenced().containsKey("type"));
        assertEquals(1, result.tableToUnreferenced().get("type").size());

        // item 表有 1 个未引用记录（id=2,3）
        assertTrue(result.tableToUnreferenced().containsKey("item"));
        assertEquals(2, result.tableToUnreferenced().get("item").size());
    }

    @Test
    void testCollectUnreferenced_emptyTable() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        assertEquals(0, result.unreferencedRecords().size());
    }

    @Test
    void testCollectUnreferenced_withInterface() {
        String cfgStr = """
                table type[id] {
                    id:int;
                    name:str;
                }
                interface action {
                    struct cast {
                        type_id:int ->type;
                    }
                    struct talk {
                        text:str;
                    }
                }
                table config[id] {
                    id:int;
                    act:action;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String typeCsv = """
                ,
                id,name
                1,fire
                2,ice
                3,unused
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String configCsv = """
                ,,
                id,act.name,act.param1
                1,cast,1
                """;
        Resources.addTempFileFromText("config.csv", tempDir, configCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable typeTable = cfgValue.getTable("type");

        UnreferencedRecordCollector.UnreferencedInTable result =
                UnreferencedRecordCollector.collectUnreferencedInTable(cfgValue, typeTable, graph);

        assertEquals(2, result.unreferencedRecords().size());
    }
}
