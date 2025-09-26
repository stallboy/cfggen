package config.other;

public class Monster {
    private int id;
    private java.util.List<config.Position> posList;
    private int lootId;
    private int lootItemId;
    private config.other.Lootitem RefLoot;
    private config.other.Loot RefAllLoot;

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

    public config.other.Lootitem refLoot() {
        return RefLoot;
    }

    public config.other.Loot refAllLoot() {
        return RefAllLoot;
    }

    @Override
    public String toString() {
        return "(" + id + "," + posList + "," + lootId + "," + lootItemId + ")";
    }

    public void _resolveDirect(config.ConfigMgr mgr) {
        RefLoot = mgr.other_lootitem_All.get(new config.other.Lootitem.LootidItemidKey(lootId, lootItemId) );
        java.util.Objects.requireNonNull(RefLoot);
        RefAllLoot = mgr.other_loot_All.get(lootId);
        java.util.Objects.requireNonNull(RefAllLoot);
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
