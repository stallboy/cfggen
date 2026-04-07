namespace Config.Equip
{
    public partial class DataTestPackBean
    {
        internal static DataTestPackBean _create(ConfigReader reader)
        {
            var name = reader.ReadStringInPool();
            var iRange = DataRange._create(reader);
            return new DataTestPackBean {
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
            var o = obj as DataTestPackBean;
            return o != null && Name.Equals(o.Name) && IRange.Equals(o.IRange);
        }

        public override string ToString()
        {
            return "(" + Name + "," + IRange + ")";
        }

    }

    public partial class DataAbilityInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Id, self);
                DataAbilityExtensions._infos[(int)self.eEnum] = self;
            }

        }

        internal static DataAbilityInfo _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            var name = reader.ReadStringInPool();
            return new DataAbilityInfo {
                Id = id,
                Name = name,
                eEnum = Enum.Parse<DataAbility>(StringUtil.UpperFirstChar(name))
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
            var o = obj as DataAbilityInfo;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Name + ")";
        }

    }

    public partial class DataEquipconfig
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            DataEquipconfig? eInstance = null;
            DataEquipconfig? eInstance2 = null;
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Entry, self);
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

            if (eInstance == null) reader.EnumNotInData("Instance");
            else Instance = eInstance;
            if (eInstance2 == null) reader.EnumNotInData("Instance2");
            else Instance2 = eInstance2;
        }

        internal static DataEquipconfig _create(ConfigReader reader)
        {
            var entry = reader.ReadStringInPool();
            var stone_count_for_set = reader.ReadInt32();
            var draw_protect_name = reader.ReadStringInPool();
            var broadcastid = reader.ReadInt32();
            var broadcast_least_quality = reader.ReadInt32();
            var week_reward_mailid = reader.ReadInt32();
            return new DataEquipconfig {
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
            var o = obj as DataEquipconfig;
            return o != null && Entry.Equals(o.Entry);
        }

        public override string ToString()
        {
            return "(" + Entry + "," + Stone_count_for_set + "," + Draw_protect_name + "," + Broadcastid + "," + Broadcast_least_quality + "," + Week_reward_mailid + ")";
        }

    }

    public partial class DataJewelry
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.ID, self);
            }

        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DataJewelry _create(ConfigReader reader)
        {
            var iD = reader.ReadInt32();
            var name = reader.ReadStringInPool();
            var iconFile = reader.ReadStringInPool();
            var lvlRank = DataLevelRank._create(reader);
            var jType = reader.ReadStringInPool();
            var suitID = reader.ReadInt32();
            var keyAbility = reader.ReadInt32();
            var keyAbilityValue = reader.ReadInt32();
            var salePrice = reader.ReadInt32();
            var description = reader.ReadStringInPool();
            return new DataJewelry {
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
            var o = obj as DataJewelry;
            return o != null && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Name + "," + IconFile + "," + LvlRank + "," + JType + "," + SuitID + "," + KeyAbility + "," + KeyAbilityValue + "," + SalePrice + "," + Description + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            LvlRank._resolve(reader);
            var rRefLvlRank = Equip.DataJewelryrandom.Get(LvlRank);
            if (rRefLvlRank == null) reader.RefNotFound("equip.jewelry", "LvlRank", LvlRank.ToString());
            else RefLvlRank = rRefLvlRank;
            var rRefJType = Equip.DataJewelrytypeInfo.Get(JType);
            if (rRefJType == null) reader.RefNotFound("equip.jewelry", "JType", JType);
            else RefJType = rRefJType.eEnum;
            NullableRefSuitID = Equip.DataJewelrysuit.Get(SuitID);
            var rRefKeyAbility = Equip.DataAbilityInfo.Get(KeyAbility);
            if (rRefKeyAbility == null) reader.RefNotFound("equip.jewelry", "KeyAbility", KeyAbility.ToString());
            else RefKeyAbility = rRefKeyAbility.eEnum;
        }
    }

    public partial class DataJewelryrandom
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.LvlRank, self);
            }

        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DataJewelryrandom _create(ConfigReader reader)
        {
            var lvlRank = DataLevelRank._create(reader);
            var attackRange = DataRange._create(reader);
            List<DataRange> otherRange = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                otherRange.Add(DataRange._create(reader));
            List<Equip.DataTestPackBean> testPack = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                testPack.Add(Equip.DataTestPackBean._create(reader));
            return new DataJewelryrandom {
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
            var o = obj as DataJewelryrandom;
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

    public partial class DataJewelrysuit
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            DataJewelrysuit? eSpecialSuit = null;
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.SuitID, self);
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

            if (eSpecialSuit == null) reader.EnumNotInData("SpecialSuit");
            else SpecialSuit = eSpecialSuit;
        }

        internal static DataJewelrysuit _create(ConfigReader reader)
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
            List<int> suitList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                suitList.Add(reader.ReadInt32());
            return new DataJewelrysuit {
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
            var o = obj as DataJewelrysuit;
            return o != null && SuitID.Equals(o.SuitID);
        }

        public override string ToString()
        {
            return "(" + SuitID + "," + Ename + "," + Name + "," + Ability1 + "," + Ability1Value + "," + Ability2 + "," + Ability2Value + "," + Ability3 + "," + Ability3Value + "," + StringUtil.ToString(SuitList) + ")";
        }

    }

    public partial class DataJewelrytypeInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.TypeName, self);
                DataJewelrytypeExtensions._infos[(int)self.eEnum] = self;
            }

        }

        internal static DataJewelrytypeInfo _create(ConfigReader reader)
        {
            var typeName = reader.ReadStringInPool();
            return new DataJewelrytypeInfo {
                TypeName = typeName,
                eEnum = Enum.Parse<DataJewelrytype>(StringUtil.UpperFirstChar(typeName))
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
            var o = obj as DataJewelrytypeInfo;
            return o != null && TypeName.Equals(o.TypeName);
        }

        public override string ToString()
        {
            return "(" + TypeName + ")";
        }

    }

    public partial class DataRankInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.RankID, self);
                DataRankExtensions._infos[(int)self.eEnum] = self;
            }

        }

        internal static DataRankInfo _create(ConfigReader reader)
        {
            var rankID = reader.ReadInt32();
            var rankName = reader.ReadStringInPool();
            var rankShowName = reader.ReadStringInPool();
            return new DataRankInfo {
                RankID = rankID,
                RankName = rankName,
                RankShowName = rankShowName,
                eEnum = Enum.Parse<DataRank>(StringUtil.UpperFirstChar(rankName))
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
            var o = obj as DataRankInfo;
            return o != null && RankID.Equals(o.RankID);
        }

        public override string ToString()
        {
            return "(" + RankID + "," + RankName + "," + RankShowName + ")";
        }

    }

}

