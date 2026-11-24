package config.equip;

// 来自：equip/equip_config.csv
public class EquipConfig {
    private final String entry;
    private final int stone_count_for_set;
    private final String draw_protect_name;
    private final int broadcastid;
    private final int broadcast_least_quality;
    private final int week_reward_mailid;

    private EquipConfig(configgen.genjava.ConfigInput input) {
        this.entry = input.readStringInPool();
        this.stone_count_for_set = input.readInt();
        this.draw_protect_name = input.readStringInPool();
        this.broadcastid = input.readInt();
        this.broadcast_least_quality = input.readInt();
        this.week_reward_mailid = input.readInt();
    }

    /**
     * 入口，程序填
     */
    public String getEntry() {
        return entry;
    }

    /**
     * 形成套装的音石数量
     */
    public int getStone_count_for_set() {
        return stone_count_for_set;
    }

    /**
     * 保底策略名称
     */
    public String getDraw_protect_name() {
        return draw_protect_name;
    }

    /**
     * 公告Id
     */
    public int getBroadcastid() {
        return broadcastid;
    }

    /**
     * 公告的最低品质
     */
    public int getBroadcast_least_quality() {
        return broadcast_least_quality;
    }

    /**
     * 抽卡周奖励的邮件id
     */
    public int getWeek_reward_mailid() {
        return week_reward_mailid;
    }

    @Override
    public String toString() {
        return "(" + entry + "," + stone_count_for_set + "," + draw_protect_name + "," + broadcastid + "," + broadcast_least_quality + "," + week_reward_mailid + ")";
    }

    public static EquipConfig _create(configgen.genjava.ConfigInput input) {
        return new EquipConfig(input);
    }

    public static EquipConfig get(String entry) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipEquipConfig(entry);
    }

    public static java.util.Collection<EquipConfig> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipEquipConfig();
    }


}
