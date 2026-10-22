package config.combat;

public class Exit {
    private config.combat.effect.Effect effect;
    private Exit() {
    }

    public Exit(config.combat.effect.Effect effect) {
        this.effect = effect;
    }

    public static Exit _create(configgen.genjava.ConfigInput input) {
        Exit self = new Exit();
        self.effect = config.combat.effect.Effect._create(input);
        return self;
    }

    public config.combat.effect.Effect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Exit o)) {
            return false;
        }
        return effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + effect + ")";
    }

}
