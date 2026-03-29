using System.Collections.Generic;
namespace Config.Other;

public partial class DataSignin
{
    public required int Id { get; init; } /* 礼包ID */
    public required OrderedDictionary<int, int> Item2countMap { get; init; } /* 普通奖励 */
    public required OrderedDictionary<int, int> Vipitem2vipcountMap { get; init; } /* vip奖励 */
    public required int Viplevel { get; init; } /* 领取vip奖励的最低等级 */
    public required string IconFile { get; init; } /* 礼包图标 */
    public OrderedDictionary<int, Other.DataLoot> RefVipitem2vipcountMap { get; private set; } = null!;

    public override int GetHashCode()
    {
        return Id.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataSignin;
        return o != null && Id.Equals(o.Id);
    }

    public override string ToString()
    {
        return "(" + Id + "," + Item2countMap + "," + Vipitem2vipcountMap + "," + Viplevel + "," + IconFile + ")";
    }

    
    private static OrderedDictionary<int, DataSignin> _all = [];

    public static DataSignin? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataSignin> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Id, self);
        }

    }

    internal static void Resolve(LoadErrors errors)
    {
        foreach (var v in All())
            v._resolve(errors);
    }
    internal static DataSignin _create(Stream os)
    {
        var id = os.ReadInt32();
        OrderedDictionary<int, int> item2countMap = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            item2countMap.Add(os.ReadInt32(), os.ReadInt32());
        }
        OrderedDictionary<int, int> vipitem2vipcountMap = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            vipitem2vipcountMap.Add(os.ReadInt32(), os.ReadInt32());
        }
        var viplevel = os.ReadInt32();
        var iconFile = os.ReadStringInPool();
        return new DataSignin {
            Id = id,
            Item2countMap = item2countMap,
            Vipitem2vipcountMap = vipitem2vipcountMap,
            Viplevel = viplevel,
            IconFile = iconFile,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        RefVipitem2vipcountMap = [];
        foreach(var kv in Vipitem2vipcountMap)
        {
            var k = kv.Key;
            var v = Other.DataLoot.Get(kv.Value);
            if (v == null) errors.RefNull("other.signin", ToString(), "vipitem2vipcountMap");
            else RefVipitem2vipcountMap.Add(k, v);
        }
    }
}
