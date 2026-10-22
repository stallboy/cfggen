package config.combat;

public class CfgReward {
    private int count;
    private CfgReward() {
    }

    public CfgReward(int count) {
        this.count = count;
    }

    public static CfgReward _create(configgen.genjava.ConfigInput input) {
        CfgReward self = new CfgReward();
        self.count = input.readInt();
        return self;
    }

    public int getCount() {
        return count;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(count);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgReward o)) {
            return false;
        }
        return count == o.count;
    }

    @Override
    public String toString() {
        return "(" + count + ")";
    }

}
