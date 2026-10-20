using System.Collections.Generic;
namespace Config.Equip
{

public partial class DEquipconfig
{
    public static DEquipconfig Instance => InstanceIn(ConfigMgr.GetMgr());
    public static DEquipconfig InstanceIn(ConfigMgr config) => config._equip_dequipconfig_Symbol_Instance!;
    public static DEquipconfig Instance2 => Instance2In(ConfigMgr.GetMgr());
    public static DEquipconfig Instance2In(ConfigMgr config) => config._equip_dequipconfig_Symbol_Instance2!;

    public string Entry { get; init; } = null!; /* 入口，程序填 */
    public int Stone_count_for_set { get; init; } /* 形成套装的音石数量 */
    public string Draw_protect_name { get; init; } = null!; /* 保底策略名称 */
    public int Broadcastid { get; init; } /* 公告Id */
    public int Broadcast_least_quality { get; init; } /* 公告的最低品质 */
    public int Week_reward_mailid { get; init; } /* 抽卡周奖励的邮件id */

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
}
