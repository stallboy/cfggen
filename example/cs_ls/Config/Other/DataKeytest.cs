using System;
using System.Collections.Generic;

namespace Config.Other
{
    public partial class DataKeytest
    {
        public int Id1 { get; private set; }
        public long Id2 { get; private set; }
        public int Id3 { get; private set; }
        public List<int> Ids { get; private set; }
        public List<Config.Other.DataSignin> RefIds { get; private set; }

        public override int GetHashCode()
        {
            return Id1.GetHashCode() + Id2.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataKeytest;
            return o != null && Id1.Equals(o.Id1) && Id2.Equals(o.Id2);
        }

        public override string ToString()
        {
            return "(" + Id1 + "," + Id2 + "," + Id3 + "," + CSV.ToString(Ids) + ")";
        }

        
        class Id1Id2Key
        {
            readonly int Id1;
            readonly long Id2;
            public Id1Id2Key(int id1, long id2)
            {
                this.Id1 = id1;
                this.Id2 = id2;
            }

            public override int GetHashCode()
            {
                return Id1.GetHashCode() + Id2.GetHashCode();
            }

            public override bool Equals(object obj)
            {
                if (obj == null) return false;
                if (obj == this) return true;
                var o = obj as Id1Id2Key;
                return o != null && Id1.Equals(o.Id1) && Id2.Equals(o.Id2);
            }
        }

        static Config.KeyedList<Id1Id2Key, DataKeytest> all = null;

        public static DataKeytest Get(int id1, long id2)
        {
            DataKeytest v;
            return all.TryGetValue(new Id1Id2Key(id1, id2), out v) ? v : null;
        }

        
        class Id1Id3Key
        {
            readonly int Id1;
            readonly int Id3;
            public Id1Id3Key(int id1, int id3)
            {
                this.Id1 = id1;
                this.Id3 = id3;
            }

            public override int GetHashCode()
            {
                return Id1.GetHashCode() + Id3.GetHashCode();
            }

            public override bool Equals(object obj)
            {
                if (obj == null) return false;
                if (obj == this) return true;
                var o = obj as Id1Id3Key;
                return o != null && Id1.Equals(o.Id1) && Id3.Equals(o.Id3);
            }
        }

        static Config.KeyedList<Id1Id3Key, DataKeytest> id1Id3Map = null;

        public static DataKeytest GetById1Id3(int id1, int id3)
        {
            DataKeytest v;
            return id1Id3Map.TryGetValue(new Id1Id3Key(id1, id3), out v) ? v : null;
        }

        
        static Config.KeyedList<long, DataKeytest> id2Map = null;

        public static DataKeytest GetById2(long id2)
        {
            DataKeytest v;
            return id2Map.TryGetValue(id2, out v) ? v : null;
        }

        
        class Id2Id3Key
        {
            readonly long Id2;
            readonly int Id3;
            public Id2Id3Key(long id2, int id3)
            {
                this.Id2 = id2;
                this.Id3 = id3;
            }

            public override int GetHashCode()
            {
                return Id2.GetHashCode() + Id3.GetHashCode();
            }

            public override bool Equals(object obj)
            {
                if (obj == null) return false;
                if (obj == this) return true;
                var o = obj as Id2Id3Key;
                return o != null && Id2.Equals(o.Id2) && Id3.Equals(o.Id3);
            }
        }

        static Config.KeyedList<Id2Id3Key, DataKeytest> id2Id3Map = null;

        public static DataKeytest GetById2Id3(long id2, int id3)
        {
            DataKeytest v;
            return id2Id3Map.TryGetValue(new Id2Id3Key(id2, id3), out v) ? v : null;
        }

        public static List<DataKeytest> All()
        {
            return all.OrderedValues;
        }

        public static List<DataKeytest> Filter(Predicate<DataKeytest> predicate)
        {
            var r = new List<DataKeytest>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<Id1Id2Key, DataKeytest>();
            id1Id3Map = new Config.KeyedList<Id1Id3Key, DataKeytest>();
            id2Map = new Config.KeyedList<long, DataKeytest>();
            id2Id3Map = new Config.KeyedList<Id2Id3Key, DataKeytest>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(new Id1Id2Key(self.Id1, self.Id2), self);
                id1Id3Map.Add(new Id1Id3Key(self.Id1, self.Id3), self);
                id2Map.Add(self.Id2, self);
                id2Id3Map.Add(new Id2Id3Key(self.Id2, self.Id3), self);
            }

        }

        internal static void Resolve(Config.LoadErrors errors)
        {
            foreach (var v in All())
                v._resolve(errors);
        }
        internal static DataKeytest _create(Config.Stream os)
        {
            var self = new DataKeytest();
            self.Id1 = os.ReadInt32();
            self.Id2 = os.ReadInt64();
            self.Id3 = os.ReadInt32();
            self.Ids = new List<int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.Ids.Add(os.ReadInt32());
            return self;
        }

        internal void _resolve(Config.LoadErrors errors)
        {
            RefIds = new List<Config.Other.DataSignin>();
            foreach(var e in Ids)
            {
                var r = Config.Other.DataSignin.Get(e);;
                if (r == null) errors.RefNull("other.keytest", ToString(), "ids");
                RefIds.Add(r);
            }
        }
    }
}