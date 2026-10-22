package config.combat;

public class CfgAnchor {
    public CfgAnchor() {
    }

    public static CfgAnchor _create(configgen.genjava.ConfigInput input) {
        CfgAnchor self = new CfgAnchor();
        return self;
    }

    @Override
    public int hashCode() {
        return CfgAnchor.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CfgAnchor;
    }

    @Override
    public String toString() {
        return "";
    }

}
