using System;
using System.Collections.Generic;
namespace Config
{
    public sealed class ConfigMgr
    {
        private static ConfigMgr? _default;
        private bool _ready;
        internal IReadOnlyList<Ai.DAi> _ai_dai_Rows = null!;
        internal IReadOnlyDictionary<int, Ai.DAi> _ai_dai_All = null!;
        internal readonly Dictionary<int, List<string>> _ai_dai_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Ai.DAiAction> _ai_dai_uaction_Rows = null!;
        internal IReadOnlyDictionary<int, Ai.DAiAction> _ai_dai_uaction_All = null!;
        internal readonly Dictionary<int, List<string>> _ai_dai_uaction_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Ai.DAiCondition> _ai_dai_ucondition_Rows = null!;
        internal IReadOnlyDictionary<int, Ai.DAiCondition> _ai_dai_ucondition_All = null!;
        internal readonly Dictionary<int, List<string>> _ai_dai_ucondition_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Equip.DAbilityInfo> _equip_dability_Rows = null!;
        internal IReadOnlyDictionary<int, Equip.DAbilityInfo> _equip_dability_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_dability_Origins = new Dictionary<int, List<string>>();
        internal readonly Equip.DAbilityInfo?[] _equip_dability_Symbols = new Equip.DAbilityInfo?[7];
        internal IReadOnlyList<Equip.DEquipConfig> _equip_dequip_uconfig_Rows = null!;
        internal IReadOnlyDictionary<string, Equip.DEquipConfig> _equip_dequip_uconfig_All = null!;
        internal readonly Dictionary<string, List<string>> _equip_dequip_uconfig_Origins = new Dictionary<string, List<string>>();
        internal Equip.DEquipConfig? _equip_dequip_uconfig_Symbol_Instance;
        internal Equip.DEquipConfig? _equip_dequip_uconfig_Symbol_Instance2;
        internal IReadOnlyList<Equip.DJewelry> _equip_djewelry_Rows = null!;
        internal IReadOnlyDictionary<int, Equip.DJewelry> _equip_djewelry_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_djewelry_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Equip.DJewelryRandom> _equip_djewelry_urandom_Rows = null!;
        internal IReadOnlyDictionary<DLevelRank, Equip.DJewelryRandom> _equip_djewelry_urandom_All = null!;
        internal readonly Dictionary<DLevelRank, List<string>> _equip_djewelry_urandom_Origins = new Dictionary<DLevelRank, List<string>>();
        internal IReadOnlyList<Equip.DJewelrySuit> _equip_djewelry_usuit_Rows = null!;
        internal IReadOnlyDictionary<int, Equip.DJewelrySuit> _equip_djewelry_usuit_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_djewelry_usuit_Origins = new Dictionary<int, List<string>>();
        internal Equip.DJewelrySuit? _equip_djewelry_usuit_Symbol_SpecialSuit;
        internal IReadOnlyList<Equip.DJewelryTypeInfo> _equip_djewelry_utype_Rows = null!;
        internal IReadOnlyDictionary<string, Equip.DJewelryTypeInfo> _equip_djewelry_utype_All = null!;
        internal readonly Dictionary<string, List<string>> _equip_djewelry_utype_Origins = new Dictionary<string, List<string>>();
        internal readonly Equip.DJewelryTypeInfo?[] _equip_djewelry_utype_Symbols = new Equip.DJewelryTypeInfo?[4];
        internal IReadOnlyList<Equip.DRankInfo> _equip_drank_Rows = null!;
        internal Equip.DRankInfo[] _equip_drank_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_drank_Origins = new Dictionary<int, List<string>>();
        internal readonly Equip.DRankInfo?[] _equip_drank_Symbols = new Equip.DRankInfo?[6];
        internal IReadOnlyList<Other.DArgCaptureModeInfo> _other_dArgCaptureMode_Rows = null!;
        internal IReadOnlyDictionary<string, Other.DArgCaptureModeInfo> _other_dArgCaptureMode_All = null!;
        internal IReadOnlyDictionary<int, Other.DArgCaptureModeInfo> _other_dArgCaptureMode_IdMap = null!;
        internal readonly Dictionary<string, List<string>> _other_dArgCaptureMode_Origins = new Dictionary<string, List<string>>();
        internal readonly Other.DArgCaptureModeInfo?[] _other_dArgCaptureMode_Symbols = new Other.DArgCaptureModeInfo?[2];
        internal IReadOnlyList<Other.DDrop> _other_ddrop_Rows = null!;
        internal IReadOnlyDictionary<int, Other.DDrop> _other_ddrop_All = null!;
        internal readonly Dictionary<int, List<string>> _other_ddrop_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Other.DKeyTest> _other_dkey_utest_Rows = null!;
        internal IReadOnlyDictionary<Other.DKeyTest.Id1Id2Key, Other.DKeyTest> _other_dkey_utest_All = null!;
        internal IReadOnlyDictionary<Other.DKeyTest.Id1Id3Key, Other.DKeyTest> _other_dkey_utest_Id1Id3Map = null!;
        internal IReadOnlyDictionary<long, Other.DKeyTest> _other_dkey_utest_Id2Map = null!;
        internal IReadOnlyDictionary<Other.DKeyTest.Id2Id3Key, Other.DKeyTest> _other_dkey_utest_Id2Id3Map = null!;
        internal readonly Dictionary<Other.DKeyTest.Id1Id2Key, List<string>> _other_dkey_utest_Origins = new Dictionary<Other.DKeyTest.Id1Id2Key, List<string>>();
        internal IReadOnlyList<Other.DLoot> _other_dloot_Rows = null!;
        internal IReadOnlyDictionary<int, Other.DLoot> _other_dloot_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dloot_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Other.DLootItem> _other_dloot_uitem_Rows = null!;
        internal IReadOnlyDictionary<Other.DLootItem.LootidItemidKey, Other.DLootItem> _other_dloot_uitem_All = null!;
        internal readonly Dictionary<Other.DLootItem.LootidItemidKey, List<string>> _other_dloot_uitem_Origins = new Dictionary<Other.DLootItem.LootidItemidKey, List<string>>();
        internal IReadOnlyList<Other.DMonster> _other_dmonster_Rows = null!;
        internal IReadOnlyDictionary<int, Other.DMonster> _other_dmonster_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dmonster_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Other.DSignIn> _other_dsign_uin_Rows = null!;
        internal IReadOnlyDictionary<int, Other.DSignIn> _other_dsign_uin_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dsign_uin_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Task.DCompleteConditionTypeInfo> _task_dcomplete_ucondition_utype_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DCompleteConditionTypeInfo> _task_dcomplete_ucondition_utype_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dcomplete_ucondition_utype_Origins = new Dictionary<int, List<string>>();
        internal readonly Task.DCompleteConditionTypeInfo?[] _task_dcomplete_ucondition_utype_Symbols = new Task.DCompleteConditionTypeInfo?[7];
        internal IReadOnlyList<Task.DTask> _task_dtask_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DTask> _task_dtask_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Task.DTask2> _task_dtask2_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DTask2> _task_dtask2_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask2_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Task.DTaskExtraExp> _task_dtask_uextra_uexp_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DTaskExtraExp> _task_dtask_uextra_uexp_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask_uextra_uexp_Origins = new Dictionary<int, List<string>>();
        public IReadOnlyList<string> Explain<K>(string table, K key)
            => table switch {
                "ai.ai" => Explain(_ai_dai_Origins, key),
                "ai.ai_action" => Explain(_ai_dai_uaction_Origins, key),
                "ai.ai_condition" => Explain(_ai_dai_ucondition_Origins, key),
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
