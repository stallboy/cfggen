package config.equip;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：equip/rank.csv
public class CfgRank_Detail {
    private final int rankID;
    private final String rankName;
    private final String rankShowName;

    private CfgRank_Detail(configgen.genjava.ConfigInput input) {
        this.rankID = input.readInt();
        this.rankName = input.readStringInPool();
        this.rankShowName = input.readStringInPool();
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

    public static CfgRank_Detail _create(configgen.genjava.ConfigInput input) {
        return new CfgRank_Detail(input);
    }

    public static @Nullable CfgRank_Detail get(int rankID) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipRank(rankID);
    }

    public static java.util.List<CfgRank_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipRank();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<Integer, CfgRank_Detail> rows, LoadValueErrs errors) {
        int c = rows.size();
        boolean validSeq = true;
        var equip_rank_All = new CfgRank_Detail[c];
        for (var self : rows.values()) {
            {
                int seq = self.getRankID();
                //noinspection ConstantValue
                if (seq < 0 || seq >= c || equip_rank_All[seq] != null) {
                    var primaryKey = self.rankID;
                    errors.invalidSeq("equip.rank", "RankID", primaryKey, seq, c, mgr.explain("equip.rank", primaryKey));
                    validSeq = false;
                } else {
                    equip_rank_All[seq] = self;
                }
            }
        }
        if (!validSeq) {
            return;
        }
        mgr._set_equip_rank_All(java.util.Arrays.asList(equip_rank_All));
    }

}
