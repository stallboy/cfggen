package config.other;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：other/monster.csv
public class Monster {
    private final int id;
    private final java.util.List<config.Position> posList;
    private final int lootId;
    private final int lootItemId;
    private final java.util.Map<String, Integer> enumMap1;
    private final java.util.Map<Integer, String> enumMap2;
    private config.other.@Nullable LootItem RefLoot;
    private config.other.@Nullable Loot RefAllLoot;
    private java.util.@Nullable Map<Integer, config.other.@Nullable ArgCaptureMode> RefEnumMap2;

    private Monster(configgen.genjava.ConfigInput input) {
        this.id = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.posList = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.Position>(c);
                for (; c > 0; c--) {
                    items.add(config.Position._create(input));
                }
                this.posList = java.util.Collections.unmodifiableList(items);
            }
        }
        this.lootId = input.readInt();
        this.lootItemId = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.enumMap1 = java.util.Collections.emptyMap();
            } else {
                var entries = new java.util.LinkedHashMap<String, Integer>(c);
                for (; c > 0; c--) {
                    var key = input.readStringInPool();
                    var value = input.readInt();
                    if (entries.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("other.monster.enumMap1", key);
                    }
                }
                this.enumMap1 = java.util.Collections.unmodifiableMap(entries);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.enumMap2 = java.util.Collections.emptyMap();
            } else {
                var entries = new java.util.LinkedHashMap<Integer, String>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readStringInPool();
                    if (entries.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("other.monster.enumMap2", key);
                    }
                }
                this.enumMap2 = java.util.Collections.unmodifiableMap(entries);
            }
        }
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

    public config.other.@Nullable LootItem refLoot() {
        return RefLoot;
    }

    public config.other.@Nullable Loot refAllLoot() {
        return RefAllLoot;
    }

    public java.util.@Nullable Map<Integer, config.other.@Nullable ArgCaptureMode> refEnumMap2() {
        return RefEnumMap2;
    }

    @Override
    public String toString() {
        return "(" + id + "," + posList + "," + lootId + "," + lootItemId + "," + enumMap1 + "," + enumMap2 + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.RefLoot = mgr.getOtherLootItem(this.lootId, this.lootItemId);
        if (this.RefLoot == null) {
            errors.missingReference("other.monster", "Loot", "other.loot_item", java.util.List.of(this.lootId, this.lootItemId));
        }
        this.RefAllLoot = mgr.getOtherLoot(this.lootId);
        if (this.RefAllLoot == null) {
            errors.missingReference("other.monster", "AllLoot", "other.loot", this.lootId);
        }
        if (this.enumMap2.isEmpty()) {
            this.RefEnumMap2 = java.util.Collections.emptyMap();
        } else {
            this.RefEnumMap2 = new java.util.LinkedHashMap<>(this.enumMap2.size());
            for (java.util.Map.Entry<Integer, String> e : this.enumMap2.entrySet()) {
                var ref = config.other.ArgCaptureMode.get(e.getValue());
                if (ref == null) {
                    errors.missingReference("other.monster", "enumMap2", "other.ArgCaptureMode", e.getValue());
                }
                this.RefEnumMap2.put(e.getKey(), ref);
            }
        }
        this.RefEnumMap2 = java.util.Collections.unmodifiableMap(this.RefEnumMap2);

    }

    public static Monster _create(configgen.genjava.ConfigInput input) {
        return new Monster(input);
    }

    public static @Nullable Monster get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherMonster(id);
    }

    public static java.util.Collection<Monster> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherMonster();
    }


}
