using System.Collections.Generic;
namespace Config.Equip;

public partial class DataRank
{
    public static DataRank White { get; private set; } = null!;
    public static DataRank Green { get; private set; } = null!;
    public static DataRank Blue { get; private set; } = null!;
    public static DataRank Purple { get; private set; } = null!;
    public static DataRank Yellow { get; private set; } = null!;
    public required int RankID { get; init; } /* 稀有度 */
    public required string RankName { get; init; } /* 程序用名字 */
    public required string RankShowName { get; init; } /* 显示名称 */

    public override int GetHashCode()
    {
        return RankID.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataRank;
        return o != null && RankID.Equals(o.RankID);
    }

    public override string ToString()
    {
        return "(" + RankID + "," + RankName + "," + RankShowName + ")";
    }

    
    private static OrderedDictionary<int, DataRank> _all = [];

    public static DataRank? Get(int rankID)
    {
        return _all.GetValueOrDefault(rankID);
    }

    public static IReadOnlyList<DataRank> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.RankID, self);
            if (self.RankName.Trim().Length == 0)
                continue;
            switch(self.RankName.Trim())
            {
                case "white":
                    if (White != null)
                        errors.EnumDup("equip.rank", self.ToString());
                    White = self;
                    break;
                case "green":
                    if (Green != null)
                        errors.EnumDup("equip.rank", self.ToString());
                    Green = self;
                    break;
                case "blue":
                    if (Blue != null)
                        errors.EnumDup("equip.rank", self.ToString());
                    Blue = self;
                    break;
                case "purple":
                    if (Purple != null)
                        errors.EnumDup("equip.rank", self.ToString());
                    Purple = self;
                    break;
                case "yellow":
                    if (Yellow != null)
                        errors.EnumDup("equip.rank", self.ToString());
                    Yellow = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.rank", self.ToString());
                    break;
            }
        }

        if (White == null)
            errors.EnumNull("equip.rank", "white");
        if (Green == null)
            errors.EnumNull("equip.rank", "green");
        if (Blue == null)
            errors.EnumNull("equip.rank", "blue");
        if (Purple == null)
            errors.EnumNull("equip.rank", "purple");
        if (Yellow == null)
            errors.EnumNull("equip.rank", "yellow");
    }

    internal static DataRank _create(Stream os)
    {
        var rankID = os.ReadInt32();
        var rankName = os.ReadStringInPool();
        var rankShowName = os.ReadStringInPool();
        return new DataRank {
            RankID = rankID,
            RankName = rankName,
            RankShowName = rankShowName,
        };
    }

}
