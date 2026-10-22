package config.combat.command;

import configgen.genjava.LoadValueErrs;

public sealed interface CfgCommand permits CfgStateMachine, CfgJump, CfgNoop {
    default void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
    }

    static CfgCommand _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "StateMachine" -> config.combat.command.CfgStateMachine._create(input);
            case "Jump" -> config.combat.command.CfgJump._create(input);
            case "Noop" -> config.combat.command.CfgNoop._create(input);
            default -> throw input.errors().unknownVariant("CfgCommand", tag);
        };
    }
}
