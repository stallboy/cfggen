namespace Config.Other
{
    public partial class DataDropItem
    {
        internal static DataDropItem _create(ConfigReader reader)
        {
            var chance = reader.ReadInt32();
            List<int> itemids = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                itemids.Add(reader.ReadInt32());
            var countmin = reader.ReadInt32();
            var countmax = reader.ReadInt32();
            return new DataDropItem {
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
            var o = obj as DataDropItem;
            return o != null && Chance.Equals(o.Chance) && Itemids.Equals(o.Itemids) && Countmin.Equals(o.Countmin) && Countmax.Equals(o.Countmax);
        }

        public override string ToString()
        {
            return "(" + Chance + "," + StringUtil.ToString(Itemids) + "," + Countmin + "," + Countmax + ")";
        }

    }

    public partial class DataArgCaptureMode
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Name, self);
                if (self.Name.Trim().Length == 0)
                    continue;
                switch(self.Name.Trim())
                {
                    case "Snapshot":
                        if (Snapshot != null)
                            reader.EnumDuplicateInData("Snapshot");
                        Snapshot = self;
                        break;
                    case "Dynamic":
                        if (Dynamic != null)
                            reader.EnumDuplicateInData("Dynamic");
                        Dynamic = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.Name.Trim());
                        break;
                }
            }

            if (Snapshot == null)
                reader.EnumNotInData("Snapshot");
            if (Dynamic == null)
                reader.EnumNotInData("Dynamic");
        }

        internal static DataArgCaptureMode _create(ConfigReader reader)
        {
            var name = reader.ReadStringInPool();
            var comment = reader.ReadStringInPool();
            return new DataArgCaptureMode {
                Name = name,
                Comment = comment,
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
            var o = obj as DataArgCaptureMode;
            return o != null && Name.Equals(o.Name);
        }

        public override string ToString()
        {
            return "(" + Name + "," + Comment + ")";
        }

    }

    public partial class DataDrop
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

        internal static DataDrop _create(ConfigReader reader)
        {
            var dropid = reader.ReadInt32();
            var name = reader.ReadTextInPool();
            List<Other.DataDropItem> items = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                items.Add(Other.DataDropItem._create(reader));
            OrderedDictionary<int, int> testmap = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                testmap.Add(reader.ReadInt32(), reader.ReadInt32());
            }
            return new DataDrop {
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
            var o = obj as DataDrop;
            return o != null && Dropid.Equals(o.Dropid);
        }

        public override string ToString()
        {
            return "(" + Dropid + "," + Name + "," + StringUtil.ToString(Items) + "," + Testmap + ")";
        }

    }

    public partial class DataKeytest
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
        internal static DataKeytest _create(ConfigReader reader)
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
            return new DataKeytest {
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
            var o = obj as DataKeytest;
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
            var r = Other.DataSignin.Get(e);
            if (r == null) reader.RefNotFound("other.keytest", "ids", e.ToString());
            else RefIds.Add(r);
        }
        RefEnumTest = Other.DataArgCaptureMode.Get(EnumTest)!;
        if (RefEnumTest == null) reader.RefNotFound("other.keytest", "enumTest", EnumTest.ToString());
        RefEnumList = [];
        foreach(var e in EnumList)
        {
            var r = Other.DataArgCaptureMode.Get(e);
            if (r == null) reader.RefNotFound("other.keytest", "enumList", e.ToString());
            else RefEnumList.Add(r);
        }
    }
    }

    public partial class DataLoot
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
        internal static DataLoot _create(ConfigReader reader)
        {
            var lootid = reader.ReadInt32();
            var ename = reader.ReadStringInPool();
            var name = reader.ReadTextInPool();
            List<int> chanceList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                chanceList.Add(reader.ReadInt32());
            return new DataLoot {
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
            var o = obj as DataLoot;
            return o != null && Lootid.Equals(o.Lootid);
        }

        public override string ToString()
        {
            return "(" + Lootid + "," + Ename + "," + Name + "," + StringUtil.ToString(ChanceList) + ")";
        }

    internal void _resolve(ConfigReader reader)
    {
        ListRefLootid = [];
        foreach (var v in Other.DataLootitem.All())
        {
            if (v.Lootid.Equals(Lootid))
                ListRefLootid.Add(v);
        }
        ListRefAnotherWay = [];
        foreach (var v in Other.DataLootitem.All())
        {
            if (v.Lootid.Equals(Lootid))
                ListRefAnotherWay.Add(v);
        }
    }
    }

    public partial class DataLootitem
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

        internal static DataLootitem _create(ConfigReader reader)
        {
            var lootid = reader.ReadInt32();
            var itemid = reader.ReadInt32();
            var chance = reader.ReadInt32();
            var countmin = reader.ReadInt32();
            var countmax = reader.ReadInt32();
            return new DataLootitem {
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
            var o = obj as DataLootitem;
            return o != null && Lootid.Equals(o.Lootid) && Itemid.Equals(o.Itemid);
        }

        public override string ToString()
        {
            return "(" + Lootid + "," + Itemid + "," + Chance + "," + Countmin + "," + Countmax + ")";
        }

    }

    public partial class DataMonster
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
        internal static DataMonster _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            List<DataPosition> posList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                posList.Add(DataPosition._create(reader));
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
            return new DataMonster {
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
            var o = obj as DataMonster;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + StringUtil.ToString(PosList) + "," + LootId + "," + LootItemId + "," + EnumMap1 + "," + EnumMap2 + ")";
        }

    internal void _resolve(ConfigReader reader)
    {
        RefLoot = Other.DataLootitem.Get(LootId, LootItemId)!;
        if (RefLoot == null) reader.RefNotFound("other.monster", "Loot", LootId.ToString());
        RefAllLoot = Other.DataLoot.Get(LootId)!;
        if (RefAllLoot == null) reader.RefNotFound("other.monster", "AllLoot", LootId.ToString());
        RefEnumMap2 = [];
        foreach(var kv in EnumMap2)
        {
            var k = kv.Key;
            var v = Other.DataArgCaptureMode.Get(kv.Value);
            if (v == null) reader.RefNotFound("other.monster", "enumMap2", kv.Value.ToString());
            else RefEnumMap2.Add(k, v);
        }
    }
    }

    public partial class DataSignin
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
        internal static DataSignin _create(ConfigReader reader)
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
            return new DataSignin {
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
            var o = obj as DataSignin;
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
            var v = Other.DataLoot.Get(kv.Value);
            if (v == null) reader.RefNotFound("other.signin", "vipitem2vipcountMap", kv.Value.ToString());
            else RefVipitem2vipcountMap.Add(k, v);
        }
    }
    }

}

