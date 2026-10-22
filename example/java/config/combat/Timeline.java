package config.combat;

import configgen.genjava.LoadValueErrs;

// CSV pack 映射：五种 map 键、多态 value、无键的 interface 固定终点。
// 来自：combat/timeline.csv
public class Timeline {
    private int id;
    private java.util.Map<config.combat.PhaseKey, config.combat.effect.Effect> events;
    private java.util.Map<String, config.combat.effect.Effect> channels;
    private java.util.Map<Integer, config.combat.effect.Effect> slots;
    private java.util.Map<Long, config.combat.effect.Effect> clock;
    private java.util.Map<Boolean, config.combat.effect.Effect> modes;
    private config.combat.PhaseKey eventKey;
    private String channel;
    private int slot;
    private long frame;
    private boolean airborne;
    private config.combat.effect.Effect fallback;
    private config.combat.effect.Effect RefEventKey;
    private config.combat.effect.Effect RefChannel;
    private config.combat.effect.Effect RefSlot;
    private config.combat.effect.Effect RefFrame;
    private config.combat.effect.Effect RefAirborne;
    private config.combat.effect.Effect RefDefault;

    public void _bindRefEventKey(config.combat.effect.Effect value) {
        this.RefEventKey = value;
    }
    public void _bindRefChannel(config.combat.effect.Effect value) {
        this.RefChannel = value;
    }
    public void _bindRefSlot(config.combat.effect.Effect value) {
        this.RefSlot = value;
    }
    public void _bindRefFrame(config.combat.effect.Effect value) {
        this.RefFrame = value;
    }
    public void _bindRefAirborne(config.combat.effect.Effect value) {
        this.RefAirborne = value;
    }
    public void _bindRefDefault(config.combat.effect.Effect value) {
        this.RefDefault = value;
    }
    private Timeline() {
    }

    public static Timeline _create(configgen.genjava.ConfigInput input) {
        Timeline self = new Timeline();
        self.id = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.events = java.util.Collections.emptyMap();
            } else {
                self.events = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = config.combat.PhaseKey._create(input);
                    var value = config.combat.effect.Effect._create(input);
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
                    var value = config.combat.effect.Effect._create(input);
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
                    var value = config.combat.effect.Effect._create(input);
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
                    var value = config.combat.effect.Effect._create(input);
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
                    var value = config.combat.effect.Effect._create(input);
                    if (self.modes.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("combat.timeline.modes", key);
                    }
                }
                self.modes = java.util.Collections.unmodifiableMap(self.modes);
            }
        }
        self.eventKey = config.combat.PhaseKey._create(input);
        self.channel = input.readStringInPool();
        self.slot = input.readInt();
        self.frame = input.readLong();
        self.airborne = input.readBool();
        self.fallback = config.combat.effect.Effect._create(input);
        return self;
    }

    public int getId() {
        return id;
    }

    public java.util.Map<config.combat.PhaseKey, config.combat.effect.Effect> getEvents() {
        return events;
    }

    public java.util.Map<String, config.combat.effect.Effect> getChannels() {
        return channels;
    }

    public java.util.Map<Integer, config.combat.effect.Effect> getSlots() {
        return slots;
    }

    public java.util.Map<Long, config.combat.effect.Effect> getClock() {
        return clock;
    }

    public java.util.Map<Boolean, config.combat.effect.Effect> getModes() {
        return modes;
    }

    public config.combat.PhaseKey getEventKey() {
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

    public config.combat.effect.Effect getFallback() {
        return fallback;
    }

    public config.combat.effect.Effect refEventKey() {
        return RefEventKey;
    }

    public config.combat.effect.Effect refChannel() {
        return RefChannel;
    }

    public config.combat.effect.Effect refSlot() {
        return RefSlot;
    }

    public config.combat.effect.Effect refFrame() {
        return RefFrame;
    }

    public config.combat.effect.Effect refAirborne() {
        return RefAirborne;
    }

    public config.combat.effect.Effect refDefault() {
        return RefDefault;
    }

    @Override
    public String toString() {
        return "(" + id + "," + events + "," + channels + "," + slots + "," + clock + "," + modes + "," + eventKey + "," + channel + "," + slot + "," + frame + "," + airborne + "," + fallback + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

    public static Timeline get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatTimeline(id);
    }

    public static java.util.Collection<Timeline> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatTimeline();
    }


}
