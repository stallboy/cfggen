package config.combat;

public class CfgBookEntry {
    private String name;
    private CfgBookEntry() {
    }

    public CfgBookEntry(String name) {
        this.name = name;
    }

    public static CfgBookEntry _create(configgen.genjava.ConfigInput input) {
        CfgBookEntry self = new CfgBookEntry();
        self.name = input.readStringInPool();
        return self;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgBookEntry o)) {
            return false;
        }
        return name.equals(o.name);
    }

    @Override
    public String toString() {
        return "(" + name + ")";
    }

}
