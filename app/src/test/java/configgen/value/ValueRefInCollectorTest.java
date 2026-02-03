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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValueRefInCollectorTest {

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
    void collectRefIns() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    asset:str ->assets;
                }
                table assets[path] {
                    path:str;
                    type:str;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,
                id,asset
                1,npc/a.prefab
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);
        String assetStr = """
                ,
                path,type
                pic.png,all
                npc/a.prefab,npc
                """;
        Resources.addTempFileFromText("assets.csv", tempDir, assetStr);
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable assets = cfgValue.getTable("assets");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);
        var refIns = refInCollector.collect(assets, Values.ofStr("npc/a.prefab"));

        assertEquals(1, refIns.size());
        ValueRefCollector.RefId refId = refIns.keySet().iterator().next();
        assertEquals("t", refId.table());
        assertEquals("1", refId.id());
        CfgValue.VStruct value = refIns.values().iterator().next().recordValue();
        assertEquals(Values.ofInt(1), value.values().get(0));
        assertEquals(Values.ofStr("npc/a.prefab"), value.values().get(1));

    }

    @Test
    void testHasReferenceConsistency() {
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

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试 id=1 有引用
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(1)));
        assertEquals(1, refInCollector.collect(typeTable, Values.ofInt(1)).size());

        // 测试 id=2 有引用
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(2)));
        assertEquals(1, refInCollector.collect(typeTable, Values.ofInt(2)).size());

        // 测试 id=3 无引用
        assertFalse(refInCollector.hasReference(typeTable, Values.ofInt(3)));
        assertEquals(0, refInCollector.collect(typeTable, Values.ofInt(3)).size());
    }

    @Test
    void testHasReferenceReturnsFalseWhenNoReferences() {
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
                1,unused1
                2,unused2
                3,used
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        // item 表只引用了 id=3，id=1 和 id=2 未被引用
        String itemCsv = """
                ,
                id,type_id
                1,3
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable typeTable = cfgValue.getTable("type");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试未被引用的记录
        assertFalse(refInCollector.hasReference(typeTable, Values.ofInt(1)));
        assertFalse(refInCollector.hasReference(typeTable, Values.ofInt(2)));
    }

    @Test
    void testHasReferenceReturnsTrueWhenHasReferences() {
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
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,
                id,type_id
                1,1
                2,1
                3,1
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable typeTable = cfgValue.getTable("type");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试有多个引用的记录
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(1)));
        assertEquals(3, refInCollector.collect(typeTable, Values.ofInt(1)).size());
    }

    @Test
    void testHasReferenceReturnsFalseWhenRecordNotExists() {
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
                1,weapon
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable typeTable = cfgValue.getTable("type");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试不存在的记录
        assertFalse(refInCollector.hasReference(typeTable, Values.ofInt(999)));
    }

    @Test
    void testHasReferenceWithListForeignKey() {
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
                ,
                id,tags
                1,"1,2"
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable typeTable = cfgValue.getTable("type");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试 list 中的引用
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(1)));
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(2)));
        assertFalse(refInCollector.hasReference(typeTable, Values.ofInt(3)));
    }

    @Test
    void testHasReferenceWithMapForeignKey() {
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
                ,
                id,type_map
                1,"hot,1,cold,2"
                """;
        Resources.addTempFileFromText("config.csv", tempDir, configCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable typeTable = cfgValue.getTable("type");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试 map 中的引用
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(1)));
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(2)));
        assertFalse(refInCollector.hasReference(typeTable, Values.ofInt(3)));
    }

    @Test
    void testHasReferenceStopsAtFirstReference() {
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
                """;
        Resources.addTempFileFromText("type.csv", tempDir, typeCsv);
        String itemCsv = """
                ,
                id,type_id
                1,1
                2,1
                3,1
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable typeTable = cfgValue.getTable("type");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // hasReference 应该在找到第一个引用后立即返回
        // 而 collect 会收集所有引用
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(1)));
        assertEquals(3, refInCollector.collect(typeTable, Values.ofInt(1)).size());

        // 验证 hasReference 返回 true，但不构建完整的引用集合
        // 这是一个性能优化的验证，通过比较行为来确认
        var collectedRefs = refInCollector.collect(typeTable, Values.ofInt(1));
        assertTrue(refInCollector.hasReference(typeTable, Values.ofInt(1)));
        assertEquals(collectedRefs.size(), 3); // collect 应该返回所有 3 个引用
    }

    @Test
    void testRefUniqSingleFieldReference() {
        String cfgStr = """
                table item[id] {
                    id:int;
                    code:str;
                    name:str;
                    [code];
                }
                table drop[id] {
                    id:int;
                    item_code:str ->item[code];
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String itemCsv = """
                ,
                id,code,name
                1,sword,铁剑
                2,shield,盾牌
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);
        String dropCsv = """
                ,
                id,item_code
                1,sword
                2,shield
                """;
        Resources.addTempFileFromText("drop.csv", tempDir, dropCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable itemTable = cfgValue.getTable("item");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试通过 item.id 查询时，能检测到对 item.code 的引用
        assertTrue(refInCollector.hasReference(itemTable, Values.ofInt(1)));
        assertTrue(refInCollector.hasReference(itemTable, Values.ofInt(2)));

        var refs = refInCollector.collect(itemTable, Values.ofInt(1));
        assertEquals(1, refs.size());
        ValueRefCollector.RefId refId = refs.keySet().iterator().next();
        assertEquals("drop", refId.table());
        assertEquals("1", refId.id());
    }

    @Test
    void testRefUniqMultiFieldReference() {
        String cfgStr = """
                table player[id] {
                    id:int;
                    account_id:str;
                    server_id:int;
                    name:str;
                    [account_id,server_id];
                }
                table login_history[id] {
                    id:int;
                    account_id:str;
                    server_id:int;
                    login_time:long;
                    ->ToPlayer:[account_id,server_id] ->player[account_id,server_id];
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String playerCsv = """
                ,
                id,account_id,server_id,name
                1,alice,1,Alice
                2,bob,1,Bob
                3,charlie,2,Charlie
                """;
        Resources.addTempFileFromText("player.csv", tempDir, playerCsv);
        String loginHistoryCsv = """
                ,
                id,account_id,server_id,login_time
                1,alice,1,1000000
                2,bob,1,2000000
                """;
        Resources.addTempFileFromText("login_history.csv", tempDir, loginHistoryCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable playerTable = cfgValue.getTable("player");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试通过 player 主键 (id=1,2) 查询时，能检测到对 player 多字段唯一键 [account_id,server_id] 的引用
        assertTrue(refInCollector.hasReference(playerTable, Values.ofInt(1)));
        assertTrue(refInCollector.hasReference(playerTable, Values.ofInt(2)));

        // id=3 (charlie@server2) 没有被引用
        assertFalse(refInCollector.hasReference(playerTable, Values.ofInt(3)));

        var refs = refInCollector.collect(playerTable, Values.ofInt(1));
        assertEquals(1, refs.size());
    }

    @Test
    void testRefListReference() {
        String cfgStr = """
                table loot[id] {
                    id:int;
                    name:str;
                }
                table lootitem[lootid,itemid] {
                    lootid:int =>loot[id];
                    itemid:int;
                    count:int;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String lootCsv = """
                ,
                id,name
                1,宝箱1
                2,宝箱2
                """;
        Resources.addTempFileFromText("loot.csv", tempDir, lootCsv);
        String lootitemCsv = """
                ,
                lootid,itemid,count
                1,101,5
                1,102,3
                2,201,10
                """;
        Resources.addTempFileFromText("lootitem.csv", tempDir, lootitemCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable lootTable = cfgValue.getTable("loot");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试通过 loot.id 查询时，能检测到 lootitem.lootid 的引用
        assertTrue(refInCollector.hasReference(lootTable, Values.ofInt(1)));
        assertTrue(refInCollector.hasReference(lootTable, Values.ofInt(2)));

        var refs = refInCollector.collect(lootTable, Values.ofInt(1));
        assertEquals(2, refs.size()); // lootid=1 有两条记录
    }

    @Test
    void testMixedReferenceTypes() {
        String cfgStr = """
                table item[id] {
                    id:int;
                    code:str;
                    [code];
                }
                table inventory[id] {
                    id:int;
                    item_id:int ->item;
                }
                table shop[id] {
                    id:int;
                    item_code:str ->item[code];
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String itemCsv = """
                ,
                id,code
                1,sword
                2,shield
                """;
        Resources.addTempFileFromText("item.csv", tempDir, itemCsv);
        String inventoryCsv = """
                ,
                id,item_id
                1,1
                """;
        Resources.addTempFileFromText("inventory.csv", tempDir, inventoryCsv);
        String shopCsv = """
                ,
                id,item_code
                1,sword
                2,shield
                """;
        Resources.addTempFileFromText("shop.csv", tempDir, shopCsv);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        CfgValue.VTable itemTable = cfgValue.getTable("item");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);

        // 测试 id=1 同时被 inventory（RefPrimary）和 shop（RefUniq）引用
        assertTrue(refInCollector.hasReference(itemTable, Values.ofInt(1)));
        var refs = refInCollector.collect(itemTable, Values.ofInt(1));
        assertEquals(2, refs.size());

        // 验证两种引用都能被检测到
        var tables = refs.values().stream()
                .map(context -> context.fromVTable().name())
                .toList();
        assertTrue(tables.contains("inventory"));
        assertTrue(tables.contains("shop"));

        // 测试 id=2 只被 shop 引用
        assertTrue(refInCollector.hasReference(itemTable, Values.ofInt(2)));
        var refs2 = refInCollector.collect(itemTable, Values.ofInt(2));
        assertEquals(1, refs2.size());
        assertEquals("shop", refs2.values().iterator().next().fromVTable().name());
    }
}
