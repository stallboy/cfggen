package config.task.completecondition;

public final class CfgChat implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgCompleteconditiontype type() {
        return config.task.CfgCompleteconditiontype.CHAT;
    }

    private String msg;

    private CfgChat() {
    }

    public CfgChat(String msg) {
        this.msg = msg;
    }

    public static CfgChat _create(configgen.genjava.ConfigInput input) {
        CfgChat self = new CfgChat();
        self.msg = input.readStringInPool();
        return self;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(msg);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgChat))
            return false;
        CfgChat o = (CfgChat) other;
        return msg.equals(o.msg);
    }

    @Override
    public String toString() {
        return "CfgChat(" + msg + ")";
    }

}
