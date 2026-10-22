package config.combat;

public class Anchor {
    public Anchor() {
    }

    public static Anchor _create(configgen.genjava.ConfigInput input) {
        Anchor self = new Anchor();
        return self;
    }

    @Override
    public int hashCode() {
        return Anchor.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Anchor;
    }

    @Override
    public String toString() {
        return "";
    }

}
