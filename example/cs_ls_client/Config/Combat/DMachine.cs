using System.Collections.Generic;
namespace Config.Combat
{

public partial class DMachine
{
    public string Code { get; init; } = null!;
    public IReadOnlyList<Combat.DPhase> Phases { get; init; } = null!;
    public IReadOnlyList<Combat.DImpact> Impacts { get; init; } = null!;
    public IReadOnlyList<Combat.DFrame> Frames { get; init; } = null!;
    public IReadOnlyList<Combat.DStance> Stances { get; init; } = null!;
    public IReadOnlyList<Combat.DPhaseVariant> Variants { get; init; } = null!;
    public IReadOnlyList<Combat.DCombo> Combos { get; init; } = null!;
    public IReadOnlyList<Combat.DMarker> Markers { get; init; } = null!; /* 未被引用也必须保持唯一。 */
    public Combat.DAnchor Anchor { get; init; } = null!;
    public IReadOnlyList<Combat.DAnchorUse> Anchors { get; init; } = null!;
    public Combat.DExit OnExit { get; init; } = null!;
    public string Initial { get; init; } = null!;
    public int Impact { get; init; }
    public long Frame { get; init; }
    public bool Stance { get; init; }
    public Combat.DPhaseKey Variant { get; init; } = null!;
    public string Weapon { get; init; } = null!;
    public int Tier { get; init; }
    public bool Airborne { get; init; }
    public Combat.DPhase RefInitial { get; internal set; } = null!;
    public Combat.DImpact RefImpact { get; internal set; } = null!;
    public Combat.DFrame RefFrame { get; internal set; } = null!;
    public Combat.DStance RefStance { get; internal set; } = null!;
    public Combat.DPhaseVariant RefVariant { get; internal set; } = null!;
    public Combat.DCombo RefCombo { get; internal set; } = null!;
}
}
