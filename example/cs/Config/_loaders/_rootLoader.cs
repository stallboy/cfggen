namespace Config
{
    public partial class DLevelRank
    {
        internal static DLevelRank _create(ConfigReader reader)
        {
            var level = reader.ReadInt32();
            var rank = reader.ReadInt32();
            return new DLevelRank {
                Level = level,
                Rank = rank,
            };
        }

        public override int GetHashCode()
        {
            return Level.GetHashCode() + Rank.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DLevelRank;
            return o != null && Level.Equals(o.Level) && Rank.Equals(o.Rank);
        }

        public override string ToString()
        {
            return "(" + Level + "," + Rank + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            var rRefRank = Equip.DRankInfo.Get(Rank);
            if (rRefRank == null) reader.RefNotFound("LevelRank", "Rank", Rank.ToString());
            else RefRank = rRefRank.EEnum;
        }
    }

    public partial class DPosition
    {
        internal static DPosition _create(ConfigReader reader)
        {
            var x = reader.ReadInt32();
            var y = reader.ReadInt32();
            var z = reader.ReadInt32();
            return new DPosition {
                X = x,
                Y = y,
                Z = z,
            };
        }

        public override int GetHashCode()
        {
            return X.GetHashCode() + Y.GetHashCode() + Z.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DPosition;
            return o != null && X.Equals(o.X) && Y.Equals(o.Y) && Z.Equals(o.Z);
        }

        public override string ToString()
        {
            return "(" + X + "," + Y + "," + Z + ")";
        }

    }

    public partial class DRange
    {
        internal static DRange _create(ConfigReader reader)
        {
            var min = reader.ReadInt32();
            var max = reader.ReadInt32();
            return new DRange {
                Min = min,
                Max = max,
            };
        }

        public override int GetHashCode()
        {
            return Min.GetHashCode() + Max.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DRange;
            return o != null && Min.Equals(o.Min) && Max.Equals(o.Max);
        }

        public override string ToString()
        {
            return "(" + Min + "," + Max + ")";
        }

    }

}

