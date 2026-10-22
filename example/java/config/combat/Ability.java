package config.combat;

import configgen.genjava.LoadValueErrs;

public class Ability {
    private int id;
    private String code;
    private config.combat.Origin origin;
    private java.util.List<config.combat.Machine> machines;
    private String startMachine;
    private String startPhase;
    private config.combat.Phase RefInitial;
    private config.combat.Origin RefSpawn;

    public void _bindRefInitial(config.combat.Phase value) {
        this.RefInitial = value;
    }
    public void _bindRefSpawn(config.combat.Origin value) {
        this.RefSpawn = value;
    }
    private Ability() {
    }

    public static Ability _create(configgen.genjava.ConfigInput input) {
        Ability self = new Ability();
        self.id = input.readInt();
        self.code = input.readStringInPool();
        self.origin = config.combat.Origin._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.machines = java.util.Collections.emptyList();
            } else {
                self.machines = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.machines.add(config.combat.Machine._create(input));
                }
                self.machines = java.util.Collections.unmodifiableList(self.machines);
            }
        }
        self.startMachine = input.readStringInPool();
        self.startPhase = input.readStringInPool();
        return self;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public config.combat.Origin getOrigin() {
        return origin;
    }

    public java.util.List<config.combat.Machine> getMachines() {
        return machines;
    }

    public String getStartMachine() {
        return startMachine;
    }

    public String getStartPhase() {
        return startPhase;
    }

    public config.combat.Phase refInitial() {
        return RefInitial;
    }

    public config.combat.Origin refSpawn() {
        return RefSpawn;
    }

    @Override
    public String toString() {
        return "(" + id + "," + code + "," + origin + "," + machines + "," + startMachine + "," + startPhase + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.Machine e : this.machines) {
            e._resolve(mgr, errors);
        }

    }

    public static Ability get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatAbility(id);
    }

    public static Ability getByCode(String code) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatAbilityByCode(code);
    }

    public static java.util.Collection<Ability> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatAbility();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<Integer, Ability> rows, LoadValueErrs errors) {
        int c = rows.size();
        var combat_ability_CodeMap = new java.util.LinkedHashMap<String, Ability>(c);
        for (var self : rows.values()) {
            if (combat_ability_CodeMap.putIfAbsent(self.code, self) != null) {
                var primaryKey = self.id;
                errors.duplicateUniqueKey("combat.ability", "code", primaryKey, self.code, mgr.explain("combat.ability", primaryKey));
            }
        }
        mgr._set_combat_ability_All(rows);
        mgr._set_combat_ability_CodeMap(combat_ability_CodeMap);
    }

}
