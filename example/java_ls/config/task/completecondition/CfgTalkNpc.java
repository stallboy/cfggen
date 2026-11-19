package config.task.completecondition;

import org.jspecify.annotations.Nullable;

public final class CfgTalkNpc implements config.task.completecondition.CfgCompletecondition {
    private final int npcid;

    public CfgTalkNpc(int npcid) {
        this.npcid = npcid;
    }

    private CfgTalkNpc(configgen.genjava.ConfigInput input) {
        this.npcid = input.readInt();
    }

    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.TALKNPC;
    }

    public int getNpcid() {
        return npcid;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(npcid);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgTalkNpc o)) {
            return false;
        }
        return npcid == o.npcid;
    }

    @Override
    public String toString() {
        return "CfgTalkNpc(" + npcid + ")";
    }

    public static CfgTalkNpc _create(configgen.genjava.ConfigInput input) {
        return new CfgTalkNpc(input);
    }

}
