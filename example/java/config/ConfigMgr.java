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

    public final java.util.Map<Integer, config.ai.Ai_action> ai_ai_action_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.ai.Ai_condition> ai_ai_condition_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<String, config.equip.Equipconfig> equip_equipconfig_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.equip.Jewelry> equip_jewelry_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<config.LevelRank, config.equip.Jewelryrandom> equip_jewelryrandom_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.equip.Jewelrysuit> equip_jewelrysuit_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.equip.Rank_Detail> equip_rank_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.other.Drop> other_drop_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.other.Loot> other_loot_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<config.other.Lootitem.LootidItemidKey, config.other.Lootitem> other_lootitem_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.other.Monster> other_monster_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.other.Signin> other_signin_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<config.other.Signin.IdViplevelKey, config.other.Signin> other_signin_IdViplevelMap = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.task.Task> task_task_All = new java.util.LinkedHashMap<>();

    public final java.util.Map<Integer, config.task.Taskextraexp> task_taskextraexp_All = new java.util.LinkedHashMap<>();

}
