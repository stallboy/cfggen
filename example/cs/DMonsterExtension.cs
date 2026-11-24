namespace Config.Other;

public partial class DMonster : IConfigExtension
{
    public int LootItemCount { get; private set; }

    void IConfigExtension.OnConfigReady(ConfigMgr _mgr)
    {
        // 加载器已解析掉落表的外键，使用回调传入的配置集查表。
        LootItemCount = DLoot.Get(_mgr, LootId)!.ListRefLootid.Count;
    }
}
