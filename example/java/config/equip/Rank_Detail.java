package config.equip;

public class Rank_Detail {
    private int rankID;
    private String rankName;
    private String rankShowName;

    private Rank_Detail() {
    }

    public static Rank_Detail _create(configgen.genjava.ConfigInput input) {
        Rank_Detail self = new Rank_Detail();
        self.rankID = input.readInt();
        self.rankName = input.readStr();
        self.rankShowName = input.readStr();
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

    public static Rank_Detail get(int rankID) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipRank(rankID);
    }

    public static java.util.Collection<Rank_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipRank();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Rank_Detail self = Rank_Detail._create(input);
                mgr.equip_rank_All.put(self.rankID, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
