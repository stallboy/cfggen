package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgAnchorUse {
    private config.combat.CfgAnchor RefAnchor;

    public void _bindRefAnchor(config.combat.CfgAnchor value) {
        this.RefAnchor = value;
    }
    public CfgAnchorUse() {
    }

    public static CfgAnchorUse _create(configgen.genjava.ConfigInput input) {
        CfgAnchorUse self = new CfgAnchorUse();
        return self;
    }

    public config.combat.CfgAnchor refAnchor() {
        return RefAnchor;
    }

    @Override
    public int hashCode() {
        return CfgAnchorUse.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CfgAnchorUse;
    }

    @Override
    public String toString() {
        return "";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

}
