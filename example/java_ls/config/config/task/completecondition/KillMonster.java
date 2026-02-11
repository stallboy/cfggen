package config.task.completecondition;

public class KillMonster implements config.task.Completecondition {
    @Override
    public config.task.Completeconditiontype type() {
        return config.task.Completeconditiontype.KILLMONSTER;
    }

    private int monsterid;
    private int count;
    private config.other.Monster RefMonsterid;

    private KillMonster() {
    }

    public KillMonster(int monsterid, int count) {
        this.monsterid = monsterid;
        this.count = count;
    }

    public static KillMonster _create(configgen.genjava.ConfigInput input) {
        KillMonster self = new KillMonster();
        self.monsterid = input.readInt();
        self.count = input.readInt();
        return self;
    }

    public int getMonsterid() {
        return monsterid;
    }

    public int getCount() {
        return count;
    }

    public config.other.Monster refMonsterid() {
        return RefMonsterid;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(monsterid, count);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof KillMonster))
            return false;
        KillMonster o = (KillMonster) other;
        return monsterid == o.monsterid && count == o.count;
    }

    @Override
    public String toString() {
        return "KillMonster(" + monsterid + "," + count + ")";
    }

    public void _resolveDirect(config.ConfigMgr mgr) {
        RefMonsterid = mgr.other_monster_All.get(monsterid);
        java.util.Objects.requireNonNull(RefMonsterid);
    }

    @Override
    public void _resolve(config.ConfigMgr mgr) {
        _resolveDirect(mgr);
    }

}
