package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgEncounter {
    private int id;
    private config.combat.command.CfgCommand command;
    private CfgEncounter() {
    }

    public static CfgEncounter _create(configgen.genjava.ConfigInput input) {
        CfgEncounter self = new CfgEncounter();
        self.id = input.readInt();
        self.command = config.combat.command.CfgCommand._create(input);
        return self;
    }

    public int getId() {
        return id;
    }

    public config.combat.command.CfgCommand getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "(" + id + "," + command + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.command._resolve(mgr, errors);
    }

    public static CfgEncounter get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatEncounter(id);
    }

    public static java.util.Collection<CfgEncounter> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatEncounter();
    }


}
