package config.combat;

public class CfgExit {
    private config.combat.effect.CfgEffect effect;
    private CfgExit() {
    }

    public CfgExit(config.combat.effect.CfgEffect effect) {
        this.effect = effect;
    }

    public static CfgExit _create(configgen.genjava.ConfigInput input) {
        CfgExit self = new CfgExit();
        self.effect = config.combat.effect.CfgEffect._create(input);
        return self;
    }

    public config.combat.effect.CfgEffect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgExit o)) {
            return false;
        }
        return effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + effect + ")";
    }

}
