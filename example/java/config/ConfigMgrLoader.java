package config;

import configgen.genjava.ConfigLoader;
import configgen.genjava.ConfigPackage;
import configgen.genjava.LoadValueErrs;
import configgen.genjava.RelationResolver;
import java.util.*;

public class ConfigMgrLoader {
    private static final java.util.Set<String> TABLES = java.util.Set.of("ai.ai", "ai.ai_action", "ai.ai_condition", "combat.ability", "combat.branchreward", "combat.cast", "combat.encounter", "combat.phasebook", "combat.timeline", "equip.ability", "equip.equip_config", "equip.jewelry", "equip.jewelry_random", "equip.jewelry_suit", "equip.jewelry_type", "equip.rank", "other.ArgCaptureMode", "other.drop", "other.key_test", "other.loot", "other.loot_item", "other.monster", "other.sign_in", "task.complete_condition_type", "task.task", "task.task2", "task.task_extra_exp");
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
                    loader.read(pack.block(block), packName, pack == packages[0]);
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
        config.equip.EquipConfig_Entry.setAllRefs(mgr, errors);
        config.equip.JewelrySuit_Entry.setAllRefs(mgr, errors);
        config.equip.Rank.setAllRefs(mgr, errors);
        config.other.ArgCaptureMode.setAllRefs(mgr, errors);
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
        loaders.put("ai.ai", new ConfigLoader<>("ai.ai", errors, config.ai.Ai::_create,
                config.ai.Ai::getID, mgr::_set_ai_ai_All, null));
        loaders.put("ai.ai_action", new ConfigLoader<>("ai.ai_action", errors, config.ai.AiAction::_create,
                config.ai.AiAction::getID, mgr::_set_ai_ai_action_All, null));
        loaders.put("ai.ai_condition", new ConfigLoader<>("ai.ai_condition", errors, config.ai.AiCondition::_create,
                config.ai.AiCondition::getID, mgr::_set_ai_ai_condition_All, null));
        loaders.put("combat.ability", new ConfigLoader<>("combat.ability", errors, config.combat.Ability::_create,
                config.combat.Ability::getId, rows -> config.combat.Ability._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("combat.branchreward", new ConfigLoader<>("combat.branchreward", errors, config.combat.Branchreward::_create,
                config.combat.Branchreward::getKey, mgr::_set_combat_branchreward_All, null));
        loaders.put("combat.cast", new ConfigLoader<>("combat.cast", errors, config.combat.Cast::_create,
                config.combat.Cast::getId, mgr::_set_combat_cast_All, row -> row._resolve(mgr, errors)));
        loaders.put("combat.encounter", new ConfigLoader<>("combat.encounter", errors, config.combat.Encounter::_create,
                config.combat.Encounter::getId, mgr::_set_combat_encounter_All, row -> row._resolve(mgr, errors)));
        loaders.put("combat.phasebook", new ConfigLoader<>("combat.phasebook", errors, config.combat.Phasebook::_create,
                row -> new config.combat.Phasebook.SchoolTierAirborneKey(row.getSchool(), row.getTier(), row.getAirborne()), rows -> config.combat.Phasebook._index(mgr, rows, errors), null));
        loaders.put("combat.timeline", new ConfigLoader<>("combat.timeline", errors, config.combat.Timeline::_create,
                config.combat.Timeline::getId, mgr::_set_combat_timeline_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.equip_config", new ConfigLoader<>("equip.equip_config", errors, config.equip.EquipConfig::_create,
                config.equip.EquipConfig::getEntry, mgr::_set_equip_equip_config_All, null));
        loaders.put("equip.jewelry", new ConfigLoader<>("equip.jewelry", errors, config.equip.Jewelry::_create,
                config.equip.Jewelry::getID, mgr::_set_equip_jewelry_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_random", new ConfigLoader<>("equip.jewelry_random", errors, config.equip.JewelryRandom::_create,
                config.equip.JewelryRandom::getLvlRank, mgr::_set_equip_jewelry_random_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_suit", new ConfigLoader<>("equip.jewelry_suit", errors, config.equip.JewelrySuit::_create,
                config.equip.JewelrySuit::getSuitID, mgr::_set_equip_jewelry_suit_All, null));
        loaders.put("equip.rank", new ConfigLoader<>("equip.rank", errors, config.equip.Rank_Detail::_create,
                config.equip.Rank_Detail::getRankID, rows -> config.equip.Rank_Detail._index(mgr, rows, errors), null));
        loaders.put("other.ArgCaptureMode", new ConfigLoader<>("other.ArgCaptureMode", errors, config.other.ArgCaptureMode_Detail::_create,
                config.other.ArgCaptureMode_Detail::getName, rows -> config.other.ArgCaptureMode_Detail._index(mgr, rows, errors), null));
        loaders.put("other.drop", new ConfigLoader<>("other.drop", errors, config.other.Drop::_create,
                config.other.Drop::getDropid, mgr::_set_other_drop_All, null));
        loaders.put("other.key_test", new ConfigLoader<>("other.key_test", errors, config.other.KeyTest::_create,
                row -> new config.other.KeyTest.Id1Id2Key(row.getId1(), row.getId2()), rows -> config.other.KeyTest._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("other.loot", new ConfigLoader<>("other.loot", errors, config.other.Loot::_create,
                config.other.Loot::getLootid, mgr::_set_other_loot_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.loot_item", new ConfigLoader<>("other.loot_item", errors, config.other.LootItem::_create,
                row -> new config.other.LootItem.LootidItemidKey(row.getLootid(), row.getItemid()), mgr::_set_other_loot_item_All, null));
        loaders.put("other.monster", new ConfigLoader<>("other.monster", errors, config.other.Monster::_create,
                config.other.Monster::getId, mgr::_set_other_monster_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.sign_in", new ConfigLoader<>("other.sign_in", errors, config.other.SignIn::_create,
                config.other.SignIn::getId, mgr::_set_other_sign_in_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task", new ConfigLoader<>("task.task", errors, config.task.Task::_create,
                config.task.Task::getTaskid, mgr::_set_task_task_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task2", new ConfigLoader<>("task.task2", errors, config.task.Task2::_create,
                config.task.Task2::getTaskid, mgr::_set_task_task2_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task_extra_exp", new ConfigLoader<>("task.task_extra_exp", errors, config.task.TaskExtraExp::_create,
                config.task.TaskExtraExp::getTaskid, mgr::_set_task_task_extra_exp_All, null));
        return loaders;
    }

    private static void resolveRelations(ConfigMgr mgr, LoadValueErrs errors) {
        var resolver = new RelationResolver(errors,
            new RelationResolver.Type("LevelRank", config.LevelRank.class,
                value -> { var row = (config.LevelRank) value; return new Object[]{row.getLevel(), row.getRank()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("Position", config.Position.class,
                value -> { var row = (config.Position) value; return new Object[]{row.getX(), row.getY(), row.getZ()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("Range", config.Range.class,
                value -> { var row = (config.Range) value; return new Object[]{row.getMin(), row.getMax()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.TriggerTick.ByLevel", config.ai.triggertick.ByLevel.class,
                value -> { var row = (config.ai.triggertick.ByLevel) value; return new Object[]{row.getInit(), row.getCoefficient()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.TriggerTick.ByServerUpDay", config.ai.triggertick.ByServerUpDay.class,
                value -> { var row = (config.ai.triggertick.ByServerUpDay) value; return new Object[]{row.getInit(), row.getCoefficient1(), row.getCoefficient2()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.TriggerTick.ConstValue", config.ai.triggertick.ConstValue.class,
                value -> { var row = (config.ai.triggertick.ConstValue) value; return new Object[]{row.getValue()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.ai", config.ai.Ai.class,
                value -> { var row = (config.ai.Ai) value; return new Object[]{row.getID(), row.getDesc(), row.getCondID(), row.getTrigTick(), row.getTrigOdds(), row.getActionID(), row.getDeathRemove()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.ai_action", config.ai.AiAction.class,
                value -> { var row = (config.ai.AiAction) value; return new Object[]{row.getID(), row.getDesc(), row.getFormulaID(), row.getArgIList(), row.getArgSList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("ai.ai_condition", config.ai.AiCondition.class,
                value -> { var row = (config.ai.AiCondition) value; return new Object[]{row.getID(), row.getDesc(), row.getFormulaID(), row.getArgIList(), row.getArgSList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.AbilityUse", config.combat.AbilityUse.class,
                value -> { var row = (config.combat.AbilityUse) value; return new Object[]{row.getBinding(), row.getMachines()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "ability", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{0, 0})}, "table:combat.ability", new int[]{0}, new RelationResolver.Step[]{})},
                (value, refs) -> {
                    var row = (config.combat.AbilityUse) value;
                    row._bindRefAbility((config.combat.Ability) refs[0]);
                }),
            new RelationResolver.Type("combat.Anchor", config.combat.Anchor.class,
                value -> { var row = (config.combat.Anchor) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.AnchorUse", config.combat.AnchorUse.class,
                value -> { var row = (config.combat.AnchorUse) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "anchor", false, new RelationResolver.Source[]{}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(8, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.AnchorUse) value;
                    row._bindRefAnchor((config.combat.Anchor) refs[0]);
                }),
            new RelationResolver.Type("combat.Binding", config.combat.Binding.class,
                value -> { var row = (config.combat.Binding) value; return new Object[]{row.getAbilityId()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.BookEntry", config.combat.BookEntry.class,
                value -> { var row = (config.combat.BookEntry) value; return new Object[]{row.getName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Combo", config.combat.Combo.class,
                value -> { var row = (config.combat.Combo) value; return new Object[]{row.getWeapon(), row.getTier(), row.getAirborne(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Command.Jump", config.combat.command.Jump.class,
                value -> { var row = (config.combat.command.Jump) value; return new Object[]{row.getKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.encounter", 0, "key", true, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{0})}, "ancestor:combat.Command.StateMachine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(0, 1, new int[]{0})})},
                (value, refs) -> {
                    var row = (config.combat.command.Jump) value;
                    row._bindNullableRefKey((config.combat.NestedPhase) refs[0]);
                }),
            new RelationResolver.Type("combat.Command.Noop", config.combat.command.Noop.class,
                value -> { var row = (config.combat.command.Noop) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Command.StateMachine", config.combat.command.StateMachine.class,
                value -> { var row = (config.combat.command.StateMachine) value; return new Object[]{row.getPhases()}; },
                new RelationResolver.Index[]{new RelationResolver.Index(0, new int[]{0})},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Effect.Damage", config.combat.effect.Damage.class,
                value -> { var row = (config.combat.effect.Damage) value; return new Object[]{row.getAmount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Effect.Heal", config.combat.effect.Heal.class,
                value -> { var row = (config.combat.effect.Heal) value; return new Object[]{row.getAmount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Effect.None", config.combat.effect.None.class,
                value -> { var row = (config.combat.effect.None) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Exit", config.combat.Exit.class,
                value -> { var row = (config.combat.Exit) value; return new Object[]{row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Frame", config.combat.Frame.class,
                value -> { var row = (config.combat.Frame) value; return new Object[]{row.getTick(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Impact", config.combat.Impact.class,
                value -> { var row = (config.combat.Impact) value; return new Object[]{row.getOrdinal(), row.getDamage()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Machine", config.combat.Machine.class,
                value -> { var row = (config.combat.Machine) value; return new Object[]{row.getCode(), row.getPhases(), row.getImpacts(), row.getFrames(), row.getStances(), row.getVariants(), row.getCombos(), row.getMarkers(), row.getAnchor(), row.getAnchors(), row.getOnExit(), row.getInitial(), row.getImpact(), row.getFrame(), row.getStance(), row.getVariant(), row.getWeapon(), row.getTier(), row.getAirborne()}; },
                new RelationResolver.Index[]{new RelationResolver.Index(1, new int[]{0}), new RelationResolver.Index(2, new int[]{0}), new RelationResolver.Index(3, new int[]{0}), new RelationResolver.Index(4, new int[]{0}), new RelationResolver.Index(5, new int[]{0}), new RelationResolver.Index(6, new int[]{0, 1, 2}), new RelationResolver.Index(7, new int[]{0})},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "initial", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{11})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 1, "impact", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{12})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 2, "frame", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{13})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 3, "stance", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{14})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(4, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 4, "variant", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{15})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(5, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 5, "combo", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{16}), new RelationResolver.Source("", new int[]{17}), new RelationResolver.Source("", new int[]{18})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(6, 1, new int[]{0, 1, 2})})},
                (value, refs) -> {
                    var row = (config.combat.Machine) value;
                    row._bindRefInitial((config.combat.Phase) refs[0]);
                    row._bindRefImpact((config.combat.Impact) refs[1]);
                    row._bindRefFrame((config.combat.Frame) refs[2]);
                    row._bindRefStance((config.combat.Stance) refs[3]);
                    row._bindRefVariant((config.combat.PhaseVariant) refs[4]);
                    row._bindRefCombo((config.combat.Combo) refs[5]);
                }),
            new RelationResolver.Type("combat.MachineUse", config.combat.MachineUse.class,
                value -> { var row = (config.combat.MachineUse) value; return new Object[]{row.getMachineKey(), row.getTargets()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Marker", config.combat.Marker.class,
                value -> { var row = (config.combat.Marker) value; return new Object[]{row.getKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.NestedPhase", config.combat.NestedPhase.class,
                value -> { var row = (config.combat.NestedPhase) value; return new Object[]{row.getKey(), row.getAction()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.OptionalAim", config.combat.OptionalAim.class,
                value -> { var row = (config.combat.OptionalAim) value; return new Object[]{row.getAbilityId(), row.getMachineKey(), row.getPhaseKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "target", true, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{0}), new RelationResolver.Source("", new int[]{1}), new RelationResolver.Source("", new int[]{2})}, "table:combat.ability", new int[]{0}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})})},
                (value, refs) -> {
                    var row = (config.combat.OptionalAim) value;
                    row._bindNullableRefTarget((config.combat.Phase) refs[0]);
                }),
            new RelationResolver.Type("combat.Origin", config.combat.Origin.class,
                value -> { var row = (config.combat.Origin) value; return new Object[]{row.getX(), row.getY()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Phase", config.combat.Phase.class,
                value -> { var row = (config.combat.Phase) value; return new Object[]{row.getKey(), row.getNext()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "next", true, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{1})}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 1, "exit", false, new RelationResolver.Source[]{}, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(10, 0, new int[]{}), new RelationResolver.Step(0, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.Phase) value;
                    row._bindNullableRefNext((config.combat.Phase) refs[0]);
                    row._bindRefExit((config.combat.effect.Effect) refs[1]);
                }),
            new RelationResolver.Type("combat.PhaseKey", config.combat.PhaseKey.class,
                value -> { var row = (config.combat.PhaseKey) value; return new Object[]{row.getName(), row.getVariant()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.PhaseUse", config.combat.PhaseUse.class,
                value -> { var row = (config.combat.PhaseUse) value; return new Object[]{row.getPhaseKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "target", false, new RelationResolver.Source[]{new RelationResolver.Source("ancestor:combat.AbilityUse", new int[]{0, 0}), new RelationResolver.Source("ancestor:combat.MachineUse", new int[]{0}), new RelationResolver.Source("", new int[]{0})}, "table:combat.ability", new int[]{0}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.cast", 1, "byCode", false, new RelationResolver.Source[]{new RelationResolver.Source("root", new int[]{1}), new RelationResolver.Source("ancestor:combat.MachineUse", new int[]{0}), new RelationResolver.Source("", new int[]{0})}, "table:combat.ability", new int[]{1}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.cast", 2, "origin", false, new RelationResolver.Source[]{}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.PhaseUse) value;
                    row._bindRefTarget((config.combat.Phase) refs[0]);
                    row._bindRefByCode((config.combat.Phase) refs[1]);
                    row._bindRefOrigin((config.combat.Origin) refs[2]);
                }),
            new RelationResolver.Type("combat.PhaseVariant", config.combat.PhaseVariant.class,
                value -> { var row = (config.combat.PhaseVariant) value; return new Object[]{row.getKey(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Reward", config.combat.Reward.class,
                value -> { var row = (config.combat.Reward) value; return new Object[]{row.getCount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.Stance", config.combat.Stance.class,
                value -> { var row = (config.combat.Stance) value; return new Object[]{row.getAirborne(), row.getEffect()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.ability", config.combat.Ability.class,
                value -> { var row = (config.combat.Ability) value; return new Object[]{row.getId(), row.getCode(), row.getOrigin(), row.getMachines(), row.getStartMachine(), row.getStartPhase()}; },
                new RelationResolver.Index[]{new RelationResolver.Index(3, new int[]{0})},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.ability", 0, "initial", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{4}), new RelationResolver.Source("", new int[]{5})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(3, 1, new int[]{0}), new RelationResolver.Step(1, 1, new int[]{0})}),
new RelationResolver.Plan("combat.ability", 1, "spawn", false, new RelationResolver.Source[]{}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.Ability) value;
                    row._bindRefInitial((config.combat.Phase) refs[0]);
                    row._bindRefSpawn((config.combat.Origin) refs[1]);
                }),
            new RelationResolver.Type("combat.branchreward", config.combat.Branchreward.class,
                value -> { var row = (config.combat.Branchreward) value; return new Object[]{row.getKey(), row.getReward()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.cast", config.combat.Cast.class,
                value -> { var row = (config.combat.Cast) value; return new Object[]{row.getId(), row.getAbilityCode(), row.getOrigin(), row.getUses(), row.getOptional(), row.getSchool(), row.getTier(), row.getAirborne(), row.getBookCode(), row.getEdition(), row.getRewardKey()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.cast", 0, "book", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{5}), new RelationResolver.Source("", new int[]{6}), new RelationResolver.Source("", new int[]{7})}, "table:combat.phasebook", new int[]{0, 1, 2}, new RelationResolver.Step[]{new RelationResolver.Step(5, 0, new int[]{})}),
new RelationResolver.Plan("combat.cast", 1, "bookByCode", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{8}), new RelationResolver.Source("", new int[]{9})}, "table:combat.phasebook", new int[]{3, 4}, new RelationResolver.Step[]{new RelationResolver.Step(5, 0, new int[]{})}),
new RelationResolver.Plan("combat.cast", 2, "bookRecord", false, new RelationResolver.Source[]{new RelationResolver.Source("root", new int[]{5}), new RelationResolver.Source("root", new int[]{6}), new RelationResolver.Source("root", new int[]{7})}, "table:combat.phasebook", new int[]{0, 1, 2}, new RelationResolver.Step[]{}),
new RelationResolver.Plan("combat.cast", 3, "reward", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{10})}, "table:combat.branchreward", new int[]{0}, new RelationResolver.Step[]{new RelationResolver.Step(1, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.Cast) value;
                    row._bindRefBook((config.combat.BookEntry) refs[0]);
                    row._bindRefBookByCode((config.combat.BookEntry) refs[1]);
                    row._bindRefBookRecord((config.combat.Phasebook) refs[2]);
                    row._bindRefReward((config.combat.Reward) refs[3]);
                }),
            new RelationResolver.Type("combat.encounter", config.combat.Encounter.class,
                value -> { var row = (config.combat.Encounter) value; return new Object[]{row.getId(), row.getCommand()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.phasebook", config.combat.Phasebook.class,
                value -> { var row = (config.combat.Phasebook) value; return new Object[]{row.getSchool(), row.getTier(), row.getAirborne(), row.getCode(), row.getEdition(), row.getEntry()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("combat.timeline", config.combat.Timeline.class,
                value -> { var row = (config.combat.Timeline) value; return new Object[]{row.getId(), row.getEvents(), row.getChannels(), row.getSlots(), row.getClock(), row.getModes(), row.getEventKey(), row.getChannel(), row.getSlot(), row.getFrame(), row.getAirborne(), row.getFallback()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{new RelationResolver.Plan("combat.timeline", 0, "eventKey", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{6})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(1, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 1, "channel", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{7})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(2, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 2, "slot", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{8})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(3, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 3, "frame", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{9})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(4, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 4, "airborne", false, new RelationResolver.Source[]{new RelationResolver.Source("", new int[]{10})}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(5, 2, new int[]{})}),
new RelationResolver.Plan("combat.timeline", 5, "default", false, new RelationResolver.Source[]{}, "root", new int[]{}, new RelationResolver.Step[]{new RelationResolver.Step(11, 0, new int[]{})})},
                (value, refs) -> {
                    var row = (config.combat.Timeline) value;
                    row._bindRefEventKey((config.combat.effect.Effect) refs[0]);
                    row._bindRefChannel((config.combat.effect.Effect) refs[1]);
                    row._bindRefSlot((config.combat.effect.Effect) refs[2]);
                    row._bindRefFrame((config.combat.effect.Effect) refs[3]);
                    row._bindRefAirborne((config.combat.effect.Effect) refs[4]);
                    row._bindRefDefault((config.combat.effect.Effect) refs[5]);
                }),
            new RelationResolver.Type("equip.TestPackBean", config.equip.TestPackBean.class,
                value -> { var row = (config.equip.TestPackBean) value; return new Object[]{row.getName(), row.getIRange()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.ability", config.equip.Ability.class,
                value -> { var row = (config.equip.Ability) value; return new Object[]{row.getId(), row.getName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.equip_config", config.equip.EquipConfig.class,
                value -> { var row = (config.equip.EquipConfig) value; return new Object[]{row.getEntry(), row.getStone_count_for_set(), row.getDraw_protect_name(), row.getBroadcastid(), row.getBroadcast_least_quality(), row.getWeek_reward_mailid()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry", config.equip.Jewelry.class,
                value -> { var row = (config.equip.Jewelry) value; return new Object[]{row.getID(), row.getName(), row.getIconFile(), row.getLvlRank(), row.getJType(), row.getSuitID(), row.getKeyAbility(), row.getKeyAbilityValue(), row.getSalePrice(), row.getDescription()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry_random", config.equip.JewelryRandom.class,
                value -> { var row = (config.equip.JewelryRandom) value; return new Object[]{row.getLvlRank(), row.getAttackRange(), row.getOtherRange(), row.getTestPack()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry_suit", config.equip.JewelrySuit.class,
                value -> { var row = (config.equip.JewelrySuit) value; return new Object[]{row.getSuitID(), row.getEname(), row.getName(), row.getAbility1(), row.getAbility1Value(), row.getAbility2(), row.getAbility2Value(), row.getAbility3(), row.getAbility3Value(), row.getSuitList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.jewelry_type", config.equip.JewelryType.class,
                value -> { var row = (config.equip.JewelryType) value; return new Object[]{row.getTypeName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("equip.rank", config.equip.Rank_Detail.class,
                value -> { var row = (config.equip.Rank_Detail) value; return new Object[]{row.getRankID(), row.getRankName(), row.getRankShowName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.ArgCaptureMode", config.other.ArgCaptureMode_Detail.class,
                value -> { var row = (config.other.ArgCaptureMode_Detail) value; return new Object[]{row.getName(), row.getId(), row.getComment()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.DropItem", config.other.DropItem.class,
                value -> { var row = (config.other.DropItem) value; return new Object[]{row.getChance(), row.getItemids(), row.getCountmin(), row.getCountmax()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.drop", config.other.Drop.class,
                value -> { var row = (config.other.Drop) value; return new Object[]{row.getDropid(), row.getName(), row.getItems(), row.getTestmap()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.key_test", config.other.KeyTest.class,
                value -> { var row = (config.other.KeyTest) value; return new Object[]{row.getId1(), row.getId2(), row.getId3(), row.getIds(), row.getEnumTest(), row.getEnumList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.loot", config.other.Loot.class,
                value -> { var row = (config.other.Loot) value; return new Object[]{row.getLootid(), row.getEname(), row.getName(), row.getChanceList()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.loot_item", config.other.LootItem.class,
                value -> { var row = (config.other.LootItem) value; return new Object[]{row.getLootid(), row.getItemid(), row.getChance(), row.getCountmin(), row.getCountmax()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.monster", config.other.Monster.class,
                value -> { var row = (config.other.Monster) value; return new Object[]{row.getId(), row.getPosList(), row.getLootId(), row.getLootItemId(), row.getEnumMap1(), row.getEnumMap2()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("other.sign_in", config.other.SignIn.class,
                value -> { var row = (config.other.SignIn) value; return new Object[]{row.getId(), row.getItem2countMap(), row.getVipitem2vipcountMap(), row.getViplevel(), row.getIconFile()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.TestDefaultBean", config.task.TestDefaultBean.class,
                value -> { var row = (config.task.TestDefaultBean) value; return new Object[]{row.getTestInt(), row.getTestBool(), row.getTestString(), row.getTestSubBean(), row.getTestList(), row.getTestList2(), row.getTestMap()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.complete_condition_type", config.task.CompleteConditionType.class,
                value -> { var row = (config.task.CompleteConditionType) value; return new Object[]{row.getId(), row.getName()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.Chat", config.task.completecondition.Chat.class,
                value -> { var row = (config.task.completecondition.Chat) value; return new Object[]{row.getMsg()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.CollectItem", config.task.completecondition.CollectItem.class,
                value -> { var row = (config.task.completecondition.CollectItem) value; return new Object[]{row.getItemid(), row.getCount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.ConditionAnd", config.task.completecondition.ConditionAnd.class,
                value -> { var row = (config.task.completecondition.ConditionAnd) value; return new Object[]{row.getCond1(), row.getCond2()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.KillMonster", config.task.completecondition.KillMonster.class,
                value -> { var row = (config.task.completecondition.KillMonster) value; return new Object[]{row.getMonsterid(), row.getCount()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.TalkNpc", config.task.completecondition.TalkNpc.class,
                value -> { var row = (config.task.completecondition.TalkNpc) value; return new Object[]{row.getNpcid()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.TestNoColumn", config.task.completecondition.TestNoColumn.class,
                value -> { var row = (config.task.completecondition.TestNoColumn) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.completecondition.aa", config.task.completecondition.Aa.class,
                value -> { var row = (config.task.completecondition.Aa) value; return new Object[]{}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.task", config.task.Task.class,
                value -> { var row = (config.task.Task) value; return new Object[]{row.getTaskid(), row.getName(), row.getNexttask(), row.getCompletecondition(), row.getExp(), row.getTestDefaultBean()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.task2", config.task.Task2.class,
                value -> { var row = (config.task.Task2) value; return new Object[]{row.getTaskid(), row.getName(), row.getNexttask(), row.getCompletecondition(), row.getExp(), row.getTestBool(), row.getTestString(), row.getTestStruct(), row.getTestList(), row.getTestListStruct(), row.getTestListInterface()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }),
            new RelationResolver.Type("task.task_extra_exp", config.task.TaskExtraExp.class,
                value -> { var row = (config.task.TaskExtraExp) value; return new Object[]{row.getTaskid(), row.getExtraexp(), row.getTest1(), row.getTest2(), row.getFielda(), row.getFieldb(), row.getFieldc()}; },
                new RelationResolver.Index[]{},
                new RelationResolver.Plan[]{},
                (value, refs) -> {
                }));
        resolver.table("combat.phasebook", mgr._get_combat_phasebook_All().values());
        resolver.table("equip.jewelry_suit", mgr._get_equip_jewelry_suit_All().values());
        resolver.table("task.task2", mgr._get_task_task2_All().values());
        resolver.table("equip.rank", mgr._get_equip_rank_All());
        resolver.table("equip.equip_config", mgr._get_equip_equip_config_All().values());
        resolver.table("task.complete_condition_type", java.util.Arrays.asList(config.task.CompleteConditionType.values()));
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
        resolver.table("equip.jewelry_type", java.util.Arrays.asList(config.equip.JewelryType.values()));
        resolver.table("combat.ability", mgr._get_combat_ability_All().values());
        resolver.table("equip.ability", java.util.Arrays.asList(config.equip.Ability.values()));
        resolver.table("combat.branchreward", mgr._get_combat_branchreward_All().values());
        resolver.table("equip.jewelry_random", mgr._get_equip_jewelry_random_All().values());
        resolver.table("combat.timeline", mgr._get_combat_timeline_All().values());
        resolver.table("combat.cast", mgr._get_combat_cast_All().values());
        resolver.resolve();
    }

}
