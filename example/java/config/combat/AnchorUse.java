package config.combat;

import configgen.genjava.LoadValueErrs;

public class AnchorUse {
    private config.combat.Anchor RefAnchor;

    public void _bindRefAnchor(config.combat.Anchor value) {
        this.RefAnchor = value;
    }
    public AnchorUse() {
    }

    public static AnchorUse _create(configgen.genjava.ConfigInput input) {
        AnchorUse self = new AnchorUse();
        return self;
    }

    public config.combat.Anchor refAnchor() {
        return RefAnchor;
    }

    @Override
    public int hashCode() {
        return AnchorUse.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AnchorUse;
    }

    @Override
    public String toString() {
        return "";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

}
