using System.Collections.Generic;
namespace Config.Equip;

public partial class DataJewelryrandom
{
    public required DataLevelRank LvlRank { get; init; } /* 等级 */
    public required DataRange AttackRange { get; init; } /* 最小攻击力 */
    public required List<DataRange> OtherRange { get; init; } /* 最小防御力 */
    public required List<Equip.DataTestPackBean> TestPack { get; init; } /* 测试pack */

    public override int GetHashCode()
    {
        return LvlRank.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataJewelryrandom;
        return o != null && LvlRank.Equals(o.LvlRank);
    }

    public override string ToString()
    {
        return "(" + LvlRank + "," + AttackRange + "," + StringUtil.ToString(OtherRange) + "," + StringUtil.ToString(TestPack) + ")";
    }

    
    private static OrderedDictionary<DataLevelRank, DataJewelryrandom> _all = [];

    public static DataJewelryrandom? Get(DataLevelRank lvlRank)
    {
        return _all.GetValueOrDefault(lvlRank);
    }

    public static IReadOnlyList<DataJewelryrandom> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.LvlRank, self);
        }

    }

    internal static void Resolve(LoadErrors errors)
    {
        foreach (var v in All())
            v._resolve(errors);
    }
    internal static DataJewelryrandom _create(Stream os)
    {
        var lvlRank = DataLevelRank._create(os);
        var attackRange = DataRange._create(os);
        List<DataRange> otherRange = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            otherRange.Add(DataRange._create(os));
        List<Equip.DataTestPackBean> testPack = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            testPack.Add(Equip.DataTestPackBean._create(os));
        return new DataJewelryrandom {
            LvlRank = lvlRank,
            AttackRange = attackRange,
            OtherRange = otherRange,
            TestPack = testPack,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        LvlRank._resolve(errors);
    }
}
