package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgCast {
    private int id;
    private String abilityCode;
    private config.combat.CfgOrigin origin;
    private java.util.List<config.combat.CfgAbilityUse> uses;
    private java.util.List<config.combat.CfgOptionalAim> optional;
    private String school;
    private int tier;
    private boolean airborne;
    private String bookCode;
    private int edition;
    private config.combat.CfgPhaseKey rewardKey;
    private config.combat.CfgBookEntry RefBook;
    private config.combat.CfgBookEntry RefBookByCode;
    private config.combat.CfgPhasebook RefBookRecord;
    private config.combat.CfgReward RefReward;

    public void _bindRefBook(config.combat.CfgBookEntry value) {
        this.RefBook = value;
    }
    public void _bindRefBookByCode(config.combat.CfgBookEntry value) {
        this.RefBookByCode = value;
    }
    public void _bindRefBookRecord(config.combat.CfgPhasebook value) {
        this.RefBookRecord = value;
    }
    public void _bindRefReward(config.combat.CfgReward value) {
        this.RefReward = value;
    }
    private CfgCast() {
    }

    public static CfgCast _create(configgen.genjava.ConfigInput input) {
        CfgCast self = new CfgCast();
        self.id = input.readInt();
        self.abilityCode = input.readStringInPool();
        self.origin = config.combat.CfgOrigin._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.uses = java.util.Collections.emptyList();
            } else {
                self.uses = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.uses.add(config.combat.CfgAbilityUse._create(input));
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
                    self.optional.add(config.combat.CfgOptionalAim._create(input));
                }
                self.optional = java.util.Collections.unmodifiableList(self.optional);
            }
        }
        self.school = input.readStringInPool();
        self.tier = input.readInt();
        self.airborne = input.readBool();
        self.bookCode = input.readStringInPool();
        self.edition = input.readInt();
        self.rewardKey = config.combat.CfgPhaseKey._create(input);
        return self;
    }

    public int getId() {
        return id;
    }

    public String getAbilityCode() {
        return abilityCode;
    }

    public config.combat.CfgOrigin getOrigin() {
        return origin;
    }

    public java.util.List<config.combat.CfgAbilityUse> getUses() {
        return uses;
    }

    public java.util.List<config.combat.CfgOptionalAim> getOptional() {
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

    public config.combat.CfgPhaseKey getRewardKey() {
        return rewardKey;
    }

    public config.combat.CfgBookEntry refBook() {
        return RefBook;
    }

    public config.combat.CfgBookEntry refBookByCode() {
        return RefBookByCode;
    }

    public config.combat.CfgPhasebook refBookRecord() {
        return RefBookRecord;
    }

    public config.combat.CfgReward refReward() {
        return RefReward;
    }

    @Override
    public String toString() {
        return "(" + id + "," + abilityCode + "," + origin + "," + uses + "," + optional + "," + school + "," + tier + "," + airborne + "," + bookCode + "," + edition + "," + rewardKey + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.CfgAbilityUse e : this.uses) {
            e._resolve(mgr, errors);
        }
        for (config.combat.CfgOptionalAim e : this.optional) {
            e._resolve(mgr, errors);
        }

    }

    public static CfgCast get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatCast(id);
    }

    public static java.util.Collection<CfgCast> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatCast();
    }


}
