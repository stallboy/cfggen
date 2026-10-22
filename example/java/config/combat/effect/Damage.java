package config.combat.effect;

public final class Damage implements config.combat.effect.Effect {
    private int amount;
    private Damage() {
    }

    public Damage(int amount) {
        this.amount = amount;
    }

    public static Damage _create(configgen.genjava.ConfigInput input) {
        Damage self = new Damage();
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
        if (!(other instanceof Damage o)) {
            return false;
        }
        return amount == o.amount;
    }

    @Override
    public String toString() {
        return "Damage(" + amount + ")";
    }

}
