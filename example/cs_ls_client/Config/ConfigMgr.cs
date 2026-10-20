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
        internal IReadOnlyList<Equip.DEquipconfig> _equip_dequipconfig_Rows = null!;
        internal IReadOnlyDictionary<string, Equip.DEquipconfig> _equip_dequipconfig_All = null!;
        internal readonly Dictionary<string, List<string>> _equip_dequipconfig_Origins = new Dictionary<string, List<string>>();
        internal Equip.DEquipconfig? _equip_dequipconfig_Symbol_Instance;
        internal Equip.DEquipconfig? _equip_dequipconfig_Symbol_Instance2;
        internal IReadOnlyList<Equip.DJewelry> _equip_djewelry_Rows = null!;
        internal IReadOnlyDictionary<int, Equip.DJewelry> _equip_djewelry_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_djewelry_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Equip.DJewelryrandom> _equip_djewelryrandom_Rows = null!;
        internal IReadOnlyDictionary<DLevelRank, Equip.DJewelryrandom> _equip_djewelryrandom_All = null!;
        internal readonly Dictionary<DLevelRank, List<string>> _equip_djewelryrandom_Origins = new Dictionary<DLevelRank, List<string>>();
        internal IReadOnlyList<Equip.DJewelrysuit> _equip_djewelrysuit_Rows = null!;
        internal IReadOnlyDictionary<int, Equip.DJewelrysuit> _equip_djewelrysuit_All = null!;
        internal readonly Dictionary<int, List<string>> _equip_djewelrysuit_Origins = new Dictionary<int, List<string>>();
        internal Equip.DJewelrysuit? _equip_djewelrysuit_Symbol_SpecialSuit;
        internal IReadOnlyList<Equip.DJewelrytypeInfo> _equip_djewelrytype_Rows = null!;
        internal IReadOnlyDictionary<string, Equip.DJewelrytypeInfo> _equip_djewelrytype_All = null!;
        internal readonly Dictionary<string, List<string>> _equip_djewelrytype_Origins = new Dictionary<string, List<string>>();
        internal readonly Equip.DJewelrytypeInfo?[] _equip_djewelrytype_Symbols = new Equip.DJewelrytypeInfo?[4];
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
        internal IReadOnlyList<Other.DKeytest> _other_dkeytest_Rows = null!;
        internal IReadOnlyDictionary<Other.DKeytest.Id1Id2Key, Other.DKeytest> _other_dkeytest_All = null!;
        internal IReadOnlyDictionary<Other.DKeytest.Id1Id3Key, Other.DKeytest> _other_dkeytest_Id1Id3Map = null!;
        internal IReadOnlyDictionary<long, Other.DKeytest> _other_dkeytest_Id2Map = null!;
        internal IReadOnlyDictionary<Other.DKeytest.Id2Id3Key, Other.DKeytest> _other_dkeytest_Id2Id3Map = null!;
        internal readonly Dictionary<Other.DKeytest.Id1Id2Key, List<string>> _other_dkeytest_Origins = new Dictionary<Other.DKeytest.Id1Id2Key, List<string>>();
        internal IReadOnlyList<Other.DLoot> _other_dloot_Rows = null!;
        internal IReadOnlyDictionary<int, Other.DLoot> _other_dloot_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dloot_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Other.DLootitem> _other_dlootitem_Rows = null!;
        internal IReadOnlyDictionary<Other.DLootitem.LootidItemidKey, Other.DLootitem> _other_dlootitem_All = null!;
        internal readonly Dictionary<Other.DLootitem.LootidItemidKey, List<string>> _other_dlootitem_Origins = new Dictionary<Other.DLootitem.LootidItemidKey, List<string>>();
        internal IReadOnlyList<Other.DMonster> _other_dmonster_Rows = null!;
        internal IReadOnlyDictionary<int, Other.DMonster> _other_dmonster_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dmonster_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Other.DSignin> _other_dsignin_Rows = null!;
        internal IReadOnlyDictionary<int, Other.DSignin> _other_dsignin_All = null!;
        internal readonly Dictionary<int, List<string>> _other_dsignin_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Task.DCompleteconditiontypeInfo> _task_dcompleteconditiontype_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DCompleteconditiontypeInfo> _task_dcompleteconditiontype_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dcompleteconditiontype_Origins = new Dictionary<int, List<string>>();
        internal readonly Task.DCompleteconditiontypeInfo?[] _task_dcompleteconditiontype_Symbols = new Task.DCompleteconditiontypeInfo?[7];
        internal IReadOnlyList<Task.DTask> _task_dtask_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DTask> _task_dtask_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Task.DTask2> _task_dtask2_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DTask2> _task_dtask2_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtask2_Origins = new Dictionary<int, List<string>>();
        internal IReadOnlyList<Task.DTaskextraexp> _task_dtaskextraexp_Rows = null!;
        internal IReadOnlyDictionary<int, Task.DTaskextraexp> _task_dtaskextraexp_All = null!;
        internal readonly Dictionary<int, List<string>> _task_dtaskextraexp_Origins = new Dictionary<int, List<string>>();
        public IReadOnlyList<string> Explain<K>(string table, K key)
            => table switch {
                "ai.ai" => Explain(_ai_dai_Origins, key),
                "ai.ai_action" => Explain(_ai_dai_uaction_Origins, key),
                "ai.ai_condition" => Explain(_ai_dai_ucondition_Origins, key),
                "equip.ability" => Explain(_equip_dability_Origins, key),
                "equip.equipconfig" => Explain(_equip_dequipconfig_Origins, key),
                "equip.jewelry" => Explain(_equip_djewelry_Origins, key),
                "equip.jewelryrandom" => Explain(_equip_djewelryrandom_Origins, key),
                "equip.jewelrysuit" => Explain(_equip_djewelrysuit_Origins, key),
                "equip.jewelrytype" => Explain(_equip_djewelrytype_Origins, key),
                "equip.rank" => Explain(_equip_drank_Origins, key),
                "other.ArgCaptureMode" => Explain(_other_dArgCaptureMode_Origins, key),
                "other.drop" => Explain(_other_ddrop_Origins, key),
                "other.keytest" => Explain(_other_dkeytest_Origins, key),
                "other.loot" => Explain(_other_dloot_Origins, key),
                "other.lootitem" => Explain(_other_dlootitem_Origins, key),
                "other.monster" => Explain(_other_dmonster_Origins, key),
                "other.signin" => Explain(_other_dsignin_Origins, key),
                "task.completeconditiontype" => Explain(_task_dcompleteconditiontype_Origins, key),
                "task.task" => Explain(_task_dtask_Origins, key),
                "task.task2" => Explain(_task_dtask2_Origins, key),
                "task.taskextraexp" => Explain(_task_dtaskextraexp_Origins, key),
                _ => Array.Empty<string>()
            };

        private static IReadOnlyList<string> Explain<K, Q>(Dictionary<K, List<string>> origins, Q key) where K : notnull
            => key is K typedKey && origins.TryGetValue(typedKey, out var chain) ? chain.AsReadOnly() : Array.Empty<string>();

        public static ConfigMgr GetMgr() => System.Threading.Volatile.Read(ref _default)
            ?? throw new InvalidOperationException("Default configuration has not been set");

        public static void SetMgr(ConfigMgr? config)
        {
            if (config == null || !config._ready) throw new InvalidOperationException("Configuration is not ready");
            System.Threading.Volatile.Write(ref _default, config);
        }

        internal void MarkReady() { _ready = true; }
    }
}
