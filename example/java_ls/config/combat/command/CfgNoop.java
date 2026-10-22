package config.combat.command;

public final class CfgNoop implements config.combat.command.CfgCommand {
    public CfgNoop() {
    }

    public static CfgNoop _create(configgen.genjava.ConfigInput input) {
        CfgNoop self = new CfgNoop();
        return self;
    }

    @Override
    public int hashCode() {
        return CfgNoop.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CfgNoop;
    }

    @Override
    public String toString() {
        return "CfgNoop";
    }

}
