package config.task.completecondition;

import org.jspecify.annotations.Nullable;

public final class Chat implements config.task.completecondition.Completecondition {
    private final String msg;

    public Chat(String msg) {
        this.msg = msg;
    }

    private Chat(configgen.genjava.ConfigInput input) {
        this.msg = input.readStringInPool();
    }

    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.CHAT;
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
        if (!(other instanceof Chat o)) {
            return false;
        }
        return msg.equals(o.msg);
    }

    @Override
    public String toString() {
        return "Chat(" + msg + ")";
    }

    public static Chat _create(configgen.genjava.ConfigInput input) {
        return new Chat(input);
    }

}
