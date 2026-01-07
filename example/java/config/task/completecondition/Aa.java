package config.task.completecondition;

public final class Aa implements config.task.completecondition.Completecondition {
    @Override
    public config.task.Completeconditiontype type() {
        return config.task.Completeconditiontype.AA;
    }


    public Aa() {
    }

    public static Aa _create(configgen.genjava.ConfigInput input) {
        Aa self = new Aa();
        return self;
    }

    @Override
    public int hashCode() {
        return Aa.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Aa;
    }

    @Override
    public String toString() {
        return "Aa";
    }

}
