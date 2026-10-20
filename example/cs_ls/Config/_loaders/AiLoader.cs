namespace Config.Ai
{
    public partial class DAi
    {
        internal static int PrimaryKey(DAi self) => self.ID;
        internal static void InitializeAll(List<DAi> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DAi>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._ai_dai_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DAi>(s_all);
            config._ai_dai_Rows = list.AsReadOnly();
        }

        internal static DAi _create(ConfigReader reader)
        {
            var valueID = reader.ReadInt32();
            var valueDesc = reader.ReadStringInPool();
            var valueCondID = reader.ReadStringInPool();
            var valueTrigTick = Ai.DTriggerTick._create(reader);
            var valueTrigOdds = reader.ReadInt32();
            int Count_valueActionID = reader.ReadCount(0);
            IReadOnlyList<int> valueActionID;
            if (Count_valueActionID == 0) valueActionID = Array.Empty<int>();
            else {
                var values_valueActionID = new List<int>(Count_valueActionID);
                for (int i = 0; i < Count_valueActionID; i++) values_valueActionID.Add(reader.ReadInt32());
                valueActionID = values_valueActionID.AsReadOnly();
            }
            var valueDeathRemove = reader.ReadBool();
            return new DAi {
                ID = valueID,
                Desc = valueDesc,
                CondID = valueCondID,
                TrigTick = valueTrigTick,
                TrigOdds = valueTrigOdds,
                ActionID = valueActionID,
                DeathRemove = valueDeathRemove,
            };
        }

        public override int GetHashCode()
        {
            return ID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAi o && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Desc + "," + CondID + "," + TrigTick + "," + TrigOdds + "," + StringUtil.ToString(ActionID) + "," + DeathRemove + ")";
        }


    }

    public partial class DAiAction
    {
        internal static int PrimaryKey(DAiAction self) => self.ID;
        internal static void InitializeAll(List<DAiAction> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DAiAction>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._ai_dai_uaction_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DAiAction>(s_all);
            config._ai_dai_uaction_Rows = list.AsReadOnly();
        }

        internal static DAiAction _create(ConfigReader reader)
        {
            var valueID = reader.ReadInt32();
            var valueDesc = reader.ReadStringInPool();
            var valueFormulaID = reader.ReadInt32();
            int Count_valueArgIList = reader.ReadCount(0);
            IReadOnlyList<int> valueArgIList;
            if (Count_valueArgIList == 0) valueArgIList = Array.Empty<int>();
            else {
                var values_valueArgIList = new List<int>(Count_valueArgIList);
                for (int i = 0; i < Count_valueArgIList; i++) values_valueArgIList.Add(reader.ReadInt32());
                valueArgIList = values_valueArgIList.AsReadOnly();
            }
            int Count_valueArgSList = reader.ReadCount(0);
            IReadOnlyList<int> valueArgSList;
            if (Count_valueArgSList == 0) valueArgSList = Array.Empty<int>();
            else {
                var values_valueArgSList = new List<int>(Count_valueArgSList);
                for (int i = 0; i < Count_valueArgSList; i++) values_valueArgSList.Add(reader.ReadInt32());
                valueArgSList = values_valueArgSList.AsReadOnly();
            }
            return new DAiAction {
                ID = valueID,
                Desc = valueDesc,
                FormulaID = valueFormulaID,
                ArgIList = valueArgIList,
                ArgSList = valueArgSList,
            };
        }

        public override int GetHashCode()
        {
            return ID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAiAction o && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Desc + "," + FormulaID + "," + StringUtil.ToString(ArgIList) + "," + StringUtil.ToString(ArgSList) + ")";
        }


    }

    public partial class DAiCondition
    {
        internal static int PrimaryKey(DAiCondition self) => self.ID;
        internal static void InitializeAll(List<DAiCondition> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DAiCondition>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._ai_dai_ucondition_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DAiCondition>(s_all);
            config._ai_dai_ucondition_Rows = list.AsReadOnly();
        }

        internal static DAiCondition _create(ConfigReader reader)
        {
            var valueID = reader.ReadInt32();
            var valueDesc = reader.ReadStringInPool();
            var valueFormulaID = reader.ReadInt32();
            int Count_valueArgIList = reader.ReadCount(0);
            IReadOnlyList<int> valueArgIList;
            if (Count_valueArgIList == 0) valueArgIList = Array.Empty<int>();
            else {
                var values_valueArgIList = new List<int>(Count_valueArgIList);
                for (int i = 0; i < Count_valueArgIList; i++) values_valueArgIList.Add(reader.ReadInt32());
                valueArgIList = values_valueArgIList.AsReadOnly();
            }
            int Count_valueArgSList = reader.ReadCount(0);
            IReadOnlyList<int> valueArgSList;
            if (Count_valueArgSList == 0) valueArgSList = Array.Empty<int>();
            else {
                var values_valueArgSList = new List<int>(Count_valueArgSList);
                for (int i = 0; i < Count_valueArgSList; i++) values_valueArgSList.Add(reader.ReadInt32());
                valueArgSList = values_valueArgSList.AsReadOnly();
            }
            return new DAiCondition {
                ID = valueID,
                Desc = valueDesc,
                FormulaID = valueFormulaID,
                ArgIList = valueArgIList,
                ArgSList = valueArgSList,
            };
        }

        public override int GetHashCode()
        {
            return ID.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAiCondition o && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Desc + "," + FormulaID + "," + StringUtil.ToString(ArgIList) + "," + StringUtil.ToString(ArgSList) + ")";
        }


    }

    public partial interface DTriggerTick
    {
        internal static DTriggerTick _create(ConfigReader reader)
        {
            return reader.ReadStringInPool() switch {
                "ConstValue" => Ai.TriggerTick.DConstValue._create(reader),
                "ByLevel" => Ai.TriggerTick.DByLevel._create(reader),
                "ByServerUpDay" => Ai.TriggerTick.DByServerUpDay._create(reader),
                var impl => throw reader.Errors.UnknownVariant("ai.TriggerTick", impl)
            };
        }
    }

}

namespace Config.Ai.TriggerTick
{
    public partial class DConstValue
    {
        internal static DConstValue _create(ConfigReader reader)
        {
            var valueValue = reader.ReadInt32();
            return new DConstValue {
                Value = valueValue,
            };
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DConstValue o && Value.Equals(o.Value);
        }

        public override string ToString()
        {
            return "(" + Value + ")";
        }


    }

    public partial class DByLevel
    {
        internal static DByLevel _create(ConfigReader reader)
        {
            var valueInit = reader.ReadInt32();
            var valueCoefficient = reader.ReadSingle();
            return new DByLevel {
                Init = valueInit,
                Coefficient = valueCoefficient,
            };
        }

        public override int GetHashCode()
        {
            return Init.GetHashCode() + Coefficient.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DByLevel o && Init.Equals(o.Init) && Coefficient.Equals(o.Coefficient);
        }

        public override string ToString()
        {
            return "(" + Init + "," + Coefficient + ")";
        }


    }

    public partial class DByServerUpDay
    {
        internal static DByServerUpDay _create(ConfigReader reader)
        {
            var valueInit = reader.ReadInt32();
            var valueCoefficient1 = reader.ReadSingle();
            var valueCoefficient2 = reader.ReadSingle();
            return new DByServerUpDay {
                Init = valueInit,
                Coefficient1 = valueCoefficient1,
                Coefficient2 = valueCoefficient2,
            };
        }

        public override int GetHashCode()
        {
            return Init.GetHashCode() + Coefficient1.GetHashCode() + Coefficient2.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DByServerUpDay o && Init.Equals(o.Init) && Coefficient1.Equals(o.Coefficient1) && Coefficient2.Equals(o.Coefficient2);
        }

        public override string ToString()
        {
            return "(" + Init + "," + Coefficient1 + "," + Coefficient2 + ")";
        }


    }

}

