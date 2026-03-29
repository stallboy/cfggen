using System.Collections.Generic;
namespace Config.Equip;

public partial class DataEquipconfig
{
    public static DataEquipconfig Instance { get; private set; } = null!;
    public static DataEquipconfig Instance2 { get; private set; } = null!;
    public required string Entry { get; init; } /* 入口，程序填 */
    public required int Stone_count_for_set { get; init; } /* 形成套装的音石数量 */
    public required string Draw_protect_name { get; init; } /* 保底策略名称 */
    public required int Broadcastid { get; init; } /* 公告Id */
    public required int Broadcast_least_quality { get; init; } /* 公告的最低品质 */
    public required int Week_reward_mailid { get; init; } /* 抽卡周奖励的邮件id */

    public override int GetHashCode()
    {
        return Entry.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataEquipconfig;
        return o != null && Entry.Equals(o.Entry);
    }

    public override string ToString()
    {
        return "(" + Entry + "," + Stone_count_for_set + "," + Draw_protect_name + "," + Broadcastid + "," + Broadcast_least_quality + "," + Week_reward_mailid + ")";
    }

    
    private static OrderedDictionary<string, DataEquipconfig> _all = [];

    public static DataEquipconfig? Get(string entry)
    {
        return _all.GetValueOrDefault(entry);
    }

    public static IReadOnlyList<DataEquipconfig> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Entry, self);
            if (self.Entry.Trim().Length == 0)
                continue;
            switch(self.Entry.Trim())
            {
                case "Instance":
                    if (Instance != null)
                        errors.EnumDup("equip.equipconfig", self.ToString());
                    Instance = self;
                    break;
                case "Instance2":
                    if (Instance2 != null)
                        errors.EnumDup("equip.equipconfig", self.ToString());
                    Instance2 = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.equipconfig", self.ToString());
                    break;
            }
        }

        if (Instance == null)
            errors.EnumNull("equip.equipconfig", "Instance");
        if (Instance2 == null)
            errors.EnumNull("equip.equipconfig", "Instance2");
    }

    internal static DataEquipconfig _create(Stream os)
    {
        var entry = os.ReadStringInPool();
        var stone_count_for_set = os.ReadInt32();
        var draw_protect_name = os.ReadStringInPool();
        var broadcastid = os.ReadInt32();
        var broadcast_least_quality = os.ReadInt32();
        var week_reward_mailid = os.ReadInt32();
        return new DataEquipconfig {
            Entry = entry,
            Stone_count_for_set = stone_count_for_set,
            Draw_protect_name = draw_protect_name,
            Broadcastid = broadcastid,
            Broadcast_least_quality = broadcast_least_quality,
            Week_reward_mailid = week_reward_mailid,
        };
    }

}
