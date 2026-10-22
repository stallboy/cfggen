package config.combat;

public class Marker {
    private String key;
    private Marker() {
    }

    public Marker(String key) {
        this.key = key;
    }

    public static Marker _create(configgen.genjava.ConfigInput input) {
        Marker self = new Marker();
        self.key = input.readStringInPool();
        return self;
    }

    public String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Marker o)) {
            return false;
        }
        return key.equals(o.key);
    }

    @Override
    public String toString() {
        return "(" + key + ")";
    }

}
