package config.combat;

public class PhaseVariant {
    private config.combat.PhaseKey key;
    private config.combat.effect.Effect effect;
    private PhaseVariant() {
    }

    public PhaseVariant(config.combat.PhaseKey key, config.combat.effect.Effect effect) {
        this.key = key;
        this.effect = effect;
    }

    public static PhaseVariant _create(configgen.genjava.ConfigInput input) {
        PhaseVariant self = new PhaseVariant();
        self.key = config.combat.PhaseKey._create(input);
        self.effect = config.combat.effect.Effect._create(input);
        return self;
    }

    public config.combat.PhaseKey getKey() {
        return key;
    }

    public config.combat.effect.Effect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PhaseVariant o)) {
            return false;
        }
        return key.equals(o.key) && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + key + "," + effect + ")";
    }

}
