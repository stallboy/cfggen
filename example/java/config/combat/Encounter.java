package config.combat;

import configgen.genjava.LoadValueErrs;

public class Encounter {
    private int id;
    private config.combat.command.Command command;
    private Encounter() {
    }

    public static Encounter _create(configgen.genjava.ConfigInput input) {
        Encounter self = new Encounter();
        self.id = input.readInt();
        self.command = config.combat.command.Command._create(input);
        return self;
    }

    public int getId() {
        return id;
    }

    public config.combat.command.Command getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "(" + id + "," + command + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.command._resolve(mgr, errors);
    }

    public static Encounter get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatEncounter(id);
    }

    public static java.util.Collection<Encounter> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatEncounter();
    }


}
