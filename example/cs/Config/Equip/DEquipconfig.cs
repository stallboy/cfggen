namespace Config.Equip;

public partial class DEquipconfig
{
    public static DEquipconfig Instance { get; private set; } = null!;
    public static DEquipconfig Instance2 { get; private set; } = null!;

    public required string Entry { get; init; } /* 入口，程序填 */
    public required int Stone_count_for_set { get; init; } /* 形成套装的音石数量 */
    public required string Draw_protect_name { get; init; } /* 保底策略名称 */
    public required int Broadcastid { get; init; } /* 公告Id */
    public required int Broadcast_least_quality { get; init; } /* 公告的最低品质 */
    public required int Week_reward_mailid { get; init; } /* 抽卡周奖励的邮件id */
    
    private static OrderedDictionary<string, DEquipconfig> _all = [];

    public static DEquipconfig? Get(string entry)
    {
        return _all.GetValueOrDefault(entry);
    }

    public static IReadOnlyList<DEquipconfig> All()
    {
        return _all.Values;
    }
}
