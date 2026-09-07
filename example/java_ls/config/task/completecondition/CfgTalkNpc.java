package config.task.completecondition;

public final class CfgTalkNpc implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgCompleteconditiontype type() {
        return config.task.CfgCompleteconditiontype.TALKNPC;
    }

    private int npcid;

    private CfgTalkNpc() {
    }

    public CfgTalkNpc(int npcid) {
        this.npcid = npcid;
    }

    public static CfgTalkNpc _create(configgen.genjava.ConfigInput input) {
        CfgTalkNpc self = new CfgTalkNpc();
        self.npcid = input.readInt();
        return self;
    }

    public int getNpcid() {
        return npcid;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(npcid);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgTalkNpc))
            return false;
        CfgTalkNpc o = (CfgTalkNpc) other;
        return npcid == o.npcid;
    }

    @Override
    public String toString() {
        return "CfgTalkNpc(" + npcid + ")";
    }

}
