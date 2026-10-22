package config.combat;

public class CfgFrame {
    private long tick;
    private config.combat.effect.CfgEffect effect;
    private CfgFrame() {
    }

    public CfgFrame(long tick, config.combat.effect.CfgEffect effect) {
        this.tick = tick;
        this.effect = effect;
    }

    public static CfgFrame _create(configgen.genjava.ConfigInput input) {
        CfgFrame self = new CfgFrame();
        self.tick = input.readLong();
        self.effect = config.combat.effect.CfgEffect._create(input);
        return self;
    }

    public long getTick() {
        return tick;
    }

    public config.combat.effect.CfgEffect getEffect() {
        return effect;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(tick, effect);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgFrame o)) {
            return false;
        }
        return tick == o.tick && effect.equals(o.effect);
    }

    @Override
    public String toString() {
        return "(" + tick + "," + effect + ")";
    }

}
