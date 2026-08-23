package config

type AiTriggerTick interface{}

func createAiTriggerTick(stream *Stream) AiTriggerTick {
    var typeName = stream.ReadStringInPool()
    switch typeName {
    case "ConstValue":
        return createAiTriggerTickConstValue(stream)
    case "ByLevel":
        return createAiTriggerTickByLevel(stream)
    case "ByServerUpDay":
        return createAiTriggerTickByServerUpDay(stream)
    default:
        panic("unexpected AiTriggerTick type: " + typeName)
    }
}
