package config

type TaskCompletecondition interface{}

func createTaskCompletecondition(stream *Stream) TaskCompletecondition {
    var typeName = stream.ReadString()
    switch typeName {
    case "KillMonster":
        return createTaskCompleteconditionKillMonster(stream)
    case "TalkNpc":
        return createTaskCompleteconditionTalkNpc(stream)
    case "TestNoColumn":
        return createTaskCompleteconditionTestNoColumn(stream)
    case "Chat":
        return createTaskCompleteconditionChat(stream)
    case "ConditionAnd":
        return createTaskCompleteconditionConditionAnd(stream)
    case "CollectItem":
        return createTaskCompleteconditionCollectItem(stream)
    default:
        panic("unexpected TaskCompletecondition type: " + typeName)
    }
}
