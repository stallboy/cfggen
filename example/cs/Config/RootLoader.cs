namespace Config
{
    public partial class DataLevelRank
    {
        internal static DataLevelRank _create(ConfigReader reader)
        {
            var level = reader.ReadInt32();
            var rank = reader.ReadInt32();
            return new DataLevelRank {
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
            var o = obj as DataLevelRank;
            return o != null && Level.Equals(o.Level) && Rank.Equals(o.Rank);
        }

        public override string ToString()
        {
            return "(" + Level + "," + Rank + ")";
        }

    internal void _resolve(ConfigReader reader)
    {
        RefRank = Equip.DataRank.Get(Rank)!;
        if (RefRank == null) reader.RefNotFound("LevelRank", "Rank", Rank.ToString());
    }
    }

    public partial class DataPosition
    {
        internal static DataPosition _create(ConfigReader reader)
        {
            var x = reader.ReadInt32();
            var y = reader.ReadInt32();
            var z = reader.ReadInt32();
            return new DataPosition {
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
            var o = obj as DataPosition;
            return o != null && X.Equals(o.X) && Y.Equals(o.Y) && Z.Equals(o.Z);
        }

        public override string ToString()
        {
            return "(" + X + "," + Y + "," + Z + ")";
        }

    }

    public partial class DataRange
    {
        internal static DataRange _create(ConfigReader reader)
        {
            var min = reader.ReadInt32();
            var max = reader.ReadInt32();
            return new DataRange {
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
            var o = obj as DataRange;
            return o != null && Min.Equals(o.Min) && Max.Equals(o.Max);
        }

        public override string ToString()
        {
            return "(" + Min + "," + Max + ")";
        }

    }

}

