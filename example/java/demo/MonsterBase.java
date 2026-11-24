package demo;

import config.ConfigExtension;
import config.other.Loot;

public abstract class MonsterBase extends ConfigExtension {
    private int lootItemCount;

    public abstract Loot refAllLoot();

    @Override
    protected void onConfigReady() {
        lootItemCount = refAllLoot().listRefLootid().size();
    }

    public final int getLootItemCount() {
        return lootItemCount;
    }
}
