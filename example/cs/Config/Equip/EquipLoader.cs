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

    public partial class DataAbility
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Id, self);
                if (self.Name.Trim().Length == 0)
                    continue;
                switch(self.Name.Trim())
                {
                    case "attack":
                        if (Attack != null)
                            reader.EnumDuplicateInData("attack");
                        Attack = self;
                        break;
                    case "defence":
                        if (Defence != null)
                            reader.EnumDuplicateInData("defence");
                        Defence = self;
                        break;
                    case "hp":
                        if (Hp != null)
                            reader.EnumDuplicateInData("hp");
                        Hp = self;
                        break;
                    case "critical":
                        if (Critical != null)
                            reader.EnumDuplicateInData("critical");
                        Critical = self;
                        break;
                    case "critical_resist":
                        if (Critical_resist != null)
                            reader.EnumDuplicateInData("critical_resist");
                        Critical_resist = self;
                        break;
                    case "block":
                        if (Block != null)
                            reader.EnumDuplicateInData("block");
                        Block = self;
                        break;
                    case "break_armor":
                        if (Break_armor != null)
                            reader.EnumDuplicateInData("break_armor");
                        Break_armor = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.Name.Trim());
                        break;
                }
            }

            if (Attack == null)
                reader.EnumNotInData("attack");
            if (Defence == null)
                reader.EnumNotInData("defence");
            if (Hp == null)
                reader.EnumNotInData("hp");
            if (Critical == null)
                reader.EnumNotInData("critical");
            if (Critical_resist == null)
                reader.EnumNotInData("critical_resist");
            if (Block == null)
                reader.EnumNotInData("block");
            if (Break_armor == null)
                reader.EnumNotInData("break_armor");
        }

        internal static DataAbility _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            var name = reader.ReadStringInPool();
            return new DataAbility {
                Id = id,
                Name = name,
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
            var o = obj as DataAbility;
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
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Entry, self);
                if (self.Entry.Trim().Length == 0)
                    continue;
                switch(self.Entry.Trim())
                {
                    case "Instance":
                        if (Instance != null)
                            reader.EnumDuplicateInData("Instance");
                        Instance = self;
                        break;
                    case "Instance2":
                        if (Instance2 != null)
                            reader.EnumDuplicateInData("Instance2");
                        Instance2 = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.Entry.Trim());
                        break;
                }
            }

            if (Instance == null)
                reader.EnumNotInData("Instance");
            if (Instance2 == null)
                reader.EnumNotInData("Instance2");
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
        RefLvlRank = Equip.DataJewelryrandom.Get(LvlRank)!;
        if (RefLvlRank == null) reader.RefNotFound("equip.jewelry", "LvlRank", LvlRank.ToString());
        RefJType = Equip.DataJewelrytype.Get(JType)!;
        if (RefJType == null) reader.RefNotFound("equip.jewelry", "JType", JType.ToString());
        NullableRefSuitID = Equip.DataJewelrysuit.Get(SuitID);
        RefKeyAbility = Equip.DataAbility.Get(KeyAbility)!;
        if (RefKeyAbility == null) reader.RefNotFound("equip.jewelry", "KeyAbility", KeyAbility.ToString());
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
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.SuitID, self);
                if (self.Ename.Trim().Length == 0)
                    continue;
                switch(self.Ename.Trim())
                {
                    case "SpecialSuit":
                        if (SpecialSuit != null)
                            reader.EnumDuplicateInData("SpecialSuit");
                        SpecialSuit = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.Ename.Trim());
                        break;
                }
            }

            if (SpecialSuit == null)
                reader.EnumNotInData("SpecialSuit");
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

    public partial class DataJewelrytype
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.TypeName, self);
                if (self.TypeName.Trim().Length == 0)
                    continue;
                switch(self.TypeName.Trim())
                {
                    case "Jade":
                        if (Jade != null)
                            reader.EnumDuplicateInData("Jade");
                        Jade = self;
                        break;
                    case "Bracelet":
                        if (Bracelet != null)
                            reader.EnumDuplicateInData("Bracelet");
                        Bracelet = self;
                        break;
                    case "Magic":
                        if (Magic != null)
                            reader.EnumDuplicateInData("Magic");
                        Magic = self;
                        break;
                    case "Bottle":
                        if (Bottle != null)
                            reader.EnumDuplicateInData("Bottle");
                        Bottle = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.TypeName.Trim());
                        break;
                }
            }

            if (Jade == null)
                reader.EnumNotInData("Jade");
            if (Bracelet == null)
                reader.EnumNotInData("Bracelet");
            if (Magic == null)
                reader.EnumNotInData("Magic");
            if (Bottle == null)
                reader.EnumNotInData("Bottle");
        }

        internal static DataJewelrytype _create(ConfigReader reader)
        {
            var typeName = reader.ReadStringInPool();
            return new DataJewelrytype {
                TypeName = typeName,
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
            var o = obj as DataJewelrytype;
            return o != null && TypeName.Equals(o.TypeName);
        }

        public override string ToString()
        {
            return "(" + TypeName + ")";
        }

    }

    public partial class DataRank
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.RankID, self);
                if (self.RankName.Trim().Length == 0)
                    continue;
                switch(self.RankName.Trim())
                {
                    case "white":
                        if (White != null)
                            reader.EnumDuplicateInData("white");
                        White = self;
                        break;
                    case "green":
                        if (Green != null)
                            reader.EnumDuplicateInData("green");
                        Green = self;
                        break;
                    case "blue":
                        if (Blue != null)
                            reader.EnumDuplicateInData("blue");
                        Blue = self;
                        break;
                    case "purple":
                        if (Purple != null)
                            reader.EnumDuplicateInData("purple");
                        Purple = self;
                        break;
                    case "yellow":
                        if (Yellow != null)
                            reader.EnumDuplicateInData("yellow");
                        Yellow = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.RankName.Trim());
                        break;
                }
            }

            if (White == null)
                reader.EnumNotInData("white");
            if (Green == null)
                reader.EnumNotInData("green");
            if (Blue == null)
                reader.EnumNotInData("blue");
            if (Purple == null)
                reader.EnumNotInData("purple");
            if (Yellow == null)
                reader.EnumNotInData("yellow");
        }

        internal static DataRank _create(ConfigReader reader)
        {
            var rankID = reader.ReadInt32();
            var rankName = reader.ReadStringInPool();
            var rankShowName = reader.ReadStringInPool();
            return new DataRank {
                RankID = rankID,
                RankName = rankName,
                RankShowName = rankShowName,
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
            var o = obj as DataRank;
            return o != null && RankID.Equals(o.RankID);
        }

        public override string ToString()
        {
            return "(" + RankID + "," + RankName + "," + RankShowName + ")";
        }

    }

}

