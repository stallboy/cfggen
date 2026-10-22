package config.combat;

import configgen.genjava.LoadValueErrs;

public class Machine {
    private String code;
    private java.util.List<config.combat.Phase> phases;
    private java.util.List<config.combat.Impact> impacts;
    private java.util.List<config.combat.Frame> frames;
    private java.util.List<config.combat.Stance> stances;
    private java.util.List<config.combat.PhaseVariant> variants;
    private java.util.List<config.combat.Combo> combos;
    private java.util.List<config.combat.Marker> markers;
    private config.combat.Anchor anchor;
    private java.util.List<config.combat.AnchorUse> anchors;
    private config.combat.Exit onExit;
    private String initial;
    private int impact;
    private long frame;
    private boolean stance;
    private config.combat.PhaseKey variant;
    private String weapon;
    private int tier;
    private boolean airborne;
    private config.combat.Phase RefInitial;
    private config.combat.Impact RefImpact;
    private config.combat.Frame RefFrame;
    private config.combat.Stance RefStance;
    private config.combat.PhaseVariant RefVariant;
    private config.combat.Combo RefCombo;

    public void _bindRefInitial(config.combat.Phase value) {
        this.RefInitial = value;
    }
    public void _bindRefImpact(config.combat.Impact value) {
        this.RefImpact = value;
    }
    public void _bindRefFrame(config.combat.Frame value) {
        this.RefFrame = value;
    }
    public void _bindRefStance(config.combat.Stance value) {
        this.RefStance = value;
    }
    public void _bindRefVariant(config.combat.PhaseVariant value) {
        this.RefVariant = value;
    }
    public void _bindRefCombo(config.combat.Combo value) {
        this.RefCombo = value;
    }
    private Machine() {
    }

    public Machine(String code, java.util.List<config.combat.Phase> phases, java.util.List<config.combat.Impact> impacts, java.util.List<config.combat.Frame> frames, java.util.List<config.combat.Stance> stances, java.util.List<config.combat.PhaseVariant> variants, java.util.List<config.combat.Combo> combos, java.util.List<config.combat.Marker> markers, config.combat.Anchor anchor, java.util.List<config.combat.AnchorUse> anchors, config.combat.Exit onExit, String initial, int impact, long frame, boolean stance, config.combat.PhaseKey variant, String weapon, int tier, boolean airborne) {
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

    public static Machine _create(configgen.genjava.ConfigInput input) {
        Machine self = new Machine();
        self.code = input.readStringInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.phases = java.util.Collections.emptyList();
            } else {
                self.phases = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.phases.add(config.combat.Phase._create(input));
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
                    self.impacts.add(config.combat.Impact._create(input));
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
                    self.frames.add(config.combat.Frame._create(input));
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
                    self.stances.add(config.combat.Stance._create(input));
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
                    self.variants.add(config.combat.PhaseVariant._create(input));
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
                    self.combos.add(config.combat.Combo._create(input));
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
                    self.markers.add(config.combat.Marker._create(input));
                }
                self.markers = java.util.Collections.unmodifiableList(self.markers);
            }
        }
        self.anchor = config.combat.Anchor._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.anchors = java.util.Collections.emptyList();
            } else {
                self.anchors = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.anchors.add(config.combat.AnchorUse._create(input));
                }
                self.anchors = java.util.Collections.unmodifiableList(self.anchors);
            }
        }
        self.onExit = config.combat.Exit._create(input);
        self.initial = input.readStringInPool();
        self.impact = input.readInt();
        self.frame = input.readLong();
        self.stance = input.readBool();
        self.variant = config.combat.PhaseKey._create(input);
        self.weapon = input.readStringInPool();
        self.tier = input.readInt();
        self.airborne = input.readBool();
        return self;
    }

    public String getCode() {
        return code;
    }

    public java.util.List<config.combat.Phase> getPhases() {
        return phases;
    }

    public java.util.List<config.combat.Impact> getImpacts() {
        return impacts;
    }

    public java.util.List<config.combat.Frame> getFrames() {
        return frames;
    }

    public java.util.List<config.combat.Stance> getStances() {
        return stances;
    }

    public java.util.List<config.combat.PhaseVariant> getVariants() {
        return variants;
    }

    public java.util.List<config.combat.Combo> getCombos() {
        return combos;
    }

    /**
     * 未被引用也必须保持唯一。
     */
    public java.util.List<config.combat.Marker> getMarkers() {
        return markers;
    }

    public config.combat.Anchor getAnchor() {
        return anchor;
    }

    public java.util.List<config.combat.AnchorUse> getAnchors() {
        return anchors;
    }

    public config.combat.Exit getOnExit() {
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

    public config.combat.PhaseKey getVariant() {
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

    public config.combat.Phase refInitial() {
        return RefInitial;
    }

    public config.combat.Impact refImpact() {
        return RefImpact;
    }

    public config.combat.Frame refFrame() {
        return RefFrame;
    }

    public config.combat.Stance refStance() {
        return RefStance;
    }

    public config.combat.PhaseVariant refVariant() {
        return RefVariant;
    }

    public config.combat.Combo refCombo() {
        return RefCombo;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(code, phases, impacts, frames, stances, variants, combos, markers, anchor, anchors, onExit, initial, impact, frame, stance, variant, weapon, tier, airborne);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Machine o)) {
            return false;
        }
        return code.equals(o.code) && phases.equals(o.phases) && impacts.equals(o.impacts) && frames.equals(o.frames) && stances.equals(o.stances) && variants.equals(o.variants) && combos.equals(o.combos) && markers.equals(o.markers) && anchor.equals(o.anchor) && anchors.equals(o.anchors) && onExit.equals(o.onExit) && initial.equals(o.initial) && impact == o.impact && frame == o.frame && stance == o.stance && variant.equals(o.variant) && weapon.equals(o.weapon) && tier == o.tier && airborne == o.airborne;
    }

    @Override
    public String toString() {
        return "(" + code + "," + phases + "," + impacts + "," + frames + "," + stances + "," + variants + "," + combos + "," + markers + "," + anchor + "," + anchors + "," + onExit + "," + initial + "," + impact + "," + frame + "," + stance + "," + variant + "," + weapon + "," + tier + "," + airborne + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.Phase e : this.phases) {
            e._resolve(mgr, errors);
        }
        for (config.combat.AnchorUse e : this.anchors) {
            e._resolve(mgr, errors);
        }

    }

}
