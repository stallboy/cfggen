package config;

public class ConfigMgr {
    private static volatile ConfigMgr mgr;
    private boolean ready;
    private final java.util.Map<String, java.util.Map<?, java.util.List<String>>> origins = new java.util.LinkedHashMap<>();

    public <K> java.util.List<String> explain(String table, K key) {
        var rows = origins.get(table);
        return rows == null ? java.util.List.of() : rows.getOrDefault(key, java.util.List.of());
    }

    void setOrigins(String table, java.util.Map<?, java.util.List<String>> rows) {
        requireBuilding();
        origins.put(table, rows);
    }

    boolean isReady() { return ready; }
    void markReady() { ready = true; }
    private void requireBuilding() { if (ready) throw new IllegalStateException("Configuration is read-only"); }

    public static ConfigMgr getMgr() {
        return mgr;
    }

    public static void setMgr(ConfigMgr newMgr) {
        if (newMgr == null || !newMgr.ready) throw new IllegalStateException("Configuration is not ready");
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

    private config.equip.Equipconfig[] equip_equipconfig_Refs;
    public config.equip.Equipconfig _get_equip_equipconfig_Refs(int index) { return equip_equipconfig_Refs[index]; }
    public void _set_equip_equipconfig_Refs(config.equip.Equipconfig[] refs) { requireBuilding(); equip_equipconfig_Refs = refs; }

    private java.util.Map<String, config.equip.Equipconfig> equip_equipconfig_All;
    public java.util.Map<String, config.equip.Equipconfig> _get_equip_equipconfig_All() { return equip_equipconfig_All; }
    public void _set_equip_equipconfig_All(java.util.Map<String, config.equip.Equipconfig> rows) { requireBuilding(); equip_equipconfig_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.Equipconfig getEquipEquipconfig(String entry) { return equip_equipconfig_All.get(entry); }

    public java.util.Collection<config.equip.Equipconfig> allEquipEquipconfig() { return equip_equipconfig_All.values(); }

    private java.util.Map<Integer, config.equip.Jewelry> equip_jewelry_All;
    public java.util.Map<Integer, config.equip.Jewelry> _get_equip_jewelry_All() { return equip_jewelry_All; }
    public void _set_equip_jewelry_All(java.util.Map<Integer, config.equip.Jewelry> rows) { requireBuilding(); equip_jewelry_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.Jewelry getEquipJewelry(int iD) { return equip_jewelry_All.get(iD); }

    public java.util.Collection<config.equip.Jewelry> allEquipJewelry() { return equip_jewelry_All.values(); }

    private java.util.Map<config.LevelRank, config.equip.Jewelryrandom> equip_jewelryrandom_All;
    public java.util.Map<config.LevelRank, config.equip.Jewelryrandom> _get_equip_jewelryrandom_All() { return equip_jewelryrandom_All; }
    public void _set_equip_jewelryrandom_All(java.util.Map<config.LevelRank, config.equip.Jewelryrandom> rows) { requireBuilding(); equip_jewelryrandom_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.Jewelryrandom getEquipJewelryrandom(config.LevelRank lvlRank) { return equip_jewelryrandom_All.get(lvlRank); }

    public java.util.Collection<config.equip.Jewelryrandom> allEquipJewelryrandom() { return equip_jewelryrandom_All.values(); }

    private config.equip.Jewelrysuit[] equip_jewelrysuit_Refs;
    public config.equip.Jewelrysuit _get_equip_jewelrysuit_Refs(int index) { return equip_jewelrysuit_Refs[index]; }
    public void _set_equip_jewelrysuit_Refs(config.equip.Jewelrysuit[] refs) { requireBuilding(); equip_jewelrysuit_Refs = refs; }

    private java.util.Map<Integer, config.equip.Jewelrysuit> equip_jewelrysuit_All;
    public java.util.Map<Integer, config.equip.Jewelrysuit> _get_equip_jewelrysuit_All() { return equip_jewelrysuit_All; }
    public void _set_equip_jewelrysuit_All(java.util.Map<Integer, config.equip.Jewelrysuit> rows) { requireBuilding(); equip_jewelrysuit_All = java.util.Collections.unmodifiableMap(rows); }

    public config.equip.Jewelrysuit getEquipJewelrysuit(int suitID) { return equip_jewelrysuit_All.get(suitID); }

    public java.util.Collection<config.equip.Jewelrysuit> allEquipJewelrysuit() { return equip_jewelrysuit_All.values(); }

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

    private java.util.Map<config.other.Keytest.Id1Id2Key, config.other.Keytest> other_keytest_All;
    public java.util.Map<config.other.Keytest.Id1Id2Key, config.other.Keytest> _get_other_keytest_All() { return other_keytest_All; }
    public void _set_other_keytest_All(java.util.Map<config.other.Keytest.Id1Id2Key, config.other.Keytest> rows) { requireBuilding(); other_keytest_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Keytest getOtherKeytest(int id1, long id2) { return other_keytest_All.get(new config.other.Keytest.Id1Id2Key(id1, id2)); }

    private java.util.Map<config.other.Keytest.Id1Id3Key, config.other.Keytest> other_keytest_Id1Id3Map;
    public java.util.Map<config.other.Keytest.Id1Id3Key, config.other.Keytest> _get_other_keytest_Id1Id3Map() { return other_keytest_Id1Id3Map; }
    public void _set_other_keytest_Id1Id3Map(java.util.Map<config.other.Keytest.Id1Id3Key, config.other.Keytest> rows) { requireBuilding(); other_keytest_Id1Id3Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Keytest getOtherKeytestById1Id3(int id1, int id3) { return other_keytest_Id1Id3Map.get(new config.other.Keytest.Id1Id3Key(id1, id3)); }

    private java.util.Map<Long, config.other.Keytest> other_keytest_Id2Map;
    public java.util.Map<Long, config.other.Keytest> _get_other_keytest_Id2Map() { return other_keytest_Id2Map; }
    public void _set_other_keytest_Id2Map(java.util.Map<Long, config.other.Keytest> rows) { requireBuilding(); other_keytest_Id2Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Keytest getOtherKeytestById2(long id2) { return other_keytest_Id2Map.get(id2); }

    private java.util.Map<config.other.Keytest.Id2Id3Key, config.other.Keytest> other_keytest_Id2Id3Map;
    public java.util.Map<config.other.Keytest.Id2Id3Key, config.other.Keytest> _get_other_keytest_Id2Id3Map() { return other_keytest_Id2Id3Map; }
    public void _set_other_keytest_Id2Id3Map(java.util.Map<config.other.Keytest.Id2Id3Key, config.other.Keytest> rows) { requireBuilding(); other_keytest_Id2Id3Map = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Keytest getOtherKeytestById2Id3(long id2, int id3) { return other_keytest_Id2Id3Map.get(new config.other.Keytest.Id2Id3Key(id2, id3)); }

    public java.util.Collection<config.other.Keytest> allOtherKeytest() { return other_keytest_All.values(); }

    private java.util.Map<Integer, config.other.Loot> other_loot_All;
    public java.util.Map<Integer, config.other.Loot> _get_other_loot_All() { return other_loot_All; }
    public void _set_other_loot_All(java.util.Map<Integer, config.other.Loot> rows) { requireBuilding(); other_loot_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Loot getOtherLoot(int lootid) { return other_loot_All.get(lootid); }

    public java.util.Collection<config.other.Loot> allOtherLoot() { return other_loot_All.values(); }

    private java.util.Map<config.other.Lootitem.LootidItemidKey, config.other.Lootitem> other_lootitem_All;
    public java.util.Map<config.other.Lootitem.LootidItemidKey, config.other.Lootitem> _get_other_lootitem_All() { return other_lootitem_All; }
    public void _set_other_lootitem_All(java.util.Map<config.other.Lootitem.LootidItemidKey, config.other.Lootitem> rows) { requireBuilding(); other_lootitem_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Lootitem getOtherLootitem(int lootid, int itemid) { return other_lootitem_All.get(new config.other.Lootitem.LootidItemidKey(lootid, itemid)); }

    public java.util.Collection<config.other.Lootitem> allOtherLootitem() { return other_lootitem_All.values(); }

    private java.util.Map<Integer, config.other.Monster> other_monster_All;
    public java.util.Map<Integer, config.other.Monster> _get_other_monster_All() { return other_monster_All; }
    public void _set_other_monster_All(java.util.Map<Integer, config.other.Monster> rows) { requireBuilding(); other_monster_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Monster getOtherMonster(int id) { return other_monster_All.get(id); }

    public java.util.Collection<config.other.Monster> allOtherMonster() { return other_monster_All.values(); }

    private java.util.Map<Integer, config.other.Signin> other_signin_All;
    public java.util.Map<Integer, config.other.Signin> _get_other_signin_All() { return other_signin_All; }
    public void _set_other_signin_All(java.util.Map<Integer, config.other.Signin> rows) { requireBuilding(); other_signin_All = java.util.Collections.unmodifiableMap(rows); }

    public config.other.Signin getOtherSignin(int id) { return other_signin_All.get(id); }

    public java.util.Collection<config.other.Signin> allOtherSignin() { return other_signin_All.values(); }

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

    private java.util.Map<Integer, config.task.Taskextraexp> task_taskextraexp_All;
    public java.util.Map<Integer, config.task.Taskextraexp> _get_task_taskextraexp_All() { return task_taskextraexp_All; }
    public void _set_task_taskextraexp_All(java.util.Map<Integer, config.task.Taskextraexp> rows) { requireBuilding(); task_taskextraexp_All = java.util.Collections.unmodifiableMap(rows); }

    public config.task.Taskextraexp getTaskTaskextraexp(int taskid) { return task_taskextraexp_All.get(taskid); }

    public java.util.Collection<config.task.Taskextraexp> allTaskTaskextraexp() { return task_taskextraexp_All.values(); }

}
