package config.combat;

// 内部引用验收：技能状态机、命中窗口、事件时间轴与连招分支。
public class PhaseKey {
    private String name;
    private int variant;
    private PhaseKey() {
    }

    public PhaseKey(String name, int variant) {
        this.name = name;
        this.variant = variant;
    }

    public static PhaseKey _create(configgen.genjava.ConfigInput input) {
        PhaseKey self = new PhaseKey();
        self.name = input.readStringInPool();
        self.variant = input.readInt();
        return self;
    }

    public String getName() {
        return name;
    }

    public int getVariant() {
        return variant;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, variant);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PhaseKey o)) {
            return false;
        }
        return name.equals(o.name) && variant == o.variant;
    }

    @Override
    public String toString() {
        return "(" + name + "," + variant + ")";
    }

}
