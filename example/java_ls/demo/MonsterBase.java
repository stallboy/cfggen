package demo;

import config.ConfigExtension;
import config.other.CfgLoot;

public abstract class MonsterBase extends ConfigExtension {
    private int lootItemCount;

    public abstract CfgLoot refAllLoot();

    @Override
    protected void onConfigReady() {
        lootItemCount = refAllLoot().listRefLootid().size();
    }

    public final int getLootItemCount() {
        return lootItemCount;
    }
}
