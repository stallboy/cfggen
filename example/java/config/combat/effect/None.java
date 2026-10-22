package config.combat.effect;

public final class None implements config.combat.effect.Effect {
    public None() {
    }

    public static None _create(configgen.genjava.ConfigInput input) {
        None self = new None();
        return self;
    }

    @Override
    public int hashCode() {
        return None.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof None;
    }

    @Override
    public String toString() {
        return "None";
    }

}
