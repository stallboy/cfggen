package config.other;

import configgen.genjava.LoadValueErrs;

// 来自：other/monster.csv
public class CfgMonster {
    private int id;
    private java.util.List<config.CfgPosition> posList;
    private int lootId;
    private int lootItemId;
    private java.util.Map<String, Integer> enumMap1;
    private java.util.Map<Integer, String> enumMap2;
    private config.other.CfgLoot_item RefLoot;
    private config.other.CfgLoot RefAllLoot;
    private java.util.Map<Integer, config.other.CfgArgCaptureMode> RefEnumMap2;

    private CfgMonster() {
    }

    public static CfgMonster _create(configgen.genjava.ConfigInput input) {
        CfgMonster self = new CfgMonster();
        self.id = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.posList = java.util.Collections.emptyList();
            } else {
                self.posList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.posList.add(config.CfgPosition._create(input));
                }
                self.posList = java.util.Collections.unmodifiableList(self.posList);
            }
        }
        self.lootId = input.readInt();
        self.lootItemId = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.enumMap1 = java.util.Collections.emptyMap();
            } else {
                self.enumMap1 = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readStringInPool();
                    var value = input.readInt();
                    if (self.enumMap1.putIfAbsent(key, value) != null) input.errors().duplicateKey("other.monster.enumMap1", key);
                }
                self.enumMap1 = java.util.Collections.unmodifiableMap(self.enumMap1);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.enumMap2 = java.util.Collections.emptyMap();
            } else {
                self.enumMap2 = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readStringInPool();
                    if (self.enumMap2.putIfAbsent(key, value) != null) input.errors().duplicateKey("other.monster.enumMap2", key);
                }
                self.enumMap2 = java.util.Collections.unmodifiableMap(self.enumMap2);
            }
        }
        return self;
    }

    public int getId() {
        return id;
    }

    public java.util.List<config.CfgPosition> getPosList() {
        return posList;
    }

    /**
     * loot
     */
    public int getLootId() {
        return lootId;
    }

    /**
     * item
     */
    public int getLootItemId() {
        return lootItemId;
    }

    public java.util.Map<String, Integer> getEnumMap1() {
        return enumMap1;
    }

    public java.util.Map<Integer, String> getEnumMap2() {
        return enumMap2;
    }

    public config.other.CfgLoot_item refLoot() {
        return RefLoot;
    }

    public config.other.CfgLoot refAllLoot() {
        return RefAllLoot;
    }

    public java.util.Map<Integer, config.other.CfgArgCaptureMode> refEnumMap2() {
        return RefEnumMap2;
    }

    @Override
    public String toString() {
        return "(" + id + "," + posList + "," + lootId + "," + lootItemId + "," + enumMap1 + "," + enumMap2 + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.RefLoot = mgr.getOtherLoot_item(this.lootId, this.lootItemId);
        if (this.RefLoot == null) errors.missingReference("other.monster", "Loot", "other.loot_item", java.util.List.of(this.lootId, this.lootItemId));
        this.RefAllLoot = mgr.getOtherLoot(this.lootId);
        if (this.RefAllLoot == null) errors.missingReference("other.monster", "AllLoot", "other.loot", this.lootId);
        if (this.enumMap2.isEmpty()) {
            this.RefEnumMap2 = java.util.Collections.emptyMap();
        } else {
            this.RefEnumMap2 = new java.util.LinkedHashMap<>(this.enumMap2.size());
            for (java.util.Map.Entry<Integer, String> e : this.enumMap2.entrySet()) {
                var ref = config.other.CfgArgCaptureMode.get(e.getValue());
                if (ref == null) errors.missingReference("other.monster", "enumMap2", "other.ArgCaptureMode", e.getValue());
                this.RefEnumMap2.put(e.getKey(), ref);
            }
        }
        this.RefEnumMap2 = java.util.Collections.unmodifiableMap(this.RefEnumMap2);

    }

    public static CfgMonster get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherMonster(id);
    }

    public static java.util.Collection<CfgMonster> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherMonster();
    }


}
