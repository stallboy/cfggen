namespace Config.Other
{
    public partial class DDropItem
    {
        internal static DDropItem _create(ConfigReader reader)
        {
            var valueChance = reader.ReadInt32();
            int Count_valueItemids = reader.ReadCount(0);
            IReadOnlyList<int> valueItemids;
            if (Count_valueItemids == 0) {
                valueItemids = Array.Empty<int>();
            }
            else {
                var values_valueItemids = new List<int>(Count_valueItemids);
                for (int i = 0; i < Count_valueItemids; i++) {
                    values_valueItemids.Add(reader.ReadInt32());
                }
                valueItemids = values_valueItemids.AsReadOnly();
            }
            var valueCountmin = reader.ReadInt32();
            var valueCountmax = reader.ReadInt32();
            return new DDropItem {
                Chance = valueChance,
                Itemids = valueItemids,
                Countmin = valueCountmin,
                Countmax = valueCountmax,
            };
        }

        public override int GetHashCode()
        {
            return Chance.GetHashCode() + Itemids.GetHashCode() + Countmin.GetHashCode() + Countmax.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DDropItem o && Chance.Equals(o.Chance) && Itemids.Equals(o.Itemids) && Countmin.Equals(o.Countmin) && Countmax.Equals(o.Countmax);
        }

        public override string ToString()
        {
            return "(" + Chance + "," + StringUtil.ToString(Itemids) + "," + Countmin + "," + Countmax + ")";
        }


    }

    public partial class DArgCaptureModeInfo
    {
        private static readonly int[] _symbolIds = { 1, 2 };
        internal static string PrimaryKey(DArgCaptureModeInfo self) => self.Name;
        internal static void InitializeAll(List<DArgCaptureModeInfo> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<string, DArgCaptureModeInfo>(count);
            var s_idMap = new Dictionary<int, DArgCaptureModeInfo>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                {
                    var uniqueKey = self.Id;
                    if (!s_idMap.TryAdd(uniqueKey, self)) {
                        errors.DuplicateUniqueKey("other.ArgCaptureMode", "id", key, uniqueKey, config.Explain("other.ArgCaptureMode", key));
                    }
                }
                string name = self.Name;
                int symbol = (int)self.EEnum;
                if (symbol < 0) {
                    errors.UnknownSymbol("other.ArgCaptureMode", name, key, config.Explain("other.ArgCaptureMode", key));
                }
                else {
                    if (self.Id != _symbolIds[symbol]) {
                        errors.ChangedSymbolId("other.ArgCaptureMode", name, _symbolIds[symbol], self.Id, config.Explain("other.ArgCaptureMode", key));
                    }
                }
            }
            config._other_dArgCaptureMode_All = new System.Collections.ObjectModel.ReadOnlyDictionary<string, DArgCaptureModeInfo>(s_all);
            config._other_dArgCaptureMode_IdMap = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DArgCaptureModeInfo>(s_idMap);
            config._other_dArgCaptureMode_Symbols[(int)DArgCaptureMode.Snapshot] = Get(config, "Snapshot");
            if (config._other_dArgCaptureMode_Symbols[(int)DArgCaptureMode.Snapshot] == null) {
                errors.MissingSymbol("other.ArgCaptureMode", "Snapshot", "Snapshot");
            }
            config._other_dArgCaptureMode_Symbols[(int)DArgCaptureMode.Dynamic] = Get(config, "Dynamic");
            if (config._other_dArgCaptureMode_Symbols[(int)DArgCaptureMode.Dynamic] == null) {
                errors.MissingSymbol("other.ArgCaptureMode", "Dynamic", "Dynamic");
            }
            config._other_dArgCaptureMode_Rows = list.AsReadOnly();
        }

        internal static DArgCaptureModeInfo _create(ConfigReader reader)
        {
            var valueName = reader.ReadStringInPool();
            var valueId = reader.ReadInt32();
            var valueComment = reader.ReadStringInPool();
            return new DArgCaptureModeInfo {
                Name = valueName,
                Id = valueId,
                Comment = valueComment,
                EEnum = valueName switch {
                    "Snapshot" => DArgCaptureMode.Snapshot,
                    "Dynamic" => DArgCaptureMode.Dynamic,
                    _ => (DArgCaptureMode)(-1)
                }
            };
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DArgCaptureModeInfo o && Name.Equals(o.Name);
        }

        public override string ToString()
        {
            return "(" + Name + "," + Id + "," + Comment + ")";
        }


    }

    public partial class DDrop
    {
        internal static int PrimaryKey(DDrop self) => self.Dropid;
        internal static void InitializeAll(List<DDrop> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DDrop>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._other_ddrop_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DDrop>(s_all);
            config._other_ddrop_Rows = list.AsReadOnly();
        }

        internal static DDrop _create(ConfigReader reader)
        {
            var valueDropid = reader.ReadInt32();
            var valueName = reader.ReadTextInPool();
            int Count_valueItems = reader.ReadCount(0);
            IReadOnlyList<Other.DDropItem> valueItems;
            if (Count_valueItems == 0) {
                valueItems = Array.Empty<Other.DDropItem>();
            }
            else {
                var values_valueItems = new List<Other.DDropItem>(Count_valueItems);
                for (int i = 0; i < Count_valueItems; i++) {
                    values_valueItems.Add(Other.DDropItem._create(reader));
                }
                valueItems = values_valueItems.AsReadOnly();
            }
            int Count_valueTestmap = reader.ReadCount(0);
            var valueTestmap = Count_valueTestmap == 0 ? ConfigMap<int, int>.Empty : new ConfigMap<int, int>(Count_valueTestmap);
            for (int i = 0; i < Count_valueTestmap; i++) {
                var key = reader.ReadInt32();
                var value = reader.ReadInt32();
                if (!valueTestmap.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("other.drop.testmap", key);
                }
            }
            return new DDrop {
                Dropid = valueDropid,
                Name = valueName,
                Items = valueItems,
                Testmap = valueTestmap,
            };
        }

        public override int GetHashCode()
        {
            return Dropid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DDrop o && Dropid.Equals(o.Dropid);
        }

        public override string ToString()
        {
            return "(" + Dropid + "," + Name + "," + StringUtil.ToString(Items) + "," + Testmap + ")";
        }


    }

    public partial class DKeyTest
    {
        internal static Id1Id2Key PrimaryKey(DKeyTest self) => new Id1Id2Key(self.Id1, self.Id2);
        internal static void InitializeAll(List<DKeyTest> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<Id1Id2Key, DKeyTest>(count);
            var s_id1Id3Map = new Dictionary<Id1Id3Key, DKeyTest>(count);
            var s_id2Map = new Dictionary<long, DKeyTest>(count);
            var s_id2Id3Map = new Dictionary<Id2Id3Key, DKeyTest>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                {
                    var uniqueKey = new Id1Id3Key(self.Id1, self.Id3);
                    if (!s_id1Id3Map.TryAdd(uniqueKey, self)) {
                        errors.DuplicateUniqueKey("other.key_test", "id1,id3", key, uniqueKey, config.Explain("other.key_test", key));
                    }
                }
                {
                    var uniqueKey = self.Id2;
                    if (!s_id2Map.TryAdd(uniqueKey, self)) {
                        errors.DuplicateUniqueKey("other.key_test", "id2", key, uniqueKey, config.Explain("other.key_test", key));
                    }
                }
                {
                    var uniqueKey = new Id2Id3Key(self.Id2, self.Id3);
                    if (!s_id2Id3Map.TryAdd(uniqueKey, self)) {
                        errors.DuplicateUniqueKey("other.key_test", "id2,id3", key, uniqueKey, config.Explain("other.key_test", key));
                    }
                }
            }
            config._other_dkey_utest_All = new System.Collections.ObjectModel.ReadOnlyDictionary<Id1Id2Key, DKeyTest>(s_all);
            config._other_dkey_utest_Id1Id3Map = new System.Collections.ObjectModel.ReadOnlyDictionary<Id1Id3Key, DKeyTest>(s_id1Id3Map);
            config._other_dkey_utest_Id2Map = new System.Collections.ObjectModel.ReadOnlyDictionary<long, DKeyTest>(s_id2Map);
            config._other_dkey_utest_Id2Id3Map = new System.Collections.ObjectModel.ReadOnlyDictionary<Id2Id3Key, DKeyTest>(s_id2Id3Map);
            config._other_dkey_utest_Rows = list.AsReadOnly();
        }

        internal static DKeyTest _create(ConfigReader reader)
        {
            var valueId1 = reader.ReadInt32();
            var valueId2 = reader.ReadInt64();
            var valueId3 = reader.ReadInt32();
            int Count_valueIds = reader.ReadCount(0);
            IReadOnlyList<int> valueIds;
            if (Count_valueIds == 0) {
                valueIds = Array.Empty<int>();
            }
            else {
                var values_valueIds = new List<int>(Count_valueIds);
                for (int i = 0; i < Count_valueIds; i++) {
                    values_valueIds.Add(reader.ReadInt32());
                }
                valueIds = values_valueIds.AsReadOnly();
            }
            var valueEnumTest = reader.ReadStringInPool();
            int Count_valueEnumList = reader.ReadCount(0);
            IReadOnlyList<string> valueEnumList;
            if (Count_valueEnumList == 0) {
                valueEnumList = Array.Empty<string>();
            }
            else {
                var values_valueEnumList = new List<string>(Count_valueEnumList);
                for (int i = 0; i < Count_valueEnumList; i++) {
                    values_valueEnumList.Add(reader.ReadStringInPool());
                }
                valueEnumList = values_valueEnumList.AsReadOnly();
            }
            return new DKeyTest {
                Id1 = valueId1,
                Id2 = valueId2,
                Id3 = valueId3,
                Ids = valueIds,
                EnumTest = valueEnumTest,
                EnumList = valueEnumList,
            };
        }

        public override int GetHashCode()
        {
            return Id1.GetHashCode() + Id2.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DKeyTest o && Id1.Equals(o.Id1) && Id2.Equals(o.Id2);
        }

        public override string ToString()
        {
            return "(" + Id1 + "," + Id2 + "," + Id3 + "," + StringUtil.ToString(Ids) + "," + EnumTest + "," + StringUtil.ToString(EnumList) + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            var resolvedRefIds = new List<Other.DSignIn>();
            foreach(var e in Ids) {
                var r = Other.DSignIn.Get(config, e);
                if (r == null) {
                    errors.MissingReference("other.key_test", "ids", "other.sign_in", e);
                }
                else {
                    resolvedRefIds.Add(r);
                }
            }
            RefIds = resolvedRefIds.Count == 0 ? Array.Empty<Other.DSignIn>() : resolvedRefIds.AsReadOnly();
            var rRefEnumTest = Other.DArgCaptureModeInfo.Get(config, EnumTest);
            if (rRefEnumTest == null) {
                errors.MissingReference("other.key_test", "enumTest", "other.ArgCaptureMode", this.EnumTest);
            }
            else {
                RefEnumTest = rRefEnumTest.EEnum;
            }
            var resolvedRefEnumList = new List<Other.DArgCaptureMode>();
            foreach(var e in EnumList) {
                var r = Other.DArgCaptureModeInfo.Get(config, e);
                if (r == null) {
                    errors.MissingReference("other.key_test", "enumList", "other.ArgCaptureMode", e);
                }
                else {
                    resolvedRefEnumList.Add(r.EEnum);
                }
            }
            RefEnumList = resolvedRefEnumList.Count == 0 ? Array.Empty<Other.DArgCaptureMode>() : resolvedRefEnumList.AsReadOnly();
        }

    }

    public partial class DLoot
    {
        internal static int PrimaryKey(DLoot self) => self.Lootid;
        internal static void InitializeAll(List<DLoot> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DLoot>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._other_dloot_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DLoot>(s_all);
            config._other_dloot_Rows = list.AsReadOnly();
        }

        internal static DLoot _create(ConfigReader reader)
        {
            var valueLootid = reader.ReadInt32();
            var valueEname = reader.ReadStringInPool();
            var valueName = reader.ReadTextInPool();
            int Count_valueChanceList = reader.ReadCount(0);
            IReadOnlyList<int> valueChanceList;
            if (Count_valueChanceList == 0) {
                valueChanceList = Array.Empty<int>();
            }
            else {
                var values_valueChanceList = new List<int>(Count_valueChanceList);
                for (int i = 0; i < Count_valueChanceList; i++) {
                    values_valueChanceList.Add(reader.ReadInt32());
                }
                valueChanceList = values_valueChanceList.AsReadOnly();
            }
            return new DLoot {
                Lootid = valueLootid,
                Ename = valueEname,
                Name = valueName,
                ChanceList = valueChanceList,
            };
        }

        public override int GetHashCode()
        {
            return Lootid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DLoot o && Lootid.Equals(o.Lootid);
        }

        public override string ToString()
        {
            return "(" + Lootid + "," + Ename + "," + Name + "," + StringUtil.ToString(ChanceList) + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            var resolvedListRefLootid = new List<Other.DLootItem>();
            foreach (var v in Other.DLootItem.All(config)) {
                if (v.Lootid.Equals(Lootid)) {
                    resolvedListRefLootid.Add(v);
                }
            }
            ListRefLootid = resolvedListRefLootid.Count == 0 ? Array.Empty<Other.DLootItem>() : resolvedListRefLootid.AsReadOnly();
            var resolvedListRefAnotherWay = new List<Other.DLootItem>();
            foreach (var v in Other.DLootItem.All(config)) {
                if (v.Lootid.Equals(Lootid)) {
                    resolvedListRefAnotherWay.Add(v);
                }
            }
            ListRefAnotherWay = resolvedListRefAnotherWay.Count == 0 ? Array.Empty<Other.DLootItem>() : resolvedListRefAnotherWay.AsReadOnly();
        }

    }

    public partial class DLootItem
    {
        internal static LootidItemidKey PrimaryKey(DLootItem self) => new LootidItemidKey(self.Lootid, self.Itemid);
        internal static void InitializeAll(List<DLootItem> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<LootidItemidKey, DLootItem>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._other_dloot_uitem_All = new System.Collections.ObjectModel.ReadOnlyDictionary<LootidItemidKey, DLootItem>(s_all);
            config._other_dloot_uitem_Rows = list.AsReadOnly();
        }

        internal static DLootItem _create(ConfigReader reader)
        {
            var valueLootid = reader.ReadInt32();
            var valueItemid = reader.ReadInt32();
            var valueChance = reader.ReadInt32();
            var valueCountmin = reader.ReadInt32();
            var valueCountmax = reader.ReadInt32();
            return new DLootItem {
                Lootid = valueLootid,
                Itemid = valueItemid,
                Chance = valueChance,
                Countmin = valueCountmin,
                Countmax = valueCountmax,
            };
        }

        public override int GetHashCode()
        {
            return Lootid.GetHashCode() + Itemid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DLootItem o && Lootid.Equals(o.Lootid) && Itemid.Equals(o.Itemid);
        }

        public override string ToString()
        {
            return "(" + Lootid + "," + Itemid + "," + Chance + "," + Countmin + "," + Countmax + ")";
        }


    }

    public partial class DMonster
    {
        internal static int PrimaryKey(DMonster self) => self.Id;
        internal static void InitializeAll(List<DMonster> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DMonster>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._other_dmonster_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DMonster>(s_all);
            config._other_dmonster_Rows = list.AsReadOnly();
        }

        internal static DMonster _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            int Count_valuePosList = reader.ReadCount(0);
            IReadOnlyList<DPosition> valuePosList;
            if (Count_valuePosList == 0) {
                valuePosList = Array.Empty<DPosition>();
            }
            else {
                var values_valuePosList = new List<DPosition>(Count_valuePosList);
                for (int i = 0; i < Count_valuePosList; i++) {
                    values_valuePosList.Add(DPosition._create(reader));
                }
                valuePosList = values_valuePosList.AsReadOnly();
            }
            var valueLootId = reader.ReadInt32();
            var valueLootItemId = reader.ReadInt32();
            int Count_valueEnumMap1 = reader.ReadCount(0);
            var valueEnumMap1 = Count_valueEnumMap1 == 0 ? ConfigMap<string, int>.Empty : new ConfigMap<string, int>(Count_valueEnumMap1);
            for (int i = 0; i < Count_valueEnumMap1; i++) {
                var key = reader.ReadStringInPool();
                var value = reader.ReadInt32();
                if (!valueEnumMap1.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("other.monster.enumMap1", key);
                }
            }
            int Count_valueEnumMap2 = reader.ReadCount(0);
            var valueEnumMap2 = Count_valueEnumMap2 == 0 ? ConfigMap<int, string>.Empty : new ConfigMap<int, string>(Count_valueEnumMap2);
            for (int i = 0; i < Count_valueEnumMap2; i++) {
                var key = reader.ReadInt32();
                var value = reader.ReadStringInPool();
                if (!valueEnumMap2.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("other.monster.enumMap2", key);
                }
            }
            return new DMonster {
                Id = valueId,
                PosList = valuePosList,
                LootId = valueLootId,
                LootItemId = valueLootItemId,
                EnumMap1 = valueEnumMap1,
                EnumMap2 = valueEnumMap2,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DMonster o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + StringUtil.ToString(PosList) + "," + LootId + "," + LootItemId + "," + EnumMap1 + "," + EnumMap2 + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            var rRefLoot = Other.DLootItem.Get(config, LootId, LootItemId);
            if (rRefLoot == null) {
                errors.MissingReference("other.monster", "Loot", "other.loot_item", (this.LootId, this.LootItemId));
            }
            else {
                RefLoot = rRefLoot;
            }
            var rRefAllLoot = Other.DLoot.Get(config, LootId);
            if (rRefAllLoot == null) {
                errors.MissingReference("other.monster", "AllLoot", "other.loot", this.LootId);
            }
            else {
                RefAllLoot = rRefAllLoot;
            }
            var resolvedRefEnumMap2 = new ConfigMap<int, Other.DArgCaptureMode>();
            foreach(var kv in EnumMap2) {
                var k = kv.Key;
                var v = Other.DArgCaptureModeInfo.Get(config, kv.Value);
                if (v == null) {
                    errors.MissingReference("other.monster", "enumMap2", "other.ArgCaptureMode", kv.Value);
                }
                else {
                    resolvedRefEnumMap2.TryAdd(k, v.EEnum);
                }
            }
            RefEnumMap2 = resolvedRefEnumMap2;
        }

    }

    public partial class DSignIn
    {
        internal static int PrimaryKey(DSignIn self) => self.Id;
        internal static void InitializeAll(List<DSignIn> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DSignIn>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._other_dsign_uin_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DSignIn>(s_all);
            config._other_dsign_uin_Rows = list.AsReadOnly();
        }

        internal static DSignIn _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            int Count_valueItem2countMap = reader.ReadCount(0);
            var valueItem2countMap = Count_valueItem2countMap == 0 ? ConfigMap<int, int>.Empty : new ConfigMap<int, int>(Count_valueItem2countMap);
            for (int i = 0; i < Count_valueItem2countMap; i++) {
                var key = reader.ReadInt32();
                var value = reader.ReadInt32();
                if (!valueItem2countMap.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("other.sign_in.item2countMap", key);
                }
            }
            int Count_valueVipitem2vipcountMap = reader.ReadCount(0);
            var valueVipitem2vipcountMap = Count_valueVipitem2vipcountMap == 0 ? ConfigMap<int, int>.Empty : new ConfigMap<int, int>(Count_valueVipitem2vipcountMap);
            for (int i = 0; i < Count_valueVipitem2vipcountMap; i++) {
                var key = reader.ReadInt32();
                var value = reader.ReadInt32();
                if (!valueVipitem2vipcountMap.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("other.sign_in.vipitem2vipcountMap", key);
                }
            }
            var valueViplevel = reader.ReadInt32();
            var valueIconFile = reader.ReadStringInPool();
            return new DSignIn {
                Id = valueId,
                Item2countMap = valueItem2countMap,
                Vipitem2vipcountMap = valueVipitem2vipcountMap,
                Viplevel = valueViplevel,
                IconFile = valueIconFile,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DSignIn o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Item2countMap + "," + Vipitem2vipcountMap + "," + Viplevel + "," + IconFile + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            var resolvedRefVipitem2vipcountMap = new ConfigMap<int, Other.DLoot>();
            foreach(var kv in Vipitem2vipcountMap) {
                var k = kv.Key;
                var v = Other.DLoot.Get(config, kv.Value);
                if (v == null) {
                    errors.MissingReference("other.sign_in", "vipitem2vipcountMap", "other.loot", kv.Value);
                }
                else {
                    resolvedRefVipitem2vipcountMap.TryAdd(k, v);
                }
            }
            RefVipitem2vipcountMap = resolvedRefVipitem2vipcountMap;
        }

    }

}

