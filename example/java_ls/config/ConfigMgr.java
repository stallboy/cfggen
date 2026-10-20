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

    private java.util.Map<Integer, config.ai.CfgAi> ai_ai_All;
    public java.util.Map<Integer, config.ai.CfgAi> _get_ai_ai_All() { return ai_ai_All; }
    public void _set_ai_ai_All(java.util.Map<Integer, config.ai.CfgAi> rows) { requireBuilding(); ai_ai_All = java.util.Collections.unmodifiableMap(rows); }

    public config.ai.CfgAi getAiAi(int iD) { return ai_ai_All.get(iD); }

    public java.util.Collection<config.ai.CfgAi> allAiAi() { return ai_ai_All.values(); }

    private java.util.Map<Integer, config.ai.CfgAi_action> ai_ai_action_All;
    public java.util.Map<Integer, config.ai.CfgAi_action> _get_ai_ai_action_All() { return ai_ai_action_All; }
    public void _set_ai_ai_action_All(java.util.Map<Integer, config.ai.CfgAi_action> rows) { requireBuilding(); ai_ai_action_All = java.util.Collections.unmodifiableMap(rows); }

    public config.ai.CfgAi_action getAiAi_action(int iD) { return ai_ai_action_All.get(iD); }

    public java.util.Collection<config.ai.CfgAi_action> allAiAi_action() { return ai_ai_action_All.values(); }

    private java.util.Map<Integer, config.ai.CfgAi_condition> ai_ai_condition_All;
    public java.util.Map<Integer, config.ai.CfgAi_condition> _get_ai_ai_condition_All() { return ai_ai_condition_All; }
    public void _set_ai_ai_condition_All(java.util.Map<Integer, config.ai.CfgAi_condition> rows) { requireBuilding(); ai_ai_condition_All = java.util.Collections.unmodifiableMap(rows); }

    public config.ai.CfgAi_condition getAiAi_condition(int iD) { return ai_ai_condition_All.get(iD); }

    public java.util.Collection<config.ai.CfgAi_condition> allAiAi_condition() { return ai_ai_condition_All.values(); }

    private config.equip.CfgEquip_config[] equip_equip_config_Refs;
    public config.equip.CfgEquip_config _get_equip_equip_config_Refs(int index) { return equip_equip_config_Refs[index]; }
    public void _set_equip_equip_config_Refs(config.equip.CfgEquip_config[] refs) { requireBuilding(); equip_equip_config_Refs = refs; }

    private java.util.Map<String, config.equip.CfgEquip_config> equip_equip_config_All;
    public java.util.Map<String, config.equip.CfgEquip_config> _get_equip_equip_config_All() { return equip_equip_config_All; }
    public void _set_equip_equip_config_All(java.util.Map<String, config.equip.CfgEquip_config> rows) { requireBuilding(); equip_equip_config_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.CfgEquip_config getEquipEquip_config(String entry) { return equip_equip_config_All.get(entry); }

    public java.util.Collection<config.equip.CfgEquip_config> allEquipEquip_config() { return equip_equip_config_All.values(); }

    private java.util.Map<Integer, config.equip.CfgJewelry> equip_jewelry_All;
    public java.util.Map<Integer, config.equip.CfgJewelry> _get_equip_jewelry_All() { return equip_jewelry_All; }
    public void _set_equip_jewelry_All(java.util.Map<Integer, config.equip.CfgJewelry> rows) { requireBuilding(); equip_jewelry_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.CfgJewelry getEquipJewelry(int iD) { return equip_jewelry_All.get(iD); }

    public java.util.Collection<config.equip.CfgJewelry> allEquipJewelry() { return equip_jewelry_All.values(); }

    private java.util.Map<config.CfgLevelRank, config.equip.CfgJewelry_random> equip_jewelry_random_All;
    public java.util.Map<config.CfgLevelRank, config.equip.CfgJewelry_random> _get_equip_jewelry_random_All() { return equip_jewelry_random_All; }
    public void _set_equip_jewelry_random_All(java.util.Map<config.CfgLevelRank, config.equip.CfgJewelry_random> rows) { requireBuilding(); equip_jewelry_random_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.CfgJewelry_random getEquipJewelry_random(config.CfgLevelRank lvlRank) { return equip_jewelry_random_All.get(lvlRank); }

    public java.util.Collection<config.equip.CfgJewelry_random> allEquipJewelry_random() { return equip_jewelry_random_All.values(); }

    private config.equip.CfgJewelry_suit[] equip_jewelry_suit_Refs;
    public config.equip.CfgJewelry_suit _get_equip_jewelry_suit_Refs(int index) { return equip_jewelry_suit_Refs[index]; }
    public void _set_equip_jewelry_suit_Refs(config.equip.CfgJewelry_suit[] refs) { requireBuilding(); equip_jewelry_suit_Refs = refs; }

    private java.util.Map<Integer, config.equip.CfgJewelry_suit> equip_jewelry_suit_All;
    public java.util.Map<Integer, config.equip.CfgJewelry_suit> _get_equip_jewelry_suit_All() { return equip_jewelry_suit_All; }
    public void _set_equip_jewelry_suit_All(java.util.Map<Integer, config.equip.CfgJewelry_suit> rows) { requireBuilding(); equip_jewelry_suit_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.CfgJewelry_suit getEquipJewelry_suit(int suitID) { return equip_jewelry_suit_All.get(suitID); }

    public java.util.Collection<config.equip.CfgJewelry_suit> allEquipJewelry_suit() { return equip_jewelry_suit_All.values(); }

    private config.equip.CfgRank_Detail[] equip_rank_Refs;
    public config.equip.CfgRank_Detail _get_equip_rank_Refs(int index) { return equip_rank_Refs[index]; }
    public void _set_equip_rank_Refs(config.equip.CfgRank_Detail[] refs) { requireBuilding(); equip_rank_Refs = refs; }

    private java.util.List<config.equip.CfgRank_Detail> equip_rank_All;
    public java.util.List<config.equip.CfgRank_Detail> _get_equip_rank_All() { return equip_rank_All; }
    public void _set_equip_rank_All(java.util.List<config.equip.CfgRank_Detail> rows) { requireBuilding(); equip_rank_All = java.util.Collections.unmodifiableList(rows); }

    public config.equip.CfgRank_Detail getEquipRank(int rankID) { return rankID >= 0 && rankID < equip_rank_All.size() ? equip_rank_All.get(rankID) : null; }

    public java.util.List<config.equip.CfgRank_Detail> allEquipRank() { return equip_rank_All; }

    private config.other.CfgArgCaptureMode_Detail[] other_ArgCaptureMode_Refs;
    public config.other.CfgArgCaptureMode_Detail _get_other_ArgCaptureMode_Refs(int index) { return other_ArgCaptureMode_Refs[index]; }
    public void _set_other_ArgCaptureMode_Refs(config.other.CfgArgCaptureMode_Detail[] refs) { requireBuilding(); other_ArgCaptureMode_Refs = refs; }

    private java.util.Map<String, config.other.CfgArgCaptureMode_Detail> other_ArgCaptureMode_All;
    public java.util.Map<String, config.other.CfgArgCaptureMode_Detail> _get_other_ArgCaptureMode_All() { return other_ArgCaptureMode_All; }
    public void _set_other_ArgCaptureMode_All(java.util.Map<String, config.other.CfgArgCaptureMode_Detail> rows) { requireBuilding(); other_ArgCaptureMode_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgArgCaptureMode_Detail getOtherArgCaptureMode(String name) { return other_ArgCaptureMode_All.get(name); }

    private java.util.Map<Integer, config.other.CfgArgCaptureMode_Detail> other_ArgCaptureMode_IdMap;
    public java.util.Map<Integer, config.other.CfgArgCaptureMode_Detail> _get_other_ArgCaptureMode_IdMap() { return other_ArgCaptureMode_IdMap; }
    public void _set_other_ArgCaptureMode_IdMap(java.util.Map<Integer, config.other.CfgArgCaptureMode_Detail> rows) { requireBuilding(); other_ArgCaptureMode_IdMap = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgArgCaptureMode_Detail getOtherArgCaptureModeById(int id) { return other_ArgCaptureMode_IdMap.get(id); }

    public java.util.Collection<config.other.CfgArgCaptureMode_Detail> allOtherArgCaptureMode() { return other_ArgCaptureMode_All.values(); }

    private java.util.Map<Integer, config.other.CfgDrop> other_drop_All;
    public java.util.Map<Integer, config.other.CfgDrop> _get_other_drop_All() { return other_drop_All; }
    public void _set_other_drop_All(java.util.Map<Integer, config.other.CfgDrop> rows) { requireBuilding(); other_drop_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgDrop getOtherDrop(int dropid) { return other_drop_All.get(dropid); }

    public java.util.Collection<config.other.CfgDrop> allOtherDrop() { return other_drop_All.values(); }

    private java.util.Map<config.other.CfgKey_test.Id1Id2Key, config.other.CfgKey_test> other_key_test_All;
    public java.util.Map<config.other.CfgKey_test.Id1Id2Key, config.other.CfgKey_test> _get_other_key_test_All() { return other_key_test_All; }
    public void _set_other_key_test_All(java.util.Map<config.other.CfgKey_test.Id1Id2Key, config.other.CfgKey_test> rows) { requireBuilding(); other_key_test_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgKey_test getOtherKey_test(int id1, long id2) { return other_key_test_All.get(new config.other.CfgKey_test.Id1Id2Key(id1, id2)); }

    private java.util.Map<config.other.CfgKey_test.Id1Id3Key, config.other.CfgKey_test> other_key_test_Id1Id3Map;
    public java.util.Map<config.other.CfgKey_test.Id1Id3Key, config.other.CfgKey_test> _get_other_key_test_Id1Id3Map() { return other_key_test_Id1Id3Map; }
    public void _set_other_key_test_Id1Id3Map(java.util.Map<config.other.CfgKey_test.Id1Id3Key, config.other.CfgKey_test> rows) { requireBuilding(); other_key_test_Id1Id3Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgKey_test getOtherKey_testById1Id3(int id1, int id3) { return other_key_test_Id1Id3Map.get(new config.other.CfgKey_test.Id1Id3Key(id1, id3)); }

    private java.util.Map<Long, config.other.CfgKey_test> other_key_test_Id2Map;
    public java.util.Map<Long, config.other.CfgKey_test> _get_other_key_test_Id2Map() { return other_key_test_Id2Map; }
    public void _set_other_key_test_Id2Map(java.util.Map<Long, config.other.CfgKey_test> rows) { requireBuilding(); other_key_test_Id2Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgKey_test getOtherKey_testById2(long id2) { return other_key_test_Id2Map.get(id2); }

    private java.util.Map<config.other.CfgKey_test.Id2Id3Key, config.other.CfgKey_test> other_key_test_Id2Id3Map;
    public java.util.Map<config.other.CfgKey_test.Id2Id3Key, config.other.CfgKey_test> _get_other_key_test_Id2Id3Map() { return other_key_test_Id2Id3Map; }
    public void _set_other_key_test_Id2Id3Map(java.util.Map<config.other.CfgKey_test.Id2Id3Key, config.other.CfgKey_test> rows) { requireBuilding(); other_key_test_Id2Id3Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgKey_test getOtherKey_testById2Id3(long id2, int id3) { return other_key_test_Id2Id3Map.get(new config.other.CfgKey_test.Id2Id3Key(id2, id3)); }

    public java.util.Collection<config.other.CfgKey_test> allOtherKey_test() { return other_key_test_All.values(); }

    private java.util.Map<Integer, config.other.CfgLoot> other_loot_All;
    public java.util.Map<Integer, config.other.CfgLoot> _get_other_loot_All() { return other_loot_All; }
    public void _set_other_loot_All(java.util.Map<Integer, config.other.CfgLoot> rows) { requireBuilding(); other_loot_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgLoot getOtherLoot(int lootid) { return other_loot_All.get(lootid); }

    public java.util.Collection<config.other.CfgLoot> allOtherLoot() { return other_loot_All.values(); }

    private java.util.Map<config.other.CfgLoot_item.LootidItemidKey, config.other.CfgLoot_item> other_loot_item_All;
    public java.util.Map<config.other.CfgLoot_item.LootidItemidKey, config.other.CfgLoot_item> _get_other_loot_item_All() { return other_loot_item_All; }
    public void _set_other_loot_item_All(java.util.Map<config.other.CfgLoot_item.LootidItemidKey, config.other.CfgLoot_item> rows) { requireBuilding(); other_loot_item_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgLoot_item getOtherLoot_item(int lootid, int itemid) { return other_loot_item_All.get(new config.other.CfgLoot_item.LootidItemidKey(lootid, itemid)); }

    public java.util.Collection<config.other.CfgLoot_item> allOtherLoot_item() { return other_loot_item_All.values(); }

    private java.util.Map<Integer, config.other.CfgMonster> other_monster_All;
    public java.util.Map<Integer, config.other.CfgMonster> _get_other_monster_All() { return other_monster_All; }
    public void _set_other_monster_All(java.util.Map<Integer, config.other.CfgMonster> rows) { requireBuilding(); other_monster_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgMonster getOtherMonster(int id) { return other_monster_All.get(id); }

    public java.util.Collection<config.other.CfgMonster> allOtherMonster() { return other_monster_All.values(); }

    private java.util.Map<Integer, config.other.CfgSign_in> other_sign_in_All;
    public java.util.Map<Integer, config.other.CfgSign_in> _get_other_sign_in_All() { return other_sign_in_All; }
    public void _set_other_sign_in_All(java.util.Map<Integer, config.other.CfgSign_in> rows) { requireBuilding(); other_sign_in_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.CfgSign_in getOtherSign_in(int id) { return other_sign_in_All.get(id); }

    public java.util.Collection<config.other.CfgSign_in> allOtherSign_in() { return other_sign_in_All.values(); }

    private java.util.Map<Integer, config.task.CfgTask> task_task_All;
    public java.util.Map<Integer, config.task.CfgTask> _get_task_task_All() { return task_task_All; }
    public void _set_task_task_All(java.util.Map<Integer, config.task.CfgTask> rows) { requireBuilding(); task_task_All = java.util.Collections.unmodifiableMap(rows); }

    public config.task.CfgTask getTaskTask(int taskid) { return task_task_All.get(taskid); }

    public java.util.Collection<config.task.CfgTask> allTaskTask() { return task_task_All.values(); }

    private java.util.Map<Integer, config.task.CfgTask2> task_task2_All;
    public java.util.Map<Integer, config.task.CfgTask2> _get_task_task2_All() { return task_task2_All; }
    public void _set_task_task2_All(java.util.Map<Integer, config.task.CfgTask2> rows) { requireBuilding(); task_task2_All = java.util.Collections.unmodifiableMap(rows); }

    public config.task.CfgTask2 getTaskTask2(int taskid) { return task_task2_All.get(taskid); }

    public java.util.Collection<config.task.CfgTask2> allTaskTask2() { return task_task2_All.values(); }

    private java.util.Map<Integer, config.task.CfgTask_extra_exp> task_task_extra_exp_All;
    public java.util.Map<Integer, config.task.CfgTask_extra_exp> _get_task_task_extra_exp_All() { return task_task_extra_exp_All; }
    public void _set_task_task_extra_exp_All(java.util.Map<Integer, config.task.CfgTask_extra_exp> rows) { requireBuilding(); task_task_extra_exp_All = java.util.Collections.unmodifiableMap(rows); }

    public config.task.CfgTask_extra_exp getTaskTask_extra_exp(int taskid) { return task_task_extra_exp_All.get(taskid); }

    public java.util.Collection<config.task.CfgTask_extra_exp> allTaskTask_extra_exp() { return task_task_extra_exp_All.values(); }

}
