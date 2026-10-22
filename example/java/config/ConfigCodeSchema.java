package config;

import configgen.genjava.*;

public class ConfigCodeSchema {

    public static SchemaInterface getCodeSchema() { return createSchema(false); }
    public static SchemaInterface getPackageSchema() { return createSchema(true); }
    private static SchemaInterface createSchema(boolean packageLayout) {
        SchemaInterface schema = new SchemaInterface();
        schema.addImp("LevelRank", LevelRank());
        schema.addImp("Position", Position());
        schema.addImp("Range", Range());
        schema.addImp("ai.TriggerTick", ai_TriggerTick());
        schema.addImp("combat.AbilityUse", combat_AbilityUse());
        schema.addImp("combat.Anchor", combat_Anchor());
        schema.addImp("combat.AnchorUse", combat_AnchorUse());
        schema.addImp("combat.Binding", combat_Binding());
        schema.addImp("combat.BookEntry", combat_BookEntry());
        schema.addImp("combat.Combo", combat_Combo());
        schema.addImp("combat.Command", combat_Command());
        schema.addImp("combat.Effect", combat_Effect());
        schema.addImp("combat.Exit", combat_Exit());
        schema.addImp("combat.Frame", combat_Frame());
        schema.addImp("combat.Impact", combat_Impact());
        schema.addImp("combat.Machine", combat_Machine());
        schema.addImp("combat.MachineUse", combat_MachineUse());
        schema.addImp("combat.Marker", combat_Marker());
        schema.addImp("combat.NestedPhase", combat_NestedPhase());
        schema.addImp("combat.OptionalAim", combat_OptionalAim());
        schema.addImp("combat.Origin", combat_Origin());
        schema.addImp("combat.Phase", combat_Phase());
        schema.addImp("combat.PhaseKey", combat_PhaseKey());
        schema.addImp("combat.PhaseUse", combat_PhaseUse());
        schema.addImp("combat.PhaseVariant", combat_PhaseVariant());
        schema.addImp("combat.Reward", combat_Reward());
        schema.addImp("combat.Stance", combat_Stance());
        schema.addImp("equip.TestPackBean", equip_TestPackBean());
        schema.addImp("other.DropItem", other_DropItem());
        schema.addImp("task.TestDefaultBean", task_TestDefaultBean());
        schema.addImp("task.completecondition", task_completecondition());
        schema.addImp("ai.ai", ai_ai());
        schema.addImp("ai.ai_action", ai_ai_action());
        schema.addImp("ai.ai_condition", ai_ai_condition());
        schema.addImp("combat.ability", combat_ability());
        schema.addImp("combat.branchreward", combat_branchreward());
        schema.addImp("combat.cast", combat_cast());
        schema.addImp("combat.encounter", combat_encounter());
        schema.addImp("combat.phasebook", combat_phasebook());
        schema.addImp("combat.timeline", combat_timeline());
        schema.addImp("equip.ability", equip_ability());
        if (packageLayout) {
            schema.addImp("equip.ability_Detail", equip_ability_Detail());
        }
        schema.addImp("equip.equip_config", equip_equip_config());
        schema.addImp("equip.equip_config_Entry", equip_equip_config_Entry());
        schema.addImp("equip.jewelry", equip_jewelry());
        schema.addImp("equip.jewelry_random", equip_jewelry_random());
        schema.addImp("equip.jewelry_suit", equip_jewelry_suit());
        schema.addImp("equip.jewelry_suit_Entry", equip_jewelry_suit_Entry());
        schema.addImp("equip.jewelry_type", equip_jewelry_type());
        if (packageLayout) {
            schema.addImp("equip.jewelry_type_Detail", equip_jewelry_type_Detail());
        }
        schema.addImp("equip.rank", equip_rank());
        schema.addImp("equip.rank_Detail", equip_rank_Detail());
        schema.addImp("other.ArgCaptureMode", other_ArgCaptureMode());
        schema.addImp("other.ArgCaptureMode_Detail", other_ArgCaptureMode_Detail());
        schema.addImp("other.drop", other_drop());
        schema.addImp("other.key_test", other_key_test());
        schema.addImp("other.loot", other_loot());
        schema.addImp("other.loot_item", other_loot_item());
        schema.addImp("other.monster", other_monster());
        schema.addImp("other.sign_in", other_sign_in());
        schema.addImp("task.complete_condition_type", task_complete_condition_type());
        if (packageLayout) {
            schema.addImp("task.complete_condition_type_Detail", task_complete_condition_type_Detail());
        }
        schema.addImp("task.task", task_task());
        schema.addImp("task.task2", task_task2());
        schema.addImp("task.task_extra_exp", task_task_extra_exp());
        return schema;
    }

    static Schema LevelRank() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("Level", SchemaPrimitive.SInt);
        s2.addColumn("Rank", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema Position() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("x", SchemaPrimitive.SInt);
        s2.addColumn("y", SchemaPrimitive.SInt);
        s2.addColumn("z", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema Range() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("Min", SchemaPrimitive.SInt);
        s2.addColumn("Max", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema ai_TriggerTick() {
        SchemaInterface s2 = new SchemaInterface();
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("value", SchemaPrimitive.SInt);
            s2.addImp("ConstValue", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("init", SchemaPrimitive.SInt);
            s3.addColumn("coefficient", SchemaPrimitive.SFloat);
            s2.addImp("ByLevel", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("init", SchemaPrimitive.SInt);
            s3.addColumn("coefficient1", SchemaPrimitive.SFloat);
            s3.addColumn("coefficient2", SchemaPrimitive.SFloat);
            s2.addImp("ByServerUpDay", s3);
        }
        return s2;
    }

    static Schema combat_AbilityUse() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("binding", new SchemaRef("combat.Binding"));
        s2.addColumn("machines", new SchemaList(new SchemaRef("combat.MachineUse")));
        return s2;
    }

    static Schema combat_Anchor() {
        SchemaBean s2 = new SchemaBean(false);
        return s2;
    }

    static Schema combat_AnchorUse() {
        SchemaBean s2 = new SchemaBean(false);
        return s2;
    }

    static Schema combat_Binding() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("abilityId", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema combat_BookEntry() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("name", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema combat_Combo() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("weapon", SchemaPrimitive.SStr);
        s2.addColumn("tier", SchemaPrimitive.SInt);
        s2.addColumn("airborne", SchemaPrimitive.SBool);
        s2.addColumn("effect", new SchemaRef("combat.Effect"));
        return s2;
    }

    static Schema combat_Command() {
        SchemaInterface s2 = new SchemaInterface();
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("phases", new SchemaList(new SchemaRef("combat.NestedPhase")));
            s2.addImp("StateMachine", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("key", SchemaPrimitive.SStr);
            s2.addImp("Jump", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s2.addImp("Noop", s3);
        }
        return s2;
    }

    static Schema combat_Effect() {
        SchemaInterface s2 = new SchemaInterface();
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("amount", SchemaPrimitive.SInt);
            s2.addImp("Damage", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("amount", SchemaPrimitive.SInt);
            s2.addImp("Heal", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s2.addImp("None", s3);
        }
        return s2;
    }

    static Schema combat_Exit() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("effect", new SchemaRef("combat.Effect"));
        return s2;
    }

    static Schema combat_Frame() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("tick", SchemaPrimitive.SLong);
        s2.addColumn("effect", new SchemaRef("combat.Effect"));
        return s2;
    }

    static Schema combat_Impact() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("ordinal", SchemaPrimitive.SInt);
        s2.addColumn("damage", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema combat_Machine() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("code", SchemaPrimitive.SStr);
        s2.addColumn("phases", new SchemaList(new SchemaRef("combat.Phase")));
        s2.addColumn("impacts", new SchemaList(new SchemaRef("combat.Impact")));
        s2.addColumn("frames", new SchemaList(new SchemaRef("combat.Frame")));
        s2.addColumn("stances", new SchemaList(new SchemaRef("combat.Stance")));
        s2.addColumn("variants", new SchemaList(new SchemaRef("combat.PhaseVariant")));
        s2.addColumn("combos", new SchemaList(new SchemaRef("combat.Combo")));
        s2.addColumn("markers", new SchemaList(new SchemaRef("combat.Marker")));
        s2.addColumn("anchor", new SchemaRef("combat.Anchor"));
        s2.addColumn("anchors", new SchemaList(new SchemaRef("combat.AnchorUse")));
        s2.addColumn("onExit", new SchemaRef("combat.Exit"));
        s2.addColumn("initial", SchemaPrimitive.SStr);
        s2.addColumn("impact", SchemaPrimitive.SInt);
        s2.addColumn("frame", SchemaPrimitive.SLong);
        s2.addColumn("stance", SchemaPrimitive.SBool);
        s2.addColumn("variant", new SchemaRef("combat.PhaseKey"));
        s2.addColumn("weapon", SchemaPrimitive.SStr);
        s2.addColumn("tier", SchemaPrimitive.SInt);
        s2.addColumn("airborne", SchemaPrimitive.SBool);
        return s2;
    }

    static Schema combat_MachineUse() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("machineKey", SchemaPrimitive.SStr);
        s2.addColumn("targets", new SchemaList(new SchemaRef("combat.PhaseUse")));
        return s2;
    }

    static Schema combat_Marker() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("key", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema combat_NestedPhase() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("key", SchemaPrimitive.SStr);
        s2.addColumn("action", new SchemaRef("combat.Command"));
        return s2;
    }

    static Schema combat_OptionalAim() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("abilityId", SchemaPrimitive.SInt);
        s2.addColumn("machineKey", SchemaPrimitive.SStr);
        s2.addColumn("phaseKey", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema combat_Origin() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("x", SchemaPrimitive.SInt);
        s2.addColumn("y", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema combat_Phase() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("key", SchemaPrimitive.SStr);
        s2.addColumn("next", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema combat_PhaseKey() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("name", SchemaPrimitive.SStr);
        s2.addColumn("variant", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema combat_PhaseUse() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("phaseKey", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema combat_PhaseVariant() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("key", new SchemaRef("combat.PhaseKey"));
        s2.addColumn("effect", new SchemaRef("combat.Effect"));
        return s2;
    }

    static Schema combat_Reward() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("count", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema combat_Stance() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("airborne", SchemaPrimitive.SBool);
        s2.addColumn("effect", new SchemaRef("combat.Effect"));
        return s2;
    }

    static Schema equip_TestPackBean() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("name", SchemaPrimitive.SStr);
        s2.addColumn("iRange", new SchemaRef("Range"));
        return s2;
    }

    static Schema other_DropItem() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("chance", SchemaPrimitive.SInt);
        s2.addColumn("itemids", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("countmin", SchemaPrimitive.SInt);
        s2.addColumn("countmax", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema task_TestDefaultBean() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("testInt", SchemaPrimitive.SInt);
        s2.addColumn("testBool", SchemaPrimitive.SBool);
        s2.addColumn("testString", SchemaPrimitive.SStr);
        s2.addColumn("testSubBean", new SchemaRef("Position"));
        s2.addColumn("testList", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("testList2", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("testMap", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SStr));
        return s2;
    }

    static Schema task_completecondition() {
        SchemaInterface s2 = new SchemaInterface();
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("monsterid", SchemaPrimitive.SInt);
            s3.addColumn("count", SchemaPrimitive.SInt);
            s2.addImp("KillMonster", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("npcid", SchemaPrimitive.SInt);
            s2.addImp("TalkNpc", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s2.addImp("TestNoColumn", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("msg", SchemaPrimitive.SStr);
            s2.addImp("Chat", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("cond1", new SchemaRef("task.completecondition"));
            s3.addColumn("cond2", new SchemaRef("task.completecondition"));
            s2.addImp("ConditionAnd", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s3.addColumn("itemid", SchemaPrimitive.SInt);
            s3.addColumn("count", SchemaPrimitive.SInt);
            s2.addImp("CollectItem", s3);
        }
        {
            SchemaBean s3 = new SchemaBean(false);
            s2.addImp("aa", s3);
        }
        return s2;
    }

    static Schema ai_ai() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("ID", SchemaPrimitive.SInt);
        s2.addColumn("Desc", SchemaPrimitive.SStr);
        s2.addColumn("CondID", SchemaPrimitive.SStr);
        s2.addColumn("TrigTick", new SchemaRef("ai.TriggerTick"));
        s2.addColumn("TrigOdds", SchemaPrimitive.SInt);
        s2.addColumn("ActionID", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("DeathRemove", SchemaPrimitive.SBool);
        return s2;
    }

    static Schema ai_ai_action() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("ID", SchemaPrimitive.SInt);
        s2.addColumn("Desc", SchemaPrimitive.SStr);
        s2.addColumn("FormulaID", SchemaPrimitive.SInt);
        s2.addColumn("ArgIList", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("ArgSList", new SchemaList(SchemaPrimitive.SInt));
        return s2;
    }

    static Schema ai_ai_condition() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("ID", SchemaPrimitive.SInt);
        s2.addColumn("Desc", SchemaPrimitive.SStr);
        s2.addColumn("FormulaID", SchemaPrimitive.SInt);
        s2.addColumn("ArgIList", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("ArgSList", new SchemaList(SchemaPrimitive.SInt));
        return s2;
    }

    static Schema combat_ability() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("code", SchemaPrimitive.SStr);
        s2.addColumn("origin", new SchemaRef("combat.Origin"));
        s2.addColumn("machines", new SchemaList(new SchemaRef("combat.Machine")));
        s2.addColumn("startMachine", SchemaPrimitive.SStr);
        s2.addColumn("startPhase", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema combat_branchreward() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("key", new SchemaRef("combat.PhaseKey"));
        s2.addColumn("reward", new SchemaRef("combat.Reward"));
        return s2;
    }

    static Schema combat_cast() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("abilityCode", SchemaPrimitive.SStr);
        s2.addColumn("origin", new SchemaRef("combat.Origin"));
        s2.addColumn("uses", new SchemaList(new SchemaRef("combat.AbilityUse")));
        s2.addColumn("optional", new SchemaList(new SchemaRef("combat.OptionalAim")));
        s2.addColumn("school", SchemaPrimitive.SStr);
        s2.addColumn("tier", SchemaPrimitive.SInt);
        s2.addColumn("airborne", SchemaPrimitive.SBool);
        s2.addColumn("bookCode", SchemaPrimitive.SStr);
        s2.addColumn("edition", SchemaPrimitive.SInt);
        s2.addColumn("rewardKey", new SchemaRef("combat.PhaseKey"));
        return s2;
    }

    static Schema combat_encounter() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("command", new SchemaRef("combat.Command"));
        return s2;
    }

    static Schema combat_phasebook() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("school", SchemaPrimitive.SStr);
        s2.addColumn("tier", SchemaPrimitive.SInt);
        s2.addColumn("airborne", SchemaPrimitive.SBool);
        s2.addColumn("code", SchemaPrimitive.SStr);
        s2.addColumn("edition", SchemaPrimitive.SInt);
        s2.addColumn("entry", new SchemaRef("combat.BookEntry"));
        return s2;
    }

    static Schema combat_timeline() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("events", new SchemaMap(new SchemaRef("combat.PhaseKey"), new SchemaRef("combat.Effect")));
        s2.addColumn("channels", new SchemaMap(SchemaPrimitive.SStr, new SchemaRef("combat.Effect")));
        s2.addColumn("slots", new SchemaMap(SchemaPrimitive.SInt, new SchemaRef("combat.Effect")));
        s2.addColumn("clock", new SchemaMap(SchemaPrimitive.SLong, new SchemaRef("combat.Effect")));
        s2.addColumn("modes", new SchemaMap(SchemaPrimitive.SBool, new SchemaRef("combat.Effect")));
        s2.addColumn("eventKey", new SchemaRef("combat.PhaseKey"));
        s2.addColumn("channel", SchemaPrimitive.SStr);
        s2.addColumn("slot", SchemaPrimitive.SInt);
        s2.addColumn("frame", SchemaPrimitive.SLong);
        s2.addColumn("airborne", SchemaPrimitive.SBool);
        s2.addColumn("fallback", new SchemaRef("combat.Effect"));
        return s2;
    }

    static Schema equip_ability() {
        SchemaEnum s2 = new SchemaEnum(false, true);
        s2.addValue("attack", 1);
        s2.addValue("defence", 2);
        s2.addValue("hp", 3);
        s2.addValue("critical", 4);
        s2.addValue("critical_resist", 5);
        s2.addValue("block", 6);
        s2.addValue("break_armor", 7);
        return s2;
    }

    static Schema equip_ability_Detail() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("name", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema equip_equip_config() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("entry", SchemaPrimitive.SStr);
        s2.addColumn("stone_count_for_set", SchemaPrimitive.SInt);
        s2.addColumn("draw_protect_name", SchemaPrimitive.SStr);
        s2.addColumn("broadcastid", SchemaPrimitive.SInt);
        s2.addColumn("broadcast_least_quality", SchemaPrimitive.SInt);
        s2.addColumn("week_reward_mailid", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema equip_equip_config_Entry() {
        SchemaEnum s2 = new SchemaEnum(true, false);
        s2.addValue("Instance");
        s2.addValue("Instance2");
        return s2;
    }

    static Schema equip_jewelry() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("ID", SchemaPrimitive.SInt);
        s2.addColumn("Name", SchemaPrimitive.SStr);
        s2.addColumn("IconFile", SchemaPrimitive.SStr);
        s2.addColumn("LvlRank", new SchemaRef("LevelRank"));
        s2.addColumn("JType", SchemaPrimitive.SStr);
        s2.addColumn("SuitID", SchemaPrimitive.SInt);
        s2.addColumn("KeyAbility", SchemaPrimitive.SInt);
        s2.addColumn("KeyAbilityValue", SchemaPrimitive.SInt);
        s2.addColumn("SalePrice", SchemaPrimitive.SInt);
        s2.addColumn("Description", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema equip_jewelry_random() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("LvlRank", new SchemaRef("LevelRank"));
        s2.addColumn("AttackRange", new SchemaRef("Range"));
        s2.addColumn("OtherRange", new SchemaList(new SchemaRef("Range")));
        s2.addColumn("TestPack", new SchemaList(new SchemaRef("equip.TestPackBean")));
        return s2;
    }

    static Schema equip_jewelry_suit() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("SuitID", SchemaPrimitive.SInt);
        s2.addColumn("Ename", SchemaPrimitive.SStr);
        s2.addColumn("Name", SchemaPrimitive.SText);
        s2.addColumn("Ability1", SchemaPrimitive.SInt);
        s2.addColumn("Ability1Value", SchemaPrimitive.SInt);
        s2.addColumn("Ability2", SchemaPrimitive.SInt);
        s2.addColumn("Ability2Value", SchemaPrimitive.SInt);
        s2.addColumn("Ability3", SchemaPrimitive.SInt);
        s2.addColumn("Ability3Value", SchemaPrimitive.SInt);
        s2.addColumn("SuitList", new SchemaList(SchemaPrimitive.SInt));
        return s2;
    }

    static Schema equip_jewelry_suit_Entry() {
        SchemaEnum s2 = new SchemaEnum(true, true);
        s2.addValue("SpecialSuit", 4);
        return s2;
    }

    static Schema equip_jewelry_type() {
        SchemaEnum s2 = new SchemaEnum(false, false);
        s2.addValue("Jade");
        s2.addValue("Bracelet");
        s2.addValue("Magic");
        s2.addValue("Bottle");
        return s2;
    }

    static Schema equip_jewelry_type_Detail() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("TypeName", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema equip_rank() {
        SchemaEnum s2 = new SchemaEnum(false, true);
        s2.addValue("white", 0);
        s2.addValue("green", 1);
        s2.addValue("blue", 2);
        s2.addValue("purple", 3);
        s2.addValue("yellow", 4);
        s2.addValue("red", 5);
        return s2;
    }

    static Schema equip_rank_Detail() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("RankID", SchemaPrimitive.SInt);
        s2.addColumn("RankName", SchemaPrimitive.SStr);
        s2.addColumn("RankShowName", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema other_ArgCaptureMode() {
        SchemaEnum s2 = new SchemaEnum(false, false);
        s2.addValue("Snapshot");
        s2.addValue("Dynamic");
        return s2;
    }

    static Schema other_ArgCaptureMode_Detail() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("name", SchemaPrimitive.SStr);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("comment", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema other_drop() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("dropid", SchemaPrimitive.SInt);
        s2.addColumn("name", SchemaPrimitive.SText);
        s2.addColumn("items", new SchemaList(new SchemaRef("other.DropItem")));
        s2.addColumn("testmap", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SInt));
        return s2;
    }

    static Schema other_key_test() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id1", SchemaPrimitive.SInt);
        s2.addColumn("id2", SchemaPrimitive.SLong);
        s2.addColumn("id3", SchemaPrimitive.SInt);
        s2.addColumn("ids", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("enumTest", SchemaPrimitive.SStr);
        s2.addColumn("enumList", new SchemaList(SchemaPrimitive.SStr));
        return s2;
    }

    static Schema other_loot() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("lootid", SchemaPrimitive.SInt);
        s2.addColumn("ename", SchemaPrimitive.SStr);
        s2.addColumn("name", SchemaPrimitive.SText);
        s2.addColumn("chanceList", new SchemaList(SchemaPrimitive.SInt));
        return s2;
    }

    static Schema other_loot_item() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("lootid", SchemaPrimitive.SInt);
        s2.addColumn("itemid", SchemaPrimitive.SInt);
        s2.addColumn("chance", SchemaPrimitive.SInt);
        s2.addColumn("countmin", SchemaPrimitive.SInt);
        s2.addColumn("countmax", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema other_monster() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("posList", new SchemaList(new SchemaRef("Position")));
        s2.addColumn("lootId", SchemaPrimitive.SInt);
        s2.addColumn("lootItemId", SchemaPrimitive.SInt);
        s2.addColumn("enumMap1", new SchemaMap(SchemaPrimitive.SStr, SchemaPrimitive.SInt));
        s2.addColumn("enumMap2", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SStr));
        return s2;
    }

    static Schema other_sign_in() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("item2countMap", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SInt));
        s2.addColumn("vipitem2vipcountMap", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SInt));
        s2.addColumn("viplevel", SchemaPrimitive.SInt);
        s2.addColumn("IconFile", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema task_complete_condition_type() {
        SchemaEnum s2 = new SchemaEnum(false, true);
        s2.addValue("KillMonster", 1);
        s2.addValue("TalkNpc", 2);
        s2.addValue("CollectItem", 3);
        s2.addValue("ConditionAnd", 4);
        s2.addValue("Chat", 5);
        s2.addValue("TestNoColumn", 6);
        s2.addValue("aa", 7);
        return s2;
    }

    static Schema task_complete_condition_type_Detail() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("name", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema task_task() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("taskid", SchemaPrimitive.SInt);
        s2.addColumn("name", new SchemaList(SchemaPrimitive.SText));
        s2.addColumn("nexttask", SchemaPrimitive.SInt);
        s2.addColumn("completecondition", new SchemaRef("task.completecondition"));
        s2.addColumn("exp", SchemaPrimitive.SInt);
        s2.addColumn("testDefaultBean", new SchemaRef("task.TestDefaultBean"));
        return s2;
    }

    static Schema task_task2() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("taskid", SchemaPrimitive.SInt);
        s2.addColumn("name", new SchemaList(SchemaPrimitive.SText));
        s2.addColumn("nexttask", SchemaPrimitive.SInt);
        s2.addColumn("completecondition", new SchemaRef("task.completecondition"));
        s2.addColumn("exp", SchemaPrimitive.SInt);
        s2.addColumn("testBool", SchemaPrimitive.SBool);
        s2.addColumn("testString", SchemaPrimitive.SStr);
        s2.addColumn("testStruct", new SchemaRef("Position"));
        s2.addColumn("testList", new SchemaList(SchemaPrimitive.SInt));
        s2.addColumn("testListStruct", new SchemaList(new SchemaRef("Position")));
        s2.addColumn("testListInterface", new SchemaList(new SchemaRef("ai.TriggerTick")));
        return s2;
    }

    static Schema task_task_extra_exp() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("taskid", SchemaPrimitive.SInt);
        s2.addColumn("extraexp", SchemaPrimitive.SInt);
        s2.addColumn("test1", SchemaPrimitive.SStr);
        s2.addColumn("test2", SchemaPrimitive.SStr);
        s2.addColumn("fielda", SchemaPrimitive.SStr);
        s2.addColumn("fieldb", SchemaPrimitive.SStr);
        s2.addColumn("fieldc", SchemaPrimitive.SStr);
        return s2;
    }

}
