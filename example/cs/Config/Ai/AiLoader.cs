namespace Config.Ai
{
    public partial class DAi
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

        internal static DAi _create(ConfigReader reader)
        {
            var iD = reader.ReadInt32();
            var desc = reader.ReadStringInPool();
            var condID = reader.ReadStringInPool();
            var trigTick = Ai.DTriggerTick._create(reader);
            var trigOdds = reader.ReadInt32();
            List<int> actionID = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                actionID.Add(reader.ReadInt32());
            var deathRemove = reader.ReadBool();
            return new DAi {
                ID = iD,
                Desc = desc,
                CondID = condID,
                TrigTick = trigTick,
                TrigOdds = trigOdds,
                ActionID = actionID,
                DeathRemove = deathRemove,
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
            var o = obj as DAi;
            return o != null && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Desc + "," + CondID + "," + TrigTick + "," + TrigOdds + "," + StringUtil.ToString(ActionID) + "," + DeathRemove + ")";
        }

    }

    public partial class DAi_action
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

        internal static DAi_action _create(ConfigReader reader)
        {
            var iD = reader.ReadInt32();
            var desc = reader.ReadStringInPool();
            var formulaID = reader.ReadInt32();
            List<int> argIList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                argIList.Add(reader.ReadInt32());
            List<int> argSList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                argSList.Add(reader.ReadInt32());
            return new DAi_action {
                ID = iD,
                Desc = desc,
                FormulaID = formulaID,
                ArgIList = argIList,
                ArgSList = argSList,
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
            var o = obj as DAi_action;
            return o != null && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Desc + "," + FormulaID + "," + StringUtil.ToString(ArgIList) + "," + StringUtil.ToString(ArgSList) + ")";
        }

    }

    public partial class DAi_condition
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

        internal static DAi_condition _create(ConfigReader reader)
        {
            var iD = reader.ReadInt32();
            var desc = reader.ReadStringInPool();
            var formulaID = reader.ReadInt32();
            List<int> argIList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                argIList.Add(reader.ReadInt32());
            List<int> argSList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                argSList.Add(reader.ReadInt32());
            return new DAi_condition {
                ID = iD,
                Desc = desc,
                FormulaID = formulaID,
                ArgIList = argIList,
                ArgSList = argSList,
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
            var o = obj as DAi_condition;
            return o != null && ID.Equals(o.ID);
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
            var impl = reader.ReadStringInPool();
            switch(impl)
            {
                case "ConstValue":
                    return Ai.TriggerTick.DConstValue._create(reader);
                case "ByLevel":
                    return Ai.TriggerTick.DByLevel._create(reader);
                case "ByServerUpDay":
                    return Ai.TriggerTick.DByServerUpDay._create(reader);
            }
            throw reader.NotFoundImpl(impl, "ai.TriggerTick");
        }
    }

}

namespace Config.Ai.TriggerTick
{
    public partial class DConstValue
    {
        internal static DConstValue _create(ConfigReader reader)
        {
            var value = reader.ReadInt32();
            return new DConstValue {
                Value = value,
            };
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DConstValue;
            return o != null && Value.Equals(o.Value);
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
            var init = reader.ReadInt32();
            var coefficient = reader.ReadSingle();
            return new DByLevel {
                Init = init,
                Coefficient = coefficient,
            };
        }

        public override int GetHashCode()
        {
            return Init.GetHashCode() + Coefficient.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DByLevel;
            return o != null && Init.Equals(o.Init) && Coefficient.Equals(o.Coefficient);
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
            var init = reader.ReadInt32();
            var coefficient1 = reader.ReadSingle();
            var coefficient2 = reader.ReadSingle();
            return new DByServerUpDay {
                Init = init,
                Coefficient1 = coefficient1,
                Coefficient2 = coefficient2,
            };
        }

        public override int GetHashCode()
        {
            return Init.GetHashCode() + Coefficient1.GetHashCode() + Coefficient2.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DByServerUpDay;
            return o != null && Init.Equals(o.Init) && Coefficient1.Equals(o.Coefficient1) && Coefficient2.Equals(o.Coefficient2);
        }

        public override string ToString()
        {
            return "(" + Init + "," + Coefficient1 + "," + Coefficient2 + ")";
        }

    }

}

