package config.combat;

public class CfgMarker {
    private String key;
    private CfgMarker() {
    }

    public CfgMarker(String key) {
        this.key = key;
    }

    public static CfgMarker _create(configgen.genjava.ConfigInput input) {
        CfgMarker self = new CfgMarker();
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
        if (!(other instanceof CfgMarker o)) {
            return false;
        }
        return key.equals(o.key);
    }

    @Override
    public String toString() {
        return "(" + key + ")";
    }

}
