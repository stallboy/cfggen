using System.Collections.Generic;
namespace Config.Combat
{

public partial class DEncounter
{
    public int Id { get; init; }
    public Combat.DCommand Command { get; init; } = null!;

    public static DEncounter? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DEncounter? Get(ConfigMgr config, int id)
    {
        var index = config._combat_dencounter_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DEncounter> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DEncounter> All(ConfigMgr config)
        => config._combat_dencounter_Rows;
}
}
