package config.combat.effect;

public final class CfgNone implements config.combat.effect.CfgEffect {
    public CfgNone() {
    }

    public static CfgNone _create(configgen.genjava.ConfigInput input) {
        CfgNone self = new CfgNone();
        return self;
    }

    @Override
    public int hashCode() {
        return CfgNone.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CfgNone;
    }

    @Override
    public String toString() {
        return "CfgNone";
    }

}
