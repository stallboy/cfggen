package config;

import configgen.genjava.ConfigLoader;
import configgen.genjava.ConfigPackage;
import configgen.genjava.LoadValueErrs;
import configgen.genjava.SchemaEnum;
import configgen.genjava.RelationResolver;
import configgen.genjava.RelationResolver.Plan;
import java.util.*;

public class ConfigMgrLoader {
    private static final String[] LANGUAGES = { "zh_cn", "en", "tw" };
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
        var resolver = new RelationResolver(errors);
        resolver.type("combat.AbilityUse", config.combat.CfgAbilityUse.class,
                row -> new Object[]{row.getBinding(), row.getMachines()})
                .bind(new Plan("combat.cast", "ability", false, "table:combat.ability").source("", 0, 0).key(0),
                        (row, ref) -> row._bindRefAbility((config.combat.CfgAbility) ref));
        resolver.type("combat.Anchor", config.combat.CfgAnchor.class,
                row -> new Object[]{});
        resolver.type("combat.AnchorUse", config.combat.CfgAnchorUse.class,
                row -> new Object[]{})
                .bind(new Plan("combat.ability", "anchor", false, "ancestor:combat.Machine").field(8),
                        (row, ref) -> row._bindRefAnchor((config.combat.CfgAnchor) ref));
        resolver.type("combat.Binding", config.combat.CfgBinding.class,
                row -> new Object[]{row.getAbilityId()});
        resolver.type("combat.BookEntry", config.combat.CfgBookEntry.class,
                row -> new Object[]{row.getName()});
        resolver.type("combat.Combo", config.combat.CfgCombo.class,
                row -> new Object[]{row.getWeapon(), row.getTier(), row.getAirborne(), row.getEffect()});
        resolver.type("combat.Command.Jump", config.combat.command.CfgJump.class,
                row -> new Object[]{row.getKey()})
                .bind(new Plan("combat.encounter", "key", true, "ancestor:combat.Command.StateMachine").source("", 0).list(0, 0),
                        (row, ref) -> row._bindNullableRefKey((config.combat.CfgNestedPhase) ref));
        resolver.type("combat.Command.Noop", config.combat.command.CfgNoop.class,
                row -> new Object[]{});
        resolver.type("combat.Command.StateMachine", config.combat.command.CfgStateMachine.class,
                row -> new Object[]{row.getPhases()})
                .index(0, 0);
        resolver.type("combat.Effect.Damage", config.combat.effect.CfgDamage.class,
                row -> new Object[]{row.getAmount()});
        resolver.type("combat.Effect.Heal", config.combat.effect.CfgHeal.class,
                row -> new Object[]{row.getAmount()});
        resolver.type("combat.Effect.None", config.combat.effect.CfgNone.class,
                row -> new Object[]{});
        resolver.type("combat.Exit", config.combat.CfgExit.class,
                row -> new Object[]{row.getEffect()});
        resolver.type("combat.Frame", config.combat.CfgFrame.class,
                row -> new Object[]{row.getTick(), row.getEffect()});
        resolver.type("combat.Impact", config.combat.CfgImpact.class,
                row -> new Object[]{row.getOrdinal(), row.getDamage()});
        resolver.type("combat.Machine", config.combat.CfgMachine.class,
                row -> new Object[]{row.getCode(), row.getPhases(), row.getImpacts(), row.getFrames(), row.getStances(), row.getVariants(), row.getCombos(), row.getMarkers(), row.getAnchor(), row.getAnchors(), row.getOnExit(), row.getInitial(), row.getImpact(), row.getFrame(), row.getStance(), row.getVariant(), row.getWeapon(), row.getTier(), row.getAirborne()})
                .index(1, 0)
                .index(2, 0)
                .index(3, 0)
                .index(4, 0)
                .index(5, 0)
                .index(6, 0, 1, 2)
                .index(7, 0)
                .bind(new Plan("combat.ability", "initial", false, "ancestor:combat.Machine").source("", 11).list(1, 0),
                        (row, ref) -> row._bindRefInitial((config.combat.CfgPhase) ref))
                .bind(new Plan("combat.ability", "impact", false, "ancestor:combat.Machine").source("", 12).list(2, 0),
                        (row, ref) -> row._bindRefImpact((config.combat.CfgImpact) ref))
                .bind(new Plan("combat.ability", "frame", false, "ancestor:combat.Machine").source("", 13).list(3, 0),
                        (row, ref) -> row._bindRefFrame((config.combat.CfgFrame) ref))
                .bind(new Plan("combat.ability", "stance", false, "ancestor:combat.Machine").source("", 14).list(4, 0),
                        (row, ref) -> row._bindRefStance((config.combat.CfgStance) ref))
                .bind(new Plan("combat.ability", "variant", false, "ancestor:combat.Machine").source("", 15).list(5, 0),
                        (row, ref) -> row._bindRefVariant((config.combat.CfgPhaseVariant) ref))
                .bind(new Plan("combat.ability", "combo", false, "ancestor:combat.Machine").source("", 16).source("", 17).source("", 18).list(6, 0, 1, 2),
                        (row, ref) -> row._bindRefCombo((config.combat.CfgCombo) ref));
        resolver.type("combat.MachineUse", config.combat.CfgMachineUse.class,
                row -> new Object[]{row.getMachineKey(), row.getTargets()});
        resolver.type("combat.Marker", config.combat.CfgMarker.class,
                row -> new Object[]{row.getKey()});
        resolver.type("combat.NestedPhase", config.combat.CfgNestedPhase.class,
                row -> new Object[]{row.getKey(), row.getAction()});
        resolver.type("combat.OptionalAim", config.combat.CfgOptionalAim.class,
                row -> new Object[]{row.getAbilityId(), row.getMachineKey(), row.getPhaseKey()})
                .bind(new Plan("combat.cast", "target", true, "table:combat.ability").source("", 0).source("", 1).source("", 2).key(0).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindNullableRefTarget((config.combat.CfgPhase) ref));
        resolver.type("combat.Origin", config.combat.CfgOrigin.class,
                row -> new Object[]{row.getX(), row.getY()});
        resolver.type("combat.Phase", config.combat.CfgPhase.class,
                row -> new Object[]{row.getKey(), row.getNext()})
                .bind(new Plan("combat.ability", "next", true, "ancestor:combat.Machine").source("", 1).list(1, 0),
                        (row, ref) -> row._bindNullableRefNext((config.combat.CfgPhase) ref))
                .bind(new Plan("combat.ability", "exit", false, "ancestor:combat.Machine").field(10).field(0),
                        (row, ref) -> row._bindRefExit((config.combat.effect.CfgEffect) ref));
        resolver.type("combat.PhaseKey", config.combat.CfgPhaseKey.class,
                row -> new Object[]{row.getName(), row.getVariant()});
        resolver.type("combat.PhaseUse", config.combat.CfgPhaseUse.class,
                row -> new Object[]{row.getPhaseKey()})
                .bind(new Plan("combat.cast", "target", false, "table:combat.ability").source("ancestor:combat.AbilityUse", 0, 0).source("ancestor:combat.MachineUse", 0).source("", 0).key(0).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindRefTarget((config.combat.CfgPhase) ref))
                .bind(new Plan("combat.cast", "byCode", false, "table:combat.ability").source("root", 1).source("ancestor:combat.MachineUse", 0).source("", 0).key(1).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindRefByCode((config.combat.CfgPhase) ref))
                .bind(new Plan("combat.cast", "origin", false, "root").field(2),
                        (row, ref) -> row._bindRefOrigin((config.combat.CfgOrigin) ref));
        resolver.type("combat.PhaseVariant", config.combat.CfgPhaseVariant.class,
                row -> new Object[]{row.getKey(), row.getEffect()});
        resolver.type("combat.Reward", config.combat.CfgReward.class,
                row -> new Object[]{row.getCount()});
        resolver.type("combat.Stance", config.combat.CfgStance.class,
                row -> new Object[]{row.getAirborne(), row.getEffect()});
        resolver.type("combat.ability", config.combat.CfgAbility.class,
                row -> new Object[]{row.getId(), row.getCode(), row.getOrigin(), row.getMachines(), row.getStartMachine(), row.getStartPhase()})
                .index(3, 0)
                .bind(new Plan("combat.ability", "initial", false, "root").source("", 4).source("", 5).list(3, 0).list(1, 0),
                        (row, ref) -> row._bindRefInitial((config.combat.CfgPhase) ref))
                .bind(new Plan("combat.ability", "spawn", false, "root").field(2),
                        (row, ref) -> row._bindRefSpawn((config.combat.CfgOrigin) ref));
        resolver.type("combat.branchreward", config.combat.CfgBranchreward.class,
                row -> new Object[]{row.getKey(), row.getReward()});
        resolver.type("combat.cast", config.combat.CfgCast.class,
                row -> new Object[]{row.getId(), row.getAbilityCode(), row.getOrigin(), row.getUses(), row.getOptional(), row.getSchool(), row.getTier(), row.getAirborne(), row.getBookCode(), row.getEdition(), row.getRewardKey()})
                .bind(new Plan("combat.cast", "book", false, "table:combat.phasebook").source("", 5).source("", 6).source("", 7).key(0, 1, 2).field(5),
                        (row, ref) -> row._bindRefBook((config.combat.CfgBookEntry) ref))
                .bind(new Plan("combat.cast", "bookByCode", false, "table:combat.phasebook").source("", 8).source("", 9).key(3, 4).field(5),
                        (row, ref) -> row._bindRefBookByCode((config.combat.CfgBookEntry) ref))
                .bind(new Plan("combat.cast", "bookRecord", false, "table:combat.phasebook").source("root", 5).source("root", 6).source("root", 7).key(0, 1, 2),
                        (row, ref) -> row._bindRefBookRecord((config.combat.CfgPhasebook) ref))
                .bind(new Plan("combat.cast", "reward", false, "table:combat.branchreward").source("", 10).key(0).field(1),
                        (row, ref) -> row._bindRefReward((config.combat.CfgReward) ref));
        resolver.type("combat.encounter", config.combat.CfgEncounter.class,
                row -> new Object[]{row.getId(), row.getCommand()});
        resolver.type("combat.phasebook", config.combat.CfgPhasebook.class,
                row -> new Object[]{row.getSchool(), row.getTier(), row.getAirborne(), row.getCode(), row.getEdition(), row.getEntry()});
        resolver.type("combat.timeline", config.combat.CfgTimeline.class,
                row -> new Object[]{row.getId(), row.getEvents(), row.getChannels(), row.getSlots(), row.getClock(), row.getModes(), row.getEventKey(), row.getChannel(), row.getSlot(), row.getFrame(), row.getAirborne(), row.getFallback()})
                .bind(new Plan("combat.timeline", "eventKey", false, "root").source("", 6).map(1),
                        (row, ref) -> row._bindRefEventKey((config.combat.effect.CfgEffect) ref))
                .bind(new Plan("combat.timeline", "channel", false, "root").source("", 7).map(2),
                        (row, ref) -> row._bindRefChannel((config.combat.effect.CfgEffect) ref))
                .bind(new Plan("combat.timeline", "slot", false, "root").source("", 8).map(3),
                        (row, ref) -> row._bindRefSlot((config.combat.effect.CfgEffect) ref))
                .bind(new Plan("combat.timeline", "frame", false, "root").source("", 9).map(4),
                        (row, ref) -> row._bindRefFrame((config.combat.effect.CfgEffect) ref))
                .bind(new Plan("combat.timeline", "airborne", false, "root").source("", 10).map(5),
                        (row, ref) -> row._bindRefAirborne((config.combat.effect.CfgEffect) ref))
                .bind(new Plan("combat.timeline", "default", false, "root").field(11),
                        (row, ref) -> row._bindRefDefault((config.combat.effect.CfgEffect) ref));
        resolver.table("combat.phasebook", mgr._get_combat_phasebook_All().values());
        resolver.table("combat.encounter", mgr._get_combat_encounter_All().values());
        resolver.table("combat.ability", mgr._get_combat_ability_All().values());
        resolver.table("combat.branchreward", mgr._get_combat_branchreward_All().values());
        resolver.table("combat.timeline", mgr._get_combat_timeline_All().values());
        resolver.table("combat.cast", mgr._get_combat_cast_All().values());
        resolver.resolve();
    }

}
