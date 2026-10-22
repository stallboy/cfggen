package config.combat;

public class CfgStance {
    private boolean airborne;
    private config.combat.effect.CfgEffect effect;
    private CfgStance() {
    }

    public CfgStance(boolean airborne, config.combat.effect.CfgEffect effect) {
        this.airborne = airborne;
        this.effect = effect;
    }

    public static CfgStance _create(configgen.genjava.ConfigInput input) {
        CfgStance self = new CfgStance();
        self.airborne = input.readBool();
        self.effect = config.combat.effect.CfgEffect._create(input);
        return self;
    }

    public boolean getAirborne() {
        return airborne;
    }

    public config.combat.effect.CfgEffect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(airborne, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgStance o)) {
            return false;
        }
        return airborne == o.airborne && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + airborne + "," + effect + ")";
    }

}
