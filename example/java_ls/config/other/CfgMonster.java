package config.other;

// 来自：other/monster.csv
public class CfgMonster {
    private int id;
    private java.util.List<config.CfgPosition> posList;
    private int lootId;
    private int lootItemId;
    private java.util.Map<String, Integer> enumMap1;
    private java.util.Map<Integer, String> enumMap2;
    private config.other.CfgLootitem RefLoot;
    private config.other.CfgLoot RefAllLoot;
    private java.util.Map<Integer, config.other.CfgArgCaptureMode> RefEnumMap2;

    private CfgMonster() {
    }

    public static CfgMonster _create(configgen.genjava.ConfigInput input) {
        CfgMonster self = new CfgMonster();
        self.id = input.readInt();
        {
            int c = input.readInt();
            if (c == 0) {
                self.posList = java.util.Collections.emptyList();
            } else {
                self.posList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.posList.add(config.CfgPosition._create(input));
                }
            }
        }
        self.lootId = input.readInt();
        self.lootItemId = input.readInt();
        {
            int c = input.readInt();
            if (c == 0) {
                self.enumMap1 = java.util.Collections.emptyMap();
            } else {
                self.enumMap1 = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    self.enumMap1.put(input.readStringInPool(), input.readInt());
                }
            }
        }
        {
            int c = input.readInt();
            if (c == 0) {
                self.enumMap2 = java.util.Collections.emptyMap();
            } else {
                self.enumMap2 = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    self.enumMap2.put(input.readInt(), input.readStringInPool());
                }
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

    public config.other.CfgLootitem refLoot() {
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

    public void _resolveDirect(config.ConfigMgr mgr) {
        RefLoot = mgr.other_lootitem_All.get(new config.other.CfgLootitem.LootidItemidKey(lootId, lootItemId) );
        configgen.genjava.LoadValueErrs.requireNonNull(RefLoot, "other.monster.Loot -> other.lootitem", "" + lootId + "," + lootItemId);
        RefAllLoot = mgr.other_loot_All.get(lootId);
        configgen.genjava.LoadValueErrs.requireNonNull(RefAllLoot, "other.monster.AllLoot -> other.loot", lootId);
        if (enumMap2.isEmpty()) {
            RefEnumMap2 = java.util.Collections.emptyMap();
        } else {
            RefEnumMap2 = new java.util.LinkedHashMap<>(enumMap2.size());
            for (java.util.Map.Entry<Integer, String> e : enumMap2.entrySet()) {
                config.other.CfgArgCaptureMode rv = config.other.CfgArgCaptureMode.get(e.getValue());
                configgen.genjava.LoadValueErrs.requireNonNull(rv, "other.monster.enumMap2 -> other.ArgCaptureMode", e.getValue());
                RefEnumMap2.put(e.getKey(), rv);
            }
        }
    }

    public void _resolve(config.ConfigMgr mgr) {
        _resolveDirect(mgr);
    }

    public static CfgMonster get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherMonster(id);
    }

    public static java.util.Collection<CfgMonster> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherMonster();
    }
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.other_monster_All = new java.util.LinkedHashMap<>(c);
            for (; c > 0; c--) {
                CfgMonster self = CfgMonster._create(input);
                mgr.other_monster_All.put(self.id, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (CfgMonster e : mgr.other_monster_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}
