namespace Config.Equip;

public partial class DEquipconfig
{
    public static DEquipconfig Instance => InstanceIn(ConfigMgr.GetMgr());
    public static DEquipconfig InstanceIn(ConfigMgr config) => config._equip_dequipconfig_Symbol_Instance!;
    public static DEquipconfig Instance2 => Instance2In(ConfigMgr.GetMgr());
    public static DEquipconfig Instance2In(ConfigMgr config) => config._equip_dequipconfig_Symbol_Instance2!;

    public required string Entry { get; init; } /* 入口，程序填 */
    public required int Stone_count_for_set { get; init; } /* 形成套装的音石数量 */
    public required string Draw_protect_name { get; init; } /* 保底策略名称 */
    public required int Broadcastid { get; init; } /* 公告Id */
    public required int Broadcast_least_quality { get; init; } /* 公告的最低品质 */
    public required int Week_reward_mailid { get; init; } /* 抽卡周奖励的邮件id */

    public static DEquipconfig? Get(string entry)
        => Get(ConfigMgr.GetMgr(), entry);

    public static DEquipconfig? Get(ConfigMgr config, string entry)
    {
        var index = config._equip_dequipconfig_All;
        return index.TryGetValue(entry, out var result) ? result : null;
    }

    public static IReadOnlyList<DEquipconfig> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DEquipconfig> All(ConfigMgr config)
        => config._equip_dequipconfig_Rows;
}
