package config;

public class ConfigMgr {
    private static volatile ConfigMgr mgr;
    private boolean ready;
    private final java.util.Map<String, java.util.Map<?, java.util.List<String>>> origins = new java.util.HashMap<>();

    public <K> java.util.List<String> explain(String table, K key) {
        var rows = origins.get(table);
        return rows == null ? java.util.List.of() : rows.getOrDefault(key, java.util.List.of());
    }

    void setOrigins(String table, java.util.Map<?, java.util.List<String>> rows) {
        requireBuilding();
        if (!rows.isEmpty()) {
            origins.put(table, rows);
        }
    }

    boolean isReady() { return ready; }
    void markReady() { ready = true; }
    private void requireBuilding() {
        if (ready) {
            throw new IllegalStateException("Configuration is read-only");
        }
    }

    public static ConfigMgr getMgr() {
        return mgr;
    }

    public static void setMgr(ConfigMgr newMgr) {
        if (newMgr == null || !newMgr.ready) {
            throw new IllegalStateException("Configuration is not ready");
        }
        mgr = newMgr;
    }

    private java.util.Map<Integer, config.ai.Ai> ai_ai_All;
    public java.util.Map<Integer, config.ai.Ai> _get_ai_ai_All() { return ai_ai_All; }
    public void _set_ai_ai_All(java.util.Map<Integer, config.ai.Ai> rows) { requireBuilding(); ai_ai_All = java.util.Collections.unmodifiableMap(rows); }

    public config.ai.Ai getAiAi(int iD) { return ai_ai_All.get(iD); }

    public java.util.Collection<config.ai.Ai> allAiAi() { return ai_ai_All.values(); }

    private java.util.Map<Integer, config.ai.AiAction> ai_ai_action_All;
    public java.util.Map<Integer, config.ai.AiAction> _get_ai_ai_action_All() { return ai_ai_action_All; }
    public void _set_ai_ai_action_All(java.util.Map<Integer, config.ai.AiAction> rows) { requireBuilding(); ai_ai_action_All = java.util.Collections.unmodifiableMap(rows); }

    public config.ai.AiAction getAiAiAction(int iD) { return ai_ai_action_All.get(iD); }

    public java.util.Collection<config.ai.AiAction> allAiAiAction() { return ai_ai_action_All.values(); }

    private java.util.Map<Integer, config.ai.AiCondition> ai_ai_condition_All;
    public java.util.Map<Integer, config.ai.AiCondition> _get_ai_ai_condition_All() { return ai_ai_condition_All; }
    public void _set_ai_ai_condition_All(java.util.Map<Integer, config.ai.AiCondition> rows) { requireBuilding(); ai_ai_condition_All = java.util.Collections.unmodifiableMap(rows); }

    public config.ai.AiCondition getAiAiCondition(int iD) { return ai_ai_condition_All.get(iD); }

    public java.util.Collection<config.ai.AiCondition> allAiAiCondition() { return ai_ai_condition_All.values(); }

    private config.equip.EquipConfig[] equip_equip_config_Refs;
    public config.equip.EquipConfig _get_equip_equip_config_Refs(int index) { return equip_equip_config_Refs[index]; }
    public void _set_equip_equip_config_Refs(config.equip.EquipConfig[] refs) { requireBuilding(); equip_equip_config_Refs = refs; }

    private java.util.Map<String, config.equip.EquipConfig> equip_equip_config_All;
    public java.util.Map<String, config.equip.EquipConfig> _get_equip_equip_config_All() { return equip_equip_config_All; }
    public void _set_equip_equip_config_All(java.util.Map<String, config.equip.EquipConfig> rows) { requireBuilding(); equip_equip_config_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.EquipConfig getEquipEquipConfig(String entry) { return equip_equip_config_All.get(entry); }

    public java.util.Collection<config.equip.EquipConfig> allEquipEquipConfig() { return equip_equip_config_All.values(); }

    private java.util.Map<Integer, config.equip.Jewelry> equip_jewelry_All;
    public java.util.Map<Integer, config.equip.Jewelry> _get_equip_jewelry_All() { return equip_jewelry_All; }
    public void _set_equip_jewelry_All(java.util.Map<Integer, config.equip.Jewelry> rows) { requireBuilding(); equip_jewelry_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.Jewelry getEquipJewelry(int iD) { return equip_jewelry_All.get(iD); }

    public java.util.Collection<config.equip.Jewelry> allEquipJewelry() { return equip_jewelry_All.values(); }

    private java.util.Map<config.LevelRank, config.equip.JewelryRandom> equip_jewelry_random_All;
    public java.util.Map<config.LevelRank, config.equip.JewelryRandom> _get_equip_jewelry_random_All() { return equip_jewelry_random_All; }
    public void _set_equip_jewelry_random_All(java.util.Map<config.LevelRank, config.equip.JewelryRandom> rows) { requireBuilding(); equip_jewelry_random_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.JewelryRandom getEquipJewelryRandom(config.LevelRank lvlRank) { return equip_jewelry_random_All.get(lvlRank); }

    public java.util.Collection<config.equip.JewelryRandom> allEquipJewelryRandom() { return equip_jewelry_random_All.values(); }

    private config.equip.JewelrySuit[] equip_jewelry_suit_Refs;
    public config.equip.JewelrySuit _get_equip_jewelry_suit_Refs(int index) { return equip_jewelry_suit_Refs[index]; }
    public void _set_equip_jewelry_suit_Refs(config.equip.JewelrySuit[] refs) { requireBuilding(); equip_jewelry_suit_Refs = refs; }

    private java.util.Map<Integer, config.equip.JewelrySuit> equip_jewelry_suit_All;
    public java.util.Map<Integer, config.equip.JewelrySuit> _get_equip_jewelry_suit_All() { return equip_jewelry_suit_All; }
    public void _set_equip_jewelry_suit_All(java.util.Map<Integer, config.equip.JewelrySuit> rows) { requireBuilding(); equip_jewelry_suit_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.JewelrySuit getEquipJewelrySuit(int suitID) { return equip_jewelry_suit_All.get(suitID); }

    public java.util.Collection<config.equip.JewelrySuit> allEquipJewelrySuit() { return equip_jewelry_suit_All.values(); }

    private config.equip.Rank_Detail[] equip_rank_Refs;
    public config.equip.Rank_Detail _get_equip_rank_Refs(int index) { return equip_rank_Refs[index]; }
    public void _set_equip_rank_Refs(config.equip.Rank_Detail[] refs) { requireBuilding(); equip_rank_Refs = refs; }

    private java.util.List<config.equip.Rank_Detail> equip_rank_All;
    public java.util.List<config.equip.Rank_Detail> _get_equip_rank_All() { return equip_rank_All; }
    public void _set_equip_rank_All(java.util.List<config.equip.Rank_Detail> rows) { requireBuilding(); equip_rank_All = java.util.Collections.unmodifiableList(rows); }

    public config.equip.Rank_Detail getEquipRank(int rankID) { return rankID >= 0 && rankID < equip_rank_All.size() ? equip_rank_All.get(rankID) : null; }

    public java.util.List<config.equip.Rank_Detail> allEquipRank() { return equip_rank_All; }

    private config.other.ArgCaptureMode_Detail[] other_ArgCaptureMode_Refs;
    public config.other.ArgCaptureMode_Detail _get_other_ArgCaptureMode_Refs(int index) { return other_ArgCaptureMode_Refs[index]; }
    public void _set_other_ArgCaptureMode_Refs(config.other.ArgCaptureMode_Detail[] refs) { requireBuilding(); other_ArgCaptureMode_Refs = refs; }

    private java.util.Map<String, config.other.ArgCaptureMode_Detail> other_ArgCaptureMode_All;
    public java.util.Map<String, config.other.ArgCaptureMode_Detail> _get_other_ArgCaptureMode_All() { return other_ArgCaptureMode_All; }
    public void _set_other_ArgCaptureMode_All(java.util.Map<String, config.other.ArgCaptureMode_Detail> rows) { requireBuilding(); other_ArgCaptureMode_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.ArgCaptureMode_Detail getOtherArgCaptureMode(String name) { return other_ArgCaptureMode_All.get(name); }

    private java.util.Map<Integer, config.other.ArgCaptureMode_Detail> other_ArgCaptureMode_IdMap;
    public java.util.Map<Integer, config.other.ArgCaptureMode_Detail> _get_other_ArgCaptureMode_IdMap() { return other_ArgCaptureMode_IdMap; }
    public void _set_other_ArgCaptureMode_IdMap(java.util.Map<Integer, config.other.ArgCaptureMode_Detail> rows) { requireBuilding(); other_ArgCaptureMode_IdMap = java.util.Collections.unmodifiableMap(rows); }

    public config.other.ArgCaptureMode_Detail getOtherArgCaptureModeById(int id) { return other_ArgCaptureMode_IdMap.get(id); }

    public java.util.Collection<config.other.ArgCaptureMode_Detail> allOtherArgCaptureMode() { return other_ArgCaptureMode_All.values(); }

    private java.util.Map<Integer, config.other.Drop> other_drop_All;
    public java.util.Map<Integer, config.other.Drop> _get_other_drop_All() { return other_drop_All; }
    public void _set_other_drop_All(java.util.Map<Integer, config.other.Drop> rows) { requireBuilding(); other_drop_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Drop getOtherDrop(int dropid) { return other_drop_All.get(dropid); }

    public java.util.Collection<config.other.Drop> allOtherDrop() { return other_drop_All.values(); }

    private java.util.Map<config.other.KeyTest.Id1Id2Key, config.other.KeyTest> other_key_test_All;
    public java.util.Map<config.other.KeyTest.Id1Id2Key, config.other.KeyTest> _get_other_key_test_All() { return other_key_test_All; }
    public void _set_other_key_test_All(java.util.Map<config.other.KeyTest.Id1Id2Key, config.other.KeyTest> rows) { requireBuilding(); other_key_test_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.KeyTest getOtherKeyTest(int id1, long id2) { return other_key_test_All.get(new config.other.KeyTest.Id1Id2Key(id1, id2)); }

    private java.util.Map<config.other.KeyTest.Id1Id3Key, config.other.KeyTest> other_key_test_Id1Id3Map;
    public java.util.Map<config.other.KeyTest.Id1Id3Key, config.other.KeyTest> _get_other_key_test_Id1Id3Map() { return other_key_test_Id1Id3Map; }
    public void _set_other_key_test_Id1Id3Map(java.util.Map<config.other.KeyTest.Id1Id3Key, config.other.KeyTest> rows) { requireBuilding(); other_key_test_Id1Id3Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.KeyTest getOtherKeyTestById1Id3(int id1, int id3) { return other_key_test_Id1Id3Map.get(new config.other.KeyTest.Id1Id3Key(id1, id3)); }

    private java.util.Map<Long, config.other.KeyTest> other_key_test_Id2Map;
    public java.util.Map<Long, config.other.KeyTest> _get_other_key_test_Id2Map() { return other_key_test_Id2Map; }
    public void _set_other_key_test_Id2Map(java.util.Map<Long, config.other.KeyTest> rows) { requireBuilding(); other_key_test_Id2Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.KeyTest getOtherKeyTestById2(long id2) { return other_key_test_Id2Map.get(id2); }

    private java.util.Map<config.other.KeyTest.Id2Id3Key, config.other.KeyTest> other_key_test_Id2Id3Map;
    public java.util.Map<config.other.KeyTest.Id2Id3Key, config.other.KeyTest> _get_other_key_test_Id2Id3Map() { return other_key_test_Id2Id3Map; }
    public void _set_other_key_test_Id2Id3Map(java.util.Map<config.other.KeyTest.Id2Id3Key, config.other.KeyTest> rows) { requireBuilding(); other_key_test_Id2Id3Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.KeyTest getOtherKeyTestById2Id3(long id2, int id3) { return other_key_test_Id2Id3Map.get(new config.other.KeyTest.Id2Id3Key(id2, id3)); }

    public java.util.Collection<config.other.KeyTest> allOtherKeyTest() { return other_key_test_All.values(); }

    private java.util.Map<Integer, config.other.Loot> other_loot_All;
    public java.util.Map<Integer, config.other.Loot> _get_other_loot_All() { return other_loot_All; }
    public void _set_other_loot_All(java.util.Map<Integer, config.other.Loot> rows) { requireBuilding(); other_loot_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Loot getOtherLoot(int lootid) { return other_loot_All.get(lootid); }

    public java.util.Collection<config.other.Loot> allOtherLoot() { return other_loot_All.values(); }

    private java.util.Map<config.other.LootItem.LootidItemidKey, config.other.LootItem> other_loot_item_All;
    public java.util.Map<config.other.LootItem.LootidItemidKey, config.other.LootItem> _get_other_loot_item_All() { return other_loot_item_All; }
    public void _set_other_loot_item_All(java.util.Map<config.other.LootItem.LootidItemidKey, config.other.LootItem> rows) { requireBuilding(); other_loot_item_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.LootItem getOtherLootItem(int lootid, int itemid) { return other_loot_item_All.get(new config.other.LootItem.LootidItemidKey(lootid, itemid)); }

    public java.util.Collection<config.other.LootItem> allOtherLootItem() { return other_loot_item_All.values(); }

    private java.util.Map<Integer, config.other.Monster> other_monster_All;
    public java.util.Map<Integer, config.other.Monster> _get_other_monster_All() { return other_monster_All; }
    public void _set_other_monster_All(java.util.Map<Integer, config.other.Monster> rows) { requireBuilding(); other_monster_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Monster getOtherMonster(int id) { return other_monster_All.get(id); }

    public java.util.Collection<config.other.Monster> allOtherMonster() { return other_monster_All.values(); }

    private java.util.Map<Integer, config.other.SignIn> other_sign_in_All;
    public java.util.Map<Integer, config.other.SignIn> _get_other_sign_in_All() { return other_sign_in_All; }
    public void _set_other_sign_in_All(java.util.Map<Integer, config.other.SignIn> rows) { requireBuilding(); other_sign_in_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.SignIn getOtherSignIn(int id) { return other_sign_in_All.get(id); }

    public java.util.Collection<config.other.SignIn> allOtherSignIn() { return other_sign_in_All.values(); }

    private java.util.Map<Integer, config.task.Task> task_task_All;
    public java.util.Map<Integer, config.task.Task> _get_task_task_All() { return task_task_All; }
    public void _set_task_task_All(java.util.Map<Integer, config.task.Task> rows) { requireBuilding(); task_task_All = java.util.Collections.unmodifiableMap(rows); }

    public config.task.Task getTaskTask(int taskid) { return task_task_All.get(taskid); }

    public java.util.Collection<config.task.Task> allTaskTask() { return task_task_All.values(); }

    private java.util.Map<Integer, config.task.Task2> task_task2_All;
    public java.util.Map<Integer, config.task.Task2> _get_task_task2_All() { return task_task2_All; }
    public void _set_task_task2_All(java.util.Map<Integer, config.task.Task2> rows) { requireBuilding(); task_task2_All = java.util.Collections.unmodifiableMap(rows); }

    public config.task.Task2 getTaskTask2(int taskid) { return task_task2_All.get(taskid); }

    public java.util.Collection<config.task.Task2> allTaskTask2() { return task_task2_All.values(); }

    private java.util.Map<Integer, config.task.TaskExtraExp> task_task_extra_exp_All;
    public java.util.Map<Integer, config.task.TaskExtraExp> _get_task_task_extra_exp_All() { return task_task_extra_exp_All; }
    public void _set_task_task_extra_exp_All(java.util.Map<Integer, config.task.TaskExtraExp> rows) { requireBuilding(); task_task_extra_exp_All = java.util.Collections.unmodifiableMap(rows); }

    public config.task.TaskExtraExp getTaskTaskExtraExp(int taskid) { return task_task_extra_exp_All.get(taskid); }

    public java.util.Collection<config.task.TaskExtraExp> allTaskTaskExtraExp() { return task_task_extra_exp_All.values(); }

}
