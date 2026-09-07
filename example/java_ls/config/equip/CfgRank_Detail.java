package config.equip;

// 来自：equip/rank.csv
public class CfgRank_Detail {
    private int rankID;
    private String rankName;
    private String rankShowName;

    private CfgRank_Detail() {
    }

    public static CfgRank_Detail _create(configgen.genjava.ConfigInput input) {
        CfgRank_Detail self = new CfgRank_Detail();
        self.rankID = input.readInt();
        self.rankName = input.readStringInPool();
        self.rankShowName = input.readStringInPool();
        return self;
    }

    /**
     * 稀有度
     */
    public int getRankID() {
        return rankID;
    }

    /**
     * 程序用名字
     */
    public String getRankName() {
        return rankName;
    }

    /**
     * 显示名称
     */
    public String getRankShowName() {
        return rankShowName;
    }

    @Override
    public String toString() {
        return "(" + rankID + "," + rankName + "," + rankShowName + ")";
    }

    public static CfgRank_Detail get(int rankID) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipRank(rankID);
    }

    public static java.util.List<CfgRank_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipRank();
    }
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.equip_rank_All = new CfgRank_Detail[c];
            for (; c > 0; c--) {
                CfgRank_Detail self = CfgRank_Detail._create(input);
                mgr.equip_rank_All[self.rankID] = self;
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
