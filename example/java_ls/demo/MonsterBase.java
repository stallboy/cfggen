package demo;

import config.ConfigExtension;
import config.ConfigMgr;
import config.other.CfgLoot;

public abstract class MonsterBase extends ConfigExtension {
    private int lootItemCount;

    public abstract CfgLoot refAllLoot();

    @Override
    protected void onConfigReady(ConfigMgr _mgr) {
        lootItemCount = refAllLoot().listRefLootid().size();
    }

    public final int getLootItemCount() {
        return lootItemCount;
    }
}
