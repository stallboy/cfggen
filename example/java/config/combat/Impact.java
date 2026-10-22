package config.combat;

public class Impact {
    private int ordinal;
    private int damage;
    private Impact() {
    }

    public Impact(int ordinal, int damage) {
        this.ordinal = ordinal;
        this.damage = damage;
    }

    public static Impact _create(configgen.genjava.ConfigInput input) {
        Impact self = new Impact();
        self.ordinal = input.readInt();
        self.damage = input.readInt();
        return self;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ordinal, damage);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Impact o)) {
            return false;
        }
        return ordinal == o.ordinal && damage == o.damage;
    }

    @Override
    public String toString() {
        return "(" + ordinal + "," + damage + ")";
    }

}
