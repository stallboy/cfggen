package config.task.completecondition;

import org.jspecify.annotations.Nullable;

public final class CfgChat implements config.task.completecondition.CfgCompletecondition {
    private final String msg;

    public CfgChat(String msg) {
        this.msg = msg;
    }

    private CfgChat(configgen.genjava.ConfigInput input) {
        this.msg = input.readStringInPool();
    }

    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.CHAT;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(msg);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgChat o)) {
            return false;
        }
        return msg.equals(o.msg);
    }

    @Override
    public String toString() {
        return "CfgChat(" + msg + ")";
    }

    public static CfgChat _create(configgen.genjava.ConfigInput input) {
        return new CfgChat(input);
    }

}
