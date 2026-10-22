package config.combat;

public class Stance {
    private boolean airborne;
    private config.combat.effect.Effect effect;
    private Stance() {
    }

    public Stance(boolean airborne, config.combat.effect.Effect effect) {
        this.airborne = airborne;
        this.effect = effect;
    }

    public static Stance _create(configgen.genjava.ConfigInput input) {
        Stance self = new Stance();
        self.airborne = input.readBool();
        self.effect = config.combat.effect.Effect._create(input);
        return self;
    }

    public boolean getAirborne() {
        return airborne;
    }

    public config.combat.effect.Effect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(airborne, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Stance o)) {
            return false;
        }
        return airborne == o.airborne && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + airborne + "," + effect + ")";
    }

}
