package config.equip;

public class Jewelrysuit_Entry {
    public static final Jewelrysuit_Entry SPECIALSUIT = new Jewelrysuit_Entry("SpecialSuit", 4);

    private final String name;
    private final int value;
    private volatile config.equip.Jewelrysuit ref;

    Jewelrysuit_Entry(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public config.equip.Jewelrysuit ref() {
        return ref;
    }

    void setRef() {
        ref = config.equip.Jewelrysuit.get(value);
        java.util.Objects.requireNonNull(ref);
    }

    public static void setAllRefs() {
        SPECIALSUIT.setRef();
    }
}
