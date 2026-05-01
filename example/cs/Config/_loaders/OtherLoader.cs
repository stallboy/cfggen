using System.Collections.Frozen;

namespace Config.Other
{
    public partial class DDropItem
    {
        internal static DDropItem _create(ConfigReader reader)
        {
            var chance = reader.ReadInt32();
            int Count_itemids = reader.ReadInt32();
            var itemids = new List<int>(Count_itemids);
            for (int i = 0; i < Count_itemids; i++)
                itemids.Add(reader.ReadInt32());
            var countmin = reader.ReadInt32();
            var countmax = reader.ReadInt32();
            return new DDropItem {
                Chance = chance,
                Itemids = itemids,
                Countmin = countmin,
                Countmax = countmax,
            };
        }

        public override int GetHashCode()
        {
            return Chance.GetHashCode() + Itemids.GetHashCode() + Countmin.GetHashCode() + Countmax.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DDropItem;
            return o != null && Chance.Equals(o.Chance) && Itemids.Equals(o.Itemids) && Countmin.Equals(o.Countmin) && Countmax.Equals(o.Countmax);
        }

        public override string ToString()
        {
            return "(" + Chance + "," + StringUtil.ToString(Itemids) + "," + Countmin + "," + Countmax + ")";
        }

    }

    public partial class DArgCaptureModeInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<string, DArgCaptureModeInfo>(count);
            var s_idMap = new Dictionary<int, DArgCaptureModeInfo>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Name, self);
                s_idMap.Add(self.Id, self);
                DArgCaptureModeExtensions._infos[(int)self.EEnum] = self;
            }
            _all = s_all.ToFrozenDictionary();
            _idMap = s_idMap.ToFrozenDictionary();
        }

        internal static DArgCaptureModeInfo _create(ConfigReader reader)
        {
            var name = reader.ReadStringInPool();
            var id = reader.ReadInt32();
            var comment = reader.ReadTextInPool();
            return new DArgCaptureModeInfo {
                Name = name,
                Id = id,
                Comment = comment,
                EEnum = Enum.Parse<DArgCaptureMode>(StringUtil.UpperFirstChar(name))
            };
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DArgCaptureModeInfo;
            return o != null && Name.Equals(o.Name);
        }

        public override string ToString()
        {
            return "(" + Name + "," + Id + "," + Comment + ")";
        }

    }

    public partial class DDrop
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DDrop>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Dropid, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static DDrop _create(ConfigReader reader)
        {
            var dropid = reader.ReadInt32();
            var name = reader.ReadTextInPool();
            int Count_items = reader.ReadInt32();
            var items = new List<Other.DDropItem>(Count_items);
            for (int i = 0; i < Count_items; i++)
                items.Add(Other.DDropItem._create(reader));
            int Count_testmap = reader.ReadInt32();
            var testmap = new OrderedDictionary<int, int>(Count_testmap);
            for (int i = 0; i < Count_testmap; i++)
                testmap.Add(reader.ReadInt32(), reader.ReadInt32());
            return new DDrop {
                Dropid = dropid,
                Name = name,
                Items = items,
                Testmap = testmap,
            };
        }

        public override int GetHashCode()
        {
            return Dropid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DDrop;
            return o != null && Dropid.Equals(o.Dropid);
        }

        public override string ToString()
        {
            return "(" + Dropid + "," + Name + "," + StringUtil.ToString(Items) + "," + Testmap + ")";
        }

    }

    public partial class DKeytest
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<Id1Id2Key, DKeytest>(count);
            var s_id1Id3Map = new Dictionary<Id1Id3Key, DKeytest>(count);
            var s_id2Map = new Dictionary<long, DKeytest>(count);
            var s_id2Id3Map = new Dictionary<Id2Id3Key, DKeytest>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(new Id1Id2Key(self.Id1, self.Id2), self);
                s_id1Id3Map.Add(new Id1Id3Key(self.Id1, self.Id3), self);
                s_id2Map.Add(self.Id2, self);
                s_id2Id3Map.Add(new Id2Id3Key(self.Id2, self.Id3), self);
            }
            _all = s_all.ToFrozenDictionary();
            _id1Id3Map = s_id1Id3Map.ToFrozenDictionary();
            _id2Map = s_id2Map.ToFrozenDictionary();
            _id2Id3Map = s_id2Id3Map.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DKeytest _create(ConfigReader reader)
        {
            var id1 = reader.ReadInt32();
            var id2 = reader.ReadInt64();
            var id3 = reader.ReadInt32();
            int Count_ids = reader.ReadInt32();
            var ids = new List<int>(Count_ids);
            for (int i = 0; i < Count_ids; i++)
                ids.Add(reader.ReadInt32());
            var enumTest = reader.ReadStringInPool();
            int Count_enumList = reader.ReadInt32();
            var enumList = new List<string>(Count_enumList);
            for (int i = 0; i < Count_enumList; i++)
                enumList.Add(reader.ReadStringInPool());
            return new DKeytest {
                Id1 = id1,
                Id2 = id2,
                Id3 = id3,
                Ids = ids,
                EnumTest = enumTest,
                EnumList = enumList,
            };
        }

        public override int GetHashCode()
        {
            return Id1.GetHashCode() + Id2.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DKeytest;
            return o != null && Id1.Equals(o.Id1) && Id2.Equals(o.Id2);
        }

        public override string ToString()
        {
            return "(" + Id1 + "," + Id2 + "," + Id3 + "," + StringUtil.ToString(Ids) + "," + EnumTest + "," + StringUtil.ToString(EnumList) + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            RefIds = [];
            foreach(var e in Ids)
            {
                var r = Other.DSignin.Get(e);
                if (r == null) reader.RefNotFound("other.keytest", "ids", e.ToString());
                else RefIds.Add(r);
            }
            var rRefEnumTest = Other.DArgCaptureModeInfo.Get(EnumTest);
            if (rRefEnumTest == null) reader.RefNotFound("other.keytest", "enumTest", EnumTest);
            else RefEnumTest = rRefEnumTest.EEnum;
            RefEnumList = [];
            foreach(var e in EnumList)
            {
                var r = Other.DArgCaptureModeInfo.Get(e);
                if (r == null) reader.RefNotFound("other.keytest", "enumList", e);
                else RefEnumList.Add(r.EEnum);
            }
        }
    }

    public partial class DLoot
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DLoot>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Lootid, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DLoot _create(ConfigReader reader)
        {
            var lootid = reader.ReadInt32();
            var ename = reader.ReadStringInPool();
            var name = reader.ReadTextInPool();
            int Count_chanceList = reader.ReadInt32();
            var chanceList = new List<int>(Count_chanceList);
            for (int i = 0; i < Count_chanceList; i++)
                chanceList.Add(reader.ReadInt32());
            return new DLoot {
                Lootid = lootid,
                Ename = ename,
                Name = name,
                ChanceList = chanceList,
            };
        }

        public override int GetHashCode()
        {
            return Lootid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DLoot;
            return o != null && Lootid.Equals(o.Lootid);
        }

        public override string ToString()
        {
            return "(" + Lootid + "," + Ename + "," + Name + "," + StringUtil.ToString(ChanceList) + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            ListRefLootid = [];
            foreach (var v in Other.DLootitem.All())
            {
                if (v.Lootid.Equals(Lootid))
                    ListRefLootid.Add(v);
            }
            ListRefAnotherWay = [];
            foreach (var v in Other.DLootitem.All())
            {
                if (v.Lootid.Equals(Lootid))
                    ListRefAnotherWay.Add(v);
            }
        }
    }

    public partial class DLootitem
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<LootidItemidKey, DLootitem>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(new LootidItemidKey(self.Lootid, self.Itemid), self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static DLootitem _create(ConfigReader reader)
        {
            var lootid = reader.ReadInt32();
            var itemid = reader.ReadInt32();
            var chance = reader.ReadInt32();
            var countmin = reader.ReadInt32();
            var countmax = reader.ReadInt32();
            return new DLootitem {
                Lootid = lootid,
                Itemid = itemid,
                Chance = chance,
                Countmin = countmin,
                Countmax = countmax,
            };
        }

        public override int GetHashCode()
        {
            return Lootid.GetHashCode() + Itemid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DLootitem;
            return o != null && Lootid.Equals(o.Lootid) && Itemid.Equals(o.Itemid);
        }

        public override string ToString()
        {
            return "(" + Lootid + "," + Itemid + "," + Chance + "," + Countmin + "," + Countmax + ")";
        }

    }

    public partial class DMonster
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DMonster>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Id, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DMonster _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            int Count_posList = reader.ReadInt32();
            var posList = new List<DPosition>(Count_posList);
            for (int i = 0; i < Count_posList; i++)
                posList.Add(DPosition._create(reader));
            var lootId = reader.ReadInt32();
            var lootItemId = reader.ReadInt32();
            int Count_enumMap1 = reader.ReadInt32();
            var enumMap1 = new OrderedDictionary<string, int>(Count_enumMap1);
            for (int i = 0; i < Count_enumMap1; i++)
                enumMap1.Add(reader.ReadStringInPool(), reader.ReadInt32());
            int Count_enumMap2 = reader.ReadInt32();
            var enumMap2 = new OrderedDictionary<int, string>(Count_enumMap2);
            for (int i = 0; i < Count_enumMap2; i++)
                enumMap2.Add(reader.ReadInt32(), reader.ReadStringInPool());
            return new DMonster {
                Id = id,
                PosList = posList,
                LootId = lootId,
                LootItemId = lootItemId,
                EnumMap1 = enumMap1,
                EnumMap2 = enumMap2,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DMonster;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + StringUtil.ToString(PosList) + "," + LootId + "," + LootItemId + "," + EnumMap1 + "," + EnumMap2 + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            var rRefLoot = Other.DLootitem.Get(LootId, LootItemId);
            if (rRefLoot == null) reader.RefNotFound("other.monster", "Loot", LootId.ToString());
            else RefLoot = rRefLoot;
            var rRefAllLoot = Other.DLoot.Get(LootId);
            if (rRefAllLoot == null) reader.RefNotFound("other.monster", "AllLoot", LootId.ToString());
            else RefAllLoot = rRefAllLoot;
            RefEnumMap2 = [];
            foreach(var kv in EnumMap2)
            {
                var k = kv.Key;
                var v = Other.DArgCaptureModeInfo.Get(kv.Value);
                if (v == null) reader.RefNotFound("other.monster", "enumMap2", kv.Value);
                else RefEnumMap2.Add(k, v.EEnum);
            }
        }
    }

    public partial class DSignin
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DSignin>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Id, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DSignin _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            int Count_item2countMap = reader.ReadInt32();
            var item2countMap = new OrderedDictionary<int, int>(Count_item2countMap);
            for (int i = 0; i < Count_item2countMap; i++)
                item2countMap.Add(reader.ReadInt32(), reader.ReadInt32());
            int Count_vipitem2vipcountMap = reader.ReadInt32();
            var vipitem2vipcountMap = new OrderedDictionary<int, int>(Count_vipitem2vipcountMap);
            for (int i = 0; i < Count_vipitem2vipcountMap; i++)
                vipitem2vipcountMap.Add(reader.ReadInt32(), reader.ReadInt32());
            var viplevel = reader.ReadInt32();
            var iconFile = reader.ReadStringInPool();
            return new DSignin {
                Id = id,
                Item2countMap = item2countMap,
                Vipitem2vipcountMap = vipitem2vipcountMap,
                Viplevel = viplevel,
                IconFile = iconFile,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DSignin;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Item2countMap + "," + Vipitem2vipcountMap + "," + Viplevel + "," + IconFile + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            RefVipitem2vipcountMap = [];
            foreach(var kv in Vipitem2vipcountMap)
            {
                var k = kv.Key;
                var v = Other.DLoot.Get(kv.Value);
                if (v == null) reader.RefNotFound("other.signin", "vipitem2vipcountMap", kv.Value.ToString());
                else RefVipitem2vipcountMap.Add(k, v);
            }
        }
    }

}

