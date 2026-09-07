package config;

public class ConfigMgr {
    private static volatile ConfigMgr mgr;

    public static ConfigMgr getMgr() {
        return mgr;
    }

    public static void setMgr(ConfigMgr newMgr) {
        mgr = newMgr;
        ConfigMgrLoader.applySetAllRefs(mgr);
    }

    public java.util.Map<Integer, config.ai.CfgAi> ai_ai_All;

    public config.ai.CfgAi getAiAi(int iD) { return ai_ai_All.get(iD); }

    public java.util.Collection<config.ai.CfgAi> allAiAi() { return ai_ai_All.values(); }

    public java.util.Map<Integer, config.ai.CfgAi_action> ai_ai_action_All;

    public config.ai.CfgAi_action getAiAi_action(int iD) { return ai_ai_action_All.get(iD); }

    public java.util.Collection<config.ai.CfgAi_action> allAiAi_action() { return ai_ai_action_All.values(); }

    public java.util.Map<Integer, config.ai.CfgAi_condition> ai_ai_condition_All;

    public config.ai.CfgAi_condition getAiAi_condition(int iD) { return ai_ai_condition_All.get(iD); }

    public java.util.Collection<config.ai.CfgAi_condition> allAiAi_condition() { return ai_ai_condition_All.values(); }

    public java.util.Map<String, config.equip.CfgEquipconfig> equip_equipconfig_All;

    public config.equip.CfgEquipconfig getEquipEquipconfig(String entry) { return equip_equipconfig_All.get(entry); }

    public java.util.Collection<config.equip.CfgEquipconfig> allEquipEquipconfig() { return equip_equipconfig_All.values(); }

    public java.util.Map<Integer, config.equip.CfgJewelry> equip_jewelry_All;

    public config.equip.CfgJewelry getEquipJewelry(int iD) { return equip_jewelry_All.get(iD); }

    public java.util.Collection<config.equip.CfgJewelry> allEquipJewelry() { return equip_jewelry_All.values(); }

    public java.util.Map<config.CfgLevelRank, config.equip.CfgJewelryrandom> equip_jewelryrandom_All;

    public config.equip.CfgJewelryrandom getEquipJewelryrandom(config.CfgLevelRank lvlRank) { return equip_jewelryrandom_All.get(lvlRank); }

    public java.util.Collection<config.equip.CfgJewelryrandom> allEquipJewelryrandom() { return equip_jewelryrandom_All.values(); }

    public java.util.Map<Integer, config.equip.CfgJewelrysuit> equip_jewelrysuit_All;

    public config.equip.CfgJewelrysuit getEquipJewelrysuit(int suitID) { return equip_jewelrysuit_All.get(suitID); }

    public java.util.Collection<config.equip.CfgJewelrysuit> allEquipJewelrysuit() { return equip_jewelrysuit_All.values(); }

    public config.equip.CfgRank_Detail[] equip_rank_All;

    public config.equip.CfgRank_Detail getEquipRank(int rankID) { return rankID >= 0 && rankID < equip_rank_All.length ? equip_rank_All[rankID] : null; }

    public java.util.List<config.equip.CfgRank_Detail> allEquipRank() { return java.util.Arrays.asList(equip_rank_All); }

    public java.util.Map<String, config.other.CfgArgCaptureMode_Detail> other_ArgCaptureMode_All;

    public config.other.CfgArgCaptureMode_Detail getOtherArgCaptureMode(String name) { return other_ArgCaptureMode_All.get(name); }

    public java.util.Map<Integer, config.other.CfgArgCaptureMode_Detail> other_ArgCaptureMode_IdMap;

    public config.other.CfgArgCaptureMode_Detail getOtherArgCaptureModeById(int id) { return other_ArgCaptureMode_IdMap.get(id); }

    public java.util.Collection<config.other.CfgArgCaptureMode_Detail> allOtherArgCaptureMode() { return other_ArgCaptureMode_All.values(); }

    public java.util.Map<Integer, config.other.CfgDrop> other_drop_All;

    public config.other.CfgDrop getOtherDrop(int dropid) { return other_drop_All.get(dropid); }

    public java.util.Collection<config.other.CfgDrop> allOtherDrop() { return other_drop_All.values(); }

    public java.util.Map<config.other.CfgKeytest.Id1Id2Key, config.other.CfgKeytest> other_keytest_All;

    public config.other.CfgKeytest getOtherKeytest(int id1, long id2) { return other_keytest_All.get(new config.other.CfgKeytest.Id1Id2Key(id1, id2)); }

    public java.util.Map<config.other.CfgKeytest.Id1Id3Key, config.other.CfgKeytest> other_keytest_Id1Id3Map;

    public config.other.CfgKeytest getOtherKeytestById1Id3(int id1, int id3) { return other_keytest_Id1Id3Map.get(new config.other.CfgKeytest.Id1Id3Key(id1, id3)); }

    public java.util.Map<Long, config.other.CfgKeytest> other_keytest_Id2Map;

    public config.other.CfgKeytest getOtherKeytestById2(long id2) { return other_keytest_Id2Map.get(id2); }

    public java.util.Map<config.other.CfgKeytest.Id2Id3Key, config.other.CfgKeytest> other_keytest_Id2Id3Map;

    public config.other.CfgKeytest getOtherKeytestById2Id3(long id2, int id3) { return other_keytest_Id2Id3Map.get(new config.other.CfgKeytest.Id2Id3Key(id2, id3)); }

    public java.util.Collection<config.other.CfgKeytest> allOtherKeytest() { return other_keytest_All.values(); }

    public java.util.Map<Integer, config.other.CfgLoot> other_loot_All;

    public config.other.CfgLoot getOtherLoot(int lootid) { return other_loot_All.get(lootid); }

    public java.util.Collection<config.other.CfgLoot> allOtherLoot() { return other_loot_All.values(); }

    public java.util.Map<config.other.CfgLootitem.LootidItemidKey, config.other.CfgLootitem> other_lootitem_All;

    public config.other.CfgLootitem getOtherLootitem(int lootid, int itemid) { return other_lootitem_All.get(new config.other.CfgLootitem.LootidItemidKey(lootid, itemid)); }

    public java.util.Collection<config.other.CfgLootitem> allOtherLootitem() { return other_lootitem_All.values(); }

    public java.util.Map<Integer, config.other.CfgMonster> other_monster_All;

    public config.other.CfgMonster getOtherMonster(int id) { return other_monster_All.get(id); }

    public java.util.Collection<config.other.CfgMonster> allOtherMonster() { return other_monster_All.values(); }

    public java.util.Map<Integer, config.other.CfgSignin> other_signin_All;

    public config.other.CfgSignin getOtherSignin(int id) { return other_signin_All.get(id); }

    public java.util.Collection<config.other.CfgSignin> allOtherSignin() { return other_signin_All.values(); }

    public java.util.Map<Integer, config.task.CfgTask> task_task_All;

    public config.task.CfgTask getTaskTask(int taskid) { return task_task_All.get(taskid); }

    public java.util.Collection<config.task.CfgTask> allTaskTask() { return task_task_All.values(); }

    public java.util.Map<Integer, config.task.CfgTask2> task_task2_All;

    public config.task.CfgTask2 getTaskTask2(int taskid) { return task_task2_All.get(taskid); }

    public java.util.Collection<config.task.CfgTask2> allTaskTask2() { return task_task2_All.values(); }

    public java.util.Map<Integer, config.task.CfgTaskextraexp> task_taskextraexp_All;

    public config.task.CfgTaskextraexp getTaskTaskextraexp(int taskid) { return task_taskextraexp_All.get(taskid); }

    public java.util.Collection<config.task.CfgTaskextraexp> allTaskTaskextraexp() { return task_taskextraexp_All.values(); }


    public void copyFrom(config.ConfigMgr src) {
        this.ai_ai_All = src.ai_ai_All;
        this.ai_ai_action_All = src.ai_ai_action_All;
        this.ai_ai_condition_All = src.ai_ai_condition_All;
        this.equip_equipconfig_All = src.equip_equipconfig_All;
        this.equip_jewelry_All = src.equip_jewelry_All;
        this.equip_jewelryrandom_All = src.equip_jewelryrandom_All;
        this.equip_jewelrysuit_All = src.equip_jewelrysuit_All;
        this.equip_rank_All = src.equip_rank_All;
        this.other_ArgCaptureMode_All = src.other_ArgCaptureMode_All;
        this.other_ArgCaptureMode_IdMap = src.other_ArgCaptureMode_IdMap;
        this.other_drop_All = src.other_drop_All;
        this.other_keytest_All = src.other_keytest_All;
        this.other_keytest_Id1Id3Map = src.other_keytest_Id1Id3Map;
        this.other_keytest_Id2Map = src.other_keytest_Id2Map;
        this.other_keytest_Id2Id3Map = src.other_keytest_Id2Id3Map;
        this.other_loot_All = src.other_loot_All;
        this.other_lootitem_All = src.other_lootitem_All;
        this.other_monster_All = src.other_monster_All;
        this.other_signin_All = src.other_signin_All;
        this.task_task_All = src.task_task_All;
        this.task_task2_All = src.task_task2_All;
        this.task_taskextraexp_All = src.task_taskextraexp_All;
    }
}
