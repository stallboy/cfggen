package configgen.write;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.write.RecordBlock.RecordBlockTransformed;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordBlockMapperTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void testMapSimplePrimitives() {
        // Given: Table with primitive fields
        String cfgStr = """
                table simple[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                ID,姓名,年龄
                id,name,age
                10,Alice,25
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("simple.csv", tempDir, csvData);

        // When: Parse and map to block
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable table = cfgValue.getTable("simple");
        CfgValue.VStruct record = table.valueList().get(0);

        RecordBlock block = RecordBlockMapper.mapToBlock(record);

        // Then: Verify block has correct data
        assertEquals(1, block.getRowCount());

        // Use identity mapping to check
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1, 2));
        String[] row = transformed.getRow(0);
        assertEquals("10", row[0]);
        assertEquals("Alice", row[1]);
        assertEquals("25", row[2]);
    }

    @Test
    void testMapNestedStruct() {
        // Given: Table with nested struct
        String cfgStr = """
                struct Point {
                    x:int;
                    y:int;
                }

                table entity[id] {
                    id:int;
                    pos:Point;
                    name:str;
                }
                """;

        String csvData = """
                ID,位置X,位置Y,名称
                id,pos.x,pos.y,name
                1,100,200,Player
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("entity.csv", tempDir, csvData);

        // When: Parse and map to block
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable table = cfgValue.getTable("entity");
        CfgValue.VStruct record = (CfgValue.VStruct) table.valueList().get(0);

        RecordBlock block = RecordBlockMapper.mapToBlock(record);

        // Then: Verify nested struct is flattened
        assertEquals(1, block.getRowCount());

        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1, 2, 3));
        String[] row = transformed.getRow(0);
        assertEquals("1", row[0]);
        assertEquals("100", row[1]);
        assertEquals("200", row[2]);
        assertEquals("Player", row[3]);
    }

    @Test
    void testMapPackStruct() {
        // Given: Table with pack struct
        String cfgStr = """
                struct Point (pack) {
                    x:int;
                    y:int;
                }

                table entity[id] {
                    id:int;
                    pos:Point;
                }
                """;

        String csvData = """
                ID,位置
                id,pos
                1,"100,200"
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("entity.csv", tempDir, csvData);

        // When: Parse and map to block
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable table = cfgValue.getTable("entity");
        CfgValue.VStruct record = table.valueList().get(0);

        RecordBlock block = RecordBlockMapper.mapToBlock(record);

        // Then: Verify pack struct is in single cell
        assertEquals(1, block.getRowCount());

        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));
        String[] row = transformed.getRow(0);
        assertEquals("1", row[0]);
        assertEquals("100,200", row[1]);
    }

    @Test
    void testMapListWithSep() {
        // Given: Table with sep list
        String cfgStr = """
                table item[id] {
                    id:int;
                    tags:list<str> (sep=',');
                }
                """;

        String csvData = """
                ID,标签
                id,tags
                1,"tag1,tag2,tag3"
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("item.csv", tempDir, csvData);

        // When: Parse and map to block
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable table = cfgValue.getTable("item");
        CfgValue.VStruct record = table.valueList().get(0);

        RecordBlock block = RecordBlockMapper.mapToBlock(record);

        // Then: Verify sep list is in single cell
        assertEquals(1, block.getRowCount());

        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));
        String[] row = transformed.getRow(0);
        assertEquals("1", row[0]);
        assertEquals("tag1,tag2,tag3", row[1]);
    }

    @Test
    void testMapListWithFix() {
        // Given: Table with fix list
        String cfgStr = """
                table item[id] {
                    id:int;
                    values:list<int> (fix=3);
                }
                """;

        String csvData = """
                ID,值1,值2,值3
                id,values.0,values.1,values.2
                1,10,20,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("item.csv", tempDir, csvData);

        // When: Parse and map to block
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable table = cfgValue.getTable("item");
        CfgValue.VStruct record = table.valueList().get(0);

        RecordBlock block = RecordBlockMapper.mapToBlock(record);

        // Then: Verify fix list expands to multiple columns
        assertEquals(1, block.getRowCount());

        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1, 2, 3));
        String[] row = transformed.getRow(0);
        assertEquals("1", row[0]);
        assertEquals("10", row[1]);
        assertEquals("20", row[2]);
        assertEquals("30", row[3]);
    }

    @Test
    void testMapListWithBlock() {
        // Given: Table with block list
        String cfgStr = """
                table item[id] {
                    id:int;
                    values:list<int> (block=1);
                }
                """;

        String csvData = """
                ID,值
                id,values
                1,10
                ,20
                ,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("item.csv", tempDir, csvData);

        // When: Parse and map to block
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable table = cfgValue.getTable("item");
        CfgValue.VStruct record = table.valueList().get(0);

        RecordBlock block = RecordBlockMapper.mapToBlock(record);

        // Then: Verify block list expands to multiple rows
        assertEquals(3, block.getRowCount());

        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));
        assertEquals("1", transformed.getRow(0)[0]);
        assertEquals("10", transformed.getRow(0)[1]);
        assertNull(transformed.getRow(1)[0]);
        assertEquals("20", transformed.getRow(1)[1]);
        assertNull(transformed.getRow(2)[0]);
        assertEquals("30", transformed.getRow(2)[1]);
    }
}
