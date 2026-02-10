package config;

import configgen.genjava.*;

public class ConfigCodeSchema {

    public static Schema getCodeSchema() {
        SchemaInterface schema = new SchemaInterface();
        schema.addImp("Text", Text());
        schema.addImp("LevelRank", LevelRank());
        schema.addImp("Position", Position());
        schema.addImp("Range", Range());
        schema.addImp("ai.TriggerTick", ai_TriggerTick());
        schema.addImp("equip.TestPackBean", equip_TestPackBean());
        schema.addImp("other.DropItem", other_DropItem());
        schema.addImp("task.TestDefaultBean", task_TestDefaultBean());
        schema.addImp("task.completecondition", task_completecondition());
        schema.addImp("ai.ai", ai_ai());
        schema.addImp("ai.ai_action", ai_ai_action());
        schema.addImp("ai.ai_condition", ai_ai_condition());
        schema.addImp("equip.ability", equip_ability());
        schema.addImp("equip.equipconfig", equip_equipconfig());
        schema.addImp("equip.equipconfig_Entry", equip_equipconfig_Entry());
        schema.addImp("equip.jewelry", equip_jewelry());
        schema.addImp("equip.jewelryrandom", equip_jewelryrandom());
        schema.addImp("equip.jewelrysuit", equip_jewelrysuit());
        schema.addImp("equip.jewelrysuit_Entry", equip_jewelrysuit_Entry());
        schema.addImp("equip.jewelrytype", equip_jewelrytype());
        schema.addImp("equip.rank", equip_rank());
        schema.addImp("equip.rank_Detail", equip_rank_Detail());
        schema.addImp("other.drop", other_drop());
        schema.addImp("other.keytest", other_keytest());
        schema.addImp("other.loot", other_loot());
        schema.addImp("other.lootitem", other_lootitem());
        schema.addImp("other.monster", other_monster());
        schema.addImp("other.signin", other_signin());
        schema.addImp("task.completeconditiontype", task_completeconditiontype());
        schema.addImp("task.task", task_task());
        schema.addImp("task.task2", task_task2());
        schema.addImp("task.taskextraexp", task_taskextraexp());
        return schema;
    }

    static Schema Text() {
        SchemaBean s2 = new SchemaBean(false);
        s2.addColumn("zh_cn", SchemaPrimitive.SStr);
        s2.addColumn("en", SchemaPrimitive.SStr);
        s2.addColumn("tw", SchemaPrimitive.SStr);
        return s2;
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

    static Schema equip_equipconfig() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("entry", SchemaPrimitive.SStr);
        s2.addColumn("stone_count_for_set", SchemaPrimitive.SInt);
        s2.addColumn("draw_protect_name", SchemaPrimitive.SStr);
        s2.addColumn("broadcastid", SchemaPrimitive.SInt);
        s2.addColumn("broadcast_least_quality", SchemaPrimitive.SInt);
        s2.addColumn("week_reward_mailid", SchemaPrimitive.SInt);
        return s2;
    }

    static Schema equip_equipconfig_Entry() {
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

    static Schema equip_jewelryrandom() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("LvlRank", new SchemaRef("LevelRank"));
        s2.addColumn("AttackRange", new SchemaRef("Range"));
        s2.addColumn("OtherRange", new SchemaList(new SchemaRef("Range")));
        s2.addColumn("TestPack", new SchemaList(new SchemaRef("equip.TestPackBean")));
        return s2;
    }

    static Schema equip_jewelrysuit() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("SuitID", SchemaPrimitive.SInt);
        s2.addColumn("Ename", SchemaPrimitive.SStr);
        s2.addColumn("Name", SchemaPrimitive.SStr);
        s2.addColumn("Ability1", SchemaPrimitive.SInt);
        s2.addColumn("Ability1Value", SchemaPrimitive.SInt);
        s2.addColumn("Ability2", SchemaPrimitive.SInt);
        s2.addColumn("Ability2Value", SchemaPrimitive.SInt);
        s2.addColumn("Ability3", SchemaPrimitive.SInt);
        s2.addColumn("Ability3Value", SchemaPrimitive.SInt);
        s2.addColumn("SuitList", new SchemaList(SchemaPrimitive.SInt));
        return s2;
    }

    static Schema equip_jewelrysuit_Entry() {
        SchemaEnum s2 = new SchemaEnum(true, true);
        s2.addValue("SpecialSuit", 4);
        return s2;
    }

    static Schema equip_jewelrytype() {
        SchemaEnum s2 = new SchemaEnum(false, false);
        s2.addValue("Jade");
        s2.addValue("Bracelet");
        s2.addValue("Magic");
        s2.addValue("Bottle");
        return s2;
    }

    static Schema equip_rank() {
        SchemaEnum s2 = new SchemaEnum(false, true);
        s2.addValue("white", 1);
        s2.addValue("green", 2);
        s2.addValue("blue", 3);
        s2.addValue("purple", 4);
        s2.addValue("yellow", 5);
        return s2;
    }

    static Schema equip_rank_Detail() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("RankID", SchemaPrimitive.SInt);
        s2.addColumn("RankName", SchemaPrimitive.SStr);
        s2.addColumn("RankShowName", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema other_drop() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("dropid", SchemaPrimitive.SInt);
        s2.addColumn("name", SchemaPrimitive.SStr);
        s2.addColumn("items", new SchemaList(new SchemaRef("other.DropItem")));
        s2.addColumn("testmap", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SInt));
        return s2;
    }

    static Schema other_keytest() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id1", SchemaPrimitive.SInt);
        s2.addColumn("id2", SchemaPrimitive.SLong);
        s2.addColumn("id3", SchemaPrimitive.SInt);
        s2.addColumn("ids", new SchemaList(SchemaPrimitive.SInt));
        return s2;
    }

    static Schema other_loot() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("lootid", SchemaPrimitive.SInt);
        s2.addColumn("ename", SchemaPrimitive.SStr);
        s2.addColumn("name", SchemaPrimitive.SStr);
        s2.addColumn("chanceList", new SchemaList(SchemaPrimitive.SInt));
        return s2;
    }

    static Schema other_lootitem() {
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
        return s2;
    }

    static Schema other_signin() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("id", SchemaPrimitive.SInt);
        s2.addColumn("item2countMap", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SInt));
        s2.addColumn("vipitem2vipcountMap", new SchemaMap(SchemaPrimitive.SInt, SchemaPrimitive.SInt));
        s2.addColumn("viplevel", SchemaPrimitive.SInt);
        s2.addColumn("IconFile", SchemaPrimitive.SStr);
        return s2;
    }

    static Schema task_completeconditiontype() {
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

    static Schema task_task() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("taskid", SchemaPrimitive.SInt);
        s2.addColumn("name", new SchemaList(SchemaPrimitive.SStr));
        s2.addColumn("nexttask", SchemaPrimitive.SInt);
        s2.addColumn("completecondition", new SchemaRef("task.completecondition"));
        s2.addColumn("exp", SchemaPrimitive.SInt);
        s2.addColumn("testDefaultBean", new SchemaRef("task.TestDefaultBean"));
        return s2;
    }

    static Schema task_task2() {
        SchemaBean s2 = new SchemaBean(true);
        s2.addColumn("taskid", SchemaPrimitive.SInt);
        s2.addColumn("name", new SchemaList(SchemaPrimitive.SStr));
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

    static Schema task_taskextraexp() {
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
