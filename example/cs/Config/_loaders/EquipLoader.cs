namespace Config.Equip
{
    public partial class DTestPackBean
    {
        internal static DTestPackBean _create(ConfigReader reader)
        {
            var valueName = reader.ReadStringInPool();
            var valueIRange = DRange._create(reader);
            return new DTestPackBean {
                Name = valueName,
                IRange = valueIRange,
            };
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode() + IRange.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTestPackBean o && Name.Equals(o.Name) && IRange.Equals(o.IRange);
        }

        public override string ToString()
        {
            return "(" + Name + "," + IRange + ")";
        }


    }

    public partial class DAbilityInfo
    {
        private static readonly int[] _symbolKeys = { 1, 2, 3, 4, 5, 6, 7 };
        internal static int PrimaryKey(DAbilityInfo self) => self.Id;
        internal static void InitializeAll(List<DAbilityInfo> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DAbilityInfo>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                string name = self.Name;
                int symbol = (int)self.EEnum;
                if (symbol < 0) errors.UnknownSymbol("equip.ability", name, key, config.Explain("equip.ability", key));
                else {
                    if (!key.Equals(_symbolKeys[symbol])) errors.ChangedSymbolId("equip.ability", name, _symbolKeys[symbol], key, config.Explain("equip.ability", key));
                }
            }
            config._equip_dability_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DAbilityInfo>(s_all);
            config._equip_dability_Symbols[(int)DAbility.Attack] = Get(config, 1);
            if (config._equip_dability_Symbols[(int)DAbility.Attack] == null) errors.MissingSymbol("equip.ability", "attack", 1);
            config._equip_dability_Symbols[(int)DAbility.Defence] = Get(config, 2);
            if (config._equip_dability_Symbols[(int)DAbility.Defence] == null) errors.MissingSymbol("equip.ability", "defence", 2);
            config._equip_dability_Symbols[(int)DAbility.Hp] = Get(config, 3);
            if (config._equip_dability_Symbols[(int)DAbility.Hp] == null) errors.MissingSymbol("equip.ability", "hp", 3);
            config._equip_dability_Symbols[(int)DAbility.Critical] = Get(config, 4);
            if (config._equip_dability_Symbols[(int)DAbility.Critical] == null) errors.MissingSymbol("equip.ability", "critical", 4);
            config._equip_dability_Symbols[(int)DAbility.Critical_resist] = Get(config, 5);
            if (config._equip_dability_Symbols[(int)DAbility.Critical_resist] == null) errors.MissingSymbol("equip.ability", "critical_resist", 5);
            config._equip_dability_Symbols[(int)DAbility.Block] = Get(config, 6);
            if (config._equip_dability_Symbols[(int)DAbility.Block] == null) errors.MissingSymbol("equip.ability", "block", 6);
            config._equip_dability_Symbols[(int)DAbility.Break_armor] = Get(config, 7);
            if (config._equip_dability_Symbols[(int)DAbility.Break_armor] == null) errors.MissingSymbol("equip.ability", "break_armor", 7);
            config._equip_dability_Rows = list.AsReadOnly();
        }

        internal static DAbilityInfo _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            var valueName = reader.ReadStringInPool();
            return new DAbilityInfo {
                Id = valueId,
                Name = valueName,
                EEnum = valueName switch {
                    "attack" => DAbility.Attack,
                    "defence" => DAbility.Defence,
                    "hp" => DAbility.Hp,
                    "critical" => DAbility.Critical,
                    "critical_resist" => DAbility.Critical_resist,
                    "block" => DAbility.Block,
                    "break_armor" => DAbility.Break_armor,
                    _ => (DAbility)(-1)
                }
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAbilityInfo o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Name + ")";
        }


    }

    public partial class DEquipConfig
    {
        internal static string PrimaryKey(DEquipConfig self) => self.Entry;
        internal static void InitializeAll(List<DEquipConfig> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<string, DEquipConfig>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                string name = self.Entry;
                if (name.Length == 0) continue;
                int symbol = name switch {
                    "Instance" => 0,
                    "Instance2" => 1,
                    _ => -1
                };
                if (symbol < 0) errors.UnknownSymbol("equip.equip_config", name, key, config.Explain("equip.equip_config", key));
            }
            config._equip_dequip_uconfig_All = new System.Collections.ObjectModel.ReadOnlyDictionary<string, DEquipConfig>(s_all);
            config._equip_dequip_uconfig_Symbol_Instance = Get(config, "Instance");
            if (config._equip_dequip_uconfig_Symbol_Instance == null) errors.MissingSymbol("equip.equip_config", "Instance", "Instance");
            config._equip_dequip_uconfig_Symbol_Instance2 = Get(config, "Instance2");
            if (config._equip_dequip_uconfig_Symbol_Instance2 == null) errors.MissingSymbol("equip.equip_config", "Instance2", "Instance2");
            config._equip_dequip_uconfig_Rows = list.AsReadOnly();
        }

        internal static DEquipConfig _create(ConfigReader reader)
        {
            var valueEntry = reader.ReadStringInPool();
            var valueStone_count_for_set = reader.ReadInt32();
            var valueDraw_protect_name = reader.ReadStringInPool();
            var valueBroadcastid = reader.ReadInt32();
            var valueBroadcast_least_quality = reader.ReadInt32();
            var valueWeek_reward_mailid = reader.ReadInt32();
            return new DEquipConfig {
                Entry = valueEntry,
                Stone_count_for_set = valueStone_count_for_set,
                Draw_protect_name = valueDraw_protect_name,
                Broadcastid = valueBroadcastid,
                Broadcast_least_quality = valueBroadcast_least_quality,
                Week_reward_mailid = valueWeek_reward_mailid,
            };
        }

        public override int GetHashCode()
        {
            return Entry.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DEquipConfig o && Entry.Equals(o.Entry);
        }

        public override string ToString()
        {
            return "(" + Entry + "," + Stone_count_for_set + "," + Draw_protect_name + "," + Broadcastid + "," + Broadcast_least_quality + "," + Week_reward_mailid + ")";
        }


    }

    public partial class DJewelry
    {
        internal static int PrimaryKey(DJewelry self) => self.ID;
        internal static void InitializeAll(List<DJewelry> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DJewelry>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._equip_djewelry_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DJewelry>(s_all);
            config._equip_djewelry_Rows = list.AsReadOnly();
        }

        internal static DJewelry _create(ConfigReader reader)
        {
            var valueID = reader.ReadInt32();
            var valueName = reader.ReadStringInPool();
            var valueIconFile = reader.ReadStringInPool();
            var valueLvlRank = DLevelRank._create(reader);
            var valueJType = reader.ReadStringInPool();
            var valueSuitID = reader.ReadInt32();
            var valueKeyAbility = reader.ReadInt32();
            var valueKeyAbilityValue = reader.ReadInt32();
            var valueSalePrice = reader.ReadInt32();
            var valueDescription = reader.ReadStringInPool();
            return new DJewelry {
                ID = valueID,
                Name = valueName,
                IconFile = valueIconFile,
                LvlRank = valueLvlRank,
                JType = valueJType,
                SuitID = valueSuitID,
                KeyAbility = valueKeyAbility,
                KeyAbilityValue = valueKeyAbilityValue,
                SalePrice = valueSalePrice,
                Description = valueDescription,
            };
        }

        public override int GetHashCode()
        {
            return ID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DJewelry o && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Name + "," + IconFile + "," + LvlRank + "," + JType + "," + SuitID + "," + KeyAbility + "," + KeyAbilityValue + "," + SalePrice + "," + Description + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            LvlRank._resolve(errors, config);
            var rRefLvlRank = Equip.DJewelryRandom.Get(config, LvlRank);
            if (rRefLvlRank == null) errors.MissingReference("equip.jewelry", "LvlRank", "equip.jewelry_random", this.LvlRank);
            else RefLvlRank = rRefLvlRank;
            var rRefJType = Equip.DJewelryTypeInfo.Get(config, JType);
            if (rRefJType == null) errors.MissingReference("equip.jewelry", "JType", "equip.jewelry_type", this.JType);
            else RefJType = rRefJType.EEnum;
            NullableRefSuitID = Equip.DJewelrySuit.Get(config, SuitID);
            var rRefKeyAbility = Equip.DAbilityInfo.Get(config, KeyAbility);
            if (rRefKeyAbility == null) errors.MissingReference("equip.jewelry", "KeyAbility", "equip.ability", this.KeyAbility);
            else RefKeyAbility = rRefKeyAbility.EEnum;
        }

    }

    public partial class DJewelryRandom
    {
        internal static DLevelRank PrimaryKey(DJewelryRandom self) => self.LvlRank;
        internal static void InitializeAll(List<DJewelryRandom> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<DLevelRank, DJewelryRandom>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._equip_djewelry_urandom_All = new System.Collections.ObjectModel.ReadOnlyDictionary<DLevelRank, DJewelryRandom>(s_all);
            config._equip_djewelry_urandom_Rows = list.AsReadOnly();
        }

        internal static DJewelryRandom _create(ConfigReader reader)
        {
            var valueLvlRank = DLevelRank._create(reader);
            var valueAttackRange = DRange._create(reader);
            int Count_valueOtherRange = reader.ReadCount(0);
            IReadOnlyList<DRange> valueOtherRange;
            if (Count_valueOtherRange == 0) valueOtherRange = Array.Empty<DRange>();
            else {
                var values_valueOtherRange = new List<DRange>(Count_valueOtherRange);
                for (int i = 0; i < Count_valueOtherRange; i++) values_valueOtherRange.Add(DRange._create(reader));
                valueOtherRange = values_valueOtherRange.AsReadOnly();
            }
            int Count_valueTestPack = reader.ReadCount(0);
            IReadOnlyList<Equip.DTestPackBean> valueTestPack;
            if (Count_valueTestPack == 0) valueTestPack = Array.Empty<Equip.DTestPackBean>();
            else {
                var values_valueTestPack = new List<Equip.DTestPackBean>(Count_valueTestPack);
                for (int i = 0; i < Count_valueTestPack; i++) values_valueTestPack.Add(Equip.DTestPackBean._create(reader));
                valueTestPack = values_valueTestPack.AsReadOnly();
            }
            return new DJewelryRandom {
                LvlRank = valueLvlRank,
                AttackRange = valueAttackRange,
                OtherRange = valueOtherRange,
                TestPack = valueTestPack,
            };
        }

        public override int GetHashCode()
        {
            return LvlRank.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DJewelryRandom o && LvlRank.Equals(o.LvlRank);
        }

        public override string ToString()
        {
            return "(" + LvlRank + "," + AttackRange + "," + StringUtil.ToString(OtherRange) + "," + StringUtil.ToString(TestPack) + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            LvlRank._resolve(errors, config);
        }

    }

    public partial class DJewelrySuit
    {
        private static readonly int[] _symbolKeys = { 4 };
        internal static int PrimaryKey(DJewelrySuit self) => self.SuitID;
        internal static void InitializeAll(List<DJewelrySuit> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DJewelrySuit>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                string name = self.Ename;
                if (name.Length == 0) continue;
                int symbol = name == "SpecialSuit" ? 0 : -1;
                if (symbol < 0) errors.UnknownSymbol("equip.jewelry_suit", name, key, config.Explain("equip.jewelry_suit", key));
                else {
                    if (!key.Equals(_symbolKeys[symbol])) errors.ChangedSymbolId("equip.jewelry_suit", name, _symbolKeys[symbol], key, config.Explain("equip.jewelry_suit", key));
                }
            }
            config._equip_djewelry_usuit_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DJewelrySuit>(s_all);
            config._equip_djewelry_usuit_Symbol_SpecialSuit = Get(config, 4);
            if (config._equip_djewelry_usuit_Symbol_SpecialSuit == null) errors.MissingSymbol("equip.jewelry_suit", "SpecialSuit", 4);
            config._equip_djewelry_usuit_Rows = list.AsReadOnly();
        }

        internal static DJewelrySuit _create(ConfigReader reader)
        {
            var valueSuitID = reader.ReadInt32();
            var valueEname = reader.ReadStringInPool();
            var valueName = reader.ReadTextInPool();
            var valueAbility1 = reader.ReadInt32();
            var valueAbility1Value = reader.ReadInt32();
            var valueAbility2 = reader.ReadInt32();
            var valueAbility2Value = reader.ReadInt32();
            var valueAbility3 = reader.ReadInt32();
            var valueAbility3Value = reader.ReadInt32();
            int Count_valueSuitList = reader.ReadCount(0);
            IReadOnlyList<int> valueSuitList;
            if (Count_valueSuitList == 0) valueSuitList = Array.Empty<int>();
            else {
                var values_valueSuitList = new List<int>(Count_valueSuitList);
                for (int i = 0; i < Count_valueSuitList; i++) values_valueSuitList.Add(reader.ReadInt32());
                valueSuitList = values_valueSuitList.AsReadOnly();
            }
            return new DJewelrySuit {
                SuitID = valueSuitID,
                Ename = valueEname,
                Name = valueName,
                Ability1 = valueAbility1,
                Ability1Value = valueAbility1Value,
                Ability2 = valueAbility2,
                Ability2Value = valueAbility2Value,
                Ability3 = valueAbility3,
                Ability3Value = valueAbility3Value,
                SuitList = valueSuitList,
            };
        }

        public override int GetHashCode()
        {
            return SuitID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DJewelrySuit o && SuitID.Equals(o.SuitID);
        }

        public override string ToString()
        {
            return "(" + SuitID + "," + Ename + "," + Name + "," + Ability1 + "," + Ability1Value + "," + Ability2 + "," + Ability2Value + "," + Ability3 + "," + Ability3Value + "," + StringUtil.ToString(SuitList) + ")";
        }


    }

    public partial class DJewelryTypeInfo
    {
        internal static string PrimaryKey(DJewelryTypeInfo self) => self.TypeName;
        internal static void InitializeAll(List<DJewelryTypeInfo> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<string, DJewelryTypeInfo>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                string name = self.TypeName;
                int symbol = (int)self.EEnum;
                if (symbol < 0) errors.UnknownSymbol("equip.jewelry_type", name, key, config.Explain("equip.jewelry_type", key));
            }
            config._equip_djewelry_utype_All = new System.Collections.ObjectModel.ReadOnlyDictionary<string, DJewelryTypeInfo>(s_all);
            config._equip_djewelry_utype_Symbols[(int)DJewelryType.Jade] = Get(config, "Jade");
            if (config._equip_djewelry_utype_Symbols[(int)DJewelryType.Jade] == null) errors.MissingSymbol("equip.jewelry_type", "Jade", "Jade");
            config._equip_djewelry_utype_Symbols[(int)DJewelryType.Bracelet] = Get(config, "Bracelet");
            if (config._equip_djewelry_utype_Symbols[(int)DJewelryType.Bracelet] == null) errors.MissingSymbol("equip.jewelry_type", "Bracelet", "Bracelet");
            config._equip_djewelry_utype_Symbols[(int)DJewelryType.Magic] = Get(config, "Magic");
            if (config._equip_djewelry_utype_Symbols[(int)DJewelryType.Magic] == null) errors.MissingSymbol("equip.jewelry_type", "Magic", "Magic");
            config._equip_djewelry_utype_Symbols[(int)DJewelryType.Bottle] = Get(config, "Bottle");
            if (config._equip_djewelry_utype_Symbols[(int)DJewelryType.Bottle] == null) errors.MissingSymbol("equip.jewelry_type", "Bottle", "Bottle");
            config._equip_djewelry_utype_Rows = list.AsReadOnly();
        }

        internal static DJewelryTypeInfo _create(ConfigReader reader)
        {
            var valueTypeName = reader.ReadStringInPool();
            return new DJewelryTypeInfo {
                TypeName = valueTypeName,
                EEnum = valueTypeName switch {
                    "Jade" => DJewelryType.Jade,
                    "Bracelet" => DJewelryType.Bracelet,
                    "Magic" => DJewelryType.Magic,
                    "Bottle" => DJewelryType.Bottle,
                    _ => (DJewelryType)(-1)
                }
            };
        }

        public override int GetHashCode()
        {
            return TypeName.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DJewelryTypeInfo o && TypeName.Equals(o.TypeName);
        }

        public override string ToString()
        {
            return "(" + TypeName + ")";
        }


    }

    public partial class DRankInfo
    {
        private static readonly int[] _symbolKeys = { 0, 1, 2, 3, 4, 5 };
        internal static int PrimaryKey(DRankInfo self) => self.RankID;
        internal static void InitializeAll(List<DRankInfo> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new DRankInfo[count];
            var seq_RankID = new bool[count];
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                {
                    int seq = self.RankID;
                    if (seq < 0 || seq >= count || seq_RankID[seq]) errors.InvalidSeq("equip.rank", "RankID", key, seq, count, config.Explain("equip.rank", key));
                    else seq_RankID[seq] = true;
                }
                if (key >= 0 && key < count) s_all[key] = self;
                string name = self.RankName;
                int symbol = (int)self.EEnum;
                if (symbol < 0) errors.UnknownSymbol("equip.rank", name, key, config.Explain("equip.rank", key));
                else {
                    if (!key.Equals(_symbolKeys[symbol])) errors.ChangedSymbolId("equip.rank", name, _symbolKeys[symbol], key, config.Explain("equip.rank", key));
                }
            }
            config._equip_drank_All = s_all;
            config._equip_drank_Symbols[(int)DRank.White] = Get(config, 0);
            if (config._equip_drank_Symbols[(int)DRank.White] == null) errors.MissingSymbol("equip.rank", "white", 0);
            config._equip_drank_Symbols[(int)DRank.Green] = Get(config, 1);
            if (config._equip_drank_Symbols[(int)DRank.Green] == null) errors.MissingSymbol("equip.rank", "green", 1);
            config._equip_drank_Symbols[(int)DRank.Blue] = Get(config, 2);
            if (config._equip_drank_Symbols[(int)DRank.Blue] == null) errors.MissingSymbol("equip.rank", "blue", 2);
            config._equip_drank_Symbols[(int)DRank.Purple] = Get(config, 3);
            if (config._equip_drank_Symbols[(int)DRank.Purple] == null) errors.MissingSymbol("equip.rank", "purple", 3);
            config._equip_drank_Symbols[(int)DRank.Yellow] = Get(config, 4);
            if (config._equip_drank_Symbols[(int)DRank.Yellow] == null) errors.MissingSymbol("equip.rank", "yellow", 4);
            config._equip_drank_Symbols[(int)DRank.Red] = Get(config, 5);
            if (config._equip_drank_Symbols[(int)DRank.Red] == null) errors.MissingSymbol("equip.rank", "red", 5);
            config._equip_drank_Rows = Array.AsReadOnly(s_all);
        }

        internal static DRankInfo _create(ConfigReader reader)
        {
            var valueRankID = reader.ReadInt32();
            var valueRankName = reader.ReadStringInPool();
            var valueRankShowName = reader.ReadStringInPool();
            return new DRankInfo {
                RankID = valueRankID,
                RankName = valueRankName,
                RankShowName = valueRankShowName,
                EEnum = valueRankName switch {
                    "white" => DRank.White,
                    "green" => DRank.Green,
                    "blue" => DRank.Blue,
                    "purple" => DRank.Purple,
                    "yellow" => DRank.Yellow,
                    "red" => DRank.Red,
                    _ => (DRank)(-1)
                }
            };
        }

        public override int GetHashCode()
        {
            return RankID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DRankInfo o && RankID.Equals(o.RankID);
        }

        public override string ToString()
        {
            return "(" + RankID + "," + RankName + "," + RankShowName + ")";
        }


    }

}

