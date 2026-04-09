using System.Collections.Frozen;

namespace Config.Equip
{
    public partial class DTestPackBean
    {
        internal static DTestPackBean _create(ConfigReader reader)
        {
            var name = reader.ReadStringInPool();
            var iRange = DRange._create(reader);
            return new DTestPackBean {
                Name = name,
                IRange = iRange,
            };
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode() + IRange.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DTestPackBean;
            return o != null && Name.Equals(o.Name) && IRange.Equals(o.IRange);
        }

        public override string ToString()
        {
            return "(" + Name + "," + IRange + ")";
        }

    }

    public partial class DAbilityInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DAbilityInfo>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Id, self);
                DAbilityExtensions._infos[(int)self.eEnum] = self;
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static DAbilityInfo _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            var name = reader.ReadStringInPool();
            return new DAbilityInfo {
                Id = id,
                Name = name,
                eEnum = Enum.Parse<DAbility>(StringUtil.UpperFirstChar(name))
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
            var o = obj as DAbilityInfo;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Name + ")";
        }

    }

    public partial class DEquipconfig
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<string, DEquipconfig>(count);
            DEquipconfig? eInstance = null;
            DEquipconfig? eInstance2 = null;
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Entry, self);
                if (self.Entry.Length == 0)
                    continue;
                switch(self.Entry)
                {
                    case "Instance":
                        if (eInstance != null)
                            reader.EnumDuplicateInData("Instance");
                        eInstance = self;
                        break;
                    case "Instance2":
                        if (eInstance2 != null)
                            reader.EnumDuplicateInData("Instance2");
                        eInstance2 = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.Entry);
                        break;
                }
            }
            _all = s_all.ToFrozenDictionary();
            if (eInstance == null) reader.EnumNotInData("Instance");
            else Instance = eInstance;
            if (eInstance2 == null) reader.EnumNotInData("Instance2");
            else Instance2 = eInstance2;
        }

        internal static DEquipconfig _create(ConfigReader reader)
        {
            var entry = reader.ReadStringInPool();
            var stone_count_for_set = reader.ReadInt32();
            var draw_protect_name = reader.ReadStringInPool();
            var broadcastid = reader.ReadInt32();
            var broadcast_least_quality = reader.ReadInt32();
            var week_reward_mailid = reader.ReadInt32();
            return new DEquipconfig {
                Entry = entry,
                Stone_count_for_set = stone_count_for_set,
                Draw_protect_name = draw_protect_name,
                Broadcastid = broadcastid,
                Broadcast_least_quality = broadcast_least_quality,
                Week_reward_mailid = week_reward_mailid,
            };
        }

        public override int GetHashCode()
        {
            return Entry.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DEquipconfig;
            return o != null && Entry.Equals(o.Entry);
        }

        public override string ToString()
        {
            return "(" + Entry + "," + Stone_count_for_set + "," + Draw_protect_name + "," + Broadcastid + "," + Broadcast_least_quality + "," + Week_reward_mailid + ")";
        }

    }

    public partial class DJewelry
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DJewelry>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.ID, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DJewelry _create(ConfigReader reader)
        {
            var iD = reader.ReadInt32();
            var name = reader.ReadStringInPool();
            var iconFile = reader.ReadStringInPool();
            var lvlRank = DLevelRank._create(reader);
            var jType = reader.ReadStringInPool();
            var suitID = reader.ReadInt32();
            var keyAbility = reader.ReadInt32();
            var keyAbilityValue = reader.ReadInt32();
            var salePrice = reader.ReadInt32();
            var description = reader.ReadStringInPool();
            return new DJewelry {
                ID = iD,
                Name = name,
                IconFile = iconFile,
                LvlRank = lvlRank,
                JType = jType,
                SuitID = suitID,
                KeyAbility = keyAbility,
                KeyAbilityValue = keyAbilityValue,
                SalePrice = salePrice,
                Description = description,
            };
        }

        public override int GetHashCode()
        {
            return ID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DJewelry;
            return o != null && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Name + "," + IconFile + "," + LvlRank + "," + JType + "," + SuitID + "," + KeyAbility + "," + KeyAbilityValue + "," + SalePrice + "," + Description + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            LvlRank._resolve(reader);
            var rRefLvlRank = Equip.DJewelryrandom.Get(LvlRank);
            if (rRefLvlRank == null) reader.RefNotFound("equip.jewelry", "LvlRank", LvlRank.ToString());
            else RefLvlRank = rRefLvlRank;
            var rRefJType = Equip.DJewelrytypeInfo.Get(JType);
            if (rRefJType == null) reader.RefNotFound("equip.jewelry", "JType", JType);
            else RefJType = rRefJType.eEnum;
            NullableRefSuitID = Equip.DJewelrysuit.Get(SuitID);
            var rRefKeyAbility = Equip.DAbilityInfo.Get(KeyAbility);
            if (rRefKeyAbility == null) reader.RefNotFound("equip.jewelry", "KeyAbility", KeyAbility.ToString());
            else RefKeyAbility = rRefKeyAbility.eEnum;
        }
    }

    public partial class DJewelryrandom
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<DLevelRank, DJewelryrandom>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.LvlRank, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DJewelryrandom _create(ConfigReader reader)
        {
            var lvlRank = DLevelRank._create(reader);
            var attackRange = DRange._create(reader);
            int Count_otherRange = reader.ReadInt32();
            var otherRange = new List<DRange>(Count_otherRange);
            for (int i = 0; i < Count_otherRange; i++)
                otherRange.Add(DRange._create(reader));
            int Count_testPack = reader.ReadInt32();
            var testPack = new List<Equip.DTestPackBean>(Count_testPack);
            for (int i = 0; i < Count_testPack; i++)
                testPack.Add(Equip.DTestPackBean._create(reader));
            return new DJewelryrandom {
                LvlRank = lvlRank,
                AttackRange = attackRange,
                OtherRange = otherRange,
                TestPack = testPack,
            };
        }

        public override int GetHashCode()
        {
            return LvlRank.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DJewelryrandom;
            return o != null && LvlRank.Equals(o.LvlRank);
        }

        public override string ToString()
        {
            return "(" + LvlRank + "," + AttackRange + "," + StringUtil.ToString(OtherRange) + "," + StringUtil.ToString(TestPack) + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            LvlRank._resolve(reader);
        }
    }

    public partial class DJewelrysuit
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DJewelrysuit>(count);
            DJewelrysuit? eSpecialSuit = null;
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.SuitID, self);
                if (self.Ename.Length == 0)
                    continue;
                switch(self.Ename)
                {
                    case "SpecialSuit":
                        if (eSpecialSuit != null)
                            reader.EnumDuplicateInData("SpecialSuit");
                        eSpecialSuit = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.Ename);
                        break;
                }
            }
            _all = s_all.ToFrozenDictionary();
            if (eSpecialSuit == null) reader.EnumNotInData("SpecialSuit");
            else SpecialSuit = eSpecialSuit;
        }

        internal static DJewelrysuit _create(ConfigReader reader)
        {
            var suitID = reader.ReadInt32();
            var ename = reader.ReadStringInPool();
            var name = reader.ReadTextInPool();
            var ability1 = reader.ReadInt32();
            var ability1Value = reader.ReadInt32();
            var ability2 = reader.ReadInt32();
            var ability2Value = reader.ReadInt32();
            var ability3 = reader.ReadInt32();
            var ability3Value = reader.ReadInt32();
            int Count_suitList = reader.ReadInt32();
            var suitList = new List<int>(Count_suitList);
            for (int i = 0; i < Count_suitList; i++)
                suitList.Add(reader.ReadInt32());
            return new DJewelrysuit {
                SuitID = suitID,
                Ename = ename,
                Name = name,
                Ability1 = ability1,
                Ability1Value = ability1Value,
                Ability2 = ability2,
                Ability2Value = ability2Value,
                Ability3 = ability3,
                Ability3Value = ability3Value,
                SuitList = suitList,
            };
        }

        public override int GetHashCode()
        {
            return SuitID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DJewelrysuit;
            return o != null && SuitID.Equals(o.SuitID);
        }

        public override string ToString()
        {
            return "(" + SuitID + "," + Ename + "," + Name + "," + Ability1 + "," + Ability1Value + "," + Ability2 + "," + Ability2Value + "," + Ability3 + "," + Ability3Value + "," + StringUtil.ToString(SuitList) + ")";
        }

    }

    public partial class DJewelrytypeInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<string, DJewelrytypeInfo>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.TypeName, self);
                DJewelrytypeExtensions._infos[(int)self.eEnum] = self;
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static DJewelrytypeInfo _create(ConfigReader reader)
        {
            var typeName = reader.ReadStringInPool();
            return new DJewelrytypeInfo {
                TypeName = typeName,
                eEnum = Enum.Parse<DJewelrytype>(StringUtil.UpperFirstChar(typeName))
            };
        }

        public override int GetHashCode()
        {
            return TypeName.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DJewelrytypeInfo;
            return o != null && TypeName.Equals(o.TypeName);
        }

        public override string ToString()
        {
            return "(" + TypeName + ")";
        }

    }

    public partial class DRankInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DRankInfo>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.RankID, self);
                DRankExtensions._infos[(int)self.eEnum] = self;
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static DRankInfo _create(ConfigReader reader)
        {
            var rankID = reader.ReadInt32();
            var rankName = reader.ReadStringInPool();
            var rankShowName = reader.ReadStringInPool();
            return new DRankInfo {
                RankID = rankID,
                RankName = rankName,
                RankShowName = rankShowName,
                eEnum = Enum.Parse<DRank>(StringUtil.UpperFirstChar(rankName))
            };
        }

        public override int GetHashCode()
        {
            return RankID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DRankInfo;
            return o != null && RankID.Equals(o.RankID);
        }

        public override string ToString()
        {
            return "(" + RankID + "," + RankName + "," + RankShowName + ")";
        }

    }

}

