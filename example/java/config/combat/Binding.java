package config.combat;

public class Binding {
    private int abilityId;
    private Binding() {
    }

    public Binding(int abilityId) {
        this.abilityId = abilityId;
    }

    public static Binding _create(configgen.genjava.ConfigInput input) {
        Binding self = new Binding();
        self.abilityId = input.readInt();
        return self;
    }

    public int getAbilityId() {
        return abilityId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(abilityId);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Binding o)) {
            return false;
        }
        return abilityId == o.abilityId;
    }

    @Override
    public String toString() {
        return "(" + abilityId + ")";
    }

}
