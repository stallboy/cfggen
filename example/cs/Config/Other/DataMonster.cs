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
        public KeyedList<string, int> EnumMap1 { get; private set; }
        public KeyedList<int, string> EnumMap2 { get; private set; }
        public Config.Other.DataLootitem RefLoot { get; private set; }
        public Config.Other.DataLoot RefAllLoot { get; private set; }
        public KeyedList<int, Config.Other.DataArgcapturemode> RefEnumMap2 { get; private set; }

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
            return "(" + Id + "," + StringUtil.ToString(PosList) + "," + LootId + "," + LootItemId + "," + EnumMap1 + "," + EnumMap2 + ")";
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
            self.EnumMap1 = new KeyedList<string, int>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                self.EnumMap1.Add(os.ReadStringInPool(), os.ReadInt32());
            }
            self.EnumMap2 = new KeyedList<int, string>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                self.EnumMap2.Add(os.ReadInt32(), os.ReadStringInPool());
            }
            return self;
        }

        internal void _resolve(Config.LoadErrors errors)
        {
            RefLoot = Config.Other.DataLootitem.Get(LootId, LootItemId);;
            if (RefLoot == null) errors.RefNull("other.monster", ToString(), "Loot");
            RefAllLoot = Config.Other.DataLoot.Get(LootId);;
            if (RefAllLoot == null) errors.RefNull("other.monster", ToString(), "AllLoot");
            RefEnumMap2 = new KeyedList<int, Config.Other.DataArgcapturemode>();
            foreach(var kv in EnumMap2.Map)
            {
                var k = kv.Key;
                var v = Config.Other.DataArgcapturemode.Get(kv.Value);;
                if (v == null) errors.RefNull("other.monster", ToString(), "enumMap2");
                RefEnumMap2.Add(k, v);
            }
        }
    }
}