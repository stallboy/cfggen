package config

import "fmt"

type CombatMachine struct {
    code string
    phases []*CombatPhase
    impacts []*CombatImpact
    frames []*CombatFrame
    stances []*CombatStance
    variants []*CombatPhaseVariant
    combos []*CombatCombo
    markers []*CombatMarker //未被引用也必须保持唯一。
    anchor *CombatAnchor
    anchors []*CombatAnchorUse
    onExit *CombatExit
    initial string
    impact int32
    frame int64
    stance bool
    variant *CombatPhaseKey
    weapon string
    tier int32
    airborne bool
    refInitial *CombatPhase
    refImpact *CombatImpact
    refFrame *CombatFrame
    refStance *CombatStance
    refVariant *CombatPhaseVariant
    refCombo *CombatCombo
}

func createCombatMachine(stream *Stream) *CombatMachine {
    v := &CombatMachine{}
    v.code = stream.ReadStringInPool()
    phasesSize := stream.ReadCount(0)
    v.phases = make([]*CombatPhase, phasesSize)
    for i := 0; i < phasesSize; i++ {
        v.phases[i] = createCombatPhase(stream)
    }
    impactsSize := stream.ReadCount(0)
    v.impacts = make([]*CombatImpact, impactsSize)
    for i := 0; i < impactsSize; i++ {
        v.impacts[i] = createCombatImpact(stream)
    }
    framesSize := stream.ReadCount(0)
    v.frames = make([]*CombatFrame, framesSize)
    for i := 0; i < framesSize; i++ {
        v.frames[i] = createCombatFrame(stream)
    }
    stancesSize := stream.ReadCount(0)
    v.stances = make([]*CombatStance, stancesSize)
    for i := 0; i < stancesSize; i++ {
        v.stances[i] = createCombatStance(stream)
    }
    variantsSize := stream.ReadCount(0)
    v.variants = make([]*CombatPhaseVariant, variantsSize)
    for i := 0; i < variantsSize; i++ {
        v.variants[i] = createCombatPhaseVariant(stream)
    }
    combosSize := stream.ReadCount(0)
    v.combos = make([]*CombatCombo, combosSize)
    for i := 0; i < combosSize; i++ {
        v.combos[i] = createCombatCombo(stream)
    }
    markersSize := stream.ReadCount(0)
    v.markers = make([]*CombatMarker, markersSize)
    for i := 0; i < markersSize; i++ {
        v.markers[i] = createCombatMarker(stream)
    }
    v.anchor = createCombatAnchor(stream)
    anchorsSize := stream.ReadCount(0)
    v.anchors = make([]*CombatAnchorUse, anchorsSize)
    for i := 0; i < anchorsSize; i++ {
        v.anchors[i] = createCombatAnchorUse(stream)
    }
    v.onExit = createCombatExit(stream)
    v.initial = stream.ReadStringInPool()
    v.impact = stream.ReadInt32()
    v.frame = stream.ReadInt64()
    v.stance = stream.ReadBool()
    v.variant = createCombatPhaseKey(stream)
    v.weapon = stream.ReadStringInPool()
    v.tier = stream.ReadInt32()
    v.airborne = stream.ReadBool()
    return v
}

func (t *CombatMachine) String() string {
    return fmt.Sprintf("CombatMachine{code=%v, phases=%v, impacts=%v, frames=%v, stances=%v, variants=%v, combos=%v, markers=%v, anchor=%v, anchors=%v, onExit=%v, initial=%v, impact=%v, frame=%v, stance=%v, variant=%v, weapon=%v, tier=%v, airborne=%v}", t.code, t.phases, t.impacts, t.frames, t.stances, t.variants, t.combos, t.markers, t.anchor, t.anchors, t.onExit, t.initial, t.impact, t.frame, t.stance, t.variant, t.weapon, t.tier, t.airborne)
}

//getters
func (t *CombatMachine) Code() string {
    return t.code
}

func (t *CombatMachine) Phases() []*CombatPhase {
    return t.phases
}

func (t *CombatMachine) Impacts() []*CombatImpact {
    return t.impacts
}

func (t *CombatMachine) Frames() []*CombatFrame {
    return t.frames
}

func (t *CombatMachine) Stances() []*CombatStance {
    return t.stances
}

func (t *CombatMachine) Variants() []*CombatPhaseVariant {
    return t.variants
}

func (t *CombatMachine) Combos() []*CombatCombo {
    return t.combos
}

func (t *CombatMachine) Markers() []*CombatMarker {
    return t.markers
}

func (t *CombatMachine) Anchor() *CombatAnchor {
    return t.anchor
}

func (t *CombatMachine) Anchors() []*CombatAnchorUse {
    return t.anchors
}

func (t *CombatMachine) OnExit() *CombatExit {
    return t.onExit
}

func (t *CombatMachine) Initial() string {
    return t.initial
}

func (t *CombatMachine) Impact() int32 {
    return t.impact
}

func (t *CombatMachine) Frame() int64 {
    return t.frame
}

func (t *CombatMachine) Stance() bool {
    return t.stance
}

func (t *CombatMachine) Variant() *CombatPhaseKey {
    return t.variant
}

func (t *CombatMachine) Weapon() string {
    return t.weapon
}

func (t *CombatMachine) Tier() int32 {
    return t.tier
}

func (t *CombatMachine) Airborne() bool {
    return t.airborne
}

func (t *CombatMachine) RefInitial() *CombatPhase {
    return t.refInitial
}
func (t *CombatMachine) RefImpact() *CombatImpact {
    return t.refImpact
}
func (t *CombatMachine) RefFrame() *CombatFrame {
    return t.refFrame
}
func (t *CombatMachine) RefStance() *CombatStance {
    return t.refStance
}
func (t *CombatMachine) RefVariant() *CombatPhaseVariant {
    return t.refVariant
}
func (t *CombatMachine) RefCombo() *CombatCombo {
    return t.refCombo
}
