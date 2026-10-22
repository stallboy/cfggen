package config.combat.command;

import configgen.genjava.LoadValueErrs;

public sealed interface Command permits StateMachine, Jump, Noop {
    default void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
    }

    static Command _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "StateMachine" -> config.combat.command.StateMachine._create(input);
            case "Jump" -> config.combat.command.Jump._create(input);
            case "Noop" -> config.combat.command.Noop._create(input);
            default -> throw input.errors().unknownVariant("Command", tag);
        };
    }
}
