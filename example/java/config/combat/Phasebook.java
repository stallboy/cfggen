package config.combat;

import configgen.genjava.LoadValueErrs;

public class Phasebook {
    private String school;
    private int tier;
    private boolean airborne;
    private String code;
    private int edition;
    private config.combat.BookEntry entry;
    private Phasebook() {
    }

    public static Phasebook _create(configgen.genjava.ConfigInput input) {
        Phasebook self = new Phasebook();
        self.school = input.readStringInPool();
        self.tier = input.readInt();
        self.airborne = input.readBool();
        self.code = input.readStringInPool();
        self.edition = input.readInt();
        self.entry = config.combat.BookEntry._create(input);
        return self;
    }

    public String getSchool() {
        return school;
    }

    public int getTier() {
        return tier;
    }

    public boolean getAirborne() {
        return airborne;
    }

    public String getCode() {
        return code;
    }

    public int getEdition() {
        return edition;
    }

    public config.combat.BookEntry getEntry() {
        return entry;
    }

    @Override
    public String toString() {
        return "(" + school + "," + tier + "," + airborne + "," + code + "," + edition + "," + entry + ")";
    }

    public static class SchoolTierAirborneKey {
        private final String school;
        private final int tier;
        private final boolean airborne;

        public SchoolTierAirborneKey(String school, int tier, boolean airborne) {
            this.school = school;
            this.tier = tier;
            this.airborne = airborne;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(school, tier, airborne);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SchoolTierAirborneKey o)) {
                return false;
            }
            return school.equals(o.school) && tier == o.tier && airborne == o.airborne;
        }

        @Override
        public String toString() {
            return "(" + school + "," + tier + "," + airborne + ")";
        }
    }

    public static Phasebook get(String school, int tier, boolean airborne) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatPhasebook(school, tier, airborne);
    }

    public static class CodeEditionKey {
        private final String code;
        private final int edition;

        public CodeEditionKey(String code, int edition) {
            this.code = code;
            this.edition = edition;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(code, edition);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CodeEditionKey o)) {
                return false;
            }
            return code.equals(o.code) && edition == o.edition;
        }

        @Override
        public String toString() {
            return "(" + code + "," + edition + ")";
        }
    }

    public static Phasebook getByCodeEdition(String code, int edition) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatPhasebookByCodeEdition(code, edition);
    }

    public static java.util.Collection<Phasebook> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatPhasebook();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<config.combat.Phasebook.SchoolTierAirborneKey, Phasebook> rows, LoadValueErrs errors) {
        int c = rows.size();
        var combat_phasebook_CodeEditionMap = new java.util.LinkedHashMap<config.combat.Phasebook.CodeEditionKey, Phasebook>(c);
        for (var self : rows.values()) {
            if (combat_phasebook_CodeEditionMap.putIfAbsent(new CodeEditionKey(self.code, self.edition), self) != null) {
                var primaryKey = new SchoolTierAirborneKey(self.school, self.tier, self.airborne);
                errors.duplicateUniqueKey("combat.phasebook", "code,edition", primaryKey, new CodeEditionKey(self.code, self.edition), mgr.explain("combat.phasebook", primaryKey));
            }
        }
        mgr._set_combat_phasebook_All(rows);
        mgr._set_combat_phasebook_CodeEditionMap(combat_phasebook_CodeEditionMap);
    }

}
