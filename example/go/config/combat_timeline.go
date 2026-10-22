package config

import "fmt"

type CombatTimeline struct {
    id int32
    events map[*CombatPhaseKey]CombatEffect
    channels map[string]CombatEffect
    slots map[int32]CombatEffect
    clock map[int64]CombatEffect
    modes map[bool]CombatEffect
    eventKey *CombatPhaseKey
    channel string
    slot int32
    frame int64
    airborne bool
    fallback CombatEffect
    refEventKey CombatEffect
    refChannel CombatEffect
    refSlot CombatEffect
    refFrame CombatEffect
    refAirborne CombatEffect
    refDefault CombatEffect
}

func createCombatTimeline(stream *Stream) *CombatTimeline {
    v := &CombatTimeline{}
    v.id = stream.ReadInt32()
    eventsSize := stream.ReadCount(0)
    v.events = make(map[*CombatPhaseKey]CombatEffect, eventsSize)
    for i := 0; i < eventsSize; i++ {
        var k = createCombatPhaseKey(stream)
        if _, exists := v.events[k]; exists { panic("duplicate map key: combat.timeline.events") }
        v.events[k] = createCombatEffect(stream)
    }
    channelsSize := stream.ReadCount(0)
    v.channels = make(map[string]CombatEffect, channelsSize)
    for i := 0; i < channelsSize; i++ {
        var k = stream.ReadStringInPool()
        if _, exists := v.channels[k]; exists { panic("duplicate map key: combat.timeline.channels") }
        v.channels[k] = createCombatEffect(stream)
    }
    slotsSize := stream.ReadCount(0)
    v.slots = make(map[int32]CombatEffect, slotsSize)
    for i := 0; i < slotsSize; i++ {
        var k = stream.ReadInt32()
        if _, exists := v.slots[k]; exists { panic("duplicate map key: combat.timeline.slots") }
        v.slots[k] = createCombatEffect(stream)
    }
    clockSize := stream.ReadCount(0)
    v.clock = make(map[int64]CombatEffect, clockSize)
    for i := 0; i < clockSize; i++ {
        var k = stream.ReadInt64()
        if _, exists := v.clock[k]; exists { panic("duplicate map key: combat.timeline.clock") }
        v.clock[k] = createCombatEffect(stream)
    }
    modesSize := stream.ReadCount(0)
    v.modes = make(map[bool]CombatEffect, modesSize)
    for i := 0; i < modesSize; i++ {
        var k = stream.ReadBool()
        if _, exists := v.modes[k]; exists { panic("duplicate map key: combat.timeline.modes") }
        v.modes[k] = createCombatEffect(stream)
    }
    v.eventKey = createCombatPhaseKey(stream)
    v.channel = stream.ReadStringInPool()
    v.slot = stream.ReadInt32()
    v.frame = stream.ReadInt64()
    v.airborne = stream.ReadBool()
    v.fallback = createCombatEffect(stream)
    return v
}

func (t *CombatTimeline) String() string {
    return fmt.Sprintf("CombatTimeline{id=%v, events=%v, channels=%v, slots=%v, clock=%v, modes=%v, eventKey=%v, channel=%v, slot=%v, frame=%v, airborne=%v, fallback=%v}", t.id, t.events, t.channels, t.slots, t.clock, t.modes, t.eventKey, t.channel, t.slot, t.frame, t.airborne, t.fallback)
}

//getters
func (t *CombatTimeline) Id() int32 {
    return t.id
}

func (t *CombatTimeline) Events() map[*CombatPhaseKey]CombatEffect {
    return t.events
}

func (t *CombatTimeline) Channels() map[string]CombatEffect {
    return t.channels
}

func (t *CombatTimeline) Slots() map[int32]CombatEffect {
    return t.slots
}

func (t *CombatTimeline) Clock() map[int64]CombatEffect {
    return t.clock
}

func (t *CombatTimeline) Modes() map[bool]CombatEffect {
    return t.modes
}

func (t *CombatTimeline) EventKey() *CombatPhaseKey {
    return t.eventKey
}

func (t *CombatTimeline) Channel() string {
    return t.channel
}

func (t *CombatTimeline) Slot() int32 {
    return t.slot
}

func (t *CombatTimeline) Frame() int64 {
    return t.frame
}

func (t *CombatTimeline) Airborne() bool {
    return t.airborne
}

func (t *CombatTimeline) Fallback() CombatEffect {
    return t.fallback
}

func (t *CombatTimeline) RefEventKey() CombatEffect {
    return t.refEventKey
}
func (t *CombatTimeline) RefChannel() CombatEffect {
    return t.refChannel
}
func (t *CombatTimeline) RefSlot() CombatEffect {
    return t.refSlot
}
func (t *CombatTimeline) RefFrame() CombatEffect {
    return t.refFrame
}
func (t *CombatTimeline) RefAirborne() CombatEffect {
    return t.refAirborne
}
func (t *CombatTimeline) RefDefault() CombatEffect {
    return t.refDefault
}
type CombatTimelineMgr struct {
    all []*CombatTimeline
    idMap map[int32]*CombatTimeline
}

func(t *CombatTimelineMgr) GetAll() []*CombatTimeline {
    return t.all
}

func(t *CombatTimelineMgr) Get(id int32) *CombatTimeline {
    return t.idMap[id]
}

func (t *CombatTimelineMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*CombatTimeline, 0, cnt)
    t.idMap = make(map[int32]*CombatTimeline, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createCombatTimeline(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
    }
}
