namespace Config.Other
{
    public partial class DDropItem
    {
        internal static DDropItem _create(ConfigReader reader)
        {
            var chance = reader.ReadInt32();
            List<int> itemids = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
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
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Name, self);
                DArgCaptureModeExtensions._infos[(int)self.eEnum] = self;
            }

        }

        internal static DArgCaptureModeInfo _create(ConfigReader reader)
        {
            var name = reader.ReadStringInPool();
            var comment = reader.ReadTextInPool();
            return new DArgCaptureModeInfo {
                Name = name,
                Comment = comment,
                eEnum = Enum.Parse<DArgCaptureMode>(StringUtil.UpperFirstChar(name))
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
            return "(" + Name + "," + Comment + ")";
        }

    }

    public partial class DDrop
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Dropid, self);
            }

        }

        internal static DDrop _create(ConfigReader reader)
        {
            var dropid = reader.ReadInt32();
            var name = reader.ReadTextInPool();
            List<Other.DDropItem> items = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                items.Add(Other.DDropItem._create(reader));
            OrderedDictionary<int, int> testmap = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                testmap.Add(reader.ReadInt32(), reader.ReadInt32());
            }
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
            _all = [];
            _id1Id3Map = [];
            _id2Map = [];
            _id2Id3Map = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(new Id1Id2Key(self.Id1, self.Id2), self);
                _id1Id3Map.Add(new Id1Id3Key(self.Id1, self.Id3), self);
                _id2Map.Add(self.Id2, self);
                _id2Id3Map.Add(new Id2Id3Key(self.Id2, self.Id3), self);
            }

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
            List<int> ids = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                ids.Add(reader.ReadInt32());
            var enumTest = reader.ReadStringInPool();
            List<string> enumList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
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
            else RefEnumTest = rRefEnumTest.eEnum;
            RefEnumList = [];
            foreach(var e in EnumList)
            {
                var r = Other.DArgCaptureModeInfo.Get(e);
                if (r == null) reader.RefNotFound("other.keytest", "enumList", e);
                else RefEnumList.Add(r.eEnum);
            }
        }
    }

    public partial class DLoot
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Lootid, self);
            }

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
            List<int> chanceList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
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
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(new LootidItemidKey(self.Lootid, self.Itemid), self);
            }

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
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Id, self);
            }

        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DMonster _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            List<DPosition> posList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                posList.Add(DPosition._create(reader));
            var lootId = reader.ReadInt32();
            var lootItemId = reader.ReadInt32();
            OrderedDictionary<string, int> enumMap1 = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                enumMap1.Add(reader.ReadStringInPool(), reader.ReadInt32());
            }
            OrderedDictionary<int, string> enumMap2 = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                enumMap2.Add(reader.ReadInt32(), reader.ReadStringInPool());
            }
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
                else RefEnumMap2.Add(k, v.eEnum);
            }
        }
    }

    public partial class DSignin
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Id, self);
            }

        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DSignin _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            OrderedDictionary<int, int> item2countMap = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                item2countMap.Add(reader.ReadInt32(), reader.ReadInt32());
            }
            OrderedDictionary<int, int> vipitem2vipcountMap = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                vipitem2vipcountMap.Add(reader.ReadInt32(), reader.ReadInt32());
            }
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

