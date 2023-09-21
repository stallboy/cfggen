package config.equip;

public class Equipconfig {
    private String entry;
    private int stone_count_for_set;
    private String draw_protect_name;
    private int broadcastid;
    private int broadcast_least_quality;
    private int week_reward_mailid;

    private Equipconfig() {
    }

    public static Equipconfig _create(configgen.genjava.ConfigInput input) {
        Equipconfig self = new Equipconfig();
        self.entry = input.readStr();
        self.stone_count_for_set = input.readInt();
        self.draw_protect_name = input.readStr();
        self.broadcastid = input.readInt();
        self.broadcast_least_quality = input.readInt();
        self.week_reward_mailid = input.readInt();
        return self;
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

    public static Equipconfig get(String entry) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.equip_equipconfig_All.get(entry);
    }

    public static java.util.Collection<Equipconfig> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.equip_equipconfig_All.values();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Equipconfig self = Equipconfig._create(input);
                mgr.equip_equipconfig_All.put(self.entry, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
