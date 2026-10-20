package config.task.completecondition;

public final class TalkNpc implements config.task.completecondition.Completecondition {
    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.TALK_NPC;
    }

    private int npcid;
    private TalkNpc() {
    }

    public TalkNpc(int npcid) {
        this.npcid = npcid;
    }

    public static TalkNpc _create(configgen.genjava.ConfigInput input) {
        TalkNpc self = new TalkNpc();
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
        if (!(other instanceof TalkNpc o))
            return false;
        return npcid == o.npcid;
    }

    @Override
    public String toString() {
        return "TalkNpc(" + npcid + ")";
    }

}
