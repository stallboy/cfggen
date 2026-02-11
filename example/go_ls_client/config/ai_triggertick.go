package config

type AiTriggerTick interface{}

func createAiTriggerTick(stream *Stream) AiTriggerTick {
    var typeName = stream.ReadStringInPool()
    switch typeName {
    case "ConstValue":
        return createAiTriggertickConstValue(stream)
    case "ByLevel":
        return createAiTriggertickByLevel(stream)
    case "ByServerUpDay":
        return createAiTriggertickByServerUpDay(stream)
    default:
        panic("unexpected AiTriggerTick type: " + typeName)
    }
}
