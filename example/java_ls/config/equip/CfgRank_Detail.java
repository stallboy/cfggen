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

    public static java.util.List<@Nullable CfgRank_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipRank();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<Integer, CfgRank_Detail> rows, LoadValueErrs errors) {
        int c = rows.size();
        var equip_rank_All = new java.util.ArrayList<@Nullable CfgRank_Detail>(java.util.Collections.nCopies(c, null));
        var seq_RankID = new boolean[c];
        for (var self : rows.values()) {
            {
                int seq = self.getRankID();
                if (seq < 0 || seq >= c || seq_RankID[seq]) {
                    var primaryKey = self.rankID;
                    errors.invalidSeq("equip.rank", "RankID", primaryKey, seq, c, mgr.explain("equip.rank", primaryKey));
                } else {
                    seq_RankID[seq] = true;
                }
            }
            int key = self.rankID;
            if (key >= 0 && key < c) {
                equip_rank_All.set(key, self);
            }
        }
        mgr._set_equip_rank_All(equip_rank_All);
    }

}
