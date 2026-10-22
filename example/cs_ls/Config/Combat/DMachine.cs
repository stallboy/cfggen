namespace Config.Combat;

public partial class DMachine
{
    public required string Code { get; init; }
    public required IReadOnlyList<Combat.DPhase> Phases { get; init; }
    public required IReadOnlyList<Combat.DImpact> Impacts { get; init; }
    public required IReadOnlyList<Combat.DFrame> Frames { get; init; }
    public required IReadOnlyList<Combat.DStance> Stances { get; init; }
    public required IReadOnlyList<Combat.DPhaseVariant> Variants { get; init; }
    public required IReadOnlyList<Combat.DCombo> Combos { get; init; }
    public required IReadOnlyList<Combat.DMarker> Markers { get; init; } /* 未被引用也必须保持唯一。 */
    public required Combat.DAnchor Anchor { get; init; }
    public required IReadOnlyList<Combat.DAnchorUse> Anchors { get; init; }
    public required Combat.DExit OnExit { get; init; }
    public required string Initial { get; init; }
    public required int Impact { get; init; }
    public required long Frame { get; init; }
    public required bool Stance { get; init; }
    public required Combat.DPhaseKey Variant { get; init; }
    public required string Weapon { get; init; }
    public required int Tier { get; init; }
    public required bool Airborne { get; init; }
    public Combat.DPhase RefInitial { get; internal set; } = null!;
    public Combat.DImpact RefImpact { get; internal set; } = null!;
    public Combat.DFrame RefFrame { get; internal set; } = null!;
    public Combat.DStance RefStance { get; internal set; } = null!;
    public Combat.DPhaseVariant RefVariant { get; internal set; } = null!;
    public Combat.DCombo RefCombo { get; internal set; } = null!;
}
