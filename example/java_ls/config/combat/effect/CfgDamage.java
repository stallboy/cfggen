package config.combat.effect;

public final class CfgDamage implements config.combat.effect.CfgEffect {
    private int amount;
    private CfgDamage() {
    }

    public CfgDamage(int amount) {
        this.amount = amount;
    }

    public static CfgDamage _create(configgen.genjava.ConfigInput input) {
        CfgDamage self = new CfgDamage();
        self.amount = input.readInt();
        return self;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(amount);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgDamage o)) {
            return false;
        }
        return amount == o.amount;
    }

    @Override
    public String toString() {
        return "CfgDamage(" + amount + ")";
    }

}
