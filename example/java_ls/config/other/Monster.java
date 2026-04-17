package config.other;

public class Monster {
    private int id;
    private java.util.List<config.Position> posList;
    private int lootId;
    private int lootItemId;
    private java.util.Map<String, Integer> enumMap1;
    private java.util.Map<Integer, String> enumMap2;
    private config.other.Lootitem RefLoot;
    private config.other.Loot RefAllLoot;
    private java.util.Map<Integer, config.other.ArgCaptureMode> RefEnumMap2;

    private Monster() {
    }

    public static Monster _create(configgen.genjava.ConfigInput input) {
        Monster self = new Monster();
        self.id = input.readInt();
        {
            int c = input.readInt();
            if (c == 0) {
                self.posList = java.util.Collections.emptyList();
            } else {
                self.posList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.posList.add(config.Position._create(input));
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

    public java.util.List<config.Position> getPosList() {
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

    public config.other.Lootitem refLoot() {
        return RefLoot;
    }

    public config.other.Loot refAllLoot() {
        return RefAllLoot;
    }

    public java.util.Map<Integer, config.other.ArgCaptureMode> refEnumMap2() {
        return RefEnumMap2;
    }

    @Override
    public String toString() {
        return "(" + id + "," + posList + "," + lootId + "," + lootItemId + "," + enumMap1 + "," + enumMap2 + ")";
    }

    public void _resolveDirect(config.ConfigMgr mgr) {
        RefLoot = mgr.other_lootitem_All.get(new config.other.Lootitem.LootidItemidKey(lootId, lootItemId) );
        java.util.Objects.requireNonNull(RefLoot);
        RefAllLoot = mgr.other_loot_All.get(lootId);
        java.util.Objects.requireNonNull(RefAllLoot);
        if (enumMap2.isEmpty()) {
            RefEnumMap2 = java.util.Collections.emptyMap();
        } else {
            RefEnumMap2 = new java.util.LinkedHashMap<>(enumMap2.size());
            for (java.util.Map.Entry<Integer, String> e : enumMap2.entrySet()) {
                config.other.ArgCaptureMode rv = config.other.ArgCaptureMode.get(e.getValue());
                java.util.Objects.requireNonNull(rv);
                RefEnumMap2.put(e.getKey(), rv);
            }
        }
    }

    public void _resolve(config.ConfigMgr mgr) {
        _resolveDirect(mgr);
    }

    public static Monster get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherMonster(id);
    }

    public static java.util.Collection<Monster> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherMonster();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Monster self = Monster._create(input);
                mgr.other_monster_All.put(self.id, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (Monster e : mgr.other_monster_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}
