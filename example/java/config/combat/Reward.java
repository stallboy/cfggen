package config.combat;

public class Reward {
    private int count;
    private Reward() {
    }

    public Reward(int count) {
        this.count = count;
    }

    public static Reward _create(configgen.genjava.ConfigInput input) {
        Reward self = new Reward();
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
        if (!(other instanceof Reward o)) {
            return false;
        }
        return count == o.count;
    }

    @Override
    public String toString() {
        return "(" + count + ")";
    }

}
