namespace Config.Other;

public partial class DKeytest
{
    public required int Id1 { get; init; }
    public required long Id2 { get; init; }
    public required int Id3 { get; init; }
    public required List<int> Ids { get; init; }
    public required string EnumTest { get; init; }
    public required List<string> EnumList { get; init; }
    public List<Other.DSignin> RefIds { get; private set; } = null!;
    public Other.DArgCaptureMode RefEnumTest { get; private set; }
    public List<Other.DArgCaptureMode> RefEnumList { get; private set; } = null!;
    
    class Id1Id2Key
    {
        readonly int Id1;
        readonly long Id2;
        public Id1Id2Key(int id1, long id2)
        {
            Id1 = id1;
            Id2 = id2;
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

    private static System.Collections.Frozen.FrozenDictionary<Id1Id2Key, DKeytest> _all = null!;

    public static DKeytest? Get(int id1, long id2)
    {
        return _all.GetValueOrDefault(new Id1Id2Key(id1, id2));
    }

    
    class Id1Id3Key
    {
        readonly int Id1;
        readonly int Id3;
        public Id1Id3Key(int id1, int id3)
        {
            Id1 = id1;
            Id3 = id3;
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

    private static System.Collections.Frozen.FrozenDictionary<Id1Id3Key, DKeytest> _id1Id3Map = null!;

    public static DKeytest? GetById1Id3(int id1, int id3)
    {
        return _id1Id3Map.GetValueOrDefault(new Id1Id3Key(id1, id3));
    }

    
    private static System.Collections.Frozen.FrozenDictionary<long, DKeytest> _id2Map = null!;

    public static DKeytest? GetById2(long id2)
    {
        return _id2Map.GetValueOrDefault(id2);
    }

    
    class Id2Id3Key
    {
        readonly long Id2;
        readonly int Id3;
        public Id2Id3Key(long id2, int id3)
        {
            Id2 = id2;
            Id3 = id3;
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

    private static System.Collections.Frozen.FrozenDictionary<Id2Id3Key, DKeytest> _id2Id3Map = null!;

    public static DKeytest? GetById2Id3(long id2, int id3)
    {
        return _id2Id3Map.GetValueOrDefault(new Id2Id3Key(id2, id3));
    }

    public static IReadOnlyList<DKeytest> All()
    {
        return _all.Values;
    }
}
