package config.task.completecondition;

public final class CfgKillMonster implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgCompleteconditiontype type() {
        return config.task.CfgCompleteconditiontype.KILLMONSTER;
    }

    private int monsterid;
    private int count;
    private config.other.CfgMonster RefMonsterid;

    private CfgKillMonster() {
    }

    public CfgKillMonster(int monsterid, int count) {
        this.monsterid = monsterid;
        this.count = count;
    }

    public static CfgKillMonster _create(configgen.genjava.ConfigInput input) {
        CfgKillMonster self = new CfgKillMonster();
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

    public config.other.CfgMonster refMonsterid() {
        return RefMonsterid;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(monsterid, count);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgKillMonster))
            return false;
        CfgKillMonster o = (CfgKillMonster) other;
        return monsterid == o.monsterid && count == o.count;
    }

    @Override
    public String toString() {
        return "CfgKillMonster(" + monsterid + "," + count + ")";
    }

    public void _resolveDirect(config.ConfigMgr mgr) {
        RefMonsterid = mgr.other_monster_All.get(monsterid);
        configgen.genjava.LoadValueErrs.requireNonNull(RefMonsterid, "KillMonster.monsterid -> other.monster", monsterid);
    }

    @Override
    public void _resolve(config.ConfigMgr mgr) {
        _resolveDirect(mgr);
    }

}
