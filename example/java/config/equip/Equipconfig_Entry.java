package config.equip;

public class Equipconfig_Entry {
    public static final Equipconfig_Entry INSTANCE = new Equipconfig_Entry("Instance");
    public static final Equipconfig_Entry INSTANCE2 = new Equipconfig_Entry("Instance2");

    private final String value;

    Equipconfig_Entry(String value) {
        this.value = value;
    }

    public config.equip.Equipconfig ref() {
        return config.equip.Equipconfig.get(value);
    }

}
