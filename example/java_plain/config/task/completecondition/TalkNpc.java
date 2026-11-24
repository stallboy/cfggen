package config.task.completecondition;

public final class TalkNpc implements config.task.completecondition.Completecondition {
    private final int npcid;

    public TalkNpc(int npcid) {
        this.npcid = npcid;
    }

    private TalkNpc(configgen.genjava.ConfigInput input) {
        this.npcid = input.readInt();
    }

    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.TALK_NPC;
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
        if (!(other instanceof TalkNpc o)) {
            return false;
        }
        return npcid == o.npcid;
    }

    @Override
    public String toString() {
        return "TalkNpc(" + npcid + ")";
    }

    public static TalkNpc _create(configgen.genjava.ConfigInput input) {
        return new TalkNpc(input);
    }

}
