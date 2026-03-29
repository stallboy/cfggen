using System.Collections.Generic;
namespace Config.Other;

public partial class DataKeytest
{
    public required int Id1 { get; init; }
    public required long Id2 { get; init; }
    public required int Id3 { get; init; }
    public required List<int> Ids { get; init; }
    public required string EnumTest { get; init; }
    public required List<string> EnumList { get; init; }
    public List<Other.DataSignin> RefIds { get; private set; } = null!;
    public Other.DataArgCaptureMode RefEnumTest { get; private set; } = null!;
    public List<Other.DataArgCaptureMode> RefEnumList { get; private set; } = null!;

    public override int GetHashCode()
    {
        return Id1.GetHashCode() + Id2.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataKeytest;
        return o != null && Id1.Equals(o.Id1) && Id2.Equals(o.Id2);
    }

    public override string ToString()
    {
        return "(" + Id1 + "," + Id2 + "," + Id3 + "," + StringUtil.ToString(Ids) + "," + EnumTest + "," + StringUtil.ToString(EnumList) + ")";
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

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as Id1Id2Key;
            return o != null && Id1.Equals(o.Id1) && Id2.Equals(o.Id2);
        }
    }

    private static OrderedDictionary<Id1Id2Key, DataKeytest> _all = [];

    public static DataKeytest? Get(int id1, long id2)
    {
        return _all.GetValueOrDefault(new Id1Id2Key(id1, id2));
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

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as Id1Id3Key;
            return o != null && Id1.Equals(o.Id1) && Id3.Equals(o.Id3);
        }
    }

    private static OrderedDictionary<Id1Id3Key, DataKeytest> _id1Id3Map = [];

    public static DataKeytest? GetById1Id3(int id1, int id3)
    {
        return _id1Id3Map.GetValueOrDefault(new Id1Id3Key(id1, id3));
    }

    
    private static OrderedDictionary<long, DataKeytest> _id2Map = [];

    public static DataKeytest? GetById2(long id2)
    {
        return _id2Map.GetValueOrDefault(id2);
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

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as Id2Id3Key;
            return o != null && Id2.Equals(o.Id2) && Id3.Equals(o.Id3);
        }
    }

    private static OrderedDictionary<Id2Id3Key, DataKeytest> _id2Id3Map = [];

    public static DataKeytest? GetById2Id3(long id2, int id3)
    {
        return _id2Id3Map.GetValueOrDefault(new Id2Id3Key(id2, id3));
    }

    public static IReadOnlyList<DataKeytest> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        _id1Id3Map = [];
        _id2Map = [];
        _id2Id3Map = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(new Id1Id2Key(self.Id1, self.Id2), self);
            _id1Id3Map.Add(new Id1Id3Key(self.Id1, self.Id3), self);
            _id2Map.Add(self.Id2, self);
            _id2Id3Map.Add(new Id2Id3Key(self.Id2, self.Id3), self);
        }

    }

    internal static void Resolve(LoadErrors errors)
    {
        foreach (var v in All())
            v._resolve(errors);
    }
    internal static DataKeytest _create(Stream os)
    {
        var id1 = os.ReadInt32();
        var id2 = os.ReadInt64();
        var id3 = os.ReadInt32();
        List<int> ids = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            ids.Add(os.ReadInt32());
        var enumTest = os.ReadStringInPool();
        List<string> enumList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            enumList.Add(os.ReadStringInPool());
        return new DataKeytest {
            Id1 = id1,
            Id2 = id2,
            Id3 = id3,
            Ids = ids,
            EnumTest = enumTest,
            EnumList = enumList,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        RefIds = [];
        foreach(var e in Ids)
        {
            var r = Other.DataSignin.Get(e);
            if (r == null) errors.RefNull("other.keytest", ToString(), "ids");
            else RefIds.Add(r);
        }
        RefEnumTest = Other.DataArgCaptureMode.Get(EnumTest)!;
        if (RefEnumTest == null) errors.RefNull("other.keytest", ToString(), "enumTest");
        RefEnumList = [];
        foreach(var e in EnumList)
        {
            var r = Other.DataArgCaptureMode.Get(e);
            if (r == null) errors.RefNull("other.keytest", ToString(), "enumList");
            else RefEnumList.Add(r);
        }
    }
}
