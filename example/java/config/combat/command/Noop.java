package config.combat.command;

public final class Noop implements config.combat.command.Command {
    public Noop() {
    }

    public static Noop _create(configgen.genjava.ConfigInput input) {
        Noop self = new Noop();
        return self;
    }

    @Override
    public int hashCode() {
        return Noop.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Noop;
    }

    @Override
    public String toString() {
        return "Noop";
    }

}
