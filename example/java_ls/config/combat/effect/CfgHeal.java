package config.combat.effect;

public final class CfgHeal implements config.combat.effect.CfgEffect {
    private int amount;
    private CfgHeal() {
    }

    public CfgHeal(int amount) {
        this.amount = amount;
    }

    public static CfgHeal _create(configgen.genjava.ConfigInput input) {
        CfgHeal self = new CfgHeal();
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
        if (!(other instanceof CfgHeal o)) {
            return false;
        }
        return amount == o.amount;
    }

    @Override
    public String toString() {
        return "CfgHeal(" + amount + ")";
    }

}
