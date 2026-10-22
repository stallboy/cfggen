package config.combat;

import configgen.genjava.LoadValueErrs;

// CSV pack 映射：五种 map 键、多态 value、无键的 interface 固定终点。
// 来自：combat/timeline.csv
public class CfgTimeline {
    private int id;
    private java.util.Map<config.combat.CfgPhaseKey, config.combat.effect.CfgEffect> events;
    private java.util.Map<String, config.combat.effect.CfgEffect> channels;
    private java.util.Map<Integer, config.combat.effect.CfgEffect> slots;
    private java.util.Map<Long, config.combat.effect.CfgEffect> clock;
    private java.util.Map<Boolean, config.combat.effect.CfgEffect> modes;
    private config.combat.CfgPhaseKey eventKey;
    private String channel;
    private int slot;
    private long frame;
    private boolean airborne;
    private config.combat.effect.CfgEffect fallback;
    private config.combat.effect.CfgEffect RefEventKey;
    private config.combat.effect.CfgEffect RefChannel;
    private config.combat.effect.CfgEffect RefSlot;
    private config.combat.effect.CfgEffect RefFrame;
    private config.combat.effect.CfgEffect RefAirborne;
    private config.combat.effect.CfgEffect RefDefault;

    public void _bindRefEventKey(config.combat.effect.CfgEffect value) {
        this.RefEventKey = value;
    }
    public void _bindRefChannel(config.combat.effect.CfgEffect value) {
        this.RefChannel = value;
    }
    public void _bindRefSlot(config.combat.effect.CfgEffect value) {
        this.RefSlot = value;
    }
    public void _bindRefFrame(config.combat.effect.CfgEffect value) {
        this.RefFrame = value;
    }
    public void _bindRefAirborne(config.combat.effect.CfgEffect value) {
        this.RefAirborne = value;
    }
    public void _bindRefDefault(config.combat.effect.CfgEffect value) {
        this.RefDefault = value;
    }
    private CfgTimeline() {
    }

    public static CfgTimeline _create(configgen.genjava.ConfigInput input) {
        CfgTimeline self = new CfgTimeline();
        self.id = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.events = java.util.Collections.emptyMap();
            } else {
                self.events = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = config.combat.CfgPhaseKey._create(input);
                    var value = config.combat.effect.CfgEffect._create(input);
                    if (self.events.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("combat.timeline.events", key);
                    }
                }
                self.events = java.util.Collections.unmodifiableMap(self.events);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.channels = java.util.Collections.emptyMap();
            } else {
                self.channels = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readStringInPool();
                    var value = config.combat.effect.CfgEffect._create(input);
                    if (self.channels.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("combat.timeline.channels", key);
                    }
                }
                self.channels = java.util.Collections.unmodifiableMap(self.channels);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.slots = java.util.Collections.emptyMap();
            } else {
                self.slots = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = config.combat.effect.CfgEffect._create(input);
                    if (self.slots.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("combat.timeline.slots", key);
                    }
                }
                self.slots = java.util.Collections.unmodifiableMap(self.slots);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.clock = java.util.Collections.emptyMap();
            } else {
                self.clock = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readLong();
                    var value = config.combat.effect.CfgEffect._create(input);
                    if (self.clock.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("combat.timeline.clock", key);
                    }
                }
                self.clock = java.util.Collections.unmodifiableMap(self.clock);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.modes = java.util.Collections.emptyMap();
            } else {
                self.modes = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readBool();
                    var value = config.combat.effect.CfgEffect._create(input);
                    if (self.modes.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("combat.timeline.modes", key);
                    }
                }
                self.modes = java.util.Collections.unmodifiableMap(self.modes);
            }
        }
        self.eventKey = config.combat.CfgPhaseKey._create(input);
        self.channel = input.readStringInPool();
        self.slot = input.readInt();
        self.frame = input.readLong();
        self.airborne = input.readBool();
        self.fallback = config.combat.effect.CfgEffect._create(input);
        return self;
    }

    public int getId() {
        return id;
    }

    public java.util.Map<config.combat.CfgPhaseKey, config.combat.effect.CfgEffect> getEvents() {
        return events;
    }

    public java.util.Map<String, config.combat.effect.CfgEffect> getChannels() {
        return channels;
    }

    public java.util.Map<Integer, config.combat.effect.CfgEffect> getSlots() {
        return slots;
    }

    public java.util.Map<Long, config.combat.effect.CfgEffect> getClock() {
        return clock;
    }

    public java.util.Map<Boolean, config.combat.effect.CfgEffect> getModes() {
        return modes;
    }

    public config.combat.CfgPhaseKey getEventKey() {
        return eventKey;
    }

    public String getChannel() {
        return channel;
    }

    public int getSlot() {
        return slot;
    }

    public long getFrame() {
        return frame;
    }

    public boolean getAirborne() {
        return airborne;
    }

    public config.combat.effect.CfgEffect getFallback() {
        return fallback;
    }

    public config.combat.effect.CfgEffect refEventKey() {
        return RefEventKey;
    }

    public config.combat.effect.CfgEffect refChannel() {
        return RefChannel;
    }

    public config.combat.effect.CfgEffect refSlot() {
        return RefSlot;
    }

    public config.combat.effect.CfgEffect refFrame() {
        return RefFrame;
    }

    public config.combat.effect.CfgEffect refAirborne() {
        return RefAirborne;
    }

    public config.combat.effect.CfgEffect refDefault() {
        return RefDefault;
    }

    @Override
    public String toString() {
        return "(" + id + "," + events + "," + channels + "," + slots + "," + clock + "," + modes + "," + eventKey + "," + channel + "," + slot + "," + frame + "," + airborne + "," + fallback + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

    public static CfgTimeline get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatTimeline(id);
    }

    public static java.util.Collection<CfgTimeline> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatTimeline();
    }


}
