package config.combat;

public class CfgPhaseVariant {
    private config.combat.CfgPhaseKey key;
    private config.combat.effect.CfgEffect effect;
    private CfgPhaseVariant() {
    }

    public CfgPhaseVariant(config.combat.CfgPhaseKey key, config.combat.effect.CfgEffect effect) {
        this.key = key;
        this.effect = effect;
    }

    public static CfgPhaseVariant _create(configgen.genjava.ConfigInput input) {
        CfgPhaseVariant self = new CfgPhaseVariant();
        self.key = config.combat.CfgPhaseKey._create(input);
        self.effect = config.combat.effect.CfgEffect._create(input);
        return self;
    }

    public config.combat.CfgPhaseKey getKey() {
        return key;
    }

    public config.combat.effect.CfgEffect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgPhaseVariant o)) {
            return false;
        }
        return key.equals(o.key) && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + key + "," + effect + ")";
    }

}
