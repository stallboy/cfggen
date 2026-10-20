using System.Collections.Generic;
namespace Config.Other
{

public partial class DKeyTest
{
    public int Id1 { get; init; }
    public long Id2 { get; init; }
    public int Id3 { get; init; }
    public IReadOnlyList<int> Ids { get; init; } = null!;
    public string EnumTest { get; init; } = null!;
    public IReadOnlyList<string> EnumList { get; init; } = null!;
    public IReadOnlyList<Other.DSignIn> RefIds { get; private set; } = null!;
    public Other.DArgCaptureMode RefEnumTest { get; private set; }
    public IReadOnlyList<Other.DArgCaptureMode> RefEnumList { get; private set; } = null!;

    public sealed record Id1Id2Key(int Id1, long Id2);

    public static DKeyTest? Get(int id1, long id2)
        => Get(ConfigMgr.GetMgr(), id1, id2);

    public static DKeyTest? Get(ConfigMgr config, int id1, long id2)
    {
        var index = config._other_dkey_utest_All;
        return index.TryGetValue(new Id1Id2Key(id1, id2), out var result) ? result : null;
    }


    public sealed record Id1Id3Key(int Id1, int Id3);

    public static DKeyTest? GetById1Id3(int id1, int id3)
        => GetById1Id3(ConfigMgr.GetMgr(), id1, id3);

    public static DKeyTest? GetById1Id3(ConfigMgr config, int id1, int id3)
    {
        var index = config._other_dkey_utest_Id1Id3Map;
        return index.TryGetValue(new Id1Id3Key(id1, id3), out var result) ? result : null;
    }


    public static DKeyTest? GetById2(long id2)
        => GetById2(ConfigMgr.GetMgr(), id2);

    public static DKeyTest? GetById2(ConfigMgr config, long id2)
    {
        var index = config._other_dkey_utest_Id2Map;
        return index.TryGetValue(id2, out var result) ? result : null;
    }


    public sealed record Id2Id3Key(long Id2, int Id3);

    public static DKeyTest? GetById2Id3(long id2, int id3)
        => GetById2Id3(ConfigMgr.GetMgr(), id2, id3);

    public static DKeyTest? GetById2Id3(ConfigMgr config, long id2, int id3)
    {
        var index = config._other_dkey_utest_Id2Id3Map;
        return index.TryGetValue(new Id2Id3Key(id2, id3), out var result) ? result : null;
    }

    public static IReadOnlyList<DKeyTest> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DKeyTest> All(ConfigMgr config)
        => config._other_dkey_utest_Rows;
}
}
