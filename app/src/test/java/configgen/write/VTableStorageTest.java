package configgen.write;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.data.CfgData.DTable;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.ValueUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static configgen.value.CfgValue.VList;
import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;
import static configgen.value.CfgValue.Value;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 addOrUpdateRecord 对嵌套 block 表的幂等性：同一份 record 反复写回 + reload，
 * wave/spawn 数不漂移、文件无残留行。
 *
 * <p>复现 levelmonster 的"点更新多出节点"：根因是清空行数算少了（VTableStorage.findRecordLoc
 * 用 mapToBlock 的逻辑行数，不含人工空分隔行），残留的旧行在 reload 时被当成新元素。
 */
class VTableStorageTest {
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
    void addOrUpdateRecord_idempotentWithNestedBlockAndManualSeparator() throws IOException {
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
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        // 列: id, waveIndex, startTime, 〔空分隔列〕, pathId, monsterId
        // 关键: 两个 wave 之间放一行全空人工分隔行（模拟 levelmonster 的视觉分组空白，
        // 它会被 CellParser 从内存表过滤，但物理行仍在文件里，使物理行跨度 > 逻辑元素行数）
        String csvStr = """
                关卡ID,波次,启动时间,总时间,路径,怪物ID
                id,waves,startTime,,pathId,monsterId
                1,1,0.0,1,1001,2001
                ,,1.0,1,1002,2002
                ,,1.0,1,1003,2003
                ,,,,,
                ,2,2.0,1,1004,2004
                ,,2.0,1,1005,2005
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        VTable vTable = cfgValue.getTable("t");
        DTable dTable = ctx.cfgData().getDTable("t");

        int expectedWaves = 2;
        int[] expectedSpawns = {3, 2};

        VStruct record = vTable.primaryKeyMap().values().iterator().next();
        Value pk = ValueUtil.extractPrimaryKeyValue(record, vTable.schema());

        for (int round = 0; round < 3; round++) {
            // 把当前 record 原样写回（模拟“没改数据点更新”）
            VTableStorage.addOrUpdateRecord(ctx, vTable, dTable, pk, record);

            ValueUpdater.NewCfgValueResult nr = ValueUpdater.updateByReloadTableData(ctx, cfgValue, vTable);
            ctx.updateDataAndValue(nr.newCfgData(), nr.newCfgValue());
            cfgValue = nr.newCfgValue();
            vTable = cfgValue.getTable("t");
            dTable = ctx.cfgData().getDTable("t");
            record = vTable.primaryKeyMap().get(pk);

            VList waves = (VList) record.values().get(1);
            assertEquals(expectedWaves, waves.valueList().size(),
                    "round " + round + " wave count drift");
            for (int i = 0; i < expectedWaves; i++) {
                VList spawns = (VList) ((VStruct) waves.valueList().get(i)).values().get(2);
                assertEquals(expectedSpawns[i], spawns.valueList().size(),
                        "round " + round + " wave " + (i + 1) + " spawn count drift");
            }
        }

        // 文件物理行无残留：block=1 下每个 spawn 占一行（wave header 行含 spawn1），
        // 故非空数据行数应 == spawn 总数；若清空行数算少了，残留旧行会使该数变大。
        List<String> lines = Files.readAllLines(tempDir.resolve("t.csv"));
        long nonEmptyDataLines = lines.stream()
                .skip(2)  // 跳过 headRow=2
                .filter(l -> !l.replace(",", "").trim().isEmpty())
                .count();
        long expected = Arrays.stream(expectedSpawns).sum();
        assertEquals(expected, nonEmptyDataLines, "physical data line count (residue check)");
    }
}
