package config;

public class ConfigMgr {
    private static volatile ConfigMgr mgr;

    public static ConfigMgr getMgr(){
        return mgr;
    }

    public static void setMgr(ConfigMgr newMgr){
        mgr = newMgr;
    }

    public final java.util.Map<Integer, config.ai.Ai> ai_ai_All = new java.util.LinkedHashMap<>();

    public config.ai.Ai getAiAi(int iD) { return ai_ai_All.get(iD); }

    public java.util.Collection<config.ai.Ai> allAiAi() { return ai_ai_All.values(); }

    public final java.util.Map<Integer, config.ai.Ai_action> ai_ai_action_All = new java.util.LinkedHashMap<>();

    public config.ai.Ai_action getAiAi_action(int iD) { return ai_ai_action_All.get(iD); }

    public java.util.Collection<config.ai.Ai_action> allAiAi_action() { return ai_ai_action_All.values(); }

    public final java.util.Map<Integer, config.ai.Ai_condition> ai_ai_condition_All = new java.util.LinkedHashMap<>();

    public config.ai.Ai_condition getAiAi_condition(int iD) { return ai_ai_condition_All.get(iD); }

    public java.util.Collection<config.ai.Ai_condition> allAiAi_condition() { return ai_ai_condition_All.values(); }

    public final java.util.Map<String, config.equip.Equipconfig> equip_equipconfig_All = new java.util.LinkedHashMap<>();

    public config.equip.Equipconfig getEquipEquipconfig(String entry) { return equip_equipconfig_All.get(entry); }

    public java.util.Collection<config.equip.Equipconfig> allEquipEquipconfig() { return equip_equipconfig_All.values(); }

    public final java.util.Map<Integer, config.equip.Jewelry> equip_jewelry_All = new java.util.LinkedHashMap<>();

    public config.equip.Jewelry getEquipJewelry(int iD) { return equip_jewelry_All.get(iD); }

    public java.util.Collection<config.equip.Jewelry> allEquipJewelry() { return equip_jewelry_All.values(); }

    public final java.util.Map<config.LevelRank, config.equip.Jewelryrandom> equip_jewelryrandom_All = new java.util.LinkedHashMap<>();

    public config.equip.Jewelryrandom getEquipJewelryrandom(config.LevelRank lvlRank) { return equip_jewelryrandom_All.get(lvlRank); }

    public java.util.Collection<config.equip.Jewelryrandom> allEquipJewelryrandom() { return equip_jewelryrandom_All.values(); }

    public final java.util.Map<Integer, config.equip.Jewelrysuit> equip_jewelrysuit_All = new java.util.LinkedHashMap<>();

    public config.equip.Jewelrysuit getEquipJewelrysuit(int suitID) { return equip_jewelrysuit_All.get(suitID); }

    public java.util.Collection<config.equip.Jewelrysuit> allEquipJewelrysuit() { return equip_jewelrysuit_All.values(); }

    public final java.util.Map<Integer, config.equip.Rank_Detail> equip_rank_All = new java.util.LinkedHashMap<>();

    public config.equip.Rank_Detail getEquipRank(int rankID) { return equip_rank_All.get(rankID); }

    public java.util.Collection<config.equip.Rank_Detail> allEquipRank() { return equip_rank_All.values(); }

    public final java.util.Map<Integer, config.other.Drop> other_drop_All = new java.util.LinkedHashMap<>();

    public config.other.Drop getOtherDrop(int dropid) { return other_drop_All.get(dropid); }

    public java.util.Collection<config.other.Drop> allOtherDrop() { return other_drop_All.values(); }

    public final java.util.Map<Integer, config.other.Loot> other_loot_All = new java.util.LinkedHashMap<>();

    public config.other.Loot getOtherLoot(int lootid) { return other_loot_All.get(lootid); }

    public java.util.Collection<config.other.Loot> allOtherLoot() { return other_loot_All.values(); }

    public final java.util.Map<config.other.Lootitem.LootidItemidKey, config.other.Lootitem> other_lootitem_All = new java.util.LinkedHashMap<>();

    public config.other.Lootitem getOtherLootitem(int lootid, int itemid) { return other_lootitem_All.get(new config.other.Lootitem.LootidItemidKey(lootid, itemid)); }

    public java.util.Collection<config.other.Lootitem> allOtherLootitem() { return other_lootitem_All.values(); }

    public final java.util.Map<Integer, config.other.Monster> other_monster_All = new java.util.LinkedHashMap<>();

    public config.other.Monster getOtherMonster(int id) { return other_monster_All.get(id); }

    public java.util.Collection<config.other.Monster> allOtherMonster() { return other_monster_All.values(); }

    public final java.util.Map<Integer, config.other.Signin> other_signin_All = new java.util.LinkedHashMap<>();

    public config.other.Signin getOtherSignin(int id) { return other_signin_All.get(id); }

    public final java.util.Map<config.other.Signin.IdViplevelKey, config.other.Signin> other_signin_IdViplevelMap = new java.util.LinkedHashMap<>();

    public config.other.Signin getOtherSigninByIdViplevel(int id, int viplevel) { return other_signin_IdViplevelMap.get(new config.other.Signin.IdViplevelKey(id, viplevel)); }

    public java.util.Collection<config.other.Signin> allOtherSignin() { return other_signin_All.values(); }

    public final java.util.Map<Integer, config.task.Task> task_task_All = new java.util.LinkedHashMap<>();

    public config.task.Task getTaskTask(int taskid) { return task_task_All.get(taskid); }

    public java.util.Collection<config.task.Task> allTaskTask() { return task_task_All.values(); }

    public final java.util.Map<Integer, config.task.Task2> task_task2_All = new java.util.LinkedHashMap<>();

    public config.task.Task2 getTaskTask2(int taskid) { return task_task2_All.get(taskid); }

    public java.util.Collection<config.task.Task2> allTaskTask2() { return task_task2_All.values(); }

    public final java.util.Map<Integer, config.task.Taskextraexp> task_taskextraexp_All = new java.util.LinkedHashMap<>();

    public config.task.Taskextraexp getTaskTaskextraexp(int taskid) { return task_taskextraexp_All.get(taskid); }

    public java.util.Collection<config.task.Taskextraexp> allTaskTaskextraexp() { return task_taskextraexp_All.values(); }

}
