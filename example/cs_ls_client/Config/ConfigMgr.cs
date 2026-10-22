using System;
using System.Collections.Generic;
namespace Config
{
    public sealed class ConfigMgr
    {
        private static ConfigMgr? _default;
        private bool _ready;
        internal IReadOnlyList<global::Config.Ai.DAi> _ai_dai_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Ai.DAi> _ai_dai_All = null!;
        internal readonly Dictionary<int, List<string>> _ai_dai_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Ai.DAiAction> _ai_dai_uaction_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Ai.DAiAction> _ai_dai_uaction_All = null!;
        internal readonly Dictionary<int, List<string>> _ai_dai_uaction_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Ai.DAiCondition> _ai_dai_ucondition_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Ai.DAiCondition> _ai_dai_ucondition_All = null!;
        internal readonly Dictionary<int, List<string>> _ai_dai_ucondition_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Combat.DAbility> _combat_dability_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Combat.DAbility> _combat_dability_All = null!;
        internal IReadOnlyDictionary<string, global::Config.Combat.DAbility> _combat_dability_CodeMap = null!;
        internal readonly Dictionary<int, List<string>> _combat_dability_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Combat.DBranchreward> _combat_dbranchreward_Rows = null!;
        internal IReadOnlyDictionary<Combat.DPhaseKey, global::Config.Combat.DBranchreward> _combat_dbranchreward_All = null!;
        internal readonly Dictionary<Combat.DPhaseKey, List<string>> _combat_dbranchreward_Origins = new Dictionary<Combat.DPhaseKey, List<string>>();
        internal IReadOnlyList<global::Config.Combat.DCast> _combat_dcast_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Combat.DCast> _combat_dcast_All = null!;
        internal readonly Dictionary<int, List<string>> _combat_dcast_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Combat.DEncounter> _combat_dencounter_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Combat.DEncounter> _combat_dencounter_All = null!;
        internal readonly Dictionary<int, List<string>> _combat_dencounter_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Combat.DPhasebook> _combat_dphasebook_Rows = null!;
        internal IReadOnlyDictionary<Combat.DPhasebook.SchoolTierAirborneKey, global::Config.Combat.DPhasebook> _combat_dphasebook_All = null!;
        internal IReadOnlyDictionary<Combat.DPhasebook.CodeEditionKey, global::Config.Combat.DPhasebook> _combat_dphasebook_CodeEditionMap = null!;
        internal readonly Dictionary<Combat.DPhasebook.SchoolTierAirborneKey, List<string>> _combat_dphasebook_Origins = new Dictionary<Combat.DPhasebook.SchoolTierAirborneKey, List<string>>();
        internal IReadOnlyList<global::Config.Combat.DTimeline> _combat_dtimeline_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Combat.DTimeline> _combat_dtimeline_All = null!;
        internal readonly Dictionary<int, List<string>> _combat_dtimeline_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Equip.DAbilityInfo> _equip_dability_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Equip.DAbilityInfo> _equip_dability_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_dability_Origins = new Dictionary<int, List<string>>();
        internal readonly global::Config.Equip.DAbilityInfo?[] _equip_dability_Symbols = new global::Config.Equip.DAbilityInfo?[7];
        internal IReadOnlyList<global::Config.Equip.DEquipConfig> _equip_dequip_uconfig_Rows = null!;
        internal IReadOnlyDictionary<string, global::Config.Equip.DEquipConfig> _equip_dequip_uconfig_All = null!;
        internal readonly Dictionary<string, List<string>> _equip_dequip_uconfig_Origins = new Dictionary<string, List<string>>();
        internal global::Config.Equip.DEquipConfig? _equip_dequip_uconfig_Symbol_Instance;
        internal global::Config.Equip.DEquipConfig? _equip_dequip_uconfig_Symbol_Instance2;
        internal IReadOnlyList<global::Config.Equip.DJewelry> _equip_djewelry_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Equip.DJewelry> _equip_djewelry_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_djewelry_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Equip.DJewelryRandom> _equip_djewelry_urandom_Rows = null!;
        internal IReadOnlyDictionary<DLevelRank, global::Config.Equip.DJewelryRandom> _equip_djewelry_urandom_All = null!;
        internal readonly Dictionary<DLevelRank, List<string>> _equip_djewelry_urandom_Origins = new Dictionary<DLevelRank, List<string>>();
        internal IReadOnlyList<global::Config.Equip.DJewelrySuit> _equip_djewelry_usuit_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Equip.DJewelrySuit> _equip_djewelry_usuit_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_djewelry_usuit_Origins = new Dictionary<int, List<string>>();
        internal global::Config.Equip.DJewelrySuit? _equip_djewelry_usuit_Symbol_SpecialSuit;
        internal IReadOnlyList<global::Config.Equip.DJewelryTypeInfo> _equip_djewelry_utype_Rows = null!;
        internal IReadOnlyDictionary<string, global::Config.Equip.DJewelryTypeInfo> _equip_djewelry_utype_All = null!;
        internal readonly Dictionary<string, List<string>> _equip_djewelry_utype_Origins = new Dictionary<string, List<string>>();
        internal readonly global::Config.Equip.DJewelryTypeInfo?[] _equip_djewelry_utype_Symbols = new global::Config.Equip.DJewelryTypeInfo?[4];
        internal IReadOnlyList<global::Config.Equip.DRankInfo> _equip_drank_Rows = null!;
        internal global::Config.Equip.DRankInfo[] _equip_drank_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_drank_Origins = new Dictionary<int, List<string>>();
        internal readonly global::Config.Equip.DRankInfo?[] _equip_drank_Symbols = new global::Config.Equip.DRankInfo?[6];
        internal IReadOnlyList<global::Config.Other.DArgCaptureModeInfo> _other_dArgCaptureMode_Rows = null!;
        internal IReadOnlyDictionary<string, global::Config.Other.DArgCaptureModeInfo> _other_dArgCaptureMode_All = null!;
        internal IReadOnlyDictionary<int, global::Config.Other.DArgCaptureModeInfo> _other_dArgCaptureMode_IdMap = null!;
        internal readonly Dictionary<string, List<string>> _other_dArgCaptureMode_Origins = new Dictionary<string, List<string>>();
        internal readonly global::Config.Other.DArgCaptureModeInfo?[] _other_dArgCaptureMode_Symbols = new global::Config.Other.DArgCaptureModeInfo?[2];
        internal IReadOnlyList<global::Config.Other.DDrop> _other_ddrop_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Other.DDrop> _other_ddrop_All = null!;
        internal readonly Dictionary<int, List<string>> _other_ddrop_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Other.DKeyTest> _other_dkey_utest_Rows = null!;
        internal IReadOnlyDictionary<Other.DKeyTest.Id1Id2Key, global::Config.Other.DKeyTest> _other_dkey_utest_All = null!;
        internal IReadOnlyDictionary<Other.DKeyTest.Id1Id3Key, global::Config.Other.DKeyTest> _other_dkey_utest_Id1Id3Map = null!;
        internal IReadOnlyDictionary<long, global::Config.Other.DKeyTest> _other_dkey_utest_Id2Map = null!;
        internal IReadOnlyDictionary<Other.DKeyTest.Id2Id3Key, global::Config.Other.DKeyTest> _other_dkey_utest_Id2Id3Map = null!;
        internal readonly Dictionary<Other.DKeyTest.Id1Id2Key, List<string>> _other_dkey_utest_Origins = new Dictionary<Other.DKeyTest.Id1Id2Key, List<string>>();
        internal IReadOnlyList<global::Config.Other.DLoot> _other_dloot_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Other.DLoot> _other_dloot_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dloot_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Other.DLootItem> _other_dloot_uitem_Rows = null!;
        internal IReadOnlyDictionary<Other.DLootItem.LootidItemidKey, global::Config.Other.DLootItem> _other_dloot_uitem_All = null!;
        internal readonly Dictionary<Other.DLootItem.LootidItemidKey, List<string>> _other_dloot_uitem_Origins = new Dictionary<Other.DLootItem.LootidItemidKey, List<string>>();
        internal IReadOnlyList<global::Config.Other.DMonster> _other_dmonster_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Other.DMonster> _other_dmonster_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dmonster_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Other.DSignIn> _other_dsign_uin_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Other.DSignIn> _other_dsign_uin_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dsign_uin_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Task.DCompleteConditionTypeInfo> _task_dcomplete_ucondition_utype_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Task.DCompleteConditionTypeInfo> _task_dcomplete_ucondition_utype_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dcomplete_ucondition_utype_Origins = new Dictionary<int, List<string>>();
        internal readonly global::Config.Task.DCompleteConditionTypeInfo?[] _task_dcomplete_ucondition_utype_Symbols = new global::Config.Task.DCompleteConditionTypeInfo?[7];
        internal IReadOnlyList<global::Config.Task.DTask> _task_dtask_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Task.DTask> _task_dtask_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Task.DTask2> _task_dtask2_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Task.DTask2> _task_dtask2_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask2_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<global::Config.Task.DTaskExtraExp> _task_dtask_uextra_uexp_Rows = null!;
        internal IReadOnlyDictionary<int, global::Config.Task.DTaskExtraExp> _task_dtask_uextra_uexp_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask_uextra_uexp_Origins = new Dictionary<int, List<string>>();
        public IReadOnlyList<string> Explain<K>(string table, K key)
            => table switch {
                "ai.ai" => Explain(_ai_dai_Origins, key),
                "ai.ai_action" => Explain(_ai_dai_uaction_Origins, key),
                "ai.ai_condition" => Explain(_ai_dai_ucondition_Origins, key),
                "combat.ability" => Explain(_combat_dability_Origins, key),
                "combat.branchreward" => Explain(_combat_dbranchreward_Origins, key),
                "combat.cast" => Explain(_combat_dcast_Origins, key),
                "combat.encounter" => Explain(_combat_dencounter_Origins, key),
                "combat.phasebook" => Explain(_combat_dphasebook_Origins, key),
                "combat.timeline" => Explain(_combat_dtimeline_Origins, key),
                "equip.ability" => Explain(_equip_dability_Origins, key),
                "equip.equip_config" => Explain(_equip_dequip_uconfig_Origins, key),
                "equip.jewelry" => Explain(_equip_djewelry_Origins, key),
                "equip.jewelry_random" => Explain(_equip_djewelry_urandom_Origins, key),
                "equip.jewelry_suit" => Explain(_equip_djewelry_usuit_Origins, key),
                "equip.jewelry_type" => Explain(_equip_djewelry_utype_Origins, key),
                "equip.rank" => Explain(_equip_drank_Origins, key),
                "other.ArgCaptureMode" => Explain(_other_dArgCaptureMode_Origins, key),
                "other.drop" => Explain(_other_ddrop_Origins, key),
                "other.key_test" => Explain(_other_dkey_utest_Origins, key),
                "other.loot" => Explain(_other_dloot_Origins, key),
                "other.loot_item" => Explain(_other_dloot_uitem_Origins, key),
                "other.monster" => Explain(_other_dmonster_Origins, key),
                "other.sign_in" => Explain(_other_dsign_uin_Origins, key),
                "task.complete_condition_type" => Explain(_task_dcomplete_ucondition_utype_Origins, key),
                "task.task" => Explain(_task_dtask_Origins, key),
                "task.task2" => Explain(_task_dtask2_Origins, key),
                "task.task_extra_exp" => Explain(_task_dtask_uextra_uexp_Origins, key),
                _ => Array.Empty<string>()
            };

        private static IReadOnlyList<string> Explain<K, Q>(Dictionary<K, List<string>> origins, Q key) where K : notnull
            => key is K typedKey && origins.TryGetValue(typedKey, out var chain) ? chain.AsReadOnly() : Array.Empty<string>();

        public static ConfigMgr GetMgr() => System.Threading.Volatile.Read(ref _default)
            ?? throw new InvalidOperationException("Default configuration has not been set");

        public static void SetMgr(ConfigMgr? config)
        {
            if (config == null || !config._ready) {
                throw new InvalidOperationException("Configuration is not ready");
            }
            System.Threading.Volatile.Write(ref _default, config);
        }

        internal void MarkReady() { _ready = true; }
    }
}
