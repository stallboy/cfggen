package config.combat;

public class Combo {
    private String weapon;
    private int tier;
    private boolean airborne;
    private config.combat.effect.Effect effect;
    private Combo() {
    }

    public Combo(String weapon, int tier, boolean airborne, config.combat.effect.Effect effect) {
        this.weapon = weapon;
        this.tier = tier;
        this.airborne = airborne;
        this.effect = effect;
    }

    public static Combo _create(configgen.genjava.ConfigInput input) {
        Combo self = new Combo();
        self.weapon = input.readStringInPool();
        self.tier = input.readInt();
        self.airborne = input.readBool();
        self.effect = config.combat.effect.Effect._create(input);
        return self;
    }

    public String getWeapon() {
        return weapon;
    }

    public int getTier() {
        return tier;
    }

    public boolean getAirborne() {
        return airborne;
    }

    public config.combat.effect.Effect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(weapon, tier, airborne, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Combo o)) {
            return false;
        }
        return weapon.equals(o.weapon) && tier == o.tier && airborne == o.airborne && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + weapon + "," + tier + "," + airborne + "," + effect + ")";
    }

}
