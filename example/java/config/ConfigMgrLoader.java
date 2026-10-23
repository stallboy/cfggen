package config;

import configgen.genjava.ConfigLoader;
import configgen.genjava.ConfigPackage;
import configgen.genjava.LoadValueErrs;
import configgen.genjava.SchemaEnum;
import configgen.genjava.RelationResolver;
import configgen.genjava.RelationResolver.Plan;
import java.util.*;

public class ConfigMgrLoader {
    public static ConfigMgr loadPackagesAndSetMgr(ConfigPackage... packages) {
        ConfigMgr config = loadPackages(packages);
        ConfigMgr.setMgr(config);
        return config;
    }

    public static ConfigMgr loadPackages(ConfigPackage... packages) {
        ConfigPackage.validateOrder(List.of(packages));
        var errors = new LoadValueErrs();
        ConfigMgr mgr = new ConfigMgr();
        var loaders = newLoaders(mgr, errors);
        var expected = ConfigCodeSchema.getPackageSchema();
        for (var pack : packages) {
            pack.validateSchema(expected, errors);
            for (String block : pack.blockNames()) {
                if (block.startsWith(".")) {
                    String table = block.substring(1);
                    if (!loaders.containsKey(table) && !(expected.implementations.get(table) instanceof SchemaEnum symbol && !symbol.isEnumPart)) {
                        errors.unknownTableBlock(pack.name(), block);
                    }
                }
            }
        }
        errors.checkErrors();
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
        var resolver = new RelationResolver(errors);
        resolver.type("combat.AbilityUse", config.combat.AbilityUse.class,
                row -> new Object[]{row.getBinding(), row.getMachines()})
                .bind(new Plan("combat.cast", "ability", false, "table:combat.ability").source("", 0, 0).key(0),
                        (row, ref) -> row._bindRefAbility((config.combat.Ability) ref));
        resolver.type("combat.Anchor", config.combat.Anchor.class,
                row -> new Object[]{});
        resolver.type("combat.AnchorUse", config.combat.AnchorUse.class,
                row -> new Object[]{})
                .bind(new Plan("combat.ability", "anchor", false, "ancestor:combat.Machine").field(8),
                        (row, ref) -> row._bindRefAnchor((config.combat.Anchor) ref));
        resolver.type("combat.Binding", config.combat.Binding.class,
                row -> new Object[]{row.getAbilityId()});
        resolver.type("combat.BookEntry", config.combat.BookEntry.class,
                row -> new Object[]{row.getName()});
        resolver.type("combat.Combo", config.combat.Combo.class,
                row -> new Object[]{row.getWeapon(), row.getTier(), row.getAirborne(), row.getEffect()});
        resolver.type("combat.Command.Jump", config.combat.command.Jump.class,
                row -> new Object[]{row.getKey()})
                .bind(new Plan("combat.encounter", "key", true, "ancestor:combat.Command.StateMachine").source("", 0).list(0, 0),
                        (row, ref) -> row._bindNullableRefKey((config.combat.NestedPhase) ref));
        resolver.type("combat.Command.Noop", config.combat.command.Noop.class,
                row -> new Object[]{});
        resolver.type("combat.Command.StateMachine", config.combat.command.StateMachine.class,
                row -> new Object[]{row.getPhases()})
                .index(0, 0);
        resolver.type("combat.Effect.Damage", config.combat.effect.Damage.class,
                row -> new Object[]{row.getAmount()});
        resolver.type("combat.Effect.Heal", config.combat.effect.Heal.class,
                row -> new Object[]{row.getAmount()});
        resolver.type("combat.Effect.None", config.combat.effect.None.class,
                row -> new Object[]{});
        resolver.type("combat.Exit", config.combat.Exit.class,
                row -> new Object[]{row.getEffect()});
        resolver.type("combat.Frame", config.combat.Frame.class,
                row -> new Object[]{row.getTick(), row.getEffect()});
        resolver.type("combat.Impact", config.combat.Impact.class,
                row -> new Object[]{row.getOrdinal(), row.getDamage()});
        resolver.type("combat.Machine", config.combat.Machine.class,
                row -> new Object[]{row.getCode(), row.getPhases(), row.getImpacts(), row.getFrames(), row.getStances(), row.getVariants(), row.getCombos(), row.getMarkers(), row.getAnchor(), row.getAnchors(), row.getOnExit(), row.getInitial(), row.getImpact(), row.getFrame(), row.getStance(), row.getVariant(), row.getWeapon(), row.getTier(), row.getAirborne()})
                .index(1, 0)
                .index(2, 0)
                .index(3, 0)
                .index(4, 0)
                .index(5, 0)
                .index(6, 0, 1, 2)
                .index(7, 0)
                .bind(new Plan("combat.ability", "initial", false, "ancestor:combat.Machine").source("", 11).list(1, 0),
                        (row, ref) -> row._bindRefInitial((config.combat.Phase) ref))
                .bind(new Plan("combat.ability", "impact", false, "ancestor:combat.Machine").source("", 12).list(2, 0),
                        (row, ref) -> row._bindRefImpact((config.combat.Impact) ref))
                .bind(new Plan("combat.ability", "frame", false, "ancestor:combat.Machine").source("", 13).list(3, 0),
                        (row, ref) -> row._bindRefFrame((config.combat.Frame) ref))
                .bind(new Plan("combat.ability", "stance", false, "ancestor:combat.Machine").source("", 14).list(4, 0),
                        (row, ref) -> row._bindRefStance((config.combat.Stance) ref))
                .bind(new Plan("combat.ability", "variant", false, "ancestor:combat.Machine").source("", 15).list(5, 0),
                        (row, ref) -> row._bindRefVariant((config.combat.PhaseVariant) ref))
                .bind(new Plan("combat.ability", "combo", false, "ancestor:combat.Machine").source("", 16).source("", 17).source("", 18).list(6, 0, 1, 2),
                        (row, ref) -> row._bindRefCombo((config.combat.Combo) ref));
        resolver.type("combat.MachineUse", config.combat.MachineUse.class,
                row -> new Object[]{row.getMachineKey(), row.getTargets()});
        resolver.type("combat.Marker", config.combat.Marker.class,
                row -> new Object[]{row.getKey()});
        resolver.type("combat.NestedPhase", config.combat.NestedPhase.class,
                row -> new Object[]{row.getKey(), row.getAction()});
        resolver.type("combat.OptionalAim", config.combat.OptionalAim.class,
                row -> new Object[]{row.getAbilityId(), row.getMachineKey(), row.getPhaseKey()})
                .bind(new Plan("combat.cast", "target", true, "table:combat.ability").source("", 0).source("", 1).source("", 2).key(0).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindNullableRefTarget((config.combat.Phase) ref));
        resolver.type("combat.Origin", config.combat.Origin.class,
                row -> new Object[]{row.getX(), row.getY()});
        resolver.type("combat.Phase", config.combat.Phase.class,
                row -> new Object[]{row.getKey(), row.getNext()})
                .bind(new Plan("combat.ability", "next", true, "ancestor:combat.Machine").source("", 1).list(1, 0),
                        (row, ref) -> row._bindNullableRefNext((config.combat.Phase) ref))
                .bind(new Plan("combat.ability", "exit", false, "ancestor:combat.Machine").field(10).field(0),
                        (row, ref) -> row._bindRefExit((config.combat.effect.Effect) ref));
        resolver.type("combat.PhaseKey", config.combat.PhaseKey.class,
                row -> new Object[]{row.getName(), row.getVariant()});
        resolver.type("combat.PhaseUse", config.combat.PhaseUse.class,
                row -> new Object[]{row.getPhaseKey()})
                .bind(new Plan("combat.cast", "target", false, "table:combat.ability").source("ancestor:combat.AbilityUse", 0, 0).source("ancestor:combat.MachineUse", 0).source("", 0).key(0).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindRefTarget((config.combat.Phase) ref))
                .bind(new Plan("combat.cast", "byCode", false, "table:combat.ability").source("root", 1).source("ancestor:combat.MachineUse", 0).source("", 0).key(1).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindRefByCode((config.combat.Phase) ref))
                .bind(new Plan("combat.cast", "origin", false, "root").field(2),
                        (row, ref) -> row._bindRefOrigin((config.combat.Origin) ref));
        resolver.type("combat.PhaseVariant", config.combat.PhaseVariant.class,
                row -> new Object[]{row.getKey(), row.getEffect()});
        resolver.type("combat.Reward", config.combat.Reward.class,
                row -> new Object[]{row.getCount()});
        resolver.type("combat.Stance", config.combat.Stance.class,
                row -> new Object[]{row.getAirborne(), row.getEffect()});
        resolver.type("combat.ability", config.combat.Ability.class,
                row -> new Object[]{row.getId(), row.getCode(), row.getOrigin(), row.getMachines(), row.getStartMachine(), row.getStartPhase()})
                .index(3, 0)
                .bind(new Plan("combat.ability", "initial", false, "root").source("", 4).source("", 5).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindRefInitial((config.combat.Phase) ref))
                .bind(new Plan("combat.ability", "spawn", false, "root").field(2),
                        (row, ref) -> row._bindRefSpawn((config.combat.Origin) ref));
        resolver.type("combat.branchreward", config.combat.Branchreward.class,
                row -> new Object[]{row.getKey(), row.getReward()});
        resolver.type("combat.cast", config.combat.Cast.class,
                row -> new Object[]{row.getId(), row.getAbilityCode(), row.getOrigin(), row.getUses(), row.getOptional(), row.getSchool(), row.getTier(), row.getAirborne(), row.getBookCode(), row.getEdition(), row.getRewardKey()})
                .bind(new Plan("combat.cast", "book", false, "table:combat.phasebook").source("", 5).source("", 6).source("", 7).key(0, 1, 2).field(5),
                        (row, ref) -> row._bindRefBook((config.combat.BookEntry) ref))
                .bind(new Plan("combat.cast", "bookByCode", false, "table:combat.phasebook").source("", 8).source("", 9).key(3, 4).field(5),
                        (row, ref) -> row._bindRefBookByCode((config.combat.BookEntry) ref))
                .bind(new Plan("combat.cast", "bookRecord", false, "table:combat.phasebook").source("root", 5).source("root", 6).source("root", 7).key(0, 1, 2),
                        (row, ref) -> row._bindRefBookRecord((config.combat.Phasebook) ref))
                .bind(new Plan("combat.cast", "reward", false, "table:combat.branchreward").source("", 10).key(0).field(1),
                        (row, ref) -> row._bindRefReward((config.combat.Reward) ref));
        resolver.type("combat.encounter", config.combat.Encounter.class,
                row -> new Object[]{row.getId(), row.getCommand()});
        resolver.type("combat.phasebook", config.combat.Phasebook.class,
                row -> new Object[]{row.getSchool(), row.getTier(), row.getAirborne(), row.getCode(), row.getEdition(), row.getEntry()});
        resolver.type("combat.timeline", config.combat.Timeline.class,
                row -> new Object[]{row.getId(), row.getEvents(), row.getChannels(), row.getSlots(), row.getClock(), row.getModes(), row.getEventKey(), row.getChannel(), row.getSlot(), row.getFrame(), row.getAirborne(), row.getFallback()})
                .bind(new Plan("combat.timeline", "eventKey", false, "root").source("", 6).map(1),
                        (row, ref) -> row._bindRefEventKey((config.combat.effect.Effect) ref))
                .bind(new Plan("combat.timeline", "channel", false, "root").source("", 7).map(2),
                        (row, ref) -> row._bindRefChannel((config.combat.effect.Effect) ref))
                .bind(new Plan("combat.timeline", "slot", false, "root").source("", 8).map(3),
                        (row, ref) -> row._bindRefSlot((config.combat.effect.Effect) ref))
                .bind(new Plan("combat.timeline", "frame", false, "root").source("", 9).map(4),
                        (row, ref) -> row._bindRefFrame((config.combat.effect.Effect) ref))
                .bind(new Plan("combat.timeline", "airborne", false, "root").source("", 10).map(5),
                        (row, ref) -> row._bindRefAirborne((config.combat.effect.Effect) ref))
                .bind(new Plan("combat.timeline", "default", false, "root").field(11),
                        (row, ref) -> row._bindRefDefault((config.combat.effect.Effect) ref));
        resolver.table("combat.phasebook", mgr._get_combat_phasebook_All().values());
        resolver.table("combat.encounter", mgr._get_combat_encounter_All().values());
        resolver.table("combat.ability", mgr._get_combat_ability_All().values());
        resolver.table("combat.branchreward", mgr._get_combat_branchreward_All().values());
        resolver.table("combat.timeline", mgr._get_combat_timeline_All().values());
        resolver.table("combat.cast", mgr._get_combat_cast_All().values());
        resolver.resolve();
    }

}
