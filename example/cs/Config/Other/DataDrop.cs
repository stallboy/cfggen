using System.Collections.Generic;
namespace Config.Other;

public partial class DataDrop
{
    public required int Dropid { get; init; } /* 序号 */
    public required string Name { get; init; } /* 名字 */
    public required List<Other.DataDropItem> Items { get; init; } /* 掉落概率 */
    public required OrderedDictionary<int, int> Testmap { get; init; } /* 测试map block */

    public override int GetHashCode()
    {
        return Dropid.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataDrop;
        return o != null && Dropid.Equals(o.Dropid);
    }

    public override string ToString()
    {
        return "(" + Dropid + "," + Name + "," + StringUtil.ToString(Items) + "," + Testmap + ")";
    }

    
    private static OrderedDictionary<int, DataDrop> _all = [];

    public static DataDrop? Get(int dropid)
    {
        return _all.GetValueOrDefault(dropid);
    }

    public static IReadOnlyList<DataDrop> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Dropid, self);
        }

    }

    internal static DataDrop _create(Stream os)
    {
        var dropid = os.ReadInt32();
        var name = os.ReadTextInPool();
        List<Other.DataDropItem> items = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            items.Add(Other.DataDropItem._create(os));
        OrderedDictionary<int, int> testmap = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            testmap.Add(os.ReadInt32(), os.ReadInt32());
        }
        return new DataDrop {
            Dropid = dropid,
            Name = name,
            Items = items,
            Testmap = testmap,
        };
    }

}
