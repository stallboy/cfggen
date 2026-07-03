using System;
using System.Collections.Generic;
namespace Config.Equip
{

public partial class DEquipconfig
{
    public static DEquipconfig Instance { get; private set; } = null!;
    public static DEquipconfig Instance2 { get; private set; } = null!;

    public string Entry { get; init; } = null!; /* 入口，程序填 */
    public int Stone_count_for_set { get; init; } /* 形成套装的音石数量 */
    public string Draw_protect_name { get; init; } = null!; /* 保底策略名称 */
    public int Broadcastid { get; init; } /* 公告Id */
    public int Broadcast_least_quality { get; init; } /* 公告的最低品质 */
    public int Week_reward_mailid { get; init; } /* 抽卡周奖励的邮件id */
    private static IReadOnlyList<DEquipconfig> _allList = null!;
    
    private static Dictionary<string, DEquipconfig> _all = null!;

    public static DEquipconfig? Get(string entry)
    {
        return _all.GetValueOrDefault(entry);
    }

    public static IReadOnlyList<DEquipconfig> All()
    {
        return _allList;
    }
}
}
