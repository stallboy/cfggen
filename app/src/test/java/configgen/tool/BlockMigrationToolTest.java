package configgen.tool;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.util.Logger;
import configgen.value.ComparingBlockParser.BlockDiff;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockMigrationToolTest {
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
    void compare_detectsLegacyBreakDifference() {
        // 复现 levelmonster 场景：外层 block(waves) 的元素 struct 有兄弟 startTime 字段，spawn 行冗余填
        // startTime → 旧算法(firstColIndex-1 落在 startTime)提前 break 丢 spawn，新算法(看祖先 waveIndex 首列)不丢。
        // 与 CfgValueParserTest.parseCfgValue_nestedBlockWithRedundantOuterField 同一 fixture。
        String cfgStr = """
                struct LevelWave {
                    waveIndex:int;
                    startTime:float;
                    spawns:list<LevelSpawn> (block=1);
                }
                struct LevelSpawn {
                    pathId:int;
                    monsterId:int;
                }
                table t[id] {
                    id:int;
                    waves:list<LevelWave> (block=1);
                }
                """;
        String csvStr = """
                关卡ID,波次,启动时间,总时间,路径,怪物ID
                id,waves,startTime,,pathId,monsterId
                1,1,0.0,1,1001,2001
                ,,1.0,1,1002,2002
                ,,1.0,1,1003,2003
                ,2,2.0,1,1004,2004
                ,,2.0,1,1005,2005
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        BlockMigrationTool.MigrationReport report = BlockMigrationTool.compare(ctx);

        assertEquals(1, report.scannedTableCount());
        assertTrue(report.hasDiff());
        assertEquals(1, report.diffTableCount());
        assertEquals(2, report.diffRecordCount());

        List<BlockDiff> diffs = report.diffs().get("t");
        assertEquals(2, diffs.size());

        // wave1 的 spawns：旧 1 → 新 3，丢失行 1、2
        BlockDiff wave1 = diffs.get(0);
        assertEquals("spawns", wave1.fieldName());
        assertEquals(0, wave1.recordRow());
        assertEquals(1, wave1.legacySize());
        assertEquals(3, wave1.newSize());
        assertEquals(List.of(1, 2), wave1.newOnly());

        // wave2 的 spawns：旧 1 → 新 2，丢失行 4
        BlockDiff wave2 = diffs.get(1);
        assertEquals("spawns", wave2.fieldName());
        assertEquals(3, wave2.recordRow());
        assertEquals(1, wave2.legacySize());
        assertEquals(2, wave2.newSize());
        assertEquals(List.of(4), wave2.newOnly());
    }

    @Test
    void compare_noDifferenceForSimpleBlock() {
        // 单层 block，firstColIndex-1 落在 id 列(pk)，续行 id 空 → 新旧判定一致，无差异。
        // 与 CfgValueParserTest.parseCfgValue_listBlock 同一 fixture。
        String cfgStr = """
                table t[id] {
                    id:int;
                    intList:list<int> (block=3);
                }
                """;
        String csvStr = """
                ,,,
                id,intList.1,intList.2,intList.3
                1,111,222,333
                ,444,555,666
                ,777,,
                2,123,,""";
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        BlockMigrationTool.MigrationReport report = BlockMigrationTool.compare(ctx);

        assertEquals(1, report.scannedTableCount());
        assertFalse(report.hasDiff());
    }
}
