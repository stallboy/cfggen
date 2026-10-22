package config.combat;

public class CfgBinding {
    private int abilityId;
    private CfgBinding() {
    }

    public CfgBinding(int abilityId) {
        this.abilityId = abilityId;
    }

    public static CfgBinding _create(configgen.genjava.ConfigInput input) {
        CfgBinding self = new CfgBinding();
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
        if (!(other instanceof CfgBinding o)) {
            return false;
        }
        return abilityId == o.abilityId;
    }

    @Override
    public String toString() {
        return "(" + abilityId + ")";
    }

}
