package config;

import configgen.genjava.ConfigLoader;
import configgen.genjava.ConfigPackage;
import configgen.genjava.LoadValueErrs;
import configgen.genjava.RelationResolver;
import java.util.*;

public class ConfigMgrLoader {
    private static final java.util.Set<String> TABLES = java.util.Set.of("ai.ai", "ai.ai_action", "ai.ai_condition", "combat.ability", "combat.branchreward", "combat.cast", "combat.encounter", "combat.phasebook", "combat.timeline", "equip.ability", "equip.equip_config", "equip.jewelry", "equip.jewelry_random", "equip.jewelry_suit", "equip.jewelry_type", "equip.rank", "other.ArgCaptureMode", "other.drop", "other.key_test", "other.loot", "other.loot_item", "other.monster", "other.sign_in", "task.complete_condition_type", "task.task", "task.task2", "task.task_extra_exp");
    private static final String[] LANGUAGES = { "zh_cn", "en", "tw" };
    public static ConfigMgr loadPackagesAndSetMgr(ConfigPackage... packages) {
        ConfigMgr config = loadPackages(packages);
        ConfigMgr.setMgr(config);
        return config;
    }

    public static ConfigMgr loadPackages(ConfigPackage... packages) {
        ConfigPackage.validateOrder(List.of(packages));
        var errors = new LoadValueErrs();
        var expected = ConfigCodeSchema.getPackageSchema();
        for (var pack : packages) {
            pack.validateSchema(expected, errors);
            for (String block : pack.blockNames()) {
                if (block.startsWith(".") && !TABLES.contains(block.substring(1))) {
                    errors.unknownTableBlock(pack.name(), block);
                }
            }
        }
        errors.checkErrors();
        ConfigMgr mgr = new ConfigMgr();
        var loaders = newLoaders(mgr, errors);
        for (var pack : packages) {
            String packName = pack.name();
            for (String block : pack.blockNames()) {
                if (!block.startsWith(".")) {
                    continue;
                }
                String table = block.substring(1);
                var loader = loaders.get(table);
                if (loader != null) {
                    loader.read(pack.block(block, LANGUAGES), packName, pack == packages[0]);
                }
            }
        }
        return finish(mgr, loaders, errors);
    }

    private static ConfigMgr finish(ConfigMgr mgr, Map<String, ConfigLoader<?, ?>> loaders, LoadValueErrs errors) {
        for (var entry : loaders.entrySet()) {
            mgr.setOrigins(entry.getKey(), entry.getValue().origins());
            entry.getValue().index();
        }
        errors.checkErrors();
        config.equip.CfgEquip_config_Entry.setAllRefs(mgr, errors);
        config.equip.CfgJewelry_suit_Entry.setAllRefs(mgr, errors);
        config.equip.CfgRank.setAllRefs(mgr, errors);
        config.other.CfgArgCaptureMode.setAllRefs(mgr, errors);
        for (var loader : loaders.values()) {
            loader.resolveAll();
        }
        resolveRelations(mgr, errors);
        errors.checkErrors();
        mgr.markReady();
        return mgr;
    }

    private static Map<String, ConfigLoader<?, ?>> newLoaders(ConfigMgr mgr, LoadValueErrs errors) {
        var loaders = new LinkedHashMap<String, ConfigLoader<?, ?>>();
        loaders.put("ai.ai", new ConfigLoader<>("ai.ai", errors, config.ai.CfgAi::_create,
                config.ai.CfgAi::getID, mgr::_set_ai_ai_All, null));
        loaders.put("ai.ai_action", new ConfigLoader<>("ai.ai_action", errors, config.ai.CfgAi_action::_create,
                config.ai.CfgAi_action::getID, mgr::_set_ai_ai_action_All, null));
        loaders.put("ai.ai_condition", new ConfigLoader<>("ai.ai_condition", errors, config.ai.CfgAi_condition::_create,
                config.ai.CfgAi_condition::getID, mgr::_set_ai_ai_condition_All, null));
        loaders.put("combat.ability", new ConfigLoader<>("combat.ability", errors, config.combat.CfgAbility::_create,
                config.combat.CfgAbility::getId, rows -> config.combat.CfgAbility._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("combat.branchreward", new ConfigLoader<>("combat.branchreward", errors, config.combat.CfgBranchreward::_create,
                config.combat.CfgBranchreward::getKey, mgr::_set_combat_branchreward_All, null));
        loaders.put("combat.cast", new ConfigLoader<>("combat.cast", errors, config.combat.CfgCast::_create,
                config.combat.CfgCast::getId, mgr::_set_combat_cast_All, row -> row._resolve(mgr, errors)));
        loaders.put("combat.encounter", new ConfigLoader<>("combat.encounter", errors, config.combat.CfgEncounter::_create,
                config.combat.CfgEncounter::getId, mgr::_set_combat_encounter_All, row -> row._resolve(mgr, errors)));
        loaders.put("combat.phasebook", new ConfigLoader<>("combat.phasebook", errors, config.combat.CfgPhasebook::_create,
                row -> new config.combat.CfgPhasebook.SchoolTierAirborneKey(row.getSchool(), row.getTier(), row.getAirborne()), rows -> config.combat.CfgPhasebook._index(mgr, rows, errors), null));
        loaders.put("combat.timeline", new ConfigLoader<>("combat.timeline", errors, config.combat.CfgTimeline::_create,
                config.combat.CfgTimeline::getId, mgr::_set_combat_timeline_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.equip_config", new ConfigLoader<>("equip.equip_config", errors, config.equip.CfgEquip_config::_create,
                config.equip.CfgEquip_config::getEntry, mgr::_set_equip_equip_config_All, null));
        loaders.put("equip.jewelry", new ConfigLoader<>("equip.jewelry", errors, config.equip.CfgJewelry::_create,
                config.equip.CfgJewelry::getID, mgr::_set_equip_jewelry_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_random", new ConfigLoader<>("equip.jewelry_random", errors, config.equip.CfgJewelry_random::_create,
                config.equip.CfgJewelry_random::getLvlRank, mgr::_set_equip_jewelry_random_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_suit", new ConfigLoader<>("equip.jewelry_suit", errors, config.equip.CfgJewelry_suit::_create,
                config.equip.CfgJewelry_suit::getSuitID, mgr::_set_equip_jewelry_suit_All, null));
        loaders.put("equip.rank", new ConfigLoader<>("equip.rank", errors, config.equip.CfgRank_Detail::_create,
                config.equip.CfgRank_Detail::getRankID, rows -> config.equip.CfgRank_Detail._index(mgr, rows, errors), null));
        loaders.put("other.ArgCaptureMode", new ConfigLoader<>("other.ArgCaptureMode", errors, config.other.CfgArgCaptureMode_Detail::_create,
                config.other.CfgArgCaptureMode_Detail::getName, rows -> config.other.CfgArgCaptureMode_Detail._index(mgr, rows, errors), null));
        loaders.put("other.drop", new ConfigLoader<>("other.drop", errors, config.other.CfgDrop::_create,
                config.other.CfgDrop::getDropid, mgr::_set_other_drop_All, null));
        loaders.put("other.key_test", new ConfigLoader<>("other.key_test", errors, config.other.CfgKey_test::_create,
                row -> new config.other.CfgKey_test.Id1Id2Key(row.getId1(), row.getId2()), rows -> config.other.CfgKey_test._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("other.loot", new ConfigLoader<>("other.loot", errors, config.other.CfgLoot::_create,
                config.other.CfgLoot::getLootid, mgr::_set_other_loot_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.loot_item", new ConfigLoader<>("other.loot_item", errors, config.other.CfgLoot_item::_create,
                row -> new config.other.CfgLoot_item.LootidItemidKey(row.getLootid(), row.getItemid()), mgr::_set_other_loot_item_All, null));
        loaders.put("other.monster", new ConfigLoader<>("other.monster", errors, config.other.CfgMonster::_create,
                config.other.CfgMonster::getId, mgr::_set_other_monster_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.sign_in", new ConfigLoader<>("other.sign_in", errors, config.other.CfgSign_in::_create,
                config.other.CfgSign_in::getId, mgr::_set_other_sign_in_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task", new ConfigLoader<>("task.task", errors, config.task.CfgTask::_create,
                config.task.CfgTask::getTaskid, mgr::_set_task_task_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task2", new ConfigLoader<>("task.task2", errors, config.task.CfgTask2::_create,
                config.task.CfgTask2::getTaskid, mgr::_set_task_task2_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task_extra_exp", new ConfigLoader<>("task.task_extra_exp", errors, config.task.CfgTask_extra_exp::_create,
                config.task.CfgTask_extra_exp::getTaskid, mgr::_set_task_task_extra_exp_All, null));
        return loaders;
    }

    private static void resolveRelations(ConfigMgr mgr, LoadValueErrs errors) {
        var resolver = new RelationResolver(errors,
            new RelationResolver.Type("LevelRank", config.CfgLevelRank.class,
                value -> { var row = (config.CfgLevelRank) value; return new Object[]{row.getLevel(), row.getRank()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("Position", config.CfgPosition.class,
                value -> { var row = (config.CfgPosition) value; return new Object[]{row.getX(), row.getY(), row.getZ()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("Range", config.CfgRange.class,
                value -> { var row = (config.CfgRange) value; return new Object[]{row.getMin(), row.getMax()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.TriggerTick.ByLevel", config.ai.triggertick.CfgByLevel.class,
                value -> { var row = (config.ai.triggertick.CfgByLevel) value; return new Object[]{row.getInit(), row.getCoefficient()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.TriggerTick.ByServerUpDay", config.ai.triggertick.CfgByServerUpDay.class,
                value -> { var row = (config.ai.triggertick.CfgByServerUpDay) value; return new Object[]{row.getInit(), row.getCoefficient1(), row.getCoefficient2()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.TriggerTick.ConstValue", config.ai.triggertick.CfgConstValue.class,
                value -> { var row = (config.ai.triggertick.CfgConstValue) value; return new Object[]{row.getValue()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.ai", config.ai.CfgAi.class,
                value -> { var row = (config.ai.CfgAi) value; return new Object[]{row.getID(), row.getDesc(), row.getCondID(), row.getTrigTick(), row.getTrigOdds(), row.getActionID(), row.getDeathRemove()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.ai_action", config.ai.CfgAi_action.class,
                value -> { var row = (config.ai.CfgAi_action) value; return new Object[]{row.getID(), row.getDesc(), row.getFormulaID(), row.getArgIList(), row.getArgSList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.ai_condition", config.ai.CfgAi_condition.class,
                value -> { var row = (config.ai.CfgAi_condition) value; return new Object[]{row.getID(), row.getDesc(), row.getFormulaID(), row.getArgIList(), row.getArgSList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.AbilityUse", config.combat.CfgAbilityUse.class,
                value -> { var row = (config.combat.CfgAbilityUse) value; return new Object[]{row.getBinding(), row.getMachines()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "ability", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{0, 0})}, "table:combat.ability", new int[]{0}, new RelationResolver.Step[]{})},
                (value, refs) -> {
                    var row = (config.combat.CfgAbilityUse) value;
                    row._bindRefAbility((config.combat.CfgAbility) refs[0]);
                }),
            new RelationResolver.Type("combat.Anchor", config.combat.CfgAnchor.class,
                value -> { var row = (config.combat.CfgAnchor) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.AnchorUse", config.combat.CfgAnchorUse.class,
                value -> { var row = (config.combat.CfgAnchorUse) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "anchor", false, new RelationResolver.Source[]{}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(8, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.CfgAnchorUse) value;
                    row._bindRefAnchor((config.combat.CfgAnchor) refs[0]);
                }),
            new RelationResolver.Type("combat.Binding", config.combat.CfgBinding.class,
                value -> { var row = (config.combat.CfgBinding) value; return new Object[]{row.getAbilityId()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.BookEntry", config.combat.CfgBookEntry.class,
                value -> { var row = (config.combat.CfgBookEntry) value; return new Object[]{row.getName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Combo", config.combat.CfgCombo.class,
                value -> { var row = (config.combat.CfgCombo) value; return new Object[]{row.getWeapon(), row.getTier(), row.getAirborne(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Command.Jump", config.combat.command.CfgJump.class,
                value -> { var row = (config.combat.command.CfgJump) value; return new Object[]{row.getKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.encounter", 0, "key", true, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{0})}, "ancestor:combat.Command.StateMachine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(0, 1, new int[]{0})})},
                (value, refs) -> {
                    var row = (config.combat.command.CfgJump) value;
                    row._bindNullableRefKey((config.combat.CfgNestedPhase) refs[0]);
                }),
            new RelationResolver.Type("combat.Command.Noop", config.combat.command.CfgNoop.class,
                value -> { var row = (config.combat.command.CfgNoop) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Command.StateMachine", config.combat.command.CfgStateMachine.class,
                value -> { var row = (config.combat.command.CfgStateMachine) value; return new Object[]{row.getPhases()}; },
                new RelationResolver.Index[]{new RelationResolver.Index(0, new int[]{0})},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Effect.Damage", config.combat.effect.CfgDamage.class,
                value -> { var row = (config.combat.effect.CfgDamage) value; return new Object[]{row.getAmount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Effect.Heal", config.combat.effect.CfgHeal.class,
                value -> { var row = (config.combat.effect.CfgHeal) value; return new Object[]{row.getAmount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Effect.None", config.combat.effect.CfgNone.class,
                value -> { var row = (config.combat.effect.CfgNone) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Exit", config.combat.CfgExit.class,
                value -> { var row = (config.combat.CfgExit) value; return new Object[]{row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Frame", config.combat.CfgFrame.class,
                value -> { var row = (config.combat.CfgFrame) value; return new Object[]{row.getTick(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Impact", config.combat.CfgImpact.class,
                value -> { var row = (config.combat.CfgImpact) value; return new Object[]{row.getOrdinal(), row.getDamage()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Machine", config.combat.CfgMachine.class,
                value -> { var row = (config.combat.CfgMachine) value; return new Object[]{row.getCode(), row.getPhases(), row.getImpacts(), row.getFrames(), row.getStances(), row.getVariants(), row.getCombos(), row.getMarkers(), row.getAnchor(), row.getAnchors(), row.getOnExit(), row.getInitial(), row.getImpact(), row.getFrame(), row.getStance(), row.getVariant(), row.getWeapon(), row.getTier(), row.getAirborne()}; },
                new RelationResolver.Index[]{new RelationResolver.Index(1, new int[]{0}), new RelationResolver.Index(2, new int[]{0}), new RelationResolver.Index(3, new int[]{0}), new RelationResolver.Index(4, new int[]{0}), new RelationResolver.Index(5, new int[]{0}), new RelationResolver.Index(6, new int[]{0, 1, 2}), new RelationResolver.Index(7, new int[]{0})},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "initial", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{11})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 1, "impact", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{12})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 2, "frame", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{13})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 3, "stance", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{14})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(4, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 4, "variant", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{15})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(5, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 5, "combo", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{16}), new RelationResolver.Source("", new int[]{17}), new RelationResolver.Source("", new int[]{18})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(6, 1, new int[]{0, 1, 2})})},
                (value, refs) -> {
                    var row = (config.combat.CfgMachine) value;
                    row._bindRefInitial((config.combat.CfgPhase) refs[0]);
                    row._bindRefImpact((config.combat.CfgImpact) refs[1]);
                    row._bindRefFrame((config.combat.CfgFrame) refs[2]);
                    row._bindRefStance((config.combat.CfgStance) refs[3]);
                    row._bindRefVariant((config.combat.CfgPhaseVariant) refs[4]);
                    row._bindRefCombo((config.combat.CfgCombo) refs[5]);
                }),
            new RelationResolver.Type("combat.MachineUse", config.combat.CfgMachineUse.class,
                value -> { var row = (config.combat.CfgMachineUse) value; return new Object[]{row.getMachineKey(), row.getTargets()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Marker", config.combat.CfgMarker.class,
                value -> { var row = (config.combat.CfgMarker) value; return new Object[]{row.getKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.NestedPhase", config.combat.CfgNestedPhase.class,
                value -> { var row = (config.combat.CfgNestedPhase) value; return new Object[]{row.getKey(), row.getAction()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.OptionalAim", config.combat.CfgOptionalAim.class,
                value -> { var row = (config.combat.CfgOptionalAim) value; return new Object[]{row.getAbilityId(), row.getMachineKey(), row.getPhaseKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "target", true, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{0}), new RelationResolver.Source("", new int[]{1}), new RelationResolver.Source("", new int[]{2})}, "table:combat.ability", new int[]{0}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})})},
                (value, refs) -> {
                    var row = (config.combat.CfgOptionalAim) value;
                    row._bindNullableRefTarget((config.combat.CfgPhase) refs[0]);
                }),
            new RelationResolver.Type("combat.Origin", config.combat.CfgOrigin.class,
                value -> { var row = (config.combat.CfgOrigin) value; return new Object[]{row.getX(), row.getY()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Phase", config.combat.CfgPhase.class,
                value -> { var row = (config.combat.CfgPhase) value; return new Object[]{row.getKey(), row.getNext()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "next", true, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{1})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 1, "exit", false, new RelationResolver.Source[]{}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(10, 0, new int[]{}), new RelationResolver.Step(0, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.CfgPhase) value;
                    row._bindNullableRefNext((config.combat.CfgPhase) refs[0]);
                    row._bindRefExit((config.combat.effect.CfgEffect) refs[1]);
                }),
            new RelationResolver.Type("combat.PhaseKey", config.combat.CfgPhaseKey.class,
                value -> { var row = (config.combat.CfgPhaseKey) value; return new Object[]{row.getName(), row.getVariant()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.PhaseUse", config.combat.CfgPhaseUse.class,
                value -> { var row = (config.combat.CfgPhaseUse) value; return new Object[]{row.getPhaseKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "target", false, new RelationResolver.Source[]{new RelationResolver.Source("ancestor:combat.AbilityUse", new int[]{0, 0}), new RelationResolver.Source("ancestor:combat.MachineUse", new int[]{0}), new RelationResolver.Source("", new int[]{0})}, "table:combat.ability", new int[]{0}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.cast", 1, "byCode", false, new RelationResolver.Source[]{new RelationResolver.Source("root", new int[]{1}), new RelationResolver.Source("ancestor:combat.MachineUse", new int[]{0}), new RelationResolver.Source("", new int[]{0})}, "table:combat.ability", new int[]{1}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.cast", 2, "origin", false, new RelationResolver.Source[]{}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.CfgPhaseUse) value;
                    row._bindRefTarget((config.combat.CfgPhase) refs[0]);
                    row._bindRefByCode((config.combat.CfgPhase) refs[1]);
                    row._bindRefOrigin((config.combat.CfgOrigin) refs[2]);
                }),
            new RelationResolver.Type("combat.PhaseVariant", config.combat.CfgPhaseVariant.class,
                value -> { var row = (config.combat.CfgPhaseVariant) value; return new Object[]{row.getKey(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Reward", config.combat.CfgReward.class,
                value -> { var row = (config.combat.CfgReward) value; return new Object[]{row.getCount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Stance", config.combat.CfgStance.class,
                value -> { var row = (config.combat.CfgStance) value; return new Object[]{row.getAirborne(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.ability", config.combat.CfgAbility.class,
                value -> { var row = (config.combat.CfgAbility) value; return new Object[]{row.getId(), row.getCode(), row.getOrigin(), row.getMachines(), row.getStartMachine(), row.getStartPhase()}; },
                new RelationResolver.Index[]{new RelationResolver.Index(3, new int[]{0})},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "initial", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{4}), new RelationResolver.Source("", new int[]{5})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 1, "spawn", false, new RelationResolver.Source[]{}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.CfgAbility) value;
                    row._bindRefInitial((config.combat.CfgPhase) refs[0]);
                    row._bindRefSpawn((config.combat.CfgOrigin) refs[1]);
                }),
            new RelationResolver.Type("combat.branchreward", config.combat.CfgBranchreward.class,
                value -> { var row = (config.combat.CfgBranchreward) value; return new Object[]{row.getKey(), row.getReward()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.cast", config.combat.CfgCast.class,
                value -> { var row = (config.combat.CfgCast) value; return new Object[]{row.getId(), row.getAbilityCode(), row.getOrigin(), row.getUses(), row.getOptional(), row.getSchool(), row.getTier(), row.getAirborne(), row.getBookCode(), row.getEdition(), row.getRewardKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "book", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{5}), new RelationResolver.Source("", new int[]{6}), new RelationResolver.Source("", new int[]{7})}, "table:combat.phasebook", new int[]{0, 1, 2}, new RelationResolver.Step[]{new RelationResolver.Step(5, 0, new int[]{})}),
new RelationResolver.Plan("combat.cast", 1, "bookByCode", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{8}), new RelationResolver.Source("", new int[]{9})}, "table:combat.phasebook", new int[]{3, 4}, new RelationResolver.Step[]{new RelationResolver.Step(5, 0, new int[]{})}),
new RelationResolver.Plan("combat.cast", 2, "bookRecord", false, new RelationResolver.Source[]{new RelationResolver.Source("root", new int[]{5}), new RelationResolver.Source("root", new int[]{6}), new RelationResolver.Source("root", new int[]{7})}, "table:combat.phasebook", new int[]{0, 1, 2}, new RelationResolver.Step[]{}),
new RelationResolver.Plan("combat.cast", 3, "reward", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{10})}, "table:combat.branchreward", new int[]{0}, new RelationResolver.Step[]{new RelationResolver.Step(1, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.CfgCast) value;
                    row._bindRefBook((config.combat.CfgBookEntry) refs[0]);
                    row._bindRefBookByCode((config.combat.CfgBookEntry) refs[1]);
                    row._bindRefBookRecord((config.combat.CfgPhasebook) refs[2]);
                    row._bindRefReward((config.combat.CfgReward) refs[3]);
                }),
            new RelationResolver.Type("combat.encounter", config.combat.CfgEncounter.class,
                value -> { var row = (config.combat.CfgEncounter) value; return new Object[]{row.getId(), row.getCommand()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.phasebook", config.combat.CfgPhasebook.class,
                value -> { var row = (config.combat.CfgPhasebook) value; return new Object[]{row.getSchool(), row.getTier(), row.getAirborne(), row.getCode(), row.getEdition(), row.getEntry()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.timeline", config.combat.CfgTimeline.class,
                value -> { var row = (config.combat.CfgTimeline) value; return new Object[]{row.getId(), row.getEvents(), row.getChannels(), row.getSlots(), row.getClock(), row.getModes(), row.getEventKey(), row.getChannel(), row.getSlot(), row.getFrame(), row.getAirborne(), row.getFallback()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.timeline", 0, "eventKey", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{6})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(1, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 1, "channel", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{7})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 2, "slot", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{8})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(3, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 3, "frame", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{9})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(4, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 4, "airborne", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{10})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(5, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 5, "default", false, new RelationResolver.Source[]{}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(11, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.CfgTimeline) value;
                    row._bindRefEventKey((config.combat.effect.CfgEffect) refs[0]);
                    row._bindRefChannel((config.combat.effect.CfgEffect) refs[1]);
                    row._bindRefSlot((config.combat.effect.CfgEffect) refs[2]);
                    row._bindRefFrame((config.combat.effect.CfgEffect) refs[3]);
                    row._bindRefAirborne((config.combat.effect.CfgEffect) refs[4]);
                    row._bindRefDefault((config.combat.effect.CfgEffect) refs[5]);
                }),
            new RelationResolver.Type("equip.TestPackBean", config.equip.CfgTestPackBean.class,
                value -> { var row = (config.equip.CfgTestPackBean) value; return new Object[]{row.getName(), row.getIRange()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.ability", config.equip.CfgAbility.class,
                value -> { var row = (config.equip.CfgAbility) value; return new Object[]{row.getId(), row.getName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.equip_config", config.equip.CfgEquip_config.class,
                value -> { var row = (config.equip.CfgEquip_config) value; return new Object[]{row.getEntry(), row.getStone_count_for_set(), row.getDraw_protect_name(), row.getBroadcastid(), row.getBroadcast_least_quality(), row.getWeek_reward_mailid()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry", config.equip.CfgJewelry.class,
                value -> { var row = (config.equip.CfgJewelry) value; return new Object[]{row.getID(), row.getName(), row.getIconFile(), row.getLvlRank(), row.getJType(), row.getSuitID(), row.getKeyAbility(), row.getKeyAbilityValue(), row.getSalePrice(), row.getDescription()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry_random", config.equip.CfgJewelry_random.class,
                value -> { var row = (config.equip.CfgJewelry_random) value; return new Object[]{row.getLvlRank(), row.getAttackRange(), row.getOtherRange(), row.getTestPack()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry_suit", config.equip.CfgJewelry_suit.class,
                value -> { var row = (config.equip.CfgJewelry_suit) value; return new Object[]{row.getSuitID(), row.getEname(), row.getName(), row.getAbility1(), row.getAbility1Value(), row.getAbility2(), row.getAbility2Value(), row.getAbility3(), row.getAbility3Value(), row.getSuitList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry_type", config.equip.CfgJewelry_type.class,
                value -> { var row = (config.equip.CfgJewelry_type) value; return new Object[]{row.getTypeName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.rank", config.equip.CfgRank_Detail.class,
                value -> { var row = (config.equip.CfgRank_Detail) value; return new Object[]{row.getRankID(), row.getRankName(), row.getRankShowName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.ArgCaptureMode", config.other.CfgArgCaptureMode_Detail.class,
                value -> { var row = (config.other.CfgArgCaptureMode_Detail) value; return new Object[]{row.getName(), row.getId(), row.getComment()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.DropItem", config.other.CfgDropItem.class,
                value -> { var row = (config.other.CfgDropItem) value; return new Object[]{row.getChance(), row.getItemids(), row.getCountmin(), row.getCountmax()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.drop", config.other.CfgDrop.class,
                value -> { var row = (config.other.CfgDrop) value; return new Object[]{row.getDropid(), row.getName(), row.getItems(), row.getTestmap()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.key_test", config.other.CfgKey_test.class,
                value -> { var row = (config.other.CfgKey_test) value; return new Object[]{row.getId1(), row.getId2(), row.getId3(), row.getIds(), row.getEnumTest(), row.getEnumList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.loot", config.other.CfgLoot.class,
                value -> { var row = (config.other.CfgLoot) value; return new Object[]{row.getLootid(), row.getEname(), row.getName(), row.getChanceList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.loot_item", config.other.CfgLoot_item.class,
                value -> { var row = (config.other.CfgLoot_item) value; return new Object[]{row.getLootid(), row.getItemid(), row.getChance(), row.getCountmin(), row.getCountmax()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.monster", config.other.CfgMonster.class,
                value -> { var row = (config.other.CfgMonster) value; return new Object[]{row.getId(), row.getPosList(), row.getLootId(), row.getLootItemId(), row.getEnumMap1(), row.getEnumMap2()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.sign_in", config.other.CfgSign_in.class,
                value -> { var row = (config.other.CfgSign_in) value; return new Object[]{row.getId(), row.getItem2countMap(), row.getVipitem2vipcountMap(), row.getViplevel(), row.getIconFile()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.TestDefaultBean", config.task.CfgTestDefaultBean.class,
                value -> { var row = (config.task.CfgTestDefaultBean) value; return new Object[]{row.getTestInt(), row.getTestBool(), row.getTestString(), row.getTestSubBean(), row.getTestList(), row.getTestList2(), row.getTestMap()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.complete_condition_type", config.task.CfgComplete_condition_type.class,
                value -> { var row = (config.task.CfgComplete_condition_type) value; return new Object[]{row.getId(), row.getName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.Chat", config.task.completecondition.CfgChat.class,
                value -> { var row = (config.task.completecondition.CfgChat) value; return new Object[]{row.getMsg()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.CollectItem", config.task.completecondition.CfgCollectItem.class,
                value -> { var row = (config.task.completecondition.CfgCollectItem) value; return new Object[]{row.getItemid(), row.getCount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.ConditionAnd", config.task.completecondition.CfgConditionAnd.class,
                value -> { var row = (config.task.completecondition.CfgConditionAnd) value; return new Object[]{row.getCond1(), row.getCond2()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.KillMonster", config.task.completecondition.CfgKillMonster.class,
                value -> { var row = (config.task.completecondition.CfgKillMonster) value; return new Object[]{row.getMonsterid(), row.getCount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.TalkNpc", config.task.completecondition.CfgTalkNpc.class,
                value -> { var row = (config.task.completecondition.CfgTalkNpc) value; return new Object[]{row.getNpcid()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.TestNoColumn", config.task.completecondition.CfgTestNoColumn.class,
                value -> { var row = (config.task.completecondition.CfgTestNoColumn) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.aa", config.task.completecondition.CfgAa.class,
                value -> { var row = (config.task.completecondition.CfgAa) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.task", config.task.CfgTask.class,
                value -> { var row = (config.task.CfgTask) value; return new Object[]{row.getTaskid(), row.getName(), row.getNexttask(), row.getCompletecondition(), row.getExp(), row.getTestDefaultBean()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.task2", config.task.CfgTask2.class,
                value -> { var row = (config.task.CfgTask2) value; return new Object[]{row.getTaskid(), row.getName(), row.getNexttask(), row.getCompletecondition(), row.getExp(), row.getTestBool(), row.getTestString(), row.getTestStruct(), row.getTestList(), row.getTestListStruct(), row.getTestListInterface()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.task_extra_exp", config.task.CfgTask_extra_exp.class,
                value -> { var row = (config.task.CfgTask_extra_exp) value; return new Object[]{row.getTaskid(), row.getExtraexp(), row.getTest1(), row.getTest2(), row.getFielda(), row.getFieldb(), row.getFieldc()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }));
        resolver.table("combat.phasebook", mgr._get_combat_phasebook_All().values());
        resolver.table("equip.jewelry_suit", mgr._get_equip_jewelry_suit_All().values());
        resolver.table("task.task2", mgr._get_task_task2_All().values());
        resolver.table("equip.rank", mgr._get_equip_rank_All());
        resolver.table("equip.equip_config", mgr._get_equip_equip_config_All().values());
        resolver.table("task.complete_condition_type", java.util.Arrays.asList(config.task.CfgComplete_condition_type.values()));
        resolver.table("other.sign_in", mgr._get_other_sign_in_All().values());
        resolver.table("task.task_extra_exp", mgr._get_task_task_extra_exp_All().values());
        resolver.table("other.ArgCaptureMode", mgr._get_other_ArgCaptureMode_All().values());
        resolver.table("task.task", mgr._get_task_task_All().values());
        resolver.table("other.loot_item", mgr._get_other_loot_item_All().values());
        resolver.table("equip.jewelry", mgr._get_equip_jewelry_All().values());
        resolver.table("other.key_test", mgr._get_other_key_test_All().values());
        resolver.table("ai.ai_action", mgr._get_ai_ai_action_All().values());
        resolver.table("other.drop", mgr._get_other_drop_All().values());
        resolver.table("ai.ai_condition", mgr._get_ai_ai_condition_All().values());
        resolver.table("combat.encounter", mgr._get_combat_encounter_All().values());
        resolver.table("ai.ai", mgr._get_ai_ai_All().values());
        resolver.table("other.monster", mgr._get_other_monster_All().values());
        resolver.table("other.loot", mgr._get_other_loot_All().values());
        resolver.table("equip.jewelry_type", java.util.Arrays.asList(config.equip.CfgJewelry_type.values()));
        resolver.table("combat.ability", mgr._get_combat_ability_All().values());
        resolver.table("equip.ability", java.util.Arrays.asList(config.equip.CfgAbility.values()));
        resolver.table("combat.branchreward", mgr._get_combat_branchreward_All().values());
        resolver.table("equip.jewelry_random", mgr._get_equip_jewelry_random_All().values());
        resolver.table("combat.timeline", mgr._get_combat_timeline_All().values());
        resolver.table("combat.cast", mgr._get_combat_cast_All().values());
        resolver.resolve();
    }

}
