package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgMachine {
    private String code;
    private java.util.List<config.combat.CfgPhase> phases;
    private java.util.List<config.combat.CfgImpact> impacts;
    private java.util.List<config.combat.CfgFrame> frames;
    private java.util.List<config.combat.CfgStance> stances;
    private java.util.List<config.combat.CfgPhaseVariant> variants;
    private java.util.List<config.combat.CfgCombo> combos;
    private java.util.List<config.combat.CfgMarker> markers;
    private config.combat.CfgAnchor anchor;
    private java.util.List<config.combat.CfgAnchorUse> anchors;
    private config.combat.CfgExit onExit;
    private String initial;
    private int impact;
    private long frame;
    private boolean stance;
    private config.combat.CfgPhaseKey variant;
    private String weapon;
    private int tier;
    private boolean airborne;
    private config.combat.CfgPhase RefInitial;
    private config.combat.CfgImpact RefImpact;
    private config.combat.CfgFrame RefFrame;
    private config.combat.CfgStance RefStance;
    private config.combat.CfgPhaseVariant RefVariant;
    private config.combat.CfgCombo RefCombo;

    public void _bindRefInitial(config.combat.CfgPhase value) {
        this.RefInitial = value;
    }
    public void _bindRefImpact(config.combat.CfgImpact value) {
        this.RefImpact = value;
    }
    public void _bindRefFrame(config.combat.CfgFrame value) {
        this.RefFrame = value;
    }
    public void _bindRefStance(config.combat.CfgStance value) {
        this.RefStance = value;
    }
    public void _bindRefVariant(config.combat.CfgPhaseVariant value) {
        this.RefVariant = value;
    }
    public void _bindRefCombo(config.combat.CfgCombo value) {
        this.RefCombo = value;
    }
    private CfgMachine() {
    }

    public CfgMachine(String code, java.util.List<config.combat.CfgPhase> phases, java.util.List<config.combat.CfgImpact> impacts, java.util.List<config.combat.CfgFrame> frames, java.util.List<config.combat.CfgStance> stances, java.util.List<config.combat.CfgPhaseVariant> variants, java.util.List<config.combat.CfgCombo> combos, java.util.List<config.combat.CfgMarker> markers, config.combat.CfgAnchor anchor, java.util.List<config.combat.CfgAnchorUse> anchors, config.combat.CfgExit onExit, String initial, int impact, long frame, boolean stance, config.combat.CfgPhaseKey variant, String weapon, int tier, boolean airborne) {
        this.code = code;
        this.phases = phases;
        this.impacts = impacts;
        this.frames = frames;
        this.stances = stances;
        this.variants = variants;
        this.combos = combos;
        this.markers = markers;
        this.anchor = anchor;
        this.anchors = anchors;
        this.onExit = onExit;
        this.initial = initial;
        this.impact = impact;
        this.frame = frame;
        this.stance = stance;
        this.variant = variant;
        this.weapon = weapon;
        this.tier = tier;
        this.airborne = airborne;
    }

    public static CfgMachine _create(configgen.genjava.ConfigInput input) {
        CfgMachine self = new CfgMachine();
        self.code = input.readStringInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.phases = java.util.Collections.emptyList();
            } else {
                self.phases = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.phases.add(config.combat.CfgPhase._create(input));
                }
                self.phases = java.util.Collections.unmodifiableList(self.phases);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.impacts = java.util.Collections.emptyList();
            } else {
                self.impacts = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.impacts.add(config.combat.CfgImpact._create(input));
                }
                self.impacts = java.util.Collections.unmodifiableList(self.impacts);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.frames = java.util.Collections.emptyList();
            } else {
                self.frames = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.frames.add(config.combat.CfgFrame._create(input));
                }
                self.frames = java.util.Collections.unmodifiableList(self.frames);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.stances = java.util.Collections.emptyList();
            } else {
                self.stances = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.stances.add(config.combat.CfgStance._create(input));
                }
                self.stances = java.util.Collections.unmodifiableList(self.stances);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.variants = java.util.Collections.emptyList();
            } else {
                self.variants = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.variants.add(config.combat.CfgPhaseVariant._create(input));
                }
                self.variants = java.util.Collections.unmodifiableList(self.variants);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.combos = java.util.Collections.emptyList();
            } else {
                self.combos = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.combos.add(config.combat.CfgCombo._create(input));
                }
                self.combos = java.util.Collections.unmodifiableList(self.combos);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.markers = java.util.Collections.emptyList();
            } else {
                self.markers = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.markers.add(config.combat.CfgMarker._create(input));
                }
                self.markers = java.util.Collections.unmodifiableList(self.markers);
            }
        }
        self.anchor = config.combat.CfgAnchor._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.anchors = java.util.Collections.emptyList();
            } else {
                self.anchors = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.anchors.add(config.combat.CfgAnchorUse._create(input));
                }
                self.anchors = java.util.Collections.unmodifiableList(self.anchors);
            }
        }
        self.onExit = config.combat.CfgExit._create(input);
        self.initial = input.readStringInPool();
        self.impact = input.readInt();
        self.frame = input.readLong();
        self.stance = input.readBool();
        self.variant = config.combat.CfgPhaseKey._create(input);
        self.weapon = input.readStringInPool();
        self.tier = input.readInt();
        self.airborne = input.readBool();
        return self;
    }

    public String getCode() {
        return code;
    }

    public java.util.List<config.combat.CfgPhase> getPhases() {
        return phases;
    }

    public java.util.List<config.combat.CfgImpact> getImpacts() {
        return impacts;
    }

    public java.util.List<config.combat.CfgFrame> getFrames() {
        return frames;
    }

    public java.util.List<config.combat.CfgStance> getStances() {
        return stances;
    }

    public java.util.List<config.combat.CfgPhaseVariant> getVariants() {
        return variants;
    }

    public java.util.List<config.combat.CfgCombo> getCombos() {
        return combos;
    }

    /**
     * 未被引用也必须保持唯一。
     */
    public java.util.List<config.combat.CfgMarker> getMarkers() {
        return markers;
    }

    public config.combat.CfgAnchor getAnchor() {
        return anchor;
    }

    public java.util.List<config.combat.CfgAnchorUse> getAnchors() {
        return anchors;
    }

    public config.combat.CfgExit getOnExit() {
        return onExit;
    }

    public String getInitial() {
        return initial;
    }

    public int getImpact() {
        return impact;
    }

    public long getFrame() {
        return frame;
    }

    public boolean getStance() {
        return stance;
    }

    public config.combat.CfgPhaseKey getVariant() {
        return variant;
    }

    public String getWeapon() {
        return weapon;
    }

    public int getTier() {
        return tier;
    }

    public boolean getAirborne() {
        return airborne;
    }

    public config.combat.CfgPhase refInitial() {
        return RefInitial;
    }

    public config.combat.CfgImpact refImpact() {
        return RefImpact;
    }

    public config.combat.CfgFrame refFrame() {
        return RefFrame;
    }

    public config.combat.CfgStance refStance() {
        return RefStance;
    }

    public config.combat.CfgPhaseVariant refVariant() {
        return RefVariant;
    }

    public config.combat.CfgCombo refCombo() {
        return RefCombo;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(code, phases, impacts, frames, stances, variants, combos, markers, anchor, anchors, onExit, initial, impact, frame, stance, variant, weapon, tier, airborne);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgMachine o)) {
            return false;
        }
        return code.equals(o.code) && phases.equals(o.phases) && impacts.equals(o.impacts) && frames.equals(o.frames) && stances.equals(o.stances) && variants.equals(o.variants) && combos.equals(o.combos) && markers.equals(o.markers) && anchor.equals(o.anchor) && anchors.equals(o.anchors) && onExit.equals(o.onExit) && initial.equals(o.initial) && impact == o.impact && frame == o.frame && stance == o.stance && variant.equals(o.variant) && weapon.equals(o.weapon) && tier == o.tier && airborne == o.airborne;
    }

    @Override
    public String toString() {
        return "(" + code + "," + phases + "," + impacts + "," + frames + "," + stances + "," + variants + "," + combos + "," + markers + "," + anchor + "," + anchors + "," + onExit + "," + initial + "," + impact + "," + frame + "," + stance + "," + variant + "," + weapon + "," + tier + "," + airborne + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.CfgPhase e : this.phases) {
            e._resolve(mgr, errors);
        }
        for (config.combat.CfgAnchorUse e : this.anchors) {
            e._resolve(mgr, errors);
        }

    }

}
