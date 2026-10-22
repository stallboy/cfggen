package config.combat;

import configgen.genjava.LoadValueErrs;

public class Cast {
    private int id;
    private String abilityCode;
    private config.combat.Origin origin;
    private java.util.List<config.combat.AbilityUse> uses;
    private java.util.List<config.combat.OptionalAim> optional;
    private String school;
    private int tier;
    private boolean airborne;
    private String bookCode;
    private int edition;
    private config.combat.PhaseKey rewardKey;
    private config.combat.BookEntry RefBook;
    private config.combat.BookEntry RefBookByCode;
    private config.combat.Phasebook RefBookRecord;
    private config.combat.Reward RefReward;

    public void _bindRefBook(config.combat.BookEntry value) {
        this.RefBook = value;
    }
    public void _bindRefBookByCode(config.combat.BookEntry value) {
        this.RefBookByCode = value;
    }
    public void _bindRefBookRecord(config.combat.Phasebook value) {
        this.RefBookRecord = value;
    }
    public void _bindRefReward(config.combat.Reward value) {
        this.RefReward = value;
    }
    private Cast() {
    }

    public static Cast _create(configgen.genjava.ConfigInput input) {
        Cast self = new Cast();
        self.id = input.readInt();
        self.abilityCode = input.readStringInPool();
        self.origin = config.combat.Origin._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.uses = java.util.Collections.emptyList();
            } else {
                self.uses = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.uses.add(config.combat.AbilityUse._create(input));
                }
                self.uses = java.util.Collections.unmodifiableList(self.uses);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.optional = java.util.Collections.emptyList();
            } else {
                self.optional = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.optional.add(config.combat.OptionalAim._create(input));
                }
                self.optional = java.util.Collections.unmodifiableList(self.optional);
            }
        }
        self.school = input.readStringInPool();
        self.tier = input.readInt();
        self.airborne = input.readBool();
        self.bookCode = input.readStringInPool();
        self.edition = input.readInt();
        self.rewardKey = config.combat.PhaseKey._create(input);
        return self;
    }

    public int getId() {
        return id;
    }

    public String getAbilityCode() {
        return abilityCode;
    }

    public config.combat.Origin getOrigin() {
        return origin;
    }

    public java.util.List<config.combat.AbilityUse> getUses() {
        return uses;
    }

    public java.util.List<config.combat.OptionalAim> getOptional() {
        return optional;
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

    public String getBookCode() {
        return bookCode;
    }

    public int getEdition() {
        return edition;
    }

    public config.combat.PhaseKey getRewardKey() {
        return rewardKey;
    }

    public config.combat.BookEntry refBook() {
        return RefBook;
    }

    public config.combat.BookEntry refBookByCode() {
        return RefBookByCode;
    }

    public config.combat.Phasebook refBookRecord() {
        return RefBookRecord;
    }

    public config.combat.Reward refReward() {
        return RefReward;
    }

    @Override
    public String toString() {
        return "(" + id + "," + abilityCode + "," + origin + "," + uses + "," + optional + "," + school + "," + tier + "," + airborne + "," + bookCode + "," + edition + "," + rewardKey + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.AbilityUse e : this.uses) {
            e._resolve(mgr, errors);
        }
        for (config.combat.OptionalAim e : this.optional) {
            e._resolve(mgr, errors);
        }

    }

    public static Cast get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatCast(id);
    }

    public static java.util.Collection<Cast> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatCast();
    }


}
