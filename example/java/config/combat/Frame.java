package config.combat;

public class Frame {
    private long tick;
    private config.combat.effect.Effect effect;
    private Frame() {
    }

    public Frame(long tick, config.combat.effect.Effect effect) {
        this.tick = tick;
        this.effect = effect;
    }

    public static Frame _create(configgen.genjava.ConfigInput input) {
        Frame self = new Frame();
        self.tick = input.readLong();
        self.effect = config.combat.effect.Effect._create(input);
        return self;
    }

    public long getTick() {
        return tick;
    }

    public config.combat.effect.Effect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(tick, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Frame o)) {
            return false;
        }
        return tick == o.tick && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + tick + "," + effect + ")";
    }

}
