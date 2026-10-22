package config.combat.effect;

public final class Heal implements config.combat.effect.Effect {
    private int amount;
    private Heal() {
    }

    public Heal(int amount) {
        this.amount = amount;
    }

    public static Heal _create(configgen.genjava.ConfigInput input) {
        Heal self = new Heal();
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
        if (!(other instanceof Heal o)) {
            return false;
        }
        return amount == o.amount;
    }

    @Override
    public String toString() {
        return "Heal(" + amount + ")";
    }

}
