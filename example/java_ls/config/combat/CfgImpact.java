package config.combat;

public class CfgImpact {
    private int ordinal;
    private int damage;
    private CfgImpact() {
    }

    public CfgImpact(int ordinal, int damage) {
        this.ordinal = ordinal;
        this.damage = damage;
    }

    public static CfgImpact _create(configgen.genjava.ConfigInput input) {
        CfgImpact self = new CfgImpact();
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
        if (!(other instanceof CfgImpact o)) {
            return false;
        }
        return ordinal == o.ordinal && damage == o.damage;
    }

    @Override
    public String toString() {
        return "(" + ordinal + "," + damage + ")";
    }

}
