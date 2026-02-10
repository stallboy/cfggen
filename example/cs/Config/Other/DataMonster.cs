using System;
using System.Collections.Generic;

namespace Config.Other
{
    public partial class DataMonster
    {
        public int Id { get; private set; }
        public List<Config.DataPosition> PosList { get; private set; }
        public int LootId { get; private set; } /* loot */
        public int LootItemId { get; private set; } /* item */
        public Config.Other.DataLootitem RefLoot { get; private set; }
        public Config.Other.DataLoot RefAllLoot { get; private set; }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataMonster;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + StringUtil.ToString(PosList) + "," + LootId + "," + LootItemId + ")";
        }

        
        static Config.KeyedList<int, DataMonster> all = null;

        public static DataMonster Get(int id)
        {
            DataMonster v;
            return all.TryGetValue(id, out v) ? v : null;
        }

        public static List<DataMonster> All()
        {
            return all.OrderedValues;
        }

        public static List<DataMonster> Filter(Predicate<DataMonster> predicate)
        {
            var r = new List<DataMonster>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, DataMonster>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(self.Id, self);
            }

        }

        internal static void Resolve(Config.LoadErrors errors)
        {
            foreach (var v in All())
                v._resolve(errors);
        }
        internal static DataMonster _create(Config.Stream os)
        {
            var self = new DataMonster();
            self.Id = os.ReadInt32();
            self.PosList = new List<Config.DataPosition>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.PosList.Add(Config.DataPosition._create(os));
            self.LootId = os.ReadInt32();
            self.LootItemId = os.ReadInt32();
            return self;
        }

        internal void _resolve(Config.LoadErrors errors)
        {
            RefLoot = Config.Other.DataLootitem.Get(LootId, LootItemId);;
            if (RefLoot == null) errors.RefNull("other.monster", ToString(), "Loot");
            RefAllLoot = Config.Other.DataLoot.Get(LootId);;
            if (RefAllLoot == null) errors.RefNull("other.monster", ToString(), "AllLoot");
        }
    }
}