package config

type CombatCommand interface{}

func createCombatCommand(stream *Stream) CombatCommand {
    var typeName = stream.ReadStringInPool()
    switch typeName {
    case "StateMachine":
        return createCombatCommandStateMachine(stream)
    case "Jump":
        return createCombatCommandJump(stream)
    case "Noop":
        return createCombatCommandNoop(stream)
    }
    panic("unexpected CombatCommand type: " + typeName)
}
