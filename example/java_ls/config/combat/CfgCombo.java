package config.combat;

public class CfgCombo {
    private String weapon;
    private int tier;
    private boolean airborne;
    private config.combat.effect.CfgEffect effect;
    private CfgCombo() {
    }

    public CfgCombo(String weapon, int tier, boolean airborne, config.combat.effect.CfgEffect effect) {
        this.weapon = weapon;
        this.tier = tier;
        this.airborne = airborne;
        this.effect = effect;
    }

    public static CfgCombo _create(configgen.genjava.ConfigInput input) {
        CfgCombo self = new CfgCombo();
        self.weapon = input.readStringInPool();
        self.tier = input.readInt();
        self.airborne = input.readBool();
        self.effect = config.combat.effect.CfgEffect._create(input);
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

    public config.combat.effect.CfgEffect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(weapon, tier, airborne, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgCombo o)) {
            return false;
        }
        return weapon.equals(o.weapon) && tier == o.tier && airborne == o.airborne && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + weapon + "," + tier + "," + airborne + "," + effect + ")";
    }

}
