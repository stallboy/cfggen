namespace Config
{
    public partial class DLevelRank
    {
        internal static DLevelRank _create(ConfigReader reader)
        {
            var valueLevel = reader.ReadInt32();
            var valueRank = reader.ReadInt32();
            return new DLevelRank {
                Level = valueLevel,
                Rank = valueRank,
            };
        }

        public override int GetHashCode()
        {
            return Level.GetHashCode() + Rank.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DLevelRank o && Level.Equals(o.Level) && Rank.Equals(o.Rank);
        }

        public override string ToString()
        {
            return "(" + Level + "," + Rank + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            var rRefRank = Equip.DRankInfo.Get(config, Rank);
            if (rRefRank == null) {
                errors.MissingReference("LevelRank", "Rank", "equip.rank", this.Rank);
            }
            else {
                RefRank = rRefRank.EEnum;
            }
        }

    }

    public partial class DPosition
    {
        internal static DPosition _create(ConfigReader reader)
        {
            var valueX = reader.ReadInt32();
            var valueY = reader.ReadInt32();
            var valueZ = reader.ReadInt32();
            return new DPosition {
                X = valueX,
                Y = valueY,
                Z = valueZ,
            };
        }

        public override int GetHashCode()
        {
            return X.GetHashCode() + Y.GetHashCode() + Z.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DPosition o && X.Equals(o.X) && Y.Equals(o.Y) && Z.Equals(o.Z);
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
            var valueMin = reader.ReadInt32();
            var valueMax = reader.ReadInt32();
            return new DRange {
                Min = valueMin,
                Max = valueMax,
            };
        }

        public override int GetHashCode()
        {
            return Min.GetHashCode() + Max.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DRange o && Min.Equals(o.Min) && Max.Equals(o.Max);
        }

        public override string ToString()
        {
            return "(" + Min + "," + Max + ")";
        }


    }

}

