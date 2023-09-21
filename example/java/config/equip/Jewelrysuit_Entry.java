package config.equip;

public class Jewelrysuit_Entry {
    public static final Jewelrysuit_Entry SPECIALSUIT = new Jewelrysuit_Entry("SpecialSuit", 4);

    private final String name;
    private final int value;

    Jewelrysuit_Entry(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public config.equip.Jewelrysuit ref() {
        return config.equip.Jewelrysuit.get(value);
    }

}
